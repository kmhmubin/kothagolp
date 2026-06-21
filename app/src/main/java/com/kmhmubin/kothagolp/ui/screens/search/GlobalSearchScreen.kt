package com.kmhmubin.kothagolp.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.ui.components.DuplicateLibraryDialog
import com.kmhmubin.kothagolp.ui.components.KothagolpSearchBar
import com.kmhmubin.kothagolp.ui.components.NovelActionSheet
import com.kmhmubin.kothagolp.ui.components.NovelCard
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.ProviderSearchState
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.SearchFiltersSheet
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import com.kmhmubin.kothagolp.util.calculateGridColumns
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    initialQuery: String = "",
    appSettings: AppSettings,
    onBack: () -> Unit,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: GlobalSearchViewModel = viewModel(
        factory = GlobalSearchViewModel.Factory(initialQuery)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionSheetState by viewModel.actionSheetState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dimensions = KothagolpTheme.dimensions
    val gridColumns = calculateGridColumns(appSettings.searchGridColumns)
    val resultsPerProvider = appSettings.searchResultsPerProvider

    // Action Sheet
    if (actionSheetState.isVisible && actionSheetState.data != null) {
        val data = actionSheetState.data!!

        NovelActionSheet(
            data = data,
            sheetState = sheetState,
            onDismiss = { viewModel.hideActionSheet() },
            onViewDetails = {
                viewModel.hideActionSheet()
                onNavigateToDetails(data.novel.url, data.novel.apiName)
            },
            onContinueReading = {
                viewModel.hideActionSheet()
                scope.launch {
                    val chapter = viewModel.getContinueReadingChapter(data.novel.url)
                    if (chapter != null) {
                        val (chapterUrl, novelUrl, providerName) = chapter
                        onNavigateToReader(chapterUrl, novelUrl, providerName)
                    } else {
                        onNavigateToDetails(data.novel.url, data.novel.apiName)
                    }
                }
            },
            onAddToLibrary = if (!data.isInLibrary) {
                { status -> viewModel.addToLibrary(data.novel, status) }
            } else null,
            onRemoveFromLibrary = if (data.isInLibrary) {
                { viewModel.removeFromLibrary(data.novel.url) }
            } else null,
            onStatusChange = { status -> viewModel.updateReadingStatus(status) },
            onRemoveFromHistory = null
        )
    }

    actionSheetState.duplicateWarning?.let { warning ->
        DuplicateLibraryDialog(
            target = warning.target,
            duplicates = warning.duplicates,
            onViewExisting = { duplicate ->
                viewModel.dismissDuplicateWarning()
                onNavigateToDetails(duplicate.novel.url, duplicate.novel.apiName)
            },
            onAddAnyway = { viewModel.addDuplicateAnyway() },
            onDismiss = { viewModel.dismissDuplicateWarning() }
        )
    }

    if (uiState.showFilters) {
        SearchFiltersSheet(
            filters = uiState.filters,
            availableProviders = uiState.providerSearchStates.keys.toList(),
            onDismiss = { viewModel.toggleFiltersPanel() },
            onFiltersChanged = { viewModel.updateFilters(it) },
            onClearFilters = { viewModel.clearFilters() }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            KothagolpSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::search,
                isLoading = uiState.isSearching,
                placeholder = "Search across all sources...",
                modifier = Modifier.padding(dimensions.gridPadding),
                autoFocus = initialQuery.isBlank(),
                showBackButton = true,
                onBackClick = onBack,
                showFilterButton = uiState.hasSearched && uiState.providerSearchStates.isNotEmpty(),
                hasActiveFilters = uiState.hasActiveFilters,
                onFilterClick = { viewModel.toggleFiltersPanel() }
            )

            // Streaming progress bar
            AnimatedVisibility(visible = uiState.isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.gridPadding),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // Filter chips
            AnimatedVisibility(visible = uiState.hasSearched && uiState.providerSearchStates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.gridPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.hideEmptyProviders,
                        onClick = { viewModel.toggleHideEmptyProviders() },
                        label = { Text("Has results only") }
                    )
                    if (!uiState.isSearching) {
                        Text(
                            text = "${uiState.totalResults} novels · ${uiState.providersWithResults}/${uiState.totalProviders} sources",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "${uiState.completedProviders}/${uiState.totalProviders} sources loaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Content area
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = uiState.hasSearched,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "search-content"
                ) { hasSearched ->
                    if (!hasSearched) {
                        SearchIdleState()
                    } else if (uiState.isSearchEmpty) {
                        SearchEmptyState(query = uiState.searchQuery)
                    } else {
                        SearchResults(
                            uiState = uiState,
                            gridColumns = gridColumns,
                            resultsPerProvider = resultsPerProvider,
                            appSettings = appSettings,
                            onNovelClick = { novel ->
                                onNavigateToDetails(novel.url, novel.apiName)
                            },
                            onNovelLongClick = { novel ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.quickSaveToLibrary(novel, appSettings.quickSaveStatus)
                            }
                        )
                    }
                }

                // Quick-save toast
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = uiState.quickSaveMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        uiState.quickSaveMessage?.let { msg ->
                            Surface(
                                shape = AppShape.large,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                tonalElevation = 4.dp,
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchIdleState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "Search across all sources",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Find novels from every source at once",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try a different search term",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchResults(
    uiState: GlobalSearchUiState,
    gridColumns: Int,
    resultsPerProvider: Int,
    appSettings: AppSettings,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXl)
    ) {
        // Same ordering as BrowseTab: results-with-content first, loading next, empty/error at bottom
        val states = uiState.filteredProviderStates
        val withResults = states.filter { (_, s) -> s is ProviderSearchState.Success && (s as ProviderSearchState.Success).novels.isNotEmpty() }
        val loading = states.filter { (_, s) -> s is ProviderSearchState.Loading }
        val noResults = states.filter { (_, s) -> s !is ProviderSearchState.Loading && !(s is ProviderSearchState.Success && (s as ProviderSearchState.Success).novels.isNotEmpty()) }
        val ordered = withResults + loading + noResults

        ordered.forEach { (providerName, state) ->
            item(key = "provider_$providerName") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    when (state) {
                        is ProviderSearchState.Loading -> {
                            ProviderLoadingRow(providerName = providerName)
                        }
                        is ProviderSearchState.Success -> {
                            if (state.novels.isNotEmpty()) {
                                ProviderResultsSection(
                                    providerName = providerName,
                                    novels = state.novels,
                                    libraryUrls = uiState.libraryUrls,
                                    maxResults = resultsPerProvider,
                                    gridColumns = gridColumns,
                                    appSettings = appSettings,
                                    onNovelClick = onNovelClick,
                                    onNovelLongClick = onNovelLongClick
                                )
                            } else if (!uiState.hideEmptyProviders) {
                                ProviderEmptyRow(providerName = providerName)
                            }
                        }
                        is ProviderSearchState.Error -> {
                            ProviderErrorRow(
                                providerName = providerName,
                                message = state.message
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProviderResultsSection(
    providerName: String,
    novels: List<Novel>,
    libraryUrls: Set<String>,
    maxResults: Int,
    gridColumns: Int,
    appSettings: AppSettings,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions
    val displayNovels = novels.take(maxResults)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Provider header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = providerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${novels.size} result${if (novels.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Horizontal scrolling row of cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensions.gridPadding),
            horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
        ) {
            items(displayNovels, key = { it.url }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onLongClick = { onNovelLongClick(novel) },
                    density = appSettings.uiDensity,
                    showApiName = false,
                    isInLibrary = novel.url in libraryUrls,
                    modifier = Modifier.width(120.dp)
                )
            }

            if (novels.size > maxResults) {
                item {
                    ViewMoreCard(
                        count = novels.size - maxResults,
                        onClick = { /* show all — could expand in future */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderLoadingRow(providerName: String) {
    val dimensions = KothagolpTheme.dimensions

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = providerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensions.gridPadding),
            horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
        ) {
            items(4) {
                SearchCardSkeleton()
            }
        }
    }
}

@Composable
private fun ProviderEmptyRow(providerName: String) {
    val dimensions = KothagolpTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.gridPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = providerName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "— No results",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ProviderErrorRow(providerName: String, message: String) {
    val dimensions = KothagolpTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.gridPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Text(
            text = providerName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "— Failed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SearchCardSkeleton() {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp),
        shape = AppShape.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {}
}

@Composable
private fun ViewMoreCard(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .height(180.dp),
        shape = AppShape.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "+$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
