package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterGroup
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class WebnovelProvider : MainProvider() {

    override val name = "Webnovel"
    override val mainUrl = "https://www.webnovel.com"
    override val iconRes = R.drawable.ic_provider_webnovel
    override val hasMainPage = true

    private val csrfToken = AtomicReference<String?>(null)
    private val bookIdCache = ConcurrentHashMap<String, String>()

    private object Endpoints {
        const val STORIES = "/stories"
        const val SEARCH = "/search"
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
        val CHAPTER_CONTENT = listOf(".cha-words", ".cha-content", "div.cha-content p")
        const val CHAPTER_PARAGRAPH = ".cha-paragraph p"
        const val CHAPTER_COMMENTS = ".para-comment"
        const val CATALOG_FALLBACK = ".j_catalog_list a"
    }

    private val customHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Referer" to mainUrl
    )

    override val tags = listOf(
        FilterOption("All Male", "male:all"),
        FilterOption("Action (Male)", "male:novel-action-male"),
        FilterOption("Eastern (Male)", "male:novel-eastern-male"),
        FilterOption("Fantasy (Male)", "male:novel-fantasy-male"),
        FilterOption("Horror (Male)", "male:novel-horror-male"),
        FilterOption("Realistic (Male)", "male:novel-realistic-male"),
        FilterOption("Sci-fi (Male)", "male:novel-scifi-male"),
        FilterOption("Urban (Male)", "male:novel-urban-male"),
        FilterOption("All Female", "female:all"),
        FilterOption("Fantasy (Female)", "female:novel-fantasy-female"),
        FilterOption("History (Female)", "female:novel-history-female"),
        FilterOption("Romance (Female)", "female:novel-general-female"),
        FilterOption("Teen (Female)", "female:novel-teen-female"),
        FilterOption("All Fanfiction", "fanfic:fanfic"),
        FilterOption("Anime & Comics (FF)", "fanfic:fanfic-anime-comics"),
        FilterOption("Video Games (FF)", "fanfic:fanfic-video-games"),
        FilterOption("Movies (FF)", "fanfic:fanfic-movies"),
        FilterOption("Book & Literature (FF)", "fanfic:fanfic-book-literature")
    )

    override val orderBys = listOf(
        FilterOption("Popular", "1"),
        FilterOption("Recommended", "2"),
        FilterOption("Most Collections", "3"),
        FilterOption("Rating", "4"),
        FilterOption("Time Updated", "5")
    )

    override val extraFilterGroups = listOf(
        FilterGroup(
            key = "status", label = "Status",
            options = listOf(
                FilterOption("All Status", "0"),
                FilterOption("Ongoing", "1"),
                FilterOption("Completed", "2")
            ),
            defaultValue = "0"
        )
    )

    private data class FilterParams(
        val gender: String? = null, val genreSlug: String? = null, val isFanfic: Boolean = false
    )

    private fun parseTagFilter(tag: String?): FilterParams {
        if (tag.isNullOrBlank()) return FilterParams()
        val parts = tag.split(":", limit = 2)
        if (parts.size != 2) return FilterParams()
        if (parts[0] == "fanfic") return FilterParams(isFanfic = true, genreSlug = parts[1])
        val gender = when (parts[0]) { "male" -> "1"; "female" -> "2"; else -> null }
        val slug = if (parts[1] == "all") null else parts[1]
        return FilterParams(gender = gender, genreSlug = slug)
    }

    private fun buildBrowseUrl(page: Int, orderBy: String? = null, tag: String? = null, status: String = "0"): String {
        val filter = parseTagFilter(tag)
        val sort = orderBy?.takeUnless { it.isEmpty() } ?: "1"
        val basePath = when {
            filter.isFanfic -> "${Endpoints.STORIES}/${filter.genreSlug}"
            filter.genreSlug != null -> "${Endpoints.STORIES}/${filter.genreSlug}"
            else -> "${Endpoints.STORIES}/novel"
        }
        val params = buildList {
            if (!filter.isFanfic && filter.genreSlug == null && filter.gender != null) add("gender=${filter.gender}")
            add("orderBy=$sort")
            add("bookStatus=$status")
            add("pageIndex=$page")
        }
        return "$mainUrl$basePath?${params.joinToString("&")}"
    }

    private fun Document.selectFirst(selectors: List<String>): Element? {
        for (s in selectors) { selectFirstOrNull(s)?.let { return it } }
        return null
    }

    private fun Document.selectAny(selectors: List<String>): Elements {
        for (s in selectors) { val e = select(s); if (e.isNotEmpty()) return e }
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
        val match = Regex("_(\\d{10,})").find(url)
        return match?.groupValues?.getOrNull(1)?.also { bookIdCache[url] = it }
    }

    private fun extractCsrfToken(document: Document) {
        document.selectFirstOrNull("meta[name=csrf-token]")?.attrOrNull("content")?.let { csrfToken.set(it) }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = cleaned.replace(Regex("<pirate>.*?</pirate>", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(
            Regex("Find authorized novels in Webnovel.*?for visiting\\.", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), ""
        )
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun parseCategoryNovels(document: Document): List<Novel> {
        return document.select(NovelSelectors.CATEGORY_CONTAINER).mapNotNull { element ->
            val thumb = element.selectFirstOrNull(NovelSelectors.CATEGORY_THUMB) ?: return@mapNotNull null
            val name = thumb.attrOrNull("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = thumb.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull(NovelSelectors.CATEGORY_COVER)
            val posterUrl = fixCoverUrl(imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src"))
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
            val posterUrl = fixCoverUrl(imgElement?.attrOrNull("src") ?: imgElement?.attrOrNull("data-original"))
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    private fun parseRelatedNovels(document: Document): List<Novel> {
        return document.select(NovelSelectors.RELATED_CONTAINER).mapNotNull { item ->
            val linkElement = item.selectFirstOrNull(NovelSelectors.RELATED_LINK)
                ?: item.selectFirstOrNull(NovelSelectors.RELATED_THUMB) ?: return@mapNotNull null
            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val title = linkElement.attrOrNull("title")
                ?: item.selectFirstOrNull(NovelSelectors.RELATED_TITLE)?.text()?.trim()
                ?: item.selectFirstOrNull("h3")?.text()?.trim() ?: return@mapNotNull null
            val imgElement = item.selectFirstOrNull(NovelSelectors.RELATED_COVER)
            val posterUrl = fixCoverUrl(imgElement?.attrOrNull("data-original") ?: imgElement?.attrOrNull("src"))
            val ratingText = item.selectFirstOrNull(NovelSelectors.RELATED_RATING)?.text()
            val rating = ratingText?.toFloatOrNull()?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
            Novel(name = title, url = novelUrl, posterUrl = posterUrl, rating = rating, apiName = this.name)
        }
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val status = extraFilters["status"] ?: "0"
        val url = buildBrowseUrl(page = page, orderBy = orderBy, tag = tag, status = status)
        val response = get(url, customHeaders)
        val document = response.document
        extractCsrfToken(document)
        val novels = parseCategoryNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val url = "$mainUrl${Endpoints.SEARCH}?keywords=$encodedQuery&pageIndex=1"
        val response = get(url, customHeaders)
        val document = response.document
        extractCsrfToken(document)
        return parseSearchNovels(document)
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val response = get(fullUrl, customHeaders)
        val document = response.document
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
            url = fullUrl, name = name, chapters = chapters,
            author = metadata.author, posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis, tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating, peopleVoted = metadata.peopleVoted,
            status = metadata.status, relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null,
        val synopsis: String? = null, val tags: List<String> = emptyList(),
        val rating: Int? = null, val peopleVoted: Int? = null, val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val coverElement = document.selectFirstOrNull(NovelSelectors.DETAIL_COVER)
        val posterUrl = fixCoverUrl(coverElement?.attrOrNull("src") ?: coverElement?.attrOrNull("data-original"))
        val synopsis = document.selectFirstOrNull(NovelSelectors.DETAIL_SYNOPSIS)?.let { element ->
            element.select("br").append("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        } ?: "No Summary Found"
        val genresText = document.selectFirstOrNull(NovelSelectors.DETAIL_GENRES)?.attrOrNull("title")
        val genreTags = genresText?.split(",")?.mapNotNull { it.trim().takeIf { t -> t.isNotBlank() } } ?: emptyList()
        val contentTags = document.select(NovelSelectors.DETAIL_TAGS).mapNotNull { el ->
            (el.attrOrNull("title")?.replace("Stories", "", ignoreCase = true)?.trim()
                ?: el.text()?.replace("#", "")?.trim()?.replaceFirstChar { it.uppercase() })
                ?.takeIf { it.isNotBlank() }
        }
        val tags = (genreTags + contentTags).distinct()
        var author: String? = null
        document.select(NovelSelectors.DETAIL_AUTHOR_LABEL).forEach { element ->
            if (element.text().trim() == "Author:") { author = element.nextElementSibling()?.text()?.trim(); return@forEach }
        }
        if (author.isNullOrBlank()) author = document.selectFirstOrNull(NovelSelectors.DETAIL_AUTHOR_ALT)?.text()?.trim()
        var statusText: String? = null
        document.select(NovelSelectors.DETAIL_STATUS).forEach { element ->
            if (element.attrOrNull("title") == "Status") { statusText = element.nextElementSibling()?.text()?.trim(); return@forEach }
        }
        val status = parseStatus(statusText)
        val ratingText = document.selectFirstOrNull(NovelSelectors.DETAIL_RATING)?.text()
            ?: document.selectFirstOrNull("[class*='score']")?.text()
        val rating = ratingText?.toFloatOrNull()?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
        return NovelMetadata(author, posterUrl, synopsis, tags, rating, null, status)
    }

    private suspend fun loadChaptersFromCatalog(catalogUrl: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        try {
            val document = get(catalogUrl, customHeaders).document
            document.select(NovelSelectors.VOLUME_CONTAINER).forEach { volumeElement ->
                val volumeText = volumeElement.selectFirstOrNull(NovelSelectors.VOLUME_TITLE)?.text()?.trim()
                    ?: volumeElement.ownText().trim()
                val volumeMatch = Regex("Volume\\s*(\\d+)", RegexOption.IGNORE_CASE).find(volumeText)
                val volumeName = volumeMatch?.let { "Vol.${it.groupValues[1]}" } ?: ""
                volumeElement.select(NovelSelectors.CHAPTER_ITEM).forEach { chapterElement ->
                    val link = chapterElement.selectFirstOrNull(NovelSelectors.CHAPTER_LINK) ?: return@forEach
                    val chapterTitle = link.attrOrNull("title")?.trim() ?: link.text()?.trim() ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach
                    val isLocked = chapterElement.select(NovelSelectors.CHAPTER_LOCKED).isNotEmpty()
                    val chapterName = buildString {
                        if (volumeName.isNotBlank()) append("$volumeName: ")
                        append(chapterTitle)
                        if (isLocked) append(" [Locked]")
                    }
                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                }
            }
            if (chapters.isEmpty()) {
                document.select(NovelSelectors.CATALOG_FALLBACK).forEach { link ->
                    val chapterTitle = link.attrOrNull("title")?.trim() ?: link.text()?.trim() ?: return@forEach
                    val chapterPath = link.attrOrNull("href") ?: return@forEach
                    val chapterUrl = fixUrl(chapterPath) ?: return@forEach
                    val isLocked = link.parent()?.select(NovelSelectors.CHAPTER_LOCKED)?.isNotEmpty() == true
                    chapters.add(Chapter(name = if (isLocked) "$chapterTitle [Locked]" else chapterTitle, url = chapterUrl))
                }
            }
        } catch (_: Exception) {}
        return chapters
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl, customHeaders).document
        document.select(NovelSelectors.CHAPTER_COMMENTS).remove()
        val titleHtml = document.selectFirstOrNull(".cha-tit")?.html() ?: ""
        val contentElements = document.selectAny(NovelSelectors.CHAPTER_CONTENT)
        var contentHtml = contentElements.firstOrNull()?.html() ?: ""
        if (contentHtml.isBlank()) {
            val paragraphs = document.select(NovelSelectors.CHAPTER_PARAGRAPH)
            if (paragraphs.isNotEmpty()) contentHtml = paragraphs.joinToString("\n") { it.outerHtml() }
        }
        if (contentHtml.isBlank()) return null
        val fullHtml = if (titleHtml.isNotBlank()) "$titleHtml\n$contentHtml" else contentHtml
        return cleanChapterHtml(fullHtml)
    }
}
