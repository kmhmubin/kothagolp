package com.kmhmubin.kothagolp.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.ui.adaptive.isTabletUi
import com.kmhmubin.kothagolp.ui.components.KothagolpBottomNavBarWithInsets
import com.kmhmubin.kothagolp.ui.components.KothagolpNavigationRail
import com.kmhmubin.kothagolp.ui.navigation.HomeTabs
import com.kmhmubin.kothagolp.ui.navigation.rememberTabNavigationState
import com.kmhmubin.kothagolp.ui.screens.home.shared.LibraryStateHolder
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
    onNavigateToTagExplorer: (TagNormalizer.TagCategory) -> Unit = {}
) {
    // Initialize shared state
    LaunchedEffect(Unit) {
        LibraryStateHolder.initialize()
    }

    val tabNavState = rememberTabNavigationState()
    val navBackStackEntry by tabNavState.navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = currentRoute?.let { HomeTabs.fromRoute(it) } ?: HomeTabs.LIBRARY

    // Handle navigation to For You tab with tag filter
    LaunchedEffect(currentRoute) {
        if (com.kmhmubin.kothagolp.ui.screens.home.shared.RecommendationNavigationHelper.consumeNavigationRequest()) {
            // Switch to For You tab
            tabNavState.navigateToTab(HomeTabs.FOR_YOU)
        }
    }

    // Handle system back button
    BackHandler(enabled = currentTab != HomeTabs.LIBRARY) {
        tabNavState.navigateToTab(HomeTabs.LIBRARY)
    }

    val isTablet = isTabletUi()

    fun onTabSelected(route: String) {
        val tabRoute = "tab_$route"
        HomeTabs.fromRoute(tabRoute)?.let { tab ->
            tabNavState.navigateToTab(tab)
        }
    }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            KothagolpNavigationRail(
                selectedRoute = currentRoute ?: HomeTabs.LIBRARY.route,
                onItemSelected = ::onTabSelected,
                modifier = Modifier.fillMaxHeight()
            )
            NavHost(
                navController = tabNavState.navController,
                startDestination = HomeTabs.LIBRARY.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(HomeTabs.LIBRARY.route) {
                    LibraryTab(
                        appSettings = appSettings,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader,
                        onNavigateToNotifications = onNavigateToNotifications
                    )
                }

                composable(HomeTabs.BROWSE.route) {
                    BrowseTab(
                        appSettings = appSettings,
                        onNavigateToProvider = onNavigateToProviderBrowse,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader
                    )
                }

                composable(HomeTabs.FOR_YOU.route) {
                    RecommendationTab(
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToBrowse = {
                            tabNavState.navigateToTab(HomeTabs.BROWSE)
                        },
                        onNavigateToOnboarding = onNavigateToOnboarding,
                        onNavigateToTagExplorer = onNavigateToTagExplorer
                    )
                }

                composable(HomeTabs.HISTORY.route) {
                    HistoryTab(
                        appSettings = appSettings,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToReader = onNavigateToReader
                    )
                }

                composable(HomeTabs.MORE.route) {
                    MoreTab(
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToDownloads = onNavigateToDownloads,
                        onNavigateToAbout = { onNavigateToAbout() },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToStorage = onNavigateToStorage
                    )
                }
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                KothagolpBottomNavBarWithInsets(
                    selectedRoute = currentRoute ?: HomeTabs.LIBRARY.route,
                    onItemSelected = ::onTabSelected
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = tabNavState.navController,
                    startDestination = HomeTabs.LIBRARY.route
                ) {
                    composable(HomeTabs.LIBRARY.route) {
                        LibraryTab(
                            appSettings = appSettings,
                            onNavigateToDetails = onNavigateToDetails,
                            onNavigateToReader = onNavigateToReader,
                            onNavigateToNotifications = onNavigateToNotifications
                        )
                    }

                    composable(HomeTabs.BROWSE.route) {
                        BrowseTab(
                            appSettings = appSettings,
                            onNavigateToProvider = onNavigateToProviderBrowse,
                            onNavigateToDetails = onNavigateToDetails,
                            onNavigateToReader = onNavigateToReader
                        )
                    }

                    composable(HomeTabs.FOR_YOU.route) {
                        RecommendationTab(
                            onNavigateToDetails = onNavigateToDetails,
                            onNavigateToBrowse = {
                                tabNavState.navigateToTab(HomeTabs.BROWSE)
                            },
                            onNavigateToOnboarding = onNavigateToOnboarding,
                            onNavigateToTagExplorer = onNavigateToTagExplorer
                        )
                    }

                    composable(HomeTabs.HISTORY.route) {
                        HistoryTab(
                            appSettings = appSettings,
                            onNavigateToDetails = onNavigateToDetails,
                            onNavigateToReader = onNavigateToReader
                        )
                    }

                    composable(HomeTabs.MORE.route) {
                        MoreTab(
                            onNavigateToProfile = onNavigateToProfile,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToAbout = { onNavigateToAbout() },
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToStorage = onNavigateToStorage
                        )
                    }
                }
            }
        }
    }
}