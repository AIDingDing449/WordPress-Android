package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.EditorThemeAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.EditorThemeSupport
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PagesAuthorFilterUIState
import org.wordpress.android.ui.pages.PagesListAction
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.SiteEditorMVPFeatureConfig
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.Date
import java.util.Locale

private const val HOUR_IN_MILLISECONDS = 3600000L

@ExperimentalCoroutinesApi
class PageListViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var mediaStore: MediaStore

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var pagesViewModel: PagesViewModel

    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Mock
    lateinit var pageItemProgressUiStateUseCase: PageItemProgressUiStateUseCase

    @Mock
    lateinit var pageListItemActionsUseCase: CreatePageListItemActionsUseCase

    @Mock
    lateinit var createUploadStateUseCase: PostModelUploadUiStateUseCase

    @Mock
    lateinit var createLabelsUseCase: CreatePageListItemLabelsUseCase

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var siteEditorMVPFeatureConfig: SiteEditorMVPFeatureConfig

    @Mock
    lateinit var editorThemeStore: EditorThemeStore

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var pageConflictDetector: PageConflictDetector

    private lateinit var viewModel: PageListViewModel

    private val site = SiteModel().apply {
        hasCapabilityEditOthersPages = true
    }

    private val pageListState = MutableLiveData<PageListState>()
    private lateinit var actions: MutableList<Action<*>>

    @Before
    fun setUp() {
        viewModel = PageListViewModel(
            createLabelsUseCase,
            createUploadStateUseCase,
            pageListItemActionsUseCase,
            pageItemProgressUiStateUseCase,
            mediaStore,
            dispatcher,
            localeManagerWrapper,
            accountStore,
            editorThemeStore,
            siteEditorMVPFeatureConfig,
            blazeFeatureUtils,
            testDispatcher(),
            pageConflictDetector
        )

        whenever(pageItemProgressUiStateUseCase.getProgressStateForPage(any())).thenReturn(
            Pair(
                ProgressBarUiState.Hidden, false
            )
        )

        val invalidateUploadStatus = MutableLiveData<List<LocalId>>()

        whenever(pagesViewModel.arePageActionsEnabled).thenReturn(false)
        site.setIsSingleUserSite(false)
        whenever(pagesViewModel.site).thenReturn(site)
        whenever(pagesViewModel.invalidateUploadStatus).thenReturn(invalidateUploadStatus)
        whenever(pagesViewModel.uploadStatusTracker).thenReturn(mock())
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.getDefault())
        whenever(createUploadStateUseCase.createUploadUiState(any(), any(), any())).thenReturn(
            PostUploadUiState.NothingToUpload
        )
        whenever(createLabelsUseCase.createLabels(any(), any())).thenReturn(Pair(emptyList(), 0))
        site.id = 10
        pageListState.value = PageListState.DONE

        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)
        val authorFilterState = MutableLiveData<PagesAuthorFilterUIState>()
        whenever(pagesViewModel.authorUIState).thenReturn(authorFilterState)
        val accountModel = AccountModel()
        accountModel.userId = 4

        doAnswer {
            actions.add(it.getArgument(0))
        }.whenever(dispatcher).dispatch(any())
        actions = mutableListOf()
    }

    @Test
    fun `on pages updates published model`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val date = Date()

        pages.value = listOf(buildPageModel(1, date))

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(3)
        val firstItem = pageItems[0] as PublishedPage
        assertThat(firstItem.title).isEqualTo("Title 01")
        assertThat(firstItem.date).isEqualTo(date)
        assertThat(firstItem.actionsEnabled).isEqualTo(false)
        assertDivider(pageItems[1])
        assertDivider(pageItems[2])
    }

    @Test
    fun `on pages updates published model for Site Editor MVP and non block-based theme`() {
        whenever(siteEditorMVPFeatureConfig.isEnabled()).thenReturn(true)
        mockEditorThemeIsBlockBased(false)

        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val date = Date()

        pages.value = listOf(buildPageModel(1, date))

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(3)
        val firstItem = pageItems[0] as PublishedPage
        assertThat(firstItem.title).isEqualTo("Title 01")
        assertThat(firstItem.date).isEqualTo(date)
        assertThat(firstItem.actionsEnabled).isEqualTo(false)
        assertDivider(pageItems[1])
        assertDivider(pageItems[2])
    }

    @Test
    fun `on pages updates published model for Site Editor MVP and block-based theme`() {
        whenever(siteEditorMVPFeatureConfig.isEnabled()).thenReturn(true)
        mockEditorThemeIsBlockBased(true)

        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val date = Date()

        pages.value = listOf(buildPageModel(1, date))

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(4)
        assertThat(pageItems[0]).isInstanceOf(PageItem.VirtualHomepage::class.java)

        val firstItem = pageItems[1] as PublishedPage
        assertThat(firstItem.title).isEqualTo("Title 01")
        assertThat(firstItem.date).isEqualTo(date)
        assertThat(firstItem.actionsEnabled).isEqualTo(false)
        assertDivider(pageItems[2])
        assertDivider(pageItems[3])
    }

    @Test
    fun `sorts 100 or more pages by date DESC`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val earlyPages = (0..30).map { buildPageModel(it, Date(HOUR_IN_MILLISECONDS * it)) }
        val latePages = (31..60).map { buildPageModel(it, Date(100 * HOUR_IN_MILLISECONDS * it)) }
        val middlePages = (61..96).map { buildPageModel(it, Date(10 * HOUR_IN_MILLISECONDS * it)) }
        val earlyChild = buildPageModel(97, Date(40 * HOUR_IN_MILLISECONDS), earlyPages[0])
        val middleChild = buildPageModel(98, Date(1000 * HOUR_IN_MILLISECONDS), middlePages[0])
        val lateChild = buildPageModel(99, Date(7000 * HOUR_IN_MILLISECONDS), latePages[0])
        val children = listOf(middleChild, earlyChild, lateChild)

        val pageModels = mutableListOf<PageModel>()
        pageModels.addAll(middlePages)
        pageModels.addAll(latePages)
        pageModels.addAll(children)
        pageModels.addAll(earlyPages)
        pages.value = pageModels

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(102)
        assertPublishedPage(pageItems[0], lateChild)
        for (index in 1..latePages.size) {
            assertPublishedPage(pageItems[index], latePages[latePages.size - index])
        }
        assertPublishedPage(pageItems[31], middleChild)
        for (index in 1..middlePages.size) {
            assertPublishedPage(pageItems[31 + index], middlePages[middlePages.size - index])
        }
        assertPublishedPage(pageItems[68], earlyChild)
        for (index in 1..earlyPages.size) {
            assertPublishedPage(pageItems[68 + index], earlyPages[earlyPages.size - index])
        }
        assertDivider(pageItems[100])
        assertDivider(pageItems[101])
    }

    @Test
    fun `sorts up to 99 pages topologically`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val earlyPages = (0..30).map { buildPageModel(it, Date(HOUR_IN_MILLISECONDS * it)) }
        val latePages = (31..60).map { buildPageModel(it, Date(100 * HOUR_IN_MILLISECONDS * it)) }
        val middlePages = (61..95).map { buildPageModel(it, Date(10 * HOUR_IN_MILLISECONDS * it)) }
        val earlyChild = buildPageModel(96, Date(40 * HOUR_IN_MILLISECONDS), earlyPages[0])
        val middleChild = buildPageModel(97, Date(1000 * HOUR_IN_MILLISECONDS), middlePages[0])
        val lateChild = buildPageModel(98, Date(7000 * HOUR_IN_MILLISECONDS), latePages[0])
        val children = listOf(middleChild, earlyChild, lateChild)

        val pageModels = mutableListOf<PageModel>()
        pageModels.addAll(middlePages)
        pageModels.addAll(latePages)
        pageModels.addAll(children)
        pageModels.addAll(earlyPages)
        pages.value = pageModels

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(101)
        assertPublishedPage(pageItems[0], earlyPages[0], 0)
        assertPublishedPage(pageItems[1], earlyChild, 1)
        for (index in 1 until earlyPages.size) {
            assertPublishedPage(pageItems[index + 1], earlyPages[index], 0)
        }
        assertPublishedPage(pageItems[32], latePages[0], 0)
        assertPublishedPage(pageItems[33], lateChild, 1)
        for (index in 1 until latePages.size) {
            assertPublishedPage(pageItems[index + 33], latePages[index], 0)
        }
        assertPublishedPage(pageItems[63], middlePages[0], 0)
        assertPublishedPage(pageItems[64], middleChild, 1)
        for (index in 1 until middlePages.size) {
            assertPublishedPage(pageItems[index + 64], middlePages[index], 0)
        }
        assertDivider(pageItems[99])
        assertDivider(pageItems[100])
    }

    @Test
    fun `sorts pages ignoring case`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val firstPage = buildPageModel(0, pageTitle = "ab", postStatus = PostStatus.PUBLISHED.toString())
        val secondPage = buildPageModel(0, pageTitle = "Ac", postStatus = PostStatus.PUBLISHED.toString())

        val pageModels = mutableListOf<PageModel>()
        pageModels += secondPage
        pageModels += firstPage
        pages.value = pageModels

        assertThat(result).hasSize(1)
        assertThat((result[0].first[0] as PublishedPage).title).isEqualTo(firstPage.title)
        assertThat((result[0].first[1] as PublishedPage).title).isEqualTo(secondPage.title)
    }

    @Test
    fun `showOverlay is correctly propagated from PageItemProgressUiStateUseCase`() {
        // Arrange
        val expectedShowOverlay = true
        val pages = MutableLiveData<List<PageModel>>()

        whenever(
            pageItemProgressUiStateUseCase.getProgressStateForPage(anyOrNull())
        ).thenReturn(
            Pair(
                mock(),
                expectedShowOverlay
            )
        )
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)
        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()
        viewModel.pages.observeForever { result.add(it) }

        // Act
        pages.value = listOf(buildPageModel(0))

        // Assert
        assertThat((result[0].first[0] as Page).showOverlay).isEqualTo(expectedShowOverlay)
    }

    @Test
    fun `ProgressBarUiState is correctly propagated from PageItemProgressUiStateUseCase`() {
        // Arrange
        val expectedProgressBarUiState = ProgressBarUiState.Indeterminate
        val pages = MutableLiveData<List<PageModel>>()

        whenever(pageItemProgressUiStateUseCase.getProgressStateForPage(anyOrNull())).thenReturn(
            Pair(
                expectedProgressBarUiState,
                true
            )
        )
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)
        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()
        viewModel.pages.observeForever { result.add(it) }

        // Act
        pages.value = listOf(buildPageModel(0))

        // Assert
        assertThat((result[0].first[0] as Page).progressBarUiState).isEqualTo(expectedProgressBarUiState)
    }

    @Test
    fun `verify PageListItemActionsUseCase passes the Menu Actions to PublishedPage`() {
        // Arrange
        val actions = mutableListOf(mock<PagesListAction>())

        whenever(
            pageListItemActionsUseCase.setupPageActions(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(
            actions
        )

        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)
        val result = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()
        viewModel.pages.observeForever { result.add(it) }

        // Act
        pages.value = listOf(buildPageModel(0))

        // Assert
        assertThat((result[0].first[0] as PublishedPage).actions).isEqualTo(actions)
    }

    @Test
    fun `filter pages by everyone`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)
        val authorFilterState = MutableLiveData<PagesAuthorFilterUIState>()
        whenever(pagesViewModel.authorUIState).thenReturn(authorFilterState)

        viewModel.start(PUBLISHED, pagesViewModel)

        val pagesResult = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { pagesResult.add(it) }

        val pageModels = (0..10).map { buildPageModel(it, authorId = it.toLong()) }
        pages.value = pageModels

        assertThat(pagesResult).hasSize(1)
        var pageItems = pagesResult[0].first
        assertThat(pageItems).hasSize(13)

        authorFilterState.value = PagesAuthorFilterUIState(
            authorFilterSelection = EVERYONE,
            authorFilterItems = listOf(),
            isAuthorFilterVisible = true
        )
        authorFilterSelection.value = EVERYONE

        pageItems = pagesResult[1].first
        assertThat(pageItems).hasSize(13)
    }

    @Test
    fun `filter pages by me`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)
        val authorFilterState = MutableLiveData<PagesAuthorFilterUIState>()
        whenever(pagesViewModel.authorUIState).thenReturn(authorFilterState)

        viewModel.start(PUBLISHED, pagesViewModel)

        val pagesResult = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { pagesResult.add(it) }

        val pageModels = (0..10).map { buildPageModel(it, authorId = it.toLong()) }
        pages.value = pageModels

        assertThat(pagesResult).hasSize(1)
        var pageItems = pagesResult[0].first
        assertThat(pageItems).hasSize(13)

        authorFilterState.value = PagesAuthorFilterUIState(
            authorFilterSelection = ME,
            authorFilterItems = listOf(),
            isAuthorFilterVisible = true
        )
        authorFilterSelection.value = ME

        pageItems = pagesResult[1].first
        assertThat(pageItems).hasSize(13)
    }

    @Test
    fun `filter pages by everyone shows author name`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)
        val authorFilterState = MutableLiveData<PagesAuthorFilterUIState>()
        whenever(pagesViewModel.authorUIState).thenReturn(authorFilterState)

        val authorDisplayName = "Automattic"
        viewModel.start(PUBLISHED, pagesViewModel)

        val pagesResult = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { pagesResult.add(it) }

        val pageModels = (0..1).map {
            buildPageModel(
                it, authorId = it.toLong(),
                authorDisplayName = authorDisplayName
            )
        }
        pages.value = pageModels

        authorFilterState.value = PagesAuthorFilterUIState(
            authorFilterSelection = EVERYONE,
            authorFilterItems = listOf(),
            isAuthorFilterVisible = true
        )
        authorFilterSelection.value = EVERYONE

        val pageItems = pagesResult[1].first
        val pageItem = pageItems[0] as PublishedPage
        assertThat(pageItem.author).isEqualToIgnoringCase(authorDisplayName)
    }

    @Test
    fun `filter pages by me does not show author name`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)
        val authorFilterState = MutableLiveData<PagesAuthorFilterUIState>()
        whenever(pagesViewModel.authorUIState).thenReturn(authorFilterState)

        viewModel.start(PUBLISHED, pagesViewModel)

        val pagesResult = mutableListOf<Triple<List<PageItem>, Boolean, Boolean>>()

        viewModel.pages.observeForever { pagesResult.add(it) }

        val pageModels = (0..1).map { buildPageModel(it, authorId = it.toLong()) }
        pages.value = pageModels

        authorFilterState.value = PagesAuthorFilterUIState(
            authorFilterSelection = ME,
            authorFilterItems = listOf(),
            isAuthorFilterVisible = true
        )
        authorFilterSelection.value = ME

        val pageItems = pagesResult[1].first
        val pageItem = pageItems[0] as PublishedPage
        assertThat(pageItem.author).isNull()
    }

    @Test
    fun `Should refresh EditorTheme when start is called the first time`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        viewModel.start(PUBLISHED, pagesViewModel)
        assertThat(actions.last().type).isEqualTo(EditorThemeAction.FETCH_EDITOR_THEME)
    }

    @Test
    fun `Should call EditorThemeStore getIsBlockBasedTheme when start is called the first time`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        viewModel.start(PUBLISHED, pagesViewModel)
        verify(editorThemeStore).getIsBlockBasedTheme(site)
    }

    @Test
    fun `onVirtualHomepageAction delegates to parent view model`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)
        val authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
        whenever(pagesViewModel.authorSelectionUpdated).thenReturn(authorFilterSelection)

        viewModel.start(PUBLISHED, pagesViewModel)

        val action = PageItem.VirtualHomepage.Action.OpenSiteEditor
        viewModel.onVirtualHomepageAction(action)

        verify(pagesViewModel).onVirtualHomepageAction(action)
    }

    private fun buildPageModel(
        id: Int,
        date: Date = Date(0),
        parent: PageModel? = null,
        pageTitle: String? = null,
        status: PageStatus = PageStatus.PUBLISHED,
        authorId: Long? = null,
        authorDisplayName: String? = null,
        postStatus: String? = PostStatus.PUBLISHED.toString()
    ): PageModel {
        val title = pageTitle ?: if (id < 10) "Title 0$id" else "Title $id"
        return PageModel(
            PostModel().apply {
                this.setId(id)
                this.setAuthorId(authorId ?: 0)
                this.setAuthorDisplayName(authorDisplayName)
                this.setStatus(postStatus)
            },
            site, id, title, status, date, false, id.toLong(),
            parent, id.toLong()
        )
    }

    private fun assertDivider(pageItem: PageItem) {
        assertThat(pageItem is Divider).isTrue
    }

    private fun assertPublishedPage(pageItem: PageItem, pageModel: PageModel, indent: Int = 0) {
        val publishedPage = pageItem as PublishedPage
        assertThat(publishedPage.title).isEqualTo(pageModel.title)
        assertThat(publishedPage.date).isEqualTo(pageModel.date)
        assertThat(publishedPage.indent).isEqualTo(indent)
        assertThat(publishedPage.actionsEnabled).isEqualTo(false)
    }

    private fun mockEditorThemeIsBlockBased(isBlockBased: Boolean) {
        whenever(dispatcher.dispatch(any())).thenAnswer {
            val action = it.getArgument<Action<*>>(0)
            if (action.type == EditorThemeAction.FETCH_EDITOR_THEME) {
                val response = EditorThemeStore.OnEditorThemeChanged(
                    editorTheme = EditorTheme(
                        themeSupport = EditorThemeSupport(
                            colors = listOf(),
                            gradients = listOf(),
                            hasBlockTemplates = null,
                            rawStyles = null,
                            rawFeatures = null,
                            isBlockBasedTheme = isBlockBased,
                            galleryWithImageBlocks = false,
                            quoteBlockV2 = false,
                            listBlockV2 = false
                        ),
                        stylesheet = null,
                        version = null
                    ),
                    siteId = site.id,
                    causeOfChange = mock(),
                    endpoint = mock()
                )
                viewModel.onEditorThemeChanged(response)
            }
            Unit
        }
    }
}
