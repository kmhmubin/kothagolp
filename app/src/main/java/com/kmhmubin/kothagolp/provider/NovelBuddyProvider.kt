package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONArray
import org.json.JSONObject

class NovelBuddyProvider : MainProvider() {

    override val name = "Novel Buddy"
    override val mainUrl = "https://novelbuddy.io"
    private val apiUrl = "https://api.novelbuddy.io"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=novelbuddy.io&sz=64"
    override val hasMainPage = true

    private val jsonHeaders = mapOf("Accept" to "application/json")

    override val orderBys = listOf(
        FilterOption("Most Viewed", "views"),
        FilterOption("Latest Updated", "latest"),
        FilterOption("Popular", "popular"),
        FilterOption("Top Rated", "rating"),
        FilterOption("Most Chapters", "chapters")
    )

    override val tags = listOf(
        FilterOption("All", "all"),
        FilterOption("Ongoing", "ongoing"),
        FilterOption("Completed", "completed"),
        FilterOption("Hiatus", "hiatus"),
        FilterOption("Cancelled", "cancelled")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "views"
        val status = tag?.takeIf { it.isNotBlank() } ?: "all"
        val url = "$apiUrl/titles/search?sort=$sort&status=$status&limit=24&page=$page"
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val data = json.optJSONObject("data") ?: return MainPageResult(url = url, novels = emptyList())
            val items = data.optJSONArray("items") ?: JSONArray()
            val novels = parseNovels(items)
            val hasNext = data.optJSONObject("pagination")?.optBoolean("has_next", false) ?: false
            MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
        } catch (_: Throwable) { MainPageResult(url = url, novels = emptyList()) }
    }

    private fun parseNovels(items: JSONArray): List<Novel> {
        val novels = mutableListOf<Novel>()
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val title = obj.optString("name", null) ?: continue
            val novelUrl = obj.optString("url", null) ?: continue
            val cover = obj.optString("cover", null)
            val fullUrl = when {
                novelUrl.startsWith("http") -> novelUrl
                else -> "$mainUrl/${novelUrl.trimStart('/')}"
            }
            val coverUrl = cover?.let {
                when {
                    it.startsWith("//") -> "https:$it"
                    it.startsWith("http") -> it
                    else -> it
                }
            }
            novels.add(Novel(name = title, url = fullUrl, posterUrl = coverUrl, apiName = this.name))
        }
        return novels
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiUrl/titles/search?q=$encoded&limit=24&page=1"
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val items = json.optJSONObject("data")?.optJSONArray("items") ?: JSONArray()
            parseNovels(items)
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/${url.trimStart('/')}"
        val document = get(fullUrl).document

        val scriptData = document.selectFirstOrNull("#__NEXT_DATA__")?.data() ?: return null
        val json = try { JSONObject(scriptData) } catch (_: Throwable) { return null }

        val initialManga = json.optJSONObject("props")
            ?.optJSONObject("pageProps")
            ?.optJSONObject("initialManga") ?: return null

        val title = initialManga.optString("name", null) ?: return null
        val novelId = initialManga.optString("id", null) ?: return null
        val cover = initialManga.optString("cover", null)?.let {
            when {
                it.startsWith("//") -> "https:$it"
                it.startsWith("http") -> it
                else -> it
            }
        }
        val summaryHtml = initialManga.optString("summary", null)
        val synopsis = summaryHtml?.let {
            try { org.jsoup.Jsoup.parse(it).body().text().trim() } catch (_: Throwable) { it }
        }
        val status = initialManga.optString("status", null)?.replaceFirstChar { it.uppercase() }

        val authorsArray = initialManga.optJSONArray("authors")
        val author = if (authorsArray != null) {
            (0 until authorsArray.length())
                .mapNotNull { authorsArray.optJSONObject(it)?.optString("name", null) }
                .joinToString(", ").takeIf { it.isNotBlank() }
        } else null

        val genresArray = initialManga.optJSONArray("genres")
        val tags = if (genresArray != null) {
            (0 until genresArray.length())
                .mapNotNull { genresArray.optJSONObject(it)?.optString("name", null) }
        } else emptyList()

        val chapters = loadChapters(novelId)

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = tags.ifEmpty { null }, status = status
        )
    }

    private suspend fun loadChapters(novelId: String): List<Chapter> {
        return try {
            val url = "$apiUrl/titles/$novelId/chapters"
            val json = JSONObject(get(url, jsonHeaders).text)
            val chaptersArray = json.optJSONObject("data")?.optJSONArray("chapters") ?: JSONArray()
            (0 until chaptersArray.length()).mapNotNull { i ->
                val obj = chaptersArray.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("name", null) ?: return@mapNotNull null
                val chUrl = obj.optString("url", null) ?: return@mapNotNull null
                val chFullUrl = when {
                    chUrl.startsWith("http") -> chUrl
                    else -> "$mainUrl/${chUrl.trimStart('/')}"
                }
                val date = obj.optString("updated_at", null)
                Chapter(name = name, url = chFullUrl, dateOfRelease = date)
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/${url.trimStart('/')}"
        val document = get(fullUrl).document

        val script = document.selectFirstOrNull("#__NEXT_DATA__")?.data()
        if (script != null) {
            try {
                val json = JSONObject(script)
                val content = json.optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("initialChapter")
                    ?.optString("content", null)
                if (!content.isNullOrBlank()) return content.trim()
            } catch (_: Throwable) {}
        }

        val contentEl = document.selectFirstOrNull(".chapter__content") ?: return null
        contentEl.select("#listen-chapter, #google_translate_element, script, style").remove()
        return contentEl.html().trim().takeIf { it.isNotBlank() }
    }
}
