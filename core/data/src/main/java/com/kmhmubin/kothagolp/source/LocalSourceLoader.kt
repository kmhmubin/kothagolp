package com.kmhmubin.kothagolp.source

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kmhmubin.kothagolp.provider.MainProvider
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages locally-imported source APKs for testing before official CI release.
 *
 * Storage layout (all internal — no permissions needed):
 *   filesDir/local_sources/
 *     {id}.apk   — copied APK bytecode
 *     {id}.json  — metadata (provider names + class names)
 *     _staging.apk — temp file during import, always cleaned up
 *
 * Local APKs are loaded AFTER the official sources.apk so they shadow/override
 * any official provider with the same name during testing.
 *
 * On the next official CI release you just clear the local APK and the official
 * version takes over automatically.
 */
object LocalSourceLoader {

    private const val TAG = "LocalSourceLoader"
    private const val DIR_NAME = "local_sources"

    // ── Public types ─────────────────────────────────────────────────────────

    data class LocalSourceMeta(
        val id: String,
        val fileName: String,
        val providerNames: List<String>,
        val classNames: List<String>,
        val importedAt: Long
    ) {
        val displayDate: String
            get() = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(importedAt))

        companion object {
            fun fromJson(json: String): LocalSourceMeta {
                val obj = JSONObject(json)
                val names = obj.getJSONArray("providerNames")
                val classes = obj.getJSONArray("classNames")
                return LocalSourceMeta(
                    id = obj.getString("id"),
                    fileName = obj.getString("fileName"),
                    importedAt = obj.getLong("importedAt"),
                    providerNames = (0 until names.length()).map { names.getString(it) },
                    classNames = (0 until classes.length()).map { classes.getString(it) }
                )
            }
        }

