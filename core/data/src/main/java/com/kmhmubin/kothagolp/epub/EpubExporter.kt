package com.kmhmubin.kothagolp.epub

import android.content.Context
import android.net.Uri
import com.kmhmubin.kothagolp.data.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Main class for exporting novels to EPUB format
 */
class EpubExporter(
    private val context: Context,
    private val offlineRepository: OfflineRepository
) {
    private val writer = EpubWriter(context)

    private val _exportState = MutableStateFlow(EpubExportState())
    val exportState: StateFlow<EpubExportState> = _exportState.asStateFlow()

    /**
     * Export a novel to EPUB
     *
     * @param novelUrl URL of the novel
     * @param outputUri URI where the EPUB should be saved
     * @param options Export options
     */
    suspend fun exportToEpub(
        novelUrl: String,
        outputUri: Uri,
        options: EpubExportOptions = EpubExportOptions()
    ): EpubExportResult = withContext(Dispatchers.IO) {
        try {
            _exportState.value = EpubExportState(
                isExporting = true,
                currentStep = "Loading novel data..."
            )

            // 1. Load novel details
            val novelDetails = offlineRepository.getNovelDetails(novelUrl)
                ?: return@withContext EpubExportResult(
                    success = false,
                    error = "Novel details not found. Please open the novel first."
                )

            // 2. Get downloaded chapter URLs
            val downloadedUrls = offlineRepository.getDownloadedChapterUrls(novelUrl)
            if (downloadedUrls.isEmpty()) {
                return@withContext EpubExportResult(
                    success = false,
                    error = "No downloaded chapters found."
                )
            }

            // 3. Filter chapters that are downloaded
            val allChapters = novelDetails.chapters
            val downloadedChapters = allChapters.filter { downloadedUrls.contains(it.url) }

            // 4. Apply chapter range filter if specified
            val chaptersToExport = options.chapterRange?.let { range ->
                downloadedChapters.filterIndexed { index, _ ->
                    index in range
                }
            } ?: downloadedChapters

            if (chaptersToExport.isEmpty()) {
                return@withContext EpubExportResult(
                    success = false,
                    error = "No chapters to export in the selected range."
                )
            }

            _exportState.value = _exportState.value.copy(
                totalChapters = chaptersToExport.size,
                currentStep = "Loading chapter content..."
            )

            // 5. Load chapter content
            val epubChapters = mutableListOf<EpubChapter>()
            chaptersToExport.forEachIndexed { index, chapter ->
                _exportState.value = _exportState.value.copy(
                    currentChapter = index + 1,
                    progress = (index.toFloat() / chaptersToExport.size) * 0.5f,
                    currentStep = "Loading: ${chapter.name}"
                )

                val content = offlineRepository.getChapterContent(chapter.url)
                if (content != null) {
                    epubChapters.add(
                        EpubChapter(
                            index = index,
                            title = chapter.name,
                            content = content,
                            originalUrl = chapter.url
                        )
                    )
                }
            }

            if (epubChapters.isEmpty()) {
                return@withContext EpubExportResult(
                    success = false,
                    error = "Could not load any chapter content."
                )
            }

            // 6. Build metadata
            val metadata = EpubMetadata(
                title = options.customTitle ?: novelDetails.name,
                author = options.customAuthor ?: novelDetails.author,
                description = novelDetails.synopsis,
                coverUrl = novelDetails.posterUrl,
                tags = novelDetails.tags ?: emptyList()
            )

            // 7. Download cover image if requested
            var coverBytes: ByteArray? = null
            if (options.includeCover && metadata.coverUrl != null) {
                _exportState.value = _exportState.value.copy(
                    progress = 0.55f,
                    currentStep = "Downloading cover image..."
                )
                coverBytes = writer.downloadCoverImage(metadata.coverUrl)
            }

            // 8. Write EPUB
            _exportState.value = _exportState.value.copy(
                progress = 0.6f,
                currentStep = "Creating EPUB..."
            )

            val writeResult = writer.writeEpubToUri(
                uri = outputUri,
                metadata = metadata,
                chapters = epubChapters,
                coverImageBytes = coverBytes
            ) { current, total, step ->
                val baseProgress = 0.6f
                val writeProgress = (current.toFloat() / total) * 0.4f
                _exportState.value = _exportState.value.copy(
                    progress = baseProgress + writeProgress,
                    currentStep = step
                )
            }

            if (writeResult.isFailure) {
                val error = writeResult.exceptionOrNull()?.message ?: "Unknown error"
                _exportState.value = _exportState.value.copy(
                    isExporting = false,
                    error = error
                )
                return@withContext EpubExportResult(
                    success = false,
                    error = error
                )
            }

            // 9. Get file size
            val fileSize = try {
                context.contentResolver.openFileDescriptor(outputUri, "r")?.use {
                    it.statSize
                } ?: 0L
            } catch (e: Exception) {
                0L
            }

            _exportState.value = EpubExportState(
                isExporting = false,
                isComplete = true,
                progress = 1f,
                currentStep = "Export complete!",
                totalChapters = epubChapters.size
            )

            EpubExportResult(
                success = true,
                fileName = metadata.safeFileName,
                chapterCount = epubChapters.size,
                fileSizeBytes = fileSize
            )

        } catch (e: Exception) {
            e.printStackTrace()
            _exportState.value = EpubExportState(
                isExporting = false,
                error = e.message ?: "Unknown error"
            )
            EpubExportResult(
                success = false,
                error = e.message ?: "Unknown error during export"
            )
        }
    }

    /**
     * Generate suggested filename for a novel
     */
    fun generateFileName(novelName: String): String {
        return "${novelName.replace(Regex("[<>:\"/\\\\|?*]"), "_").take(100)}.epub"
    }

    /**
     * Reset export state
     */
    fun resetState() {
        _exportState.value = EpubExportState()
    }
}