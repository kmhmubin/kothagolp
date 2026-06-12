package com.kmhmubin.kothagolp.data.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kmhmubin.kothagolp.core.data.R

class SyncNotifier(
    private val context: Context
) {
    fun showProgress(stage: String): Notification {
        val notification = NotificationCompat.Builder(context, SyncConstants.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle("Syncing library")
            .setContentText(stage)
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_notification_cancel,
                "Cancel",
                getSyncCancelPendingIntent(context)
            )
            .build()

        notify(notification)
        return notification
    }

    fun showSuccess(message: String) {
        val notification = NotificationCompat.Builder(context, SyncConstants.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_done)
            .setContentTitle("Sync complete")
            .setContentText(message)
            .setContentIntent(contentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    fun showError(message: String) {
        val notification = NotificationCompat.Builder(context, SyncConstants.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_error)
            .setContentTitle("Sync failed")
            .setContentText(message)
            .setContentIntent(contentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    fun clear() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(SyncConstants.NOTIFICATION_ID_SYNC)
        }
    }

    private fun notify(notification: Notification) {
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(SyncConstants.NOTIFICATION_ID_SYNC, notification)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            SyncConstants.NOTIFICATION_ID_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun getSyncCancelPendingIntent(context: Context): PendingIntent {
            val intent = Intent(SyncConstants.ACTION_SYNC_CANCEL)
            intent.`package` = context.packageName
            return PendingIntent.getBroadcast(
                context,
                SyncConstants.NOTIFICATION_ID_SYNC,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
