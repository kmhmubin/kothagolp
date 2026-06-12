package com.kmhmubin.kothagolp.source

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class SourceSyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val (manifestJson, manifest) = fetchManifest()
                ?: return@withContext Result.retry()

            val localVersion = SourceLoader.localVersion(applicationContext)
            if (manifest.version <= localVersion) {
                Log.d(TAG, "Sources up to date (v$localVersion)")
                return@withContext Result.success()
            }

            Log.i(TAG, "Updating sources v$localVersion → v${manifest.version}")
            val tempApk = downloadApk(manifest.url)
                ?: return@withContext Result.retry()

            val destApk = applicationContext.codeCacheDir.resolve("sources.apk")
            tempApk.copyTo(destApk, overwrite = true)
            tempApk.delete()

            SourceLoader.saveManifest(applicationContext, manifestJson)
            SourceLoader.saveLocalVersion(applicationContext, manifest.version)
            SourceLoader.reload(applicationContext, manifest)

            Log.i(TAG, "Sources updated to v${manifest.version}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Source sync failed", e)
            Result.retry()
        }
    }

    private suspend fun fetchManifest(): Pair<String, SourceManifest>? =
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(MANIFEST_URL).build()
                val body = client.newCall(request).execute().body?.string()
                    ?: return@withContext null
                body to SourceManifest.fromJson(body)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch manifest", e)
                null
            }
        }

    private suspend fun downloadApk(url: String): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().body
                ?: return@withContext null

            val temp = applicationContext.cacheDir.resolve("sources_${System.nanoTime()}.apk")
            temp.outputStream().use { out -> body.byteStream().copyTo(out) }
            temp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sources APK", e)
            null
        }
    }

    companion object {
        private const val TAG = "SourceSyncWorker"
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/kmhmubin/kothagolp-sources/main/manifest.json"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Run once immediately (e.g. on app open). Skipped if already queued. */
        fun syncOnce(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "source_sync_once",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SourceSyncWorker>()
                    .setConstraints(networkConstraints)
                    .build()
            )
        }

        /** Schedule a repeating check every 24 hours. */
        fun schedulePeriodicSync(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "source_sync_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SourceSyncWorker>(24, TimeUnit.HOURS)
                    .setConstraints(networkConstraints)
                    .build()
            )
        }
    }
}
