package org.wordpress.android.ui.accounts.login

import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
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
    private val siteSqlUtils: SiteSqlUtils,
    private val uriLoginWrapper: UriLoginWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val wpLoginClient: WpLoginClient,
    private val appLogWrapper: AppLogWrapper,
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
                val authorizationUrl = urlDiscoveryResult.success.applicationPasswordsAuthenticationUrl.url()
                val authorizationUrlComplete =
                    uriLoginWrapper.appendParamsToRestAuthorizationUrl(authorizationUrl)
                Log.d("WP_RS", "Found authorization for $siteUrl URL: $authorizationUrlComplete")
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

    @Suppress("ReturnCount")
    suspend fun storeApplicationPasswordCredentialsFrom(url: String): Boolean {
        if (url.isEmpty() || url == processedAppPasswordData) {
            return false
        }

        return withContext(bgDispatcher) {
            val uriLogin = uriLoginWrapper.parseUriLogin(url)

            if (uriLogin.user.isNullOrEmpty() || uriLogin.password.isNullOrEmpty() ) {
                false
            } else {
                val normalizedUrl = UrlUtils.normalizeUrl(uriLogin.siteUrl)
                val site = siteSqlUtils.getSites().firstOrNull { UrlUtils.normalizeUrl(it.url) ==  normalizedUrl}
                if (site != null) {
                    site.apply {
                        apiRestUsernameEncrypted = ""
                        apiRestPasswordEncrypted = ""
                        apiRestUsernameIV = ""
                        apiRestPasswordIV = ""
                        apiRestUsernamePlain = uriLogin.user
                        apiRestPasswordPlain = uriLogin.password
                    }
                    siteSqlUtils.insertOrUpdateSite(site)
                    uriLogin.siteUrl?.let { trackSuccessful(it) }
                    processedAppPasswordData = url // Save locally to avoid duplicated calls
                    true
                } else {
                    appLogWrapper.e(
                        AppLog.T.DB,
                        "WP_RS: Cannot save application password credentials for: ${uriLogin.siteUrl}"
                    )
                    false
                }
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
            val sites = siteSqlUtils.getSites()
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
                siteSqlUtils.insertOrUpdateSite(site)
            }
            affectedSites
        }
    }

    fun getApplicationPasswordSitesCount(): Int {
        val sites = siteSqlUtils.getSites()
        return sites.count { !it.apiRestUsernameEncrypted.isNullOrEmpty() }
    }

    /**
     * This class is created to wrap the Uri calls and let us unit test the login helper
     */
    class UriLoginWrapper @Inject constructor() {
        fun parseUriLogin(url: String): UriLogin {
            val uri = url.toUri()
            return UriLogin(
                uri.getQueryParameter("site_url"),
                uri.getQueryParameter("user_login"),
                uri.getQueryParameter("password")
            )
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
        val password: String?
    )
}
