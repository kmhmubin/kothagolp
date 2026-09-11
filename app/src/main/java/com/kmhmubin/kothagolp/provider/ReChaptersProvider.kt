package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * ReChapters (Next.js App Router) only server-renders its shell + a recent
 * slice of the chapter list as plain HTML; chapter reading text and the
 * "browse everything" / "load more" listings are client-only. This provider
 * only ever touches paths the site's own robots.txt does NOT disallow for
 * crawlers: individual /book/ pages, /tag/{genre} listings, and the site's
 * own published sitemaps — never /browse, /search, or /api/.
 *
 *  - browse: /tag/{genre} pages (real HTML cards, covers included); the
 *    site has no working "everything" or search listing outside /browse
 *    and /search, so both loadMainPage's untagged case and search() fall
 *    back to the book sitemap (title derived from the URL slug, no cover —
 *    the sitemap carries no image, only the /tag/ pages do)
 *  - detail: schema.org Book JSON-LD (name/author/synopsis/genres/cover)
 *  - chapters: the detail page only embeds a recent slice; the *full*,
 *    correctly-ordered list comes from the book's own chapters sitemap
 *    (https://www.rechapters.com/sitemaps/chapters/{bookId}.xml) — verified
 *    first entry is chapter 1, last is the latest chapter
 *  - chapter content: server-rendered but only inside Next.js's RSC
 *    "flight" hydration payload (`self.__next_f.push(...)` script tags),
 *    not as plain HTML — extracted by parsing that payload's `pageBlocks`
 *    array rather than by CSS selector
 */
class ReChaptersProvider : MainProvider() {

    override val name = "ReChapters"
    override val mainUrl = "https://www.rechapters.com"
    override val hasMainPage = true

    override val tags = listOf(
        FilterOption("Action", "Action"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Ecchi", "Ecchi"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Isekai", "ISEKAI"),
        FilterOption("Josei", "Josei"),
        FilterOption("Martial Arts", "Martial Arts"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Romance", "Romance"),
        FilterOption("School Life", "School Life"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Seinen", "Seinen"),
        FilterOption("Shounen", "Shounen"),
        FilterOption("Slice of Life", "Slice of Life"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy")
    )

    private fun titleFromSlug(slug: String): String =
        slug.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    /** The book-id suffix the site appends to every slug, e.g. "...-gnbeu68zptqb". */
    private fun bookIdOf(novelUrl: String): String =
        novelUrl.trimEnd('/').substringAfterLast("/").substringAfterLast("-")

    // The site has no compliant "everything" or search listing (both /browse
    // and /search are robots.txt-disallowed); its own book sitemap is the
    // only alternative, so it's fetched once and reused for both.
    private var sitemapCache: List<Novel>? = null

    private suspend fun loadSitemapBooks(): List<Novel> {
        sitemapCache?.let { return it }
        val document = get("$mainUrl/sitemaps/books/1.xml").document
        val novels = document.select("url > loc").mapNotNull { loc ->
            val url = loc.textOrNull()?.trim() ?: return@mapNotNull null
            val slug = url.substringAfterLast("/book/").substringBeforeLast("-")
            if (slug.isBlank()) return@mapNotNull null
            Novel(name = titleFromSlug(slug), url = url, apiName = this.name)
        }
        sitemapCache = novels
        return novels
    }

    private fun parseNovels(document: Document): List<Novel> {
        return document.select("a[href^=/book/]").mapNotNull { link ->
            val img = link.selectFirstOrNull("img") ?: return@mapNotNull null
            val name = img.attrOrNull("alt")?.trim() ?: return@mapNotNull null
            val novelUrl = fixUrl(link.attrOrNull("href")) ?: return@mapNotNull null
            val posterUrl = img.attrOrNull("src")?.let { fixUrl(it) }
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        if (tag.isNullOrBlank()) {
            // No compliant "everything" listing exists; page through the sitemap instead.
            val all = loadSitemapBooks()
            val pageSize = 24
            val slice = all.drop((page - 1) * pageSize).take(pageSize)
            return MainPageResult(url = "$mainUrl/sitemaps/books/1.xml", novels = slice, hasNextPage = (page * pageSize) < all.size)
        }
        val encodedTag = java.net.URLEncoder.encode(tag, "UTF-8")
        val url = "$mainUrl/tag/$encodedTag"
        val document = get(url).document
        // The site's own listing caps at 20 with a JS/API-driven "Load more"
        // this provider won't use (that API path is robots.txt-disallowed).
        return MainPageResult(url = url, novels = parseNovels(document), hasNextPage = false)
    }

    override suspend fun search(query: String): List<Novel> {
        val all = loadSitemapBooks()
        return all.filter { it.name.contains(query.trim(), ignoreCase = true) }
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

    private fun parseBookJsonLd(document: Document): JSONObject? {
        for (script in document.select("script[type=application/ld+json]")) {
            val raw = script.data().ifBlank { script.html() }
            if (!raw.contains("\"@type\":\"Book\"")) continue
            return try { JSONObject(raw) } catch (_: Exception) { null }
        }
        return null
    }

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url).document
        val book = parseBookJsonLd(document)
        val name = book?.optString("name")?.takeIf { it.isNotBlank() }
            ?: document.selectFirstOrNull("h1")?.textOrNull()?.trim() ?: return null

        val posterUrl = book?.optString("image")?.takeIf { it.isNotBlank() }?.let { fixUrl(it) }
        val synopsis = book?.optString("description")?.trim()?.takeIf { it.isNotBlank() }
        val author = book?.optJSONArray("author")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() }
        val genres = book?.optString("keywords")?.split(",")?.mapNotNull { it.trim().takeIf(String::isNotBlank) } ?: emptyList()
        val statusWords = setOf("ongoing", "completed", "hiatus", "dropped", "cancelled")
        val status = document.select("span").map { it.textOrNull()?.trim().orEmpty() }
            .firstOrNull { it.lowercase() in statusWords }?.let { parseStatus(it) }

        val chapters = parseChapters(document, url)

        return NovelDetails(
            url = url, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis, tags = genres.ifEmpty { null }, status = status
        )
    }

    /**
     * The detail page only embeds a recent slice of chapters (for titles);
     * the full, correctly-ordered list comes from the book's chapters
     * sitemap, which lists every chapter URL in reading order.
     */
    private suspend fun parseChapters(document: Document, novelUrl: String): List<Chapter> {
        val bookId = bookIdOf(novelUrl)
        val bookPathPrefix = novelUrl.trimEnd('/').substringAfterLast("/book/").let { "/book/$it/" }
        val scrapedNames = mutableMapOf<String, String>()
        // Chapter rows are "<li><a>"; the page also has a standalone
        // "Start reading" CTA anchor whose href matches the same book path
        // prefix but whose nested responsive spans ("Start" + "Start
        // reading") would otherwise get scraped together as a fake title.
        for (link in document.select("li > a[href^=$bookPathPrefix]")) {
            val href = link.attrOrNull("href") ?: continue
            val chapterId = href.substringAfterLast("/").takeIf { it.isNotBlank() } ?: continue
            val title = link.selectFirstOrNull("span")?.textOrNull()?.trim() ?: continue
            scrapedNames[chapterId] = title
        }

        val sitemapDoc = try { get("$mainUrl/sitemaps/chapters/$bookId.xml").document } catch (_: Exception) { return emptyList() }
        val chapterUrls = sitemapDoc.select("url > loc").mapNotNull { it.textOrNull()?.trim() }
        return chapterUrls.mapIndexed { index, chapterUrl ->
            val chapterId = chapterUrl.trimEnd('/').substringAfterLast("/")
            Chapter(name = scrapedNames[chapterId] ?: "Chapter ${index + 1}", url = chapterUrl)
        }
    }

    /** Recursively searches a parsed JSON value for a "pageBlocks" array. */
    private fun findPageBlocks(value: Any?): JSONArray? {
        when (value) {
            is JSONObject -> {
                value.optJSONArray("pageBlocks")?.let { return it }
                val keys = value.keys()
                while (keys.hasNext()) {
                    findPageBlocks(value.opt(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findPageBlocks(value.opt(i))?.let { return it }
                }
            }
        }
        return null
    }

    private fun escapeParagraph(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br/>")

    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url).document
        for (script in document.select("script")) {
            val raw = script.data()
            if (!raw.startsWith("self.__next_f.push(") || !raw.contains("pageBlocks")) continue
            val blocks = try {
                val outer = JSONArray(raw.removePrefix("self.__next_f.push(").removeSuffix(")"))
                val rowText = outer.optString(1, "")
                val body = rowText.substringAfter(":", "")
                if (body.isBlank()) continue
                findPageBlocks(JSONTokener(body).nextValue())
            } catch (_: Exception) { null } ?: continue

            val html = buildString {
                for (i in 0 until blocks.length()) {
                    val block = blocks.optJSONObject(i)?.optJSONObject("block") ?: continue
                    if (block.optString("type") != "text") continue
                    val content = block.optString("content").takeIf { it.isNotBlank() } ?: continue
                    append("<p>").append(escapeParagraph(content)).append("</p>")
                }
            }
            if (html.isNotBlank()) return html
        }
        return null
    }
}
