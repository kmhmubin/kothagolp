package com.kmhmubin.kothagolp.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.kmhmubin.kothagolp.service.NotificationHelper

class ChapterUpdateCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotificationHelper.ACTION_CHAPTER_UPDATE_CANCEL) return
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(ChapterUpdateScheduler.WORK_NAME_ONETIME)
    }
}
