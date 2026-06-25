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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Backup
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.data.backup.LocalBackupWorker
import com.kmhmubin.kothagolp.domain.model.ChapterUpdateInterval
import com.kmhmubin.kothagolp.domain.model.LocalBackupInterval
import com.kmhmubin.kothagolp.domain.model.CustomThemeColors
import com.kmhmubin.kothagolp.domain.model.DisplayMode
import com.kmhmubin.kothagolp.domain.model.GridColumns
import com.kmhmubin.kothagolp.util.calculateGridColumns
import com.kmhmubin.kothagolp.domain.model.LibraryFilter
import com.kmhmubin.kothagolp.domain.model.LibrarySortOrder
import com.kmhmubin.kothagolp.domain.model.RatingFormat
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.domain.model.ThemeMode
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.update.ChapterUpdateScheduler
import com.kmhmubin.kothagolp.ui.components.ColorPickerDialog
import com.kmhmubin.kothagolp.ui.navigation.NavRoutes
import com.kmhmubin.kothagolp.ui.theme.AccentCyan
import com.kmhmubin.kothagolp.ui.theme.AppElevation
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.AppSpacing
import com.kmhmubin.kothagolp.ui.theme.StatusCompleted
import com.kmhmubin.kothagolp.ui.theme.StatusDROPPED
import com.kmhmubin.kothagolp.ui.theme.StatusOnHold
import com.kmhmubin.kothagolp.ui.theme.StatusPlanToRead
import com.kmhmubin.kothagolp.ui.theme.StatusReading
import com.kmhmubin.kothagolp.ui.theme.StatusSpicy
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
            // ── Personalization ───────────────────────────────────────────
            item {
                Text(
                    text = "PERSONALIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.large
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Recommendation Engine",
                            subtitle = "AI picks, OpenRouter key and model",
                            onClick = { onNavigateTo(NavRoutes.SettingsForYou.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Palette,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Appearance",
                            subtitle = "Theme, colors and display layout",
                            onClick = { onNavigateTo(NavRoutes.SettingsAppearance.route) }
                        )
                    }
                }
            }

            // ── Content & Reading ─────────────────────────────────────────
            item {
                Text(
                    text = "CONTENT & READING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.large
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            iconTint = StatusPlanToRead,
                            title = "Reader",
                            subtitle = "Reading experience and preferences",
                            onClick = { onNavigateTo(NavRoutes.SettingsReader.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                            iconTint = StatusReading,
                            title = "Library",
                            subtitle = "Shelves, sorting and visibility",
                            onClick = { onNavigateTo(NavRoutes.SettingsLibrary.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Extension,
                            iconTint = StatusSpicy,
                            title = "Sources",
                            subtitle = "Manage and enable providers",
                            onClick = { onNavigateTo(NavRoutes.SettingsSources.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Search,
                            iconTint = AccentCyan,
                            title = "Browse & Downloads",
                            subtitle = "Search, ratings and auto-downloads",
                            onClick = { onNavigateTo(NavRoutes.SettingsBrowse.route) }
                        )
                    }
                }
            }

            // ── System ────────────────────────────────────────────────────
            item {
                Text(
                    text = "SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.large
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Backup,
                            iconTint = StatusCompleted,
                            title = "Backup & Sync",
                            subtitle = "Cloud sync, local backup and cache",
                            onClick = { onNavigateTo(NavRoutes.Storage.route) }
                        )
                        RowDivider()
                        SettingsNavRow(
                            icon = Icons.Outlined.Notifications,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Permissions",
                            subtitle = "App permissions and storage folder",
                            onClick = { onNavigateTo(NavRoutes.SettingsPermissions.route) }
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
                    DisplayModeRow("Browse (Books)", settings.browseDisplayMode) {
                        preferencesManager.updateBrowseDisplayMode(it)
                    }
                    DisplayModeRow("Sources List", settings.sourceListDisplayMode) {
                        preferencesManager.updateSourceListDisplayMode(it)
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
                    GridColumnsRow("Browse (Books)", settings.browseGridColumns) {
                        preferencesManager.updateBrowseGridColumns(it)
                    }
                    GridColumnsRow("Sources List", settings.sourceListGridColumns) {
                        preferencesManager.updateSourceListGridColumns(it)
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
    val context = LocalContext.current
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

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

            item { SectionHeader("Chapter Updates", Icons.Outlined.Sync) }
            item {
                SettingsCard {
                    DropdownItem(
                        icon = Icons.Outlined.Schedule,
                        title = "Check Interval",
                        selectedValue = settings.chapterUpdateInterval.displayName(),
                        options = ChapterUpdateInterval.entries.map { it.displayName() },
                        selectedIndex = settings.chapterUpdateInterval.ordinal,
                        onSelect = { idx ->
                            val interval = ChapterUpdateInterval.entries[idx]
                            preferencesManager.updateAppSettings(
                                settings.copy(chapterUpdateInterval = interval)
                            )
                            ChapterUpdateScheduler.schedule(
                                context = context,
                                interval = interval,
                                wifiOnly = settings.chapterUpdateOnWifiOnly
                            )
                        }
                    )
                    if (settings.chapterUpdateInterval != ChapterUpdateInterval.OFF) {
                        SettingsDivider()
                        ToggleItem(
                            icon = Icons.Outlined.Wifi,
                            title = "Wi-Fi Only",
                            subtitle = "Only check when connected to Wi-Fi",
                            checked = settings.chapterUpdateOnWifiOnly,
                            onCheckedChange = { wifiOnly ->
                                preferencesManager.updateAppSettings(
                                    settings.copy(chapterUpdateOnWifiOnly = wifiOnly)
                                )
                                ChapterUpdateScheduler.schedule(
                                    context = context,
                                    interval = settings.chapterUpdateInterval,
                                    wifiOnly = wifiOnly
                                )
                            }
                        )
                        SettingsDivider()
                        ToggleItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Show Notifications",
                            subtitle = "Notify when new chapters are found",
                            checked = settings.chapterUpdateNotify,
                            onCheckedChange = {
                                preferencesManager.updateAppSettings(
                                    settings.copy(chapterUpdateNotify = it)
                                )
                            }
                        )
                    }
                    SettingsDivider()
                    ClickableItem(
                        icon = Icons.Outlined.Sync,
                        title = "Check Now",
                        subtitle = "Run chapter check immediately",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            ChapterUpdateScheduler.runNow(context)
                            scope.launch {
                                snackbarState.showSnackbar("Checking for new chapters…")
                            }
                        }
                    )
                }
            }

            item { SectionHeader("Local Backup", Icons.Outlined.Backup) }
            item {
                SettingsCard {
                    DropdownItem(
                        icon = Icons.Outlined.Schedule,
                        title = "Auto Backup Interval",
                        selectedValue = settings.localBackupInterval.displayName(),
                        options = LocalBackupInterval.entries.map { it.displayName() },
                        selectedIndex = settings.localBackupInterval.ordinal,
                        onSelect = { idx ->
                            val interval = LocalBackupInterval.entries[idx]
                            preferencesManager.updateAppSettings(
                                settings.copy(localBackupInterval = interval)
                            )
                            LocalBackupWorker.schedule(context, interval, forceUpdate = true)
                        }
                    )
                }
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
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Rounded.BookmarkAdd,
                        title = "Quick-Save Category",
                        selectedValue = settings.quickSaveStatus.displayName(),
                        options = ReadingStatus.entries.map { it.displayName() },
                        selectedIndex = ReadingStatus.entries.indexOf(settings.quickSaveStatus),
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(quickSaveStatus = ReadingStatus.entries[it])
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
    val allProviders = remember { com.kmhmubin.kothagolp.provider.MainProvider.getProviders() }

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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                .background(MaterialTheme.colorScheme.primaryContainer, AppShape.medium),
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
        shape = AppShape.large
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
                .background(iconTint.copy(alpha = 0.15f), AppShape.medium),
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
            .clip(AppShape.medium)
            .clickable(onClick = onClick),
        color = bg,
        shape = AppShape.medium
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
            .clip(AppShape.medium)
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
            .clip(AppShape.medium)
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
                shape = AppShape.small
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
            .clip(AppShape.medium)
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
            .clip(AppShape.medium)
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
                    .clip(AppShape.small)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(mode)
                    },
                color = bg,
                shape = AppShape.small
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
    val autoColumns = calculateGridColumns(GridColumns.Auto)
    val options = listOf(
        GridColumns.Auto to "Auto\n($autoColumns)",
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
                    .clip(AppShape.small)
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
    val animBorder by animateColorAsState(
        targetValue = if (isSelected) Color(colors.primaryColor.toInt())
                      else Color.Transparent,
        label = "presetBorder"
    )
    val bgColor      = Color(colors.backgroundColor.toInt())
    val primaryColor = Color(colors.primaryColor.toInt())
    val secondaryColor = Color(colors.secondaryColor.toInt())
    val surfColor    = Color(colors.surfaceColor.toInt())

    // Infer a legible label color from bg luminance
    val bgLum = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue
    val nameColor = if (bgLum < 0.45f)
        Color.White.copy(alpha = if (isSelected) 1f else 0.75f)
    else
        Color.Black.copy(alpha = if (isSelected) 1f else 0.75f)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(92.dp)
            .height(120.dp),
        shape = AppShape.large,
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) animBorder
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Colour preview area ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Overlapping circles: large primary (front-left) + small secondary (back-right)
                Box(modifier = Modifier.size(60.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(secondaryColor)
                            .border(2.dp, bgColor, CircleShape)
                    )
                }

                // Selected checkmark badge in top-end corner
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(12.dp),
                            tint = bgColor
                        )
                    }
                }
            }

            // ── Name strip ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(
                        color = surfColor.copy(alpha = 0.85f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            bottomStart = 16.dp, bottomEnd = 16.dp
                        )
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) primaryColor else nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            .clip(AppShape.medium)
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
                .clip(AppShape.medium)
                .background(color)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = AppShape.medium
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
            shape = AppShape.extraSmall
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
        shape = AppShape.large
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
                shape = AppShape.medium
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
                            .clip(AppShape.small)
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
                        shape = AppShape.small
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
                    shape = AppShape.extraLarge
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
                    shape = AppShape.extraLarge
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
        shape = AppShape.medium,
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
        shape = AppShape.medium
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

// ═══════════════════════════════════════════════════════════════════════════
// PERMISSIONS SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }

    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var batteryGranted by remember {
        mutableStateOf(
            context.getSystemService<PowerManager>()
                ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        )
    }
    var storageFolderUri by remember { mutableStateOf(preferencesManager.getStorageFolderUri()) }

    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
                batteryGranted = context.getSystemService<PowerManager>()
                    ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* state re-checked on resume */ }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // URI grant may fail if system revoked it; proceed with the URI anyway
            }
            val uriString = uri.toString()
            preferencesManager.setStorageFolderUri(uriString)
            storageFolderUri = uriString
            // Create standard sub-folders in the chosen storage root
            try {
                val root = DocumentFile.fromTreeUri(context, uri)
                listOf("downloads", "autobackup", "logs", "notes").forEach { name ->
                    if (root?.findFile(name) == null) root?.createDirectory(name)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions", fontWeight = FontWeight.SemiBold) },
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    SettingsPermissionRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notifications",
                        subtitle = "Get alerts for new chapters, download progress, and library update results.",
                        granted = notificationsGranted,
                        required = false,
                        onGrant = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item {
                @Suppress("BatteryLife")
                SettingsPermissionRow(
                    icon = Icons.Rounded.Battery5Bar,
                    title = "Unrestricted Battery Usage",
                    subtitle = "Prevents the OS from killing background library updates, downloads, and backup restores mid-run.",
                    granted = batteryGranted,
                    required = false,
                    onGrant = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                val folderName = storageFolderUri?.let {
                    Uri.parse(it).lastPathSegment?.substringAfterLast(':') ?: "Selected"
                }
                SettingsPermissionRow(
                    icon = Icons.Rounded.Folder,
                    title = "Download Folder",
                    subtitle = if (folderName != null)
                        "Folder: $folderName"
                    else
                        "Choose where to save EPUB exports and backup files. Recommended but optional.",
                    granted = storageFolderUri != null,
                    required = false,
                    grantLabel = if (storageFolderUri != null) "Change" else "Choose",
                    onGrant = { folderLauncher.launch(null) }
                )
            }
        }
    }
}

