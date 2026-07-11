package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import kotlinx.coroutines.delay
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.atomic.AtomicLong

class NovelBinProvider : MainProvider() {

    override val name = "NovelBin"
    override val mainUrl = "https://novelbin.com"
    override val iconRes = R.drawable.ic_provider_novelbin
    override val hasMainPage = true

    private val searchInterval = 3400L
    private val lastSearchTime = AtomicLong(0)
    private val fullPosterRegex = Regex("/novel_[0-9]*_[0-9]*/")
    private val novelIdRegex = Regex("\\d+")
    private val alertRegex = Regex("alert\\(['\"]?(.*?)['\"]?\\)")

    override val tags = listOf(
        FilterOption("All", "All"),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
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
        FilterOption("Military", "military"),
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

    override val orderBys = listOf(
        FilterOption("Latest Release", "sort/latest"),
        FilterOption("Hot Novel", "sort/top-hot-novel"),
        FilterOption("Completed Novel", "sort/completed"),
        FilterOption("Most Popular", "sort/top-view-novel")
    )

    private object Selectors {
        val novelContainers = listOf(
            "div.archive div.list > div.row", "div.col-content div.list > div.row",
            "#list-page .archive .list .row", "div.list > div.row"
        )
        val novelTitle = listOf(
            "h3.novel-title > a", "h3.truyen-title > a", ".novel-title > a", ".truyen-title > a", "h3 > a"
        )
        val novelDetailTitle = listOf("h3.title", "div.books h3", "div.m-imgtxt h3", ".book-info h3")
        val chapterContent = listOf("#chapter-content", "#chr-content", ".txt", ".chapter-content", "#content")
        val chapterList = listOf("li[data-chapter-item] a", "select > option[value]", ".list-chapter > li > a", "ul.list-chapter li a", "#list-chapter a")
        val synopsis = listOf("div.desc-text", "div.inner", ".summary .content", "#editdescription")
        val poster = listOf("div.book > img", "div.books img", "div.m-imgtxt img", ".book-info img")
        val author = listOf(
            "ul.info > li:nth-child(1) > a", "ul.info-meta li:contains(Author) a",
            ".info li:contains(Author) a", "a[href*='/author/']"
        )
        val genres = listOf(
            "ul.info > li:nth-child(5) a", "ul.info-meta li:contains(Genre) a",
            ".info li:contains(Genre) a", "a[href*='/genre/']"
        )
        val status = listOf(
            "ul.info > li:nth-child(3) > a", "ul.info-meta li:contains(Status) a",
            ".info li:contains(Status) a"
        )
        val novelId = listOf("#rating[data-novel-id]", "[data-novel-id]")
        val ratingValue = listOf("div.small > em > strong:nth-child(1) > span", ".rating-value")
        val ratingCount = listOf("div.small > em > strong:nth-child(3) > span", ".rating-count")
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

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src")
            ?: imgElement.attrOrNull("data-cfsrc") ?: return null
        if (rawSrc.contains("loading") || rawSrc.contains("placeholder")) return null
        val cleanedSrc = rawSrc.replace(fullPosterRegex, "/novel/")
        return fixUrl(cleanedSrc)
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

    private fun extractNovelId(document: Document, url: String): String? {
        for (selector in Selectors.novelId) {
            val element = document.selectFirstOrNull(selector)
            val id = element?.attrOrNull("data-novel-id")
            if (!id.isNullOrBlank()) return id
        }
        return novelIdRegex.find(url)?.value
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
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
        val name = titleElement.textOrNull()?.trim() ?: return null
        val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return null
        val imgElement = element.selectFirstOrNull("img")
        val posterUrl = fixPosterUrl(imgElement)
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val url = when {
            orderBy.isNullOrEmpty() && !tag.isNullOrEmpty() && tag != "All" ->
                "$mainUrl/genre/$tag?page=$page"
            else -> {
                val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "sort/top-hot-novel"
                "$mainUrl/$sort?page=$page"
            }
        }
        val document = get(url).document
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val now = System.currentTimeMillis()
        val lastSearch = lastSearchTime.get()
        if (lastSearch > 0 && (now - lastSearch) < searchInterval) delay(searchInterval - (now - lastSearch))
        lastSearchTime.set(System.currentTimeMillis())
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search?keyword=$encodedQuery"
        val response = get(url)
        val alertMatch = alertRegex.find(response.text)
        if (alertMatch != null) throw Exception("Search blocked: ${alertMatch.groupValues.getOrNull(1)}")
        return parseNovels(response.document)
    }

    override suspend fun load(url: String): NovelDetails? {
        val response = get(url)
        val document = response.document
        val name = document.selectFirst(Selectors.novelDetailTitle)?.textOrNull()?.trim() ?: "Unknown"
        val dataNovelId = extractNovelId(document, url)
        val chapters = loadChaptersViaAjax(dataNovelId)
        val metadata = extractMetadata(document)
        return NovelDetails(
            url = url, name = name, chapters = chapters, author = metadata.author,
            posterUrl = metadata.posterUrl, synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null }, rating = metadata.rating,
            peopleVoted = metadata.peopleVoted, status = metadata.status
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null, val synopsis: String? = null,
        val tags: List<String> = emptyList(), val rating: Int? = null,
        val peopleVoted: Int? = null, val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val author = document.selectFirst(Selectors.author)?.textOrNull()?.trim()
        val posterUrl = document.selectFirst(Selectors.poster)?.let { fixPosterUrl(it) }
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            element.select("br").append("\\n")
            element.select("p").prepend("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        }
        val tags = document.selectAny(Selectors.genres)
            .mapNotNull { it.textOrNull()?.trim() }.filter { it.isNotBlank() }.distinct()
        val status = document.selectFirst(Selectors.status)?.textOrNull()?.let { parseStatus(it) }
        val ratingText = document.selectFirst(Selectors.ratingValue)?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let { (it / 10f * 1000f).toInt().coerceIn(0, 1000) }
        val votedText = document.selectFirst(Selectors.ratingCount)?.textOrNull()
        val peopleVoted = votedText?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        return NovelMetadata(author, posterUrl, synopsis, tags, rating, peopleVoted, status)
    }

    private suspend fun loadChaptersViaAjax(novelId: String?): List<Chapter> {
        if (novelId.isNullOrBlank()) return emptyList()
        val chapters = mutableListOf<Chapter>()
        try {
            val ajaxUrl = "$mainUrl/ajax/chapter-archive?novelId=$novelId"
            val chapterResponse = get(ajaxUrl)
            val chapterDoc = chapterResponse.document
            for (selector in Selectors.chapterList) {
                val chapterElements = chapterDoc.select(selector)
                if (chapterElements.isNotEmpty()) {
                    chapterElements.forEach { element ->
                        val chapterUrl = element.attrOrNull("value") ?: element.attrOrNull("href") ?: return@forEach
                        if (chapterUrl.isBlank() || chapterUrl == "#" || chapterUrl == "0") return@forEach
                        val fixedUrl = fixUrl(chapterUrl) ?: return@forEach
                        val chapterName = element.attrOrNull("title")?.takeIf { it.isNotBlank() }
                            ?: element.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                            ?: "Chapter ${chapters.size + 1}"
                        chapters.add(Chapter(name = chapterName, url = fixedUrl))
                    }
                    if (chapters.isNotEmpty()) break
                }
            }
        } catch (_: Exception) {}
        return chapters
    }

    override suspend fun loadChapterContent(url: String): String? {
        val response = get(url)
        val document = response.document
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
