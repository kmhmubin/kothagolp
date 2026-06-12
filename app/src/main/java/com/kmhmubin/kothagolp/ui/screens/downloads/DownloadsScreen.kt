package com.kmhmubin.kothagolp.ui.screens.downloads

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kmhmubin.kothagolp.epub.EpubExportOptions
import com.kmhmubin.kothagolp.service.DownloadPriority
import com.kmhmubin.kothagolp.ui.screens.downloads.components.EpubExportDialog
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onNovelClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf<DownloadedNovel?>(null) }
    var showCancelActiveDialog by remember { mutableStateOf(false) }
    var showCancelQueuedDialog by remember { mutableStateOf<ActiveDownload?>(null) }
    var showCancelAllDialog by remember { mutableStateOf(false) }
    var showRetryDialog by remember { mutableStateOf<FailedDownload?>(null) }

    // Pull to refresh
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf<DownloadedNovel?>(null) }
    var exportOptions by remember { mutableStateOf(EpubExportOptions()) }
    var pendingExportNovel by remember { mutableStateOf<DownloadedNovel?>(null) }
    val exportState by viewModel.epubExportState.collectAsStateWithLifecycle()

    // Initialize exporter
    LaunchedEffect(Unit) {
        viewModel.initializeExporter(context)
    }

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let { outputUri ->
            pendingExportNovel?.let { novel ->
                scope.launch {
                    val result = viewModel.exportNovelToEpub(
                        novelUrl = novel.novelUrl,
                        outputUri = outputUri,
                        options = exportOptions
                    )
                    if (result.success) {
                        snackbarHostState.showSnackbar(
                            "Exported ${result.chapterCount} chapters (${result.formattedFileSize})"
                        )
                    }
                }
            }
        }
        pendingExportNovel = null
    }

    LaunchedEffect(Unit) {
        viewModel.loadDownloads()
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadDownloads()
            delay(500)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Downloads",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.activeDownloads.isNotEmpty()) {
                            Text(
                                text = "${uiState.activeDownloads.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.activeDownloads.size > 1) {
                        TextButton(onClick = { showCancelAllDialog = true }) {
                            Text(
                                text = "Cancel All",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.downloadedNovels.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.downloadedNovels.isEmpty() &&
                        uiState.activeDownloads.isEmpty() &&
                        uiState.failedDownloads.isEmpty() -> {
                    DownloadsEmptyState()
                }

                else -> {
                    DownloadsContent(
                        downloadedNovels = uiState.downloadedNovels,
                        activeDownloads = uiState.activeDownloads,
                        failedDownloads = uiState.failedDownloads,
                        totalStorageUsed = uiState.totalStorageUsed,
                        sortOrder = uiState.sortOrder,
                        onNovelClick = { novel ->
                            onNovelClick(novel.novelUrl, novel.sourceName)
                        },
                        onDeleteClick = { novel ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteDialog = novel
                        },
                        onSwipeDelete = { novel ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteNovelDownloads(novel.novelUrl)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Deleted ${novel.downloadedChapters} chapters",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onExportClick = { novel ->
                            showExportDialog = novel
                        },
                        onPauseClick = { viewModel.pauseDownload() },
                        onResumeClick = { viewModel.resumeDownload() },
                        onCancelActiveClick = { showCancelActiveDialog = true },
                        onCancelQueuedClick = { download -> showCancelQueuedDialog = download },
                        onRemoveFromQueue = { novelUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.removeFromQueue(novelUrl)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Removed from queue",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onMoveToTop = { novelUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.moveToTop(novelUrl)
                        },
                        onMoveToBottom = { novelUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.moveToBottom(novelUrl)
                        },
                        onMoveUp = { novelUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.moveUp(novelUrl)
                        },
                        onMoveDown = { novelUrl ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.moveDown(novelUrl)
                        },
                        onReorderQueue = { fromIndex, toIndex ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.reorderQueue(fromIndex, toIndex)
                        },
                        onRetryFailed = { failed -> showRetryDialog = failed },
                        onDismissFailed = { novelUrl -> viewModel.dismissFailedDownload(novelUrl) },
                        onToggleSortOrder = { viewModel.toggleSortOrder() }
                    )
                }
            }
        }
    }

    // Delete downloaded novel dialog
    showDeleteDialog?.let { novel ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Downloads?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will delete all downloaded chapters for:")
                    Text(
                        text = "\"${novel.novelName}\"",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${novel.downloadedChapters} chapters will be removed. This cannot be undone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNovelDownloads(novel.novelUrl)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cancel active download dialog
    if (showCancelActiveDialog) {
        val activeDownload = uiState.activeDownloads.firstOrNull { it.currentChapterName != "Queued" }
        AlertDialog(
            onDismissRequest = { showCancelActiveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Cancel Download?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeDownload != null) {
                        Text("Stop the download for:")
                        Text(
                            text = "\"${activeDownload.novelName}\"",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Progress: ${activeDownload.downloadedCount}/${activeDownload.totalCount} chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val queueCount = uiState.activeDownloads.count { it.currentChapterName == "Queued" }
                    if (queueCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$queueCount queued downloads will continue after cancellation.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Already downloaded chapters will be kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelCurrentDownload()
                        showCancelActiveDialog = false
                    }
                ) {
                    Text("Cancel Download", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelActiveDialog = false }) {
                    Text("Keep Downloading")
                }
            }
        )
    }

    // Cancel queued download dialog
    showCancelQueuedDialog?.let { queuedDownload ->
        AlertDialog(
            onDismissRequest = { showCancelQueuedDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove from Queue?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remove from the download queue:")
                    Text(
                        text = "\"${queuedDownload.novelName}\"",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${queuedDownload.totalCount} chapters will not be downloaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFromQueue(queuedDownload.novelUrl)
                        showCancelQueuedDialog = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelQueuedDialog = null }) {
                    Text("Keep in Queue")
                }
            }
        )
    }

    // Cancel all dialog
    if (showCancelAllDialog) {
        AlertDialog(
            onDismissRequest = { showCancelAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Cancel All Downloads?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All active downloads will stop and the queue will be cleared.")
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${uiState.activeDownloads.size} downloads will be cancelled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Already downloaded chapters will be kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAllDownloads()
                        showCancelAllDialog = false
                    }
                ) {
                    Text("Cancel All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAllDialog = false }) {
                    Text("Keep Downloading")
                }
            }
        )
    }

    // Export dialog
    showExportDialog?.let { novel ->
        EpubExportDialog(
            novel = novel,
            exportState = exportState,
            options = exportOptions,
            onOptionsChange = { exportOptions = it },
            onExport = {
                pendingExportNovel = novel
                exportLauncher.launch(viewModel.generateEpubFileName(novel.novelName))
            },
            onDismiss = {
                showExportDialog = null
                viewModel.resetExportState()
                exportOptions = EpubExportOptions()
            }
        )
    }

    // Retry failed download dialog
    showRetryDialog?.let { failed ->
        AlertDialog(
            onDismissRequest = { showRetryDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Retry Failed Chapters?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Retry all failed chapters for:")
                    Text(
                        text = "\"${failed.novelName}\"",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${failed.failedChapterCount} chapters failed to download.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (failed.errorMessage.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Error: ${failed.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.retryFailedDownload(failed)
                        showRetryDialog = null
                    }
                ) {
                    Text("Retry", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRetryDialog = null }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
private fun DownloadsContent(
    downloadedNovels: List<DownloadedNovel>,
    activeDownloads: List<ActiveDownload>,
    failedDownloads: List<FailedDownload>,
    totalStorageUsed: String,
    sortOrder: DownloadSortOrder,
    onNovelClick: (DownloadedNovel) -> Unit,
    onDeleteClick: (DownloadedNovel) -> Unit,
    onSwipeDelete: (DownloadedNovel) -> Unit,
    onExportClick: (DownloadedNovel) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelActiveClick: () -> Unit,
    onCancelQueuedClick: (ActiveDownload) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveToTop: (String) -> Unit,
    onMoveToBottom: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onRetryFailed: (FailedDownload) -> Unit,
    onDismissFailed: (String) -> Unit,
    onToggleSortOrder: () -> Unit
) {
    val dimensions = KothagolpTheme.dimensions
    val listState = rememberLazyListState()

    // Separate active from queued
    val currentDownload = activeDownloads.firstOrNull { it.currentChapterName != "Queued" }
    val queuedDownloads = activeDownloads.filter { it.currentChapterName == "Queued" }

    // Drag state for queue reordering
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = dimensions.gridPadding,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Summary
        item(key = "storage") {
            StorageSummaryCard(
                totalDownloads = downloadedNovels.sumOf { it.downloadedChapters },
                novelCount = downloadedNovels.size,
                storageUsed = totalStorageUsed,
                activeCount = activeDownloads.size,
                failedCount = failedDownloads.size
            )
        }

        // Failed Downloads Section
        if (failedDownloads.isNotEmpty()) {
            item(key = "failed_header") {
                SectionHeader(
                    title = "Failed Downloads",
                    icon = Icons.Rounded.Error,
                    badge = "${failedDownloads.size}",
                    badgeColor = MaterialTheme.colorScheme.error
                )
            }

            items(
                count = failedDownloads.size,
                key = { "failed_${failedDownloads[it].novelUrl}" }
            ) { index ->
                val failed = failedDownloads[index]
                FailedDownloadCard(
                    failed = failed,
                    onRetryClick = { onRetryFailed(failed) },
                    onDismissClick = { onDismissFailed(failed.novelUrl) }
                )
            }
        }

        // Active Download Section
        if (currentDownload != null) {
            item(key = "active_header") {
                SectionHeader(
                    title = "Downloading Now",
                    icon = Icons.Rounded.Downloading,
                    badge = null
                )
            }

            item(key = "active_${currentDownload.novelUrl}") {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CurrentDownloadCard(
                        download = currentDownload,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onCancelClick = onCancelActiveClick
                    )
                }
            }
        }

        // Queued Downloads Section
        if (queuedDownloads.isNotEmpty()) {
            item(key = "queue_header") {
                SectionHeader(
                    title = "Queue",
                    icon = Icons.Rounded.HourglassEmpty,
                    badge = "${queuedDownloads.size}",
                    subtitle = "Long press to drag and reorder"
                )
            }

            itemsIndexed(
                items = queuedDownloads,
                key = { _, download -> "queued_${download.novelUrl}" }
            ) { index, download ->
                val isDragging = draggedItemIndex == index
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    label = "elevation"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.02f else 1f,
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (isDragging) dragOffset.roundToInt() else 0
                            )
                        }
                        .shadow(elevation, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedItemIndex = index
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val itemHeight = 80.dp.toPx()
                                    val draggedPositions = (dragOffset / itemHeight).roundToInt()
                                    val targetIndex = (index + draggedPositions)
                                        .coerceIn(0, queuedDownloads.lastIndex)

                                    if (targetIndex != index && targetIndex in queuedDownloads.indices) {
                                        onReorderQueue(index, targetIndex)
                                        draggedItemIndex = targetIndex
                                        dragOffset = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggedItemIndex = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = -1
                                    dragOffset = 0f
                                }
                            )
                        }
                ) {
                    QueuedDownloadCard(
                        download = download,
                        position = index + 1,
                        totalInQueue = queuedDownloads.size,
                        isDragging = isDragging,
                        onCancelClick = { onCancelQueuedClick(download) },
                        onRemoveClick = { onRemoveFromQueue(download.novelUrl) },
                        onMoveToTop = { onMoveToTop(download.novelUrl) },
                        onMoveToBottom = { onMoveToBottom(download.novelUrl) },
                        onMoveUp = { onMoveUp(download.novelUrl) },
                        onMoveDown = { onMoveDown(download.novelUrl) }
                    )
                }
            }
        }

        // Downloaded Novels Section
        if (downloadedNovels.isNotEmpty()) {
            item(key = "downloaded_header") {
                SectionHeaderWithSort(
                    title = "Downloaded",
                    icon = Icons.Rounded.DownloadDone,
                    count = downloadedNovels.size,
                    sortOrder = sortOrder,
                    onToggleSortOrder = onToggleSortOrder
                )
            }

            items(
                count = downloadedNovels.size,
                key = { "downloaded_${downloadedNovels[it].novelUrl}" }
            ) { index ->
                val novel = downloadedNovels[index]
                SwipeableDownloadedNovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onDeleteClick = { onDeleteClick(novel) },
                    onSwipeDelete = { onSwipeDelete(novel) },
                    onExportClick = { onExportClick(novel) }
                )
            }
        }

        // Bottom spacing
        item(key = "bottom_spacer") {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StorageSummaryCard(
    totalDownloads: Int,
    novelCount: Int,
    storageUsed: String,
    activeCount: Int,
    failedCount: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageStatItem(value = "$totalDownloads", label = "Chapters")
                VerticalDivider()
                StorageStatItem(value = "$novelCount", label = "Novels")
                VerticalDivider()
                StorageStatItem(value = storageUsed, label = "Storage")
            }

            if (activeCount > 0 || failedCount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeCount > 0) {
                        StatusChip(
                            icon = Icons.Rounded.Downloading,
                            text = "$activeCount active",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (activeCount > 0 && failedCount > 0) {
                        Spacer(Modifier.width(8.dp))
                    }
                    if (failedCount > 0) {
                        StatusChip(
                            icon = Icons.Rounded.Error,
                            text = "$failedCount failed",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    )
}

@Composable
private fun StorageStatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FailedDownloadCard(
    failed: FailedDownload,
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = failed.novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${failed.failedChapterCount} chapters failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (failed.errorMessage.isNotBlank()) {
                    Text(
                        text = failed.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onRetryClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentDownloadCard(
    download: ActiveDownload,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val progressAnimation by animateFloatAsState(
        targetValue = download.progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (download.isPaused) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(60.dp),
                    shadowElevation = 2.dp
                ) {
                    if (!download.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = download.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = download.novelName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        PriorityBadge(priority = download.priority)
                    }

                    Text(
                        text = download.currentChapterName.removePrefix("Downloading: "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = if (download.isPaused) onResumeClick else onPauseClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (download.isPaused)
                                Icons.Rounded.PlayArrow
                            else
                                Icons.Rounded.Pause,
                            contentDescription = if (download.isPaused) "Resume" else "Pause",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onCancelClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (download.isPaused) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Paused",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            if (download.speed.isNotBlank() && download.speed != "--") {
                                Text(
                                    text = download.speed,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Downloading...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        text = "${download.downloadedCount} / ${download.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                LinearProgressIndicator(
                    progress = { progressAnimation },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    strokeCap = StrokeCap.Round,
                    color = if (download.isPaused)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (download.successCount > 0 || download.failedCount > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (download.successCount > 0) {
                                Text(
                                    text = "✓ ${download.successCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                            }
                            if (download.failedCount > 0) {
                                Text(
                                    text = "✗ ${download.failedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (!download.isPaused && download.eta.isNotBlank() && download.eta != "--:--") {
                        Text(
                            text = "~${download.eta} remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: DownloadPriority) {
    when (priority) {
        DownloadPriority.HIGH -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PriorityHigh,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "HIGH",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        DownloadPriority.LOW -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "LOW",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        DownloadPriority.NORMAL -> {
            // No badge for normal priority
        }
    }
}

@Composable
private fun QueuedDownloadCard(
    download: ActiveDownload,
    position: Int,
    totalInQueue: Int,
    isDragging: Boolean,
    onCancelClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                if (!download.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = download.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = download.novelName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    PriorityBadge(priority = download.priority)
                }
                Text(
                    text = "${download.totalCount} chapters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (position > 1) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            contentDescription = "Move up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (position < totalInQueue) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Move down",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (position > 1) {
                        DropdownMenuItem(
                            text = { Text("Move to top") },
                            onClick = {
                                onMoveToTop()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.KeyboardDoubleArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    if (position < totalInQueue) {
                        DropdownMenuItem(
                            text = { Text("Move to bottom") },
                            onClick = {
                                onMoveToBottom()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.KeyboardDoubleArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Remove from queue",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onCancelClick()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDownloadedNovelCard(
    novel: DownloadedNovel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSwipeDelete: () -> Unit,
    onExportClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var dismissed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                dismissed = true
                onSwipeDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    AnimatedVisibility(
        visible = !dismissed,
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val scale by animateFloatAsState(
                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.8f,
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .scale(scale)
                            .padding(end = 24.dp)
                            .size(28.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true
        ) {
            DownloadedNovelCard(
                novel = novel,
                onClick = onClick,
                onDeleteClick = onDeleteClick,
                onExportClick = onExportClick
            )
        }
    }
}

@Composable
private fun DownloadedNovelCard(
    novel: DownloadedNovel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(64.dp),
                shadowElevation = 1.dp
            ) {
                if (!novel.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Novel info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = novel.novelName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF10B981)
                            )
                            Text(
                                text = "${novel.downloadedChapters}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    if (novel.sourceName.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = novel.sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                // Export button
                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = "Export to EPUB",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete downloads",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 28.dp)
            )
        }
    }
}

@Composable
private fun SectionHeaderWithSort(
    title: String,
    icon: ImageVector,
    count: Int,
    sortOrder: DownloadSortOrder,
    onToggleSortOrder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Surface(
            onClick = onToggleSortOrder,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (sortOrder) {
                        DownloadSortOrder.NEWEST_FIRST -> Icons.Rounded.ArrowDownward
                        DownloadSortOrder.OLDEST_FIRST -> Icons.Rounded.ArrowUpward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (sortOrder) {
                        DownloadSortOrder.NEWEST_FIRST -> "Newest"
                        DownloadSortOrder.OLDEST_FIRST -> "Oldest"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No Downloads Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Download chapters from any novel\nto read offline anytime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap download in any novel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}