package com.kmhmubin.kothagolp.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.LocalBackupInterval
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "LocalBackupWorker"
private const val MAX_AUTO_BACKUPS = 5
private const val AUTOBACKUP_FOLDER = "autobackup"

class LocalBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager.getInstance(applicationContext)
        val settings = prefs.appSettings.value

        if (settings.localBackupInterval == LocalBackupInterval.OFF) {
            return Result.success()
        }

        return try {
            val database = RepositoryProvider.getDatabase()
            val backupManager = BackupManager(applicationContext, database, prefs)
            val backupJson = backupManager.exportToJson()

            val storageFolderUri = prefs.getStorageFolderUri()
            if (storageFolderUri != null) {
                saveToStorageFolder(storageFolderUri, backupJson)
            } else {
                saveToInternalStorage(backupJson)
            }

            Log.d(TAG, "Local backup completed")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Local backup failed", e)
            Result.retry()
        }
    }

    private fun saveToStorageFolder(treeUriString: String, backupJson: String) {
        val treeUri = Uri.parse(treeUriString)
        val rootDoc = DocumentFile.fromTreeUri(applicationContext, treeUri) ?: run {
            saveToInternalStorage(backupJson)
            return
        }

        val autobackupDir = rootDoc.findFile(AUTOBACKUP_FOLDER)
            ?: rootDoc.createDirectory(AUTOBACKUP_FOLDER)
            ?: run {
                saveToInternalStorage(backupJson)
                return
            }

        val fileName = generateFileName()
        val newFile = autobackupDir.createFile("application/json", fileName) ?: run {
            saveToInternalStorage(backupJson)
            return
        }

        applicationContext.contentResolver.openOutputStream(newFile.uri)?.use { out ->
            out.write(backupJson.toByteArray(Charsets.UTF_8))
        }

        pruneOldBackups(autobackupDir)
    }

    private fun saveToInternalStorage(backupJson: String) {
        val dir = applicationContext.getFilesDir().resolve(AUTOBACKUP_FOLDER).also { it.mkdirs() }
        val file = dir.resolve(generateFileName())
        file.writeText(backupJson, Charsets.UTF_8)
        pruneOldInternalBackups(dir)
    }

    private fun generateFileName(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        return "kothagolp_auto_${fmt.format(Date())}.json"
    }

    private fun pruneOldBackups(dir: DocumentFile) {
        val backups = dir.listFiles()
            .filter { it.name?.startsWith("kothagolp_auto_") == true }
            .sortedByDescending { it.lastModified() }
        backups.drop(MAX_AUTO_BACKUPS).forEach { it.delete() }
    }

    private fun pruneOldInternalBackups(dir: java.io.File) {
        val backups = dir.listFiles { f -> f.name.startsWith("kothagolp_auto_") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        backups.drop(MAX_AUTO_BACKUPS).forEach { it.delete() }
    }

    companion object {
        private const val WORK_NAME = "kothagolp_local_backup_periodic"

        fun schedule(context: Context, interval: LocalBackupInterval, forceUpdate: Boolean = false) {
            val workManager = WorkManager.getInstance(context)

            if (interval == LocalBackupInterval.OFF) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val intervalHours = interval.hours()
            val request = PeriodicWorkRequestBuilder<LocalBackupWorker>(
                intervalHours, TimeUnit.HOURS,
                (intervalHours / 4).coerceAtLeast(1), TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                if (forceUpdate) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                else ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
