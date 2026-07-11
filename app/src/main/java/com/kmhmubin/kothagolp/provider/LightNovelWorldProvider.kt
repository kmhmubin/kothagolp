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
import org.jsoup.nodes.Document

class LightNovelWorldProvider : MainProvider() {

    override val name = "Light Novel World"
    override val mainUrl = "https://lightnovelworld.org"
    override val iconRes = R.drawable.ic_provider_lightnovelworld
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Popular", "rank"),
        FilterOption("Latest Updates", "updates"),
        FilterOption("Newest Added", "new"),
        FilterOption("Top Rated", "rating")
    )

    override val tags = listOf(
        FilterOption("All", "all"),
        FilterOption("Ongoing", "ongoing"),
        FilterOption("Completed", "completed"),
        FilterOption("Hiatus", "hiatus")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "rank"
        val order = if (sort == "rank") "asc" else "desc"
        val status = tag?.takeIf { it.isNotBlank() } ?: "all"
        val url = "$mainUrl/advanced-search/?sort=$sort&order=$order&status=$status&page=$page"
        val document = get(url).document

        val novels = document.select(".recommendation-card").mapNotNull { card ->
            val link = card.selectFirstOrNull("a.card-cover-link")
                ?: card.selectFirstOrNull(".card-footer a") ?: return@mapNotNull null
            val title = card.selectFirstOrNull(".card-title")?.textOrNull()?.trim()
                ?: return@mapNotNull null
            val href = link.attrOrNull("href") ?: return@mapNotNull null
            val cover = card.selectFirstOrNull(".card-cover img")?.attrOrNull("src")
            Novel(
                name = title,
                url = fixUrl(href) ?: href,
                posterUrl = cover?.let { if (it.startsWith("http")) it else "$mainUrl$it" },
                apiName = this.name
            )
        }

        val totalCount = document.selectFirstOrNull(".results-count")?.textOrNull()
            ?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
        val hasNext = page * 24 < totalCount

        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$mainUrl/api/search/?q=$encoded&search_type=title"
        return try {
            val json = JSONObject(get(url).text)
            val novelsArray = json.optJSONArray("novels") ?: JSONArray()
            (0 until novelsArray.length()).mapNotNull { i ->
                val obj = novelsArray.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title", null)?.trim() ?: return@mapNotNull null
                val slug = obj.optString("slug", null)?.trim() ?: return@mapNotNull null
                val cover = obj.optString("cover_path", null)
                Novel(
                    name = title,
                    url = "$mainUrl/novel/$slug/",
                    posterUrl = cover?.let { if (it.startsWith("http")) it else "$mainUrl$it" },
                    apiName = this.name
                )
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document

        val title = document.selectFirstOrNull(".novel-title")?.textOrNull()?.trim()
            ?: return null
        val cover = document.selectFirstOrNull("img.novel-cover")?.attrOrNull("src")
            ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
        val author = document.selectFirstOrNull(".novel-author")?.textOrNull()
            ?.removePrefix("Author:")?.trim()
        val synopsis = document.selectFirstOrNull(".summary-content")?.textOrNull()?.trim()
        val status = document.selectFirstOrNull(".status-badge")?.textOrNull()?.trim()
        val genres = document.select(".novel-genres .genre-tag")
            .mapNotNull { it.textOrNull()?.trim() }
        val tagItems = document.select(".tags-container .tag-item")
            .mapNotNull { it.textOrNull()?.trim() }
        val allTags = (genres + tagItems).filter { it.isNotBlank() }.distinct()

        val chapters = loadChapters(fullUrl)

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = allTags.ifEmpty { null }, status = status
        )
    }

    private suspend fun loadChapters(novelUrl: String): List<Chapter> {
        val cleanUrl = novelUrl.trimEnd('/')
        val chaptersUrl = "$cleanUrl/chapters/"
        return try {
            val doc = get(chaptersUrl).document
            val totalPages = doc.select("#pageSelect option").size.coerceAtLeast(1)
            val chapters = mutableListOf<Chapter>()
            chapters.addAll(parseChapterPage(doc))
            for (page in 2..totalPages) {
                val pageDoc = get("$chaptersUrl?page=$page").document
                chapters.addAll(parseChapterPage(pageDoc))
            }
            chapters
        } catch (_: Throwable) { emptyList() }
    }

    private fun parseChapterPage(doc: Document): List<Chapter> {
        return doc.select(".chapter-card").mapNotNull { card ->
            val onclick = card.attrOrNull("onclick") ?: ""
            val href = Regex("""location\.href='([^']+)'""").find(onclick)?.groupValues?.get(1)
                ?: card.selectFirstOrNull("a")?.attrOrNull("href")
                ?: return@mapNotNull null
            val chapterUrl = if (href.startsWith("http")) href else "$mainUrl$href"
            val name = card.selectFirstOrNull(".chapter-title")?.textOrNull()?.trim()
                ?: "Chapter ${card.selectFirstOrNull(".chapter-number")?.textOrNull()?.trim().orEmpty()}"
            Chapter(name = name.takeIf { it.isNotBlank() } ?: return@mapNotNull null, url = chapterUrl)
        }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val doc = get(fullUrl).document
        val content = doc.selectFirstOrNull("#chapterText")
            ?: doc.selectFirstOrNull(".chapter-content")
            ?: return null
        content.select("script, style, ins, iframe, .chapter-ad-container, .ad-unit, .chapter-promo, [data-ad-position]").remove()
        return content.html().trim().takeIf { it.isNotBlank() }
    }
}
