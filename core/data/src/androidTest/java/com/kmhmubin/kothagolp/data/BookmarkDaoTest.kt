package com.kmhmubin.kothagolp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.dao.BookmarkDao
import com.kmhmubin.kothagolp.data.local.entity.BookmarkEntity
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
class BookmarkDaoTest {

    private lateinit var db: NovelDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NovelDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bookmarkDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun makeBookmark(
        novelUrl: String = "https://novel.test",
        chapterUrl: String = "https://chapter.test",
        category: String = "default",
        note: String? = null
    ) = BookmarkEntity(
        novelUrl = novelUrl,
        novelName = "Test Novel",
        chapterUrl = chapterUrl,
        chapterName = "Chapter 1",
        category = category,
        note = note
    )

    @Test
    fun insert_returnsId_andGetById_works() = runTest {
        val id = dao.insert(makeBookmark())
        assertTrue(id > 0)
        val found = dao.getById(id)
        assertNotNull(found)
        assertEquals(id, found!!.id)
    }

    @Test
    fun getById_missing_returnsNull() = runTest {
        assertNull(dao.getById(99999L))
    }

    @Test
    fun deleteById_removesEntry() = runTest {
        val id = dao.insert(makeBookmark())
        assertNotNull(dao.getById(id))
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun observeForNovel_reflectsChanges() = runTest {
        val novelUrl = "https://novel.test"
        val flow = dao.observeForNovel(novelUrl)
        assertTrue(flow.first().isEmpty())
        dao.insert(makeBookmark(novelUrl = novelUrl))
        assertEquals(1, flow.first().size)
    }

    @Test
    fun getForChapter_returnsMatchingBookmarks() = runTest {
        val chapterUrl = "https://chapter.test"
        dao.insert(makeBookmark(chapterUrl = chapterUrl))
        dao.insert(makeBookmark(chapterUrl = chapterUrl))
        dao.insert(makeBookmark(chapterUrl = "https://other.chapter"))
        val results = dao.getForChapter(chapterUrl)
        assertEquals(2, results.size)
    }

    @Test
    fun observeByCategory_filtersByCategory() = runTest {
        dao.insert(makeBookmark(category = "favorites"))
        dao.insert(makeBookmark(category = "favorites"))
        dao.insert(makeBookmark(category = "default"))
        val favorites = dao.observeByCategory("favorites").first()
        assertEquals(2, favorites.size)
    }

    @Test
    fun countForNovel_returnsCorrectCount() = runTest {
        val novelUrl = "https://count.novel"
        dao.insert(makeBookmark(novelUrl = novelUrl, chapterUrl = "https://ch1.test"))
        dao.insert(makeBookmark(novelUrl = novelUrl, chapterUrl = "https://ch2.test"))
        dao.insert(makeBookmark(novelUrl = "https://other.novel", chapterUrl = "https://ch3.test"))
        assertEquals(2, dao.countForNovel(novelUrl))
    }

    @Test
    fun search_findsByNote() = runTest {
        dao.insert(makeBookmark(note = "important scene here"))
        dao.insert(makeBookmark(note = "something else"))
        val results = dao.search("important")
        assertEquals(1, results.size)
    }

    @Test
    fun deleteAll_emptiesTable() = runTest {
        dao.insert(makeBookmark(chapterUrl = "https://ch1.test"))
        dao.insert(makeBookmark(chapterUrl = "https://ch2.test"))
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun observeCategories_returnsDistinctCategories() = runTest {
        dao.insert(makeBookmark(category = "alpha"))
        dao.insert(makeBookmark(category = "alpha"))
        dao.insert(makeBookmark(category = "beta"))
        val categories = dao.observeCategories().first()
        assertEquals(2, categories.size)
        assertTrue(categories.contains("alpha"))
        assertTrue(categories.contains("beta"))
    }
}
