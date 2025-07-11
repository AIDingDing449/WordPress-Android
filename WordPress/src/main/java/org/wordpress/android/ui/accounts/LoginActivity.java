package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.material.snackbar.Snackbar;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.login.AuthOptions;
import org.wordpress.android.login.GoogleFragment;
import org.wordpress.android.login.GoogleFragment.GoogleListener;
import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.login.LoginEmailFragment;
import org.wordpress.android.login.LoginEmailPasswordFragment;
import org.wordpress.android.login.LoginGoogleFragment;
import org.wordpress.android.login.LoginListener;
import org.wordpress.android.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.login.LoginMagicLinkSentFragment;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.login.LoginSiteAddressFragment;
import org.wordpress.android.ui.accounts.login.applicationpassword.LoginSiteApplicationPasswordFragment;
import org.wordpress.android.login.LoginUsernamePasswordFragment;
import org.wordpress.android.login.SignupConfirmationFragment;
import org.wordpress.android.login.SignupGoogleFragment;
import org.wordpress.android.login.SignupMagicLinkFragment;
import org.wordpress.android.support.SupportWebViewActivity;
import org.wordpress.android.support.ZendeskExtraTags;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionSource;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError;
import org.wordpress.android.ui.accounts.SmartLockHelper.Callback;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Source;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.ui.accounts.login.LoginPrologueListener;
import org.wordpress.android.ui.accounts.login.LoginPrologueRevampedFragment;
import org.wordpress.android.ui.accounts.login.WPcomLoginHelper;
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesFragment;
import org.wordpress.android.ui.accounts.login.jetpack.LoginSiteCheckErrorFragment;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.main.ChooseSiteActivity;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures;
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.config.ContactSupportFeatureConfig;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.hilt.android.AndroidEntryPoint;

import static org.wordpress.android.util.ActivityUtils.hideKeyboard;

