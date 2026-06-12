package com.kmhmubin.kothagolp.source

import android.content.Context
import android.util.Log
import com.kmhmubin.kothagolp.provider.MainProvider
import dalvik.system.DexClassLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SourceLoader {

    private const val TAG = "SourceLoader"
    private const val PREFS_NAME = "source_sync_prefs"
    private const val KEY_MANIFEST = "manifest_json"

    private val _isApkLoaded = MutableStateFlow(false)
    val isApkLoaded: StateFlow<Boolean> = _isApkLoaded

    /**
     * Called at app startup. If a downloaded sources.apk exists and a
     * cached manifest is stored, reload providers from the APK.
     * APK providers replace bundled providers with matching names.
     */
    fun loadIfAvailable(context: Context) {
        val apk = context.codeCacheDir.resolve("sources.apk")
            .takeIf { it.exists() }
            ?: context.filesDir.resolve("sources.apk")  // legacy path fallback
        if (!apk.exists()) return

        val manifestJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MANIFEST, null) ?: return

        try {
            val manifest = SourceManifest.fromJson(manifestJson)
            val count = reload(context, manifest)
            Log.i(TAG, "Loaded $count providers from sources.apk (v${manifest.version})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sources.apk on startup", e)
        }
    }

    /**
     * Loads all providers from the APK described by [manifest].
     * Registers each via MainProvider.register() — providers with the same
     * name as an already-registered provider REPLACE the existing entry.
     * Returns the number of successfully loaded providers.
     */
    fun reload(context: Context, manifest: SourceManifest): Int {
        val apk = context.codeCacheDir.resolve("sources.apk")
            .takeIf { it.exists() }
            ?: context.filesDir.resolve("sources.apk")
        if (!apk.exists()) return 0

        val dexOutDir = context.codeCacheDir.resolve("sources_dex").also { it.mkdirs() }
        val loader = DexClassLoader(
            apk.absolutePath,
            dexOutDir.absolutePath,
            null,
            context.classLoader
        )

        var count = 0
        for (entry in manifest.sources) {
            try {
                val clazz = loader.loadClass(entry.className)
                val provider = clazz.getDeclaredConstructor().newInstance() as MainProvider
                MainProvider.register(provider)
                count++
            } catch (e: Exception) {
                Log.w(TAG, "Skipping ${entry.id}: ${e.message}")
            }
        }

        _isApkLoaded.value = count > 0
        Log.i(TAG, "Loaded $count/${manifest.sources.size} providers from sources.apk (v${manifest.version})")
        return count
    }

    fun saveManifest(context: Context, manifestJson: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_MANIFEST, manifestJson).apply()
    }

    fun localVersion(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("sources_version", 0)
    }

    fun saveLocalVersion(context: Context, version: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt("sources_version", version).apply()
    }
}
