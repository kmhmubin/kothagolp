package com.kmhmubin.kothagolp.epub

import java.util.UUID

/**
 * Metadata for an EPUB book
 */
data class EpubMetadata(
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val language: String = "en",
    val publisher: String = "Kothagolp",
    val tags: List<String> = emptyList(),
    val uuid: String = UUID.randomUUID().toString(),
    val creationDate: String = java.time.LocalDate.now().toString()
) {
    val identifier: String get() = "urn:uuid:$uuid"

    val safeTitle: String get() = title
        .replace(Regex("[<>:\"/\\\\|?*]"), "_")
        .take(100)

    val safeFileName: String get() = "${safeTitle}.epub"
}