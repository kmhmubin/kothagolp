package com.kmhmubin.kothagolp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kmhmubin.kothagolp.data.backup.BackupManager
import com.kmhmubin.kothagolp.data.cache.CacheManager
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.data.sync.SyncManager
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.ui.screens.about.AboutScreen
import com.kmhmubin.kothagolp.ui.screens.details.DetailsScreen
import com.kmhmubin.kothagolp.ui.screens.downloads.DownloadsScreen
import com.kmhmubin.kothagolp.ui.screens.home.HomeScreen
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.ProviderBrowseScreen
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.ProviderWebViewScreen
import com.kmhmubin.kothagolp.ui.screens.notification.NotificationScreen
import com.kmhmubin.kothagolp.ui.screens.onboarding.OnboardingScreen
import com.kmhmubin.kothagolp.ui.screens.profile.ProfileScreen
import com.kmhmubin.kothagolp.ui.screens.reader.ReaderScreen
import com.kmhmubin.kothagolp.ui.screens.reader.settings.ReaderSettingsScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsAboutScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsAppearanceScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsBrowseScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsLibraryScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsReaderPrefsScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsScreen
import com.kmhmubin.kothagolp.ui.screens.settings.SettingsSourcesScreen
import com.kmhmubin.kothagolp.ui.screens.settings.StorageScreen
import com.kmhmubin.kothagolp.ui.screens.migration.MigrationBulkScreen
import com.kmhmubin.kothagolp.ui.screens.migration.MigrationNovelsScreen
import com.kmhmubin.kothagolp.ui.screens.migration.MigrationSearchScreen
import com.kmhmubin.kothagolp.ui.screens.migration.MigrationSourcesScreen
import com.kmhmubin.kothagolp.ui.screens.tagexplorer.TagExplorerScreen

