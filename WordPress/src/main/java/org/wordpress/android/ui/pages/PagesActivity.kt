package org.wordpress.android.ui.pages

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PagesActivityBinding
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.PostResolutionOverlayActionEvent
import org.wordpress.android.ui.posts.PostResolutionOverlayListener
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.pages.PageListViewModel
import javax.inject.Inject
import android.R as AndroidR

const val EXTRA_PAGE_REMOTE_ID_KEY = "extra_page_remote_id_key"
const val EXTRA_PAGE_PARENT_ID_KEY = "extra_page_parent_id_key"
const val EXTRA_PAGE_LIST_TYPE_KEY = "extra_page_list_type_key"

class PagesActivity : BaseAppCompatActivity(),
    BasicDialogPositiveClickInterface,
    BasicDialogNegativeClickInterface,
    PostResolutionOverlayListener {
    @Inject
    internal lateinit var systemNotificationTracker: SystemNotificationsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(PagesActivityBinding.inflate(layoutInflater).root)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
            val notificationType = requireNotNull(
                intent.getSerializableExtraCompat<NotificationType>(ARG_NOTIFICATION_TYPE)
            )
            systemNotificationTracker.trackTappedNotification(notificationType)
        }

        if (intent.hasExtra(EXTRA_PAGE_REMOTE_ID_KEY)) {
            val pageId = intent.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, -1)
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                (it as PagesFragment).onSpecificPageRequested(pageId)
            }
        }

        if (intent.hasExtra(EXTRA_PAGE_LIST_TYPE_KEY)) {
            val pageListType = requireNotNull(
                intent.getSerializableExtraCompat<PageListViewModel.PageListType>(EXTRA_PAGE_LIST_TYPE_KEY)
            )
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                (it as PagesFragment).onSpecificPageListTypeRequested(pageListType)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("UseCheckOrError")
    override fun onPositiveClicked(instanceTag: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onPositiveClickedForBasicDialog(instanceTag)
        } else {
            throw IllegalStateException("PagesFragment is required to consume this event.")
        }
    }

    @Suppress("UseCheckOrError")
    override fun onNegativeClicked(instanceTag: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onNegativeClickedForBasicDialog(instanceTag)
        } else {
            throw IllegalStateException("PagesFragment is required to consume this event.")
        }
    }

    @Suppress("UseCheckOrError")
    override fun onPostResolutionConfirmed(event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onPostResolutionConfirmed(event)
        } else {
            throw IllegalStateException("PagesFragment is required to consume this event.")
        }
    }
}
