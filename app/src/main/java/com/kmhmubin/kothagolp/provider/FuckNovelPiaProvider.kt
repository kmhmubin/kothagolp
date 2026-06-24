package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONObject

class FuckNovelPiaProvider : MainProvider() {

    override val name = "FuckNovelPia"
    override val mainUrl = "https://fucknovelpia.com"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=fucknovelpia.com&sz=64"
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Newest", "newest"),
        FilterOption("Popular", "popular"),
        FilterOption("Oldest", "oldest")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "newest"
        val url = "$mainUrl/index.php?sort=$sort&page=$page"
        return try {
            val document = get(url).document
            val novels = document.select("div.grid div.card").mapNotNull { card ->
                val link = card.selectFirstOrNull("a.card-link") ?: return@mapNotNull null
                val href = link.attrOrNull("href") ?: return@mapNotNull null
                val novelUrl = if (href.startsWith("http")) href else "$mainUrl$href"
                val title = card.selectFirstOrNull("strong.book-title")?.textOrNull()?.trim()
                    ?: return@mapNotNull null
                val cover = card.selectFirstOrNull("div.cover img")?.attrOrNull("src")
                    ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                Novel(name = title, url = novelUrl, posterUrl = cover, apiName = this.name)
            }
            val hasNext = document.selectFirstOrNull("a[href*='page=${page + 1}']") != null
                || document.select("div.grid div.card").size >= 20
            MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
        } catch (_: Throwable) { MainPageResult(url = url, novels = emptyList()) }
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$mainUrl/search.php?q=$encoded"
        return try {
            val document = get(url).document
            document.select("article.card-book").mapNotNull { card ->
                val link = card.selectFirstOrNull("a") ?: return@mapNotNull null
                val href = link.attrOrNull("href") ?: return@mapNotNull null
                val novelUrl = if (href.startsWith("http")) href else "$mainUrl$href"
                val title = card.selectFirstOrNull("div.info div.title")?.textOrNull()?.trim()
                    ?: card.selectFirstOrNull("div.info strong, div.title")?.textOrNull()?.trim()
                    ?: return@mapNotNull null
                val cover = card.selectFirstOrNull("div.cover img")?.attrOrNull("src")
                    ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                Novel(name = title, url = novelUrl, posterUrl = cover, apiName = this.name)
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document

        val title = document.selectFirstOrNull("h1.hero-title")?.textOrNull()?.trim()
            ?: return null

        // LD+JSON for cover, author, synopsis, genres
        var cover: String? = null
        var author: String? = null
        var synopsis: String? = null
        var genres: List<String> = emptyList()

        val ldScript = document.select("script[type='application/ld+json']")
            .mapNotNull { it.data() }
            .firstOrNull { it.contains("\"@type\":\"Book\"") }
        if (ldScript != null) {
            try {
                val json = JSONObject(ldScript)
                cover = json.optString("image", null)?.takeIf { it.isNotBlank() }
                synopsis = json.optString("description", null)?.takeIf { it.isNotBlank() }
                author = json.optJSONObject("author")?.optString("name", null)?.takeIf { it.isNotBlank() }
                val genreRaw = json.opt("genre")
                genres = when (genreRaw) {
                    is org.json.JSONArray -> (0 until genreRaw.length())
                        .mapNotNull { genreRaw.optString(it).takeIf { s -> s.isNotBlank() } }
                    is String -> if (genreRaw.isNotBlank()) listOf(genreRaw) else emptyList()
                    else -> emptyList()
                }
            } catch (_: Throwable) {}
        }
        if (cover == null) {
            cover = document.selectFirstOrNull("meta[property='og:image']")?.attrOrNull("content")
        }
        if (synopsis == null) {
            synopsis = document.selectFirstOrNull("p.hero-summary")?.textOrNull()?.trim()
        }

        val status = document.select("div.stat-card").firstOrNull { card ->
            card.selectFirstOrNull("span.stat-label")?.textOrNull()?.trim() == "Status"
        }?.selectFirstOrNull("span.stat-value")?.textOrNull()?.trim()

        // Tags from tag-pill links
        val tags = document.select("div.tags a.tag-pill").mapNotNull { it.textOrNull()?.trim() }

        // Combine genres + tags, deduplicate
        val allTags = (genres + tags).distinctBy { it.lowercase() }

        val chapters = document.select("ul.chapter-list li a").mapNotNull { a ->
            val href = a.attrOrNull("href") ?: return@mapNotNull null
            val chUrl = if (href.startsWith("http")) href else "$mainUrl$href"
            val chTitle = a.selectFirstOrNull("span.chapter-item-main")?.textOrNull()?.trim()
                ?: return@mapNotNull null
            Chapter(name = chTitle, url = chUrl)
        }

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = allTags.ifEmpty { null }, status = status
        )
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document
        val reader = document.selectFirstOrNull("div.reader") ?: return null
        reader.select("script, style, .reader-nav, .reader-actions").remove()
        val html = reader.html().trim()
        return html.takeIf { it.isNotBlank() }
    }
}
