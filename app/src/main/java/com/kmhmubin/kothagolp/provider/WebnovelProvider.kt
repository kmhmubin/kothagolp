package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterGroup
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.domain.model.RepliesResult
import com.kmhmubin.kothagolp.domain.model.UserReview
import com.kmhmubin.kothagolp.util.HtmlUtils
import com.kmhmubin.kothagolp.util.RatingUtils
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class WebnovelProvider : MainProvider() {

    override val name = "Webnovel"
    override val mainUrl = "https://www.webnovel.com"
    override val hasMainPage = true
    override val hasReviews = true
    override val iconRes: Int = R.drawable.ic_provider_webnovel
    override val ratingScale: RatingScale = RatingScale.FIVE_STAR

    private val csrfToken = AtomicReference<String?>(null)
    private val bookIdCache = ConcurrentHashMap<String, String>()

    companion object {
        private const val SCORE_MULTIPLIER = 200

        private object Endpoints {
            const val STORIES = "/stories"
            const val SEARCH = "/search"
            const val REVIEW_DETAIL = "/go/pcm/bookReview/detail"
        }

        private object Selectors {
            const val REVIEW_CONTAINER = ".j_pageReviewList"
            const val AVATAR = "a.g_avatar img"
            const val USERNAME = ".m-comment-hd-mn a.c_l"
            const val USER_LEVEL = ".g_lv"
            const val USER_BADGE = ".m-comment-hd-mn img[alt=Badge]"
            const val STAR_CONTAINER = ".g_star"
            const val CONTENT = ".j_book_review_content"
            const val SPOILER_CONTAINER = ".m-comment-spoiler"
            const val TIME = ".m-comment-ft strong.fl"
            const val LIKE_COUNT = ".j_like_num"
            const val REPLY_BUTTON = ".m-comment-reply-btn"
            const val REVIEW_IMAGE = ".m-comment-img img"
        }

        private object NovelSelectors {
            const val CATEGORY_CONTAINER = ".j_category_wrapper li"
            const val CATEGORY_THUMB = ".g_thumb"
            const val CATEGORY_COVER = ".g_thumb > img"
            const val SEARCH_CONTAINER = ".j_list_container li"
            const val SEARCH_THUMB = ".g_thumb"
            const val SEARCH_COVER = ".g_thumb > img"
            const val DETAIL_COVER = ".g_thumb > img"
            val DETAIL_TITLE = listOf(".g_thumb > img", ".det-hd-detail h2", "div.g_col h2")
            const val DETAIL_GENRES = ".det-hd-detail > .det-hd-tag"
            const val DETAIL_TAGS = ".m-tags .m-tag a"
            const val DETAIL_SYNOPSIS = ".j_synopsis > p"
            const val DETAIL_AUTHOR_LABEL = ".det-info .c_s"
            const val DETAIL_AUTHOR_ALT = "p.ell a.c_primary"
            const val DETAIL_STATUS = ".det-hd-detail svg[title=Status]"
            const val DETAIL_RATING = ".g_star_num small"
            const val RELATED_CONTAINER = "ul.j_books_you_also_like li"
            const val RELATED_LINK = "a.m-book-title"
            const val RELATED_THUMB = ".g_thumb"
            const val RELATED_COVER = ".g_thumb img"
            const val RELATED_TITLE = "a.m-book-title h3"
            const val RELATED_RATING = ".g_star_num small"
            const val VOLUME_CONTAINER = ".volume-item"
            const val VOLUME_TITLE = "h4"
            const val CHAPTER_ITEM = "li"
            const val CHAPTER_LINK = "a"
            const val CHAPTER_LOCKED = "svg"
            const val CHAPTER_TITLE = ".cha-tit"
            val CHAPTER_CONTENT = listOf(".cha-words", ".cha-content", "div.cha-content p")
            const val CHAPTER_PARAGRAPH = ".cha-paragraph p"
            const val CHAPTER_COMMENTS = ".para-comment"
            const val CATALOG_FALLBACK = ".j_catalog_list a"
        }
    }

    private val customHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to mainUrl
    )

    private fun getAjaxHeaders(): Map<String, String> = customHeaders + mapOf(
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "X-Requested-With" to "XMLHttpRequest"
    )

    // ================================================================
    // ENHANCED FILTER OPTIONS WITH FANFICTION
    // ================================================================

    override val tags = listOf(
        // Male genres with category codes
        FilterOption("All Male", "male:all"),
        FilterOption("Action (Male)", "male:novel-action-male"),
        FilterOption("ACG (Male)", "male:novel-acg-male"),
        FilterOption("Eastern (Male)", "male:novel-eastern-male"),
        FilterOption("Fantasy (Male)", "male:novel-fantasy-male"),
        FilterOption("Games (Male)", "male:novel-games-male"),
        FilterOption("History (Male)", "male:novel-history-male"),
        FilterOption("Horror (Male)", "male:novel-horror-male"),
        FilterOption("Realistic (Male)", "male:novel-realistic-male"),
        FilterOption("Sci-fi (Male)", "male:novel-scifi-male"),
        FilterOption("Sports (Male)", "male:novel-sports-male"),
        FilterOption("Urban (Male)", "male:novel-urban-male"),
        FilterOption("War (Male)", "male:novel-war-male"),

        // Female genres with category codes
        FilterOption("All Female", "female:all"),
        FilterOption("Fantasy (Female)", "female:novel-fantasy-female"),
        FilterOption("General (Female)", "female:novel-general-female"),
        FilterOption("History (Female)", "female:novel-history-female"),
        FilterOption("LGBT+ (Female)", "female:novel-lgbt-female"),
        FilterOption("Sci-fi (Female)", "female:novel-scifi-female"),
        FilterOption("Teen (Female)", "female:novel-teen-female"),
        FilterOption("Urban (Female)", "female:novel-urban-female"),

        // Fanfiction categories (based on actual HTML structure)
        FilterOption("All Fanfiction", "fanfic:fanfic"),
        FilterOption("Anime & Comics (FF)", "fanfic:fanfic-anime-comics"),
        FilterOption("Video Games (FF)", "fanfic:fanfic-video-games"),
        FilterOption("TV (FF)", "fanfic:fanfic-tv"),
        FilterOption("Movies (FF)", "fanfic:fanfic-movies"),
        FilterOption("Book & Literature (FF)", "fanfic:fanfic-book-literature"),
        FilterOption("Celebrities (FF)", "fanfic:fanfic-celebrities"),
        FilterOption("Music & Bands (FF)", "fanfic:fanfic-music-bands"),
        FilterOption("Theater (FF)", "fanfic:fanfic-theater"),
        FilterOption("Others (FF)", "fanfic:fanfic-others"),
    )

    override val orderBys = listOf(
        FilterOption("Popular", "1"),
        FilterOption("Recommended", "2"),
        FilterOption("Most Collections", "3"),
        FilterOption("Rating", "4"),
        FilterOption("Time Updated", "5"),
    )

    override val extraFilterGroups = listOf(
        FilterGroup(
            key = "status",
            label = "Status",
            options = listOf(
                FilterOption("All Status", "0"),
                FilterOption("Ongoing", "1"),
                FilterOption("Completed", "2")
            ),
            defaultValue = "0"
        ),
        FilterGroup(
            key = "type",
            label = "Content Type",
            options = listOf(
                FilterOption("All Types", "0"),
                FilterOption("Translated", "1"),
                FilterOption("Original", "2"),
                FilterOption("MTL", "3")
            ),
            defaultValue = "0"
        )
    )

    // ================================================================
    // URL BUILDING & FILTER PARSING
    // ================================================================

    private data class FilterParams(
        val gender: String? = null,
        val genreSlug: String? = null,
        val isFanfic: Boolean = false
    )

    /**
     * Parses tag filter format: "gender:slug" or "fanfic:slug"
     * Examples:
     * - "male:all" → gender=1, genreSlug=null
     * - "male:novel-action-male" → gender=1, genreSlug=novel-action-male
     * - "fanfic:fanfic" → isFanfic=true, genreSlug=fanfic
     * - "fanfic:fanfic-anime-comics" → isFanfic=true, genreSlug=fanfic-anime-comics
     */
    private fun parseTagFilter(tag: String?): FilterParams {
        if (tag.isNullOrBlank()) return FilterParams()

        val parts = tag.split(":", limit = 2)
        if (parts.size != 2) return FilterParams()

        // Check for fanfiction
        if (parts[0] == "fanfic") {
            return FilterParams(isFanfic = true, genreSlug = parts[1])
        }

        // Check for male/female
        val gender = when (parts[0]) {
            "male" -> "1"
            "female" -> "2"
            else -> null
        }

        val slug = if (parts[1] == "all") null else parts[1]

        return FilterParams(gender = gender, genreSlug = slug)
    }

    /**
     * Builds browse URL with all filter parameters
     */
    private fun buildBrowseUrl(
        page: Int,
        orderBy: String? = null,
        tag: String? = null,
        status: String = "0",
        type: String = "0"
    ): String {
        val filter = parseTagFilter(tag)
        val sort = orderBy?.takeUnless { it.isEmpty() } ?: "1"

        // Determine base path
        val basePath = when {
            // Fanfiction mode - use /stories/fanfic or /stories/fanfic-*
            filter.isFanfic -> "${Endpoints.STORIES}/${filter.genreSlug}"

            // Specific genre URL (e.g., /stories/novel-action-male)
            filter.genreSlug != null -> "${Endpoints.STORIES}/${filter.genreSlug}"

            // Generic novel listing
            else -> "${Endpoints.STORIES}/novel"
        }

        // Build query parameters
        val params = buildList {
            // Gender parameter (only for generic novel path, not for fanfic or specific genres)
            if (!filter.isFanfic && filter.genreSlug == null && filter.gender != null) {
                add("gender=${filter.gender}")
            }

            add("orderBy=$sort")
            add("bookStatus=$status")

            // Content type handling (skip for fanfiction)
            if (!filter.isFanfic) {
                when (type) {
                    "3" -> {
                        add("translateMode=3")
                        add("sourceType=1")
                    }
                    "0" -> {}
                    else -> add("sourceType=$type")
                }
            }

            add("pageIndex=$page")
        }

        return "$mainUrl$basePath?${params.joinToString("&")}"
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun Document.selectFirst(selectors: List<String>): Element? {
        for (selector in selectors) {
            selectFirstOrNull(selector)?.let { return it }
        }
        return null
    }

    private fun Element.selectFirst(selectors: List<String>): Element? {
        for (selector in selectors) {
            selectFirstOrNull(selector)?.let { return it }
        }
        return null
    }

    private fun Document.selectAny(selectors: List<String>): Elements {
        for (selector in selectors) {
            val elements = select(selector)
            if (elements.isNotEmpty()) return elements
        }
        return Elements()
    }

    private fun fixCoverUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http") -> url
            url.startsWith("/") -> "$mainUrl$url"
            else -> url
        }
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when {
            statusText.contains("Ongoing", ignoreCase = true) -> "Ongoing"
            statusText.contains("Completed", ignoreCase = true) -> "Completed"
            statusText.contains("Hiatus", ignoreCase = true) -> "On Hiatus"
            else -> statusText.trim()
        }
    }

    private fun extractBookId(url: String): String? {
        bookIdCache[url]?.let { return it }

        val regex = Regex("_(\\d{10,})")
        val match = regex.find(url)
        val bookId = match?.groupValues?.getOrNull(1)

        if (bookId != null) {
            bookIdCache[url] = bookId
        }
        return bookId
    }

    private fun extractCsrfToken(document: Document): String? {
        document.selectFirstOrNull("meta[name=csrf-token]")?.attrOrNull("content")?.let {
            csrfToken.set(it)
            return it
        }

        val scripts = document.select("script").map { it.html() }
        for (script in scripts) {
            val tokenMatch = Regex("_csrfToken[\"']?\\s*[:=]\\s*[\"']([a-f0-9-]+)[\"']").find(script)
            tokenMatch?.groupValues?.getOrNull(1)?.let {
                csrfToken.set(it)
                return it
            }
        }

        if (csrfToken.get() == null) {
            csrfToken.set(java.util.UUID.randomUUID().toString())
        }
        return csrfToken.get()
    }

    private fun generateCsrfToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        csrfToken.set(token)
        return token
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = cleaned.replace(Regex("<pirate>.*?</pirate>", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(
            Regex("Find authorized novels in Webnovel.*?for visiting\\.",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            ""
        )
        cleaned = HtmlUtils.cleanChapterContent(cleaned, "webnovel")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun String.extractUserId(): String? {
        return split("/").lastOrNull()?.takeIf { it.isNotBlank() }
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseCategoryNovels(document: Document): List<Novel> {
        return document.select(NovelSelectors.CATEGORY_CONTAINER).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(NovelSelectors.CATEGORY_THUMB) ?: return@mapNotNull null
            val name = thumb.attrOrNull("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            val imgElement = element.selectFirstOrNull(NovelSelectors.CATEGORY_COVER)
            val rawCover = imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)

            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    private fun parseSearchNovels(document: Document): List<Novel> {
        return document.select(NovelSelectors.SEARCH_CONTAINER).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(NovelSelectors.SEARCH_THUMB) ?: return@mapNotNull null
            val name = thumb.attrOrNull("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            val imgElement = element.selectFirstOrNull(NovelSelectors.SEARCH_COVER)
            val rawCover = imgElement?.attrOrNull("src") ?: imgElement?.attrOrNull("data-original")
            val posterUrl = fixCoverUrl(rawCover)

            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    private fun parseRelatedNovels(document: Document): List<Novel> {
        return document.select(NovelSelectors.RELATED_CONTAINER).mapNotNull { item ->
            val linkElement = item.selectFirstOrNull(NovelSelectors.RELATED_LINK)
                ?: item.selectFirstOrNull(NovelSelectors.RELATED_THUMB)
                ?: return@mapNotNull null

            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null

            val title = linkElement.attrOrNull("title")
                ?: item.selectFirstOrNull(NovelSelectors.RELATED_TITLE)?.text()?.trim()
                ?: item.selectFirstOrNull("h3")?.text()?.trim()
                ?: return@mapNotNull null

            val imgElement = item.selectFirstOrNull(NovelSelectors.RELATED_COVER)
            val rawCover = imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src")
            val posterUrl = fixCoverUrl(rawCover)

            val ratingText = item.selectFirstOrNull(NovelSelectors.RELATED_RATING)?.text()
            val rating = ratingText?.toFloatOrNull()?.let { RatingUtils.from5Stars(it) }

            Novel(name = title, url = novelUrl, posterUrl = posterUrl, rating = rating, apiName = this.name)
        }
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
        val status = extraFilters["status"] ?: "0"
        val type = extraFilters["type"] ?: "0"

        val url = buildBrowseUrl(
            page = page,
            orderBy = orderBy,
            tag = tag,
            status = status,
            type = type
        )

        val response = get(url, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected.")
        }

        extractCsrfToken(document)

        // Fanfiction and regular categories both use .j_category_wrapper
        val novels = parseCategoryNovels(document)

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")

        val url = "$mainUrl${Endpoints.SEARCH}?keywords=$encodedQuery&pageIndex=1"

        val response = get(url, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected.")
        }

        extractCsrfToken(document)
        return parseSearchNovels(document)
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected.")
        }

        extractCsrfToken(document)

        val bookId = extractBookId(fullUrl)
        bookId?.let { bookIdCache[fullUrl] = it }

        val name = document.selectFirstOrNull(NovelSelectors.DETAIL_COVER)?.attrOrNull("alt")
            ?: document.selectFirst(NovelSelectors.DETAIL_TITLE)?.text()?.trim()
            ?: "Unknown"

        val catalogUrl = fullUrl.trimEnd('/') + "/catalog"
        val chapters = loadChaptersFromCatalog(catalogUrl)
        val metadata = extractMetadata(document)
        val relatedNovels = parseRelatedNovels(document)

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
            status = metadata.status,
            relatedNovels = relatedNovels.ifEmpty { null }
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
        val coverElement = document.selectFirstOrNull(NovelSelectors.DETAIL_COVER)
        val rawCover = coverElement?.attrOrNull("src") ?: coverElement?.attrOrNull("data-original")
        val posterUrl = fixCoverUrl(rawCover)

        val synopsis = document.selectFirstOrNull(NovelSelectors.DETAIL_SYNOPSIS)?.let { element ->
            element.select("br").append("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        } ?: "No Summary Found"

        val genresText = document.selectFirstOrNull(NovelSelectors.DETAIL_GENRES)?.attrOrNull("title")
        val genreTags = genresText?.split(",")
            ?.mapNotNull { it.trim().takeIf { tag -> tag.isNotBlank() } }
            ?: emptyList()

        val contentTags = document.select(NovelSelectors.DETAIL_TAGS).mapNotNull { el ->
            el.attrOrNull("title")
                ?.replace("Stories", "", ignoreCase = true)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: el.text()
                    ?.replace("#", "")
                    ?.trim()
                    ?.replaceFirstChar { it.uppercase() }
                    ?.takeIf { it.isNotBlank() }
        }

        val tags = (genreTags + contentTags).distinct()

        var author: String? = null
        document.select(NovelSelectors.DETAIL_AUTHOR_LABEL).forEach { element ->
            if (element.text().trim() == "Author:") {
                author = element.nextElementSibling()?.text()?.trim()
                return@forEach
            }
        }
        if (author.isNullOrBlank()) {
            author = document.selectFirstOrNull(NovelSelectors.DETAIL_AUTHOR_ALT)?.text()?.trim()
        }

        var statusText: String? = null
        document.select(NovelSelectors.DETAIL_STATUS).forEach { element ->
            if (element.attrOrNull("title") == "Status") {
                statusText = element.nextElementSibling()?.text()?.trim()
                return@forEach
            }
        }
        val status = parseStatus(statusText)

        val ratingText = document.selectFirstOrNull(NovelSelectors.DETAIL_RATING)?.text()
            ?: document.selectFirstOrNull("[class*='score']")?.text()
        val rating = ratingText?.toFloatOrNull()?.let { RatingUtils.from5Stars(it) }

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            status = status
        )
    }

    private suspend fun loadChaptersFromCatalog(catalogUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            val response = get(catalogUrl, customHeaders)
            val document = response.document

            document.select(NovelSelectors.VOLUME_CONTAINER).forEach { volumeElement ->
                val volumeText = volumeElement.selectFirstOrNull(NovelSelectors.VOLUME_TITLE)?.text()?.trim()
                    ?: volumeElement.ownText().trim()

                val volumeMatch = Regex("Volume\\s*(\\d+)", RegexOption.IGNORE_CASE).find(volumeText)
                val volumeName = volumeMatch?.let { "Vol.${it.groupValues[1]}" } ?: ""

                volumeElement.select(NovelSelectors.CHAPTER_ITEM).forEach { chapterElement ->
                    val link = chapterElement.selectFirstOrNull(NovelSelectors.CHAPTER_LINK) ?: return@forEach
                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.text()?.trim()
                        ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach

                    val isLocked = chapterElement.select(NovelSelectors.CHAPTER_LOCKED).isNotEmpty()

                    val chapterName = buildString {
                        if (volumeName.isNotBlank()) append("$volumeName: ")
                        append(chapterTitle)
                        if (isLocked) append(" 🔒")
                    }

                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }
            }

            if (chapters.isEmpty()) {
                document.select(NovelSelectors.CATALOG_FALLBACK).forEach { link ->
                    val chapterTitle = link.attrOrNull("title")?.trim()
                        ?: link.text()?.trim()
                        ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach

                    val isLocked = link.parent()?.select(NovelSelectors.CHAPTER_LOCKED)?.isNotEmpty() == true
                    val chapterName = if (isLocked) "$chapterTitle 🔒" else chapterTitle

                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load chapter list: ${e.message}")
        }

        return chapters
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl, customHeaders)
        val document = response.document

        if (document.title().contains("Cloudflare", ignoreCase = true) ||
            document.title().contains("Just a moment", ignoreCase = true)) {
            throw Exception("Cloudflare protection detected.")
        }

        document.select(NovelSelectors.CHAPTER_COMMENTS).remove()

        val titleHtml = document.selectFirstOrNull(NovelSelectors.CHAPTER_TITLE)?.html() ?: ""

        var contentHtml = ""
        val contentElement = document.selectAny(NovelSelectors.CHAPTER_CONTENT).firstOrNull()
        if (contentElement != null) {
            contentHtml = contentElement.html()
        } else {
            val paragraphs = document.select(NovelSelectors.CHAPTER_PARAGRAPH)
            if (paragraphs.isNotEmpty()) {
                contentHtml = paragraphs.joinToString("\n") { it.outerHtml() }
            }
        }

        if (contentHtml.isBlank()) return null

        val fullHtml = if (titleHtml.isNotBlank()) "$titleHtml\n$contentHtml" else contentHtml

        return cleanChapterHtml(fullHtml)
    }

    // ================================================================
    // REVIEWS (unchanged from previous version)
    // ================================================================

    override suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean
    ): List<UserReview> {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        return try {
            val response = get(fullUrl, customHeaders)
            val document = response.document

            extractCsrfToken(document)
            val bookId = extractBookId(fullUrl)

            parseReviewsFromHtml(document, bookId, showSpoilers)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseReviewsFromHtml(
        document: Document,
        bookId: String?,
        showSpoilers: Boolean
    ): List<UserReview> {
        return document.select(Selectors.REVIEW_CONTAINER).mapNotNull { reviewEl ->
            parseReviewElement(reviewEl, bookId, showSpoilers)
        }
    }

    private fun parseReviewElement(
        reviewEl: Element,
        bookId: String?,
        showSpoilers: Boolean
    ): UserReview? {
        val reviewId = extractReviewId(reviewEl) ?: return null

        val spoilerContainer = reviewEl.selectFirst(Selectors.SPOILER_CONTAINER)
        val isSpoiler = spoilerContainer != null

        val contentElement = if (isSpoiler) {
            spoilerContainer?.selectFirst(Selectors.CONTENT)
        } else {
            reviewEl.selectFirst(Selectors.CONTENT)
        }

        val content = contentElement?.text()?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val usernameEl = reviewEl.selectFirst(Selectors.USERNAME)
        val username = usernameEl?.attrOrNull("title") ?: usernameEl?.text()?.trim()
        val userId = usernameEl?.attrOrNull("href")?.extractUserId()

        val avatarEl = reviewEl.selectFirst(Selectors.AVATAR)
        val avatarUrl = fixCoverUrl(
            avatarEl?.attrOrNull("data-original") ?: avatarEl?.attrOrNull("src")
        )?.takeIf { !it.contains("data:image/gif") }

        val levelEl = reviewEl.selectFirst(Selectors.USER_LEVEL)
        val userLevel = levelEl?.attrOrNull("title")
            ?.replace("Level", "", ignoreCase = true)
            ?.trim()
            ?.toIntOrNull()
            ?: levelEl?.text()
                ?.replace("LV", "", ignoreCase = true)
                ?.trim()
                ?.toIntOrNull()

        val badgeEl = reviewEl.selectFirst(Selectors.USER_BADGE)
        val userBadgeUrl = fixCoverUrl(
            badgeEl?.attrOrNull("data-original") ?: badgeEl?.attrOrNull("src")
        )?.takeIf { !it.contains("data:image/gif") }

        val starContainer = reviewEl.selectFirst(Selectors.STAR_CONTAINER)
        val fullStars = starContainer?.select("svg._on")?.size ?: 0
        val halfStars = starContainer?.select("svg._half")?.size ?: 0
        val ratingValue = fullStars + (halfStars * 0.5f)
        val overallScore = if (ratingValue > 0) (ratingValue * SCORE_MULTIPLIER).toInt() else null

        val time = reviewEl.selectFirst(Selectors.TIME)?.text()?.trim()
        val likeCount = reviewEl.selectFirst(Selectors.LIKE_COUNT)?.text()?.toIntOrNull() ?: 0

        val replyButton = reviewEl.selectFirst(Selectors.REPLY_BUTTON)
        val replyCount = replyButton?.attrOrNull("data-rc")?.toIntOrNull() ?: 0

        val images = reviewEl.select(Selectors.REVIEW_IMAGE).mapNotNull { img ->
            fixCoverUrl(img.attrOrNull("data-original") ?: img.attrOrNull("src"))
                ?.takeIf { !it.contains("data:image/gif") }
        }

        val isPinned = reviewEl.attrOrNull("data-pinned") == "1"

        return UserReview(
            id = reviewId,
            content = content,
            username = username,
            userId = userId,
            avatarUrl = avatarUrl,
            userLevel = userLevel,
            userBadgeUrl = userBadgeUrl,
            overallScore = overallScore,
            time = time,
            likeCount = likeCount,
            replyCount = replyCount,
            hasMoreReplies = replyCount > 0,
            isSpoiler = isSpoiler,
            isPinned = isPinned,
            images = images,
            providerData = buildMap {
                bookId?.let { put("bookId", it) }
                put("reviewId", reviewId)
            }
        )
    }

    suspend fun loadReviewReplies(review: UserReview, page: Int = 1): RepliesResult {
        val bookId = review.providerData["bookId"] ?: return RepliesResult(emptyList(), false)
        val reviewId = review.id
        val token = csrfToken.get() ?: generateCsrfToken()

        val url = buildString {
            append("$mainUrl${Endpoints.REVIEW_DETAIL}?")
            append("_csrfToken=$token")
            append("&reviewId=$reviewId")
            append("&pageIndex=$page")
            append("&bookId=$bookId")
            append("&_=${System.currentTimeMillis()}")
        }

        return try {
            val response = get(url, getAjaxHeaders())
            val json = JSONObject(response.text)
            parseRepliesFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            RepliesResult(emptyList(), false)
        }
    }

    private fun parseRepliesFromJson(json: JSONObject): RepliesResult {
        val code = json.optInt("code", -1)
        if (code != 0) return RepliesResult(emptyList(), false)

        val data = json.optJSONObject("data") ?: return RepliesResult(emptyList(), false)
        val replyItems = data.optJSONArray("replyItems") ?: return RepliesResult(emptyList(), false)
        val isLast = data.optBoolean("isLast", true)

        val replies = (0 until replyItems.length()).mapNotNull { i ->
            replyItems.optJSONObject(i)?.let { parseReplyFromJson(it) }
        }

        return RepliesResult(replies = replies, hasMore = !isLast)
    }

    private fun parseReplyFromJson(obj: JSONObject): UserReview? {
        val reviewId = obj.optString("reviewId", null)?.takeIf { it.isNotBlank() } ?: return null
        val content = obj.optString("content", null)?.takeIf { it.isNotBlank() } ?: return null

        val username = obj.optString("userName", null)?.takeIf { it.isNotBlank() }
        val userId = obj.optLong("userId", 0).takeIf { it > 0 }?.toString()
        val userLevel = obj.optInt("userLevel", 0).takeIf { it > 0 }

        val headImageId = obj.optLong("headImageId", 0)
        val avatarUrl = if (headImageId > 0 && userId != null) {
            "https://user-pic.webnovel.com/userheadimg/$userId-10/100.jpg?uut=$headImageId"
        } else null

        val badgeUrl = obj.optString("holdBadgeCoverURL", null)?.takeIf { it.isNotBlank() }
        val badgeCoverId = obj.optLong("holdBadgeCoverId", 0)
        val userBadgeUrl = if (badgeUrl != null && badgeCoverId > 0) {
            "${badgeUrl}40.png?mt=$badgeCoverId"
        } else null

        val time = obj.optString("createTimeFormat", null)
        val likeCount = obj.optInt("likeAmount", 0)
        val isLikedByAuthor = obj.optInt("isLikedByAuthor", 0) == 1
        val isModerator = obj.optInt("isViceModerator", 0) == 1

        val parentReviewId = obj.optLong("pReviewId", 0).takeIf { it > 0 }?.toString()
        val parentUsername = obj.optString("pUserName", null)?.takeIf { it.isNotBlank() }
        val parentContent = obj.optString("pContent", null)?.takeIf { it.isNotBlank() }

        val imageItems = obj.optJSONArray("imageItems")
        val images = imageItems?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i)?.takeIf { it.isNotBlank() }
            }
        } ?: emptyList()

        return UserReview(
            id = reviewId,
            content = content,
            username = username,
            userId = userId,
            avatarUrl = avatarUrl,
            userLevel = userLevel,
            userBadgeUrl = userBadgeUrl,
            time = time,
            likeCount = likeCount,
            isLikedByAuthor = isLikedByAuthor,
            isModerator = isModerator,
            parentReviewId = parentReviewId,
            parentUsername = parentUsername,
            parentContentPreview = parentContent?.take(100),
            images = images
        )
    }

    private fun extractReviewId(element: Element): String? {
        element.attrOrNull("data-ejs")?.let { ejsData ->
            try {
                val json = JSONObject(ejsData)
                json.optString("reviewId")?.takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: Exception) {}
        }

        val className = element.className()
        val match = Regex("j_review_del_(\\d+)").find(className)
        return match?.groupValues?.getOrNull(1)
    }
}