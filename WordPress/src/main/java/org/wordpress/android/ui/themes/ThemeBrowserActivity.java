package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnScrollChangeListener;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.OnCurrentThemeFetched;
import org.wordpress.android.fluxc.store.ThemeStore.OnSiteThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeActivated;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeInstalled;
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;
import org.wordpress.android.models.JetpackPoweredScreen;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JetpackBrandingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.extensions.CompatExtensionsKt;
import org.wordpress.android.widgets.HeaderGridView;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ThemeBrowserActivity extends BaseAppCompatActivity implements ThemeBrowserFragmentCallback,
        ScrollableViewInitializedListener {
    public static final int ACTIVATE_THEME = 1;
    public static final String THEME_ID = "theme_id";

    // refresh WP.com themes every 3 days
    private static final long WP_COM_THEMES_SYNC_TIMEOUT = 1000 * 60 * 60 * 24 * 3;

    private ThemeBrowserFragment mThemeBrowserFragment;
    private ThemeModel mCurrentTheme;
    private boolean mIsFetchingInstalledThemes;
    private SiteModel mSite;

    @Inject ThemeStore mThemeStore;
    @Inject Dispatcher mDispatcher;
    @Inject JetpackBrandingUtils mJetpackBrandingUtils;
    @Inject UiHelpers mUiHelpers;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDispatcher.register(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.theme_browser_activity);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        if (savedInstanceState == null) {
            addBrowserFragment();
            fetchInstalledThemesIfJetpackSite();
            fetchWpComThemesIfSyncTimedOut(false);
        } else {
            mThemeBrowserFragment =
                    (ThemeBrowserFragment) getSupportFragmentManager().findFragmentByTag(ThemeBrowserFragment.TAG);
        }

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.THEMES);
        fetchCurrentTheme();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVATE_THEME && resultCode == RESULT_OK && data != null) {
            String themeId = data.getStringExtra(THEME_ID);
            if (!TextUtils.isEmpty(themeId)) {
                activateTheme(themeId);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.unregister(this);
    }

    @Override
    public void onActivateSelected(@NonNull String themeId) {
        activateTheme(themeId);
    }

    @Override
    public void onTryAndCustomizeSelected(@Nullable String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.PREVIEW);
    }

    @Override
    public void onViewSelected(@NonNull String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.DEMO);
    }

    @Override
    public void onDetailsSelected(@Nullable String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.DETAILS);
    }

    @Override
    public void onSupportSelected(@Nullable String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.SUPPORT);
    }

    @Override
    public void onSwipeToRefresh() {
        fetchInstalledThemesIfJetpackSite();
        fetchWpComThemesIfSyncTimedOut(true);
    }

    @Override
    public void onScrollableViewInitialized(int containerId) {
        if (mJetpackBrandingUtils.shouldShowJetpackBrandingForPhaseTwo()) {
            final JetpackPoweredScreen screen = JetpackPoweredScreen.WithDynamicText.THEMES;
            findViewById(R.id.root_view).post(() -> {
                View jetpackBannerView = findViewById(R.id.jetpack_banner);
                TextView jetpackBannerTextView = jetpackBannerView.findViewById(R.id.jetpack_banner_text);
                jetpackBannerTextView.setText(
                        mUiHelpers.getTextOfUiString(
                                this,
                                mJetpackBrandingUtils.getBrandingTextForScreen(screen))
                );

                HeaderGridView scrollableView = findViewById(containerId);

                showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView);
                initJetpackBannerAnimation(jetpackBannerView, scrollableView);

                if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBannerView.setOnClickListener(v -> {
                        mJetpackBrandingUtils.trackBannerTapped(screen);
                        new JetpackPoweredBottomSheetFragment()
                                .show(getSupportFragmentManager(), JetpackPoweredBottomSheetFragment.TAG);
                    });
                }
            });
        }
    }

    private void showJetpackBannerIfScrolledToTop(View banner, HeaderGridView scrollableView) {
        banner.setVisibility(View.VISIBLE);

        boolean isEmpty = scrollableView.getAdapter().isEmpty();
        int scrollOffset = scrollableView.computeVerticalScrollOffset();

        float jetpackBannerHeight = banner.getResources().getDimension(R.dimen.jetpack_banner_height);

        float translationY = scrollOffset == 0 || isEmpty ? 0 : jetpackBannerHeight;
        banner.setTranslationY(translationY);
    }

    private void initJetpackBannerAnimation(View banner, HeaderGridView scrollableView) {
        scrollableView.setOnScrollChangeListener(new OnScrollChangeListener() {
            private boolean mIsScrollAtTop = true;

            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int scrollOffset = scrollableView.computeVerticalScrollOffset();

                if (scrollOffset == 0 && !mIsScrollAtTop) {
                    // Show the banner by moving up
                    mIsScrollAtTop = true;
                    banner.animate().translationY(0f).start();
                } else if (scrollOffset != 0 && mIsScrollAtTop) {
                    // Hide the banner by moving down
                    mIsScrollAtTop = false;
                    float jetpackBannerHeight = banner.getResources().getDimension(R.dimen.jetpack_banner_height);
                    banner.animate().translationY(jetpackBannerHeight).start();
                }
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWpComThemesChanged(OnWpComThemesChanged event) {
        // always unset refreshing status to remove progress indicator
        if (mThemeBrowserFragment != null) {
            mThemeBrowserFragment.setRefreshing(false);
            mThemeBrowserFragment.refreshView();
        }

        if (event.isError()) {
            AppLog.e(T.THEMES, "Error fetching themes: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);
        } else {
            AppLog.d(T.THEMES, "WordPress.com Theme fetch successful!");
        }
        AppPrefs.setLastWpComThemeSync(System.currentTimeMillis());
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteThemesChanged(OnSiteThemesChanged event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
            // always unset refreshing status to remove progress indicator
            if (mThemeBrowserFragment != null) {
                mThemeBrowserFragment.setRefreshing(false);
                mThemeBrowserFragment.refreshView();
            }

            mIsFetchingInstalledThemes = false;

            if (event.isError()) {
                AppLog.e(T.THEMES, "Error fetching themes: " + event.error.message);
                ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);
            } else {
                AppLog.d(T.THEMES, "Installed themes fetch successful!");
            }
        } else if (event.origin == ThemeAction.REMOVE_SITE_THEMES) {
            // Since this is a logout event, we don't need to do anything
            AppLog.d(T.THEMES, "Site themes removed for site: " + event.site.getDisplayName());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(OnCurrentThemeFetched event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error fetching current theme: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);

            // set the new current theme to update header
            if (mCurrentTheme != null && mThemeBrowserFragment != null) {
                if (mThemeBrowserFragment.getCurrentThemeTextView() != null) {
                    mThemeBrowserFragment.getCurrentThemeTextView().setText(mCurrentTheme.getName());
                    mThemeBrowserFragment.setCurrentThemeId(mCurrentTheme.getThemeId());
                }
            }
        } else {
            AppLog.d(T.THEMES, "Current Theme fetch successful!");
            mCurrentTheme = mThemeStore.getActiveThemeForSite(event.site);
            AppLog.d(T.THEMES, "Current theme is " + (mCurrentTheme == null ? "(null)" : mCurrentTheme.getName()));
            updateCurrentThemeView();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeInstalled(OnThemeInstalled event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error installing theme: " + event.error.message);
        } else {
            AppLog.d(T.THEMES, "Theme installation successful! Installed theme: " + event.theme.getName());
            activateTheme(event.theme.getThemeId());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(OnThemeActivated event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error activating theme: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_activation_error, ToastUtils.Duration.SHORT);
        } else {
            AppLog.d(T.THEMES, "Theme activation successful! New theme: " + event.theme.getName());

            mCurrentTheme = mThemeStore.getActiveThemeForSite(event.site);
            if (mCurrentTheme == null) {
                AppLog.e(T.THEMES, "NOT A CRASH: OnThemeActivated event is ignored as `getActiveThemeForSite` "
                                   + "returned null.");
                return;
            }
            updateCurrentThemeView();

            Map<String, Object> themeProperties = new HashMap<>();
            themeProperties.put(THEME_ID, mCurrentTheme.getThemeId());
            AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_CHANGED_THEME, mSite, themeProperties);

            if (!isFinishing()) {
                showAlertDialogOnNewSettingNewTheme(mCurrentTheme);
            }
        }
    }

    private void updateCurrentThemeView() {
        if (mCurrentTheme != null && mThemeBrowserFragment != null
            && mThemeBrowserFragment.getCurrentThemeTextView() != null) {
            String text =
                    TextUtils.isEmpty(mCurrentTheme.getName()) ? getString(R.string.unknown) : mCurrentTheme.getName();
            mThemeBrowserFragment.getCurrentThemeTextView().setText(text);
            mThemeBrowserFragment.setCurrentThemeId(mCurrentTheme.getThemeId());
        }
    }

    private void fetchCurrentTheme() {
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(mSite));
    }

    private void fetchWpComThemesIfSyncTimedOut(boolean force) {
        long currentTime = System.currentTimeMillis();
        if (force || currentTime - AppPrefs.getLastWpComThemeSync() > WP_COM_THEMES_SYNC_TIMEOUT) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction(new FetchWPComThemesPayload()));
        }
    }

    private void fetchInstalledThemesIfJetpackSite() {
        if (mSite.isJetpackConnected() && mSite.isUsingWpComRestApi() && !mIsFetchingInstalledThemes) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(mSite));
            mIsFetchingInstalledThemes = true;
        }
    }

    private void activateTheme(@NonNull String themeId) {
        if (!mSite.isUsingWpComRestApi()) {
            AppLog.i(T.THEMES, "Theme activation requires a site using WP.com REST API. Aborting request.");
            return;
        }

        ThemeModel theme = mThemeStore.getInstalledThemeByThemeId(mSite, themeId);
        if (theme == null) {
            theme = mThemeStore.getWpComThemeByThemeId(themeId);
            if (theme == null) {
                AppLog.w(T.THEMES, "Theme unavailable to activate. Fetch it and try again.");
                return;
            }

            if (mSite.isJetpackConnected()) {
                // first install the theme, then activate it
                mDispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(new SiteThemePayload(mSite, theme)));
                return;
            }
        }

        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(new SiteThemePayload(mSite, theme)));
    }

    private void addBrowserFragment() {
        mThemeBrowserFragment = ThemeBrowserFragment.newInstance(mSite);
        getSupportFragmentManager().beginTransaction()
                                   .add(R.id.fragment_container, mThemeBrowserFragment, ThemeBrowserFragment.TAG)
                                   .commit();
    }

    private void showAlertDialogOnNewSettingNewTheme(ThemeModel newTheme) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(this);

        String thanksMessage = String.format(getString(R.string.theme_prompt), newTheme.getName());
        if (!TextUtils.isEmpty(newTheme.getAuthorName())) {
            String append = String.format(getString(R.string.theme_by_author_prompt_append), newTheme.getAuthorName());
            thanksMessage = thanksMessage + " " + append;
        }

        dialogBuilder.setMessage(thanksMessage);
        dialogBuilder.setNegativeButton(R.string.theme_done, null);
        dialogBuilder.setPositiveButton(R.string.theme_manage_site, (dialog, which) -> finish());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void startWebActivity(String themeId, ThemeWebActivity.ThemeWebActivityType type) {
        ThemeModel theme =
                TextUtils.isEmpty(themeId) ? null : mThemeStore.getWpComThemeByThemeId(themeId.replace("-wpcom", ""));
        if (theme == null) {
            theme = mThemeStore.getInstalledThemeByThemeId(mSite, themeId);
            if (theme == null) {
                ToastUtils.showToast(this, R.string.could_not_load_theme);
                return;
            }
        }

        Map<String, Object> themeProperties = new HashMap<>();
        themeProperties.put(THEME_ID, themeId);
        theme.setActive(isActiveThemeForSite(theme.getThemeId()));

        switch (type) {
            case PREVIEW:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_PREVIEWED_SITE, mSite, themeProperties);
                break;
            case DEMO:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_DEMO_ACCESSED, mSite, themeProperties);
                break;
            case DETAILS:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_DETAILS_ACCESSED, mSite, themeProperties);
                break;
            case SUPPORT:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_SUPPORT_ACCESSED, mSite, themeProperties);
                break;
        }
        ThemeWebActivity.openTheme(this, mSite, theme, type);
    }

    private boolean isActiveThemeForSite(@NonNull String themeId) {
        final ThemeModel storedActiveTheme = mThemeStore.getActiveThemeForSite(mSite);
        return storedActiveTheme != null && themeId.equals(storedActiveTheme.getThemeId().replace("-wpcom", ""));
    }
}
