package org.wordpress.android.ui.reader.discover

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderDiscoverFragmentLayoutBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType
import org.wordpress.android.ui.reader.ReaderPostWebViewCachingFragment
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_POST_CARD
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.SharePost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostDetail
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderComments
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderSubs
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportUser
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowVideoViewer
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.addItemDivider
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.RecyclerItemDecoration
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ReaderDiscoverFragment : ViewPagerFragment(R.layout.reader_discover_fragment_layout), OnScrollToTopListener {
    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager
    private lateinit var viewModel: ReaderDiscoverViewModel

    @Inject
    lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var parentViewModel: ReaderViewModel

    private var binding: ReaderDiscoverFragmentLayoutBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ReaderDiscoverFragmentLayoutBinding.bind(view).apply {
            recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            recyclerView.adapter =
                ReaderDiscoverAdapter(
                    uiHelpers,
                    imageManager,
                    readerTracker,
                    networkUtilsWrapper,
                )

            // set the background color as we have different colors for the new and legacy designs that are not easy to
            // change via styles, because of the FeatureConfig logic
            val backgroundColor = R.color.reader_post_list_background
            recyclerView.setBackgroundColor(ContextCompat.getColor(requireContext(), backgroundColor))

            val spacingVerticalRes = R.dimen.reader_card_gutters
            val spacingHorizontal = resources.getDimensionPixelSize(R.dimen.reader_card_margin)
            val spacingVertical = resources.getDimensionPixelSize(spacingVerticalRes)
            recyclerView.addItemDecoration(RecyclerItemDecoration(spacingHorizontal, spacingVertical, false))

            // add a proper item divider to the RecyclerView when Reader Improvements are enabled
            recyclerView.addItemDivider(R.drawable.default_list_divider)

            WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(ptrLayout) { viewModel.swipeToRefresh() }

            initViewModel()
        }
    }

    private fun ReaderDiscoverFragmentLayoutBinding.initViewModel() {
        viewModel = ViewModelProvider(this@ReaderDiscoverFragment, viewModelFactory)
            .get(ReaderDiscoverViewModel::class.java)
        parentViewModel = ViewModelProvider(requireParentFragment()).get(ReaderViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                is DiscoverUiState.ContentUiState -> {
                    (recyclerView.adapter as ReaderDiscoverAdapter).update(it.cards)
                }
                is DiscoverUiState.EmptyUiState -> {
                    uiHelpers.setTextOrHide(actionableEmptyView.title, it.titleResId)
                    uiHelpers.setTextOrHide(actionableEmptyView.subtitle, it.subTitleRes)
                    uiHelpers.setImageOrHide(actionableEmptyView.image, it.illustrationResId)
                    uiHelpers.setTextOrHide(actionableEmptyView.button, it.buttonResId)
                    actionableEmptyView.button.setOnClickListener { _ -> it.action.invoke() }
                }
                is DiscoverUiState.LoadingUiState -> Unit // Do nothing
            }

            uiHelpers.updateVisibility(recyclerView, it.contentVisiblity)
            uiHelpers.updateVisibility(progressBar, it.fullscreenProgressVisibility)
            uiHelpers.updateVisibility(progressText, it.fullscreenProgressVisibility)
            uiHelpers.updateVisibility(progressLoadingMore, it.loadMoreProgressVisibility)
            uiHelpers.updateVisibility(actionableEmptyView, it.fullscreenEmptyVisibility)
            ptrLayout.isEnabled = it.swipeToRefreshEnabled
            ptrLayout.isRefreshing = it.reloadProgressVisibility
        }
        viewModel.scrollToTopEvent.observeEvent(viewLifecycleOwner) { recyclerView.scrollToPosition(0) }
        viewModel.navigationEvents.observeEvent(viewLifecycleOwner) { handleNavigation(it) }
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { it.showSnackbar() }
        viewModel.preloadPostEvents.observeEvent(viewLifecycleOwner) { it.addWebViewCachingFragment() }
        viewModel.start(parentViewModel)
    }

    @Suppress("LongMethod")
    private fun handleNavigation(event: ReaderNavigationEvents) = when (event) {
        is ShowPostDetail -> {
            ReaderActivityLauncher.showReaderPostDetail(
                context = requireActivity(),
                blogId = event.post.blogId,
                postId = event.post.postId
            )
        }
        is SharePost -> {
            ReaderActivityLauncher.sharePost(requireActivity(), event.post)
        }
        is OpenPost -> {
            ReaderActivityLauncher.openPost(requireActivity(), event.post)
        }
        is ShowReaderComments ->  {
            ReaderActivityLauncher.showReaderComments(
                context = requireActivity(),
                blogId = event.blogId,
                postId = event.postId,
                source = READER_POST_CARD.sourceDescription
            )
        }
        is ShowNoSitesToReblog -> ReaderActivityLauncher.showNoSiteToReblog(requireActivity())
        is ShowSitePickerForResult -> {
            ActivityLauncher.showSitePickerForResult(
                this@ReaderDiscoverFragment,
                event.preselectedSite,
                event.mode
            )
        }
        is OpenEditorForReblog -> {
            ActivityLauncher.openEditorForReblog(
                requireActivity(),
                event.site,
                event.post,
                event.source)
        }
        is ShowBookmarkedTab -> ActivityLauncher.viewSavedPostsListInReader(requireActivity())
        is ShowBookmarkedSavedOnlyLocallyDialog -> showBookmarkSavedLocallyDialog(event)
        is ShowPostsByTag -> {
            ReaderActivityLauncher.showReaderTagPreview(
                context = requireActivity(),
                tag = event.tag,
                source = ReaderTracker.SOURCE_DISCOVER,
                readerTracker = readerTracker
            )
        }
        is ShowVideoViewer -> ReaderActivityLauncher.showReaderVideoViewer(requireActivity(), event.videoUrl)
        is ShowBlogPreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
            requireActivity(),
            event.siteId,
            event.feedId,
            event.isFollowed,
            ReaderTracker.SOURCE_DISCOVER,
            readerTracker
        )
        is ShowReportPost -> ReaderActivityLauncher.openUrl(
            requireActivity(),
            readerUtilsWrapper.getReportPostUrl(event.url),
            OpenUrlType.INTERNAL
        )
        is ShowReportUser -> ReaderActivityLauncher.openUrl(
            requireActivity(),
            readerUtilsWrapper.getReportUserUrl(event.url, event.authorId),
            OpenUrlType.INTERNAL
        )
        is ShowReaderSubs -> ReaderActivityLauncher.showReaderSubs(requireActivity())
        else -> Unit // Do Nothing
    }

    private fun showBookmarkSavedLocallyDialog(bookmarkDialog: ShowBookmarkedSavedOnlyLocallyDialog) {
        if (bookmarksSavedLocallyDialog == null) {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(bookmarkDialog.title))
                .setMessage(getString(bookmarkDialog.message))
                .setPositiveButton(getString(bookmarkDialog.buttonLabel)) { _, _ ->
                    bookmarkDialog.okButtonAction.invoke()
                }
                .setOnDismissListener {
                    bookmarksSavedLocallyDialog = null
                }
                .setCancelable(false)
                .create()
                .let {
                    bookmarksSavedLocallyDialog = it
                    it.show()
                }
        }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        activity?.findViewById<View>(R.id.coordinator)?.let { coordinator ->
            val snackbar = WPSnackbar.make(
                coordinator,
                uiHelpers.getTextOfUiString(requireContext(), this.message),
                Snackbar.LENGTH_LONG
            )
            if (this.buttonTitle != null) {
                snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), this.buttonTitle)) {
                    this.buttonAction.invoke()
                }
            }
            snackbar.show()
        }
    }

    private fun PreLoadPostContent.addWebViewCachingFragment() {
        val tag = "$blogId$postId"

        if (parentFragmentManager.findFragmentByTag(tag) == null) {
            parentFragmentManager.beginTransaction()
                .add(ReaderPostWebViewCachingFragment.newInstance(blogId, postId), tag)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookmarksSavedLocallyDialog?.dismiss()
        binding = null
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
        return binding?.recyclerView
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val siteLocalId = data.getIntExtra(
                ChooseSiteActivity.KEY_SITE_LOCAL_ID,
                SelectedSiteRepository.UNAVAILABLE
            )
            viewModel.onReblogSiteSelected(siteLocalId)
        }
    }

    override fun onScrollToTop() {
        binding?.recyclerView?.smoothScrollToPosition(0)
    }
}
