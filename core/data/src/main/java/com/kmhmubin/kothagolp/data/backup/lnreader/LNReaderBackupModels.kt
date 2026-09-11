package com.kmhmubin.kothagolp.data.backup.lnreader

import kotlinx.serialization.Serializable

/**
 * Mirrors LNReader's own `ChapterInfo` (src/database/types), as embedded in
 * each novel's entry under NovelAndChapters/ in its backup zip.
 */
@Serializable
data class LNReaderChapter(
    val id: Long = 0,
    val path: String = "",
    val name: String = "",
    val releaseTime: String? = null,
    val readTime: String? = null,
    val unread: Boolean? = null,
    val chapterNumber: Double? = null
)

/**
 * Mirrors LNReader's `BackupNovel` (NovelInfo + chapters). `path` is always
 * relative — LNReader plugins build the real URL as `site + path` at
 * runtime, and that per-plugin `site` isn't part of the backup, so it can't
 * be reconstructed here.
 */
@Serializable
data class LNReaderNovel(
    val id: Long = 0,
    val path: String = "",
    val pluginId: String = "",
    val name: String = "",
    val cover: String? = null,
    val inLibrary: Boolean? = null,
    val isLocal: Boolean? = null,
    val chapters: List<LNReaderChapter> = emptyList()
)
