package com.kmhmubin.kothagolp.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kmhmubin.kothagolp.MainActivity
import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.LibraryFilter
import com.kmhmubin.kothagolp.provider.MainProvider
import com.kmhmubin.kothagolp.service.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChapterUpdateWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val nm by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = RepositoryProvider.getPreferencesManager()
        val settings = prefs.appSettings.value

        if (!settings.chapterUpdateNotify && settings.chapterUpdateInterval.hours() == 0L) {
            return@withContext Result.success()
        }

        postProgressNotification(0, 0)
        try {
            val libraryRepo = RepositoryProvider.getLibraryRepository()

            val result = libraryRepo.refreshNovelsWithFilter(
                getProvider = { apiName -> MainProvider.getProvider(apiName) },
                filter = LibraryFilter.ALL,
                onProgress = { current, total, _ -> postProgressNotification(current, total) }
            )

            Log.d(TAG, "Chapter update: ${result.updatedCount} novels updated, ${result.totalNewChapters} new chapters")

            if (settings.chapterUpdateNotify && result.updatedCount > 0) {
                val novelsWithNew = libraryRepo.getLibrary().filter { it.hasNewChapters }
                postResultNotification(result.updatedCount, result.totalNewChapters, novelsWithNew.map { it.novel.name })
            }

            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Chapter update cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chapter update check failed", e)
            Result.retry()
        } finally {
            nm.cancel(NOTIFICATION_ID_PROGRESS)
        }
    }

    private fun postProgressNotification(current: Int, total: Int) {
        ensureChannel()
        val indeterminate = total == 0
        val contentText = if (indeterminate) "Scanning your library…" else "$current / $total books"
        val cancelIntent = Intent(NotificationHelper.ACTION_CHAPTER_UPDATE_CANCEL).apply {
            setPackage(applicationContext.packageName)
        }
        val cancelPending = PendingIntent.getBroadcast(
            applicationContext, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setContentTitle("Checking for new chapters")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, current, indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setShowWhen(false)
            .addAction(R.drawable.ic_notification_cancel, "Cancel", cancelPending)
            .build()
        nm.notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    private fun postResultNotification(
        novelCount: Int,
        totalNewChapters: Int,
        novelNames: List<String>
    ) {
        ensureChannel()

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (novelCount == 1) {
            "${novelNames.firstOrNull() ?: "Novel"} updated"
        } else {
            "$novelCount novels updated"
        }

        val body = if (totalNewChapters == 1) "1 new chapter available" else "$totalNewChapters new chapters available"

        val expandedText = novelNames.take(10).joinToString("\n") { "• $it" } +
            if (novelNames.size > 10) "\n…and ${novelNames.size - 10} more" else ""

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_CHAPTER_UPDATES)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(expandedText)
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(NotificationHelper.CHANNEL_CHAPTER_UPDATES) == null) {
                val channel = NotificationChannel(
                    NotificationHelper.CHANNEL_CHAPTER_UPDATES,
                    "New Chapters",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies when new chapters are available in your library"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val TAG = "ChapterUpdateWorker"
        private val NOTIFICATION_ID_PROGRESS = NotificationHelper.NOTIFICATION_ID_CHAPTER_UPDATE_PROGRESS
        const val NOTIFICATION_ID = 7000
    }
}
