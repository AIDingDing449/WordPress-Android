package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.extensions.CompatExtensionsKt;

public class PostSettingsTagsActivity extends BaseAppCompatActivity implements TagsSelectedListener {
    public static final String KEY_TAGS = "KEY_TAGS";
    public static final String KEY_SELECTED_TAGS = "KEY_SELECTED_TAGS";
    private SiteModel mSite;
    private String mTags;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndFinish();
                CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mTags = getIntent().getStringExtra(KEY_TAGS);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.post_settings_tags_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        postponeEnterTransition();
        showPostSettingsTagsFragment();
    }

    private void showPostSettingsTagsFragment() {
        PostSettingsTagsFragment postSettingsTagsFragment = PostSettingsTagsFragment.newInstance(mSite, mTags);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, postSettingsTagsFragment, PostSettingsTagsFragment.TAG)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAndFinish() {
        closeKeyboard();

        Bundle bundle = new Bundle();
        bundle.putString(KEY_SELECTED_TAGS, mTags);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void closeKeyboard() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PostSettingsTagsFragment.TAG);
        if (fragment != null) {
            ((PostSettingsTagsFragment) fragment).closeKeyboard();
        }
    }

    @Override public void onTagsSelected(@NonNull String selectedTags) {
        mTags = selectedTags;
    }
}
