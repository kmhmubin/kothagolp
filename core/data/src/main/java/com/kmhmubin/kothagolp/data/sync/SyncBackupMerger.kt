package com.kmhmubin.kothagolp.data.sync

import com.kmhmubin.kothagolp.data.backup.AppSettingsBackup
import com.kmhmubin.kothagolp.data.backup.BackupData
import com.kmhmubin.kothagolp.data.backup.BookmarkBackup
import com.kmhmubin.kothagolp.data.backup.HistoryBackup
import com.kmhmubin.kothagolp.data.backup.LibraryBackup
import com.kmhmubin.kothagolp.data.backup.ReadChapterBackup
import com.kmhmubin.kothagolp.data.backup.ReaderSettingsBackup
import com.kmhmubin.kothagolp.data.backup.ReadingStatsBackup
import com.kmhmubin.kothagolp.data.backup.ReadingStreakBackup

/**
 * Merges local and remote backup snapshots into a single sync-safe payload.
 */
object SyncBackupMerger {

    fun merge(local: BackupData, remote: BackupData): BackupData {
        return BackupData(
            version = maxOf(local.version, remote.version),
            createdAt = maxOf(local.createdAt, remote.createdAt),
            appVersion = local.appVersion.ifBlank { remote.appVersion },
            deviceInfo = local.deviceInfo.ifBlank { remote.deviceInfo },
            library = mergeLibrary(local.library, remote.library),
            bookmarks = mergeBookmarks(local.bookmarks, remote.bookmarks),
            history = mergeHistory(local.history, remote.history),
            readChapters = mergeReadChapters(local.readChapters, remote.readChapters),
            readingStats = mergeReadingStats(local.readingStats, remote.readingStats),
            readingStreak = mergeReadingStreak(local.readingStreak, remote.readingStreak),
            appSettings = mergeAppSettings(local.appSettings, remote.appSettings),
            readerSettings = mergeReaderSettings(local.readerSettings, remote.readerSettings)
        )
    }

    private fun mergeLibrary(
        local: List<LibraryBackup>,
        remote: List<LibraryBackup>
    ): List<LibraryBackup> {
        val allBooks = local + remote

        // Cross-source dedup: if same book exists in multiple sources, keep newest
        // & remove older. Detects title migrations (SourceA → SourceB).
        val deduplicated = mutableMapOf<String, LibraryBackup>()
        val titleIndex = mutableMapOf<String, MutableList<LibraryBackup>>()

        // First pass: group by title (normalized) to find cross-source duplicates
        for (book in allBooks) {
            val normalized = normalizeTitleForDedup(book.name)
            titleIndex.getOrPut(normalized) { mutableListOf() }.add(book)
        }

        // Second pass: for each title group, keep only the best book (newest activity)
        for ((_, booksWithSameTitle) in titleIndex) {
            if (booksWithSameTitle.size == 1) {
                // No duplicate, keep as-is
                val book = booksWithSameTitle.first()
                deduplicated[book.url] = book
            } else {
                // Multiple books with same title (cross-source migration case)
                // Check if they're from different sources
                val hasDifferentSources = booksWithSameTitle.map { it.apiName }.toSet().size > 1
                if (hasDifferentSources) {
                    // Cross-source duplicate: keep the one with most recent activity
                    val best = booksWithSameTitle.maxByOrNull {
                        maxOf(it.lastReadAt ?: 0L, it.lastUpdatedAt, it.addedAt)
                    } ?: booksWithSameTitle.first()
                    deduplicated[best.url] = best
                    // Don't add others (they're removed by not adding to map)
                } else {
                    // Same source (shouldn't happen), keep all
                    for (book in booksWithSameTitle) {
                        deduplicated[book.url] = book
                    }
                }
            }
        }

        // Third pass: merge same-URL books (standard conflict resolution)
        return deduplicated.values
            .groupBy { it.url }
            .map { (_, entries) ->
                val newestRead = entries.maxByOrNull { it.lastReadAt ?: 0L } ?: entries.first()
                val newestUpdate = entries.maxByOrNull { it.lastUpdatedAt }
                val newestMeta = entries.maxByOrNull { libraryTimestamp(it) } ?: entries.first()

                newestMeta.copy(
                    name = newestMeta.name.ifBlank { newestRead.name },
                    posterUrl = newestMeta.posterUrl ?: newestRead.posterUrl,
                    latestChapter = newestUpdate?.latestChapter ?: newestMeta.latestChapter,
                    lastChapterUrl = newestRead.lastChapterUrl,
                    lastChapterName = newestRead.lastChapterName,
                    lastReadAt = newestRead.lastReadAt,
                    lastScrollIndex = newestRead.lastScrollIndex,
                    lastScrollOffset = newestRead.lastScrollOffset,
                    totalChapterCount = entries.maxOf { it.totalChapterCount },
                    acknowledgedChapterCount = entries.maxOf { it.acknowledgedChapterCount },
                    lastCheckedAt = entries.maxOf { it.lastCheckedAt },
                    lastUpdatedAt = entries.maxOf { it.lastUpdatedAt },
                    lastReadChapterIndex = entries.maxOf { it.lastReadChapterIndex },
                    unreadChapterCount = newestRead.unreadChapterCount.coerceAtLeast(0)
                )
            }
            .sortedByDescending { libraryTimestamp(it) }
    }

