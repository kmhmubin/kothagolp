package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class LightNovelWorldProvider : MainProvider() {

    override val name = "Chikari"
    override val mainUrl = "https://chikari.moe"
    override val iconRes = R.drawable.ic_provider_lightnovelworld
    override val hasMainPage = true

    private val apiBase = "$mainUrl/api"
    private val jsonHeaders = mapOf("Accept" to "application/json")

    private val browsePageSize = 24
    private val chapterPageSize = 500

    override val orderBys = listOf(
        FilterOption("Popular", "popular"),
        FilterOption("Latest Updates", "updated"),
        FilterOption("Newest Added", "added"),
        FilterOption("Top Rated", "top_rated")
    )

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Ongoing", "releasing"),
        FilterOption("Completed", "completed"),
        FilterOption("Hiatus", "hiatus")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "updated"
        val status = tag?.takeIf { it.isNotBlank() }
        val genre = extraFilters["genre"]
        val offset = (page - 1) * browsePageSize
        val url = buildString {
            append("$apiBase/novels?medium=novel&sort=$sort&limit=$browsePageSize&offset=$offset")
            if (!status.isNullOrBlank()) append("&status=$status")
            if (!genre.isNullOrBlank()) append("&genre=$genre")
        }
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            val items = json.optJSONArray("items") ?: JSONArray()
            val total = json.optInt("total", 0)
            val novels = parseNovels(items)
            MainPageResult(url = url, novels = novels, hasNextPage = offset + browsePageSize < total)
        } catch (_: Throwable) {
            MainPageResult(url = url, novels = emptyList())
        }
    }

    private fun parseNovels(items: JSONArray): List<Novel> {
        val novels = mutableListOf<Novel>()
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val title = obj.optString("title", null) ?: continue
            val slug = obj.optString("slug", null) ?: continue
            val cover = obj.optString("cover_url", null)?.takeIf { it.isNotBlank() }
            novels.add(Novel(name = title, url = "$mainUrl/novels/$slug", posterUrl = cover, apiName = this.name))
        }
        return novels
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiBase/novels?medium=novel&q=$encoded&limit=40"
        return try {
            val json = JSONObject(get(url, jsonHeaders).text)
            parseNovels(json.optJSONArray("items") ?: JSONArray())
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? = coroutineScope {
        val slug = url.trimEnd('/').substringAfterLast("/")
        // Chapter list needs its own paginated fetch loop; run it alongside
        // the metadata call instead of waiting on it first.
        val chaptersDeferred = async { loadChapters(slug) }
        val meta = try {
            JSONObject(get("$apiBase/novels/$slug", jsonHeaders).text)
        } catch (_: Throwable) {
            chaptersDeferred.cancel()
            return@coroutineScope null
        }
        val title = meta.optString("title", null) ?: run {
            chaptersDeferred.cancel()
            return@coroutineScope null
        }
        val cover = meta.optString("cover_url", null)?.takeIf { it.isNotBlank() }
        val synopsis = meta.optString("description", null)?.trim()?.takeIf { it.isNotBlank() }
        val status = parseStatus(meta.optString("status", null))
        val genresArray = meta.optJSONArray("genres")
        val tagsList = if (genresArray != null) {
            (0 until genresArray.length()).mapNotNull { genresArray.optJSONObject(it)?.optString("name", null) }
        } else emptyList()
        val authorsArray = meta.optJSONArray("authors")
        val author = authorsArray?.optJSONObject(0)?.optString("name", null)
        val rating = if (meta.has("rating") && !meta.isNull("rating")) {
            (meta.optDouble("rating") * 100).roundToInt()
        } else null
        val peopleVoted = meta.optInt("rating_count", 0).takeIf { it > 0 }
        val chapters = chaptersDeferred.await()
        NovelDetails(
            url = "$mainUrl/novels/$slug", name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = tagsList.ifEmpty { null }, status = status,
            rating = rating, peopleVoted = peopleVoted
        )
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "releasing", "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "hiatus" -> "On Hiatus"
            "dropped", "cancelled", "canceled" -> "Cancelled"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private suspend fun loadChapters(slug: String): List<Chapter> {
        return try {
            val chapters = mutableListOf<Chapter>()
            var offset = 0
            var total = Int.MAX_VALUE
            while (offset < total) {
                val json = JSONObject(
                    get("$apiBase/novels/$slug/chapters?limit=$chapterPageSize&offset=$offset", jsonHeaders).text
                )
                total = json.optInt("total", 0)
                val items = json.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val number = obj.optDouble("number", Double.NaN)
                    if (number.isNaN()) continue
                    val numberText = formatChapterNumber(number)
                    val title = obj.optString("title", null)?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Chapter $numberText"
                    val date = obj.optString("created_at", null)
                    chapters.add(Chapter(name = title, url = "$mainUrl/novels/$slug~~$numberText", dateOfRelease = date))
                }
                offset += chapterPageSize
            }
            chapters.sortedBy { it.url.substringAfterLast("~~").toDoubleOrNull() ?: 0.0 }
        } catch (_: Throwable) { emptyList() }
    }

    private fun formatChapterNumber(number: Double): String =
        if (number == number.toLong().toDouble()) number.toLong().toString() else number.toString()

    override suspend fun loadChapterContent(url: String): String? {
        val parts = url.split("~~")
        val slug = parts.getOrNull(0)?.trimEnd('/')?.substringAfterLast("/") ?: return null
        val number = parts.getOrNull(1) ?: return null
        return try {
            val json = JSONObject(get("$apiBase/novels/$slug/chapters/$number/read", jsonHeaders).text)
            val body = json.optString("body", null) ?: return null
            formatBodyAsHtml(body)
        } catch (_: Throwable) { null }
    }

    private fun formatBodyAsHtml(body: String): String {
        return body.split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("") { paragraph ->
                val escaped = paragraph
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br/>")
                "<p>$escaped</p>"
            }
    }
}
