package org.wordpress.android.ui.activitylog.list

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityLogListActivityBinding
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailActivity
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_ACTION_STATE_ID
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_REWIND_ID
import org.wordpress.android.ui.jetpack.common.JetpackBackupDownloadActionState
import org.wordpress.android.ui.jetpack.restore.KEY_RESTORE_RESTORE_ID
import org.wordpress.android.ui.jetpack.restore.KEY_RESTORE_REWIND_ID
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWINDABLE_ONLY_KEY
import javax.inject.Inject
import android.R as AndroidR

@AndroidEntryPoint
class ActivityLogListActivity : BaseAppCompatActivity(), ScrollableViewInitializedListener {
    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var uiHelpers: UiHelpers

    private var binding: ActivityLogListActivityBinding? = null

    private val isRewindableOnlyFromExtras by lazy {
        intent.getBooleanExtra(ACTIVITY_LOG_REWINDABLE_ONLY_KEY, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ActivityLogListActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this
            checkAndUpdateUiToBackupScreen()

            setSupportActionBar(toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        initJetpackBanner(containerId)
    }

    private fun initJetpackBanner(scrollableContainerId: Int) {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = when (isRewindableOnlyFromExtras) {
                true -> JetpackPoweredScreen.WithDynamicText.BACKUP
                else -> JetpackPoweredScreen.WithDynamicText.ACTIVITY_LOG
            }

            binding?.root?.post {
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(scrollableContainerId) as? RecyclerView
                    ?: return@post

                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                binding?.jetpackBanner?.jetpackBannerText?.text = uiHelpers.getTextOfUiString(
                    this,
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    binding?.jetpackBanner?.root?.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
                        JetpackPoweredBottomSheetFragment
                            .newInstance()
                            .show(supportFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }

    /**
     * It was decided to reuse the 'Activity Log' screen instead of creating a new 'Backup' screen. This was due to the
     * fact that there will be lots of code that would need to be duplicated for the new 'Backup' screen. On the other
     * hand, not much more complexity would be introduced if the 'Activity Log' screen is reused (mainly some 'if/else'
     * code branches here and there).
     *
     * However, should more 'Backup' related additions are added to the 'Activity Log' screen, then it should become a
     * necessity to split those features in separate screens in order not to increase further the complexity of this
     * screen's architecture.
     */
    private fun ActivityLogListActivityBinding.checkAndUpdateUiToBackupScreen() {
        if (isRewindableOnlyFromExtras) {
            setTitle(R.string.backup)
            activityTypeFilter.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.ACTIVITY_LOG_DETAIL -> {
                when (data?.getStringExtra(ActivityLogDetailActivity.EXTRA_INNER_FLOW)) {
                    ActivityLogDetailActivity.EXTRA_RESTORE_FLOW -> onActivityResultForRestore(data)
                    ActivityLogDetailActivity.EXTRA_BACKUP_DOWNLOAD_FLOW -> onActivityResultForBackupDownload(data)
                    else -> Unit // Do nothing
                }
            }
            RequestCodes.RESTORE -> onActivityResultForRestore(data)
            RequestCodes.BACKUP_DOWNLOAD -> onActivityResultForBackupDownload(data)
        }
    }

    private fun onActivityResultForRestore(data: Intent?) {
        val rewindId = data?.getStringExtra(KEY_RESTORE_REWIND_ID)
        val restoreId = data?.getLongExtra(KEY_RESTORE_RESTORE_ID, 0)
        if (rewindId != null && restoreId != null) {
            passQueryRestoreStatus(rewindId, restoreId)
        }
    }

    private fun onActivityResultForBackupDownload(data: Intent?) {
        val rewindId = data?.getStringExtra(KEY_BACKUP_DOWNLOAD_REWIND_ID)
        val downloadId = data?.getLongExtra(KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID, 0)
        val actionState = data?.getIntExtra(KEY_BACKUP_DOWNLOAD_ACTION_STATE_ID, 0)
            ?: JetpackBackupDownloadActionState.CANCEL.id
        if (actionState != JetpackBackupDownloadActionState.CANCEL.id && rewindId != null && downloadId != null) {
            passQueryBackupDownloadStatus(rewindId, downloadId, actionState)
        }
    }

    private fun passQueryRestoreStatus(rewindId: String, restoreId: Long) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onQueryRestoreStatus(rewindId, restoreId)
        }
    }

    private fun passQueryBackupDownloadStatus(rewindId: String, downloadId: Long, actionState: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onQueryBackupDownloadStatus(rewindId, downloadId, actionState)
        }
    }
}
