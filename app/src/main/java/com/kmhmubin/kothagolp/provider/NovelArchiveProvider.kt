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

class NovelArchiveProvider : MainProvider() {

    override val name = "Novel Archive"
    override val mainUrl = "https://novelarchive.cc"
    override val iconRes = R.drawable.ic_provider_novelarchive
    override val hasMainPage = true

    private val apiUrl = "$mainUrl/api"
    private val jsonHeaders = mapOf("Content-Type" to "application/json")

    override val orderBys = listOf(
        FilterOption("Recent", "recent"),
        FilterOption("Popular", "popular"),
        FilterOption("Top Rated", "rating"),
        FilterOption("Oldest", "oldest")
    )

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Game", "game"),
        FilterOption("Gender Bender", "gender bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("LitRPG", "litrpg"),
        FilterOption("Martial Arts", "martial arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school life"),
        FilterOption("Sci-Fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Slice of Life", "slice of life"),
        FilterOption("Smut", "smut"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("System", "system"),
        FilterOption("Thriller", "thriller"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Urban", "urban"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "recent"
        val genre = tag?.takeIf { it.isNotBlank() }

        val url = buildString {
            append("$apiUrl/novels?page=$page&per_page=24")
            if (sort != "recent") append("&sort=$sort")
            if (!genre.isNullOrBlank()) append("&genres_include=${java.net.URLEncoder.encode(genre, "UTF-8")}")
        }

        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val novels = parseNovels(json.optJSONArray("novels") ?: JSONArray())
            val pagination = json.optJSONObject("pagination")
            val hasNext = pagination?.optBoolean("has_next", false) ?: false
            MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
        } catch (_: Throwable) { MainPageResult(url = url, novels = emptyList()) }
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiUrl/novels?search=$encoded&fuzzy=1&per_page=24"
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            parseNovels(json.optJSONArray("novels") ?: JSONArray())
        } catch (_: Throwable) { emptyList() }
    }

    private fun parseNovels(arr: JSONArray): List<Novel> {
        val result = mutableListOf<Novel>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", null) ?: continue
            val title = obj.optString("title", null) ?: continue
            val coverPath = obj.optString("cover_url", null)
                ?: obj.optString("image_url", null)
                ?: obj.optString("novel_image", null)
            val cover = coverPath?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
            result.add(Novel(
                name = title,
                url = "$mainUrl/novel?id=$id",
                posterUrl = cover,
                apiName = this.name
            ))
        }
        return result
    }

    override suspend fun load(url: String): NovelDetails? {
        val novelId = extractId(url) ?: return null
        val apiDetailUrl = "$apiUrl/novels/$novelId"
        return try {
            val json = JSONObject(get(apiDetailUrl, jsonHeaders).text)
            val novel = json.optJSONObject("novel") ?: return null

            val title = novel.optString("title", null) ?: return null
            val author = novel.optString("author", null)?.takeIf { it.isNotBlank() }
            val synopsis = novel.optString("description", null)
                ?.replace("Show More", "")?.trim()?.takeIf { it.isNotBlank() }

            val coverPath = novel.optString("cover_url", null)
                ?: novel.optString("image_url", null)
                ?: novel.optString("novel_image", null)
            val cover = coverPath?.let { if (it.startsWith("http")) it else "$mainUrl$it" }

            val status = novel.optString("release_status", null)
                ?.replaceFirstChar { it.uppercase() }

            // Genres from comma-separated string, filter out navigation labels
            val excludeGenres = setOf("browse", "latest novels", "completed novels")
            val genres = novel.optString("genres", "")
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() && it !in excludeGenres }
                .distinctBy { it }
                .map { it.replaceFirstChar { c -> c.uppercase() } }

            // Chapters from chapter_names array
            val chapterNames = novel.optJSONArray("chapter_names") ?: JSONArray()
            val totalChaptersStr = novel.optString("total_chapters", "0")
            val totalChapters = totalChaptersStr.toIntOrNull() ?: chapterNames.length()

            val chapters = if (chapterNames.length() > 0) {
                (0 until chapterNames.length()).mapNotNull { i ->
                    val chName = chapterNames.optString(i).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    Chapter(name = chName, url = "$mainUrl/reader?id=$novelId&chapter=${i + 1}")
                }
            } else {
                (1..totalChapters).map { n ->
                    Chapter(name = "Chapter $n", url = "$mainUrl/reader?id=$novelId&chapter=$n")
                }
            }

            NovelDetails(
                url = url, name = title, chapters = chapters,
                author = author, posterUrl = cover, synopsis = synopsis,
                tags = genres.ifEmpty { null }, status = status
            )
        } catch (_: Throwable) { null }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val novelId = extractId(url) ?: return null
        val chapter = extractChapter(url) ?: return null
        val apiChUrl = "$apiUrl/novels/$novelId/chapters/$chapter"
        return try {
            val json = JSONObject(get(apiChUrl, jsonHeaders).text)
            val chapterObj = json.optJSONObject("chapter") ?: return null
            val name = chapterObj.optString("name", "").trim()
            val content = chapterObj.optString("content", "").trim()
            if (content.isBlank()) return null
            buildHtml(name, content)
        } catch (_: Throwable) { null }
    }

    private fun buildHtml(title: String, text: String): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) sb.append("<h2>${escapeHtml(title)}</h2>\n")
        text.split("\n").forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) sb.append("<p>${escapeHtml(trimmed)}</p>\n")
        }
        return sb.toString().trim()
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun extractId(url: String): String? {
        return Regex("[?&]id=([^&]+)").find(url)?.groupValues?.get(1)
            ?: Regex("/novels/([a-f0-9]+)").find(url)?.groupValues?.get(1)
    }

    private fun extractChapter(url: String): String? {
        return Regex("[?&]chapter=([^&]+)").find(url)?.groupValues?.get(1)
    }
}
