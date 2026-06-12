package com.kmhmubin.kothagolp.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SyncActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SyncConstants.ACTION_SYNC_CANCEL) {
            return
        }

        val appContext = context.applicationContext
        SyncWorker.cancel(appContext)
        SyncStatusTracker.finishCancelled()
        SyncNotifier(appContext).clear()
    }
}
