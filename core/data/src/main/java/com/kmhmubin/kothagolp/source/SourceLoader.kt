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
    private const val KEY_LAST_CHECKED = "sources_last_checked"
    private const val APK_NAME = "sources.apk"

    private val _isApkLoaded = MutableStateFlow(false)
    val isApkLoaded: StateFlow<Boolean> = _isApkLoaded

    /** Canonical storage location for sources APK (filesDir avoids codeCacheDir SELinux scrutiny). */
    fun apkFile(context: Context) = context.filesDir.resolve(APK_NAME)

    /**
     * Called at app startup. If a downloaded sources.apk exists and a
     * cached manifest is stored, reload providers from the APK.
     */
    fun loadIfAvailable(context: Context) {
        // Migrate legacy APK from codeCacheDir to filesDir on first run after upgrade.
        val legacy = context.codeCacheDir.resolve(APK_NAME)
        val canonical = apkFile(context)
        if (legacy.exists() && !canonical.exists()) {
            legacy.copyTo(canonical, overwrite = true)
            legacy.delete()
            Log.i(TAG, "Migrated sources.apk from codeCacheDir → filesDir")
        }

        if (!canonical.exists()) return

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
     * Registers each via MainProvider.register().
     * Throws [IllegalStateException] if the APK exists but zero providers loaded —
     * callers should surface this as a user-visible error.
     */
    fun reload(context: Context, manifest: SourceManifest): Int {
        val apk = apkFile(context)
        if (!apk.exists()) return 0

        // W^X policy (Android 10+): ART refuses writable DEX files.
        if (apk.canWrite()) apk.setReadOnly()

        // dex output goes to codeCacheDir (designed for compiled code artifacts)
        val dexOutDir = context.codeCacheDir.resolve("sources_dex").also { it.mkdirs() }

        val loader = try {
            DexClassLoader(
                apk.absolutePath,
                dexOutDir.absolutePath,
                null,
                context.classLoader
            )
        } catch (e: Exception) {
            Log.e(TAG, "DexClassLoader init failed: ${e.message}", e)
            throw IllegalStateException("Failed to initialise source loader: ${e.message}", e)
        }

        var count = 0
        val errors = mutableListOf<String>()
        for (entry in manifest.sources) {
            try {
                val clazz = loader.loadClass(entry.className)
                val provider = clazz.getDeclaredConstructor().newInstance() as MainProvider
                MainProvider.register(provider)
                count++
            } catch (e: Exception) {
                val msg = "${entry.id}: ${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "Failed to load provider $msg", e)
                errors.add(msg)
            }
        }

        _isApkLoaded.value = count > 0
        Log.i(TAG, "Loaded $count/${manifest.sources.size} providers (v${manifest.version})")

        if (count == 0 && manifest.sources.isNotEmpty()) {
            throw IllegalStateException(
                "Source APK loaded but 0/${manifest.sources.size} providers initialised. " +
                "First error: ${errors.firstOrNull() ?: "unknown"}"
            )
        }

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

    fun lastCheckedTime(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CHECKED, 0L)

    fun saveLastCheckedTime(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()
    }
}
