package com.kmhmubin.kothagolp.update

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kmhmubin.kothagolp.domain.model.ChapterUpdateInterval
import java.util.concurrent.TimeUnit

object ChapterUpdateScheduler {

    private const val WORK_NAME = "chapter_update_periodic"

    fun schedule(context: Context, interval: ChapterUpdateInterval, wifiOnly: Boolean) {
        val wm = WorkManager.getInstance(context)

        if (interval == ChapterUpdateInterval.OFF) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }

        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ChapterUpdateWorker>(
            interval.hours(), TimeUnit.HOURS,
            (interval.hours() / 4).coerceAtLeast(1), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ChapterUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME_ONETIME, ExistingWorkPolicy.REPLACE, request)
    }

    internal const val WORK_NAME_ONETIME = "chapter_update_onetime"
}
