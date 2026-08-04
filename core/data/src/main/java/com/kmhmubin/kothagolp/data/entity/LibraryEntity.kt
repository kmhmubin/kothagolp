package com.kmhmubin.kothagolp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.ReadingStatus

/**
 * Database entity for saved library novels with chapter tracking.
 */
@Entity(
    tableName = "library",
    // Covers the dominant query shape (WHERE deletedAt IS NULL ORDER BY
    // lastReadAt DESC, addedAt DESC — the main library listing) as a single
    // index scan instead of a full table scan on every library render.
    indices = [Index(value = ["deletedAt", "lastReadAt", "addedAt"])]
)
data class LibraryEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val posterUrl: String? = null,
    val apiName: String,
    val latestChapter: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val readingStatus: String = ReadingStatus.READING.name,

    // Reading position tracking
    val lastChapterUrl: String? = null,
    val lastChapterName: String? = null,
    val lastReadAt: Long? = null,
    val lastScrollIndex: Int = 0,
    val lastScrollOffset: Int = 0,

    // Chapter tracking
    val totalChapterCount: Int = 0,
    val acknowledgedChapterCount: Int = 0,
    val lastCheckedAt: Long = 0,
    val lastUpdatedAt: Long = 0,
    val lastReadChapterIndex: Int = -1,
    val unreadChapterCount: Int = 0,

    //Custom Cover
    val customCoverUrl: String? = null,

    /**
     * Soft-delete tombstone. Non-null = removed from the library at this time.
     *
     * Removal must survive as *state*, not absence: a deleted row that simply
     * vanished from the sync payload gets resurrected by the other device's
     * surviving copy on the next merge. The row stays here (and in backups) so
     * the deletion can win a newest-wins comparison, then is filtered out of
     * every library query.
     */
    val deletedAt: Long? = null,

    /**
     * Monotonic counter bumped on every *local* change (see LibraryDao). Writes
     * that merely apply synced data must NOT bump it — otherwise received data
     * looks locally authored and this device wins every future comparison.
     *
     * Used to detect "did this row change locally", independent of device
     * clocks. It counts edits, not progress, so it decides metadata conflicts
     * only — the reading position is still resolved by recency, because a device
     * that edited more often is not the device that read further.
     */
    val version: Long = 0,

    /**
     * The [version] this row had when it last agreed with Drive: the common
     * ancestor for a 3-way merge. `version > syncedVersion` means "changed here
     * since the last sync". Without this baseline every difference looks like a
     * conflict and has to be guessed at.
     */
    val syncedVersion: Long = 0
) {
    // Update toNovel() to use custom cover
    fun toNovel(): Novel = Novel(
        name = name,
        url = url,
        posterUrl = customCoverUrl ?: posterUrl,  // Prioritize custom cover
        apiName = apiName,
        latestChapter = latestChapter
    )
    /**
     * Number of new chapters since last acknowledgment
     */
    val newChapterCount: Int
        get() = (totalChapterCount - acknowledgedChapterCount).coerceAtLeast(0)

    /**
     * Whether there are new chapters to show badge
     */
    val hasNewChapters: Boolean
        get() = newChapterCount > 0


    fun getStatus(): ReadingStatus = try {
        ReadingStatus.valueOf(readingStatus)
    } catch (e: Exception) {
        ReadingStatus.READING
    }

    companion object {
        fun fromNovel(
            novel: Novel,
            status: ReadingStatus = ReadingStatus.READING,
            chapterCount: Int = 0
        ): LibraryEntity {
            return LibraryEntity(
                url = novel.url,
                name = novel.name,
                posterUrl = novel.posterUrl,
                apiName = novel.apiName,
                latestChapter = novel.latestChapter,
                readingStatus = status.name,
                totalChapterCount = chapterCount,
                acknowledgedChapterCount = chapterCount // No new chapters on add
            )
        }
    }
}