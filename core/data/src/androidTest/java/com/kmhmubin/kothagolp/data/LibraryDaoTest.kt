package com.kmhmubin.kothagolp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.dao.LibraryDao
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {

    private lateinit var db: NovelDatabase
    private lateinit var dao: LibraryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NovelDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.libraryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun makeEntry(url: String = "https://test.com/novel") = LibraryEntity(
        url = url,
        name = "Test Novel",
        apiName = "TestProvider",
        addedAt = System.currentTimeMillis()
    )

    @Test
    fun insert_andGetByUrl_returnsEntry() = runTest {
        val entry = makeEntry()
        dao.insert(entry)
        val found = dao.getByUrl(entry.url)
        assertNotNull(found)
        assertEquals(entry.url, found!!.url)
        assertEquals(entry.name, found.name)
    }

    @Test
    fun exists_missingUrl_returnsFalse() = runTest {
        assertFalse(dao.exists("https://no.such.url"))
    }

    @Test
    fun exists_afterInsert_returnsTrue() = runTest {
        dao.insert(makeEntry("https://exists.test"))
        assertTrue(dao.exists("https://exists.test"))
    }

    @Test
    fun softDelete_hidesEntryFromLibrary() = runTest {
        val url = "https://delete.test"
        dao.insert(makeEntry(url))
        assertTrue(dao.exists(url))
        dao.softDelete(url)
        // Hidden from the library, but retained as a tombstone so the deletion
        // can propagate through sync instead of being undone by a stale copy.
        assertFalse(dao.exists(url))
        assertEquals(1, dao.getAllForSync().size)
    }

    @Test
    fun clearTombstone_restoresEntryToLibrary() = runTest {
        val url = "https://readd.test"
        dao.insert(makeEntry(url))
        dao.softDelete(url)
        assertFalse(dao.exists(url))

        // Re-adding a previously removed book must undo the tombstone,
        // otherwise the row stays invisible forever.
        dao.clearTombstone(url)
        assertTrue(dao.exists(url))
        assertNull(dao.getByUrl(url)!!.deletedAt)
    }

    @Test
    fun purgeOldTombstones_removesOnlyExpiredOnes() = runTest {
        val url = "https://purge.test"
        dao.insert(makeEntry(url))
        dao.softDelete(url, deletedAt = 1_000)

        // A tombstone still inside the retention window must survive: it is the
        // only thing stopping another device from resurrecting the book.
        dao.purgeOldTombstones(threshold = 500)
        assertNotNull(dao.getByUrlIncludingDeleted(url))

        dao.purgeOldTombstones(threshold = 5_000)
        assertNull(dao.getByUrlIncludingDeleted(url))
    }

    @Test
    fun localEdits_bumpVersion_butSyncBookkeepingDoesNot() = runTest {
        val url = "https://version.test"
        dao.insert(makeEntry(url))
        val start = dao.getByUrl(url)!!.version

        dao.updateStatus(url, ReadingStatus.COMPLETED.name, System.currentTimeMillis())
        val afterEdit = dao.getByUrl(url)!!.version
        assertTrue("a local edit must bump version", afterEdit > start)

        // Recording sync agreement is bookkeeping, not a user edit. Bumping the
        // counter here would make every synced row look locally changed and
        // collapse the 3-way merge back into guesswork.
        dao.markSynced(url, afterEdit)
        val row = dao.getByUrl(url)!!
        assertEquals(afterEdit, row.version)
        assertEquals(afterEdit, row.syncedVersion)
    }

    @Test
    fun getLocallyChanged_reportsOnlyRowsEditedSinceLastSync() = runTest {
        val synced = "https://synced.test"
        val edited = "https://edited.test"
        dao.insert(makeEntry(synced))
        dao.insert(makeEntry(edited))

        // Bring both to an agreed baseline.
        dao.markSynced(synced, dao.getByUrl(synced)!!.version)
        dao.markSynced(edited, dao.getByUrl(edited)!!.version)
        assertTrue(dao.getLocallyChanged().isEmpty())

        dao.updateStatus(edited, ReadingStatus.COMPLETED.name, System.currentTimeMillis())

        val changed = dao.getLocallyChanged().map { it.url }
        assertEquals(listOf(edited), changed)
    }

    @Test
    fun getAll_returnsAllInserted() = runTest {
        dao.insert(makeEntry("https://a.test"))
        dao.insert(makeEntry("https://b.test"))
        dao.insert(makeEntry("https://c.test"))
        val all = dao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun updateStatus_changesStatus() = runTest {
        val url = "https://status.test"
        dao.insert(makeEntry(url))
        dao.updateStatus(url, ReadingStatus.COMPLETED.name, System.currentTimeMillis())
        val found = dao.getByUrl(url)
        assertEquals(ReadingStatus.COMPLETED.name, found!!.readingStatus)
    }

    @Test
    fun getAllFlow_emitsOnInsert() = runTest {
        val flow = dao.getAllFlow()
        assertTrue(flow.first().isEmpty())
        dao.insert(makeEntry())
        assertEquals(1, flow.first().size)
    }

    @Test
    fun existsFlow_updatesOnChange() = runTest {
        val url = "https://flow.test"
        val flow = dao.existsFlow(url)
        assertFalse(flow.first())
        dao.insert(makeEntry(url))
        assertTrue(flow.first())
    }

    @Test
    fun deleteAll_emptiesTable() = runTest {
        dao.insert(makeEntry("https://x.test"))
        dao.insert(makeEntry("https://y.test"))
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun updateChapterCount_updatesCorrectly() = runTest {
        val url = "https://chapters.test"
        dao.insert(makeEntry(url))
        dao.updateChapterCount(url, 50, "Chapter 50", System.currentTimeMillis())
        val found = dao.getByUrl(url)
        assertEquals(50, found!!.totalChapterCount)
        assertEquals("Chapter 50", found.latestChapter)
    }

    @Test
    fun newChapterCount_calculatesCorrectly() = runTest {
        val url = "https://newchapters.test"
        dao.insert(makeEntry(url))
        dao.updateChapterCount(url, 10, "Ch 10", System.currentTimeMillis())
        val found = dao.getByUrl(url)!!
        assertEquals(10, found.newChapterCount)
        assertTrue(found.hasNewChapters)
    }

    @Test
    fun acknowledgeNewChapters_clearsNewCount() = runTest {
        val url = "https://ack.test"
        dao.insert(makeEntry(url))
        dao.updateChapterCount(url, 10, "Ch 10", System.currentTimeMillis())
        dao.acknowledgeNewChapters(url)
        val found = dao.getByUrl(url)!!
        assertEquals(0, found.newChapterCount)
        assertFalse(found.hasNewChapters)
    }

    @Test
    fun search_findsMatchingName() = runTest {
        dao.insert(LibraryEntity(url = "https://magic.test", name = "Magic Academy", apiName = "TestProvider"))
        dao.insert(LibraryEntity(url = "https://dragon.test", name = "Dragon Rider", apiName = "TestProvider"))
        val results = dao.search("Magic")
        assertEquals(1, results.size)
        assertEquals("https://magic.test", results.first().url)
    }

    @Test
    fun customCover_updatesAndRetrieves() = runTest {
        val url = "https://cover.test"
        dao.insert(makeEntry(url))
        dao.updateCustomCover(url, "https://custom.cover/image.jpg")
        val cover = dao.getCustomCover(url)
        assertEquals("https://custom.cover/image.jpg", cover)
    }
}
