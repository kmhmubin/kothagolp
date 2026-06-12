package com.kmhmubin.kothagolp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.dao.HistoryDao
import com.kmhmubin.kothagolp.data.local.entity.HistoryEntity
import com.kmhmubin.kothagolp.data.local.entity.ReadChapterEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {

    private lateinit var db: NovelDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NovelDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.historyDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun makeHistory(
        novelUrl: String = "https://test.com/novel",
        chapterUrl: String = "https://test.com/chapter/1"
    ) = HistoryEntity(
        novelUrl = novelUrl,
        novelName = "Test Novel",
        chapterName = "Chapter 1",
        chapterUrl = chapterUrl,
        apiName = "TestProvider"
    )

    @Test
    fun insert_andGetByNovelUrl_returnsEntry() = runTest {
        val entry = makeHistory()
        dao.insert(entry)
        val found = dao.getByNovelUrl(entry.novelUrl)
        assertNotNull(found)
        assertEquals(entry.novelUrl, found!!.novelUrl)
    }

    @Test
    fun getByNovelUrl_missingUrl_returnsNull() = runTest {
        assertNull(dao.getByNovelUrl("https://no.such.url"))
    }

    @Test
    fun getAll_returnsAllInserted() = runTest {
        dao.insert(makeHistory("https://a.test", "https://a.test/ch1"))
        dao.insert(makeHistory("https://b.test", "https://b.test/ch1"))
        val all = dao.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteByNovelUrl_removesEntry() = runTest {
        val url = "https://delete.test"
        dao.insert(makeHistory(url))
        assertNotNull(dao.getByNovelUrl(url))
        dao.deleteByNovelUrl(url)
        assertNull(dao.getByNovelUrl(url))
    }

    @Test
    fun deleteAll_emptiesTable() = runTest {
        dao.insert(makeHistory("https://x.test", "https://x.test/ch1"))
        dao.insert(makeHistory("https://y.test", "https://y.test/ch1"))
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun getAllFlow_emitsUpdates() = runTest {
        val flow = dao.getAllFlow()
        assertTrue(flow.first().isEmpty())
        dao.insert(makeHistory())
        assertEquals(1, flow.first().size)
    }

    @Test
    fun markChapterRead_andGetReadUrls() = runTest {
        val novelUrl = "https://novel.test"
        dao.markChapterRead(ReadChapterEntity("https://ch1.test", novelUrl))
        dao.markChapterRead(ReadChapterEntity("https://ch2.test", novelUrl))
        val urls = dao.getReadChapterUrls(novelUrl)
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://ch1.test"))
        assertTrue(urls.contains("https://ch2.test"))
    }

    @Test
    fun markChapterUnread_removesEntry() = runTest {
        val novelUrl = "https://novel.test"
        val chapterUrl = "https://ch1.test"
        dao.markChapterRead(ReadChapterEntity(chapterUrl, novelUrl))
        assertEquals(1, dao.getReadChapterUrls(novelUrl).size)
        dao.markChapterUnread(novelUrl, chapterUrl)
        assertTrue(dao.getReadChapterUrls(novelUrl).isEmpty())
    }

    @Test
    fun getReadChapterCount_returnsCorrectCount() = runTest {
        val novelUrl = "https://count.test"
        dao.markChaptersRead(listOf(
            ReadChapterEntity("https://ch1.test", novelUrl),
            ReadChapterEntity("https://ch2.test", novelUrl),
            ReadChapterEntity("https://ch3.test", novelUrl)
        ))
        assertEquals(3, dao.getReadChapterCount(novelUrl))
    }

    @Test
    fun clearReadChapters_removesAllForNovel() = runTest {
        val novelUrl = "https://clear.test"
        dao.markChaptersRead(listOf(
            ReadChapterEntity("https://ch1.test", novelUrl),
            ReadChapterEntity("https://ch2.test", novelUrl)
        ))
        dao.clearReadChapters(novelUrl)
        assertEquals(0, dao.getReadChapterCount(novelUrl))
    }
}
