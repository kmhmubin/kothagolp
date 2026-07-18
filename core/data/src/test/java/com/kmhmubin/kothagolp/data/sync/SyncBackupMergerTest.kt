package com.kmhmubin.kothagolp.data.sync

import com.kmhmubin.kothagolp.data.backup.BackupData
import com.kmhmubin.kothagolp.data.backup.LibraryBackup
import com.kmhmubin.kothagolp.data.backup.ReadChapterBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the two reported sync bugs:
 *  1. reading position reset to an old chapter after sync
 *  2. deleted / migrated books reappearing after sync
 */
class SyncBackupMergerTest {

    private fun book(
        url: String = "https://src-a.com/novel/x",
        apiName: String = "SourceA",
        name: String = "Same Title",
        lastChapterUrl: String? = null,
        lastReadChapterIndex: Int = -1,
        lastReadAt: Long? = null,
        lastScrollIndex: Int = 0,
        lastUpdatedAt: Long = 0,
        addedAt: Long = 1_000,
        deletedAt: Long? = null,
        version: Long = 0
    ) = LibraryBackup(
        url = url,
        name = name,
        apiName = apiName,
        addedAt = addedAt,
        readingStatus = "READING",
        lastChapterUrl = lastChapterUrl,
        lastChapterName = lastChapterUrl?.substringAfterLast('/'),
        lastReadAt = lastReadAt,
        lastScrollIndex = lastScrollIndex,
        lastReadChapterIndex = lastReadChapterIndex,
        lastUpdatedAt = lastUpdatedAt,
        deletedAt = deletedAt,
        version = version
    )

    private fun merge(local: List<LibraryBackup>, remote: List<LibraryBackup>): List<LibraryBackup> =
        SyncBackupMerger.merge(
            BackupData(library = local, appVersion = "test", deviceInfo = "test"),
            BackupData(library = remote, appVersion = "test", deviceInfo = "test")
        ).library

    private fun mergeRead(
        local: List<ReadChapterBackup>,
        remote: List<ReadChapterBackup>
    ): List<ReadChapterBackup> =
        SyncBackupMerger.merge(
            BackupData(readChapters = local, appVersion = "test", deviceInfo = "test"),
            BackupData(readChapters = remote, appVersion = "test", deviceInfo = "test")
        ).readChapters

    private fun read(url: String = "ch-1", readAt: Long = 0, unreadAt: Long? = null) =
        ReadChapterBackup(chapterUrl = url, novelUrl = "novel", readAt = readAt, unreadAt = unreadAt)

    // ------------------------------------------------- bug: unread not syncing

    @Test
    fun `unread after read wins when it happened later`() {
        // Both devices read the chapter; one then marked it unread.
        val readCopy = read(readAt = 1_000)
        val unreadCopy = read(readAt = 1_000, unreadAt = 2_000)

        assertNotNull(mergeRead(listOf(readCopy), listOf(unreadCopy)).single().unreadAt)
        assertNotNull(mergeRead(listOf(unreadCopy), listOf(readCopy)).single().unreadAt)
    }

    @Test
    fun `re-reading after an unread clears the tombstone`() {
        // Marked unread at 2000, then read again at 3000 (which clears unreadAt).
        val unreadCopy = read(readAt = 1_000, unreadAt = 2_000)
        val rereadCopy = read(readAt = 3_000, unreadAt = null)

        assertNull(mergeRead(listOf(unreadCopy), listOf(rereadCopy)).single().unreadAt)
        assertNull(mergeRead(listOf(rereadCopy), listOf(unreadCopy)).single().unreadAt)
    }

    // ---------------------------------------------------------------- bug 1

