package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.roundToInt

/**
 * Provider for AllNovel.org
 */
class AllNovelProvider : MainProvider() {

    override val name = "AllNovel"
    override val mainUrl = "https://allnovel.org"
    override val hasMainPage = true
    override val hasReviews = false
    override val iconRes: Int = R.drawable.ic_provider_allnovel

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", "All"),
        FilterOption("Action", "Action"),
        FilterOption("Adult", "Adult"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Ecchi", "Ecchi"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Gender Bender", "Gender Bender"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Josei", "Josei"),
        FilterOption("Game", "Game"),
        FilterOption("Martial Arts", "Martial Arts"),
        FilterOption("Mature", "Mature"),
        FilterOption("Mecha", "Mecha"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Romance", "Romance"),
        FilterOption("School Life", "School Life"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Seinen", "Seinen"),
        FilterOption("Shoujo", "Shoujo"),
        FilterOption("Shoujo Ai", "Shoujo Ai"),
        FilterOption("Shounen", "Shounen"),
        FilterOption("Shounen Ai", "Shounen Ai"),
        FilterOption("Slice of Life", "Slice of Life"),
        FilterOption("Smut", "Smut"),
        FilterOption("Sports", "Sports"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy"),
        FilterOption("Wuxia", "Wuxia"),
        FilterOption("Xianxia", "Xianxia"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Yaoi", "Yaoi"),
        FilterOption("Yuri", "Yuri"),
        FilterOption("Eastern", "Eastern"),
        FilterOption("Reincarnation", "Reincarnation"),
        FilterOption("Isekai", "Isekai"),
        FilterOption("LitRPG", "LitRPG"),
        FilterOption("Video Games", "Video Games"),
        FilterOption("Magical Realism", "Magical Realism")
    )

    override val orderBys = listOf(
        FilterOption("Hot Novel", "hot-novel"),
        FilterOption("Latest Release", "latest-release-novel"),
        FilterOption("Most Popular", "most-popular"),
        FilterOption("Completed Novel", "completed-novel")
    )

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun deSlash(url: String): String {
        return if (url.startsWith("/")) url.substring(1) else url
    }

    /**
     * Fix poster URL to prevent zoom on images
     * Replaces specific hash patterns used by AllNovel
     */
    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null

        val rawSrc = imgElement.attrOrNull("data-src")
            ?: imgElement.attrOrNull("src")
            ?: return null

        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null

        // Apply AllNovel's poster fix to prevent zoom
        val fixedSrc = rawSrc
            .replace("fc05345726d3e134d2f7187dc70f047b", "4d27e0af8cf6e971f7ee3c995fc55190")
            .replace("9798407846f8032e6a88fa71b2c62ce9", "9c3d392ccc7c95187a8c6e37c6bdac6f")

        val cleanedSrc = deSlash(fixedSrc)
        return if (cleanedSrc.startsWith("http")) {
            cleanedSrc
        } else {
            "$mainUrl/$cleanedSrc"
        }
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null

        return when (statusText.lowercase().trim()) {
            "ongoing", "en cours", "em andamento", "مستمرة" -> "Ongoing"
            "completed", "complété", "completo", "completado", "مكتملة" -> "Completed"
            "hiatus", "on hiatus", "en pause", "pausa", "pausado", "متوقفة" -> "On Hiatus"
            "dropped", "cancelled", "canceled" -> "Cancelled"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "allnovel")

        // Remove AllNovel specific ads and notices
        cleaned = cleaned.replace(
            Regex("<iframe.*?src=\"//ad\\.{0,2}-ads\\.com/.*?\".*?></iframe>"),
            ""
        )
        cleaned = cleaned.replace(
            "If you find any errors ( broken links, non-standard content, etc.. ), Please let us know < report chapter > so we can fix it as soon as possible.",
            ""
        )
        cleaned = cleaned.replace(
            "If you find any errors ( Ads popup, ads redirect, broken links, non-standard content, etc.. ), Please let us know < report chapter > so we can fix it as soon as possible.",
            ""
        )
        cleaned = cleaned.replace("[Updated from F r e e w e b n o v e l. c o m]", "")

        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")

        return cleaned.trim()
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.select("div.list > div.row")
        return elements.mapNotNull { element ->
            parseNovelElement(element)
        }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst("div > div > h3.truyen-title > a")
            ?: element.selectFirst("div > div > .novel-title > a")
            ?: return null

        val name = titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null

        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(deSlash(href)) ?: return null

        val imgElement = element.selectFirst("div > div > img")
        val posterUrl = fixPosterUrl(imgElement)

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            apiName = this.name
        )
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?,
        extraFilters: Map<String, String>
    ): MainPageResult {
        val url = if (!tag.isNullOrEmpty() && tag != "All") {
            "$mainUrl/genre/$tag?page=$page"
        } else {
            val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "hot-novel"
            "$mainUrl/$sort?page=$page"
        }

        val response = get(url)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        val novels = parseNovels(document)

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search?keyword=$encodedQuery"

        val response = get(url)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        val elements = document.select("#list-page > .archive > .list > .row")
        return elements.mapNotNull { element ->
            parseNovelElement(element)
        }
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val novelPath = deSlash(url.replace(mainUrl, ""))
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$novelPath"

        val response = get(fullUrl)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        // Get novel title
        val name = document.selectFirst("h3.title")?.textOrNull()?.trim()
            ?: return null

        // Extract metadata
        val metadata = extractMetadata(document)

        // Load chapters via AJAX
        val chapters = loadChapters(document)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating,
            peopleVoted = metadata.peopleVoted,
            status = metadata.status
        )
    }

    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val rating: Int? = null,
        val peopleVoted: Int? = null,
        val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val infoDivs = document.select("div.info > div").takeIf { it.isNotEmpty() }
            ?: document.select("ul.info > li")

        // Get author
        val author = infoDivs.find { it.text().contains("Author", ignoreCase = true) }
            ?.selectFirst("a")?.textOrNull()?.trim()

        // Get genres/tags
        val tags = infoDivs.find { it.text().contains("Genre", ignoreCase = true) }
            ?.select("a")
            ?.mapNotNull { it.textOrNull()?.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        // Get status
        val statusText = infoDivs.find { it.text().contains("Status", ignoreCase = true) }
            ?.selectFirst("a")?.textOrNull()
        val status = parseStatus(statusText)

        // Get poster
        val imgElement = document.selectFirst("div.book img")
        val posterUrl = fixPosterUrl(imgElement)
            ?: imgElement?.attrOrNull("src")?.let { src ->
                if (src.startsWith("http")) src else "$mainUrl/${deSlash(src)}"
            }

        // Get synopsis
        val synopsis = document.selectFirst("div.desc-text")?.text()?.trim()
            ?: "No Summary Found"

        // Parse rating
        var rating: Int? = null
        var peopleVoted: Int? = null
        try {
            val ratingText = document.selectFirst("div.small > em > strong:nth-child(1) > span")
                ?.textOrNull()
            val ratingValue = ratingText?.toFloatOrNull()
            if (ratingValue != null) {
                rating = (ratingValue * 100).roundToInt()
            }

            val votesText = document.selectFirst("div.small > em > strong:nth-child(3) > span")
                ?.textOrNull()
            peopleVoted = votesText?.toIntOrNull()
        } catch (e: Exception) {
            // No rating available
        }

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status
        )
    }

