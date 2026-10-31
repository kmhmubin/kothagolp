package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistoryClearDialog
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistoryEmpty
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistoryNoMatch
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistorySelectToolbar
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistorySheet
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistorySkeleton
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.HistoryTopBar
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components.historyGroup

/** History tab. The app theme uses the standard motion scheme, so History opts into Expressive here. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryTab(
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToBrowse: () -> Unit,
    onSearchAllSources: (query: String) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        HistoryContent(onNavigateToDetails, onNavigateToReader, onNavigateToBrowse, onSearchAllSources, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    onNavigateToDetails: (String, String) -> Unit,
    onNavigateToReader: (String, String, String) -> Unit,
    onNavigateToBrowse: () -> Unit,
    onSearchAllSources: (String) -> Unit,
    viewModel: HistoryViewModel,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    @Suppress("DEPRECATION") // LocalClipboard needs a suspend ClipEntry; the sync manager is what the app uses elsewhere.
    val clipboard = LocalClipboardManager.current

    var sheetUrl by rememberSaveable { mutableStateOf<String?>(null) }
    // Look the row up live, so the sheet's library chip morphs after "＋ Library".
    val sheetRow = remember(ui.groups, sheetUrl) {
        sheetUrl?.let { url -> ui.groups.firstNotNullOfOrNull { g -> g.rows.find { it.item.novel.url == url } } }
    }

    BackHandler(enabled = ui.isSelecting || ui.isSearchOpen) {
        if (ui.isSelecting) viewModel.stopSelecting() else viewModel.closeSearch()
    }

    // Show first, then acknowledge: clearing before showing would cancel this keyed effect.
    LaunchedEffect(ui.snackbar) {
        val s = ui.snackbar ?: return@LaunchedEffect
        val (message, action) = when (s) {
            is HistorySnack.Removed ->
                (if (s.items.size == 1) HistoryCopy.removedOne(s.items[0].novel.name) else HistoryCopy.removedMany(s.items.size)) to HistoryCopy.UNDO
            is HistorySnack.AddedToLibrary -> HistoryCopy.ADDED_TO_LIBRARY to HistoryCopy.UNDO
            HistorySnack.TitleCopied -> HistoryCopy.TITLE_COPIED to null
        }
        val result = snackbar.showSnackbar(message, action, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) {
            when (s) {
                is HistorySnack.Removed -> viewModel.undoRemove(s.items)
                is HistorySnack.AddedToLibrary -> viewModel.undoAddToLibrary(s.novel)
                HistorySnack.TitleCopied -> Unit
            }
        }
        viewModel.snackbarShown()
    }

    if (ui.showClearDialog) HistoryClearDialog(ui.totalCount, viewModel::confirmClear, viewModel::dismissClear)

    sheetRow?.let { row ->
        val novel = row.item.novel
        fun act(action: () -> Unit): () -> Unit = { sheetUrl = null; action() }
        HistorySheet(
            row = row,
            onDismiss = { sheetUrl = null },
            onContinue = act { onNavigateToReader(row.item.chapterUrl, novel.url, novel.apiName) },
            onNovelPage = act { onNavigateToDetails(novel.url, novel.apiName) },
            onAddToLibrary = { viewModel.addToLibrary(novel) },
            onCopyTitle = act {
                clipboard.setText(AnnotatedString(row.title))
                viewModel.titleCopied()
            },
            onSelect = act { viewModel.startSelecting(novel.url) },
            onRemove = act { viewModel.remove(listOf(novel.url)) },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HistoryTopBar(
                totalCount = ui.totalCount,
                isLoading = ui.isLoading,
                isSelecting = ui.isSelecting,
                selectedCount = ui.selectedUrls.size,
                isSearchOpen = ui.isSearchOpen,
                query = ui.query,
                onQueryChange = viewModel::setQuery,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                onDoneSelecting = viewModel::stopSelecting,
                onSelect = { viewModel.startSelecting(null) },
                onClear = viewModel::requestClear,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.isLoading -> HistorySkeleton()
                ui.totalCount == 0 -> HistoryEmpty(onFindNovels = onNavigateToBrowse)
                ui.groups.isEmpty() && ui.query.isNotBlank() ->
                    HistoryNoMatch(ui.query.trim(), onSearchAllSources = { onSearchAllSources(ui.query.trim()) })
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                    ui.groups.forEach { group ->
                        historyGroup(
                            group = group,
                            isSelecting = ui.isSelecting,
                            selectedUrls = ui.selectedUrls,
                            onClick = { row ->
                                val n = row.item.novel
                                if (ui.isSelecting) viewModel.toggle(n.url) else onNavigateToReader(row.item.chapterUrl, n.url, n.apiName)
                            },
                            onLongClick = { row -> if (ui.isSelecting) viewModel.toggle(row.item.novel.url) else sheetUrl = row.item.novel.url },
                            onSwipeRemove = { row -> viewModel.remove(listOf(row.item.novel.url)) },
                        )
                    }
                }
            }
            HistorySelectToolbar(
                visible = ui.isSelecting,
                selectedCount = ui.selectedUrls.size,
                onSelectAll = viewModel::selectAll,
                onRemove = { viewModel.remove(ui.selectedUrls.toList()) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
    }
}
