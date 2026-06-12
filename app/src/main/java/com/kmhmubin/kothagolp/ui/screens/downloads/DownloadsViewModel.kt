package com.kmhmubin.kothagolp.ui.screens.downloads

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.epub.EpubExportOptions
import com.kmhmubin.kothagolp.epub.EpubExportResult
import com.kmhmubin.kothagolp.epub.EpubExportState
import com.kmhmubin.kothagolp.epub.EpubExporter
import com.kmhmubin.kothagolp.service.DownloadPriority
import com.kmhmubin.kothagolp.service.DownloadServiceManager
import com.kmhmubin.kothagolp.service.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadedNovel(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String?,
    val sourceName: String,
    val downloadedChapters: Int,
    val totalChapters: Int = 0,
    val lastDownloadedAt: Long = 0L
)

data class ActiveDownload(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String?,
    val currentChapterName: String,
    val downloadedCount: Int,
    val totalCount: Int,
    val progress: Float,
    val isPaused: Boolean = false,
    val speed: String = "",
    val eta: String = "",
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0
)

data class FailedDownload(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String?,
    val sourceName: String,
    val failedChapterCount: Int,
    val failedChapterUrls: List<String>,
    val failedChapterNames: List<String>,
    val errorMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

data class DownloadsUiState(
    val isLoading: Boolean = true,
    val downloadedNovels: List<DownloadedNovel> = emptyList(),
    val activeDownloads: List<ActiveDownload> = emptyList(),
    val failedDownloads: List<FailedDownload> = emptyList(),
    val totalStorageUsed: String = "0 MB",
    val sortOrder: DownloadSortOrder = DownloadSortOrder.NEWEST_FIRST
)

class DownloadsViewModel : ViewModel() {

    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private var previousActiveNovelUrl: String? = null
    private var wasDownloadActive: Boolean = false
    private var previousQueueSize: Int = 0

    private var cachedNovels: List<DownloadedNovel> = emptyList()

    // Track failed downloads locally
    private val failedDownloadsMap = mutableMapOf<String, FailedDownload>()

    init {
        observeActiveDownloads()
    }

    private var epubExporter: EpubExporter? = null

    val epubExportState: StateFlow<EpubExportState>
        get() = epubExporter?.exportState ?: MutableStateFlow(EpubExportState()).asStateFlow()

    fun initializeExporter(context: Context) {
        if (epubExporter == null) {
            epubExporter = EpubExporter(context, offlineRepository)
        }
    }

    suspend fun exportNovelToEpub(
        novelUrl: String,
        outputUri: Uri,
        options: EpubExportOptions = EpubExportOptions()
    ): EpubExportResult {
        return epubExporter?.exportToEpub(novelUrl, outputUri, options)
            ?: EpubExportResult(success = false, error = "Exporter not initialized")
    }

    fun generateEpubFileName(novelName: String): String {
        return epubExporter?.generateFileName(novelName)
            ?: "${novelName.take(50)}.epub"
    }

    fun resetExportState() {
        epubExporter?.resetState()
    }


    private fun observeActiveDownloads() {
        viewModelScope.launch {
            DownloadServiceManager.downloadState.collect { downloadState ->
                updateActiveDownloads(downloadState)
            }
        }
    }

    private fun updateActiveDownloads(downloadState: DownloadState) {
        val activeList = mutableListOf<ActiveDownload>()

        val isCurrentlyActive = downloadState.isActive || downloadState.isPaused
        val currentNovelUrl = downloadState.novelUrl.takeIf { it.isNotBlank() }
        val currentQueueSize = downloadState.queuedDownloads.size

        // Current download
        if (isCurrentlyActive) {
            activeList.add(
                ActiveDownload(
                    novelUrl = downloadState.novelUrl,
                    novelName = downloadState.novelName,
                    coverUrl = downloadState.novelCoverUrl,
                    currentChapterName = downloadState.currentChapterName,
                    downloadedCount = downloadState.currentProgress,
                    totalCount = downloadState.totalChapters,
                    progress = downloadState.progressPercent,
                    isPaused = downloadState.isPaused,
                    speed = downloadState.formattedSpeed,
                    eta = downloadState.estimatedTimeRemaining,
                    priority = DownloadPriority.NORMAL, // Current download doesn't have priority info
                    successCount = downloadState.successCount,
                    failedCount = downloadState.failedCount,
                    skippedCount = downloadState.skippedCount
                )
            )
        }

        // Queued downloads
        downloadState.queuedDownloads.forEach { queued ->
            activeList.add(
                ActiveDownload(
                    novelUrl = queued.novelUrl,
                    novelName = queued.novelName,
                    coverUrl = queued.novelCoverUrl,
                    currentChapterName = "Queued",
                    downloadedCount = 0,
                    totalCount = queued.chapterCount,
                    progress = 0f,
                    isPaused = false,
                    priority = queued.priority
                )
            )
        }

        _uiState.update {
            it.copy(
                activeDownloads = activeList,
                failedDownloads = failedDownloadsMap.values.toList()
            )
        }

        // Detect completion with failures
        if (wasDownloadActive && !isCurrentlyActive && !downloadState.isPaused) {
            // Check if there were failures
            if (downloadState.failedCount > 0 && previousActiveNovelUrl != null) {
                // A download just completed with failures - this would need
                // info from DownloadService about which chapters failed
                // For now, we track via the error callback
            }
        }

        // Detect completion scenarios
        val shouldRefresh = when {
            wasDownloadActive && !isCurrentlyActive && !downloadState.isPaused -> true
            wasDownloadActive && isCurrentlyActive &&
                    previousActiveNovelUrl != null &&
                    currentNovelUrl != null &&
                    currentNovelUrl != previousActiveNovelUrl -> true
            currentQueueSize < previousQueueSize && wasDownloadActive -> true
            else -> false
        }

        if (shouldRefresh) {
            loadDownloads()
        }

        previousActiveNovelUrl = currentNovelUrl
        wasDownloadActive = isCurrentlyActive
        previousQueueSize = currentQueueSize
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val downloadInfo = offlineRepository.getAllDownloadInfo()

                val downloadedNovels = downloadInfo.mapNotNull { info ->
                    if (info.chapterCount <= 0) return@mapNotNull null

                    val novelUrl = info.novelUrl
                    val offlineDetails = offlineRepository.getNovelDetails(novelUrl)
                    val libraryItem = libraryRepository.getLibraryItem(novelUrl)

                    val novelName = offlineDetails?.name
                        ?: libraryItem?.novel?.name
                        ?: extractNameFromUrl(novelUrl)

                    val coverUrl = offlineDetails?.posterUrl
                        ?: libraryItem?.novel?.posterUrl

                    val sourceName = libraryItem?.novel?.apiName
                        ?: extractSourceFromUrl(novelUrl)

                    DownloadedNovel(
                        novelUrl = novelUrl,
                        novelName = novelName,
                        coverUrl = coverUrl,
                        sourceName = sourceName,
                        downloadedChapters = info.chapterCount,
                        lastDownloadedAt = info.lastDownloadedAt
                    )
                }

                cachedNovels = downloadedNovels
                val sortedNovels = sortNovels(downloadedNovels, _uiState.value.sortOrder)

                val totalChapters = downloadInfo.sumOf { it.chapterCount }
                val estimatedMB = (totalChapters * 10) / 1024.0
                val storageString = if (estimatedMB < 1) {
                    "${(estimatedMB * 1024).toInt()} KB"
                } else {
                    String.format("%.1f MB", estimatedMB)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadedNovels = sortedNovels,
                        totalStorageUsed = storageString,
                        failedDownloads = failedDownloadsMap.values.toList()
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleSortOrder() {
        val newOrder = when (_uiState.value.sortOrder) {
            DownloadSortOrder.NEWEST_FIRST -> DownloadSortOrder.OLDEST_FIRST
            DownloadSortOrder.OLDEST_FIRST -> DownloadSortOrder.NEWEST_FIRST
        }

        _uiState.update {
            it.copy(
                sortOrder = newOrder,
                downloadedNovels = sortNovels(cachedNovels, newOrder)
            )
        }
    }

    private fun sortNovels(
        novels: List<DownloadedNovel>,
        order: DownloadSortOrder
    ): List<DownloadedNovel> {
        return when (order) {
            DownloadSortOrder.NEWEST_FIRST -> novels.sortedByDescending { it.lastDownloadedAt }
            DownloadSortOrder.OLDEST_FIRST -> novels.sortedBy { it.lastDownloadedAt }
        }
    }

    fun deleteNovelDownloads(novelUrl: String) {
        viewModelScope.launch {
            try {
                offlineRepository.deleteNovelDownloads(novelUrl)
                cachedNovels = cachedNovels.filter { it.novelUrl != novelUrl }
                _uiState.update { state ->
                    state.copy(
                        downloadedNovels = state.downloadedNovels.filter { it.novelUrl != novelUrl }
                    )
                }
                loadDownloads()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseDownload() {
        DownloadServiceManager.pauseDownload()
    }

    fun resumeDownload() {
        DownloadServiceManager.resumeDownload()
    }

    fun cancelCurrentDownload() {
        DownloadServiceManager.cancelCurrentDownload()
        loadDownloads()
    }

    fun cancelAllDownloads() {
        DownloadServiceManager.cancelDownload()
        loadDownloads()
    }

    fun removeFromQueue(novelUrl: String) {
        DownloadServiceManager.removeFromQueue(novelUrl)
    }

    // Queue reordering methods
    fun moveToTop(novelUrl: String) {
        DownloadServiceManager.moveToTop(novelUrl)
    }

    fun moveToBottom(novelUrl: String) {
        DownloadServiceManager.moveToBottom(novelUrl)
    }

    fun moveUp(novelUrl: String) {
        DownloadServiceManager.moveUp(novelUrl)
    }

    fun moveDown(novelUrl: String) {
        DownloadServiceManager.moveDown(novelUrl)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        DownloadServiceManager.reorderQueue(fromIndex, toIndex)
    }

    // Failed downloads management
    fun retryFailedDownload(failed: FailedDownload) {
        viewModelScope.launch {
            // Remove from failed list
            failedDownloadsMap.remove(failed.novelUrl)
            _uiState.update {
                it.copy(failedDownloads = failedDownloadsMap.values.toList())
            }

            // Re-queue the failed chapters
            DownloadServiceManager.retryFailedChapters(
                novelUrl = failed.novelUrl,
                novelName = failed.novelName,
                novelCoverUrl = failed.coverUrl,
                sourceName = failed.sourceName,
                chapterUrls = failed.failedChapterUrls,
                chapterNames = failed.failedChapterNames
            )
        }
    }

    fun dismissFailedDownload(novelUrl: String) {
        failedDownloadsMap.remove(novelUrl)
        _uiState.update {
            it.copy(failedDownloads = failedDownloadsMap.values.toList())
        }
    }

    // Called when a download completes with failures
    fun recordFailedDownload(
        novelUrl: String,
        novelName: String,
        coverUrl: String?,
        sourceName: String,
        failedChapterUrls: List<String>,
        failedChapterNames: List<String>,
        errorMessage: String
    ) {
        if (failedChapterUrls.isNotEmpty()) {
            failedDownloadsMap[novelUrl] = FailedDownload(
                novelUrl = novelUrl,
                novelName = novelName,
                coverUrl = coverUrl,
                sourceName = sourceName,
                failedChapterCount = failedChapterUrls.size,
                failedChapterUrls = failedChapterUrls,
                failedChapterNames = failedChapterNames,
                errorMessage = errorMessage
            )
            _uiState.update {
                it.copy(failedDownloads = failedDownloadsMap.values.toList())
            }
        }
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            url.substringAfterLast("/")
                .replace("-", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        } catch (e: Exception) {
            "Unknown Novel"
        }
    }

    private fun extractSourceFromUrl(url: String): String {
        return try {
            val host = url.removePrefix("https://").removePrefix("http://")
                .substringBefore("/")
                .removePrefix("www.")
            host.substringBefore(".").replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }
}