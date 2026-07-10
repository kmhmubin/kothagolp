package com.kmhmubin.kothagolp.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.zIndex
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.ui.adaptive.isTabletUi
import com.kmhmubin.kothagolp.ui.components.KothagolpBottomNavBarWithInsets
import com.kmhmubin.kothagolp.ui.components.KothagolpNavigationRail
import com.kmhmubin.kothagolp.ui.navigation.HomeTabs
import com.kmhmubin.kothagolp.ui.screens.home.shared.LibraryStateHolder
import com.kmhmubin.kothagolp.ui.screens.home.shared.RecommendationNavigationHelper
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.BrowseTab
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryTab
import com.kmhmubin.kothagolp.ui.screens.home.tabs.library.LibraryTab
import com.kmhmubin.kothagolp.ui.screens.home.tabs.more.MoreTab
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.RecommendationTab

@Composable
fun HomeScreen(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProviderBrowse: (providerName: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToTagExplorer: (TagNormalizer.TagCategory) -> Unit = {},
    onNavigateToMigration: (() -> Unit)? = null,
    onNavigateToGlobalSearch: ((query: String) -> Unit)? = null,
    onNavigateToNotesHighlights: (() -> Unit)? = null,
    onNavigateToForYouSettings: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        LibraryStateHolder.initialize()
    }

    // rememberSaveable preserves selected tab across process death/recreation.
    var currentTabIndex by rememberSaveable { mutableIntStateOf(HomeTabs.LIBRARY.ordinal) }
    val currentTab = HomeTabs.entries[currentTabIndex]

    // All tab states are kept alive inside this holder — tab composables never leave composition.
    val savedStateHolder = rememberSaveableStateHolder()

    // shouldNavigateToForYou is a Compose mutableStateOf, so this LaunchedEffect re-runs
    // reactively whenever any code calls RecommendationNavigationHelper.navigateWithTag().
    LaunchedEffect(RecommendationNavigationHelper.shouldNavigateToForYou) {
        if (RecommendationNavigationHelper.consumeNavigationRequest()) {
            currentTabIndex = HomeTabs.FOR_YOU.ordinal
        }
    }

    BackHandler(enabled = currentTab != HomeTabs.LIBRARY) {
        currentTabIndex = HomeTabs.LIBRARY.ordinal
    }

    // Incremented when the Library nav button is pressed while already on Library —
    // LibraryTab observes this to open its options sheet (Komikku behavior).
    var libraryOptionsRequestId by rememberSaveable { mutableIntStateOf(0) }

    fun onTabSelected(route: String) {
        HomeTabs.fromRoute("tab_$route")?.let { tab ->
            if (tab == HomeTabs.LIBRARY && currentTab == HomeTabs.LIBRARY) {
                libraryOptionsRequestId++
            }
            currentTabIndex = tab.ordinal
        }
    }

    val isTablet = isTabletUi()

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            KothagolpNavigationRail(
                selectedRoute = currentTab.route,
                onItemSelected = ::onTabSelected,
                modifier = Modifier.fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End
                        )
                    )
            ) {
                PersistentTabContent(
                    currentTab = currentTab,
                    savedStateHolder = savedStateHolder,
                    appSettings = appSettings,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToReader = onNavigateToReader,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToProviderBrowse = onNavigateToProviderBrowse,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onNavigateToAbout = onNavigateToAbout,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onNavigateToTagExplorer = onNavigateToTagExplorer,
                    onNavigateToMigration = onNavigateToMigration,
                    onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                    onNavigateToNotesHighlights = onNavigateToNotesHighlights,
                    onNavigateToForYouSettings = onNavigateToForYouSettings,
                    libraryOptionsRequestId = libraryOptionsRequestId,
                    onSwitchTab = { currentTabIndex = it.ordinal }
                )
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                KothagolpBottomNavBarWithInsets(
                    selectedRoute = currentTab.route,
                    onItemSelected = ::onTabSelected
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PersistentTabContent(
                    currentTab = currentTab,
                    savedStateHolder = savedStateHolder,
                    appSettings = appSettings,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToReader = onNavigateToReader,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToProviderBrowse = onNavigateToProviderBrowse,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onNavigateToAbout = onNavigateToAbout,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onNavigateToTagExplorer = onNavigateToTagExplorer,
                    onNavigateToMigration = onNavigateToMigration,
                    onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                    onNavigateToNotesHighlights = onNavigateToNotesHighlights,
                    onNavigateToForYouSettings = onNavigateToForYouSettings,
                    libraryOptionsRequestId = libraryOptionsRequestId,
                    onSwitchTab = { currentTabIndex = it.ordinal }
                )
            }
        }
    }
}

/**
 * Renders all 5 tabs simultaneously. Inactive tabs are hidden via graphicsLayer alpha
 * (GPU skips their rendering) and drawn BEFORE the active tab. The active tab is always
 * the last child in the Box, so it sits on top in z-order and receives all touch events
 * first — no explicit touch blocking needed. Tab switching is an instant alpha change;
 * no Compose tree rebuild, no ViewModel re-init, no LazyColumn re-measure.
 */
@Composable
private fun PersistentTabContent(
    currentTab: HomeTabs,
    savedStateHolder: SaveableStateHolder,
    appSettings: AppSettings,
    onNavigateToDetails: (String, String) -> Unit,
    onNavigateToReader: (String, String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProviderBrowse: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToTagExplorer: (TagNormalizer.TagCategory) -> Unit,
    onNavigateToMigration: (() -> Unit)?,
    onNavigateToGlobalSearch: ((String) -> Unit)?,
    onNavigateToNotesHighlights: (() -> Unit)?,
    onNavigateToForYouSettings: () -> Unit = {},
    libraryOptionsRequestId: Int = 0,
    onSwitchTab: (HomeTabs) -> Unit
) {
    @Composable
    fun TabItemContent(tab: HomeTabs) {
        when (tab) {
            HomeTabs.LIBRARY -> LibraryTab(
                appSettings = appSettings,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToReader = onNavigateToReader,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                optionsSheetRequestId = libraryOptionsRequestId
            )
            HomeTabs.BROWSE -> BrowseTab(
                appSettings = appSettings,
                onNavigateToProvider = onNavigateToProviderBrowse,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToReader = onNavigateToReader,
                onNavigateToMigration = onNavigateToMigration
            )
            HomeTabs.FOR_YOU -> RecommendationTab(
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToBrowse = { onSwitchTab(HomeTabs.BROWSE) },
                onNavigateToTagExplorer = onNavigateToTagExplorer,
                onNavigateToSettings = onNavigateToForYouSettings
            )
            HomeTabs.HISTORY -> HistoryTab(
                appSettings = appSettings,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToReader = onNavigateToReader
            )
            HomeTabs.MORE -> MoreTab(
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToAbout = { onNavigateToAbout() },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNotesHighlights = onNavigateToNotesHighlights
            )
        }
    }

    // All tabs stay in composition (no VM re-init, no LazyColumn re-measure).
    // Active tab gets zIndex(1f) for correct touch routing during crossfade.
    HomeTabs.entries.forEachIndexed { index, tab ->
        val isActive = tab == currentTab
        val alpha by animateFloatAsState(
            targetValue = if (isActive) 1f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "tab_alpha_$index"
        )
        savedStateHolder.SaveableStateProvider(tab.route) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isActive) 1f else 0f)
                    .graphicsLayer { this.alpha = alpha }
            ) {
                TabItemContent(tab)
            }
        }
    }
}
