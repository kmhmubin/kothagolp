package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.data.repository.HistoryItem
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.ui.components.HistoryListItem
import com.kmhmubin.kothagolp.ui.components.HistoryListItemCompact
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryTab(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimensions = KothagolpTheme.dimensions
    val useCompactLayout = appSettings.uiDensity == UiDensity.COMPACT

    // Handle back press in selection mode
    if (uiState.isSelectionMode) {
        BackHandler { viewModel.exitSelectionMode() }
    }

    // Clear all confirmation dialog
    if (uiState.showClearConfirmation) {
        ClearHistoryConfirmationDialog(
            itemCount = uiState.totalCount,
            onConfirm = viewModel::confirmClearHistory,
            onDismiss = viewModel::dismissClearConfirmation
        )
    }

    // Delete selected confirmation dialog
    if (uiState.showDeleteSelectedConfirmation) {
        DeleteSelectedConfirmationDialog(
            count = uiState.selectedItems.size,
            onConfirm = viewModel::confirmDeleteSelected,
            onDismiss = viewModel::dismissDeleteSelectedConfirmation
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding()
    ) {
        // Header: crossfade between normal and selection headers
        if (uiState.totalCount > 0 || uiState.isSelectionMode) {
            Crossfade(
                targetState = uiState.isSelectionMode,
                label = "header_crossfade"
            ) { selectionMode ->
                if (selectionMode) {
                    val allVisibleSelected = remember(uiState.groupedItems, uiState.selectedItems) {
                        val visibleUrls =
                            uiState.groupedItems.values.flatten().map { it.novel.url }.toSet()
                        visibleUrls.isNotEmpty() && visibleUrls.all { it in uiState.selectedItems }
                    }
                    SelectionHeader(
                        selectedCount = uiState.selectedItems.size,
                        allVisibleSelected = allVisibleSelected,
                        onClose = viewModel::exitSelectionMode,
                        onSelectAll = viewModel::selectAllVisible,
                        onDeleteSelected = viewModel::requestDeleteSelected
                    )
                } else {
                    HistoryHeader(
                        itemCount = uiState.totalCount,
                        onClearHistory = viewModel::requestClearHistory
                    )
                }
            }
        }

        // Search bar
        AnimatedVisibility(
            visible = uiState.totalCount > 0,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            HistorySearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.padding(
                    horizontal = dimensions.gridPadding,
                    vertical = 8.dp
                )
            )
        }

        // Content
        when {
            uiState.isLoading -> {
                HistoryLoadingState()
            }

            uiState.totalCount == 0 -> {
                EmptyHistoryState()
            }

            uiState.filteredCount == 0 && uiState.searchQuery.isNotBlank() -> {
                NoResultsState(query = uiState.searchQuery)
            }

            else -> {
                GroupedHistoryList(
                    groupedItems = uiState.groupedItems,
                    useCompactLayout = useCompactLayout,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedItems = uiState.selectedItems,
                    onContinueReading = { item ->
                        onNavigateToReader(item.chapterUrl, item.novel.url, item.novel.apiName)
                    },
                    onRemoveFromHistory = { item ->
                        viewModel.removeFromHistory(item.novel.url)
                    },
                    onNovelClick = { item ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleSelection(item.novel.url)
                        } else {
                            onNavigateToDetails(item.novel.url, item.novel.apiName)
                        }
                    },
                    onLongClick = { item ->
                        if (!uiState.isSelectionMode) {
                            viewModel.enterSelectionMode(item.novel.url)
                        } else {
                            viewModel.toggleSelection(item.novel.url)
                        }
                    }
                )
            }
        }
    }
}

// ================================================================
// HEADER
// ================================================================

@Composable
private fun HistoryHeader(
    itemCount: Int,
    onClearHistory: () -> Unit
) {
    val dimensions = KothagolpTheme.dimensions

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.gridPadding,
                    vertical = dimensions.spacingMd
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Reading History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "novel" else "novels"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = onClearHistory,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ================================================================
// SELECTION HEADER
// ================================================================

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit selection",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = if (allVisibleSelected) "Deselect" else "All",
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

// ================================================================
// SEARCH BAR
// ================================================================

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search history…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            onClick = { onQueryChange("") }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

// ================================================================
// GROUPED HISTORY LIST
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedHistoryList(
    groupedItems: Map<HistoryDateGroup, List<HistoryItem>>,
    useCompactLayout: Boolean,
    isSelectionMode: Boolean,
    selectedItems: Set<String>,
    onContinueReading: (HistoryItem) -> Unit,
    onRemoveFromHistory: (HistoryItem) -> Unit,
    onNovelClick: (HistoryItem) -> Unit,
    onLongClick: (HistoryItem) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions
    val orderedGroups = listOf(
        HistoryDateGroup.TODAY,
        HistoryDateGroup.YESTERDAY,
        HistoryDateGroup.THIS_WEEK,
        HistoryDateGroup.THIS_MONTH,
        HistoryDateGroup.EARLIER
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dimensions.gridPadding,
            end = dimensions.gridPadding,
            top = dimensions.spacingSm,
            bottom = dimensions.spacingXl + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        orderedGroups.forEach { group ->
            val items = groupedItems[group] ?: emptyList()
            if (items.isNotEmpty()) {
                stickyHeader(key = "header_$group") {
                    DateGroupHeader(
                        group = group,
                        itemCount = items.size
                    )
                }

                items(
                    items = items,
                    key = { "${it.novel.url}_${it.timestamp}" }
                ) { item ->
                    val isSelected = item.novel.url in selectedItems

                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (useCompactLayout) {
                            HistoryListItemCompact(
                                item = item,
                                onContinueClick = { onContinueReading(item) },
                                onRemoveClick = { onRemoveFromHistory(item) },
                                onItemClick = { onNovelClick(item) },
                                onLongClick = { onLongClick(item) },
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected
                            )
                        } else {
                            HistoryListItem(
                                item = item,
                                onContinueClick = { onContinueReading(item) },
                                onRemoveClick = { onRemoveFromHistory(item) },
                                onItemClick = { onNovelClick(item) },
                                onLongClick = { onLongClick(item) },
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected
                            )
                        }
                    }
                }

                item(key = "spacer_$group") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ================================================================
// DATE GROUP HEADER
// ================================================================

@Composable
private fun DateGroupHeader(
    group: HistoryDateGroup,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = itemCount.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ================================================================
// EMPTY / LOADING / NO RESULTS STATES
// ================================================================

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .alpha(0.5f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No Reading History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Novels you read will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .alpha(0.4f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "No Results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "No matches for \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HistoryLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ================================================================
// DIALOGS
// ================================================================

@Composable
private fun ClearHistoryConfirmationDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Clear Reading History?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "This will permanently remove all $itemCount ${if (itemCount == 1) "entry" else "entries"} from your reading history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DeleteSelectedConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Remove Selected?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Remove $count ${if (count == 1) "entry" else "entries"} from your reading history?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}