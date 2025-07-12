package org.wordpress.android.ui.accounts.login

import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.UrlUtils
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import javax.inject.Named

private const val URL_TAG = "url"
private const val SUCCESS_TAG = "success"

class ApplicationPasswordLoginHelper @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcherWrapper: DispatcherWrapper,
    private val siteStore: SiteStore,
    private val uriLoginWrapper: UriLoginWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val wpLoginClient: WpLoginClient,
    private val appLogWrapper: AppLogWrapper,
    private val apiRootUrlCache: ApiRootUrlCache,
    private val discoverSuccessWrapper: DiscoverSuccessWrapper
) {
    private var processedAppPasswordData: String? = null

    @Suppress("TooGenericExceptionCaught")
    suspend fun getAuthorizationUrlComplete(siteUrl: String): String =
        try {
            getAuthorizationUrlCompleteInternal(siteUrl)
        } catch (throwable: Throwable) {
            handleAuthenticationDiscoveryError(siteUrl, throwable)
        }

    private suspend fun getAuthorizationUrlCompleteInternal(siteUrl: String): String = withContext(bgDispatcher) {
        when (val urlDiscoveryResult = wpLoginClient.apiDiscovery(siteUrl)) {
            is ApiDiscoveryResult.Success -> {
                val authorizationUrl =
                    discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(urlDiscoveryResult)
                val apiRootUrl = discoverSuccessWrapper.getApiRootUrl(urlDiscoveryResult)
                // Store the ApiRootUrl for use it after the login
                apiRootUrlCache.put(UrlUtils.normalizeUrl(siteUrl), apiRootUrl)
                val authorizationUrlComplete =
                    uriLoginWrapper.appendParamsToRestAuthorizationUrl(authorizationUrl)
                Log.d("WP_RS", "Found authorization for $siteUrl URL: $authorizationUrlComplete" +
                        " API_ROOT_URL $apiRootUrl")
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
                authorizationUrlComplete
            }

            is ApiDiscoveryResult.FailureFetchAndParseApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFetchAndParseApiRoot"))

            is ApiDiscoveryResult.FailureFindApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFindApiRoot"))

            is ApiDiscoveryResult.FailureParseSiteUrl ->
                handleAuthenticationDiscoveryError(siteUrl, urlDiscoveryResult.error)
        }
    }

    private fun handleAuthenticationDiscoveryError(siteUrl: String, throwable: Throwable): String {
        appLogWrapper.e(AppLog.T.API, "WP_RS: Error during API discovery for $siteUrl - ${throwable.message}")
        AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        return ""
    }

    @Suppress("ComplexCondition")
    suspend fun storeApplicationPasswordCredentialsFrom(urlLogin: UriLogin): Boolean {
        if (urlLogin.apiRootUrl == null ||
            urlLogin.user.isNullOrEmpty() ||
            urlLogin.password.isNullOrEmpty() ||
            urlLogin.siteUrl == null ||
            urlLogin.siteUrl == processedAppPasswordData
            ) {
            return false
        }

        return withContext(bgDispatcher) {
            val normalizedUrl = UrlUtils.normalizeUrl(urlLogin.siteUrl)
            val site = siteStore.sites.firstOrNull { UrlUtils.normalizeUrl(it.url) ==  normalizedUrl}
            if (site != null) {
                site.apply {
                    apiRestUsernameEncrypted = ""
                    apiRestPasswordEncrypted = ""
                    apiRestUsernameIV = ""
                    apiRestPasswordIV = ""
                    apiRestUsernamePlain = urlLogin.user
                    apiRestPasswordPlain = urlLogin.password
                    wpApiRestUrl = urlLogin.apiRootUrl
                }
                dispatcherWrapper.insertOrUpdateSite(site)
                trackSuccessful(urlLogin.siteUrl)
                processedAppPasswordData = urlLogin.siteUrl // Save locally to avoid duplicated calls
                true
            } else {
                appLogWrapper.e(
                    AppLog.T.DB,
                    "WP_RS: Cannot save application password credentials for: ${urlLogin.siteUrl}"
                )
                false
            }
        }
    }

    private fun trackSuccessful(siteUrl: String) {
        val properties: MutableMap<String, String?> = HashMap()
        properties[URL_TAG] = siteUrl
        properties[SUCCESS_TAG] = "true"
        AnalyticsTracker.track(
            if (buildConfigWrapper.isJetpackApp) {
                Stat.JP_ANDROID_APPLICATION_PASSWORD_LOGIN
            } else {
                Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN
            },
            properties
        )
        appLogWrapper.d(AppLog.T.DB, "WP_RS: Saved application password credentials for: $siteUrl")
    }

    fun getSiteUrlLoginFromRawData(url: String): UriLogin {
        return uriLoginWrapper.parseUriLogin(url)
    }

    /**
     * Removes all the application Password credentials
     * @return the number of sites that were affected
     */
    suspend fun removeAllApplicationPasswordCredentials(): Int {
        return withContext(bgDispatcher) {
            val sites = siteStore.sites
            val affectedSites = sites.count { !it.apiRestUsernameEncrypted.isNullOrEmpty() }
            sites.forEach { site ->
                site.apply {
                    apiRestUsernamePlain = ""
                    apiRestPasswordPlain = ""
                    apiRestUsernameEncrypted = ""
                    apiRestPasswordEncrypted = ""
                    apiRestUsernameIV = ""
                    apiRestPasswordIV = ""
                }
                dispatcherWrapper.insertOrUpdateSite(site)
            }
            affectedSites
        }
    }

    fun getApplicationPasswordSitesCount(): Int {
        return siteStore.sites.count { !it.apiRestUsernameEncrypted.isNullOrEmpty() }
    }

    /**
     * This class is created to wrap the Uri calls and let us unit test the login helper
     */
    class UriLoginWrapper @Inject constructor(private val apiRootUrlCache: ApiRootUrlCache) {
        fun parseUriLogin(url: String): UriLogin {
            val uri = url.toUri()
            val siteUrl = UrlUtils.normalizeUrl(uri.getQueryParameter("site_url"))
            val userLogin = uri.getQueryParameter("user_login")
            val password = uri.getQueryParameter("password")
            val apiRootUrl = apiRootUrlCache.get(siteUrl)
            return UriLogin(siteUrl, userLogin, password, apiRootUrl)
        }

        fun appendParamsToRestAuthorizationUrl(authorizationUrl: String?): String {
            return if (authorizationUrl.isNullOrEmpty()) {
                authorizationUrl.orEmpty()
            } else {
                authorizationUrl.toUri().buildUpon().apply {
                    appendQueryParameter("app_name", "android-jetpack-client")
                    appendQueryParameter("success_url", "jetpack://app-pass-authorize")
                }.build().toString()
            }
        }
    }

    data class UriLogin(
        val siteUrl: String?,
        val user: String?,
        val password: String?,
        val apiRootUrl: String?
    )

    // We need to wrap the dispatcher because tests are failing due to the actions not having a proper equals method
    // so, every action is returning false when compared with the one we want to test
    class DispatcherWrapper @Inject constructor(private val dispatcher: Dispatcher) {
        fun insertOrUpdateSite(site: SiteModel) {
            dispatcher.dispatch(
                SiteActionBuilder.newUpdateSiteAction(site)
            )
        }
    }

    class DiscoverSuccessWrapper @Inject constructor() {
        fun getApiRootUrl(successObject: ApiDiscoveryResult.Success) = successObject.success.apiRootUrl.url()

        fun getApplicationPasswordsAuthenticationUrl(successObject: ApiDiscoveryResult.Success) =
            successObject.success.applicationPasswordsAuthenticationUrl.url()
    }
}
