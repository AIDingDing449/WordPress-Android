package org.wordpress.android.ui.reader;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.AppBarLayout;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.reader.adapters.ReaderUserAdapter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;

/*
 * displays a list of users who like a specific reader post
 */
public class ReaderUserListActivity extends BaseAppCompatActivity {
    private ReaderRecyclerView mRecyclerView;
    private ReaderUserAdapter mAdapter;
    private AppBarLayout mAppBarLayout;
    private int mRestorePosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_userlist);
        setTitle(null);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
        }

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(this, 1);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mAppBarLayout = findViewById(R.id.appbar_main);

        long blogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
        long postId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
        long commentId = getIntent().getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0);
        loadUsers(blogId, postId, commentId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        int position = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        if (position > 0) {
            outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, position);
        }
        super.onSaveInstanceState(outState);
    }

    private ReaderUserAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderUserAdapter(this);
            mAdapter.setDataLoadedListener(new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (!isEmpty && mRestorePosition > 0) {
                        mRecyclerView.scrollToPosition(mRestorePosition);
                        mAppBarLayout.post(mAppBarLayout::requestLayout);
                    }
                    mRestorePosition = 0;
                }
            });
            mRecyclerView.setAdapter(mAdapter);
        }
        return mAdapter;
    }

    private void loadUsers(final long blogId,
                           final long postId,
                           final long commentId) {
        new Thread() {
            @Override
            public void run() {
                final String title = getTitleString(blogId, postId, commentId);

                final ReaderUserList users;
                if (commentId == 0) {
                    // commentId is empty (not passed), so we're showing users who like a post
                    users = ReaderUserTable.getUsersWhoLikePost(
                            blogId,
                            postId,
                            ReaderConstants.READER_MAX_USERS_TO_DISPLAY);
                } else {
                    // commentId is non-empty, so we're showing users who like a comment
                    users = ReaderUserTable.getUsersWhoLikeComment(
                            blogId,
                            commentId,
                            ReaderConstants.READER_MAX_USERS_TO_DISPLAY);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            setTitle(title);
                            ReaderPost post = ReaderPostTable.getBlogPost(blogId, postId, true);
                            if (post != null) {
                                getAdapter().setIsFollowed(post.isFollowedByCurrentUser);
                            }
                            getAdapter().setUsers(users);
                        }
                    }
                });
            }
        }.start();
    }

    private String getTitleString(final long blogId,
                                  final long postId,
                                  final long commentId) {
        final int numLikes;
        final boolean isLikedByCurrentUser;
        if (commentId == 0) {
            numLikes = ReaderPostTable.getNumLikesForPost(blogId, postId);
            isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(blogId, postId);
        } else {
            numLikes = ReaderCommentTable.getNumLikesForComment(blogId, postId, commentId);
            isLikedByCurrentUser = ReaderCommentTable.isCommentLikedByCurrentUser(blogId, postId, commentId);
        }
        return ReaderUtils.getLongLikeLabelText(this, numLikes, isLikedByCurrentUser);
    }
}