@AndroidEntryPoint
public class LoginActivity extends BaseAppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        Callback, LoginListener, GoogleListener, LoginPrologueListener,
        HasAndroidInjector, BasicDialogPositiveClickInterface {
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    private static final String KEY_SMARTLOCK_HELPER_STATE = "KEY_SMARTLOCK_HELPER_STATE";
    private static final String KEY_SIGNUP_FROM_LOGIN_ENABLED = "KEY_SIGNUP_FROM_LOGIN_ENABLED";
    private static final String KEY_SITE_LOGIN_AVAILABLE_FROM_PROLOGUE = "KEY_SITE_LOGIN_AVAILABLE_FROM_PROLOGUE";
    private static final String KEY_UNIFIED_TRACKER_SOURCE = "KEY_UNIFIED_TRACKER_SOURCE";
    private static final String KEY_UNIFIED_TRACKER_FLOW = "KEY_UNIFIED_TRACKER_FLOW";

    private static final String FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword";

    private static final String GOOGLE_ERROR_DIALOG_TAG = "google_error_dialog_tag";

    private enum SmartLockHelperState {
        NOT_TRIGGERED,
        TRIGGER_FILL_IN_ON_CONNECT,
        FINISH_ON_CONNECT,
        FINISHED
    }

    private SmartLockHelper mSmartLockHelper;
    private SmartLockHelperState mSmartLockHelperState = SmartLockHelperState.NOT_TRIGGERED;
    private JetpackConnectionSource mJetpackConnectSource;
    private boolean mIsJetpackConnect;

    private boolean mIsSignupFromLoginEnabled;
    private boolean mIsSmartLockTriggeredFromPrologue;
    private boolean mIsSiteLoginAvailableFromPrologue;

    private LoginMode mLoginMode;
    private LoginViewModel mViewModel;
    @Inject protected WPcomLoginHelper mLoginHelper;

    @Inject DispatchingAndroidInjector<Object> mDispatchingAndroidInjector;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject ZendeskHelper mZendeskHelper;
    @Inject UnifiedLoginTracker mUnifiedLoginTracker;
    @Inject protected SiteStore mSiteStore;
    @Inject protected ViewModelProvider.Factory mViewModelFactory;
    @Inject BuildConfigWrapper mBuildConfigWrapper;
    @Inject ContactSupportFeatureConfig mContactSupportFeatureConfig;

    @Inject ExperimentalFeatures mExperimentalFeatures;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Attempt Login if this activity was created in response to a user confirming login, and if
        // successful clear the intent so we don't reuse the OAuth code if the activity is recreated
        boolean loginProcessed = mLoginHelper.tryLoginWithDataString(getIntent().getDataString());

        if (loginProcessed) {
            getIntent().setData(null);
        }

        // Start preloading the WordPress.com login page if needed – this avoids visual hitches
        // when displaying that screen
        mLoginHelper.bindCustomTabsService(this);

        // go no further if the user is already logged in and this is the login screen shown at startup
        //      FULL = WPAndroid
        //      JETPACK_LOGIN_ONLY = JPAndroid
        LoginMode loginMode = getLoginMode();
        if ((mLoginHelper.isLoggedIn()) && (loginMode == LoginMode.FULL || loginMode == LoginMode.JETPACK_LOGIN_ONLY)) {
            this.loggedInAndFinish(new ArrayList<Integer>(), true);
            return;
        }

        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            if (getIntent() != null) {
                mJetpackConnectSource =
                        (JetpackConnectionSource) getIntent().getSerializableExtra(ARG_JETPACK_CONNECT_SOURCE);
            }

            mLoginAnalyticsListener.trackLoginAccessed();

            switch (loginMode) {
                case FULL:
                case JETPACK_LOGIN_ONLY:
                    mUnifiedLoginTracker.setSource(Source.DEFAULT);
                    mIsSignupFromLoginEnabled = mBuildConfigWrapper.isSignupEnabled();
                    loginFromPrologue();
                    break;
                case WPCOM_LOGIN_ONLY:
                    mUnifiedLoginTracker.setSource(Source.ADD_WORDPRESS_COM_ACCOUNT);
                    mIsSignupFromLoginEnabled = mBuildConfigWrapper.isSignupEnabled();
                    checkSmartLockPasswordAndStartLogin();
                    break;
                case JETPACK_SELFHOSTED:
                case SELFHOSTED_ONLY:
                    mUnifiedLoginTracker.setSource(Source.SELF_HOSTED);
                    if (mExperimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE)) {
                        showFragment(new LoginSiteApplicationPasswordFragment(), LoginSiteAddressFragment.TAG);
                    } else {
                        showFragment(new LoginSiteAddressFragment(), LoginSiteAddressFragment.TAG);
                    }
                    break;
                case JETPACK_STATS:
                    mUnifiedLoginTracker.setSource(Source.JETPACK);
                    mIsSignupFromLoginEnabled = mBuildConfigWrapper.isSignupEnabled();
                    checkSmartLockPasswordAndStartLogin();
                    break;
                case WPCOM_LOGIN_DEEPLINK:
                    mUnifiedLoginTracker.setSource(Source.DEEPLINK);
                    checkSmartLockPasswordAndStartLogin();
                    break;
                case WPCOM_REAUTHENTICATE:
                    mUnifiedLoginTracker.setSource(Source.REAUTHENTICATION);
                    showWPcomLoginScreen(getBaseContext());
                    break;
                case SHARE_INTENT:
                    mUnifiedLoginTracker.setSource(Source.SHARE);
                    checkSmartLockPasswordAndStartLogin();
                    break;
                case WOO_LOGIN_MODE:
                    break;
            }
        } else {
            mSmartLockHelperState = SmartLockHelperState.valueOf(
                    savedInstanceState.getString(KEY_SMARTLOCK_HELPER_STATE));

            if (mSmartLockHelperState != SmartLockHelperState.NOT_TRIGGERED) {
                // reconnect SmartLockHelper
                initSmartLockHelperConnection();
            }

            mIsSignupFromLoginEnabled = savedInstanceState.getBoolean(KEY_SIGNUP_FROM_LOGIN_ENABLED);
            mIsSiteLoginAvailableFromPrologue = savedInstanceState.getBoolean(KEY_SITE_LOGIN_AVAILABLE_FROM_PROLOGUE);
            String source = savedInstanceState.getString(KEY_UNIFIED_TRACKER_SOURCE);
            if (source != null) {
                mUnifiedLoginTracker.setSource(source);
            }
            mUnifiedLoginTracker.setFlow(savedInstanceState.getString(KEY_UNIFIED_TRACKER_FLOW));
        }

        initViewModel();
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(LoginViewModel.class);

        // initObservers
        mViewModel.getNavigationEvents().observe(this, event -> {
            LoginNavigationEvents loginEvent = event.getContentIfNotHandled();
            if (loginEvent instanceof ShowSiteAddressError) {
                showSiteAddressError((ShowSiteAddressError) loginEvent);
            } else if (loginEvent instanceof ShowNoJetpackSites) {
                showNoJetpackSites();
            }
        });
    }

    private void loginFromPrologue() {
        showFragment(new LoginPrologueRevampedFragment(), LoginPrologueRevampedFragment.TAG);
        mIsSmartLockTriggeredFromPrologue = true;
        mIsSiteLoginAvailableFromPrologue = true;
        initSmartLockIfNotFinished(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SMARTLOCK_HELPER_STATE, mSmartLockHelperState.name());
        outState.putBoolean(KEY_SIGNUP_FROM_LOGIN_ENABLED, mIsSignupFromLoginEnabled);
        outState.putBoolean(KEY_SITE_LOGIN_AVAILABLE_FROM_PROLOGUE, mIsSiteLoginAvailableFromPrologue);
        outState.putString(KEY_UNIFIED_TRACKER_SOURCE, mUnifiedLoginTracker.getSource().getValue());
        Flow flow = mUnifiedLoginTracker.getFlow();
        if (flow != null) {
            outState.putString(KEY_UNIFIED_TRACKER_FLOW, flow.getValue());
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        fragmentTransaction.commit();
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void addGoogleFragment(GoogleFragment googleFragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        googleFragment.setRetainInstance(true);
        fragmentTransaction.add(googleFragment, tag);
        fragmentTransaction.commit();
    }

    private LoginPrologueRevampedFragment getLoginPrologueRevampedFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LoginPrologueRevampedFragment.TAG);
        return fragment == null ? null : (LoginPrologueRevampedFragment) fragment;
    }

    private LoginEmailFragment getLoginEmailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        return fragment == null ? null : (LoginEmailFragment) fragment;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        return false;
    }

    @Override
    public LoginMode getLoginMode() {
        if (mLoginMode != null) {
            // returned the cached value
            return mLoginMode;
        }

        // compute and cache the Login mode
        mLoginMode = LoginMode.fromIntent(getIntent());

        return mLoginMode;
    }

    private void loggedInAndFinish(ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        AppPrefs.setIsJetpackMigrationEligible(false);
        AppPrefs.setIsJetpackMigrationInProgress(false);
        switch (getLoginMode()) {
            case FULL:
            case JETPACK_LOGIN_ONLY:
            case WPCOM_LOGIN_ONLY:
                if (!mSiteStore.hasSite() && AppPrefs.shouldShowPostSignupInterstitial() && !doLoginUpdate) {
                    ActivityLauncher.showPostSignupInterstitial(this);
                } else {
                    ActivityLauncher.showMainActivityAndLoginEpilogue(this, oldSitesIds, doLoginUpdate);
                }
                setResult(Activity.RESULT_OK);
                finish();
                break;
            case JETPACK_STATS:
                ActivityLauncher.showLoginEpilogueForResult(this, oldSitesIds, true);
                break;
            case WPCOM_LOGIN_DEEPLINK:
            case WPCOM_REAUTHENTICATE:
                ActivityLauncher.showLoginEpilogueForResult(this, oldSitesIds, false);
                break;
            case SHARE_INTENT:
            case JETPACK_SELFHOSTED:
            case SELFHOSTED_ONLY:
                // We are comparing list of site ID's before self-hosted site was added and after, trying to find a
                // newly added self-hosted site's ID, so we can select it
                ArrayList<Integer> newSitesIds = new ArrayList<>();
                for (SiteModel site : mSiteStore.getSites()) {
                    newSitesIds.add(site.getId());
                }
                newSitesIds.removeAll(oldSitesIds);

                if (newSitesIds.size() > 0) {
                    Intent intent = new Intent();
                    intent.putExtra(ChooseSiteActivity.KEY_SITE_LOCAL_ID, newSitesIds.get(0));
                    setResult(Activity.RESULT_OK, intent);
                } else {
                    AppLog.e(T.MAIN, "Couldn't detect newly added self-hosted site. "
                                     + "Expected at least 1 site ID but was 0.");
                    ToastUtils.showToast(this, R.string.site_picker_failed_selecting_added_site);
                    setResult(Activity.RESULT_OK);
                }

                // skip the epilogue when only added a self-hosted site or sharing to WordPress
                finish();
                break;
            case WOO_LOGIN_MODE:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppLog.d(T.MAIN, "LoginActivity: onActivity Result - requestCode" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.SHOW_LOGIN_EPILOGUE_AND_RETURN:
            case RequestCodes.SHOW_SIGNUP_EPILOGUE_AND_RETURN:
                // we showed the epilogue screen as informational and sites got loaded so, just
                // return to login caller now
                setResult(RESULT_OK);
                finish();
                break;
            case RequestCodes.SMART_LOCK_SAVE:
                if (resultCode == RESULT_OK) {
                    mLoginAnalyticsListener.trackLoginAutofillCredentialsUpdated();
                    AppLog.d(AppLog.T.NUX, "Credentials saved");
                } else {
                    AppLog.d(AppLog.T.NUX, "Credentials save cancelled");
                }
                break;
            case RequestCodes.SMART_LOCK_READ:
                if (resultCode == RESULT_OK) {
                    AppLog.d(AppLog.T.NUX, "Credentials retrieved");
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onCredentialRetrieved(credential);
                } else {
                    AppLog.e(AppLog.T.NUX, "Credential read failed");
                    onCredentialsUnavailable();
                }
                break;
        }
    }

    private void jumpToUsernamePassword(String username, String password) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment =
                LoginUsernamePasswordFragment.newInstance("wordpress.com", "wordpress.com", username, password, true);
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG);
    }

    private boolean initSmartLockHelperConnection() {
        mSmartLockHelper = new SmartLockHelper(this);
        return mSmartLockHelper.initSmartLockForPasswords();
    }

    private void checkSmartLockPasswordAndStartLogin() {
        initSmartLockIfNotFinished(true);

        if (mSmartLockHelperState == SmartLockHelperState.FINISHED) {
            startLogin();
        }
    }

    /**
     * @param triggerFillInOnConnect set to true, if you want to show an account chooser dialog when the user has
     *                               stored their credentials in the past. Set to false, if you just want to
     *                               initialize SmartLock eg. when you want to use it just to save users credentials.
     */
    private void initSmartLockIfNotFinished(boolean triggerFillInOnConnect) {
        if (mSmartLockHelperState == SmartLockHelperState.NOT_TRIGGERED) {
            if (initSmartLockHelperConnection()) {
                if (triggerFillInOnConnect) {
                    mSmartLockHelperState = SmartLockHelperState.TRIGGER_FILL_IN_ON_CONNECT;
                } else {
                    mSmartLockHelperState = SmartLockHelperState.FINISH_ON_CONNECT;
                }
            } else {
                // just shortcircuit the attempt to use SmartLockHelper
                mSmartLockHelperState = SmartLockHelperState.FINISHED;
            }
        }
    }

    private void startLogin() {
        if (getLoginEmailFragment() != null) {
            // email screen is already shown so, login has already started. Just bail.
            return;
        }

        if (getLoginPrologueRevampedFragment() == null) {
            // prologue fragment is not shown so, the email screen will be the initial screen on the fragment container
            showFragment(LoginEmailFragment.newInstance(mIsSignupFromLoginEnabled), LoginEmailFragment.TAG);

            if (getLoginMode() == LoginMode.JETPACK_STATS) {
                mIsJetpackConnect = true;
            }
        } else {
            // prologue fragment is shown so, slide in the email screen (and add to history)
            slideInFragment(LoginEmailFragment.newInstance(mIsSignupFromLoginEnabled), true, LoginEmailFragment.TAG);
        }
    }

    // LoginPrologueListener implementation methods

    public void showWPcomLoginScreen(@NonNull Context context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_WEBVIEW);
        mUnifiedLoginTracker.setFlowAndStep(Flow.WORDPRESS_COM_WEB, Step.WPCOM_WEB_START);

        CustomTabsIntent intent = getCustomTabsIntent();

        Uri loginUri = mLoginHelper.getWpcomLoginUri();
        try {
            intent.launchUrl(this, loginUri);
        } catch (SecurityException | ActivityNotFoundException e) {
            AppLog.e(AppLog.T.UTILS, "Error opening login uri in CustomTabsIntent, attempting external browser", e);
            ActivityLauncher.openUrlExternal(this, loginUri.toString());
        }
    }

    @NonNull private CustomTabsIntent getCustomTabsIntent() {
        return new CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setStartAnimations(this, R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left)
                .setExitAnimations(this, R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right)
                .setUrlBarHidingEnabled(true)
                .setInstantAppsEnabled(false)
                .setShowTitle(false)
                .build();
    }

    @Override
    public void onTermsOfServiceClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_TERMS_OF_SERVICE_TAPPED);
        mUnifiedLoginTracker.trackClick(Click.TERMS_OF_SERVICE_CLICKED);
        ActivityLauncher.openUrlExternal(this, WPUrlUtils.buildTermsOfServiceUrl(this));
    }

    // LoginListener implementation methods

    @Override
    public void gotWpcomEmail(String email, boolean verifyEmail, @Nullable AuthOptions authOptions) {
        initSmartLockIfNotFinished(false);
        boolean isMagicLinkEnabled =
                getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT;

        if (authOptions != null) {
            if (authOptions.isPasswordless()) {
                showMagicLinkRequestScreen(email, verifyEmail, false, true);
            } else {
                showEmailPasswordScreen(email, verifyEmail, isMagicLinkEnabled);
            }
        } else {
            if (isMagicLinkEnabled) {
                showMagicLinkRequestScreen(email, verifyEmail, true, false);
            } else {
                showEmailPasswordScreen(email, verifyEmail, false);
            }
        }
    }

    private void showEmailPasswordScreen(String email, boolean verifyEmail, boolean allowMagicLink) {
        LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment
                .newInstance(email, null, null, null, false, allowMagicLink, verifyEmail);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    private void showMagicLinkRequestScreen(String email, boolean verifyEmail, boolean allowPassword,
                                            boolean forceRequestAtStart) {
        AuthEmailPayloadScheme scheme = mViewModel.getMagicLinkScheme();
        String jetpackConnectionSource = mJetpackConnectSource != null ? mJetpackConnectSource.toString() : null;
        LoginMagicLinkRequestFragment loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment
                .newInstance(email, scheme, mIsJetpackConnect, jetpackConnectionSource, verifyEmail, allowPassword,
                        forceRequestAtStart);
        slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG);
    }

    @Override
    public void gotUnregisteredEmail(String email) {
        showSignupMagicLink(email);
    }

    @Override
    public void gotUnregisteredSocialAccount(String email, String displayName, String idToken, String photoUrl,
                                             String service) {
        SignupConfirmationFragment signupConfirmationFragment =
                SignupConfirmationFragment.newInstance(email, displayName, idToken, photoUrl, service);
        slideInFragment(signupConfirmationFragment, true, SignupConfirmationFragment.TAG);
    }

    @Override
    public void loginViaSiteAddress() {
        final Fragment loginSiteAddressFragment;
        if (mExperimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE)) {
            loginSiteAddressFragment = new LoginSiteApplicationPasswordFragment();
        } else {
            loginSiteAddressFragment = new LoginSiteAddressFragment();
        }
        slideInFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG);
    }

    @Override
    public void loginViaSocialAccount(String email, String idToken, String service, boolean isPasswordRequired) {
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                LoginEmailPasswordFragment.newInstance(email, null, idToken, service, isPasswordRequired);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void loggedInViaSocialAccount(ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        mLoginAnalyticsListener.trackLoginSocialSuccess();
        loggedInAndFinish(oldSitesIds, doLoginUpdate);
    }

    @Override
    public void loginViaWpcomUsernameInstead() {
        jumpToUsernamePassword(null, null);
    }

    @Override
    public void showMagicLinkSentScreen(String email, boolean allowPassword) {
        LoginMagicLinkSentFragment loginMagicLinkSentFragment =
                LoginMagicLinkSentFragment.newInstance(email, allowPassword);
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG);
    }

    @Override
    public void showSignupMagicLink(String email) {
        boolean isEmailClientAvailable = WPActivityUtils.isEmailClientAvailable(this);
        AuthEmailPayloadScheme scheme = mViewModel.getMagicLinkScheme();
        SignupMagicLinkFragment signupMagicLinkFragment = SignupMagicLinkFragment.newInstance(email, mIsJetpackConnect,
                mJetpackConnectSource != null ? mJetpackConnectSource.toString() : null, isEmailClientAvailable,
                scheme);
        slideInFragment(signupMagicLinkFragment, true, SignupMagicLinkFragment.TAG);
    }

    @Override
    public void showSignupSocial(String email, String displayName, String idToken, String photoUrl, String service) {
        if (GoogleFragment.SERVICE_TYPE_GOOGLE.equals(service)) {
            addGoogleFragment(SignupGoogleFragment.newInstance(email, displayName, idToken, photoUrl),
                    SignupGoogleFragment.TAG);
        }
    }

    @Override
    public void openEmailClient(boolean isLogin) {
        mUnifiedLoginTracker.trackClick(Click.OPEN_EMAIL_CLIENT);
        if (WPActivityUtils.isEmailClientAvailable(this)) {
            if (isLogin) {
                mLoginAnalyticsListener.trackLoginMagicLinkOpenEmailClientClicked();
            } else {
                mLoginAnalyticsListener.trackSignupMagicLinkOpenEmailClientClicked();
            }

            WPActivityUtils.openEmailClientChooser(this, getString(R.string.login_select_email_client));
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found);
        }
    }

    @Override
    public void usePasswordInstead(String email) {
        mLoginAnalyticsListener.trackLoginMagicLinkExited();
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                LoginEmailPasswordFragment.newInstance(email, null, null, null, false);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void forgotPassword(String url) {
        mLoginAnalyticsListener.trackLoginForgotPasswordClicked();
        ActivityLauncher.openUrlExternal(this, url + FORGOT_PASSWORD_URL_SUFFIX);
    }

    @Override
    public void useMagicLinkInstead(String email, boolean verifyEmail) {
        showMagicLinkRequestScreen(email, verifyEmail, false, true);
    }

    @Override
    public void needs2fa(String email, String password) {
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstance(email, password);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void needs2fa(String email, String password, String userId, String webauthnNonce, String nonceAuthenticator,
                         String nonceBackup, String noncePush, List<String> supportedAuthTypes) {
        mLoginAnalyticsListener.trackLogin2faNeeded();
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstance(email, password, userId, webauthnNonce,
                nonceAuthenticator, nonceBackup, noncePush, supportedAuthTypes);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void needs2faSocial(String email, String userId, String nonceAuthenticator, String nonceBackup,
                               String nonceSms, String nonceWebauthn, List<String> authTypes) {
        mLoginAnalyticsListener.trackLoginSocial2faNeeded();
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstanceSocial(email, userId,
                nonceAuthenticator, nonceBackup,
                nonceSms, nonceWebauthn, authTypes);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void needs2faSocialConnect(String email, String password, String idToken, String service) {
        mLoginAnalyticsListener.trackLoginSocial2faNeeded();
        Login2FaFragment login2FaFragment =
                Login2FaFragment.newInstanceSocialConnect(email, password, idToken, service);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void loggedInViaPassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG);
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void gotWpcomSiteInfo(String siteAddress) {
        LoginEmailFragment loginEmailFragment = LoginEmailFragment.newInstance(siteAddress);
        slideInFragment(loginEmailFragment, true, LoginEmailFragment.TAG);
    }

    @Override
    public void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment =
                LoginUsernamePasswordFragment.newInstance(inputSiteAddress, endpointAddress, null, null, false);
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG);
    }

    @Override
    public void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager,
                                          final SelfSignedSSLCallback callback) {
        SelfSignedSSLUtils.showSSLWarningDialog(this, memorizingTrustManager, new SelfSignedSSLUtils.Callback() {
            @Override
            public void certificateTrusted() {
                callback.certificateTrusted();
            }
        });
    }

    private void viewHelp(Origin origin) {
        List<String> extraSupportTags = getLoginMode() == LoginMode.JETPACK_STATS ? Collections
                .singletonList(ZendeskExtraTags.connectingJetpack) : null;
        ActivityLauncher.viewHelp(this, origin, null, extraSupportTags);
    }

    @Override
    public void helpSiteAddress(String url) {
        viewHelp(Origin.LOGIN_SITE_ADDRESS);
    }

    @Override
    public void helpFindingSiteAddress(String username, SiteStore siteStore) {
        mUnifiedLoginTracker.trackClick(Click.HELP_FINDING_SITE_ADDRESS);
        if (!mBuildConfigWrapper.isJetpackApp()) {
            viewHelp(Origin.LOGIN_SITE_ADDRESS);
        } else {
            if (mContactSupportFeatureConfig.isEnabled()) {
                Intent intent = SupportWebViewActivity.createIntent(
                        this,
                        Origin.LOGIN_SITE_ADDRESS,
                        null,
                        null);
                startActivity(intent);
            } else {
                mZendeskHelper.createNewTicket(this, Origin.LOGIN_SITE_ADDRESS, null);
            }
        }
    }

    @Override
    public void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void helpEmailScreen(String email) {
        viewHelp(Origin.LOGIN_EMAIL);
    }

    @Override
    public void helpSignupEmailScreen(String email) {
        viewHelp(Origin.SIGNUP_EMAIL);
    }

    @Override
    public void helpSignupMagicLinkScreen(String email) {
        viewHelp(Origin.SIGNUP_MAGIC_LINK);
    }

    @Override
    public void helpSignupConfirmationScreen(String email) {
        viewHelp(Origin.SIGNUP_CONFIRMATION);
    }

    @Override
    public void helpSocialEmailScreen(String email) {
        viewHelp(Origin.LOGIN_SOCIAL);
    }

    @Override
    public void addGoogleLoginFragment(boolean isSignupFromLoginEnabled) {
        addGoogleFragment(LoginGoogleFragment.newInstance(isSignupFromLoginEnabled), LoginGoogleFragment.TAG);
    }

    @Override
    public void helpMagicLinkRequest(String email) {
        viewHelp(Origin.LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpMagicLinkSent(String email) {
        viewHelp(Origin.LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpEmailPasswordScreen(String email) {
        viewHelp(Origin.LOGIN_EMAIL_PASSWORD);
    }

    @Override
    public void help2FaScreen(String email) {
        viewHelp(Origin.LOGIN_2FA);
    }

    @Override
    public void startPostLoginServices() {
        // Get reader tags so they're available as soon as the Reader is accessed - done for
        // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
        // uses the application context since the activity is finished immediately below
        ReaderUpdateServiceStarter.startService(getApplicationContext(), EnumSet.of(ReaderUpdateLogic.UpdateTask.TAGS));

        // Start Notification service
        NotificationsUpdateServiceStarter.startService(getApplicationContext());
    }

    @Override
    public void helpUsernamePassword(String url, String username, boolean isWpcom) {
        viewHelp(Origin.LOGIN_USERNAME_PASSWORD);
    }

    // SmartLock

    @Override
    public void saveCredentialsInSmartLock(@Nullable final String username, @Nullable final String password,
                                           @NonNull final String displayName, @Nullable final Uri profilePicture) {
        LoginMode mode = getLoginMode();
        if (mode == LoginMode.SELFHOSTED_ONLY || mode == LoginMode.JETPACK_SELFHOSTED) {
            // bail if we are on the selfhosted flow since we haven't initialized SmartLock-for-Passwords for it.
            // Otherwise, logging in to WPCOM via the site-picker flow (for example) results in a crash.
            // See https://github.com/wordpress-mobile/WordPress-Android/issues/7182#issuecomment-362791364
            // There might be more circumstances that lead to this crash though. Not all crash reports seem to
            // originate from the site-picker.
            return;
        }

        if (mSmartLockHelper == null) {
            // log some data to help us debug https://github.com/wordpress-mobile/WordPress-Android/issues/7182
            final String loginModeStr = "LoginMode: " + (getLoginMode() != null ? getLoginMode().name() : "null");
            AppLog.w(AppLog.T.NUX, "Internal inconsistency error! mSmartLockHelper found null!" + loginModeStr);

            // bail
            return;
        }

        mSmartLockHelper.saveCredentialsInSmartLock(StringUtils.notNullStr(username), StringUtils.notNullStr(password),
                displayName, profilePicture);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(AppLog.T.NUX, "Connection result: " + connectionResult);
        mSmartLockHelperState = SmartLockHelperState.FINISHED;
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppLog.d(AppLog.T.NUX, "Google API client connected");

        switch (mSmartLockHelperState) {
            case NOT_TRIGGERED:
                // should not reach this state here!
                throw new RuntimeException("Internal inconsistency error!");
            case TRIGGER_FILL_IN_ON_CONNECT:
                mSmartLockHelperState = SmartLockHelperState.FINISHED;

                // force account chooser
                mSmartLockHelper.disableAutoSignIn();

                mSmartLockHelper.smartLockAutoFill(this);
                break;
            case FINISH_ON_CONNECT:
                mSmartLockHelperState = SmartLockHelperState.FINISHED;
                break;
            case FINISHED:
                // don't do anything special. We're reconnecting the GoogleApiClient on rotation.
                break;
        }
    }

    @Override
    public void onCredentialRetrieved(Credential credential) {
        mLoginAnalyticsListener.trackLoginAutofillCredentialsFilled();

        mSmartLockHelperState = SmartLockHelperState.FINISHED;

        final String username = credential.getId();
        final String password = credential.getPassword();
        jumpToUsernamePassword(username, password);
    }

    @Override
    public void onCredentialsUnavailable() {
        mSmartLockHelperState = SmartLockHelperState.FINISHED;
        if (mIsSmartLockTriggeredFromPrologue) {
            return;
        }
        startLogin();
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(AppLog.T.NUX, "Google API client connection suspended");
    }

    @Override
    public void showSignupToLoginMessage() {
        WPSnackbar.make(
                findViewById(R.id.main_view),
                R.string.signup_user_exists,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // GoogleListener

    @Override
    public void onGoogleEmailSelected(String email) {
        LoginEmailFragment loginEmailFragment =
                (LoginEmailFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        if (loginEmailFragment != null) {
            loginEmailFragment.setGoogleEmail(email);
        }
    }

    @Override
    public void onGoogleLoginFinished() {
        LoginEmailFragment loginEmailFragment =
                (LoginEmailFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        if (loginEmailFragment != null) {
            loginEmailFragment.finishLogin();
        }
    }

    @Override
    public void onGoogleSignupFinished(String name, String email, String photoUrl, String username) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_SUCCESS);
        if (mIsJetpackConnect) {
            ActivityLauncher.showSignupEpilogueForResult(this, name, email, photoUrl, username, false);
        } else {
            ActivityLauncher.showMainActivityAndSignupEpilogue(this, name, email, photoUrl, username);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onGoogleSignupError(String msg) {
        mUnifiedLoginTracker.trackFailure(msg);
        // Only show the error dialog if the activity is still active
        if (!getSupportFragmentManager().isStateSaved()) {
            BasicFragmentDialog dialog = new BasicFragmentDialog();
            dialog.initialize(GOOGLE_ERROR_DIALOG_TAG, getString(R.string.error),
                    msg,
                    getString(org.wordpress.android.login.R.string.login_error_button),
                    null,
                    null);
            dialog.show(getSupportFragmentManager(), GOOGLE_ERROR_DIALOG_TAG);
        } else {
            AppLog.d(T.MAIN, "'Google sign up failed' dialog not shown, because the activity wasn't visible.");
        }
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case GOOGLE_ERROR_DIALOG_TAG:
                // just dismiss the dialog
                break;
        }
    }

    @Override public AndroidInjector<Object> androidInjector() {
        return mDispatchingAndroidInjector;
    }

    @Override public void startOver() {
        // Not used in WordPress app
    }

    @Override
    public void showHelpFindingConnectedEmail() {
        // Not used in WordPress app
    }

    @Override
    public void gotConnectedSiteInfo(
            @NonNull String siteAddress,
            @Nullable String redirectUrl,
            boolean hasJetpack) {
        // Not used in WordPress app
    }

    @Override
    public void helpHandleDiscoveryError(
            String siteAddress,
            String endpointAddress,
            String username,
            String password,
            String userAvatarUrl,
            int errorMessage) {
        // Not used in WordPress app
    }

    @Override
    public void helpNoJetpackScreen(
            String siteAddress,
            String endpointAddress,
            String username,
            String password,
            String userAvatarUrl,
            Boolean checkJetpackAvailability) {
        // Not used in WordPress app
    }

    @Override
    public void loginViaSiteCredentials(String inputSiteAddress) {
        // Not used in WordPress app
    }

    @Override
    public void handleSiteAddressError(ConnectSiteInfoPayload siteInfo) {
        mViewModel.onHandleSiteAddressError(siteInfo);
    }

    public void handleNoJetpackSites() {
        // hide keyboard if you can
        hideKeyboard(this);
        mViewModel.onHandleNoJetpackSites();
    }


    private void showSiteAddressError(ShowSiteAddressError event) {
        LoginSiteCheckErrorFragment fragment = LoginSiteCheckErrorFragment.Companion.newInstance(event.getUrl());
        slideInFragment(fragment, true, LoginSiteCheckErrorFragment.TAG);
    }

    private void showNoJetpackSites() {
        LoginNoSitesFragment fragment = LoginNoSitesFragment.Companion.newInstance();
        slideInFragment(fragment, false, LoginNoSitesFragment.TAG);
    }
}
