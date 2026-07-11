package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONArray
import org.json.JSONObject

class NovelDexProvider : MainProvider() {

    override val name = "NovelDex"
    override val mainUrl = "https://noveldex.io"
    override val iconRes = R.drawable.ic_provider_noveldex
    override val hasMainPage = true

    private val apiUrl = "$mainUrl/api/series"
    private val jsonHeaders = mapOf("Accept" to "application/json")
    private val rscHeaders = mapOf("rsc" to "1", "Accept" to "*/*")

    override val orderBys = listOf(
        FilterOption("Popular", "popular"),
        FilterOption("Recently Updated", ""),
        FilterOption("Newest", "newest"),
        FilterOption("Most Views", "views"),
        FilterOption("Longest", "longest"),
        FilterOption("Top Rated", "rating")
    )

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Harem", "harem"),
        FilterOption("Horror", "horror"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Romance", "romance"),
        FilterOption("Sci-Fi", "sci-fi"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("System", "system"),
        FilterOption("Thriller", "thriller"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Cultivation", "cultivation"),
        FilterOption("Regression", "regression"),
        FilterOption("Apocalypse", "apocalypse"),
        FilterOption("Overpowered", "overpowered"),
        FilterOption("Transmigration", "transmigration")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "popular"
        val url = buildString {
            append("$apiUrl?page=$page&limit=24")
            if (sort.isNotBlank()) append("&sort=$sort")
            if (!tag.isNullOrBlank()) append("&genre=$tag")
        }
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val data = json.optJSONArray("data") ?: JSONArray()
            val meta = json.optJSONObject("meta")
            val hasMore = meta?.optBoolean("hasMore", false) ?: false
            MainPageResult(url = url, novels = parseNovels(data), hasNextPage = hasMore)
        } catch (_: Throwable) { MainPageResult(url = url, novels = emptyList()) }
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiUrl?search=$encoded&page=1&limit=24"
        return try {
            val data = JSONObject(get(url, jsonHeaders).text).optJSONArray("data") ?: JSONArray()
            parseNovels(data)
        } catch (_: Throwable) { emptyList() }
    }

    private fun parseNovels(data: JSONArray): List<Novel> {
        val result = mutableListOf<Novel>()
        for (i in 0 until data.length()) {
            val obj = data.optJSONObject(i) ?: continue
            val title = obj.optString("title", null) ?: continue
            val slug = obj.optString("slug", null) ?: continue
            val type = obj.optString("type", "WEB_NOVEL")
            val typeSegment = typeToSegment(type)
            val coverImage = obj.optString("coverImage", null)
            val cover = coverImage?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
            result.add(Novel(
                name = title.cleanTitle(),
                url = "/series/$typeSegment/$slug",
                posterUrl = cover,
                apiName = this.name
            ))
        }
        return result
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val body = get(fullUrl, rscHeaders).text

        // Extract description — first one in RSC is the series description
        val descriptionRaw = Regex(""""description"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)
            ?.groupValues?.get(1)?.unescape()

        // Extract title nearest to slug (avoids meta/nav titles)
        val titleRaw = Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"\s*,\s*"slug"\s*:""").find(body)
            ?.groupValues?.get(1)?.unescape()
            ?: Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)
                ?.groupValues?.get(1)?.unescape()
            ?: return null

        val title = titleRaw.cleanTitle()

        val coverImageRaw = Regex(""""coverImage"\s*:\s*"(/[^"]+)"""").find(body)
            ?.groupValues?.get(1)
        val cover = coverImageRaw?.let { "$mainUrl$it" }

        val teamName = Regex(""""team"\s*:\s*\{[^}]*"name"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)
            ?.groupValues?.get(1)?.unescape()

        val statusRaw = Regex(""""status"\s*:\s*"([A-Z_]+)"""").find(body)?.groupValues?.get(1)
        val status = statusRaw?.let {
            when (it) {
                "ONGOING" -> "Ongoing"
                "COMPLETED" -> "Completed"
                "HIATUS" -> "Hiatus"
                "CANCELLED", "CANCELED" -> "Cancelled"
                "DROPPED" -> "Dropped"
                else -> it.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        }

        val genresSection = Regex(""""genres"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(body)
        val genres = genresSection?.groupValues?.get(1)?.let { gs ->
            Regex(""""name"\s*:\s*"((?:[^"\\]|\\.)*)"""").findAll(gs)
                .map { it.groupValues[1].unescape() }.filter { it.isNotBlank() }.toList()
        } ?: emptyList()

        val chapters = parseChaptersFromRsc(body, fullUrl)

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = teamName, posterUrl = cover,
            synopsis = descriptionRaw?.takeIf { it.isNotBlank() },
            tags = genres.ifEmpty { null }, status = status
        )
    }

    private fun parseChaptersFromRsc(body: String, seriesUrl: String): List<Chapter> {
        val slugMatch = Regex("""/series/([^/]+)/([^/?#]+)""").find(seriesUrl)
        val typeSegment = slugMatch?.groupValues?.get(1) ?: "novel"
        val slug = slugMatch?.groupValues?.get(2) ?: return emptyList()

        // Try "allChapters" first (full list)
        val allChaptersMatch = Regex(""""allChapters"\s*:\s*(\[.*?\])(?=\s*[,}])""", RegexOption.DOT_MATCHES_ALL)
            .find(body)
        if (allChaptersMatch != null) {
            val chapters = parseChapterArray(allChaptersMatch.groupValues[1], typeSegment, slug)
            if (chapters.isNotEmpty()) return chapters
        }

        // Fallback to "chapters"
        val chaptersMatch = Regex(""""chapters"\s*:\s*(\[.*?\])(?=\s*[,}])""", RegexOption.DOT_MATCHES_ALL)
            .find(body)
        if (chaptersMatch != null) {
            return parseChapterArray(chaptersMatch.groupValues[1], typeSegment, slug)
        }

        return emptyList()
    }

    private fun parseChapterArray(jsonStr: String, typeSegment: String, slug: String): List<Chapter> {
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val number = obj.optInt("number", -1).takeIf { it >= 0 } ?: return@mapNotNull null
                val chTitle = obj.optString("title", null) ?: "Chapter $number"
                val publishedAt = obj.optString("publishedAt", null)
                val isLocked = obj.optBoolean("isLocked", false)
                val prefix = if (isLocked) "🔒 " else ""
                Chapter(
                    name = "$prefix$chTitle",
                    url = "$mainUrl/series/$typeSegment/$slug/chapter/$number",
                    dateOfRelease = publishedAt
                )
            }.sortedBy { it.url }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val body = get(fullUrl, rscHeaders).text

        // Try XOR decryption first (primary content delivery method)
        val xorResult = extractFromXorEncryption(body)
        if (!xorResult.isNullOrBlank()) return xorResult

        // Fallback: scan RSC T-tags for chapter HTML
        return extractFromTTags(body)
    }

    private fun extractFromXorEncryption(body: String): String? {
        val xorMatch = Regex(""""xorEncryption"\s*:\s*\{([^}]+)\}""").find(body) ?: return null
        val xorObj = xorMatch.groupValues[1]

        val encField = Regex(""""encryptedBase64"\s*:\s*"([^"]+)"""").find(xorObj)
            ?.groupValues?.get(1) ?: return null
        val partialKeyHint = Regex(""""partialKeyHint"\s*:\s*"([^"]+)"""").find(xorObj)
            ?.groupValues?.get(1) ?: return null
        val timestamp = Regex(""""timestamp"\s*:\s*(\d+)""").find(xorObj)
            ?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val clientNonce = Regex(""""clientNonce"\s*:\s*"([^"]+)"""").find(xorObj)
            ?.groupValues?.get(1) ?: return null

        // Resolve encryptedBase64 — may be a "$KEY" RSC pointer to a T-tag
        val encryptedBase64 = resolveRscPointer(body, encField) ?: return null
        val decrypted = decryptXor(encryptedBase64, partialKeyHint, timestamp, clientNonce)
        if (decrypted.isNullOrBlank() || decrypted.length < 20) return null
        return stripWatermark(decrypted)
    }

    private fun resolveRscPointer(body: String, field: String): String? {
        if (!field.startsWith("\$")) return field
        val key = field.removePrefix("\$")
        // Look for T-tag: "\nKEY:THEXSIZE,<content>"
        val prefix = "$key:T"
        var idx = body.indexOf("\n$prefix")
        if (idx >= 0) idx++ else if (body.startsWith(prefix)) idx = 0 else return null
        val sizeStart = idx + prefix.length
        val commaIdx = body.indexOf(',', sizeStart)
        if (commaIdx <= sizeStart || commaIdx - sizeStart > 8) return null
        val sizeBytes = body.substring(sizeStart, commaIdx).toIntOrNull(16) ?: return null
        if (sizeBytes < 10) return null
        return extractTTagBytes(body, commaIdx + 1, sizeBytes)
    }

    private fun extractTTagBytes(body: String, start: Int, sizeBytes: Int): String {
        if (start >= body.length) return ""
        val bytes = body.substring(start).toByteArray(Charsets.UTF_8)
        val len = sizeBytes.coerceAtMost(bytes.size)
        return String(bytes, 0, len, Charsets.UTF_8)
    }

    private fun decryptXor(encryptedBase64: String, partialKeyHint: String, timestamp: Long, clientNonce: String): String? {
        return try {
            val key = deriveXorKey(partialKeyHint, timestamp, clientNonce)
            val cipherBytes = java.util.Base64.getDecoder().decode(encryptedBase64.trim())
            val plainBytes = ByteArray(cipherBytes.size) { i ->
                (cipherBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Throwable) { null }
    }

    private fun deriveXorKey(partialKeyHint: String, timestamp: Long, clientNonce: String): ByteArray {
        val combined = "$partialKeyHint|$timestamp|$clientNonce"
        var hash = 0x811c9dc5u
        for (ch in combined) {
            hash = (hash xor ch.code.toUInt()) * 0x1000193u
        }
        val key = ByteArray(32)
        for (i in 0 until 32) {
            hash = (hash xor (i.toUInt() * 0x9e3779b9u)) * 0x1000193u
            key[i] = (hash and 0xffu).toByte()
        }
        return key
    }

    private fun extractFromTTags(body: String): String? {
        val tTagRegex = Regex("""[0-9a-fA-F]+:T([0-9a-fA-F]+),""")
        var bestContent: String? = null
        var bestScore = 0
        for (match in tTagRegex.findAll(body)) {
            val sizeBytes = match.groupValues[1].toIntOrNull(16) ?: continue
            if (sizeBytes < 100) continue
            val contentStart = match.range.last + 1
            val raw = extractTTagBytes(body, contentStart, sizeBytes)
            val content = stripWatermark(raw)
            if (content.isBlank() || content.length < 50) continue
            if (content.trimStart().startsWith("{") || content.trimStart().startsWith("<script")) continue
            var score = content.length / 10
            score += Regex("<p[ >]").findAll(content).count() * 50
            score += Regex("<br").findAll(content).count() * 5
            if (content.contains("function ") || content.contains("var ")) score -= 500
            if (score > bestScore) { bestScore = score; bestContent = content }
        }
        return bestContent
    }

    private fun stripWatermark(text: String): String = text
        .replace(Regex("[﻿​‌‍‎‏͏⁠-⁤⁩￾]+"), "")
        .trim()

    private fun typeToSegment(type: String): String = when (type) {
        "WEB_NOVEL" -> "novel"
        "MANHWA" -> "manhwa"
        "MANGA" -> "manga"
        "MANHUA" -> "manhua"
        "WEBTOON" -> "webtoon"
        else -> "novel"
    }

    private fun String.cleanTitle(): String = this
        .replace(" — New Chapters", "")
        .replace(" - New Chapters", "")
        .trim()

    private fun String.unescape(): String = this
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "")
        .replace("\\/", "/")
        .replace("\\\\", "\\")
        .replace("\\t", "\t")
}
