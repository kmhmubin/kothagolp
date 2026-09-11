package com.kmhmubin.kothagolp.data.backup.lnreader

import android.content.Context
import android.net.Uri
import com.kmhmubin.kothagolp.data.backup.BackupData
import com.kmhmubin.kothagolp.data.backup.HistoryBackup
import com.kmhmubin.kothagolp.data.backup.LibraryBackup
import com.kmhmubin.kothagolp.data.backup.ReadChapterBackup
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.zip.ZipInputStream

/**
 * Converts an LNReader backup (a .zip exported from Settings > Backup) into
 * this app's [BackupData].
 *
 * LNReader's zip carries `NovelAndChapters/{id}.json` per library novel
 * (NovelInfo + its chapters) plus Version.json/Category.json/etc. Only the
 * novel/chapter data is used here — Settings.json and Plugins.json describe
 * a different app's config and don't map onto this one.
 *
 * The one thing the backup can't provide is a working novel URL: LNReader
 * stores `path` relative to a per-plugin `site` that lives in the plugin's
 * own code, not in the backup, and isn't discoverable at import time in a
 * way worth trusting (this app's own source domains have moved more than
 * once this year — guessing LNReader's would be worse, not better). So
 * every imported novel gets a stable placeholder URL and an `apiName` that
 * names its original plugin, and shows up needing "Find on other sources"
 * (already a first-class flow in this app) to attach to a live source here.
 * Everything else — reading position, read/unread state, history — comes
 * across intact.
 */
class LNReaderBackupConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun openZip(context: Context, uri: Uri): ZipInputStream? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        return ZipInputStream(stream)
    }

    fun isLNReaderBackup(context: Context, uri: Uri): Boolean {
        return try {
            var found = false
            openZip(context, uri)?.use { zip ->
                var entry = zip.nextEntry
                while (entry != null && !found) {
                    if (entry.name == "Version.json" || entry.name.startsWith("NovelAndChapters/")) {
                        found = true
                    }
                    entry = zip.nextEntry
                }
            }
            found
        } catch (_: Exception) {
            false
        }
    }

    fun convert(context: Context, uri: Uri): BackupData {
        val novels = mutableListOf<LNReaderNovel>()
        openZip(context, uri)?.use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory &&
                    entry.name.startsWith("NovelAndChapters/") &&
                    entry.name.endsWith(".json")
                ) {
                    try {
                        val text = zip.readBytes().toString(Charsets.UTF_8)
                        novels.add(json.decodeFromString<LNReaderNovel>(text))
                    } catch (_: Exception) {
                        // One malformed novel entry shouldn't sink the whole import.
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return buildBackupData(novels)
    }

    private fun parseTimeMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            value.toLongOrNull()
        }
    }

    private fun placeholderUrl(novel: LNReaderNovel) = "lnreader-import://${novel.pluginId}${novel.path}"

    private fun buildBackupData(novels: List<LNReaderNovel>): BackupData {
        val now = System.currentTimeMillis()
        val library = mutableListOf<LibraryBackup>()
        val history = mutableListOf<HistoryBackup>()
        val readChapters = mutableListOf<ReadChapterBackup>()

        for (novel in novels) {
            if (novel.isLocal == true) continue // no remote source to migrate to
            if (novel.inLibrary == false) continue
            if (novel.pluginId.isBlank() || novel.path.isBlank() || novel.name.isBlank()) continue

            val novelUrl = placeholderUrl(novel)
            val apiName = "LNReader: ${novel.pluginId}"
            val posterUrl = novel.cover?.takeIf { it.startsWith("http") }

            val chapters = novel.chapters
            val unreadCount = chapters.count { it.unread != false }
            val readCount = chapters.size - unreadCount
            val readingStatus = when {
                chapters.isNotEmpty() && unreadCount == 0 -> "COMPLETED"
                readCount > 0 -> "READING"
                else -> "PLAN_TO_READ"
            }

            var lastRead: LNReaderChapter? = null
            var lastReadAtMillis: Long? = null
            var lastReadIndex = -1
            chapters.forEachIndexed { index, chapter ->
                val millis = parseTimeMillis(chapter.readTime) ?: return@forEachIndexed
                if (lastReadAtMillis == null || millis > lastReadAtMillis!!) {
                    lastReadAtMillis = millis
                    lastRead = chapter
                    lastReadIndex = index
                }
            }

            library.add(
                LibraryBackup(
                    url = novelUrl,
                    name = novel.name,
                    posterUrl = posterUrl,
                    apiName = apiName,
                    addedAt = now,
                    readingStatus = readingStatus,
                    lastChapterUrl = lastRead?.let { "$novelUrl#${it.path}" },
                    lastChapterName = lastRead?.name,
                    lastReadAt = lastReadAtMillis,
                    totalChapterCount = chapters.size,
                    acknowledgedChapterCount = chapters.size,
                    lastReadChapterIndex = lastReadIndex,
                    unreadChapterCount = unreadCount
                )
            )

            val readAt = lastReadAtMillis
            if (lastRead != null && readAt != null) {
                history.add(
                    HistoryBackup(
                        novelUrl = novelUrl,
                        novelName = novel.name,
                        posterUrl = posterUrl,
                        chapterName = lastRead!!.name,
                        chapterUrl = "$novelUrl#${lastRead!!.path}",
                        apiName = apiName,
                        timestamp = readAt
                    )
                )
            }

            chapters.forEach { chapter ->
                if (chapter.unread == false) {
                    readChapters.add(
                        ReadChapterBackup(
                            chapterUrl = "$novelUrl#${chapter.path}",
                            novelUrl = novelUrl,
                            readAt = parseTimeMillis(chapter.readTime) ?: now
                        )
                    )
                }
            }
        }

        return BackupData(
            version = BackupData.CURRENT_VERSION,
            createdAt = now,
            appVersion = "LNReader Import",
            deviceInfo = "Imported from LNReader",
            library = library,
            history = history,
            readChapters = readChapters
        )
    }
}