        fun toJson(): String {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("fileName", fileName)
            obj.put("importedAt", importedAt)
            val namesArr = JSONArray().also { arr -> providerNames.forEach { arr.put(it) } }
            obj.put("providerNames", namesArr)
            val classArr = JSONArray().also { arr -> classNames.forEach { arr.put(it) } }
            obj.put("classNames", classArr)
            return obj.toString(2)
        }
    }

    sealed class ImportResult {
        data class Success(val providerNames: List<String>, val count: Int) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    // ── Directories ──────────────────────────────────────────────────────────

    private fun getDir(context: Context): File =
        context.filesDir.resolve(DIR_NAME).also { it.mkdirs() }

    private fun getDexOutDir(context: Context): File =
        context.codeCacheDir.resolve("local_sources_dex").also { it.mkdirs() }

    // ── Import ───────────────────────────────────────────────────────────────

    suspend fun importApk(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val dir = getDir(context)
        val staging = dir.resolve("_staging.apk")

        try {
            // 1. Copy from content URI to internal storage
            context.contentResolver.openInputStream(uri)?.use { input ->
                staging.outputStream().use { out -> input.copyTo(out) }
            } ?: return@withContext ImportResult.Failure("Cannot open selected file")

            if (!staging.exists() || staging.length() == 0L) {
                return@withContext ImportResult.Failure("File is empty or unreadable")
            }

            // 2. Scan DEX for MainProvider subclasses
            val dexOutDir = getDexOutDir(context)
            val found = scanForProviders(staging, dexOutDir, context.classLoader)

            if (found.isEmpty()) {
                staging.delete()
                return@withContext ImportResult.Failure(
                    "No MainProvider subclasses found. " +
                    "Build with ./gradlew :sources:assembleRelease from kothagolp-sources."
                )
            }

            // 3. Remove any stale local APK that contains the same provider names
            val providerNames = found.map { it.second }
            val classNames = found.map { it.first }
            getAll(context).forEach { existing ->
                val overlap = existing.providerNames.any { name ->
                    providerNames.any { it.equals(name, ignoreCase = true) }
                }
                if (overlap) remove(context, existing.id)
            }

            // 4. Move staging to final path, named by providers + timestamp
            val ts = System.currentTimeMillis()
            val safeName = providerNames.joinToString("_")
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(64)
            val id = "${safeName}_$ts"
            val destApk = dir.resolve("$id.apk")
            staging.renameTo(destApk)

            // 5. Write metadata JSON
            val meta = LocalSourceMeta(
                id = id,
                fileName = destApk.name,
                providerNames = providerNames,
                classNames = classNames,
                importedAt = ts
            )
            dir.resolve("$id.json").writeText(meta.toJson())

            // 6. Load into provider registry immediately (no restart needed)
            val loaded = loadApk(context, destApk, classNames)

            Log.i(TAG, "Imported ${found.size} local providers: $providerNames")
            ImportResult.Success(providerNames, loaded)

        } catch (e: Exception) {
            staging.delete()
            Log.e(TAG, "Import failed", e)
            ImportResult.Failure(e.message ?: "Unknown error")
        }
    }

    // ── Startup load ─────────────────────────────────────────────────────────

    /**
     * Call at app startup AFTER SourceLoader.loadIfAvailable().
     * Local APKs shadow official providers with the same name.
     */
    fun loadAll(context: Context) {
        val dir = getDir(context)
        if (!dir.exists()) return

        val jsonFiles = dir.listFiles { f -> f.extension == "json" && !f.name.startsWith("_") }
            ?: return

        var total = 0
        for (jsonFile in jsonFiles) {
            try {
                val meta = LocalSourceMeta.fromJson(jsonFile.readText())
                val apk = dir.resolve(meta.fileName)
                if (!apk.exists()) {
                    jsonFile.delete()
                    continue
                }
                val count = loadApk(context, apk, meta.classNames)
                total += count
                Log.i(TAG, "Loaded $count local providers from ${meta.fileName}")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to load ${jsonFile.name}: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        if (total > 0) Log.i(TAG, "Total local providers loaded on startup: $total")
    }

    // ── Query / remove ───────────────────────────────────────────────────────

    fun getAll(context: Context): List<LocalSourceMeta> {
        val dir = getDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" && !f.name.startsWith("_") }
            ?.mapNotNull { f ->
                try { LocalSourceMeta.fromJson(f.readText()) } catch (_: Exception) { null }
            }
            ?.sortedByDescending { it.importedAt }
            ?: emptyList()
    }

    fun remove(context: Context, id: String) {
        val dir = getDir(context)
        dir.resolve("$id.apk").delete()
        dir.resolve("$id.json").delete()
        Log.i(TAG, "Removed local source: $id")
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun loadApk(context: Context, apk: File, classNames: List<String>): Int {
        val dexOutDir = getDexOutDir(context)
        val loader = DexClassLoader(
            apk.absolutePath,
            dexOutDir.absolutePath,
            null,
            context.classLoader
        )
        var count = 0
        for (className in classNames) {
            try {
                val clazz = loader.loadClass(className)
                val provider = clazz.getDeclaredConstructor().newInstance() as MainProvider
                MainProvider.register(provider)
                count++
            } catch (e: Throwable) {
                Log.w(TAG, "Skipping $className: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        return count
    }

    /**
     * Enumerate all classes in the APK's DEX and return those that are concrete
     * MainProvider subclasses.
     *
     * DexFile is deprecated since API 26 but remains functional through current
     * Android versions. It is the only public API that enumerates DEX class names
     * without knowing them in advance.
     */
    @Suppress("DEPRECATION")
    private fun scanForProviders(
        apk: File,
        dexOutDir: File,
        parentClassLoader: ClassLoader
    ): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val loader = DexClassLoader(apk.absolutePath, dexOutDir.absolutePath, null, parentClassLoader)

        try {
            val dexFile = dalvik.system.DexFile(apk.absolutePath)
            val entries = dexFile.entries()
            while (entries.hasMoreElements()) {
                val className = entries.nextElement()
                // Only inspect our sources package — skip all Android/Kotlin framework classes
                if (!className.startsWith("com.kmhmubin.kothagolp.")) continue
                try {
                    val clazz = loader.loadClass(className)
                    val mods = clazz.modifiers
                    if (MainProvider::class.java.isAssignableFrom(clazz) &&
                        !java.lang.reflect.Modifier.isAbstract(mods) &&
                        !clazz.isInterface
                    ) {
                        val instance = clazz.getDeclaredConstructor().newInstance() as MainProvider
                        result.add(className to instance.name)
                    }
                } catch (_: Exception) {}
            }
            dexFile.close()
        } catch (e: Exception) {
            Log.e(TAG, "DEX scan failed: ${e.message}")
        }

        return result
    }
}
