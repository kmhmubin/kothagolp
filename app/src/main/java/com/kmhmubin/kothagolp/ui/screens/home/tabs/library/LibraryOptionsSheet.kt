package com.kmhmubin.kothagolp.ui.screens.home.tabs.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.domain.model.DisplayMode
import com.kmhmubin.kothagolp.domain.model.GridColumns
import com.kmhmubin.kothagolp.domain.model.LibrarySortOrder
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.ui.theme.AppShape
import kotlinx.coroutines.launch

/**
 * Komikku-style library options sheet: tabbed Sort / Display pages.
 * Opened by pressing the Library nav button while already on the Library tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsSheet(
    sortOrder: LibrarySortOrder,
    appSettings: AppSettings,
    onSortSelected: (LibrarySortOrder) -> Unit,
    onDisplayModeSelected: (DisplayMode) -> Unit,
    onGridColumnsSelected: (GridColumns) -> Unit,
    onDensitySelected: (UiDensity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val tabs = listOf("Sort", "Display")
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.animateContentSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> SortPage(current = sortOrder, onSelect = onSortSelected)
                    else -> DisplayPage(
                        appSettings = appSettings,
                        onDisplayModeSelected = onDisplayModeSelected,
                        onGridColumnsSelected = onGridColumnsSelected,
                        onDensitySelected = onDensitySelected
                    )
                }
            }
        }
    }
}

// ============================================================================
// Sort page
// ============================================================================

@Composable
private fun SortPage(
    current: LibrarySortOrder,
    onSelect: (LibrarySortOrder) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Title toggles A-Z / Z-A on repeated taps, Komikku-style
        SortItem(
            label = "Title",
            sortDescending = when (current) {
                LibrarySortOrder.TITLE_ASC -> false
                LibrarySortOrder.TITLE_DESC -> true
                else -> null
            },
            onClick = {
                onSelect(
                    if (current == LibrarySortOrder.TITLE_ASC) {
                        LibrarySortOrder.TITLE_DESC
                    } else {
                        LibrarySortOrder.TITLE_ASC
                    }
                )
            }
        )
        SortItem(
            label = "Last read",
            sortDescending = true.takeIf { current == LibrarySortOrder.LAST_READ },
            onClick = { onSelect(LibrarySortOrder.LAST_READ) }
        )
        SortItem(
            label = "Date added",
            sortDescending = true.takeIf { current == LibrarySortOrder.DATE_ADDED },
            onClick = { onSelect(LibrarySortOrder.DATE_ADDED) }
        )
        SortItem(
            label = "Unread count",
            sortDescending = true.takeIf { current == LibrarySortOrder.UNREAD_COUNT },
            onClick = { onSelect(LibrarySortOrder.UNREAD_COUNT) }
        )
        SortItem(
            label = "New chapters",
            sortDescending = true.takeIf { current == LibrarySortOrder.NEW_CHAPTERS },
            onClick = { onSelect(LibrarySortOrder.NEW_CHAPTERS) }
        )
    }
}

// ============================================================================
// Display page
// ============================================================================

@Composable
private fun DisplayPage(
    appSettings: AppSettings,
    onDisplayModeSelected: (DisplayMode) -> Unit,
    onGridColumnsSelected: (GridColumns) -> Unit,
    onDensitySelected: (UiDensity) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        HeadingItem("Display mode")
        ChipRow {
            FilterChip(
                selected = appSettings.libraryDisplayMode == DisplayMode.GRID,
                onClick = { onDisplayModeSelected(DisplayMode.GRID) },
                label = { Text("Grid") }
            )
            FilterChip(
                selected = appSettings.libraryDisplayMode == DisplayMode.LIST,
                onClick = { onDisplayModeSelected(DisplayMode.LIST) },
                label = { Text("List") }
            )
        }

        if (appSettings.libraryDisplayMode == DisplayMode.GRID) {
            HeadingItem("Grid size")
            GridSizeSlider(
                columns = appSettings.libraryGridColumns,
                onChange = onGridColumnsSelected
            )
        }

        HeadingItem("Card style")
        ChipRow {
            UiDensity.entries.forEach { density ->
                FilterChip(
                    selected = appSettings.uiDensity == density,
                    onClick = { onDensitySelected(density) },
                    label = {
                        // Komikku grid style names
                        Text(
                            when (density) {
                                UiDensity.COMPACT -> "Cover only"
                                UiDensity.DEFAULT -> "Compact"
                                UiDensity.COMFORTABLE -> "Comfortable"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GridSizeSlider(
    columns: GridColumns,
    onChange: (GridColumns) -> Unit
) {
    // Local value during drag; persisting per drag-frame rewrites all settings
    // and recomposes the whole app each frame. Commit once on release.
    var sliderValue by remember(columns) {
        mutableFloatStateOf(GridColumns.toInt(columns).toFloat())
    }
    val displayValue = sliderValue.toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onChange(GridColumns.fromInt(sliderValue.toInt()))
            },
            valueRange = 0f..5f,
            steps = 4
        )
        Surface(
            shape = AppShape.pill,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = if (displayValue <= 0) "Auto" else "$displayValue",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ============================================================================
// Komikku settings item primitives
// ============================================================================

@Composable
private fun HeadingItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SortItem(
    label: String,
    sortDescending: Boolean?,
    onClick: () -> Unit
) {
    val arrowIcon: ImageVector? = when (sortDescending) {
        true -> Icons.Default.ArrowDownward
        false -> Icons.Default.ArrowUpward
        null -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (arrowIcon != null) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
