package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NovelsOnlineProvider : MainProvider() {

    override val name = "NovelsOnline"
    override val mainUrl = "https://novelsonline.org"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=novelsonline.org&sz=64"
    override val hasMainPage = true

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Celebrity", "celebrity"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Josei", "josei"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shoujo Ai", "shoujo-ai"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Shounen Ai", "shounen-ai"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    override val orderBys = listOf(FilterOption("Popular", ""))

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("src") ?: imgElement.attrOrNull("data-src") ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null
        return if (rawSrc.startsWith("http")) rawSrc else "$mainUrl$rawSrc"
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "hiatus", "on hiatus" -> "On Hiatus"
            "dropped", "cancelled", "canceled" -> "Cancelled"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = cleaned.replace("Your browser does not support JavaScript!", "")
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun parseViewCount(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.replace(",", "").trim().uppercase()
        return when {
            cleaned.endsWith("K") -> cleaned.dropLast(1).toFloatOrNull()?.times(1_000)?.toInt()
            cleaned.endsWith("M") -> cleaned.dropLast(1).toFloatOrNull()?.times(1_000_000)?.toInt()
            else -> cleaned.toIntOrNull()
        }
    }

    private fun parseNovels(document: Document): List<Novel> {
        return document.select("div.top-novel-block").mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirstOrNull("div.top-novel-header > h2 > a")
            ?: element.selectFirstOrNull("h2 > a")
            ?: return null
        val name = titleElement.textOrNull()?.trim() ?: return null
        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return null
        val imgElement = element.selectFirstOrNull("div.top-novel-content > div.top-novel-cover > a > img")
            ?: element.selectFirstOrNull(".top-novel-cover img")
        val posterUrl = fixPosterUrl(imgElement)
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val url = if (tag.isNullOrBlank()) "$mainUrl/top-novel/$page"
        else "$mainUrl/category/$tag/$page"
        val document = get(url).document
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val response = post(url = "$mainUrl/sResults.php", data = mapOf("q" to query))
        val document = response.document
        return document.select("li").mapNotNull { element ->
            val linkElement = element.selectFirstOrNull("a") ?: return@mapNotNull null
            val name = element.textOrNull()?.trim() ?: return@mapNotNull null
            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href.removePrefix(mainUrl).removePrefix("/")) ?: return@mapNotNull null
            val posterUrl = fixPosterUrl(element.selectFirstOrNull("img"))
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val document = get(fullUrl).document
        val name = document.selectFirstOrNull("h1")?.textOrNull()?.trim() ?: "Unknown"
        val chapters = document.select("ul.chapter-chs > li > a").mapNotNull { el ->
            val chapterName = el.textOrNull()?.trim() ?: return@mapNotNull null
            val chapterHref = el.attrOrNull("href") ?: return@mapNotNull null
            val chapterUrl = fixUrl(chapterHref.removePrefix(mainUrl).removePrefix("/")) ?: return@mapNotNull null
            Chapter(name = chapterName, url = chapterUrl)
        }
        val metadata = extractMetadata(document)
        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters,
            author = metadata.author, posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis, tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating, status = metadata.status, views = metadata.views
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null,
        val synopsis: String? = null, val tags: List<String> = emptyList(),
        val rating: Int? = null, val status: String? = null, val views: Int? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        var author: String? = null
        var synopsis: String? = null
        var tags: List<String> = emptyList()
        var status: String? = null
        var rating: Int? = null
        var views: Int? = null

        document.select(".novel-detail-item").forEach { item ->
            val label = item.selectFirstOrNull("h6")?.textOrNull()?.trim()?.lowercase() ?: return@forEach
            val body = item.selectFirstOrNull(".novel-detail-body")
            when {
                label.contains("description") -> synopsis = body?.textOrNull()?.trim()
                label.contains("genre") -> tags = body?.select("li")
                    ?.mapNotNull { it.textOrNull()?.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                label.contains("author") -> author = body?.select("li")
                    ?.mapNotNull { it.textOrNull()?.trim() }
                    ?.filter { it.isNotBlank() && it != "N/A" }?.joinToString(", ")
                label.contains("status") -> status = parseStatus(body?.textOrNull()?.trim())
                label.contains("total views") -> views = parseViewCount(body?.textOrNull()?.trim())
                label.contains("rating") -> {
                    val ratingValue = body?.textOrNull()?.trim()?.toFloatOrNull()
                    rating = ratingValue?.let { (it / 10f * 1000f).toInt().coerceIn(0, 1000) }
                }
            }
        }

        val posterElement = document.selectFirstOrNull("div.novel-left > div.novel-cover > a > img")
            ?: document.selectFirstOrNull("div.novel-cover > a > img")
            ?: document.selectFirstOrNull(".novel-cover a > img")
        val posterUrl = fixPosterUrl(posterElement)

        return NovelMetadata(author, posterUrl, synopsis ?: "No Summary Found", tags, rating, status, views)
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val document = get(fullUrl).document
        val contentElement = document.selectFirstOrNull("#contentall") ?: return null
        contentElement.select(
            ".ads, .adsbygoogle, script, style, .ads-holder, .ads-middle, " +
                "[id*='ads'], [class*='ads'], .hidden, [style*='display:none'], " +
                "[style*='display: none'], iframe, .social-share, .chapter-nav, .pagination"
        ).remove()
        return cleanChapterHtml(contentElement.html())
    }
}
