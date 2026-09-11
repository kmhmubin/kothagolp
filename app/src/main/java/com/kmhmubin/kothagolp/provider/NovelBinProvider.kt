package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.data.remote.NetworkClient
import com.kmhmubin.kothagolp.data.remote.NetworkException
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * NovelBin runs the same backend behind three domains (novel-bin.com,
 * novel-bin.net, novelbin.cc) with a different visual "skin" per domain —
 * each rebuilt its frontend independently (Tailwind, mid-2026), but all three
 * share the underlying `nl2-*` structural hooks, `og:novel:*` meta block, and
 * schema.org Book JSON-LD, which is what this provider actually parses.
 *
 *  - lists/genre/search render `a.nl2-book-card` (or `a[data-nl2-book-card]`
 *    on novelbin.cc, which uses that as an attribute instead of a class)
 *  - novel detail: JSON-LD (name, author, description, genre[]) + og:novel:*
 *    meta (status, latest chapter url) — far more stable than the layout
 *  - the chapter list on the detail page is capped at the first 30 entries;
 *    the rest are generated from the predictable /chapter-N URL scheme using
 *    the total pulled from og:novel:lastest_chapter_url
 *  - chapter reader markup (#chr-content) is unchanged from the old site
 *
 * novel-bin.com is primary; the other two are only used when it's down. The
 * per-novel path prefix differs on novelbin.cc ("/book/" vs "/novel-bin/"
 * elsewhere) — [adaptPath] swaps it when falling back to a different domain.
 */
class NovelBinProvider : MainProvider() {

    override val name = "NovelBin"
    override val mainUrl = "https://novel-bin.com"
    override val iconRes = R.drawable.ic_provider_novelbin
    override val hasMainPage = true

    // Backups, tried in order when the previous one is down or dead.
    private val domains = listOf(mainUrl, "https://novel-bin.net", "https://novelbin.cc")
    @Volatile private var activeDomain = mainUrl

    private fun novelPathPrefix(domain: String) = if (domain == "https://novelbin.cc") "/book/" else "/novel-bin/"

    /** Swaps the per-novel path prefix when the path is domain-specific (detail/chapter urls). */
    private fun adaptPath(path: String, targetDomain: String): String {
        val prefixes = listOf("/novel-bin/", "/book/")
        val matched = prefixes.firstOrNull { path.startsWith(it) } ?: return path
        return novelPathPrefix(targetDomain) + path.removePrefix(matched)
    }

    private fun pathOf(url: String): String {
        for (domain in domains) if (url.startsWith(domain)) return url.removePrefix(domain)
        return try {
            val uri = java.net.URI(url)
            (uri.rawPath ?: "") + (uri.rawQuery?.let { "?$it" } ?: "")
        } catch (_: Exception) { url }
    }

    /** Tries [activeDomain] first, then the remaining domains, sticking with whichever works. */
    private suspend fun fetch(path: String, headers: Map<String, String> = emptyMap()): NetworkClient.NetworkResponse {
        val order = listOf(activeDomain) + domains.filter { it != activeDomain }
        var lastError: Exception? = null
        for (domain in order) {
            val fullUrl = "$domain${adaptPath(path, domain)}"
            try {
                val response = get(fullUrl, headers)
                if (response.isSuccessful) {
                    activeDomain = domain
                    return response
                }
                lastError = NetworkException("HTTP ${response.code} from $fullUrl")
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: NetworkException("NovelBin unreachable on all known domains")
    }

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "Action"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Eastern", "Eastern"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Josei", "Josei"),
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
        FilterOption("Shounen", "Shounen"),
        FilterOption("Slice of Life", "Slice of Life"),
        FilterOption("Smut", "Smut"),
        FilterOption("Sports", "Sports"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy"),
        FilterOption("Wuxia", "Wuxia"),
        FilterOption("Xianxia", "Xianxia"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Yaoi", "Yaoi")
    )

    // The homepage uses a different markup (index-novel) with no parseable
    // rows, so every sort maps to a standard list page; default = Most Visited.
    override val orderBys = listOf(
        FilterOption("Most Visited", "allvisit"),
        FilterOption("Daily Top", "dayvisit"),
        FilterOption("Monthly Top", "monthvisit"),
        FilterOption("Completed", "full.html")
    )

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

    private fun parseNovels(document: Document): List<Novel> {
        return document.select("a.nl2-book-card, a[data-nl2-book-card]").mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val name = element.attrOrNull("title") ?: return null
        val novelUrl = fixUrl(element.attrOrNull("href")) ?: return null
        val posterUrl = element.selectFirstOrNull("img")
            ?.let { it.attrOrNull("src") ?: it.attrOrNull("data-src") }
            ?.let { fixUrl(it) }
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val path = when {
            !tag.isNullOrBlank() -> "/genre/${java.net.URLEncoder.encode(tag, "UTF-8")}/?page=$page"
            orderBy == "full.html" -> "/full.html?page=$page"
            else -> "/${orderBy?.takeIf { it.isNotBlank() } ?: "allvisit"}/?page=$page"
        }
        val document = fetch(path).document
        val novels = parseNovels(document)
        val nextLink = document.selectFirstOrNull("a[aria-label='Next page']")
        val hasNext = nextLink != null &&
            !nextLink.className().contains("disabled", ignoreCase = true) &&
            nextLink.attrOrNull("href") != null &&
            novels.isNotEmpty()
        return MainPageResult(url = "$activeDomain$path", novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val document = fetch("/search?keyword=$encoded").document
        return parseNovels(document)
    }

    /** The schema.org Book block is far more stable than the visual layout. */
    private fun parseBookJsonLd(document: Document): JSONObject? {
        for (script in document.select("script[type=application/ld+json]")) {
            val raw = script.data().ifBlank { script.html() }
            if (!raw.contains("\"@type\":\"Book\"")) continue
            return try { JSONObject(raw) } catch (_: Exception) { null }
        }
        return null
    }

    override suspend fun load(url: String): NovelDetails? {
        val document = fetch(pathOf(url)).document
        val book = parseBookJsonLd(document)
        val name = book?.optString("name")?.takeIf { it.isNotBlank() }
            ?: document.selectFirstOrNull("h1")?.textOrNull()?.trim()
            ?: return null

        val posterUrl = book?.optString("image")?.takeIf { it.isNotBlank() }?.let { fixUrl(it) }
        val synopsis = book?.optString("description")?.trim()?.takeIf { it.isNotBlank() }
        val author = book?.optJSONObject("author")?.optString("name")?.takeIf { it.isNotBlank() }
        val genresArray = book?.optJSONArray("genre")
        val tagsList = if (genresArray != null) {
            (0 until genresArray.length()).mapNotNull { genresArray.optString(it, null) }
        } else emptyList()
        val status = document.selectFirstOrNull("meta[property=og:novel:status]")
            ?.attrOrNull("content")?.let { parseStatus(it) }

        val chapters = parseChapters(document, url)

        return NovelDetails(
            url = url, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis,
            tags = tagsList.ifEmpty { null }, status = status
        )
    }

    /**
     * The detail page only embeds the first 30 chapters (SEO/perf cap). The
     * rest are generated from the predictable /chapter-N scheme, using the
     * total chapter count pulled from og:novel:lastest_chapter_url. The
     * per-skin chapter list markup differs (<strong> vs a plain trailing
     * <span> for the title), so this reads whichever child text comes last.
     */
    private fun parseChapters(document: Document, novelUrl: String): List<Chapter> {
        val base = novelUrl.trimEnd('/')
        val scrapedNames = mutableMapOf<Int, String>()
        for (link in document.select("li > a[href*='chapter-']")) {
            val href = link.attrOrNull("href") ?: continue
            val number = href.substringAfterLast("chapter-").toIntOrNull() ?: continue
            val chapterName = link.children().mapNotNull { it.textOrNull()?.trim() }.lastOrNull()
            if (chapterName != null) scrapedNames[number] = chapterName
        }

        val total = document.selectFirstOrNull("meta[property=og:novel:lastest_chapter_url]")
            ?.attrOrNull("content")?.substringAfterLast("chapter-")?.toIntOrNull()
            ?: document.select("dl.almanac-detail-facts > div").find { it.selectFirstOrNull("dt")?.text() == "Chapters" }
                ?.selectFirstOrNull("dd")?.text()?.toIntOrNull()
            ?: scrapedNames.keys.maxOrNull()
            ?: return emptyList()

        return (1..total).map { number ->
            Chapter(
                name = scrapedNames[number] ?: "Chapter $number",
                url = "$base/chapter-$number"
            )
        }
    }

    // The site injects a "Read N more chapters here:" upsell line as the last
    // paragraph of chapter text; strip it, it's not part of the chapter.
    private val readMoreTeaser = Regex("(?i)<p>\\s*Read\\s+\\d+\\s+more\\s+chapters?\\s+here:?\\s*</p>")

    override suspend fun loadChapterContent(url: String): String? {
        val document = fetch(pathOf(url)).document
        val contentElement = document.selectFirstOrNull("#chr-content, #chapter-content, .chr-c")
            ?: return null
        contentElement.select(
            ".unlock-buttons, .ads, .adsbygoogle, sub, script, style, iframe, " +
                ".ads-holder, .ads-middle, [id*='ads'], [class*='ads'], " +
                ".hidden, [style*='display:none'], [style*='display: none']"
        ).remove()
        val rawHtml = contentElement.html()
            .replace(readMoreTeaser, "")
            .replace(Regex("\\s{3,}"), "\n\n")
            .replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return rawHtml.trim().takeIf { it.isNotBlank() }
    }
}
