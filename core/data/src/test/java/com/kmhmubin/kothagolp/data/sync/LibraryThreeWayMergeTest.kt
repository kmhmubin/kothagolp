package com.kmhmubin.kothagolp.data.sync

import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.data.backup.mergeForSyncForTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The 3-way merge for library rows.
 *
 * `syncedVersion` is the baseline — the version a row had when it last agreed
 * with Drive. Its purpose is to tell a real local edit apart from a row that
 * simply already agreed, so that only genuine concurrent edits are treated as
 * conflicts.
 */
class LibraryThreeWayMergeTest {

    private fun row(
        url: String = "https://src.com/novel/x",
        lastChapterUrl: String? = null,
        lastReadChapterIndex: Int = -1,
        lastReadAt: Long? = null,
        lastUpdatedAt: Long = 0,
        readingStatus: String = "READING",
        deletedAt: Long? = null,
        version: Long = 0,
        syncedVersion: Long = 0
    ) = LibraryEntity(
        url = url,
        name = "Book",
        apiName = "SourceA",
        addedAt = 1_000,
        readingStatus = readingStatus,
        lastChapterUrl = lastChapterUrl,
        lastReadAt = lastReadAt,
        lastReadChapterIndex = lastReadChapterIndex,
        lastUpdatedAt = lastUpdatedAt,
        deletedAt = deletedAt,
        version = version,
        syncedVersion = syncedVersion
    )

    @Test
    fun `only local changed - local wins without consulting timestamps`() {
        // Baseline 5: local moved to 6, remote is still at the agreed 5.
        val local = row(
            lastChapterUrl = "ch-204", lastReadChapterIndex = 203,
            lastReadAt = 1_000, version = 6, syncedVersion = 5
        )
        // Remote carries a *newer wall-clock* but no actual change since the
        // baseline. A pure timestamp merge would wrongly prefer it.
        val remote = row(
            lastChapterUrl = "ch-1", lastReadChapterIndex = 0,
            lastReadAt = 9_999, version = 5
        )

        val merged = local.mergeForSyncForTest(remote)

        assertEquals("ch-204", merged.lastChapterUrl)
        assertEquals(203, merged.lastReadChapterIndex)
    }

    @Test
    fun `only remote changed - remote wins`() {
        val local = row(lastChapterUrl = "ch-1", lastReadChapterIndex = 0, version = 5, syncedVersion = 5)
        val remote = row(lastChapterUrl = "ch-30", lastReadChapterIndex = 29, version = 9)

        val merged = local.mergeForSyncForTest(remote)

        assertEquals("ch-30", merged.lastChapterUrl)
        assertEquals(29, merged.lastReadChapterIndex)
    }

    @Test
    fun `sync bookkeeping is never taken from the remote device`() {
        // syncedVersion is per-device local state; adopting the remote's would
        // corrupt this device's notion of what it has already agreed to.
        val local = row(version = 5, syncedVersion = 5)
        val remote = row(version = 9, syncedVersion = 99)

        val merged = local.mergeForSyncForTest(remote)

        assertEquals(5, merged.syncedVersion)
    }

    @Test
    fun `both changed - reading position still resolved by recency not version`() {
        // The trap: a device that merely edited more often must not beat the
        // device that actually read further.
        val local = row(
            lastChapterUrl = "ch-204", lastReadChapterIndex = 203,
            lastReadAt = 9_000,          // read most recently
            version = 6, syncedVersion = 5
        )
        val remote = row(
            lastChapterUrl = "ch-201", lastReadChapterIndex = 200,
            lastReadAt = 1_000,
            version = 50                 // many more edits, but stale progress
        )

        val merged = local.mergeForSyncForTest(remote)

        assertEquals("ch-204", merged.lastChapterUrl)
        assertEquals(203, merged.lastReadChapterIndex)
    }

    @Test
    fun `both changed - remote wins the position when it read last`() {
        val local = row(
            lastChapterUrl = "ch-10", lastReadChapterIndex = 9,
            lastReadAt = 1_000, version = 6, syncedVersion = 5
        )
        val remote = row(
            lastChapterUrl = "ch-30", lastReadChapterIndex = 29,
            lastReadAt = 9_000, version = 7
        )

        val merged = local.mergeForSyncForTest(remote)

        assertEquals("ch-30", merged.lastChapterUrl)
        assertEquals(29, merged.lastReadChapterIndex)
    }

    @Test
    fun `legacy rows with no version information still reconcile by recency`() {
        // Everything at version 0 (upgraded install): must not freeze.
        val local = row(lastChapterUrl = "ch-5", lastReadChapterIndex = 4, lastReadAt = 1_000)
        val remote = row(lastChapterUrl = "ch-9", lastReadChapterIndex = 8, lastReadAt = 5_000)

        val merged = local.mergeForSyncForTest(remote)

        assertEquals("ch-9", merged.lastChapterUrl)
    }

    @Test
    fun `deletion made locally survives an unchanged remote`() {
        val local = row(deletedAt = 5_000, version = 6, syncedVersion = 5)
        val remote = row(version = 5)

        assertEquals(5_000L, local.mergeForSyncForTest(remote).deletedAt)
    }

    @Test
    fun `re-add outranks a synced tombstone by version`() {
        // Local re-added the book, bumping version past the synced tombstone.
        // The re-add has no fresh activity timestamp, so only the counter can
        // establish it as the newer intent.
        val local = row(deletedAt = null, lastUpdatedAt = 0, version = 3, syncedVersion = 1)
        val remote = row(deletedAt = 5_000, lastUpdatedAt = 5_000, version = 2)

        assertNull(local.mergeForSyncForTest(remote).deletedAt)
    }

    @Test
    fun `reading elsewhere after a local delete revives the book`() {
        // Both sides moved: genuine conflict, resolved by recency.
        val local = row(deletedAt = 2_000, lastUpdatedAt = 2_000, version = 6, syncedVersion = 5)
        val remote = row(
            lastChapterUrl = "ch-3", lastReadChapterIndex = 2,
            lastReadAt = 9_000, version = 7
        )

        val merged = local.mergeForSyncForTest(remote)

        assertNull(merged.deletedAt)
        assertEquals("ch-3", merged.lastChapterUrl)
    }
}
