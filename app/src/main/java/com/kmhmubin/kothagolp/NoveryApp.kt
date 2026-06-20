package com.kmhmubin.kothagolp

import android.app.Application
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.kmhmubin.kothagolp.data.remote.CloudflareManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.data.sync.SyncWorker
import com.kmhmubin.kothagolp.service.NotificationHelper
import com.kmhmubin.kothagolp.source.SourceLoader
import com.kmhmubin.kothagolp.source.SourceSyncWorker
import com.kmhmubin.kothagolp.tts.TTSManager
import com.kmhmubin.kothagolp.tts.VoiceManager
import com.kmhmubin.kothagolp.update.ChapterUpdateScheduler

/**
 * Application class - initializes app-wide dependencies.
 */
class KothagolpApp : Application() {

    // Lazy-initialized singletons
    val database: NovelDatabase by lazy { NovelDatabase.getInstance(this) }
    val preferences: PreferencesManager by lazy { PreferencesManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()

        CloudflareManager.init(this)

        // Initialize repository provider (only once!)
        RepositoryProvider.initialize(this)

        // Initialize TTS engine
        TTSManager.initialize(this)

        // Initialize Voice Manager
        VoiceManager.initialize(this) {
            // Restore saved voice preference
            val prefs = PreferencesManager.getInstance(this)
            val savedVoiceId = prefs.getTtsVoice()
            if (savedVoiceId != null) {
                VoiceManager.selectVoice(savedVoiceId)
            }
        }

        // Load downloaded sources APK if available
        SourceLoader.loadIfAvailable(this)

        // Check for source updates on launch
        SourceSyncWorker.syncOnce(this)
        // Schedule periodic 24h check only if auto-update is enabled
        val autoUpdate = RepositoryProvider.getPreferencesManager().appSettings.value.autoUpdateSources
        if (autoUpdate) {
            SourceSyncWorker.schedulePeriodicSync(this)
        }

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Restore background sync scheduling from saved preferences.
        SyncWorker.schedule(this)

        // Schedule periodic chapter update checker
        val chapterUpdateSettings = RepositoryProvider.getPreferencesManager().appSettings.value
        ChapterUpdateScheduler.schedule(
            context = this,
            interval = chapterUpdateSettings.chapterUpdateInterval,
            wifiOnly = chapterUpdateSettings.chapterUpdateOnWifiOnly
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        TTSManager.shutdown()
    }

}
