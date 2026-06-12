package com.kmhmubin.kothagolp.epub

/**
 * State for EPUB export operation
 */
data class EpubExportState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val currentStep: String = "",
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val error: String? = null,
    val isComplete: Boolean = false,
    val exportedFilePath: String? = null
) {
    val progressPercent: Int get() = (progress * 100).toInt()

    val statusText: String get() = when {
        error != null -> "Error: $error"
        isComplete -> "Export complete!"
        isExporting -> currentStep
        else -> "Ready to export"
    }
}

/**
 * Export options
 */
data class EpubExportOptions(
    val includeCover: Boolean = true,
    val includeMetadata: Boolean = true,
    val chapterRange: IntRange? = null, // null = all chapters
    val customTitle: String? = null,
    val customAuthor: String? = null
)

/**
 * Result of an export operation
 */
data class EpubExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val chapterCount: Int = 0,
    val fileSizeBytes: Long = 0,
    val error: String? = null
) {
    val formattedFileSize: String get() {
        return when {
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
            else -> String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
        }
    }
}