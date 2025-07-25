/**
 * This suppression is so we can include deprecated activities (CommentsDetailActivity)
 */
@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.wordpress.android.support.SupportWebViewActivity
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignParentActivity
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListActivity
import org.wordpress.android.ui.comments.CommentsDetailActivity
import org.wordpress.android.ui.debug.preferences.DebugSharedPreferenceFlagsActivity
import org.wordpress.android.ui.domains.management.DomainManagementActivity
import org.wordpress.android.ui.domains.management.newdomainsearch.NewDomainSearchActivity
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity
import org.wordpress.android.ui.jetpackoverlay.JetpackStaticPosterActivity
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.install.JetpackFullPluginInstallActivity
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallActivity
import org.wordpress.android.ui.main.feedbackform.FeedbackFormActivity
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.mysite.menu.MenuActivity
import org.wordpress.android.ui.mysite.personalization.PersonalizationActivity
import org.wordpress.android.ui.notifications.NotificationsDetailActivity
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeaturesActivity
import org.wordpress.android.ui.reader.ReaderCommentListActivity
import org.wordpress.android.ui.reader.ReaderPostPagerActivity
import org.wordpress.android.ui.reader.ReaderSubsActivity
import org.wordpress.android.ui.selfhostedusers.SelfHostedUsersActivity
import org.wordpress.android.ui.sitemonitor.SiteMonitorParentActivity
import org.wordpress.android.ui.subscribers.SubscribersActivity

/**
 * Base class for all activities - initially created to handle insets for Android 15's edge-to-edge support,
 * but can be extended in the future to handle other situations
 */
open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // apply insets for Android 15+ edge-to-edge
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) &&
            !isExcludedActivity(this)
        ) {
            applyInsetOffsets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun applyInsetOffsets() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                innerPadding.left,
                innerPadding.top,
                innerPadding.right,
                innerPadding.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }
}

private fun isExcludedActivity(activity: BaseAppCompatActivity) =
    excludedActivities.contains(activity::class.java.name)

/**
 * Activities that are excluded from the edge-to-edge offset. Note that many of these excluded activities are
 * Compose-based because Compose automatically adjusts for edge-to-edge insets. We may want to revisit this
 * approach as we add more Compose-based activities to the project.
 */
private val excludedActivities = listOf(
    BlazeCampaignParentActivity::class.java.name,
    BloggingPromptsListActivity::class.java.name,
    DebugSharedPreferenceFlagsActivity::class.java.name,
    DomainManagementActivity::class.java.name,
    EditJetpackSocialShareMessageActivity::class.java.name,
    ExperimentalFeaturesActivity::class.java.name,
    FeedbackFormActivity::class.java.name,
    JetpackFullPluginInstallActivity::class.java.name,
    JetpackRemoteInstallActivity::class.java.name,
    JetpackStaticPosterActivity::class.java.name,
    MediaPreviewActivity::class.java.name,
    MenuActivity::class.java.name,
    NewDomainSearchActivity::class.java.name,
    PersonalizationActivity::class.java.name,
    PurchaseDomainActivity::class.java.name,
    SelfHostedUsersActivity::class.java.name,
    SiteMonitorParentActivity::class.java.name,
    SubscribersActivity::class.java.name,
    SupportWebViewActivity::class.java.name,

    // these are excluded because they explicitly enable edge-to-edge
    ReaderPostPagerActivity::class.java.name,

    // these are excluded and use the NoEdgeToEdge style to avoid the keyboard overlapping
    // their editors
    CommentsDetailActivity::class.java.name,
    EditPostActivity::class.java.name,
    NotificationsDetailActivity::class.java.name,
    ReaderCommentListActivity::class.java.name,
    ReaderSubsActivity::class.java.name,
)
