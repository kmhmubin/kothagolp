package com.kmhmubin.kothagolp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kmhmubin.kothagolp.data.local.entity.NovelDetailsEntity
import com.kmhmubin.kothagolp.data.local.entity.OfflineChapterEntity
import com.kmhmubin.kothagolp.data.local.entity.OfflineNovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {

    // ============ OFFLINE CHAPTERS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapter(chapter: OfflineChapterEntity)

    @Query("SELECT * FROM offline_chapters WHERE url = :url")
    suspend fun getChapter(url: String): OfflineChapterEntity?

    @Query("SELECT url FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getDownloadedChapterUrls(novelUrl: String): List<String>

    @Query("SELECT url FROM offline_chapters WHERE novelUrl = :novelUrl")
    fun getDownloadedChapterUrlsFlow(novelUrl: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getDownloadedCount(novelUrl: String): Int

    @Query("SELECT novelUrl, COUNT(*) as count FROM offline_chapters GROUP BY novelUrl")
    suspend fun getAllNovelCounts(): List<NovelChapterCount>

    @Query("""
        SELECT novelUrl, 
               COUNT(*) as count, 
               MAX(downloadedAt) as lastDownloadedAt 
        FROM offline_chapters 
        GROUP BY novelUrl
    """)
    suspend fun getAllNovelDownloadData(): List<NovelDownloadData>

    @Query("DELETE FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun deleteChaptersForNovel(novelUrl: String)

    @Query("DELETE FROM offline_chapters WHERE url IN (:urls)")
    suspend fun deleteChapters(urls: List<String>)

    // ============ OFFLINE NOVELS (Metadata) ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNovel(novel: OfflineNovelEntity)

    @Query("SELECT * FROM offline_novels WHERE url = :url")
    suspend fun getNovel(url: String): OfflineNovelEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM offline_novels WHERE url = :url)")
    suspend fun isNovelDownloaded(url: String): Boolean

    @Query("DELETE FROM offline_novels WHERE url = :url")
    suspend fun deleteNovel(url: String)

    // ============ NOVEL DETAILS CACHE ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNovelDetails(details: NovelDetailsEntity)

    @Query("SELECT * FROM novel_details WHERE url = :url")
    suspend fun getNovelDetails(url: String): NovelDetailsEntity?

    @Query("DELETE FROM novel_details WHERE url = :url")
    suspend fun deleteNovelDetails(url: String)

    /**
     * Delete cached details only if the novel is NOT in the library.
     * Library books' metadata must survive cache eviction so they stay
     * readable and migratable even when their source goes dark.
     */
    @Query("DELETE FROM novel_details WHERE url = :url AND url NOT IN (SELECT url FROM library)")
    suspend fun deleteNovelDetailsIfNotInLibrary(url: String)

    @Query("SELECT * FROM novel_details")
    suspend fun getAllNovelDetails(): List<NovelDetailsEntity>

    @Query("SELECT * FROM offline_chapters")
    suspend fun getAllChapters(): List<OfflineChapterEntity>

    @Query("DELETE FROM offline_chapters")
    suspend fun deleteAllChapters()

    @Query("DELETE FROM offline_novels")
    suspend fun deleteAllNovels()

    @Query("DELETE FROM novel_details")
    suspend fun deleteAllNovelDetails()

    /** Bulk cache clear that preserves library books' metadata. */
    @Query("DELETE FROM novel_details WHERE url NOT IN (SELECT url FROM library)")
    suspend fun deleteNonLibraryNovelDetails()

    /** Evictable (non-library) cached details, for accurate clear-cache size reporting. */
    @Query("SELECT * FROM novel_details WHERE url NOT IN (SELECT url FROM library)")
    suspend fun getNonLibraryNovelDetails(): List<NovelDetailsEntity>

    // ============ OFFLINE METADATA COVERAGE ============

    /** How many library books have offline metadata cached. Reactive — updates as rows land. */
    @Query("SELECT COUNT(*) FROM library WHERE url IN (SELECT url FROM novel_details)")
    fun libraryMetadataCoverageFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM library")
    fun libraryCountFlow(): Flow<Int>

    // ============ SIZE AGGREGATES (SQL-side; avoids loading content into memory) ============

    /** Estimated bytes of downloaded chapters (UTF-16 ≈ 2 bytes/char), computed in SQLite. */
    @Query("SELECT COALESCE(SUM(LENGTH(content) + LENGTH(title)) * 2, 0) FROM offline_chapters")
    suspend fun getChaptersSizeBytes(): Long

    @Query("SELECT COALESCE(SUM(LENGTH(content) + LENGTH(title)) * 2, 0) FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getNovelChaptersSizeBytes(novelUrl: String): Long

    @Query("SELECT COUNT(*) FROM offline_chapters")
    suspend fun getTotalChapterCount(): Int

    @Query("SELECT COUNT(*) FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getNovelChapterCount(novelUrl: String): Int

    /**
     * Estimated bytes of evictable (non-library) cached novel details, computed in SQLite.
     * chapters/tags/relatedNovelsJson are serialized TEXT columns, so LENGTH covers them.
     */
    @Query("""
        SELECT COALESCE(SUM(
            (LENGTH(name) + LENGTH(url) + LENGTH(apiName)
             + LENGTH(COALESCE(synopsis, '')) + LENGTH(COALESCE(author, ''))
             + LENGTH(COALESCE(status, '')) + LENGTH(COALESCE(posterUrl, ''))
             + LENGTH(COALESCE(relatedNovelsJson, '')) + LENGTH(COALESCE(chapters, ''))
             + LENGTH(COALESCE(tags, ''))) * 2
        ), 0)
        FROM novel_details WHERE url NOT IN (SELECT url FROM library)
    """)
    suspend fun getNonLibraryNovelDetailsSizeBytes(): Long

    @Query("SELECT COUNT(*) FROM novel_details WHERE url NOT IN (SELECT url FROM library)")
    suspend fun getNonLibraryNovelDetailsCount(): Int

    /** Per-novel download size + count in one grouped query — no chapter content leaves SQLite. */
    @Query("""
        SELECT novelUrl,
               COUNT(*) as count,
               COALESCE(SUM(LENGTH(content) + LENGTH(title)) * 2, 0) as sizeBytes
        FROM offline_chapters
        GROUP BY novelUrl
    """)
    suspend fun getNovelDownloadSizes(): List<NovelDownloadSize>
    /**
     * Get download info for all novels with downloaded chapters
     */
    @Query("""
    SELECT novelUrl, 
           COUNT(*) as chapterCount, 
           MAX(downloadedAt) as lastDownloadedAt 
    FROM offline_chapters 
    GROUP BY novelUrl
""")
    suspend fun getAllDownloadInfo(): List<DownloadInfoTuple>

    /**
     * Tuple for download info query result
     */
    data class DownloadInfoTuple(
        val novelUrl: String,
        val chapterCount: Int,
        val lastDownloadedAt: Long
    )

    // ============ CUSTOM COVER ============

    @Query("UPDATE novel_details SET customCoverUrl = :coverUrl WHERE url = :novelUrl")
    suspend fun updateNovelDetailsCustomCover(novelUrl: String, coverUrl: String?)

    @Query("UPDATE offline_novels SET customCoverUrl = :coverUrl WHERE url = :novelUrl")
    suspend fun updateOfflineNovelCustomCover(novelUrl: String, coverUrl: String?)
}

/**
 * Helper class for count query results
 */
data class NovelChapterCount(
    val novelUrl: String,
    val count: Int
)

/**
 * Helper class for download data with timestamps
 */
data class NovelDownloadData(
    val novelUrl: String,
    val count: Int,
    val lastDownloadedAt: Long?
)

/**
 * Per-novel download size computed in SQL
 */
data class NovelDownloadSize(
    val novelUrl: String,
    val count: Int,
    val sizeBytes: Long
)