package org.wordpress.android.ui.media;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.push.NotificationType;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityNavigator;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.media.MediaGridFragment.MediaFilter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.photopicker.MediaPickerConstants;
import org.wordpress.android.ui.photopicker.MediaPickerLauncher;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.MediaUtilsWrapper;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.widgets.AppReviewManager;
import org.wordpress.android.widgets.QuickStartFocusPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA;
import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;
import static org.wordpress.android.util.ToastUtils.Duration.LONG;

/**
 * The main activity in which the user can browse their media.
 */
public class MediaBrowserActivity extends BaseAppCompatActivity implements MediaGridListener,
        OnQueryTextListener, OnActionExpandListener,
        WPMediaUtils.LaunchCameraCallback, WpAppNotifierHandler.NotifierListener {
    public static final String ARG_BROWSER_TYPE = "media_browser_type";
    public static final String ARG_FILTER = "filter";
    public static final String ARG_LAUNCH_PHOTO_PICKER = "launch_photo_picker";
    public static final String RESULT_IDS = "result_ids";

    private static final String SAVED_QUERY = "SAVED_QUERY";
    private static final String BUNDLE_MEDIA_CAPTURE_PATH = "mediaCapturePath";
    private static final String SHOW_AUDIO_TAB = "showAudioTab";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject SiteStore mSiteStore;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject MediaPickerLauncher mMediaPickerLauncher;
    @Inject MediaUtilsWrapper mMediaUtilsWrapper;
    @Inject QuickStartRepository mQuickStartRepository;
    @Inject SelectedSiteRepository mSelectedSiteRepository;
    @Inject JetpackFeatureRemovalPhaseHelper mJetpackFeatureRemovalPhaseHelper;
    @Inject ActivityNavigator mActivityNavigator;
    @Inject WpAppNotifierHandler mWpAppNotifierHandler;

    private SiteModel mSite;

    private MediaGridFragment mMediaGridFragment;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Menu mMenu;
    private TabLayout mTabLayout;
    private RelativeLayout mQuotaBar;
    private TextView mQuotaText;

    private MediaDeleteService.MediaDeleteBinder mDeleteService;
    private boolean mDeleteServiceBound;

    private String mQuery;
    private String mMediaCapturePath;
    private MediaBrowserType mBrowserType;
    private AddMenuItem mLastAddMediaItemClicked;
    private MenuItem menuNewMediaItem;
    private QuickStartFocusPoint mMenuNewMediaQuickStartFocusPoint;

    private boolean mShowAudioTab;

    private boolean mLaunchPhotoPicker = false;

    private enum AddMenuItem {
        ITEM_CAPTURE_PHOTO,
        ITEM_CAPTURE_VIDEO,
        ITEM_CHOOSE_FILE,
        ITEM_CHOOSE_STOCK_MEDIA,
        ITEM_CHOOSE_GIF
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mBrowserType = (MediaBrowserType) getIntent().getSerializableExtra(ARG_BROWSER_TYPE);
            mShowAudioTab = mMediaStore.getSiteAudio(mSite).size() > 0;
            mLaunchPhotoPicker = getIntent().getBooleanExtra(ARG_LAUNCH_PHOTO_PICKER, false);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(ARG_BROWSER_TYPE);
            mMediaCapturePath = savedInstanceState.getString(BUNDLE_MEDIA_CAPTURE_PATH);
            mQuery = savedInstanceState.getString(SAVED_QUERY);
            mShowAudioTab = savedInstanceState.getBoolean(SHOW_AUDIO_TAB);
            mLaunchPhotoPicker = savedInstanceState.getBoolean(ARG_LAUNCH_PHOTO_PICKER);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        if (mBrowserType == null) {
            // default to browser mode if missing type
            AppLog.w(AppLog.T.MEDIA, "MediaBrowserType is null. Defaulting to MediaBrowserType.BROWSER mode.");
            mBrowserType = MediaBrowserType.BROWSER;
        }

        setContentView(R.layout.media_browser_activity);

        setSupportActionBar(findViewById(R.id.toolbar_main));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.wp_media_title);
        }

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);

        // if media was shared add it to the library
        handleSharedMedia();

        mTabLayout = findViewById(R.id.tab_layout);

        setupTabs();

        MediaFilter filter;
        if (mBrowserType.isSingleImagePicker()) {
            filter = MediaFilter.FILTER_IMAGES;
        } else if (mBrowserType == MediaBrowserType.GUTENBERG_IMAGE_PICKER) {
            filter = MediaFilter.FILTER_IMAGES;
        } else if (mBrowserType == MediaBrowserType.GUTENBERG_VIDEO_PICKER) {
            filter = MediaFilter.FILTER_VIDEOS;
        } else if (mBrowserType == MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER) {
            filter = MediaFilter.FILTER_AUDIO;
        } else if (savedInstanceState != null) {
            filter = (MediaFilter) savedInstanceState.getSerializable(ARG_FILTER);
        } else {
            filter = MediaFilter.FILTER_ALL;
        }

        mMediaGridFragment = (MediaGridFragment) fm.findFragmentByTag(MediaGridFragment.TAG);
        if (mMediaGridFragment == null) {
            mMediaGridFragment = MediaGridFragment.newInstance(mSite, mBrowserType, filter);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.media_browser_container, mMediaGridFragment, MediaGridFragment.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else {
            setFilter(filter);
        }

        mQuotaBar = findViewById(R.id.quota_bar);
        mQuotaText = findViewById(R.id.quota_text);

        showQuota(true);

        if (mLaunchPhotoPicker) {
            doAddMediaItemClicked(AddMenuItem.ITEM_CHOOSE_FILE);
            mLaunchPhotoPicker = false;
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
            NotificationType notificationType =
                    (NotificationType) intent.getSerializableExtra(ARG_NOTIFICATION_TYPE);
            if (notificationType != null) {
                mSystemNotificationsTracker.trackTappedNotification(notificationType);
            }
        }
    }

    private void formatQuotaDiskSpace() {
        final String[] units = new String[] {
                getString(R.string.file_size_in_bytes),
                getString(R.string.file_size_in_kilobytes),
                getString(R.string.file_size_in_megabytes),
                getString(R.string.file_size_in_gigabytes),
                getString(R.string.file_size_in_terabytes)
        };

        String quota;

        if (mSite.getPlanId() == PlansConstants.BUSINESS_PLAN_ID) {
            String space = FormatUtils.formatFileSize(mSite.getSpaceUsed(), units);
            quota = String.format(getString(R.string.site_settings_quota_space_unlimited), space);
        } else {
            String percentage = FormatUtils.formatPercentageLimit100(mSite.getSpacePercentUsed() / 100, true);

            String space = FormatUtils.formatFileSize(mSite.getSpaceAllowed(), units);
            quota = String.format(getString(R.string.site_settings_quota_space_value), percentage, space);
        }

        mQuotaText.setText(getString(R.string.media_space_used, quota));
        mQuotaText.setTextColor(
                getResources().getColor(mSite.getSpacePercentUsed() > 90 ? R.color.error_50 : R.color.neutral));
    }

    private void showQuota(boolean show) {
        if (!mBrowserType.canFilter()) {
            mQuotaBar.setVisibility(View.GONE);
        } else if (show && mSite != null && mSite.hasDiskSpaceQuotaInformation()) {
            mQuotaBar.setVisibility(View.VISIBLE);
            formatQuotaDiskSpace();
        } else if (!show) {
            mQuotaBar.setVisibility(View.GONE);
        }
    }

    @Override public void onRequestedWithInvalidAuthentication(@NonNull String authenticationUrl) {
        showApplicationPasswordReauthenticateDialog(authenticationUrl);
    }

    private void showApplicationPasswordReauthenticateDialog(@NonNull String authenticationUrl) {
        mActivityNavigator.navigateToApplicationPasswordReauthentication(this, authenticationUrl);
    }

    public MediaDeleteService getMediaDeleteService() {
        if (mDeleteService == null) {
            return null;
        }
        return mDeleteService.getService();
    }

    /*
     * only show tabs when the user can filter the media by type
     */
    private boolean shouldShowTabs() {
        return mBrowserType.canFilter();
    }

    private void enableTabs(boolean enable) {
        if (!shouldShowTabs()) return;

        if (enable && mTabLayout.getVisibility() != View.VISIBLE) {
            mTabLayout.setVisibility(View.VISIBLE);
        } else if (!enable && mTabLayout.getVisibility() == View.VISIBLE) {
            mTabLayout.setVisibility(View.GONE);
        }
    }

    private void setupTabs() {
        if (shouldShowTabs()) {
            mTabLayout.removeAllTabs();
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_all)); // FILTER_ALL
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_images)); // FILTER_IMAGES
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_documents)); // FILTER_DOCUMENTS
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_videos)); // FILTER_VIDEOS
            if (mShowAudioTab) {
                mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_audio)); // FILTER_AUDIO
            }

            mTabLayout.clearOnTabSelectedListeners();
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    setFilter(getFilterForPosition(tab.getPosition()));
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // noop
                }
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    setFilter(getFilterForPosition(tab.getPosition()));
                }
            });

            // tabMode is set to scrollable in layout, set to fixed if there's enough space to show them all
            mTabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mTabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if (mTabLayout.getChildCount() > 0) {
                        int tabLayoutWidth = 0;
                        LinearLayout tabFirstChild = (LinearLayout) mTabLayout.getChildAt(0);
                        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                            LinearLayout tabView = (LinearLayout) (tabFirstChild.getChildAt(i));
                            tabLayoutWidth += (tabView.getMeasuredWidth() + ViewCompat.getPaddingStart(tabView)
                                               + ViewCompat.getPaddingEnd(tabView));
                        }

                        int displayWidth = DisplayUtils.getWindowPixelWidth(MediaBrowserActivity.this);
                        if (tabLayoutWidth < displayWidth) {
                            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
                            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                        }
                    }
                }
            });
        } else {
            mTabLayout.setVisibility(View.GONE);
        }
    }

    private void refreshTabs() {
        boolean hasAudio = mMediaStore.getSiteAudio(mSite).size() > 0;
        if (mShowAudioTab != hasAudio) {
            mShowAudioTab = hasAudio;
            setupTabs();
        }
    }

    private int getPositionForFilter(@NonNull MediaFilter filter) {
        return filter.getValue();
    }

    private MediaFilter getFilterForPosition(int position) {
        for (MediaFilter filter : MediaFilter.values()) {
            if (filter.getValue() == position) {
                return filter;
            }
        }
        return MediaFilter.FILTER_ALL;
    }

    private void setFilter(@NonNull MediaFilter filter) {
        int position = getPositionForFilter(filter);
        if (shouldShowTabs()
                && mTabLayout != null
                && mTabLayout.getSelectedTabPosition() != position) {
            mTabLayout.setScrollPosition(position, 0f, true);
        }

        if (mMediaGridFragment != null
                && (mMediaGridFragment.getFilter() != filter || mMediaGridFragment.isEmpty())) {
            mMediaGridFragment.setFilter(filter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mWpAppNotifierHandler.addListener(this);

        if (Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(
                    mReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                    RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(mReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSearchMenuItem != null) {
            String tempQuery = mQuery;
            MenuItemCompat.collapseActionView(mSearchMenuItem);
            mQuery = tempQuery;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService(null);
        ActivityId.trackLastActivity(ActivityId.MEDIA);
    }

    @Override
    public void onStop() {
        mWpAppNotifierHandler.removeListener(this);
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindDeleteService();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_QUERY, mQuery);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(ARG_BROWSER_TYPE, mBrowserType);
        outState.putBoolean(SHOW_AUDIO_TAB, mShowAudioTab);
        if (mMediaGridFragment != null) {
            outState.putSerializable(ARG_FILTER, mMediaGridFragment.getFilter());
        }
        if (!TextUtils.isEmpty(mMediaCapturePath)) {
            outState.putString(BUNDLE_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
        outState.putBoolean(ARG_LAUNCH_PHOTO_PICKER, mLaunchPhotoPicker);
    }

    private void getMediaFromDeviceAndTrack(Uri videoUri, int requestCode) {
        final String mimeType = getContentResolver().getType(videoUri);

        if (mMediaUtilsWrapper.isProhibitedVideoDuration(this, mSite, videoUri)) {
            ToastUtils.showToast(this, R.string.error_media_video_duration_exceeds_limit, LONG);
        } else {
            fetchMediaAndDoNext(videoUri, requestCode, mimeType);
        }
    }

    private void fetchMediaAndDoNext(Uri imageUri, int requestCode, String mimeType) {
        WPMediaUtils.fetchMediaAndDoNext(this, imageUri,
                uri -> queueFileForUpload(getOptimizedPictureIfNecessary(uri), mimeType));
        trackAddMediaFromDeviceEvents(
                false,
                requestCode == RequestCodes.VIDEO_LIBRARY,
                imageUri
        );
    }

    private void checkRecordedVideoDurationBeforeUploadAndTrack(Uri uri) {
        if (mMediaUtilsWrapper.isProhibitedVideoDuration(this, mSite, uri)) {
            ToastUtils.showToast(this, R.string.error_media_video_duration_exceeds_limit, LONG);
        } else {
            queueFileForUpload(uri, getContentResolver().getType(uri));
        }

        trackAddMediaFromDeviceEvents(true, true, uri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.PICTURE_LIBRARY:
            case RequestCodes.VIDEO_LIBRARY:
            case RequestCodes.AUDIO_LIBRARY:
            case RequestCodes.FILE_LIBRARY:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handlePickerResult(data, resultCode);
                }
                break;
            case RequestCodes.TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    addLastTakenPicture();
                }
                break;
            case RequestCodes.TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    checkRecordedVideoDurationBeforeUploadAndTrack(uri);
                }
                break;
            case RequestCodes.MEDIA_SETTINGS:
                if (resultCode == MediaSettingsActivity.RESULT_MEDIA_DELETED) {
                    reloadMediaGrid();
                    refreshTabs();
                }
                break;
            case RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT:
                if (resultCode == RESULT_OK) {
                    reloadMediaGrid();
                }
                break;
            case RequestCodes.GIF_PICKER_SINGLE_SELECT:
            case RequestCodes.GIF_PICKER_MULTI_SELECT:
                if (resultCode == RESULT_OK
                    && data.hasExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS)) {
                    int[] mediaLocalIds = data.getIntArrayExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS);

                    ArrayList<MediaModel> mediaModels = new ArrayList<>();
                    for (int localId : mediaLocalIds) {
                        mediaModels.add(mMediaStore.getMediaWithLocalId(localId));
                    }

                    addMediaToUploadService(mediaModels);
                }
                break;
        }
    }

    private void addLastTakenPicture() {
        WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
        Uri uri = getOptimizedPictureIfNecessary(Uri.parse(mMediaCapturePath));
        mMediaCapturePath = null;
        queueFileForUpload(uri, getContentResolver().getType(uri));
        trackAddMediaFromDeviceEvents(true, false, uri);
    }

    private void handlePickerResult(Intent data, int requestCode) {
        if (data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)) {
            List<Uri> uris = convertStringArrayIntoUrisList(
                    data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS));
            for (Uri uri : uris) {
                getMediaFromDeviceAndTrack(uri, requestCode);
            }
        }
    }

    private List<Uri> convertStringArrayIntoUrisList(String[] stringArray) {
        List<Uri> uris = new ArrayList<>(stringArray.length);
        for (String stringUri : stringArray) {
            uris.add(Uri.parse(stringUri));
        }
        return uris;
    }

    /**
     * Analytics about new media
     *
     * @param isNewMedia Whether is a fresh (just taken) photo/video or not
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    private void trackAddMediaFromDeviceEvents(boolean isNewMedia, boolean isVideo, Uri uri) {
        if (uri == null) {
            AppLog.e(AppLog.T.MEDIA, "Cannot track new media event if mediaURI is null!!");
            return;
        }

        Map<String, Object> properties = AnalyticsUtils.getMediaProperties(this, isVideo, uri, null);
        properties.put("via", isNewMedia ? "device_camera" : "device_library");

        if (isVideo) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_VIDEO, properties);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_PHOTO, properties);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, results, true);

        if (allGranted && requestCode == WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE) {
            doAddMediaItemClicked(mLastAddMediaItemClicked);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.media_browser, menu);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(this);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        menuNewMediaItem = menu.findItem(R.id.menu_new_media);
        mMenuNewMediaQuickStartFocusPoint = menuNewMediaItem.getActionView()
                                                            .findViewById(R.id.menu_add_media_quick_start_focus_point);

        menuNewMediaItem.getActionView().setOnClickListener(v -> {
            showAddMediaPopup();
            completeUploadMediaQuickStartTask();
            updateMenuNewMediaQuickStartFocusPoint(false);
        });

        // open search bar if we were searching for something before
        if (!TextUtils.isEmpty(mQuery) && mMediaGridFragment != null && mMediaGridFragment.isVisible()) {
            String tempQuery = mQuery; // temporary hold onto query
            MenuItemCompat.expandActionView(mSearchMenuItem); // this will reset mQuery
            onQueryTextSubmit(tempQuery);
            mSearchView.setQuery(mQuery, true);
        }

        // hide "add media" if the user doesn't have upload permission or this is a multiselect picker
        if (mBrowserType.canMultiselect()
            || !WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            menuNewMediaItem.setVisible(false);
            mMediaGridFragment.showActionableEmptyViewButton(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getOnBackPressedDispatcher().onBackPressed();
                return true;
            case R.id.menu_new_media:
                // Do Nothing (handled in action view click listener)
                return true;
            case R.id.menu_search:
                mSearchMenuItem = item;
                mSearchMenuItem.setOnActionExpandListener(this);
                mSearchMenuItem.expandActionView();

                mSearchView = (SearchView) item.getActionView();
                mSearchView.setOnQueryTextListener(this);

                // load last saved query
                if (!TextUtils.isEmpty(mQuery)) {
                    onQueryTextSubmit(mQuery);
                    mSearchView.setQuery(mQuery, true);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
        mMenu.findItem(R.id.menu_new_media).setVisible(false);
        mMediaGridFragment.showActionableEmptyViewButton(false);

        enableTabs(false);
        showQuota(false);

        // load last search query
        if (!TextUtils.isEmpty(mQuery)) {
            onQueryTextChange(mQuery);
        }

        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
        mMenu.findItem(R.id.menu_new_media).setVisible(true);
        mMediaGridFragment.showActionableEmptyViewButton(true);
        invalidateOptionsMenu();

        enableTabs(true);
        showQuota(true);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.search(query);
        }

        mQuery = query;
        mSearchView.clearFocus();

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.search(newText);
        }

        mQuery = newText;

        return true;
    }

    @Override
    public void onMediaItemSelected(int localMediaId, boolean isLongClick) {
        MediaModel media = mMediaStore.getMediaWithLocalId(localMediaId);
        if (media == null) {
            AppLog.w(AppLog.T.MEDIA, "Media browser > unable to load localMediaId = " + localMediaId);
            ToastUtils.showToast(this, R.string.error_media_load);
            return;
        }

        // do nothing for failed uploads
        if (MediaUploadState.fromString(media.getUploadState()) == MediaUploadState.FAILED) {
            return;
        }

        // show detail view when tapped if we're browsing media, when used as a picker show detail
        // when long tapped (to mimic native photo picker)
        if (mBrowserType.isBrowser() && !isLongClick
            || mBrowserType.isPicker() && isLongClick) {
            showMediaSettings(media);
        } else if ((mBrowserType.isSingleImagePicker() || mBrowserType.isSingleMediaPicker() || mBrowserType
                .isSingleFilePicker() || mBrowserType
                            .isSingleAudioFilePicker()) && !isLongClick) {
            // if we're picking a single media item, we're done
            Intent intent = new Intent();
            ArrayList<Long> remoteMediaIds = new ArrayList<>();
            remoteMediaIds.add(media.getMediaId());
            intent.putExtra(RESULT_IDS, ListUtils.toLongArray(remoteMediaIds));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onMediaRequestRetry(int localMediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(localMediaId);
        if (media != null) {
            addMediaToUploadService(media);
        } else {
            ToastUtils.showToast(this, R.string.error_media_not_found);
        }
    }

    @Override
    public void onMediaRequestDelete(int localMediaId) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(localMediaId);
        deleteMedia(ids);
    }

    private void showMediaSettings(@NonNull MediaModel media) {
        List<MediaModel> mediaList = mMediaGridFragment.getFilteredMedia();
        ArrayList<String> idList = new ArrayList<>();
        for (MediaModel thisMedia : mediaList) {
            idList.add(Integer.toString(thisMedia.getId()));
        }
        MediaSettingsActivity.showForResult(this, mSite, media, idList);
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }

    @Override public void onCameraError(String errorMessage) {
        ToastUtils.showToast(this, errorMessage, LONG);
    }

    private void showMediaToastError(@StringRes int message, @Nullable String messageDetail) {
        if (isFinishing()) {
            return;
        }
        String errorMessage = getString(message);
        if (!TextUtils.isEmpty(messageDetail)) {
            errorMessage += ". " + messageDetail;
        }
        ToastUtils.showToast(this, errorMessage, LONG);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        AppLog.d(AppLog.T.MEDIA, "MediaBrowser onMediaChanged > " + event.cause);

        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "Received onMediaChanged error: " + event.error.type
                    + " - " + event.error.message);
            showMediaToastError(R.string.media_generic_error, event.error.message);
            return;
        }

        switch (event.cause) {
            case DELETE_MEDIA:
                // If the media was deleted, remove it from multi select if it was selected
                for (MediaModel mediaModel : event.mediaList) {
                    int localMediaId = mediaModel.getId();
                    mMediaGridFragment.removeFromMultiSelect(localMediaId);
                }
                break;
        }

        if (event.mediaList.size() == 1) {
            updateMediaGridItem(event.mediaList.get(0), true);
        } else {
            reloadMediaGrid();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSite));

        if (event.media != null) {
            updateMediaGridItem(event.media, event.isError());
        } else {
            reloadMediaGrid();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            return;
        }
        refreshTabs();
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        enableTabs(false);
        showQuota(false);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        enableTabs(true);
        showQuota(true);
    }

    // TODO: in a future PR this and startMediaDeleteService() can be simplified since multiselect delete was dropped
    private void deleteMedia(final ArrayList<Integer> ids) {
        final ArrayList<MediaModel> mediaToDelete = new ArrayList<>();
        int processedItemCount = 0;

        for (int currentId : ids) {
            MediaModel mediaModel = mMediaStore.getMediaWithLocalId(currentId);
            if (mediaModel == null) {
                continue;
            }

            // if uploading, first issue a cancel upload command
            if (UploadService.isPendingOrInProgressMediaUpload(mediaModel)) {
                CancelMediaPayload payload = new CancelMediaPayload(mSite, mediaModel, false);
                mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
            }

            if (mediaModel.getUploadState() != null
                    && MediaUtils.isLocalFile(mediaModel.getUploadState().toLowerCase(Locale.ROOT))) {
                mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel));
            } else {
                mediaToDelete.add(mediaModel);
                mediaModel.setUploadState(MediaUploadState.DELETING);
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));
            }
            processedItemCount++;
        }

        if (processedItemCount != ids.size()) {
            ToastUtils.showToast(this, R.string.cannot_delete_multi_media_items, LONG);
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        if (!mediaToDelete.isEmpty()) {
            startMediaDeleteService(mediaToDelete);
        }
    }

    private void uploadList(List<Uri> uriList) {
        if (uriList == null || uriList.size() == 0) {
            return;
        }
        for (Uri uri : uriList) {
            if (uri != null) {
                WPMediaUtils.fetchMediaAndDoNext(this, uri,
                        downloadedUri -> queueFileForUpload(
                                getOptimizedPictureIfNecessary(downloadedUri),
                                getContentResolver().getType(downloadedUri)));
            }
        }
    }

    private final OnBackStackChangedListener mOnBackStackChangedListener =
            () -> ActivityUtils.hideKeyboard(MediaBrowserActivity.this);

    private void doBindDeleteService(Intent intent) {
        mDeleteServiceBound = bindService(intent, mDeleteConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
    }

    private void doUnbindDeleteService() {
        if (mDeleteServiceBound) {
            unbindService(mDeleteConnection);
            mDeleteServiceBound = false;
        }
    }

    private final ServiceConnection mDeleteConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDeleteService = (MediaDeleteService.MediaDeleteBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDeleteService = null;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                // Coming from zero connection. Continue what's pending for delete
                if (mMediaStore.hasSiteMediaToDelete(mSite)) {
                    startMediaDeleteService(null);
                }
            }
        }
    };

    public void showAddMediaPopup() {
        View anchor = findViewById(R.id.menu_new_media);
        PopupMenu popup = new PopupMenu(this, anchor);

        popup.getMenu().add(R.string.photo_picker_capture_photo).setOnMenuItemClickListener(
                item -> {
                    doAddMediaItemClicked(AddMenuItem.ITEM_CAPTURE_PHOTO);
                    return true;
                });

        if (!mBrowserType.isSingleImagePicker()) {
            popup.getMenu().add(R.string.photo_picker_capture_video).setOnMenuItemClickListener(
                    item -> {
                        doAddMediaItemClicked(AddMenuItem.ITEM_CAPTURE_VIDEO);
                        return true;
                    });
        }

        popup.getMenu().add(R.string.photo_picker_choose_file).setOnMenuItemClickListener(
                item -> {
                    doAddMediaItemClicked(AddMenuItem.ITEM_CHOOSE_FILE);
                    return true;
                });

        if (mBrowserType.isBrowser() && mSite.isUsingWpComRestApi()
            && !mJetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()) {
            popup.getMenu().add(R.string.photo_picker_stock_media).setOnMenuItemClickListener(
                    item -> {
                        doAddMediaItemClicked(AddMenuItem.ITEM_CHOOSE_STOCK_MEDIA);
                        return true;
                    });
        }

        if (mBrowserType.isBrowser() && !mJetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()) {
            popup.getMenu().add(R.string.photo_picker_gif).setOnMenuItemClickListener(
                    item -> {
                        doAddMediaItemClicked(AddMenuItem.ITEM_CHOOSE_GIF);
                        return true;
                    });
        }

        popup.show();
    }

    private void completeUploadMediaQuickStartTask() {
        if (mSelectedSiteRepository.getSelectedSite() != null
            && mQuickStartRepository.isPendingTask(QuickStartExistingSiteTask.UPLOAD_MEDIA)) {
            mQuickStartRepository.completeTask(QuickStartExistingSiteTask.UPLOAD_MEDIA);
        }
    }

    private void doAddMediaItemClicked(@NonNull AddMenuItem item) {
        mLastAddMediaItemClicked = item;

        // stock photos item requires no permission, all other items do
        if (item != AddMenuItem.ITEM_CHOOSE_STOCK_MEDIA) {
            String[] permissions = null;
            if (item == AddMenuItem.ITEM_CAPTURE_PHOTO || item == AddMenuItem.ITEM_CAPTURE_VIDEO) {
                permissions = PermissionUtils.getCameraAndStoragePermissions();
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            if (permissions != null && !PermissionUtils.checkAndRequestPermissions(
                    this, WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE, permissions)) {
                return;
            }
        }

        switch (item) {
            case ITEM_CAPTURE_PHOTO:
                WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID, this);
                break;
            case ITEM_CAPTURE_VIDEO:
                WPMediaUtils.launchVideoCamera(this);
                break;
            case ITEM_CHOOSE_FILE:
                mMediaPickerLauncher.showFilePicker(this, true, mSite);
                break;
            case ITEM_CHOOSE_STOCK_MEDIA:
                mMediaPickerLauncher.showStockMediaPickerForResult(this,
                        mSite, RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT, true);
                break;
            case ITEM_CHOOSE_GIF:
                mMediaPickerLauncher.showGifPickerForResult(
                        this,
                        mSite,
                        true
                );
                break;
        }
    }

    private Uri getOptimizedPictureIfNecessary(Uri originalUri) {
        String filePath = MediaUtils.getRealPathFromURI(this, originalUri);
        if (TextUtils.isEmpty(filePath)) {
            return originalUri;
        }
        Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(this, filePath, false);
        if (optimizedMedia != null) {
            return optimizedMedia;
        } else {
            // Optimization is OFF. Make sure the picture is in portrait for .org site
            // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
            if (!mSite.isWPCom()) {
                // If it's not wpcom we must rotate the picture locally
                Uri rotatedMedia = WPMediaUtils.fixOrientationIssue(this, filePath, false);
                if (rotatedMedia != null) {
                    return rotatedMedia;
                }
            }
        }

        return originalUri;
    }

    private void addMediaToUploadService(@NonNull MediaModel media) {
        ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);

        addMediaToUploadService(mediaList);
    }

    private void addMediaToUploadService(@NonNull ArrayList<MediaModel> mediaModels) {
        // Start the upload service if it's not started and fill the media queue
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start UploadService, internet connection required.");
            ToastUtils.showToast(this, R.string.no_network_message, ToastUtils.Duration.SHORT);
            return;
        }

        UploadService.uploadMedia(this, mediaModels, "MediaBrowserActivity#addMediaToUploadService");
        AppReviewManager.INSTANCE.incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA);
    }

    private void queueFileForUpload(Uri uri, String mimeType) {
        MediaModel media = FluxCUtils.mediaModelFromLocalUri(this, uri, mimeType, mMediaStore, mSite.getId());
        if (media == null) {
            ToastUtils.showToast(this, R.string.file_not_found, ToastUtils.Duration.SHORT);
            return;
        }

        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        addMediaToUploadService(media);

        updateMediaGridItem(media, false);
    }

    private void handleSharedMedia() {
        Intent intent = getIntent();

        final List<Uri> multiStream;
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            multiStream = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            multiStream = new ArrayList<>();
            multiStream.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
        } else {
            multiStream = null;
        }

        if (multiStream != null) {
            uploadList(multiStream);
        }

        // clear the intent's action, so that in case the user rotates, we don't re-upload the same files
        getIntent().setAction(null);
    }

    private void startMediaDeleteService(ArrayList<MediaModel> mediaToDelete) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start MediaDeleteService, internet connection required.");
            return;
        }

        if (mDeleteService != null) {
            if (mediaToDelete != null && !mediaToDelete.isEmpty()) {
                for (MediaModel media : mediaToDelete) {
                    mDeleteService.addMediaToDeleteQueue(media);
                }
            }
        } else {
            Intent intent = new Intent(this, MediaDeleteService.class);
            intent.putExtra(MediaDeleteService.SITE_KEY, mSite);
            if (mediaToDelete != null) {
                intent.putExtra(MediaDeleteService.MEDIA_LIST_KEY, mediaToDelete);
                doBindDeleteService(intent);
            }
            try {
                startService(intent);
            } catch (IllegalStateException e) {
                // This can happen if the app still appears to be running in the background
                // see: https://github.com/wordpress-mobile/WordPress-Android/issues/18638
                AppLog.e(AppLog.T.MEDIA, "Unable to start MediaDeleteService: " + e.getMessage());
            }
        }
    }

    private void updateMediaGridItem(@NonNull MediaModel media, boolean forceUpdate) {
        if (mMediaGridFragment != null) {
            if (mMediaStore.getMediaWithLocalId(media.getId()) != null) {
                mMediaGridFragment.updateMediaItem(media, forceUpdate);
            } else {
                mMediaGridFragment.removeMediaItem(media);
            }
        }
    }

    private void updateMediaGridForTheseMedia(List<MediaModel> mediaModelList) {
        if (mediaModelList != null) {
            for (MediaModel media : mediaModelList) {
                updateMediaGridItem(media, true);
            }
        }
    }

    private void reloadMediaGrid() {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.reload();
            mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSite));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        SiteModel site = mSiteStore.getSiteByLocalId(mSite.getId());

        if (site != null) {
            mSite = site;
            showQuota(true);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UploadService.UploadErrorEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        View snackbarAttachView = findViewById(R.id.tab_layout);
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty() && snackbarAttachView != null) {
            mUploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                    this,
                    snackbarAttachView,
                    true,
                    !TextUtils.isEmpty(event.errorMessage)
                    && event.errorMessage.contains(getString(R.string.error_media_quota_exceeded))
                        ? null
                        : event.mediaModelList,
                    mSite,
                    event.errorMessage
            );
            updateMediaGridForTheseMedia(event.mediaModelList);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UploadService.UploadMediaSuccessEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        View snackbarAttachView = findViewById(R.id.tab_layout);
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty() && snackbarAttachView != null) {
            mUploadUtilsWrapper.onMediaUploadedSnackbarHandler(this, snackbarAttachView, false, event.mediaModelList,
                    mSite, event.successMessage);
            updateMediaGridForTheseMedia(event.mediaModelList);
        }
    }

    public void updateMenuNewMediaQuickStartFocusPoint(boolean shouldShow) {
        if (mMenuNewMediaQuickStartFocusPoint != null && menuNewMediaItem.isVisible()) {
            mMenuNewMediaQuickStartFocusPoint.setVisibleOrGone(shouldShow);
        }
    }
}
