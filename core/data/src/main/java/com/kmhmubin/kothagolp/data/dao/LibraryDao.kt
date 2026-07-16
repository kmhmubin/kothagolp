package com.kmhmubin.kothagolp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // ============ BASIC QUERIES ============

    @Query("SELECT * FROM library WHERE deletedAt IS NULL ORDER BY lastReadAt DESC, addedAt DESC")
    fun getAllFlow(): Flow<List<LibraryEntity>>

    @Query("SELECT url FROM library WHERE deletedAt IS NULL")
    fun observeLibraryUrls(): Flow<List<String>>

    @Query("SELECT * FROM library WHERE deletedAt IS NULL ORDER BY lastReadAt DESC, addedAt DESC")
    suspend fun getAll(): List<LibraryEntity>

    /**
     * Includes tombstoned rows — backup/sync payloads must carry deletions,
     * otherwise a removal never reaches the other device and the book is
     * resurrected from its stale copy on the next merge.
     */
    @Query("SELECT * FROM library ORDER BY lastReadAt DESC, addedAt DESC")
    suspend fun getAllForSync(): List<LibraryEntity>

    @Query("SELECT * FROM library WHERE url = :url AND deletedAt IS NULL")
    suspend fun getByUrl(url: String): LibraryEntity?

    /** Includes tombstoned rows — sync/merge needs to see deletions. */
    @Query("SELECT * FROM library WHERE url = :url")
    suspend fun getByUrlIncludingDeleted(url: String): LibraryEntity?

    @Query("SELECT * FROM library WHERE url = :url AND deletedAt IS NULL")
    fun getByUrlFlow(url: String): Flow<LibraryEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url AND deletedAt IS NULL)")
    suspend fun exists(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url AND deletedAt IS NULL)")
    fun existsFlow(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LibraryEntity)

    @Update
    suspend fun update(entity: LibraryEntity)

    @Query("UPDATE library SET readingStatus = :status, lastUpdatedAt = :updatedAt, version = version + 1 WHERE url = :url")
    suspend fun updateStatus(url: String, status: String, updatedAt: Long)

    // ============ READING POSITION ============

    /**
     * A reading position is one atomic unit: chapter URL, chapter index, scroll
     * and timestamp must be written together. `lastReadChapterIndex` used to be
     * left out here, so it went stale while the rest advanced — and sync then
     * paired a fresh chapter URL with a stale index, sending the reader back to
     * whatever chapter that old index pointed at.
     */
    @Query("""
        UPDATE library SET 
            lastChapterUrl = :chapterUrl,
            lastChapterName = :chapterName,
            lastReadAt = :timestamp,
            lastScrollIndex = :scrollIndex,
            lastScrollOffset = :scrollOffset,
            lastReadChapterIndex = :chapterIndex,
            version = version + 1
        WHERE url = :novelUrl
    """)
    suspend fun updateReadingPosition(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        timestamp: Long,
        scrollIndex: Int,
        scrollOffset: Int,
        chapterIndex: Int
    )

    @Query("""
        UPDATE library SET 
            lastChapterUrl = :chapterUrl,
            lastChapterName = :chapterName,
            lastReadAt = :timestamp,
            version = version + 1
        WHERE url = :novelUrl
    """)
    suspend fun updateLastChapter(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        timestamp: Long
    )

    // ============ NEW: CHAPTER COUNT & BADGE TRACKING ============

    /**
     * Update chapter counts after refreshing novel details
     */
    @Query("""
        UPDATE library SET 
            totalChapterCount = :totalCount,
            lastCheckedAt = :checkedAt,
            lastUpdatedAt = CASE 
                WHEN totalChapterCount != :totalCount THEN :checkedAt 
                ELSE lastUpdatedAt 
            END,
            latestChapter = :latestChapter
        WHERE url = :novelUrl
    """)
    suspend fun updateChapterCount(
        novelUrl: String,
        totalCount: Int,
        latestChapter: String?,
        checkedAt: Long = System.currentTimeMillis()
    )

    /**
     * Acknowledge new chapters (clear badge)
     * Called when user views the novel details
     */
    @Query("""
        UPDATE library SET 
            acknowledgedChapterCount = totalChapterCount
        WHERE url = :novelUrl
    """)
    suspend fun acknowledgeNewChapters(novelUrl: String)

    /**
     * Update unread chapter tracking
     */
    @Query("""
        UPDATE library SET 
            lastReadChapterIndex = :chapterIndex,
            unreadChapterCount = :unreadCount,
            version = version + 1
        WHERE url = :novelUrl
    """)
    suspend fun updateUnreadTracking(
        novelUrl: String,
        chapterIndex: Int,
        unreadCount: Int
    )

    /**
     * Get novels with new chapters
     */
    @Query("""
        SELECT * FROM library 
        WHERE deletedAt IS NULL AND totalChapterCount > acknowledgedChapterCount
        ORDER BY lastUpdatedAt DESC
    """)
    suspend fun getNovelsWithNewChapters(): List<LibraryEntity>

    /**
     * Get novels with new chapters as Flow
     */
    @Query("""
        SELECT * FROM library 
        WHERE deletedAt IS NULL AND totalChapterCount > acknowledgedChapterCount
        ORDER BY lastUpdatedAt DESC
    """)
    fun observeNovelsWithNewChapters(): Flow<List<LibraryEntity>>

    /**
     * Get total count of new chapters across all novels
     */
    @Query("""
        SELECT COALESCE(SUM(totalChapterCount - acknowledgedChapterCount), 0)
        FROM library
        WHERE deletedAt IS NULL AND totalChapterCount > acknowledgedChapterCount
    """)
    suspend fun getTotalNewChapterCount(): Int

    /**
     * Get total count as Flow
     */
    @Query("""
        SELECT COALESCE(SUM(totalChapterCount - acknowledgedChapterCount), 0)
        FROM library
        WHERE deletedAt IS NULL AND totalChapterCount > acknowledgedChapterCount
    """)
    fun observeTotalNewChapterCount(): Flow<Int>

    /**
     * Get novels that need refresh (not checked recently)
     */
    @Query("""
        SELECT * FROM library 
        WHERE deletedAt IS NULL AND lastCheckedAt < :threshold
        ORDER BY lastCheckedAt ASC
        LIMIT :limit
    """)
    suspend fun getNovelsNeedingRefresh(threshold: Long, limit: Int = 10): List<LibraryEntity>

    // ============ DELETE ============

    /**
     * Soft-delete: keep the row as a tombstone so the removal survives a sync
     * merge instead of being resurrected by the other device's stale copy.
     */
    @Query("UPDATE library SET deletedAt = :deletedAt, version = version + 1 WHERE url = :url")
    suspend fun softDelete(url: String, deletedAt: Long = System.currentTimeMillis())

    /** Re-adding a previously removed book clears its tombstone. */
    @Query("UPDATE library SET deletedAt = NULL, version = version + 1 WHERE url = :url")
    suspend fun clearTombstone(url: String)

    @Query("DELETE FROM library WHERE url = :url")
    suspend fun deleteHard(url: String)

    @Query("DELETE FROM library")
    suspend fun deleteAll()

    /**
     * Records that this row's current [LibraryEntity.version] is what Drive has.
     * Deliberately does not bump `version`: this is sync bookkeeping, not a user
     * edit, and treating it as one would make every synced row look changed.
     */
    @Query("UPDATE library SET syncedVersion = :version WHERE url = :url")
    suspend fun markSynced(url: String, version: Long)

    /** Rows changed locally since the last sync agreement. */
    @Query("SELECT * FROM library WHERE version > syncedVersion")
    suspend fun getLocallyChanged(): List<LibraryEntity>

    /** Tombstones older than [threshold] are safe to purge permanently. */
    @Query("DELETE FROM library WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun purgeOldTombstones(threshold: Long)

    // ============ SEARCH ============

    @Query("""
        SELECT * FROM library 
        WHERE deletedAt IS NULL
          AND (name LIKE '%' || :query || '%' 
           OR apiName LIKE '%' || :query || '%')
        ORDER BY lastReadAt DESC, addedAt DESC
    """)
    suspend fun search(query: String): List<LibraryEntity>

    // ============ CUSTOM COVER ============

    @Query("UPDATE library SET customCoverUrl = :coverUrl, version = version + 1 WHERE url = :novelUrl")
    suspend fun updateCustomCover(novelUrl: String, coverUrl: String?)

    @Query("SELECT customCoverUrl FROM library WHERE url = :novelUrl")
    suspend fun getCustomCover(novelUrl: String): String?

    // ============ MIGRATION ============

    @Query("SELECT * FROM library WHERE apiName = :sourceName AND deletedAt IS NULL ORDER BY name ASC")
    suspend fun getBySourceName(sourceName: String): List<LibraryEntity>

    @Query("SELECT DISTINCT apiName FROM library WHERE deletedAt IS NULL ORDER BY apiName ASC")
    suspend fun getDistinctSources(): List<String>

    @Query("UPDATE library SET deletedAt = :deletedAt, version = version + 1 WHERE url = :url")
    suspend fun deleteByUrl(url: String, deletedAt: Long = System.currentTimeMillis())
}
