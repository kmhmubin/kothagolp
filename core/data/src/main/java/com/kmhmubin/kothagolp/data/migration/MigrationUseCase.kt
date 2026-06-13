package com.kmhmubin.kothagolp.data.migration

import android.util.Log
import androidx.room.withTransaction
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.entity.HistoryEntity
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.data.local.entity.ReadChapterEntity
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.Novel

class MigrationUseCase(private val database: NovelDatabase) {

    private val libraryDao get() = database.libraryDao()
    private val historyDao get() = database.historyDao()

    sealed class Result {
        data class Success(val migratedReadCount: Int) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Migrate [fromEntry] to [toNovel] using [toChapters] as the new chapter list.
     *
     * - Preserves reading status, custom cover, addedAt, notes.
     * - Maps read chapters by chapter number parsed from name.
     * - Falls back to index-based matching when numbers can't be parsed.
     */
    suspend fun migrate(
        fromEntry: LibraryEntity,
        toNovel: Novel,
        toChapters: List<Chapter>
    ): Result {
        return try {
            val readUrls = historyDao.getReadChapterUrls(fromEntry.url).toSet()

            // Try to load old chapter list for number-based matching.
            val fromProvider = com.kmhmubin.kothagolp.provider.MainProvider
                .getProviders().find { it.name == fromEntry.apiName }
            val fromChapters: List<Chapter> = try {
                fromProvider?.load(fromEntry.url)?.chapters ?: emptyList()
            } catch (_: Exception) { emptyList() }

            // Map: chapterNumber → whether it was read in old source
            val readNumbers: Set<Float> = if (fromChapters.isNotEmpty()) {
                fromChapters
                    .filter { it.url in readUrls }
                    .mapNotNull { parseChapterNumber(it.name) }
                    .toSet()
            } else emptySet()

            // Select new chapters to mark as read
            val newReadChapters: List<Chapter> = when {
                readNumbers.isNotEmpty() -> {
                    // Number-based: mark chapters whose numbers match
                    toChapters.filter { chapter ->
                        val num = parseChapterNumber(chapter.name)
                        num != null && num in readNumbers
                    }.ifEmpty {
                        // Numbers parsed but nothing matched — fall back to index
                        indexBasedRead(fromEntry, toChapters)
                    }
                }
                fromEntry.lastReadChapterIndex >= 0 -> {
                    // Index-based: mark all chapters up to lastReadChapterIndex
                    indexBasedRead(fromEntry, toChapters)
                }
                else -> emptyList()
            }

            val newLastChapter = newReadChapters.lastOrNull()
            val newEntry = fromEntry.copy(
                url = toNovel.url,
                name = toNovel.name,
                posterUrl = toNovel.posterUrl,
                apiName = toNovel.apiName,
                latestChapter = toChapters.lastOrNull()?.name ?: fromEntry.latestChapter,
                totalChapterCount = toChapters.size,
                acknowledgedChapterCount = newReadChapters.size,
                lastChapterUrl = newLastChapter?.url ?: fromEntry.lastChapterUrl,
                lastChapterName = newLastChapter?.name ?: fromEntry.lastChapterName,
                lastReadChapterIndex = if (newReadChapters.isNotEmpty()) newReadChapters.size - 1
                else fromEntry.lastReadChapterIndex,
                unreadChapterCount = maxOf(0, toChapters.size - newReadChapters.size)
            )

            database.withTransaction {
                // Insert new library entry (REPLACE handles potential duplicate)
                libraryDao.insert(newEntry)

                // Insert read chapters for new novel
                if (newReadChapters.isNotEmpty()) {
                    historyDao.markChaptersRead(
                        newReadChapters.map { ch ->
                            ReadChapterEntity(chapterUrl = ch.url, novelUrl = toNovel.url)
                        }
                    )
                }

                // Migrate history entry
                val oldHistory = historyDao.getByNovelUrl(fromEntry.url)
                if (oldHistory != null) {
                    val historyChapter = newLastChapter
                    historyDao.insert(
                        HistoryEntity(
                            novelUrl = toNovel.url,
                            novelName = toNovel.name,
                            posterUrl = newEntry.customCoverUrl ?: toNovel.posterUrl,
                            chapterName = historyChapter?.name ?: oldHistory.chapterName,
                            chapterUrl = historyChapter?.url ?: oldHistory.chapterUrl,
                            apiName = toNovel.apiName,
                            timestamp = oldHistory.timestamp,
                            customCoverUrl = newEntry.customCoverUrl
                        )
                    )
                    historyDao.deleteByNovelUrl(fromEntry.url)
                }

                // Remove old library entry and its read chapters
                historyDao.clearReadChapters(fromEntry.url)
                libraryDao.deleteByUrl(fromEntry.url)
            }

            Log.i(TAG, "Migrated '${fromEntry.name}' → '${toNovel.name}' (${toNovel.apiName}), ${newReadChapters.size} chapters transferred")
            Result.Success(newReadChapters.size)
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed for '${fromEntry.name}'", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private fun indexBasedRead(from: LibraryEntity, toChapters: List<Chapter>): List<Chapter> {
        if (from.lastReadChapterIndex < 0) return emptyList()
        return toChapters.take(from.lastReadChapterIndex + 1)
    }

    companion object {
        private const val TAG = "MigrationUseCase"

        private val NUMBER_PATTERNS = listOf(
            Regex("""(?:chapter|ch|ep|episode)[.\s]*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9]+(?:\.[0-9]+)?)\s*[-:]"""),
            Regex("""^\s*([0-9]+(?:\.[0-9]+)?)"""),
        )

        fun parseChapterNumber(name: String): Float? {
            for (pattern in NUMBER_PATTERNS) {
                val value = pattern.find(name)?.groupValues?.getOrNull(1)?.toFloatOrNull()
                if (value != null) return value
            }
            return null
        }
    }
}