    // ================================================================
    // LOAD CHAPTERS
    // ================================================================

    private suspend fun loadChapters(document: Document): List<Chapter> {
        // Get novel ID from rating element
        val novelId = document.selectFirst("#rating")
            ?.attrOrNull("data-novel-id")
            ?: return emptyList()

        // Try AJAX endpoint first
        val ajaxChapters = tryLoadChaptersViaAjax(novelId)
        if (ajaxChapters.isNotEmpty()) {
            return ajaxChapters
        }

        // Fallback to HTML parsing (shouldn't happen but just in case)
        return emptyList()
    }

    /**
     * Load chapters via AJAX endpoint
     * GET /ajax-chapter-option?novelId={id}
     */
    private suspend fun tryLoadChaptersViaAjax(novelId: String): List<Chapter> {
        try {
            val ajaxUrl = "$mainUrl/ajax-chapter-option?novelId=$novelId"
            val response = get(ajaxUrl)

            val document = response.document

            // Try parsing as select options first
            var chapterElements = document.select("select > option")

            // If no select options, try list items
            if (chapterElements.isEmpty()) {
                chapterElements = document.select(".list-chapter > li > a")
            }

            val chapters = mutableListOf<Chapter>()
            var chapterNumber = 0

            for (element in chapterElements) {
                chapterNumber++

                // Get URL from value or href
                val chapterUrl = element.attrOrNull("value")
                    ?: element.attrOrNull("href")
                    ?: continue

                val url = fixUrl(deSlash(chapterUrl)) ?: continue

                // Get chapter name
                val chapterName = element.textOrNull()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Chapter $chapterNumber"

                // Extract chapter number from URL if not in name
                val extractedNumber = Regex("chapter-(\\d+)")
                    .find(chapterUrl)
                    ?.groupValues?.getOrNull(1)
                    ?.toIntOrNull()

                chapters.add(
                    Chapter(
                        name = chapterName,
                        url = url,
                        dateOfRelease = null
                    )
                )
            }

            return chapters
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"

        val response = get(fullUrl)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        // Try both possible content selectors
        val contentElement = document.selectFirst("#chapter-content")
            ?: document.selectFirst("#chr-content")
            ?: return null

        // Remove ads and unwanted elements
        contentElement.select(
            ".ads, .adsbygoogle, script, style, " +
                    ".ads-holder, .ads-middle, [id*='ads'], [class*='ads']"
        ).remove()

        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}