@Composable
fun KothagolpNavGraph(
    navController: NavHostController,
    appSettings: AppSettings
) {
    // Check if onboarding is needed
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val needsOnboarding = remember {
        preferencesManager.isFirstRun() || !preferencesManager.hasCompletedOnboarding()
    }

    val startDestination = if (needsOnboarding) {
        NavRoutes.Onboarding.route
    } else {
        NavRoutes.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ================================================================
        // ONBOARDING
        // ================================================================
        composable(route = NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // HOME (with nested tab navigation)
        // ================================================================
        composable(route = NavRoutes.Home.route) {
            HomeScreen(
                appSettings = appSettings,
                onNavigateToDetails = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                },
                onNavigateToReader = { chapterUrl, novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName)
                    )
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                onNavigateToProviderBrowse = { providerName ->
                    navController.navigate(
                        NavRoutes.ProviderBrowse.createRoute(providerName)
                    )
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.Notifications.route)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.Profile.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(NavRoutes.Downloads.route)
                },
                onNavigateToAbout = {
                    navController.navigate(NavRoutes.About.route)
                },
                onNavigateToStorage = {
                    navController.navigate(NavRoutes.Storage.route)
                },
                onNavigateToOnboarding = {
                    navController.navigate(NavRoutes.Onboarding.route)
                },
                onNavigateToTagExplorer = { tagCategory ->
                    navController.navigate(
                        NavRoutes.TagExplorer.createRoute(tagCategory)
                    )
                },
                onNavigateToMigration = {
                    navController.navigate(NavRoutes.MigrationSources.route)
                }
            )
        }


        // ================================================================
        // NOTIFICATIONS
        // ================================================================
        composable(route = NavRoutes.Notifications.route) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReader = { chapterUrl, novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName)
                    )
                },
                onNavigateToDetails = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // SETTINGS
        // ================================================================
        composable(route = NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateTo = { route -> navController.navigate(route) }
            )
        }

        composable(route = NavRoutes.SettingsAppearance.route) {
            SettingsAppearanceScreen(onBack = { navController.popBackStack() })
        }

        composable(route = NavRoutes.SettingsLibrary.route) {
            SettingsLibraryScreen(onBack = { navController.popBackStack() })
        }

        composable(route = NavRoutes.SettingsBrowse.route) {
            SettingsBrowseScreen(onBack = { navController.popBackStack() })
        }

        composable(route = NavRoutes.SettingsReader.route) {
            SettingsReaderPrefsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReaderSettings = { navController.navigate(NavRoutes.ReaderSettings.route) }
            )
        }

        composable(route = NavRoutes.SettingsSources.route) {
            SettingsSourcesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMigration = {
                    navController.navigate(NavRoutes.MigrationSources.route)
                }
            )
        }

        composable(route = NavRoutes.SettingsAbout.route) {
            SettingsAboutScreen(onBack = { navController.popBackStack() })
        }

        // ================================================================
        // STORAGE & BACKUP
        // ================================================================
        composable(route = NavRoutes.Storage.route) {
            val context = LocalContext.current
            val database = remember { NovelDatabase.getInstance(context) }
            val prefsManager = remember { RepositoryProvider.getPreferencesManager() }
            val cacheManager = remember { CacheManager(context, database) }
            val backupManager = remember { BackupManager(context, database, prefsManager) }
            val syncManager = remember { SyncManager(context, prefsManager, backupManager) }

            StorageScreen(
                cacheManager = cacheManager,
                backupManager = backupManager,
                preferencesManager = prefsManager,
                syncManager = syncManager,
                onBack = { navController.popBackStack() }
            )
        }

        // ================================================================
        // SOURCE MIGRATION
        // ================================================================
        composable(route = NavRoutes.MigrationSources.route) {
            MigrationSourcesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToNovels = { sourceName ->
                    navController.navigate(NavRoutes.MigrationNovels.createRoute(sourceName))
                }
            )
        }

        composable(
            route = NavRoutes.MigrationNovels.route,
            arguments = listOf(navArgument("sourceName") { type = NavType.StringType })
        ) {
            MigrationNovelsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSearch = { novelUrl, sourceName ->
                    navController.navigate(
                        NavRoutes.MigrationSearch.createRoute(novelUrl, sourceName)
                    )
                },
                onNavigateToBulk = { sourceName ->
                    navController.navigate(NavRoutes.MigrationBulk.createRoute(sourceName))
                }
            )
        }

        composable(
            route = NavRoutes.MigrationSearch.route,
            arguments = listOf(
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("sourceName") { type = NavType.StringType }
            )
        ) {
            MigrationSearchScreen(
                onBack = { navController.popBackStack() },
                onMigrationComplete = {
                    navController.popBackStack(NavRoutes.MigrationSources.route, inclusive = true)
                }
            )
        }

        composable(
            route = NavRoutes.MigrationBulk.route,
            arguments = listOf(navArgument("sourceName") { type = NavType.StringType })
        ) {
            MigrationBulkScreen(
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.popBackStack(NavRoutes.MigrationSources.route, inclusive = true)
                }
            )
        }

        // ================================================================
        // PROFILE
        // ================================================================
        composable(route = NavRoutes.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNovelClick = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // DOWNLOADS
        // ================================================================
        composable(route = NavRoutes.Downloads.route) {
            DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onNovelClick = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // ABOUT
        // ================================================================
        composable(route = NavRoutes.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ================================================================
        // PROVIDER BROWSE (novels from specific provider)
        // ================================================================
        composable(
            route = NavRoutes.ProviderBrowse.route,
            arguments = listOf(
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            ProviderBrowseScreen(
                providerName = providerName,
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
                onNavigateToDetails = { novelUrl, provider ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, provider)
                    )
                },
                onNavigateToReader = { chapterUrl, novelUrl, provider ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, novelUrl, provider)
                    )
                },
                onNavigateToWebView = { provider, url ->
                    navController.navigate(
                        NavRoutes.ProviderWebView.createRoute(provider, url)
                    )
                }
            )
        }

        // ================================================================
        // PROVIDER WEBVIEW (for Cloudflare bypass, manual browsing)
        // ================================================================
        composable(
            route = NavRoutes.ProviderWebView.route,
            arguments = listOf(
                navArgument("providerName") { type = NavType.StringType },
                navArgument("initialUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("initialUrl") ?: ""

            val providerName = NavRoutes.decodeUrl(encodedProvider)
            val initialUrl = NavRoutes.decodeUrl(encodedUrl).takeIf { it.isNotBlank() }

            ProviderWebViewScreen(
                providerName = providerName,
                initialUrl = initialUrl,
                onBack = { navController.popBackStack() },
                onOpenNovelInApp = { novelUrl ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    ) {
                        popUpTo(NavRoutes.ProviderWebView.route) { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // DETAILS
        // ================================================================
        composable(
            route = NavRoutes.Details.route,
            arguments = listOf(
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("novelUrl") ?: ""
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""

            val novelUrl = NavRoutes.decodeUrl(encodedUrl)
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            DetailsScreen(
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() },
                onChapterClick = { chapterUrl, nUrl, provider ->
                    navController.navigate(
                        NavRoutes.Reader.createRoute(chapterUrl, nUrl, provider)
                    )
                },
                onNovelClick = { relatedNovelUrl, relatedProviderName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(relatedNovelUrl, relatedProviderName)
                    )
                },
                onOpenInWebView = { provider, url ->
                    navController.navigate(
                        NavRoutes.ProviderWebView.createRoute(provider, url)
                    )
                },
                onNavigateToDownloads = {
                    navController.navigate(NavRoutes.Downloads.route)
                },
                onNavigateToTagExplorer = { tagCategory -> // NEW
                    navController.navigate(
                        NavRoutes.TagExplorer.createRoute(tagCategory)
                    )
                },
                onNavigateToMigration = { nUrl, sourceName ->
                    navController.navigate(
                        NavRoutes.MigrationSearch.createRoute(nUrl, sourceName)
                    )
                }
            )
        }

        // ================================================================
        // TAG EXPLORER
        // ================================================================
        composable(
            route = NavRoutes.TagExplorer.route,
            arguments = listOf(
                navArgument("tagName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tagName = backStackEntry.arguments?.getString("tagName") ?: ""

            TagExplorerScreen(
                tagName = tagName,
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
                onNovelClick = { novelUrl, providerName ->
                    navController.navigate(
                        NavRoutes.Details.createRoute(novelUrl, providerName)
                    )
                }
            )
        }

        // ================================================================
        // READER
        // ================================================================
        composable(
            route = NavRoutes.Reader.route,
            arguments = listOf(
                navArgument("chapterUrl") { type = NavType.StringType },
                navArgument("novelUrl") { type = NavType.StringType },
                navArgument("providerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedChapterUrl = backStackEntry.arguments?.getString("chapterUrl") ?: ""
            val encodedNovelUrl = backStackEntry.arguments?.getString("novelUrl") ?: ""
            val encodedProvider = backStackEntry.arguments?.getString("providerName") ?: ""

            val chapterUrl = NavRoutes.decodeUrl(encodedChapterUrl)
            val novelUrl = NavRoutes.decodeUrl(encodedNovelUrl)
            val providerName = NavRoutes.decodeUrl(encodedProvider)

            ReaderScreen(
                chapterUrl = chapterUrl,
                novelUrl = novelUrl,
                providerName = providerName,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.ReaderSettings.route)
                }
            )
        }

        // ================================================================
        // READER SETTINGS
        // ================================================================
        composable(route = NavRoutes.ReaderSettings.route) {
            ReaderSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
