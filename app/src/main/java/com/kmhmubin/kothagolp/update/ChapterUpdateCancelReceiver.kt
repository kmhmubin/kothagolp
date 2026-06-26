package com.kmhmubin.kothagolp.update

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.kmhmubin.kothagolp.service.NotificationHelper

class ChapterUpdateCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotificationHelper.ACTION_CHAPTER_UPDATE_CANCEL) return
        WorkManager.getInstance(context.applicationContext).apply {
            cancelUniqueWork(ChapterUpdateScheduler.WORK_NAME_ONETIME)
            cancelUniqueWork(ChapterUpdateScheduler.WORK_NAME)
        }
        // Dismiss the ongoing notification immediately — don't wait for the worker's finally block
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NotificationHelper.NOTIFICATION_ID_CHAPTER_UPDATE_PROGRESS)
    }
}
