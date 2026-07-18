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

    /**
     * Merges local and remote library snapshots.
     *
     * Books are keyed by URL only — a book's identity is its source + URL. The
     * previous version also tried to guess cross-source duplicates by comparing
     * normalized titles and silently dropped the "losing" copy; that both merged
     * genuinely unrelated same-titled books and fought with migration, which now
     * records its own tombstone for the old source instead.
     *
     * Conflict rules mirror LibraryEntity.mergeForSync: the reading position is
     * taken atomically from whichever side read last, deletions compete with
     * activity on recency, and only monotonic counters take the max.
     */
    private fun mergeLibrary(
        local: List<LibraryBackup>,
        remote: List<LibraryBackup>
    ): List<LibraryBackup> {
        return (local + remote)
            .groupBy { it.url }
            .map { (_, entries) ->
                if (entries.size == 1) entries.first() else entries.reduce(::mergeLibraryEntry)
            }
            .sortedByDescending { libraryTimestamp(it) }
    }

    /**
     * Payload-level merge of one book from two snapshots.
     *
     * Note this is deliberately *not* the 3-way merge that
     * LibraryEntity.mergeForSync performs. The baseline (`syncedVersion`) is
     * per-device local bookkeeping and never travels in a payload, so two
     * snapshots alone cannot say which side changed since they last agreed.
     * The authoritative 3-way decision happens on restore, against the local
     * row; this pass only has to produce a payload that loses nothing, so it
     * resolves by recency and keeps the highest version seen.
     */
    private fun mergeLibraryEntry(a: LibraryBackup, b: LibraryBackup): LibraryBackup {
        // Whole-entity winner for the reading position (ties keep `a`).
        val newestRead = if ((b.lastReadAt ?: 0L) > (a.lastReadAt ?: 0L)) b else a
        val newestMeta = if (b.lastUpdatedAt > a.lastUpdatedAt) b else a

        // Deletion vs. presence is decided by the change counter first: a higher
        // version is the more recent authoritative edit, so its delete/re-add
        // state wins. This is what lets a re-add (which bumps version past the
        // synced tombstone) survive — recency alone can't, because re-adding a
        // book carries no fresh read/update timestamp to beat the deletion time.
        // Only when versions tie do we fall back to activity recency.
        val mergedDeletedAt = when {
            a.deletedAt == null && b.deletedAt == null -> null
            a.version > b.version -> a.deletedAt
            b.version > a.version -> b.deletedAt
            a.deletedAt != null && b.deletedAt != null -> maxOf(a.deletedAt, b.deletedAt)
            else -> {
                val deletion = a.deletedAt ?: b.deletedAt!!
                val otherSide = if (a.deletedAt != null) b else a
                val otherActivity = maxOf(otherSide.lastReadAt ?: 0L, otherSide.lastUpdatedAt)
                if (otherActivity > deletion) null else deletion
            }
        }

        return newestMeta.copy(
            name = newestMeta.name.ifBlank { newestRead.name },
            posterUrl = newestMeta.posterUrl ?: newestRead.posterUrl,
            latestChapter = newestMeta.latestChapter ?: newestRead.latestChapter,
            addedAt = minOf(a.addedAt, b.addedAt),
            // --- atomic reading position ---
            lastChapterUrl = newestRead.lastChapterUrl,
            lastChapterName = newestRead.lastChapterName,
            lastReadAt = newestRead.lastReadAt,
            lastScrollIndex = newestRead.lastScrollIndex,
            lastScrollOffset = newestRead.lastScrollOffset,
            lastReadChapterIndex = newestRead.lastReadChapterIndex,
            unreadChapterCount = newestRead.unreadChapterCount.coerceAtLeast(0),
            // --- monotonic counters ---
            totalChapterCount = maxOf(a.totalChapterCount, b.totalChapterCount),
            acknowledgedChapterCount = maxOf(a.acknowledgedChapterCount, b.acknowledgedChapterCount),
            lastCheckedAt = maxOf(a.lastCheckedAt, b.lastCheckedAt),
            lastUpdatedAt = maxOf(a.lastUpdatedAt, b.lastUpdatedAt),
            deletedAt = mergedDeletedAt,
            // Keep the highest counter so the merged payload supersedes both
            // sides on every device that later merges against it.
            version = maxOf(a.version, b.version)
        )
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
                // Read and unread are competing actions on the same chapter.
                // The winner is whichever happened last: readAt vs unreadAt.
                // A row without an unread tombstone dates from readAt; a
                // tombstoned row's action time is unreadAt.
                entries.maxByOrNull { maxOf(it.readAt, it.unreadAt ?: Long.MIN_VALUE) }
                    ?: entries.first()
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
        // Prioritize lastReadAt for bidirectional sync: "which device read further"
        // Falls back to other timestamps if no read activity
        return entry.lastReadAt ?: maxOf(
            entry.lastUpdatedAt,
            entry.lastCheckedAt,
            entry.addedAt
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
