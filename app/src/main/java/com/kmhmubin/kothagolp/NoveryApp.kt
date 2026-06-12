package com.kmhmubin.kothagolp

import android.app.Application
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.kmhmubin.kothagolp.data.remote.CloudflareManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.data.sync.SyncWorker
import com.kmhmubin.kothagolp.provider.AllNovelProvider
import com.kmhmubin.kothagolp.provider.FreeWebNovelProvider
import com.kmhmubin.kothagolp.provider.LibReadProvider
import com.kmhmubin.kothagolp.provider.LnoriProvider
import com.kmhmubin.kothagolp.provider.MainProvider
import com.kmhmubin.kothagolp.provider.NovelBinProvider
import com.kmhmubin.kothagolp.provider.NovelFireProvider
import com.kmhmubin.kothagolp.provider.NovelsOnlineProvider
import com.kmhmubin.kothagolp.provider.RoyalRoadProvider
import com.kmhmubin.kothagolp.provider.WebnovelProvider
import com.kmhmubin.kothagolp.provider.WtrLabProvider
import com.kmhmubin.kothagolp.service.NotificationHelper
import com.kmhmubin.kothagolp.source.SourceLoader
import com.kmhmubin.kothagolp.source.SourceSyncWorker
import com.kmhmubin.kothagolp.tts.TTSManager
import com.kmhmubin.kothagolp.tts.VoiceManager

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

        // Initialize TTS Manager
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

        // Register all novel providers (bundled fallbacks)
        registerProviders()

        // Load downloaded sources APK if available (replaces bundled providers with same name)
        SourceLoader.loadIfAvailable(this)

        // Check for source updates on launch, schedule 24h periodic check
        SourceSyncWorker.syncOnce(this)
        SourceSyncWorker.schedulePeriodicSync(this)

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Restore background sync scheduling from saved preferences.
        SyncWorker.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        TTSManager.shutdown()
    }

    private fun registerProviders() {
        // Add providers here - order determines display order
        MainProvider.register(NovelFireProvider())
        MainProvider.register(WtrLabProvider())
        MainProvider.register(NovelBinProvider())
        MainProvider.register(LibReadProvider())
        MainProvider.register(RoyalRoadProvider())
        MainProvider.register(NovelsOnlineProvider())
        MainProvider.register(LnoriProvider())
        MainProvider.register(WebnovelProvider())
        MainProvider.register(FreeWebNovelProvider())
        MainProvider.register(AllNovelProvider())
        //MainProvider.register(EmpireNovelProvider())
    }
}
