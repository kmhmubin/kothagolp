package com.kmhmubin.kothagolp.data.sync

import com.kmhmubin.kothagolp.data.backup.AppSettingsBackup
import com.kmhmubin.kothagolp.data.backup.BackupData
import com.kmhmubin.kothagolp.data.backup.ReaderSettingsBackup
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Conflict resolution for the last-write-wins sections of a sync payload.
 *
 * These guard the ordering itself. The subtle failure they exist for: restoring
 * settings used to stamp them as authored "now", so the device that synced most
 * recently always looked like the newest author and its stale values reverted
 * genuine edits made on another device.
 */
class SyncSettingsConflictTest {

    private fun appSettings(updatedAt: Long, theme: String) =
        AppSettingsBackup(updatedAt = updatedAt, themeMode = theme)

    private fun readerSettings(updatedAt: Long, fontSize: Int) =
        ReaderSettingsBackup(updatedAt = updatedAt, fontSize = fontSize)

    private fun merge(local: BackupData, remote: BackupData) =
        SyncBackupMerger.merge(local, remote)

    @Test
    fun `newer local settings are not replaced by older remote`() {
        val local = BackupData(appSettings = appSettings(updatedAt = 9_000, theme = "DARK"))
        val remote = BackupData(appSettings = appSettings(updatedAt = 1_000, theme = "LIGHT"))

        val merged = merge(local, remote)

        assertEquals("DARK", merged.appSettings?.themeMode)
        assertEquals(9_000L, merged.appSettings?.updatedAt)
    }

    @Test
    fun `newer remote settings win over older local`() {
        val local = BackupData(appSettings = appSettings(updatedAt = 1_000, theme = "DARK"))
        val remote = BackupData(appSettings = appSettings(updatedAt = 9_000, theme = "LIGHT"))

        val merged = merge(local, remote)

        assertEquals("LIGHT", merged.appSettings?.themeMode)
    }

    @Test
    fun `merged settings keep their authoring timestamp`() {
        // The merge must not invent a fresh timestamp: the restored value carries
        // this one back into prefs, and inflating it would make this device beat
        // every other device's later edits.
        val local = BackupData(appSettings = appSettings(updatedAt = 1_000, theme = "DARK"))
        val remote = BackupData(appSettings = appSettings(updatedAt = 9_000, theme = "LIGHT"))

        val merged = merge(local, remote)

        assertEquals(9_000L, merged.appSettings?.updatedAt)
    }

    @Test
    fun `reader settings follow the same ordering`() {
        val local = BackupData(readerSettings = readerSettings(updatedAt = 5_000, fontSize = 22))
        val remote = BackupData(readerSettings = readerSettings(updatedAt = 1_000, fontSize = 14))

        val merged = merge(local, remote)

        assertEquals(22, merged.readerSettings?.fontSize)
        assertEquals(5_000L, merged.readerSettings?.updatedAt)
    }

    @Test
    fun `settings absent on one side are taken from the other`() {
        val local = BackupData(appSettings = null)
        val remote = BackupData(appSettings = appSettings(updatedAt = 1_000, theme = "LIGHT"))

        assertEquals("LIGHT", merge(local, remote).appSettings?.themeMode)
        assertEquals("LIGHT", merge(remote, local).appSettings?.themeMode)
    }
}