    @Test
    fun `local progress past remote wins and stays coherent`() {
        // Remote is parked on chapter 201; this device read on to 204 offline.
        val remote = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-201",
            lastReadChapterIndex = 200,
            lastReadAt = 1_000
        )
        val local = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-204",
            lastReadChapterIndex = 203,
            lastScrollIndex = 7,
            lastReadAt = 2_000
        )

        val merged = merge(listOf(local), listOf(remote)).single()

        assertEquals("https://src-a.com/novel/x/chapter-204", merged.lastChapterUrl)
        assertEquals(203, merged.lastReadChapterIndex)
        assertEquals(2_000L, merged.lastReadAt)
        assertEquals(7, merged.lastScrollIndex)
    }

    @Test
    fun `position fields are never mixed across devices`() {
        // The old merge took the URL from the newest reader but maxOf() for the
        // index, producing a position that existed on neither device.
        val remote = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-500",
            lastReadChapterIndex = 499,
            lastScrollIndex = 3,
            lastReadAt = 1_000
        )
        val local = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-204",
            lastReadChapterIndex = 203,
            lastScrollIndex = 9,
            lastReadAt = 5_000
        )

        val merged = merge(listOf(local), listOf(remote)).single()

        // Whole position comes from the newest reader — not max of each field.
        assertEquals("https://src-a.com/novel/x/chapter-204", merged.lastChapterUrl)
        assertEquals(203, merged.lastReadChapterIndex)
        assertEquals(9, merged.lastScrollIndex)
    }

    @Test
    fun `remote progress wins when it read last`() {
        val local = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-10",
            lastReadChapterIndex = 9,
            lastReadAt = 1_000
        )
        val remote = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-30",
            lastReadChapterIndex = 29,
            lastReadAt = 9_000
        )

        val merged = merge(listOf(local), listOf(remote)).single()

        assertEquals("https://src-a.com/novel/x/chapter-30", merged.lastChapterUrl)
        assertEquals(29, merged.lastReadChapterIndex)
    }

    // ---------------------------------------------------------------- bug 2

    @Test
    fun `deleted book does not come back from a stale remote copy`() {
        val remote = book(lastReadAt = 1_000, lastUpdatedAt = 1_000)
        val local = book(lastReadAt = 1_000, lastUpdatedAt = 1_000, deletedAt = 5_000)

        val merged = merge(listOf(local), listOf(remote)).single()

        assertNotNull("tombstone must survive the merge", merged.deletedAt)
        assertEquals(5_000L, merged.deletedAt)
    }

    @Test
    fun `deletion propagates from remote to local`() {
        val local = book(lastReadAt = 1_000, lastUpdatedAt = 1_000)
        val remote = book(lastReadAt = 1_000, lastUpdatedAt = 1_000, deletedAt = 5_000)

        val merged = merge(listOf(local), listOf(remote)).single()

        assertNotNull(merged.deletedAt)
    }

    @Test
    fun `reading a book after deleting it elsewhere revives it`() {
        // Deleted on device A at t=2000, but device B kept reading at t=9000.
        val remote = book(deletedAt = 2_000, lastUpdatedAt = 2_000)
        val local = book(
            lastChapterUrl = "https://src-a.com/novel/x/chapter-3",
            lastReadChapterIndex = 2,
            lastReadAt = 9_000
        )

        val merged = merge(listOf(local), listOf(remote)).single()

        assertNull("newer reading activity beats the older deletion", merged.deletedAt)
        assertEquals("https://src-a.com/novel/x/chapter-3", merged.lastChapterUrl)
    }

    @Test
    fun `migrated book keeps only the new source`() {
        // Migration tombstones the old-source row; the remote snapshot predates
        // the migration and still lists it as live.
        val oldSourceRemote = book(url = "https://src-a.com/novel/x", apiName = "SourceA", lastUpdatedAt = 1_000)
        val oldSourceLocal = oldSourceRemote.copy(deletedAt = 5_000)
        val newSource = book(url = "https://src-c.com/novel/x", apiName = "SourceC", lastReadAt = 5_000)

        val merged = merge(listOf(oldSourceLocal, newSource), listOf(oldSourceRemote))

        val live = merged.filter { it.deletedAt == null }
        assertEquals(1, live.size)
        assertEquals("SourceC", live.single().apiName)
    }

    @Test
    fun `same title from different sources is not silently merged away`() {
        // Two genuinely different books that happen to share a title must both
        // survive — the old title-based dedup dropped one of them.
        val a = book(url = "https://src-a.com/novel/x", apiName = "SourceA", name = "Shadow Slave")
        val b = book(url = "https://src-c.com/novel/y", apiName = "SourceC", name = "Shadow Slave")

        val merged = merge(listOf(a, b), emptyList())

        assertEquals(2, merged.size)
    }

    @Test
    fun `re-adding a book after syncing its deletion keeps it live`() {
        // Caught on two real devices: delete on A -> sync (tombstone reaches
        // Drive) -> re-add on A -> sync, and the book came back deleted.
        // A re-add carries no fresh read/update timestamp, so recency alone
        // hands the win to the deletion. The bumped version is the only signal
        // that the re-add is the newer intent.
        val tombstone = book(deletedAt = 5_000, lastUpdatedAt = 5_000, version = 1)
        val readded = book(deletedAt = null, lastUpdatedAt = 0, lastReadAt = null, version = 2)

        // Order must not matter.
        assertNull(merge(listOf(readded), listOf(tombstone)).single().deletedAt)
        assertNull(merge(listOf(tombstone), listOf(readded)).single().deletedAt)
    }

    @Test
    fun `higher version deletion still wins over a stale live copy`() {
        // The mirror case: the delete is the newer edit and must stick.
        val deleted = book(deletedAt = 5_000, lastUpdatedAt = 5_000, version = 3)
        val stale = book(deletedAt = null, lastUpdatedAt = 9_000, version = 2)

        assertNotNull(merge(listOf(stale), listOf(deleted)).single().deletedAt)
    }

    @Test
    fun `equal-version deletion falls back to recency`() {
        // Guards the fallback path for rows predating the version counter.
        val deleted = book(deletedAt = 9_000, lastUpdatedAt = 9_000, version = 1)
        val stale = book(deletedAt = null, lastUpdatedAt = 1_000, version = 1)

        assertNotNull(merge(listOf(stale), listOf(deleted)).single().deletedAt)
    }

    @Test
    fun `unseen remote book is added`() {
        val local = book(url = "https://src-a.com/novel/x")
        val remote = book(url = "https://src-b.com/novel/z", apiName = "SourceB")

        val merged = merge(listOf(local), listOf(remote))

        assertEquals(2, merged.size)
        assertTrue(merged.all { it.deletedAt == null })
    }
}