    private fun normalizeTitleForDedup(title: String): String {
        return title
            .lowercase()
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\w\s]"""), "")
    }

    private fun mergeBookmarks(
        local: List<BookmarkBackup>,
        remote: List<BookmarkBackup>
    ): List<BookmarkBackup> {
        return (local + remote)
            .groupBy { bookmarkKey(it) }
            .map { (_, entries) ->
                entries.maxByOrNull { it.updatedAt } ?: entries.first()
            }
            .sortedByDescending { it.updatedAt }
    }

    private fun mergeHistory(
        local: List<HistoryBackup>,
        remote: List<HistoryBackup>
    ): List<HistoryBackup> {
        return (local + remote)
            .groupBy { it.novelUrl }
            .map { (_, entries) ->
                entries.maxByOrNull { it.timestamp } ?: entries.first()
            }
            .sortedByDescending { it.timestamp }
    }

    private fun mergeReadChapters(
        local: List<ReadChapterBackup>,
        remote: List<ReadChapterBackup>
    ): List<ReadChapterBackup> {
        return (local + remote)
            .groupBy { it.chapterUrl }
            .map { (_, entries) ->
                entries.maxByOrNull { it.readAt } ?: entries.first()
            }
            .sortedByDescending { it.readAt }
    }

    private fun mergeReadingStats(
        local: List<ReadingStatsBackup>,
        remote: List<ReadingStatsBackup>
    ): List<ReadingStatsBackup> {
        return (local + remote)
            .groupBy { "${it.novelUrl}|${it.date}" }
            .map { (_, entries) ->
                val newest = entries.maxByOrNull { it.updatedAt } ?: entries.first()
                newest.copy(
                    readingTimeSeconds = entries.maxOf { it.readingTimeSeconds },
                    chaptersRead = entries.maxOf { it.chaptersRead },
                    wordsRead = entries.maxOf { it.wordsRead },
                    sessionsCount = entries.maxOf { it.sessionsCount },
                    longestSessionSeconds = entries.maxOf { it.longestSessionSeconds },
                    createdAt = entries.minOf { it.createdAt },
                    updatedAt = entries.maxOf { it.updatedAt }
                )
            }
            .sortedByDescending { it.updatedAt }
    }

    private fun mergeReadingStreak(
        local: ReadingStreakBackup?,
        remote: ReadingStreakBackup?
    ): ReadingStreakBackup? {
        return when {
            local == null -> remote
            remote == null -> local
            else -> {
                val newest = if (local.updatedAt >= remote.updatedAt) local else remote
                newest.copy(
                    currentStreak = maxOf(local.currentStreak, remote.currentStreak),
                    longestStreak = maxOf(local.longestStreak, remote.longestStreak),
                    lastReadDate = maxOf(local.lastReadDate, remote.lastReadDate),
                    totalDaysRead = maxOf(local.totalDaysRead, remote.totalDaysRead),
                    totalReadingTimeSeconds = maxOf(
                        local.totalReadingTimeSeconds,
                        remote.totalReadingTimeSeconds
                    ),
                    updatedAt = maxOf(local.updatedAt, remote.updatedAt)
                )
            }
        }
    }

    private fun mergeAppSettings(
        local: AppSettingsBackup?,
        remote: AppSettingsBackup?
    ): AppSettingsBackup? {
        return pickNewest(local, remote) { it.updatedAt }
    }

    private fun mergeReaderSettings(
        local: ReaderSettingsBackup?,
        remote: ReaderSettingsBackup?
    ): ReaderSettingsBackup? {
        return pickNewest(local, remote) { it.updatedAt }
    }

    private fun bookmarkKey(bookmark: BookmarkBackup): String {
        val segment = bookmark.segmentId ?: bookmark.textSnippet.orEmpty()
        return buildString {
            append(bookmark.novelUrl)
            append('|')
            append(bookmark.chapterUrl)
            append('|')
            append(segment)
            append('|')
            append(bookmark.segmentIndex)
            append('|')
            append(bookmark.category)
        }
    }

    private fun libraryTimestamp(entry: LibraryBackup): Long {
        return maxOf(
            entry.addedAt,
            entry.lastReadAt ?: 0L,
            entry.lastCheckedAt,
            entry.lastUpdatedAt
        )
    }

    private fun <T> pickNewest(
        local: T?,
        remote: T?,
        timestamp: (T) -> Long
    ): T? {
        return when {
            local == null -> remote
            remote == null -> local
            timestamp(local) >= timestamp(remote) -> local
            else -> remote
        }
    }
}
