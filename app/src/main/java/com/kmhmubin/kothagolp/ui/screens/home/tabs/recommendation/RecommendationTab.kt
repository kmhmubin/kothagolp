package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.data.local.entity.TagFilterType
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.recommendation.model.LibrarySourceNovel
import com.kmhmubin.kothagolp.recommendation.model.OnboardingGenres
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.ui.screens.home.shared.RecommendationNavigationHelper
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.AiNoticeRow
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.AiOffRow
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouCarousel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouCoverRow
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouFirstLoad
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouFirstTime
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouLoading
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouMenu
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.ForYouRowHeader
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.GenrePreferencesSheet
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.GenreShapes
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.MoreLikeHeader
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.NovelActionMenu
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.RecommendationSettingsSheet
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.SeeAllSheet
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components.TagFilterSheet
import com.kmhmubin.kothagolp.ui.components.AlignedLargeTopBar
import kotlinx.coroutines.launch

private fun Recommendation.toCoverItem() =
    ForYouItem(novel.url, novel.name, novel.posterUrl, novel.apiName, novel.apiName, this)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecommendationTab(
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit = { _, _ -> },
    onNavigateToBrowse: () -> Unit = {},
    onNavigateToTagExplorer: (TagNormalizer.TagCategory) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val viewModel: RecommendationViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tagFilters by viewModel.tagFilters.collectAsStateWithLifecycle()
    val hiddenNovels by viewModel.hiddenNovels.collectAsStateWithLifecycle()
    val blockedAuthors by viewModel.blockedAuthors.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showHiddenSheet by remember { mutableStateOf(false) }
    var showGenrePrefsSheet by remember { mutableStateOf(false) }
    var selectedRecommendation by remember { mutableStateOf<Recommendation?>(null) }
    var seeAll by remember { mutableStateOf<Pair<String, String>?>(null) } // title to section key

    LaunchedEffect("init") {
        viewModel.checkGeminiKey()
        viewModel.loadAiRecommendations()
        viewModel.loadAiTrendingRecommendations()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkGeminiKey()
                viewModel.loadAiRecommendations()
                viewModel.loadAiTrendingRecommendations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A genre tapped elsewhere in the app: show more of it and open "Show more or less".
    LaunchedEffect(Unit) {
        RecommendationNavigationHelper.consumePendingTag()?.let { tag ->
            viewModel.setTagFilter(tag, TagFilterType.BOOSTED)
            showFilterSheet = true
            snackbarHostState.showSnackbar(ForYouCopy.showingMore(TagNormalizer.getDisplayName(tag)), duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    fun open(url: String, apiName: String) {
        viewModel.onRecommendationClicked(url)
        onNavigateToDetails(url, apiName)
    }

    fun notForMe(rec: Recommendation) {
        viewModel.hideNovel(rec.novel.url, rec.novel.name)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                ForYouCopy.notForMeToast(rec.novel.name), ForYouCopy.UNDO, duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.unhideNovel(rec.novel.url)
        }
    }

    val sections = remember(uiState) {
        val genres = (uiState.topPreferences.mapNotNull(TagNormalizer::normalize) +
            OnboardingGenres.mainGenres.map { it.category }).distinct()
        buildForYouSections(uiState, genres)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AlignedLargeTopBar(
                title = { Text(ForYouCopy.SCREEN_TITLE) },
                subtitle = when {
                    uiState.isSeeding || uiState.isLoading -> { { Text(ForYouCopy.LOADING_SUBTITLE) } }
                    uiState.librarySourceNovels.isNotEmpty() -> { { Text(ForYouCopy.leadPickedFrom(uiState.librarySourceNovels.size)) } }
                    else -> null
                },
                actions = {
                    ForYouMenu(
                        hiddenNovels = hiddenNovels.size,
                        hiddenAuthors = blockedAuthors.size,
                        useAllSources = uiState.showCrossProvider,
                        aiOn = uiState.hasGeminiKey,
                        onRefresh = { viewModel.refresh() },
                        onTaste = { showGenrePrefsSheet = true },
                        onShowMoreLess = { showFilterSheet = true },
                        onHidden = { showHiddenSheet = true },
                        onToggleAllSources = { viewModel.toggleCrossProvider() },
                        onAiPicks = onNavigateToSettings
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                uiState.isSeeding -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ForYouFirstLoad(uiState.seedingProgress)
                }
                uiState.isLoading -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ForYouLoading()
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                    sections.forEach { section ->
                        forYouSection(
                            section = section,
                            uiState = uiState,
                            onOpen = { open(it.url, it.apiName) },
                            onOpenRec = { open(it.novel.url, it.novel.apiName) },
                            onLongPress = { selectedRecommendation = it },
                            onSeeAll = { title, key -> seeAll = title to key },
                            onSelectSource = { novel ->
                                viewModel.selectSourceNovel(
                                    uiState.librarySourceNovels.find { it.novel.url == novel.url }
                                        ?: LibrarySourceNovel(novel, lastReadAt = 0L, chaptersRead = 0, totalChapters = 0, readingProgress = 0f)
                                )
                            },
                            onDismissHint = { viewModel.dismissLongPressHint() },
                            onAiSettings = onNavigateToSettings,
                            onAiRetry = {
                                viewModel.loadAiRecommendations(force = true)
                                viewModel.loadAiTrendingRecommendations(force = true)
                            },
                            onGenre = onNavigateToTagExplorer,
                            onFindNovels = onNavigateToBrowse
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        TagFilterSheet(
            tagFilters = tagFilters,
            onSetFilter = { tag, type -> viewModel.setTagFilter(tag, type) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showHiddenSheet) {
        RecommendationSettingsSheet(
            hiddenNovels = hiddenNovels,
            blockedAuthors = blockedAuthors,
            onUnhideNovel = { viewModel.unhideNovel(it) },
            onUnblockAuthor = { viewModel.unblockAuthor(it) },
            onClearAllHidden = { viewModel.clearAllHiddenNovels() },
            onClearAllBlocked = { viewModel.clearAllBlockedAuthors() },
            onDismiss = { showHiddenSheet = false }
        )
    }

    seeAll?.let { (title, key) ->
        // Live from the current sections, so a novel hidden from the sheet leaves it too.
        val items = when (val s = sections.firstOrNull { it.key == key }) {
            is ForYouSection.Row -> s.items
            is ForYouSection.AiRow -> s.items
            else -> emptyList()
        }
        SeeAllSheet(
            title = title,
            items = items,
            onOpen = { seeAll = null; open(it.url, it.apiName) },
            onLongPress = { selectedRecommendation = it.recommendation },
            onDismiss = { seeAll = null }
        )
    }

    selectedRecommendation?.let { recommendation ->
        var authorInfo by remember(recommendation.novel.url) { mutableStateOf<Pair<String, String>?>(null) }
        LaunchedEffect(recommendation.novel.url) {
            authorInfo = viewModel.getAuthorForNovel(recommendation.novel.url)
        }

        NovelActionMenu(
            recommendation = recommendation,
            onDismiss = { selectedRecommendation = null },
            onNotInterested = { notForMe(recommendation) },
            onBlockAuthor = authorInfo?.let { (normalized, display) ->
                {
                    viewModel.blockAuthor(normalized, display)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            ForYouCopy.hideAuthorToast(display), ForYouCopy.UNDO, duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.unblockAuthor(normalized)
                    }
                }
            },
            onViewDetails = { open(recommendation.novel.url, recommendation.novel.apiName) },
            onAddToLibrary = { viewModel.addToLibrary(recommendation) }
        )
    }

    if (showGenrePrefsSheet) {
        GenrePreferencesSheet(
            viewModel = viewModel,
            onDismiss = { showGenrePrefsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.forYouSection(
    section: ForYouSection,
    uiState: RecommendationUiState,
    onOpen: (ForYouItem) -> Unit,
    onOpenRec: (Recommendation) -> Unit,
    onLongPress: (Recommendation) -> Unit,
    onSeeAll: (title: String, sectionKey: String) -> Unit,
    onSelectSource: (Novel) -> Unit,
    onDismissHint: () -> Unit,
    onAiSettings: () -> Unit,
    onAiRetry: () -> Unit,
    onGenre: (TagNormalizer.TagCategory) -> Unit,
    onFindNovels: () -> Unit
) {
    val longPressItem: (ForYouItem) -> Unit = { it.recommendation?.let(onLongPress) }
    when (section) {
        is ForYouSection.Carousel -> {
            item(key = section.key) { ForYouCarousel(section.items, onOpenRec, onLongPress) }
            if (uiState.showLongPressHint) item(key = "hint") { LongPressHint(onDismissHint) }
        }

        is ForYouSection.MoreLike -> {
            item(key = "${section.key}_header") {
                MoreLikeHeader(section.source, section.options, onSelectSource)
            }
            item(key = section.key) {
                when {
                    uiState.isLoadingSourceRecommendations ->
                        Box(Modifier.fillMaxWidth().height(156.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    section.items.isEmpty() -> EmptyNote(ForYouCopy.nothingLike(section.source.name), ForYouCopy.NOTHING_LIKE_BODY)
                    else -> ForYouCoverRow(section.items.map { it.toCoverItem() }, onOpen, longPressItem)
                }
            }
        }

        is ForYouSection.Row -> {
            item(key = "${section.key}_header") {
                ForYouRowHeader(
                    title = section.title,
                    subtitle = section.subtitle,
                    onSeeAll = section.seeAllTag?.let { tag -> { onGenre(tag) } }
                        ?: { onSeeAll(section.title, section.key) }
                )
            }
            item(key = section.key) { ForYouCoverRow(section.items, onOpen, longPressItem) }
        }

        is ForYouSection.AiRow -> {
            item(key = "${section.key}_header") {
                ForYouRowHeader(
                    title = ForYouCopy.PICKED_BY_AI,
                    subtitle = null,
                    onSeeAll = if (section.items.isNotEmpty()) ({ onSeeAll(ForYouCopy.PICKED_BY_AI, section.key) }) else null,
                    leading = {
                        Icon(
                            Icons.Rounded.AutoAwesome, null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp)
                        )
                    }
                )
            }
            item(key = section.key) {
                when {
                    section.items.isNotEmpty() -> ForYouCoverRow(section.items, onOpen, longPressItem)
                    section.state == AiState.Off -> AiOffRow(onAiSettings)
                    section.state == AiState.Loading -> AiNoticeRow(ForYouCopy.AI_LOADING, null, {})
                    else -> AiNoticeRow(
                        uiState.aiRecsError ?: uiState.trendingError ?: ForYouCopy.AI_UNREADABLE,
                        ForYouCopy.TRY_AGAIN,
                        onAiRetry
                    )
                }
            }
        }

        is ForYouSection.BrowseGenres -> {
            item(key = "${section.key}_header") { ForYouRowHeader(ForYouCopy.BROWSE_BY_GENRE, null, null) }
            item(key = section.key) { GenreShapes(section.tags, onGenre) }
        }

        ForYouSection.FirstTimeEmpty -> item(key = section.key) { ForYouFirstTime(onFindNovels) }
    }
}

@Composable
private fun LongPressHint(onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp)
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TouchApp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                ForYouCopy.HINT_LONG_PRESS,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            TextButton(onClick = onDismiss) { Text(ForYouCopy.HINT_DISMISS, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun EmptyNote(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
