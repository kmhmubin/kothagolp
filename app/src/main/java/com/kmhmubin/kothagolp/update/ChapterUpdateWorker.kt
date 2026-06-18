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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = RepositoryProvider.getPreferencesManager()
            val settings = prefs.appSettings.value

            if (!settings.chapterUpdateNotify && settings.chapterUpdateInterval.hours() == 0L) {
                return@withContext Result.success()
            }

            val libraryRepo = RepositoryProvider.getLibraryRepository()

            val result = libraryRepo.refreshNovelsWithFilter(
                getProvider = { apiName -> MainProvider.getProvider(apiName) },
                filter = LibraryFilter.ALL
            )

            Log.d(TAG, "Chapter update: ${result.updatedCount} novels updated, ${result.totalNewChapters} new chapters")

            if (settings.chapterUpdateNotify && result.updatedCount > 0) {
                val novelsWithNew = libraryRepo.getLibrary().filter { it.hasNewChapters }
                postNotification(result.updatedCount, result.totalNewChapters, novelsWithNew.map { it.novel.name })
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Chapter update check failed", e)
            Result.retry()
        }
    }

    private fun postNotification(
        novelCount: Int,
        totalNewChapters: Int,
        novelNames: List<String>
    ) {
        ensureChannel()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            .setSmallIcon(R.drawable.ic_notification_download)
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
        const val NOTIFICATION_ID = 7000
    }
}
