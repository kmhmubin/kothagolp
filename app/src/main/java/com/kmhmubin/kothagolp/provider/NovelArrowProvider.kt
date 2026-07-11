package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.json.JSONArray
import org.json.JSONObject

/**
 * NovelArrow (novelarrow.com) — a Next.js site with a JSON API under /api-web.
 *
 * API map (all GET, JSON):
 *  - browse/search: /api-web/novels?page=N[&keyword=][&genre=][&status=]
 *  - details:       /api-web/novels/{id}          -> item.novelInfo
 *  - chapter list:  /api-web/novels/{id}/chapters?sort=asc -> items[]
 *  - content:       /api-web/novels/{id}/chapters/{chapterId}
 *                   -> item.chapterInfo.chapter_content (HTML)
 *  - covers:        https://images.novelarrow.com/novel/{id}.jpg
 */
class NovelArrowProvider : MainProvider() {

    override val name = "NovelArrow"
    override val mainUrl = "https://novelarrow.com"
    override val iconRes = R.drawable.ic_provider_novelarrow
    override val hasMainPage = true

    private val apiBase = "$mainUrl/api-web"
    private val jsonHeaders = mapOf("Accept" to "application/json")

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Fan-fiction", "fan-fiction"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Game", "game"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("LitRPG", "litrpg"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Smut", "smut"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("System", "system"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    // The new API has no working sort parameter; ordering is the site default
    // (recently updated). Status doubles as the only server-side list filter.
    override val orderBys = listOf(
        FilterOption("All", ""),
        FilterOption("Ongoing", "ongoing"),
        FilterOption("Completed", "completed")
    )

    private fun coverUrl(novelId: String) = "https://images.novelarrow.com/novel/$novelId.jpg"

    private fun parseNovels(items: JSONArray): List<Novel> {
        val novels = mutableListOf<Novel>()
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val id = obj.optString("novel_id", null) ?: continue
            val title = obj.optString("novel_name", null) ?: continue
            novels.add(
                Novel(
                    name = title,
                    url = "$mainUrl/novel/$id",
                    posterUrl = coverUrl(id),
                    apiName = this.name
                )
            )
        }
        return novels
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val params = buildString {
            append("page=$page")
            orderBy?.takeIf { it.isNotBlank() }?.let { append("&status=$it") }
            tag?.takeIf { it.isNotBlank() }?.let { append("&genre=$it") }
        }
        val url = "$apiBase/novels?$params"
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val novels = parseNovels(json.optJSONArray("items") ?: JSONArray())
            val pagination = json.optJSONObject("pagination")
            val hasNext = pagination != null &&
                pagination.optInt("page", page) < pagination.optInt("totalPages", page)
            MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
        } catch (_: Throwable) {
            MainPageResult(url = url, novels = emptyList())
        }
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        return try {
            val json = JSONObject(get("$apiBase/novels?keyword=$encoded&page=1", jsonHeaders).text)
            parseNovels(json.optJSONArray("items") ?: JSONArray())
        } catch (_: Throwable) { emptyList() }
    }

    /** Novel id is the last path segment of /novel/{id} (or a legacy URL). */
    private fun novelIdFromUrl(url: String): String =
        url.substringBefore("?").trimEnd('/').substringAfterLast("/")

    override suspend fun load(url: String): NovelDetails? {
        val novelId = novelIdFromUrl(url)
        if (novelId.isBlank()) return null
        val json = try {
            JSONObject(get("$apiBase/novels/$novelId", jsonHeaders).text)
        } catch (_: Throwable) { return null }
        val info = json.optJSONObject("item")?.optJSONObject("novelInfo") ?: return null

        val title = info.optString("novel_name", null) ?: return null
        val author = info.optString("novel_author", null)?.takeIf { it.isNotBlank() }
        val synopsis = info.optString("novel_desc", null)?.takeIf { it.isNotBlank() }
        // Verified against the status filter: ?status=ongoing -> 0, ?status=completed -> 1
        val status = when (info.optInt("novel_status", -1)) {
            0 -> "Ongoing"
            1 -> "Completed"
            else -> null
        }
        val genres = info.optJSONArray("novel_genres")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it, null) }
                .map { g -> g.lowercase().replaceFirstChar { it.uppercase() } }
        } ?: emptyList()
        // avgPoint is {"$numberDecimal": "4.51..."} on a 0-5 scale
        val rating = info.optJSONObject("avgPoint")
            ?.optString("\$numberDecimal", null)?.toFloatOrNull()
            ?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
        val peopleVoted = info.optInt("voteCount", 0).takeIf { it > 0 }

        return NovelDetails(
            url = "$mainUrl/novel/$novelId",
            name = title,
            chapters = loadChapters(novelId),
            author = author,
            posterUrl = coverUrl(novelId),
            synopsis = synopsis,
            tags = genres.ifEmpty { null },
            rating = rating,
            peopleVoted = peopleVoted,
            status = status
        )
    }

    private suspend fun loadChapters(novelId: String): List<Chapter> {
        return try {
            val json = JSONObject(get("$apiBase/novels/$novelId/chapters?sort=asc", jsonHeaders).text)
            val items = json.optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { i ->
                val obj = items.optJSONObject(i) ?: return@mapNotNull null
                val chapterId = obj.optString("chapter_id", null) ?: return@mapNotNull null
                val chapterName = obj.optString("chapter_name", null)
                    ?.takeIf { it.isNotBlank() } ?: "Chapter ${i + 1}"
                Chapter(name = chapterName, url = "$mainUrl/chapter/$novelId/$chapterId")
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        // Chapter URLs are /chapter/{novelId}/{chapterId}; strip any host
        val segments = url.substringBefore("?").trimEnd('/')
            .replace(Regex("^https?://[^/]+"), "").trim('/').split("/")
        if (segments.size < 3 || segments[0] != "chapter") return null
        val novelId = segments[1]
        val chapterId = segments[2]
        return try {
            val json = JSONObject(
                get("$apiBase/novels/$novelId/chapters/$chapterId", jsonHeaders).text
            )
            json.optJSONObject("item")
                ?.optJSONObject("chapterInfo")
                ?.optString("chapter_content", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) { null }
    }
}
