package com.kmhmubin.kothagolp.data.sync

import android.content.Context
import com.kmhmubin.kothagolp.data.backup.BackupManager
import com.kmhmubin.kothagolp.data.backup.BackupSelection
import com.kmhmubin.kothagolp.data.backup.RestoreOptions
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.google.api.client.auth.oauth2.TokenResponseException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

/**
 * Creates local sync payloads, reconciles remote data, and applies merged results.
 */
class SyncManager(
    context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager.getInstance(context),
    private val database: NovelDatabase = NovelDatabase.getInstance(context.applicationContext),
    private val backupManager: BackupManager = BackupManager(
        context.applicationContext,
        NovelDatabase.getInstance(context.applicationContext),
        PreferencesManager.getInstance(context.applicationContext)
    ),
    private val googleDriveSyncService: GoogleDriveSyncService = GoogleDriveSyncService(context.applicationContext)
) {
    suspend fun sync(trigger: SyncTrigger): Result<String> {
        val settings = preferencesManager.getSyncSettings()
        if (settings.service == SyncServiceType.NONE) {
            return Result.failure(IllegalStateException("Sync is disabled."))
        }
        if (!settings.googleDriveSignedIn) {
            return Result.failure(IllegalStateException("Google Drive is not connected."))
        }

        val selection = preferencesManager.getSyncDataSelection()
        if (!selection.anyEnabled()) {
            return Result.failure(IllegalStateException("Select at least one sync category."))
        }

        return when (settings.service) {
            SyncServiceType.GOOGLE_DRIVE -> syncWithGoogleDrive(trigger, selection)
            SyncServiceType.NONE -> Result.failure(IllegalStateException("Sync is disabled."))
        }
    }

    private suspend fun syncWithGoogleDrive(
        trigger: SyncTrigger,
        selection: SyncDataSelection
    ): Result<String> {
        SyncStatusTracker.start(trigger, "Preparing local backup")

        return try {
            // pull -> merge -> push is not atomic on Drive. If another device
            // pushes between our pull and our push, a blind update drops its
            // changes (classic lost update). The push therefore guards on the
            // revision we merged from, and a conflict restarts the cycle with a
            // fresh pull so the other device's data is merged in rather than
            // overwritten.
            var mergedPayload: SyncPayload? = null
            var appliedRemote = false

            for (attempt in 1..MAX_SYNC_ATTEMPTS) {
                currentCoroutineContext().ensureActive()
                val localBackup = backupManager.createBackup(selection.toBackupSelection())
                val localPayload = SyncPayload(
                    syncedAt = System.currentTimeMillis(),
                    deviceId = preferencesManager.getUniqueDeviceId(),
                    backup = localBackup
                )

                SyncStatusTracker.update(
                    if (attempt == 1) "Checking Google Drive" else "Remote changed — remerging"
                )
                currentCoroutineContext().ensureActive()
                val snapshot = googleDriveSyncService.pullSnapshot()

                val payload = when (snapshot) {
                    null -> localPayload
                    else -> {
                        SyncStatusTracker.update("Merging remote changes")
                        currentCoroutineContext().ensureActive()
                        SyncPayload(
                            syncedAt = System.currentTimeMillis(),
                            deviceId = localPayload.deviceId,
                            backup = SyncBackupMerger.merge(localBackup, snapshot.payload.backup)
                        )
                    }
                }

                SyncStatusTracker.update("Uploading merged data")
                currentCoroutineContext().ensureActive()
                try {
                    googleDriveSyncService.pushSyncPayload(
                        payload = payload,
                        expected = snapshot,
                        requireExpectedVersion = true,
                        // pullSnapshot() merged every remote file into this
                        // payload, so consolidating duplicates is safe.
                        mergedAllRemoteFiles = true
                    )
                } catch (conflict: GoogleDriveSyncService.RemoteChangedException) {
                    if (attempt == MAX_SYNC_ATTEMPTS) throw conflict
                    continue
                }

                mergedPayload = payload
                appliedRemote = snapshot != null
                break
            }

            val finalPayload = mergedPayload ?: error("Sync did not produce a payload.")

            if (appliedRemote) {
                SyncStatusTracker.update("Applying merged data locally")
                currentCoroutineContext().ensureActive()
                val restoreResult = backupManager.restoreBackupData(
                    finalPayload.backup,
                    selection.toRestoreOptions()
                )

                if (!restoreResult.success) {
                    error(restoreResult.error ?: "Failed to restore merged sync data.")
                }
            }

            val syncedAt = System.currentTimeMillis()

            // Tombstones exist only to outlive a stale remote copy. Once a
            // deletion is this old every device has long since merged it, so
            // drop the rows rather than growing the table forever. A device
            // offline past this window can still resurrect the book — the same
            // bounded tradeoff every tombstone-based sync makes.
            runCatching {
                database.libraryDao().purgeOldTombstones(syncedAt - TOMBSTONE_RETENTION_MS)
            }

            preferencesManager.setLastSyncTime(syncedAt)
            val message = "Last synced ${selection.enabledCountLabel()} to Google Drive"
            SyncStatusTracker.finishSuccess(message)
            Result.success(message)
        } catch (error: Exception) {
            val safeMessage = error.toSyncMessage()
            SyncStatusTracker.finishError(safeMessage)
            Result.failure(IllegalStateException(safeMessage, error))
        }
    }

    fun getGoogleDriveService(): GoogleDriveSyncService {
        return googleDriveSyncService
    }

    companion object {
        /** Re-merge attempts when another device writes mid-sync. */
        private const val MAX_SYNC_ATTEMPTS = 3

        /** Deletions older than this are assumed fully propagated (90 days). */
        private const val TOMBSTONE_RETENTION_MS = 90L * 24 * 60 * 60 * 1000

        fun shouldTriggerSync(
            preferencesManager: PreferencesManager,
            trigger: SyncTrigger
        ): Boolean {
            val settings = preferencesManager.getSyncSettings()
            if (settings.service == SyncServiceType.NONE || !settings.googleDriveSignedIn) {
                return false
            }

            return when (trigger) {
                SyncTrigger.MANUAL,
                SyncTrigger.AUTO -> true
                SyncTrigger.APP_START -> preferencesManager.getSyncTriggerOptions().syncOnAppStart
                SyncTrigger.APP_RESUME -> preferencesManager.getSyncTriggerOptions().syncOnAppResume
                SyncTrigger.CHAPTER_OPEN -> preferencesManager.getSyncTriggerOptions().syncOnChapterOpen
                SyncTrigger.CHAPTER_READ -> preferencesManager.getSyncTriggerOptions().syncOnChapterRead
            }
        }
    }
}