@Composable
private fun SettingsPermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    required: Boolean,
    grantLabel: String = "Grant",
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = AppShape.medium,
            color = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (required) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (granted && grantLabel == "Grant") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (grantLabel == "Grant") {
                Button(
                    onClick = onGrant,
                    shape = AppShape.small,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(grantLabel, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(
                    onClick = onGrant,
                    shape = AppShape.small,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(grantLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FOR YOU SETTINGS  (AI / OpenRouter)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsForYouScreen(onBack: () -> Unit) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var apiKey by remember { mutableStateOf(preferencesManager.getGeminiApiKey() ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(preferencesManager.getGeminiApiKey() != null) }

    var selectedModel by remember { mutableStateOf(preferencesManager.getSelectedAiModel()) }
    var showModelSheet by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(com.kmhmubin.kothagolp.ai.OpenRouterModelsService.FALLBACK_FREE_MODELS) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var modelSearchQuery by remember { mutableStateOf("") }

    // Load models when key is present
    LaunchedEffect(apiKey) {
        if (apiKey.length > 10) {
            isLoadingModels = true
            val result = com.kmhmubin.kothagolp.ai.OpenRouterModelsService.getModels(apiKey)
            result.onSuccess { models = it }
            isLoadingModels = false
        }
    }

    val filteredModels = remember(models, modelSearchQuery) {
        if (modelSearchQuery.isBlank()) models
        else models.filter {
            it.name.contains(modelSearchQuery, ignoreCase = true) ||
            it.id.contains(modelSearchQuery, ignoreCase = true) ||
            it.provider.contains(modelSearchQuery, ignoreCase = true)
        }
    }

    if (showModelSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Choose AI Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Text(
                    text = "${models.count { it.isFree }} free · ${models.count { !it.isFree }} paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelSearchQuery,
                    onValueChange = { modelSearchQuery = it },
                    placeholder = { Text("Search models...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = AppShape.medium
                )
                Spacer(Modifier.height(8.dp))
                if (isLoadingModels) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Loading live models...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val recModels  = filteredModels.filter { it.isRecommended }
                    val freeModels = filteredModels.filter { it.isFree && !it.isRecommended }
                    val paidModels = filteredModels.filter { !it.isFree && !it.isRecommended }

                    fun onPick(model: com.kmhmubin.kothagolp.ai.AiModel) {
                        selectedModel = model.id
                        preferencesManager.setSelectedAiModel(model.id)
                        showModelSheet = false
                        scope.launch { snackbarHostState.showSnackbar("Model: ${model.name}") }
                    }

                    if (recModels.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = androidx.compose.ui.graphics.Color(0xFFFFC107)
                                )
                                Text(
                                    "RECOMMENDED FOR NOVEL PICKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color(0xFFFFC107)
                                )
                            }
                        }
                        items(recModels, key = { "rec_${it.id}" }) { model ->
                            AiModelRow(model = model, isSelected = model.id == selectedModel, onClick = { onPick(model) })
                        }
                    }
                    if (freeModels.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(if (recModels.isNotEmpty()) 8.dp else 0.dp))
                            Text(
                                "OTHER FREE MODELS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        items(freeModels, key = { "free_${it.id}" }) { model ->
                            AiModelRow(model = model, isSelected = model.id == selectedModel, onClick = { onPick(model) })
                        }
                    }
                    if (paidModels.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "PAID MODELS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        items(paidModels, key = { "paid_${it.id}" }) { model ->
                            AiModelRow(model = model, isSelected = model.id == selectedModel, onClick = { onPick(model) })
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("For You", fontWeight = FontWeight.SemiBold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // API Key card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = AppShape.large,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Recommendations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Via OpenRouter — access Gemini, Llama, DeepSeek and more. Free models available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text("OpenRouter API Key", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; saved = false },
                            placeholder = { Text("sk-or-v1-...") },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (apiKey.isNotBlank()) {
                                    preferencesManager.setGeminiApiKey(apiKey.trim())
                                    saved = true
                                    scope.launch { snackbarHostState.showSnackbar("API key saved") }
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShape.medium
                        )

                        Text(
                            "Get your free key at openrouter.ai → Keys. No credit card needed for free models.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (apiKey.isNotBlank()) {
                                        preferencesManager.setGeminiApiKey(apiKey.trim())
                                        saved = true
                                        com.kmhmubin.kothagolp.ai.OpenRouterModelsService.invalidateCache()
                                        scope.launch { snackbarHostState.showSnackbar("API key saved") }
                                    }
                                },
                                enabled = apiKey.length > 10 && !saved,
                                modifier = Modifier.weight(1f),
                                shape = AppShape.medium
                            ) {
                                Text(if (saved) "Saved" else "Save Key")
                            }
                            if (preferencesManager.getGeminiApiKey() != null) {
                                OutlinedButton(
                                    onClick = {
                                        preferencesManager.clearGeminiApiKey()
                                        apiKey = ""
                                        saved = false
                                        scope.launch { snackbarHostState.showSnackbar("API key removed") }
                                    },
                                    shape = AppShape.medium,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("Remove") }
                            }
                        }
                    }
                }
            }

            // Model picker card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = AppShape.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("AI Model", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)

                        val currentModel = models.find { it.id == selectedModel }
                        Surface(
                            onClick = { showModelSheet = true },
                            shape = AppShape.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (currentModel?.isRecommended == true) {
                                            Icon(
                                                Icons.Rounded.Star,
                                                null,
                                                tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = currentModel?.name ?: selectedModel.substringAfterLast("/"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (currentModel?.isFree == true) {
                                            Surface(
                                                shape = AppShape.small,
                                                color = androidx.compose.ui.graphics.Color(0xFF1B5E20).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "FREE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (currentModel != null) {
                                        Text(
                                            text = "${currentModel.provider} · ${currentModel.contextLength / 1000}K ctx",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            "FREE models are rate-limited (typically 20 req/min, 200 req/day) but cost $0. Paid models have higher limits and are billed per token through your OpenRouter account.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AiModelRow(
    model: com.kmhmubin.kothagolp.ai.AiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val amber = androidx.compose.ui.graphics.Color(0xFFFFC107)
    val green = androidx.compose.ui.graphics.Color(0xFF4CAF50)

    val bgColor = when {
        isSelected && model.isRecommended -> amber.copy(alpha = 0.12f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        model.isRecommended -> amber.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        onClick = onClick,
        shape = AppShape.medium,
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star icon for recommended
            if (model.isRecommended) {
                Icon(
                    Icons.Rounded.Star,
                    null,
                    tint = amber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        model.isRecommended -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (model.isRecommended && model.recommendReason.isNotBlank()) {
                    Text(
                        text = model.recommendReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.2f
                    )
                } else {
                    Text(
                        text = "${model.provider} · ${model.contextLength / 1000}K ctx",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (model.isFree) {
                    Surface(shape = AppShape.small, color = green.copy(alpha = 0.15f)) {
                        Text(
                            "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = green,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(shape = AppShape.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            model.displayPrice,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (isSelected) {
                    Surface(shape = AppShape.small, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Text(
                            "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getLibraryShelfColor(filter: LibraryFilter): Color {
    return when (filter) {
        LibraryFilter.ALL -> MaterialTheme.colorScheme.primary
        LibraryFilter.SPICY -> StatusSpicy
        LibraryFilter.DOWNLOADED -> AccentCyan
        LibraryFilter.READING -> StatusReading
        LibraryFilter.COMPLETED -> StatusCompleted
        LibraryFilter.ON_HOLD -> StatusOnHold
        LibraryFilter.PLAN_TO_READ -> StatusPlanToRead
        LibraryFilter.DROPPED -> StatusDROPPED
    }
}

