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

class PawReadProvider : MainProvider() {

    override val name = "PawRead"
    override val mainUrl = "https://pawread.com"
    override val iconRes = R.drawable.ic_provider_pawread
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Time Updated", "update"),
        FilterOption("Time Posted", "post"),
        FilterOption("Most Clicked", "click")
    )

    override val tags = listOf(
        FilterOption("All", "All"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Action", "Action"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Romance", "Romance"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Horror", "Horror"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("School Life", "School+Life"),
        FilterOption("Martial Arts", "Martial+Arts"),
        FilterOption("History", "History"),
        FilterOption("Drama", "Drama")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "click"
        val genre = tag?.takeIf { it.isNotBlank() && it != "All" } ?: ""
        val genrePath = if (genre.isNotBlank()) genre else "All"
        val url = "$mainUrl/list/all-$genrePath/$sort/?page=$page"
        val document = get(url).document

        val novels = document.select(".list-comic-thumbnail").mapNotNull { card ->
            val anchor = card.selectFirstOrNull(".caption > h3 > a") ?: return@mapNotNull null
            val title = anchor.textOrNull()?.trim() ?: return@mapNotNull null
            val href = anchor.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = if (href.startsWith("http")) href else "$mainUrl$href"
            val cover = card.selectFirstOrNull(".image-link > img")?.attrOrNull("src")
                ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
            Novel(name = title, url = novelUrl, posterUrl = cover, apiName = this.name)
        }

        val hasNext = document.selectFirstOrNull(".pagination .next, a[rel=next]") != null
            || document.select(".list-comic-thumbnail").size >= 20

        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$mainUrl/search/?keywords=$encoded"
        return try {
            val document = get(url).document
            document.select(".list-comic-thumbnail").mapNotNull { card ->
                val anchor = card.selectFirstOrNull(".caption > h3 > a") ?: return@mapNotNull null
                val title = anchor.textOrNull()?.trim() ?: return@mapNotNull null
                val href = anchor.attrOrNull("href") ?: return@mapNotNull null
                val novelUrl = if (href.startsWith("http")) href else "$mainUrl$href"
                val cover = card.selectFirstOrNull(".image-link > img")?.attrOrNull("src")
                    ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                Novel(name = title, url = novelUrl, posterUrl = cover, apiName = this.name)
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document

        val title = document.selectFirstOrNull("h1.j_novel_title, .book-title h1, h1")
            ?.textOrNull()?.trim() ?: return null

        val cover = document.selectFirstOrNull("meta[property=\"og:image\"]")
            ?.attrOrNull("content")
            ?: document.selectFirstOrNull("#tab1_board .col-md-3 div[style*=\"background-image\"]")
                ?.attrOrNull("style")
                ?.let { Regex("""url\(([^)]+)\)""").find(it)?.groupValues?.get(1)?.trim('\'', '"') }
                ?.let { if (it.startsWith("//")) "https:$it" else if (it.startsWith("http")) it else "$mainUrl$it" }

        var author: String? = null
        var synopsis: String? = null
        var tags: List<String> = emptyList()
        val jsonLd = document.select("script[type=\"application/ld+json\"]")
            .mapNotNull { it.data() }
            .firstOrNull { it.contains("\"@type\":\"Book\"") }
        if (jsonLd != null) {
            try {
                val json = JSONObject(jsonLd)
                author = json.optJSONObject("author")?.optString("name", null)
                synopsis = json.optString("description", null)?.takeIf { it.isNotBlank() }
                val genreVal = json.opt("genre")
                tags = when (genreVal) {
                    is JSONArray -> (0 until genreVal.length()).mapNotNull { genreVal.optString(it).takeIf { s -> s.isNotBlank() } }
                    is String -> if (genreVal.isNotBlank()) listOf(genreVal) else emptyList()
                    else -> emptyList()
                }
            } catch (_: Throwable) {}
        }
        if (tags.isEmpty()) {
            tags = document.select("div.tags span").mapNotNull { it.textOrNull()?.trim()?.trimStart('#')?.trim() }.filter { it.isNotBlank() }
        }

        val status = document.selectFirstOrNull("#tab1_board span.label")?.textOrNull()?.trim()

        val chapters = loadChapters(fullUrl)

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = tags.ifEmpty { null }, status = status
        )
    }

    private suspend fun loadChapters(novelUrl: String): List<Chapter> {
        return try {
            val document = get(novelUrl).document
            val novelPath = novelUrl.trimEnd('/')
            document.select(".item-box").mapNotNull { item ->
                val onclick = item.attrOrNull("onclick") ?: ""
                val chapterId = Regex("""'(\d+)'""").find(onclick)?.groupValues?.get(1)
                    ?: Regex("""\b(\d+)\b""").find(onclick)?.groupValues?.get(1)
                    ?: return@mapNotNull null
                val chapterUrl = "$novelPath/$chapterId.html"
                val name = item.selectFirstOrNull(".chapter-name, .title, a, span")
                    ?.textOrNull()?.trim()
                    ?: "Chapter $chapterId"
                val date = item.selectFirstOrNull(".date, .time, .update-time")?.textOrNull()?.trim()
                Chapter(name = name, url = chapterUrl, dateOfRelease = date)
            }
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document
        val content = document.selectFirstOrNull("#chapter_item, .chapter-item, .chapter-content, div.main")
            ?: return null
        content.select("script, style, .ads, .adsbygoogle, [class*='ad-'], [id*='ad-']").remove()
        val html = content.html().trim()
        return html.takeIf { it.isNotBlank() }
    }
}
