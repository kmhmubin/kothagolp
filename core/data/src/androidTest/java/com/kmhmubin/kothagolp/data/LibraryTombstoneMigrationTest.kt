package com.kmhmubin.kothagolp.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the 14 -> 15 upgrade (soft-delete tombstone column) against a real
 * on-device database.
 *
 * Room's MigrationTestHelper needs exported schema JSON, which this project does
 * not generate, so the pre-upgrade database is built by hand at the v14 shape and
 * then opened through the real Room builder to drive the actual migration. The
 * risk being tested is upgrade data loss: the builder has
 * fallbackToDestructiveMigration(), so a broken migration silently wipes the
 * user's library rather than failing loudly.
 */
@RunWith(AndroidJUnit4::class)
class LibraryTombstoneMigrationTest {

    private val dbName = "migration_test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = context.deleteDatabase(dbName).let {}

    @After
    fun tearDown() = context.deleteDatabase(dbName).let {}

    /** Creates a v14 `library` table (pre-tombstone) with one book in it. */
    private fun createV14Database() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `library` (
                            `url` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `posterUrl` TEXT,
                            `apiName` TEXT NOT NULL,
                            `latestChapter` TEXT,
                            `addedAt` INTEGER NOT NULL,
                            `readingStatus` TEXT NOT NULL,
                            `lastChapterUrl` TEXT,
                            `lastChapterName` TEXT,
                            `lastReadAt` INTEGER,
                            `lastScrollIndex` INTEGER NOT NULL,
                            `lastScrollOffset` INTEGER NOT NULL,
                            `totalChapterCount` INTEGER NOT NULL,
                            `acknowledgedChapterCount` INTEGER NOT NULL,
                            `lastCheckedAt` INTEGER NOT NULL,
                            `lastUpdatedAt` INTEGER NOT NULL,
                            `lastReadChapterIndex` INTEGER NOT NULL,
                            `unreadChapterCount` INTEGER NOT NULL,
                            `customCoverUrl` TEXT,
                            PRIMARY KEY(`url`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO library (
                            url, name, posterUrl, apiName, latestChapter, addedAt,
                            readingStatus, lastChapterUrl, lastChapterName, lastReadAt,
                            lastScrollIndex, lastScrollOffset, totalChapterCount,
                            acknowledgedChapterCount, lastCheckedAt, lastUpdatedAt,
                            lastReadChapterIndex, unreadChapterCount, customCoverUrl
                        ) VALUES (
                            'https://src.com/novel/pre-existing', 'Pre-existing Book', NULL,
                            'SourceA', 'Chapter 204', 1000, 'READING',
                            'https://src.com/novel/pre-existing/chapter-204', 'Chapter 204',
                            2000, 7, 0, 300, 300, 1500, 1500, 203, 96, NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()
    }

    private fun openMigratedDatabase(): NovelDatabase =
        Room.databaseBuilder(context, NovelDatabase::class.java, dbName)
            .addMigrations(NovelDatabase.MIGRATION_14_15)
            .build()

    @Test
    fun upgradeKeepsExistingLibraryAndAddsTombstoneColumn() = runBlocking {
        createV14Database()

        val db = openMigratedDatabase()
        try {
            val books = db.libraryDao().getAll()

            // The pre-upgrade book must survive — a destructive fallback here
            // would mean every user losing their library on update.
            assertEquals(1, books.size)
            val book = books.single()
            assertEquals("Pre-existing Book", book.name)
            assertEquals(203, book.lastReadChapterIndex)
            assertEquals("https://src.com/novel/pre-existing/chapter-204", book.lastChapterUrl)

            // New column defaults to "not deleted".
            assertNull(book.deletedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun softDeleteHidesBookFromLibraryButKeepsItForSync() = runBlocking {
        createV14Database()

        val db = openMigratedDatabase()
        try {
            val dao = db.libraryDao()
            val url = "https://src.com/novel/pre-existing"

            dao.softDelete(url, deletedAt = 9_000)

            // Gone from every library-facing query...
            assertTrue(dao.getAll().isEmpty())
            assertNull(dao.getByUrl(url))
            assertEquals(false, dao.exists(url))

            // ...but still present for sync, carrying the tombstone. This is what
            // stops the other device's stale copy from resurrecting it.
            val forSync = dao.getAllForSync()
            assertEquals(1, forSync.size)
            assertEquals(9_000L, forSync.single().deletedAt)
            assertNotNull(dao.getByUrlIncludingDeleted(url))
        } finally {
            db.close()
        }
    }

    @Test
    fun reAddingClearsTombstone() = runBlocking {
        createV14Database()

        val db = openMigratedDatabase()
        try {
            val dao = db.libraryDao()
            val url = "https://src.com/novel/pre-existing"

            dao.softDelete(url, deletedAt = 9_000)
            dao.clearTombstone(url)

            assertEquals(1, dao.getAll().size)
            assertNull(dao.getByUrl(url)?.deletedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun purgeRemovesOnlyOldTombstones() = runBlocking {
        createV14Database()

        val db = openMigratedDatabase()
        try {
            val dao = db.libraryDao()
            val url = "https://src.com/novel/pre-existing"

            dao.softDelete(url, deletedAt = 1_000)
            dao.purgeOldTombstones(threshold = 500)
            assertNotNull("recent tombstone must be kept", dao.getByUrlIncludingDeleted(url))

            dao.purgeOldTombstones(threshold = 5_000)
            assertNull("old tombstone should be purged", dao.getByUrlIncludingDeleted(url))
        } finally {
            db.close()
        }
    }
}
