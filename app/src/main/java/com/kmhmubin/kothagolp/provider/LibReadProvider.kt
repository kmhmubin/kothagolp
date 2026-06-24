package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.atomic.AtomicLong

class LibReadProvider : MainProvider() {

    override val name = "LibRead"
    override val mainUrl = "https://libread.com"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=libread.com&sz=64"
    override val hasMainPage = true

    private val searchInterval = 3400L
    private val lastSearchTime = AtomicLong(0)
    private val noPagesPaths = listOf("sort/most-popular")
    private val alertRegex = Regex("alert\\(['\"]?(.*?)['\"]?\\)")
    private val aidRegex = Regex("([0-9]+)s\\.jpg")

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "Action"),
        FilterOption("Adult", "Adult"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Eastern", "Eastern"),
        FilterOption("Ecchi", "Ecchi"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Game", "Game"),
        FilterOption("Gender Bender", "Gender+Bender"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Josei", "Josei"),
        FilterOption("Martial Arts", "Martial+Arts"),
        FilterOption("Mature", "Mature"),
        FilterOption("Mecha", "Mecha"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Reincarnation", "Reincarnation"),
        FilterOption("Romance", "Romance"),
        FilterOption("School Life", "School+Life"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Seinen", "Seinen"),
        FilterOption("Shoujo", "Shoujo"),
        FilterOption("Shounen Ai", "Shounen+Ai"),
        FilterOption("Shounen", "Shounen"),
        FilterOption("Slice of Life", "Slice+of+Life"),
        FilterOption("Smut", "Smut"),
        FilterOption("Sports", "Sports"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy"),
        FilterOption("Wuxia", "Wuxia"),
        FilterOption("Xianxia", "Xianxia"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Yaoi", "Yaoi")
    )

    override val orderBys = listOf(
        FilterOption("Latest Release", "sort/latest-release"),
        FilterOption("Chinese Novel", "sort/latest-release/chinese-novel"),
        FilterOption("Korean Novel", "sort/latest-release/korean-novel"),
        FilterOption("Japanese Novel", "sort/latest-release/japanese-novel"),
        FilterOption("English Novel", "sort/latest-release/english-novel"),
        FilterOption("Latest Novels", "sort/latest-novels"),
        FilterOption("Completed Novels", "sort/completed-novel"),
        FilterOption("Most Popular", "sort/most-popular")
    )

    private object Selectors {
        val novelContainers = listOf(
            "div.ul-list1.ul-list1-2.ss-custom > div.li-row",
            "div.archive div.li-row",
            "div.col-content div.li-row",
            "div.li-row"
        )
        val novelTitle = listOf("h3.tit > a", ".tit > a", "h3 > a")
        val chapterContent = listOf("div.txt", "#chr-content", "#chapter-content", ".chapter-content")
        val synopsis = listOf("div.inner", "div.desc-text", ".summary .content")
        val poster = listOf("div.pic > img", "div.m-imgtxt img", "div.books img")
        val chapterList = listOf("ul#idData > li > a", "#idData li a", ".list-chapter li a")
        val searchResults = listOf(
            "div.li-row > div.li > div.con", "div.archive div.con", "div.li-row div.con"
        )
        val searchTitle = listOf("div.txt > h3.tit > a", ".txt .tit > a", "h3.tit > a")
    }

    private fun Document.selectFirst(selectors: List<String>): Element? {
        for (s in selectors) { val e = this.selectFirstOrNull(s); if (e != null) return e }
        return null
    }
    private fun Element.selectFirst(selectors: List<String>): Element? {
        for (s in selectors) { val e = this.selectFirstOrNull(s); if (e != null) return e }
        return null
    }
    private fun Document.selectAny(selectors: List<String>): Elements {
        for (s in selectors) { val e = this.select(s); if (e.isNotEmpty()) return e }
        return Elements()
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
        cleaned = cleaned.replace("libread.com", "", ignoreCase = true)
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.selectAny(Selectors.novelContainers)
        return elements.mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst(Selectors.novelTitle) ?: return null
        val name = titleElement.attrOrNull("title")?.takeIf { it.isNotBlank() }
            ?: titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null
        val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return null
        val imgElement = element.selectFirstOrNull("img")
        val rawSrc = imgElement?.attrOrNull("data-src") ?: imgElement?.attrOrNull("src")
        val posterUrl = fixUrl(rawSrc)
        val latestChapter = element.select("div.item").getOrNull(2)?.selectFirstOrNull("div > a")?.textOrNull()
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, latestChapter = latestChapter, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val currentPath = orderBy ?: "sort/latest-release"
        if (page > 1 && noPagesPaths.any { currentPath.contains(it) } && tag.isNullOrEmpty()) {
            return MainPageResult(url = "", novels = emptyList())
        }
        val url = when {
            !tag.isNullOrEmpty() && tag != "All" -> "$mainUrl/genre/$tag/$page"
            else -> if (page > 1) "$mainUrl/$currentPath/$page" else "$mainUrl/$currentPath"
        }
        val document = get(url).document
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val now = System.currentTimeMillis()
        val lastSearch = lastSearchTime.get()
        if (lastSearch > 0 && (now - lastSearch) < searchInterval) {
            delay(searchInterval - (now - lastSearch))
        }
        lastSearchTime.set(System.currentTimeMillis())
        val url = "$mainUrl/search"
        val response = post(url = url, data = mapOf("searchkey" to query))
        val html = response.text
        val alertMatch = alertRegex.find(html)
        if (alertMatch != null) {
            throw Exception("Search blocked: ${alertMatch.groupValues.getOrNull(1) ?: "Search blocked"}")
        }
        val document = response.document
        val results = document.selectAny(Selectors.searchResults)
        return results.mapNotNull { element ->
            val titleElement = element.selectFirst(Selectors.searchTitle) ?: return@mapNotNull null
            val name = titleElement.attrOrNull("title")?.takeIf { it.isNotBlank() }
                ?: titleElement.textOrNull()?.trim()
            if (name.isNullOrBlank()) return@mapNotNull null
            val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull("div.pic > img") ?: element.selectFirstOrNull("img")
            val rawSrc = imgElement?.attrOrNull("data-src") ?: imgElement?.attrOrNull("src")
            val posterUrl = fixUrl(rawSrc)
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        val trimmedUrl = url.trim().trimEnd('/')
        val response = get(url)
        val document = response.document
        val html = response.text
        val name = document.selectFirst(listOf("h1.tit", "div.m-desc h1", "div.books h1"))
            ?.textOrNull()?.trim() ?: "Unknown"
        var chapters = loadChaptersViaApi(html, trimmedUrl)
        if (chapters.isEmpty()) chapters = loadChaptersFromHtml(document, trimmedUrl)
        val metadata = extractMetadata(document)
        return NovelDetails(
            url = url, name = name, chapters = chapters,
            author = metadata.author, posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis, tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating, peopleVoted = metadata.peopleVoted, status = metadata.status
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null, val synopsis: String? = null,
        val tags: List<String> = emptyList(), val rating: Int? = null,
        val peopleVoted: Int? = null, val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val author = document.selectFirstOrNull("span.glyphicon.glyphicon-user")
            ?.nextElementSibling()?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("ul.info li:contains(Author) a")?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("a[href*='/author/']")?.textOrNull()?.trim()
        val posterUrl = document.selectFirst(Selectors.poster)?.let { img ->
            val src = img.attrOrNull("data-src") ?: img.attrOrNull("src")
            fixUrl(src)
        }
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            element.select("br").append("\\n")
            element.select("p").prepend("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        }
        val tagsText = document.selectFirstOrNull("span.glyphicon.glyphicon-th-list")
            ?.nextElementSibling()?.textOrNull()
            ?: document.selectFirstOrNull("ul.info li:contains(Genre)")?.textOrNull()
        val tags = tagsText?.split(",")
            ?.map { it.trim() }?.filter { it.isNotBlank() && !it.contains("Genre", ignoreCase = true) }
            ?.distinct() ?: emptyList()
        val statusText = document.selectFirstOrNull("span.s1.s3 > a")?.textOrNull()
            ?: document.selectFirstOrNull("span.s1.s2 > a")?.textOrNull()
            ?: document.selectFirstOrNull("ul.info li:contains(Status) a")?.textOrNull()
        val status = parseStatus(statusText)
        val votesP = document.selectFirstOrNull("div.m-desc > div.score > p:nth-child(2)")
            ?: document.selectFirstOrNull("div.score p")
        val votesText = votesP?.textOrNull()
        var rating: Int? = null
        var peopleVoted: Int? = null
        if (votesText != null) {
            val ratingMatch = Regex("([0-9.]+)").find(votesText)
            val rawRating = ratingMatch?.groupValues?.getOrNull(1)?.toFloatOrNull()
            rating = rawRating?.let { value ->
                when {
                    votesText.contains("/10") -> (value / 10f * 1000f).toInt().coerceIn(0, 1000)
                    votesText.contains("/5") -> (value / 5f * 1000f).toInt().coerceIn(0, 1000)
                    value > 5f -> (value / 10f * 1000f).toInt().coerceIn(0, 1000)
                    else -> (value / 5f * 1000f).toInt().coerceIn(0, 1000)
                }
            }
            val voteMatch = Regex("\\(([0-9,]+)").find(votesText)
            peopleVoted = voteMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
        }
        return NovelMetadata(author, posterUrl, synopsis, tags, rating, peopleVoted, status)
    }

    private suspend fun loadChaptersViaApi(html: String, baseUrl: String): List<Chapter> {
        val aidMatch = aidRegex.find(html)
        val aid = aidMatch?.groupValues?.getOrNull(1) ?: return emptyList()
        val chapters = mutableListOf<Chapter>()
        try {
            val chapterResponse = post("$mainUrl/api/chapterlist.php", data = mapOf("aid" to aid))
            val cleanText = chapterResponse.text.replace("\\", "")
            val chapterDoc = Jsoup.parse(cleanText)
            val prefix = baseUrl.removeSuffix(".html")
            chapterDoc.select("option").forEach { option ->
                val value = option.attrOrNull("value") ?: return@forEach
                if (value.isBlank() || value == "#" || value == "0") return@forEach
                val lastPart = value.split("/").lastOrNull() ?: return@forEach
                val chapterUrl = "$prefix/$lastPart"
                val chapterName = option.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Chapter ${chapters.size + 1}"
                chapters.add(Chapter(name = chapterName, url = chapterUrl))
            }
        } catch (_: Exception) {}
        return chapters
    }

    private fun loadChaptersFromHtml(document: Document, baseUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        for (selector in Selectors.chapterList) {
            val chapterElements = document.select(selector)
            if (chapterElements.isNotEmpty()) {
                chapterElements.forEach { element ->
                    val href = element.attrOrNull("href") ?: return@forEach
                    if (href.isBlank() || href == "#") return@forEach
                    val chapterUrl = when {
                        href.startsWith("http") -> href
                        href.startsWith("/") -> "$mainUrl$href"
                        else -> "$baseUrl.removeSuffix(\".html\")/$href"
                    }
                    val chapterName = element.attrOrNull("title")?.takeIf { it.isNotBlank() }
                        ?: element.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Chapter ${chapters.size + 1}"
                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }
                if (chapters.isNotEmpty()) break
            }
        }
        return chapters
    }

    override suspend fun loadChapterContent(url: String): String? {
        val response = get(url)
        val cleanedText = response.text
            .replace("libread.com", "", ignoreCase = true)
            .replace("libread", "", ignoreCase = true)
        val document = Jsoup.parse(cleanedText)
        val contentElement = document.selectFirst(Selectors.chapterContent) ?: return null
        contentElement.select(
            ".unlock-buttons, .ads, .adsbygoogle, sub, script, style, " +
                ".ads-holder, .ads-middle, [id*='ads'], [class*='ads'], " +
                ".hidden, [style*='display:none'], [style*='display: none']"
        ).remove()
        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}
