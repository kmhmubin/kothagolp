package com.kmhmubin.kothagolp

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache
import com.kmhmubin.kothagolp.data.local.NovelDatabase
import com.kmhmubin.kothagolp.data.local.PreferencesManager
import com.kmhmubin.kothagolp.data.remote.CloudflareManager
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.data.sync.SyncWorker
import com.kmhmubin.kothagolp.service.NotificationHelper
import com.kmhmubin.kothagolp.provider.AllNovelProvider
import com.kmhmubin.kothagolp.provider.CyrisiaProvider
import com.kmhmubin.kothagolp.provider.FenrirRealmProvider
import com.kmhmubin.kothagolp.provider.FreeWebNovelProvider
import com.kmhmubin.kothagolp.provider.FuckNovelPiaProvider
import com.kmhmubin.kothagolp.provider.LibReadProvider
import com.kmhmubin.kothagolp.provider.LightNovelTranslationsProvider
import com.kmhmubin.kothagolp.provider.LightNovelWorldProvider
import com.kmhmubin.kothagolp.provider.LnoriProvider
import com.kmhmubin.kothagolp.provider.MainProvider
import com.kmhmubin.kothagolp.provider.NovelArchiveProvider
import com.kmhmubin.kothagolp.provider.NovelBinProvider
import com.kmhmubin.kothagolp.provider.NovelBuddyProvider
import com.kmhmubin.kothagolp.provider.NovelDexProvider
import com.kmhmubin.kothagolp.provider.NovelFireProvider
import com.kmhmubin.kothagolp.provider.NovelsOnlineProvider
import com.kmhmubin.kothagolp.provider.PawReadProvider
import com.kmhmubin.kothagolp.provider.RanobesProvider
import com.kmhmubin.kothagolp.provider.RoyalRoadProvider
import com.kmhmubin.kothagolp.provider.WebnovelProvider
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

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .allowHardware(true)
                .crossfade(150)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.20)
                        .build()
                }
                .build()
        )

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

        // Register all built-in providers
        MainProvider.register(AllNovelProvider())
        MainProvider.register(CyrisiaProvider())
        MainProvider.register(FenrirRealmProvider())
        MainProvider.register(FreeWebNovelProvider())
        MainProvider.register(FuckNovelPiaProvider())
        MainProvider.register(LibReadProvider())
        MainProvider.register(LightNovelTranslationsProvider())
        MainProvider.register(LightNovelWorldProvider())
        MainProvider.register(LnoriProvider())
        MainProvider.register(NovelArchiveProvider())
        MainProvider.register(NovelBinProvider())
        MainProvider.register(NovelBuddyProvider())
        MainProvider.register(NovelDexProvider())
        MainProvider.register(NovelFireProvider())
        MainProvider.register(NovelsOnlineProvider())
        MainProvider.register(PawReadProvider())
        MainProvider.register(RanobesProvider())
        MainProvider.register(RoyalRoadProvider())
        MainProvider.register(WebnovelProvider())

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
