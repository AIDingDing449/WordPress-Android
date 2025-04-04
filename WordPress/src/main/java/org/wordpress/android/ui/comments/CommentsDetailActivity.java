package org.wordpress.android.ui.comments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.CommentsDetailActivityBinding;
import org.wordpress.android.fluxc.action.CommentAction;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload;
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.comments.unified.CommentConstants;
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter;
import org.wordpress.android.ui.comments.unified.OnLoadMoreListener;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource;
import org.wordpress.android.util.extensions.CompatExtensionsKt;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static org.wordpress.android.ui.comments.unified.CommentConstants.COMMENTS_PER_PAGE;

/**
 * @deprecated
 * Comments are being refactored as part of Comments Unification project. If you are adding any
 * features or modifying this class, please ping develric or klymyam
 */
@Deprecated
@AndroidEntryPoint
@SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
public class CommentsDetailActivity extends BaseAppCompatActivity
        implements OnLoadMoreListener,
        CommentActions.OnCommentActionListener, ScrollableViewInitializedListener {
    public static final String COMMENT_ID_EXTRA = "commentId";
    public static final String COMMENT_STATUS_FILTER_EXTRA = "commentStatusFilter";

    @SuppressWarnings("deprecation")
    @Inject CommentsStoreAdapter mCommentsStoreAdapter;

    private long mCommentId;
    @Nullable private CommentStatus mStatusFilter;
    @Nullable private SiteModel mSite;
    @SuppressWarnings("deprecation")
    @Nullable private CommentDetailFragmentAdapter mAdapter;
    @Nullable private ViewPager.OnPageChangeListener mOnPageChangeListener;

    private boolean mIsLoadingComments;
    private boolean mIsUpdatingComments;
    private boolean mCanLoadMoreComments = true;

    @Nullable private CommentsDetailActivityBinding mBinding = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCommentsStoreAdapter.register(this);
        AppLog.i(AppLog.T.COMMENTS, "Creating CommentsDetailActivity");

        mBinding = CommentsDetailActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                        getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

                if (fragment != null) {
                    fragment.collapse();
                } else {
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        if (mBinding != null) {
            setSupportActionBar(mBinding.toolbarMain);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        if (savedInstanceState == null) {
            mCommentId = getIntent().getLongExtra(COMMENT_ID_EXTRA, -1);
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mStatusFilter = (CommentStatus) getIntent().getSerializableExtra(COMMENT_STATUS_FILTER_EXTRA);
        } else {
            mCommentId = savedInstanceState.getLong(COMMENT_ID_EXTRA);
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mStatusFilter = (CommentStatus) savedInstanceState.getSerializable(COMMENT_STATUS_FILTER_EXTRA);
        }

        if (mBinding != null) {
            // set up the viewpager and adapter for lateral navigation
            mBinding.viewpager.setPageTransformer(false,
                    new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER));

            // Asynchronously loads comments and build the adapter
            loadDataInViewPager(mBinding);
        }

        if (savedInstanceState == null) {
            // track initial comment view
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.SITE_COMMENTS, mSite);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(COMMENT_ID_EXTRA, mCommentId);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(COMMENT_STATUS_FILTER_EXTRA, mStatusFilter);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mCommentsStoreAdapter.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadMore() {
        if (mBinding != null) {
            updateComments(mBinding);
        }
    }

    private void updateComments(@NonNull CommentsDetailActivityBinding binding) {
        if (mIsUpdatingComments) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        } else if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, getString(R.string.error_refresh_comments_showing_older));
            return;
        } else if (!mCanLoadMoreComments) {
            AppLog.w(AppLog.T.COMMENTS, "no more comments to be loaded");
            return;
        }

        if (mSite != null && mStatusFilter != null && mAdapter != null) {
            mCommentsStoreAdapter.dispatch(CommentActionBuilder.newFetchCommentsAction(
                    new FetchCommentsPayload(mSite, mStatusFilter, COMMENTS_PER_PAGE, mAdapter.getCount()))
            );
            mIsUpdatingComments = true;
            setLoadingState(binding, true);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCommentChanged(OnCommentChanged event) {
        if (mBinding != null) {
            mIsUpdatingComments = false;
            setLoadingState(mBinding, false);
            // Don't refresh the list on push, we already updated comments
            if (event.causeOfChange != CommentAction.PUSH_COMMENT) {
                if (event.changedCommentsLocalIds.size() > 0) {
                    loadDataInViewPager(mBinding);
                } else if (!event.isError()) {
                    // There are no more comments to load
                    mCanLoadMoreComments = false;
                }
            }
            if (event.isError()) {
                if (!TextUtils.isEmpty(event.error.message)) {
                    ToastUtils.showToast(this, event.error.message);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void loadDataInViewPager(@NonNull CommentsDetailActivityBinding binding) {
        if (mIsLoadingComments) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(mCommentsStoreAdapter, mStatusFilter, mSite, new LoadCommentsTask.LoadingCallback() {
                @Override
                public void isLoading(boolean loading) {
                    setLoadingState(binding, loading);
                    mIsLoadingComments = loading;
                }

                @Override
                public void loadingFinished(CommentList commentList) {
                    if (!commentList.isEmpty()) {
                        showCommentList(binding, commentList);
                    }
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @SuppressWarnings("deprecation")
    private void showCommentList(
            @NonNull CommentsDetailActivityBinding binding,
            CommentList commentList
    ) {
        if (isFinishing()) {
            return;
        }
        final int previousItem = binding.viewpager.getCurrentItem();

        // Only notify adapter when loading new page
        if (mAdapter != null && mAdapter.isAddingNewComments(commentList)) {
            mAdapter.onNewItems(commentList);
        } else {
            if (mSite != null) {
                // If current items change, rebuild the adapter
                mAdapter = new CommentDetailFragmentAdapter(getSupportFragmentManager(), commentList, mSite,
                        CommentsDetailActivity.this);
                binding.viewpager.setAdapter(mAdapter);
            } else {
                throw new IllegalStateException("mAdapter cannot be constructed; mSite is null");
            }
        }

        final int commentIndex = mAdapter.commentIndex(mCommentId);
        if (commentIndex < 0) {
            showErrorToastAndFinish();
        }
        if (mOnPageChangeListener != null) {
            binding.viewpager.removeOnPageChangeListener(mOnPageChangeListener);
        } else {
            mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (mAdapter != null) {
                        final CommentModel comment = mAdapter.getCommentAtPosition(position);
                        if (comment != null) {
                            mCommentId = comment.getRemoteCommentId();
                            // track subsequent comment views
                            AnalyticsUtils.trackCommentActionWithSiteDetails(
                                    Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.SITE_COMMENTS, mSite);
                        }
                    }
                }
            };
        }
        if (commentIndex != previousItem) {
            binding.viewpager.setCurrentItem(commentIndex);
        }

        binding.viewpager.addOnPageChangeListener(mOnPageChangeListener);
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.COMMENTS, "Comment could not be found.");
        ToastUtils.showToast(this, R.string.error_load_comment);
        finish();
    }

    private void setLoadingState(
            @NonNull CommentsDetailActivityBinding binding,
            boolean visible
    ) {
        binding.progressLoading.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onModerateComment(
            final CommentModel comment,
            final CommentStatus newStatus
    ) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_ID_EXTRA, comment.getRemoteCommentId());
        resultIntent.putExtra(CommentConstants.COMMENT_MODERATE_STATUS_EXTRA, newStatus.toString());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onScrollableViewInitialized(int containerId) {
        if (mBinding != null) {
            mBinding.appbarMain.setLiftOnScrollTargetViewId(containerId);
        }
    }
}