private fun Throwable.toSyncMessage(): String {
    return when (this) {
        is TokenResponseException -> {
            if (details?.error == "invalid_grant") {
                "Google Drive sign-in expired. Connect again."
            } else {
                "Google Drive authorization failed."
            }
        }
        is IOException -> "Network error while syncing. Try again later."
        is IllegalStateException -> message ?: "Sync failed."
        else -> "Sync failed. Try again later."
    }
}

private fun SyncDataSelection.toBackupSelection(): BackupSelection {
    return BackupSelection(
        includeLibrary = syncLibrary,
        includeBookmarks = syncBookmarks,
        includeHistory = syncHistory,
        includeStatistics = syncStatistics,
        includeSettings = syncSettings
    )
}

private fun SyncDataSelection.toRestoreOptions(): RestoreOptions {
    return RestoreOptions(
        restoreLibrary = syncLibrary,
        restoreBookmarks = syncBookmarks,
        restoreHistory = syncHistory,
        restoreStatistics = syncStatistics,
        restoreSettings = syncSettings,
        mergeWithExisting = true
    )
}

private fun SyncDataSelection.enabledCountLabel(): String {
    val enabledCount = listOf(
        syncLibrary,
        syncBookmarks,
        syncHistory,
        syncStatistics,
        syncSettings
    ).count { it }

    return when (enabledCount) {
        1 -> "1 section"
        else -> "$enabledCount sections"
    }
}
