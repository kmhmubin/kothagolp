package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.RatingUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Provider for FreeWebNovel.com
 */
class FreeWebNovelProvider : MainProvider() {

    override val name = "FreeWebNovel"
    override val mainUrl = "https://freewebnovel.com"
    override val hasMainPage = true
    override val hasReviews = false
    override val iconRes: Int = R.drawable.ic_provider_freewebnovel

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

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
        FilterOption("Shounen", "Shounen"),
        FilterOption("Shounen Ai", "Shounen+Ai"),
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
        FilterOption("Latest Release", "latest-release-novels"),
        FilterOption("Most Popular", "most-popular"),
        FilterOption("Chinese Novel", "latest-release-novels/chinese-novel"),
        FilterOption("Korean Novel", "latest-release-novels/korean-novel"),
        FilterOption("Japanese Novel", "latest-release-novels/japanese-novel"),
        FilterOption("English Novel", "latest-release-novels/english-novel")
    )

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun deSlash(url: String): String {
        return if (url.startsWith("/")) url.substring(1) else url
    }

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null

        val rawSrc = imgElement.attrOrNull("data-src")
            ?: imgElement.attrOrNull("src")
            ?: return null

        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null

        val cleanedSrc = deSlash(rawSrc)
        return if (cleanedSrc.startsWith("http")) {
            cleanedSrc
        } else {
            "$mainUrl/$cleanedSrc"
        }
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
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "freewebnovel")
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.select("div.ul-list1.ul-list1-2.ss-custom > div.li-row")
        return elements.mapNotNull { element ->
            parseNovelElement(element)
        }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst("h3.tit > a") ?: return null

        val name = titleElement.attrOrNull("title")
            ?: titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null

        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(deSlash(href)) ?: return null

        val imgElement = element.selectFirst("div.pic > a > img")
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
        val url = if (!tag.isNullOrEmpty()) {
            "$mainUrl/genres/$tag/$page"
        } else {
            val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "latest-release-novels"
            "$mainUrl/$sort/$page"
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
        val url = "$mainUrl/search/"

        val response = post(
            url = url,
            headers = mapOf(
                "Referer" to mainUrl,
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded"
            ),
            data = mapOf("searchkey" to query)
        )

        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        val elements = document.select("div.li-row")
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
        val name = document.selectFirst("h1.tit")?.textOrNull()?.trim()
            ?: return null

        // Extract metadata
        val metadata = extractMetadata(document)

        // Load chapters
        val chapters = loadChapters(document, novelPath)

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
        // Get poster
        val posterUrl = document.selectFirst("div.pic > img")?.let { imgElement ->
            fixPosterUrl(imgElement)
        }

        // Get synopsis
        val synopsis = document.selectFirst("div.inner")?.text()?.trim()
            ?: "No Summary Found"

        // Get author
        val author = document.selectFirst("span.glyphicon.glyphicon-user")
            ?.nextElementSibling()?.textOrNull()?.trim()

        // Get genres/tags
        val tags = document.selectFirst("span.glyphicon.glyphicon-th-list")
            ?.nextElementSiblings()?.getOrNull(0)
            ?.text()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        // Get status
        val statusElement = document.selectFirst("span.s1.s2, span.s1.s3")
        val status = statusElement?.selectFirst("a")?.textOrNull()?.let { parseStatus(it) }

        // Parse rating
        var rating: Int? = null
        var peopleVoted: Int? = null
        try {
            val ratingText = document.selectFirst("div.m-desc > div.score > p:nth-child(2)")
                ?.textOrNull()
            if (ratingText != null) {
                val ratingValue = ratingText.substringBefore("/").trim().toFloatOrNull()
                if (ratingValue != null) {
                    rating = RatingUtils.from5Stars(ratingValue)
                }

                // Extract people voted (e.g., "4.5/5 (234)" -> 234)
                val votedMatch = Regex("\\((\\d+)\\)").find(ratingText)
                peopleVoted = votedMatch?.groupValues?.getOrNull(1)
                    ?.filter { it.isDigit() }?.toIntOrNull()
            }
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

    private suspend fun loadChapters(document: Document, novelPath: String): List<Chapter> {
        // Try AJAX endpoint first
        val ajaxChapters = tryLoadChaptersViaAjax(document)
        if (ajaxChapters.isNotEmpty()) {
            return ajaxChapters
        }

        // Fallback to HTML parsing
        return loadChaptersFromHtml(document)
    }

    /**
     * Load chapters via AJAX API
     * POST /api/chapterlist.php with acode and aid parameters
     */
    private suspend fun tryLoadChaptersViaAjax(document: Document): List<Chapter> {
        try {
            val scriptText = document.select("script").joinToString("\n") { it.html() }

            // Extract aid (novel ID) from image URL pattern (e.g., "12345s.jpg")
            val aidMatch = Regex("(\\d+)s\\.jpg").find(scriptText)
            val aid = aidMatch?.groupValues?.getOrNull(1) ?: return emptyList()

            // Extract acode from canonical URL or r_url meta tag
            val acodeMatch = Regex("(?<=freewebnovel\\.com/)([^/\"]+)(?=/chapter)").find(scriptText)
            val acode = acodeMatch?.value ?: return emptyList()

            val ajaxUrl = "$mainUrl/api/chapterlist.php"
            val response = post(
                url = ajaxUrl,
                data = mapOf(
                    "acode" to acode,
                    "aid" to aid
                )
            )

            // Response is escaped HTML, clean it up
            val html = response.text.replace("""\\""", "")
            val parsed = Jsoup.parse(html)
            val options = parsed.select("option")

            val chapters = mutableListOf<Chapter>()
            var chapterNumber = 0

            for (option in options) {
                chapterNumber++
                val value = option.attrOrNull("value") ?: continue
                val chapterUrl = fixUrl(deSlash(value)) ?: continue

                val chapterName = option.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Chapter $chapterNumber"

                chapters.add(
                    Chapter(
                        name = chapterName,
                        url = chapterUrl,
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

    /**
     * Fallback: parse chapters from HTML
     */
    private fun loadChaptersFromHtml(document: Document): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val chapterElements = document.select("ul#idData li, ul.chapter-list li")

        var chapterNumber = 0
        for (element in chapterElements) {
            chapterNumber++
            val linkElement = element.selectFirst("a") ?: continue
            val href = linkElement.attrOrNull("href") ?: continue

            val chapterUrl = fixUrl(deSlash(href)) ?: continue

            val chapterName = linkElement.attrOrNull("title")
                ?: linkElement.textOrNull()?.trim()
                ?: "Chapter $chapterNumber"

            chapters.add(
                Chapter(
                    name = chapterName,
                    url = chapterUrl,
                    dateOfRelease = null
                )
            )
        }

        return chapters
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"

        val response = get(fullUrl)

        // Clean up the response text to remove FreeWebNovel ads/notices
        val cleanedHtml = response.text
            .replace("New novel chapters are published on Freewebnovel.com.", "")
            .replace("The source of this content is Freewebnᴏvel.com.", "")
            .replace("☞ We are moving Freewebnovel.com to Libread.com, Please visit libread.com for more chapters! ☜", "")

        val document = Jsoup.parse(cleanedHtml)

        if (document.title().contains("Cloudflare", ignoreCase = true)) {
            throw Exception("Cloudflare is blocking requests. Try again later.")
        }

        // Remove notice text
        document.select("div.txt > .notice-text").remove()

        val contentElement = document.selectFirst("div.txt") ?: return null

        // Remove ads and unwanted elements
        contentElement.select(
            ".ads, .adsbygoogle, script, style, " +
                    ".ads-holder, .ads-middle, [id*='ads'], [class*='ads']"
        ).remove()

        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}