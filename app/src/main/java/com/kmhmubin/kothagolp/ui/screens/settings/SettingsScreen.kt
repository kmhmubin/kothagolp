package com.kmhmubin.kothagolp.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.ViewComfy
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material.icons.outlined.ViewCozy
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.domain.model.CustomThemeColors
import com.kmhmubin.kothagolp.domain.model.DisplayMode
import com.kmhmubin.kothagolp.domain.model.GridColumns
import com.kmhmubin.kothagolp.domain.model.LibraryFilter
import com.kmhmubin.kothagolp.domain.model.LibrarySortOrder
import com.kmhmubin.kothagolp.domain.model.RatingFormat
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.domain.model.ThemeMode
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.source.SourceLoader
import com.kmhmubin.kothagolp.source.SourceSyncWorker
import com.kmhmubin.kothagolp.ui.components.ColorPickerDialog
import com.kmhmubin.kothagolp.ui.navigation.NavRoutes
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════
// MAIN SETTINGS SCREEN  (navigation hub)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Palette,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Appearance",
                            subtitle = "Theme, colors and display layout",
                            onClick = { onNavigateTo(NavRoutes.SettingsAppearance.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                            iconTint = Color(0xFF3B82F6),
                            title = "Library",
                            subtitle = "Shelves, sorting and visibility",
                            onClick = { onNavigateTo(NavRoutes.SettingsLibrary.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Search,
                            iconTint = Color(0xFF06B6D4),
                            title = "Browse & Downloads",
                            subtitle = "Search, ratings and auto-downloads",
                            onClick = { onNavigateTo(NavRoutes.SettingsBrowse.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            iconTint = Color(0xFF8B5CF6),
                            title = "Reader",
                            subtitle = "Reading experience and preferences",
                            onClick = { onNavigateTo(NavRoutes.SettingsReader.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Extension,
                            iconTint = Color(0xFFF97316),
                            title = "Sources",
                            subtitle = "Manage sources and updates",
                            onClick = { onNavigateTo(NavRoutes.SettingsSources.route) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Storage,
                            iconTint = Color(0xFF22C55E),
                            title = "Data",
                            subtitle = "Storage, cache and backup",
                            onClick = { onNavigateTo(NavRoutes.Storage.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Info,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "About",
                            subtitle = "App information and reset",
                            onClick = { onNavigateTo(NavRoutes.SettingsAbout.route) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// APPEARANCE  (Theme + Layout)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Theme", Icons.Outlined.Palette) }
            item {
                SettingsCard {
                    SettingsLabel("Theme Mode", Icons.Outlined.DarkMode)
                    Spacer(Modifier.height(12.dp))
                    ThemeModeSelector(
                        selected = settings.themeMode,
                        onSelect = { preferencesManager.updateThemeMode(it) }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.Contrast,
                        title = "AMOLED Black",
                        subtitle = "Deeper blacks on OLED displays",
                        checked = settings.amoledBlack,
                        enabled = settings.themeMode != ThemeMode.LIGHT,
                        onCheckedChange = { preferencesManager.updateAmoledBlack(it) }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Pull colors from your wallpaper",
                        checked = settings.useDynamicColor,
                        enabled = !settings.useCustomTheme,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(useDynamicColor = it))
                        }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.Palette,
                        title = "Custom Theme",
                        subtitle = "Set your own color scheme",
                        checked = settings.useCustomTheme,
                        highlight = true,
                        onCheckedChange = { preferencesManager.updateUseCustomTheme(it) }
                    )
                    AnimatedVisibility(
                        visible = settings.useCustomTheme,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            CustomThemeSection(
                                colors = settings.customThemeColors,
                                onColorsChange = { preferencesManager.updateCustomThemeColors(it) }
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Layout", Icons.Outlined.ViewCompact) }
            item {
                SettingsCard {
                    SettingsLabel("UI Density", Icons.Outlined.SpaceDashboard)
                    Spacer(Modifier.height(12.dp))
                    DensitySelector(
                        selected = settings.uiDensity,
                        onSelect = { preferencesManager.updateDensity(it) }
                    )
                    SettingsDivider()
                    SettingsLabel("Display Mode", Icons.Outlined.ViewModule)
                    Spacer(Modifier.height(8.dp))
                    DisplayModeRow("Library", settings.libraryDisplayMode) {
                        preferencesManager.updateLibraryDisplayMode(it)
                    }
                    DisplayModeRow("Browse", settings.browseDisplayMode) {
                        preferencesManager.updateBrowseDisplayMode(it)
                    }
                    DisplayModeRow("Search", settings.searchDisplayMode) {
                        preferencesManager.updateSearchDisplayMode(it)
                    }
                    SettingsDivider()
                    SettingsLabel("Grid Columns", Icons.Outlined.GridView)
                    Spacer(Modifier.height(8.dp))
                    GridColumnsRow("Library", settings.libraryGridColumns) {
                        preferencesManager.updateLibraryGridColumns(it)
                    }
                    GridColumnsRow("Browse", settings.browseGridColumns) {
                        preferencesManager.updateBrowseGridColumns(it)
                    }
                    GridColumnsRow("Search", settings.searchGridColumns) {
                        preferencesManager.updateSearchGridColumns(it)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLibraryScreen(onBack: () -> Unit) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Library", Icons.AutoMirrored.Outlined.LibraryBooks) }
            item {
                SettingsCard {
                    val defaultFilterOptions = LibraryFilter.standardOptions(settings.enabledLibraryFilters)

                    ToggleItem(
                        icon = Icons.Outlined.Badge,
                        title = "Show Badges",
                        subtitle = "Show badge counts on library covers",
                        checked = settings.showBadges,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(showBadges = it))
                        }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.VisibilityOff,
                        title = "Hide Spicy Shelf",
                        subtitle = "Hidden from your Library until you double-tap All",
                        checked = settings.hideSpicyLibraryContent,
                        onCheckedChange = {
                            if (it) {
                                preferencesManager.setSpicyShelfRevealed(false)
                            }
                            preferencesManager.updateAppSettings(
                                settings.copy(hideSpicyLibraryContent = it)
                            )
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Outlined.FilterList,
                        title = "Default Filter",
                        selectedValue = settings.defaultLibraryFilter.displayName(),
                        options = defaultFilterOptions.map { it.displayName() },
                        selectedIndex = defaultFilterOptions
                            .indexOf(settings.defaultLibraryFilter)
                            .coerceAtLeast(0),
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(defaultLibraryFilter = defaultFilterOptions[it])
                            )
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        title = "Default Sort",
                        selectedValue = settings.defaultLibrarySort.displayName(),
                        options = LibrarySortOrder.values().map { it.displayName() },
                        selectedIndex = settings.defaultLibrarySort.ordinal,
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(defaultLibrarySort = LibrarySortOrder.values()[it])
                            )
                        }
                    )
                }
            }
            item {
                LibraryShelfCard(
                    settings = settings,
                    onShelfEnabledChange = { filter, enabled ->
                        preferencesManager.setLibraryShelfEnabled(filter, enabled)
                    }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BROWSE & DOWNLOADS
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBrowseScreen(onBack: () -> Unit) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse & Downloads", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Browse & Search", Icons.Outlined.Search) }
            item {
                SettingsCard {
                    SliderItem(
                        icon = Icons.Outlined.Numbers,
                        title = "Search Results",
                        subtitle = "${settings.searchResultsPerProvider} results per source",
                        value = settings.searchResultsPerProvider.toFloat(),
                        valueRange = 4f..12f,
                        steps = 7,
                        valueLabel = settings.searchResultsPerProvider.toString(),
                        onValueChange = {
                            preferencesManager.updateAppSettings(
                                settings.copy(searchResultsPerProvider = it.toInt())
                            )
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Outlined.Star,
                        title = "Rating Format",
                        selectedValue = settings.ratingFormat.shortDisplayName(),
                        options = RatingFormat.values().map { it.displayName() },
                        selectedIndex = settings.ratingFormat.ordinal,
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(ratingFormat = RatingFormat.values()[it])
                            )
                        }
                    )
                }
            }

            item { SectionHeader("Auto-Download", Icons.Rounded.CloudDownload) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.DownloadForOffline,
                        title = "Auto-Download Chapters",
                        subtitle = "Automatically download new chapters",
                        checked = settings.autoDownloadEnabled,
                        highlight = true,
                        onCheckedChange = { preferencesManager.updateAutoDownloadEnabled(it) }
                    )
                    AnimatedVisibility(
                        visible = settings.autoDownloadEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            SettingsDivider()
                            ToggleItem(
                                icon = Icons.Outlined.Wifi,
                                title = "WiFi Only",
                                subtitle = "Skip downloads on mobile data",
                                checked = settings.autoDownloadOnWifiOnly,
                                onCheckedChange = { preferencesManager.updateAutoDownloadWifiOnly(it) }
                            )
                            SettingsDivider()
                            SliderItem(
                                icon = Icons.Outlined.Numbers,
                                title = "Download Limit",
                                subtitle = if (settings.autoDownloadLimit == 0) "Unlimited"
                                else "Max ${settings.autoDownloadLimit} per novel",
                                value = settings.autoDownloadLimit.toFloat(),
                                valueRange = 0f..50f,
                                steps = 9,
                                valueLabel = if (settings.autoDownloadLimit == 0) "∞"
                                else settings.autoDownloadLimit.toString(),
                                onValueChange = { preferencesManager.updateAutoDownloadLimit(it.toInt()) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// READER PREFERENCES
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsReaderPrefsScreen(
    onBack: () -> Unit,
    onNavigateToReaderSettings: () -> Unit
) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Reader", Icons.AutoMirrored.Outlined.MenuBook) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.LightMode,
                        title = "Keep Screen On",
                        subtitle = "Keep screen on while reading",
                        checked = settings.keepScreenOn,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(keepScreenOn = it))
                        }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.AllInclusive,
                        title = "Infinite Scroll (Experimental)",
                        subtitle = "Load next chapter automatically",
                        checked = settings.infiniteScroll,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(infiniteScroll = it))
                        }
                    )
                    SettingsDivider()
                    NavigationItem(
                        icon = Icons.Outlined.SettingsSuggest,
                        title = "Reader Settings",
                        subtitle = "Font, size, line height and more",
                        onClick = onNavigateToReaderSettings
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SOURCES
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSourcesScreen(
    onBack: () -> Unit,
    onNavigateToMigration: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val localVersion = remember { SourceLoader.localVersion(context) }
    val lastChecked = remember { SourceLoader.lastCheckedTime(context) }
    val allProviders = remember { com.kmhmubin.kothagolp.provider.MainProvider.getProviders() }

    val lastCheckedText = remember(lastChecked) {
        if (lastChecked == 0L) "Never"
        else SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(lastChecked))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sources", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Source Info", Icons.Outlined.Extension) }
            item {
                SettingsCard {
                    InfoItem(Icons.Outlined.Numbers, "Current Version",
                        if (localVersion == 0) "Not installed" else "v$localVersion")
                    SettingsDivider()
                    InfoItem(Icons.Outlined.Extension, "Loaded Sources", "${allProviders.size} sources")
                    SettingsDivider()
                    InfoItem(Icons.Outlined.Schedule, "Last Checked", lastCheckedText)
                }
            }

            item { SectionHeader("Updates", Icons.Outlined.SystemUpdate) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Auto-Update Sources",
                        subtitle = "Check for updates daily in the background",
                        checked = settings.autoUpdateSources,
                        onCheckedChange = { enabled ->
                            preferencesManager.updateAppSettings(
                                settings.copy(autoUpdateSources = enabled)
                            )
                            if (enabled) {
                                SourceSyncWorker.schedulePeriodicSync(context)
                            } else {
                                WorkManager.getInstance(context)
                                    .cancelUniqueWork("source_sync_periodic")
                            }
                        }
                    )
                    SettingsDivider()
                    ClickableItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Check for Updates",
                        subtitle = "Check for new source versions now",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            SourceSyncWorker.forceSync(context)
                            scope.launch {
                                snackbarState.showSnackbar("Checking for source updates…")
                            }
                        }
                    )
                }
            }

            item { SectionHeader("Provider Management", Icons.Outlined.SwapVert) }
            item {
                ProviderCard(
                    settings = settings,
                    onOrderChange = { preferencesManager.updateProviderOrder(it) },
                    onEnabledChange = { name, enabled ->
                        preferencesManager.setProviderEnabled(name, enabled)
                    }
                )
            }

            item { SectionHeader("Migration", Icons.Outlined.SwapVert) }
            item {
                SettingsCard {
                    ClickableItem(
                        icon = Icons.Outlined.SwapVert,
                        title = "Migrate Sources",
                        subtitle = "Move library novels from one source to another",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToMigration
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ABOUT
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    var showResetDialog by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                preferencesManager.resetToDefaults()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("About", Icons.Outlined.Info) }
            item {
                SettingsCard {
                    InfoItem(Icons.Outlined.Apartment, "App", "Kothagolp")
                    SettingsDivider()
                    InfoItem(Icons.Outlined.Numbers, "Version", "1.0.0")
                }
            }

            item { SectionHeader("Advanced", Icons.Outlined.SettingsSuggest) }
            item {
                SettingsCard {
                    ClickableItem(
                        icon = Icons.Outlined.RestartAlt,
                        title = "Reset to Defaults",
                        subtitle = "Restore all settings to defaults",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsLabel(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(22.dp), iconTint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.values().forEach { mode ->
            val isSelected = mode == selected
            val icon = when (mode) {
                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                ThemeMode.DARK -> Icons.Outlined.DarkMode
                ThemeMode.SYSTEM -> Icons.Outlined.SettingsSuggest
            }
            SelectableChip(
                selected = isSelected,
                label = mode.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(mode)
                }
            )
        }
    }
}

@Composable
private fun DensitySelector(selected: UiDensity, onSelect: (UiDensity) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UiDensity.values().forEach { density ->
            val isSelected = density == selected
            val icon = when (density) {
                UiDensity.COMPACT -> Icons.Outlined.ViewCompact
                UiDensity.DEFAULT -> Icons.Outlined.ViewComfy
                UiDensity.COMFORTABLE -> Icons.Outlined.ViewCozy
            }
            SelectableChip(
                selected = isSelected,
                label = density.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(density)
                }
            )
        }
    }
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), content)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content
            )
        }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    highlight: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, Modifier.size(24.dp),
            if (checked && enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
            enabled = enabled
        )
    }
}

@Composable
private fun DropdownItem(
    icon: ImageVector,
    title: String,
    selectedValue: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                selectedValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (i == selectedIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(i)
                        expanded = false
                    },
                    leadingIcon = if (i == selectedIndex) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    valueLabel,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(start = 36.dp),
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun InfoItem(icon: ImageVector, title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tint.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisplayModeRow(
    label: String,
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        DisplayMode.values().forEach { mode ->
            val isSelected = mode == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "bg"
            )
            Surface(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(mode)
                    },
                color = bg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        mode.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GridColumnsRow(
    label: String,
    selected: GridColumns,
    onSelect: (GridColumns) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(
        GridColumns.Auto to "Auto",
        GridColumns.Fixed(2) to "2",
        GridColumns.Fixed(3) to "3",
        GridColumns.Fixed(4) to "4",
        GridColumns.Fixed(5) to "5"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        options.forEach { (cols, text) ->
            val isSelected = when {
                cols is GridColumns.Auto && selected is GridColumns.Auto -> true
                cols is GridColumns.Fixed && selected is GridColumns.Fixed -> cols.count == selected.count
                else -> false
            }
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "bg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(cols)
                    },
                Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChips(
    selected: Set<ReadingStatus>,
    onChange: (Set<ReadingStatus>) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ReadingStatus.READING,
            ReadingStatus.PLAN_TO_READ,
            ReadingStatus.ON_HOLD
        ).forEach { status ->
            val isSelected = status in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChange(if (isSelected) selected - status else selected + status)
                },
                label = {
                    Text(
                        status.displayName(),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isSelected) Icons.Default.Check else Icons.Outlined.Bookmark,
                        null,
                        Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CUSTOM THEME SECTION
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThemeSection(
    colors: CustomThemeColors,
    onColorsChange: (CustomThemeColors) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Presets",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomThemeColors.PRESETS.forEach { (name, preset) ->
                ThemePresetCard(
                    name = name,
                    colors = preset,
                    isSelected = colors == preset,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onColorsChange(preset)
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Custom Colors",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ColorPickerRow(
            label = "Primary",
            color = Color(colors.primaryColor),
            description = "Main accent",
            onClick = { showColorPicker = ColorPickerTarget.PRIMARY }
        )

        ColorPickerRow(
            label = "Secondary",
            color = Color(colors.secondaryColor),
            description = "Supporting accent",
            onClick = { showColorPicker = ColorPickerTarget.SECONDARY }
        )

        ColorPickerRow(
            label = "Background",
            color = Color(colors.backgroundColor),
            description = "App background",
            onClick = { showColorPicker = ColorPickerTarget.BACKGROUND }
        )

        ColorPickerRow(
            label = "Surface",
            color = Color(colors.surfaceColor),
            description = "Cards and sheets",
            onClick = { showColorPicker = ColorPickerTarget.SURFACE }
        )

        Spacer(Modifier.height(8.dp))
        ThemePreviewCard(colors = colors)
    }

    showColorPicker?.let { target ->
        val currentColor = when (target) {
            ColorPickerTarget.PRIMARY -> Color(colors.primaryColor)
            ColorPickerTarget.SECONDARY -> Color(colors.secondaryColor)
            ColorPickerTarget.BACKGROUND -> Color(colors.backgroundColor)
            ColorPickerTarget.SURFACE -> Color(colors.surfaceColor)
        }

        ColorPickerDialog(
            currentColor = currentColor,
            title = "Pick ${target.displayName} Color",
            onColorSelected = { newColor ->
                val colorLong = newColor.toArgb().toLong() and 0xFFFFFFFFL or 0xFF000000L.toLong()
                val newColors = when (target) {
                    ColorPickerTarget.PRIMARY -> colors.copy(primaryColor = colorLong)
                    ColorPickerTarget.SECONDARY -> colors.copy(secondaryColor = colorLong)
                    ColorPickerTarget.BACKGROUND -> colors.copy(backgroundColor = colorLong)
                    ColorPickerTarget.SURFACE -> colors.copy(surfaceColor = colorLong)
                }
                onColorsChange(newColors)
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }
}

private enum class ColorPickerTarget(val displayName: String) {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    BACKGROUND("Background"),
    SURFACE("Surface")
}

@Composable
private fun ThemePresetCard(
    name: String,
    colors: CustomThemeColors,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    borderColor,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(colors.backgroundColor)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.primaryColor))
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.secondaryColor))
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.surfaceColor))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }

            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(colors.primaryColor)
                else Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = Color(colors.primaryColor)
                )
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Color,
    description: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
        )

        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                colorToHex(color),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemePreviewCard(colors: CustomThemeColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(colors.backgroundColor)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Surface(
                color = Color(colors.surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(colors.primaryColor).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Color(colors.primaryColor)
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Sample Novel Title",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Chapter 42 • Author Name",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Surface(
                        color = Color(colors.primaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Read",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = getContrastColor(Color(colors.primaryColor))
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(colors.primaryColor).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Fantasy",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(colors.primaryColor)
                    )
                }
                Surface(
                    color = Color(colors.secondaryColor).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Adventure",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(colors.secondaryColor)
                    )
                }
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}

private fun getContrastColor(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// PROVIDER MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProviderCard(
    settings: AppSettings,
    onOrderChange: (List<String>) -> Unit,
    onEnabledChange: (String, Boolean) -> Unit
) {
    val allProviders = remember { com.kmhmubin.kothagolp.provider.MainProvider.getProviders() }
    var order by remember(settings.providerOrder) {
        mutableStateOf(settings.providerOrder.ifEmpty { allProviders.map { it.name } })
    }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        order = order.toMutableList().apply { add(to.index, removeAt(from.index)) }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging) {
            val current = settings.providerOrder.ifEmpty { allProviders.map { it.name } }
            if (order != current) onOrderChange(order)
        }
    }

    SettingsCard {
        SettingsLabel("Provider Order", Icons.Outlined.SwapVert)
        Text(
            "Hold and drag to reorder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height((56.dp + 4.dp) * minOf(order.size, 6)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(order, key = { _, item -> item }) { _, name ->
                ReorderableItem(reorderState, key = name) { isDragging ->
                    ProviderItem(
                        name = name,
                        enabled = name !in settings.disabledProviders,
                        isDragging = isDragging,
                        onEnabledChange = { onEnabledChange(name, it) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderItem(
    name: String,
    enabled: Boolean,
    isDragging: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val elevation by animateDpAsState(
        if (isDragging) 8.dp else 0.dp,
        spring(stiffness = Spring.StiffnessHigh),
        label = "elev"
    )
    val scale by animateFloatAsState(
        if (isDragging) 1.02f else 1f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    val bg by animateColorAsState(
        when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer
            enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        label = "bg"
    )

    Surface(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .zIndex(if (isDragging) 1f else 0f),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = elevation
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                "Drag",
                modifier.size(24.dp),
                if (isDragging) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 0.6f else 0.3f
                )
            )
            Text(
                name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isDragging) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Switch(enabled, onEnabledChange)
        }
    }
}

@Composable
private fun LibraryShelfCard(
    settings: AppSettings,
    onShelfEnabledChange: (LibraryFilter, Boolean) -> Unit
) {
    SettingsCard {
        SettingsLabel("Visible Shelves", Icons.Outlined.ViewModule)
        Text(
            "Choose which shelves appear in your Library. 'All' is always visible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LibraryFilter.shelfOptions().forEach { filter ->
                LibraryShelfItem(
                    filter = filter,
                    enabled = filter in settings.enabledLibraryFilters,
                    onEnabledChange = { onShelfEnabledChange(filter, it) }
                )
            }
        }
    }
}

@Composable
private fun LibraryShelfItem(
    filter: LibraryFilter,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val bg by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        label = "shelf_bg"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getLibraryShelfIcon(filter),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (enabled) getLibraryShelfColor(filter)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            Text(
                filter.displayName(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Switch(enabled, onEnabledChange)
        }
    }
}

private fun getLibraryShelfIcon(filter: LibraryFilter): ImageVector {
    return when (filter) {
        LibraryFilter.ALL -> Icons.AutoMirrored.Outlined.LibraryBooks
        LibraryFilter.SPICY -> Icons.Rounded.LocalFireDepartment
        LibraryFilter.DOWNLOADED -> Icons.Rounded.CloudDownload
        LibraryFilter.READING -> Icons.AutoMirrored.Rounded.MenuBook
        LibraryFilter.COMPLETED -> Icons.Rounded.CheckCircle
        LibraryFilter.ON_HOLD -> Icons.Rounded.PauseCircle
        LibraryFilter.PLAN_TO_READ -> Icons.Rounded.BookmarkAdd
        LibraryFilter.DROPPED -> Icons.Rounded.Cancel
    }
}

@Composable
private fun getLibraryShelfColor(filter: LibraryFilter): Color {
    return when (filter) {
        LibraryFilter.ALL -> MaterialTheme.colorScheme.primary
        LibraryFilter.SPICY -> Color(0xFFF97316)
        LibraryFilter.DOWNLOADED -> Color(0xFF06B6D4)
        LibraryFilter.READING -> Color(0xFF3B82F6)
        LibraryFilter.COMPLETED -> Color(0xFF22C55E)
        LibraryFilter.ON_HOLD -> Color(0xFFF59E0B)
        LibraryFilter.PLAN_TO_READ -> Color(0xFF8B5CF6)
        LibraryFilter.DROPPED -> Color(0xFFEF4444)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// RESET DIALOG
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Warning,
                null,
                Modifier.size(32.dp),
                MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Reset All Settings?", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "Resets all settings to defaults. Your library and downloaded chapters won't be affected.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}
