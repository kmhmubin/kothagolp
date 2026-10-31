package com.kmhmubin.kothagolp.data.repository

import com.kmhmubin.kothagolp.data.local.dao.HistoryDao
import com.kmhmubin.kothagolp.data.local.entity.HistoryEntity
import com.kmhmubin.kothagolp.data.local.entity.ReadChapterEntity
import com.kmhmubin.kothagolp.domain.model.Novel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRestoreTest {

    private class FakeHistoryDao : HistoryDao {
        val inserted = mutableListOf<HistoryEntity>()
        var readMarks = 0
        override suspend fun insert(entity: HistoryEntity) { inserted += entity }
        override suspend fun markChapterRead(entity: ReadChapterEntity) { readMarks++ }
        override suspend fun markChaptersRead(entities: List<ReadChapterEntity>) { readMarks++ }

        override fun getAllFlow(): Flow<List<HistoryEntity>> = TODO()
        override suspend fun getAll(): List<HistoryEntity> = TODO()
        override suspend fun getByNovelUrl(novelUrl: String): HistoryEntity? = TODO()
        override suspend fun deleteByNovelUrl(novelUrl: String) = TODO()
        override suspend fun deleteAll() = TODO()
        override suspend fun getReadChapterUrls(novelUrl: String): List<String> = TODO()
        override fun getReadChapterUrlsFlow(novelUrl: String): Flow<List<String>> = TODO()
        override suspend fun markChapterUnread(novelUrl: String, chapterUrl: String, unreadAt: Long) = TODO()
        override suspend fun markChaptersUnread(novelUrl: String, chapterUrls: List<String>, unreadAt: Long) = TODO()
        override suspend fun clearReadChapters(novelUrl: String) = TODO()
        override suspend fun clearAllReadChapters() = TODO()
        override suspend fun getReadCountForNovel(novelUrl: String): Int = TODO()
        override suspend fun getAllReadChapters(): List<ReadChapterEntity> = TODO()
        override suspend fun getReadChapterCount(novelUrl: String): Int = TODO()
        override suspend fun updateCustomCover(novelUrl: String, coverUrl: String?) = TODO()
    }

    private fun item(n: Int, ts: Long) = HistoryItem(
        novel = Novel(name = "Novel $n", url = "https://x/n$n", posterUrl = "https://x/p$n.jpg", apiName = "Src$n"),
        chapterName = "Chapter $n",
        chapterUrl = "https://x/n$n/c$n",
        timestamp = ts,
    )

    @Test
    fun restoreKeepsOriginalTimestamp() = runTest {
        val dao = FakeHistoryDao()
        HistoryRepository(dao).restore(listOf(item(1, 1_000L), item(2, 2_000L)))

        assertEquals(listOf(1_000L, 2_000L), dao.inserted.map { it.timestamp })
        assertEquals(listOf("Src1", "Src2"), dao.inserted.map { it.apiName })
        assertEquals("https://x/n1/c1", dao.inserted[0].chapterUrl)
        assertEquals("Novel 2", dao.inserted[1].novelName)
        assertEquals("https://x/p2.jpg", dao.inserted[1].posterUrl)
        assertTrue("restore must not touch read chapters", dao.readMarks == 0)
    }
}
