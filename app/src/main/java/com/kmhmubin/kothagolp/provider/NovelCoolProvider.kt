package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.roundToInt

/**
 * NovelCool cloaks its own in-page chapter links: to a normal browser UA,
 * every chapter link (the chapter list on the detail page, and the
 * prev/next nav on the reading page) is rewritten to an ad-redirect domain
 * ("mechanismexplained.com" / "workexplained.com") that leads to unrelated
 * ad content instead of the chapter. To a crawler UA the exact same markup
 * carries the real, direct `/chapter/{slug}/{id}/` link — confirmed by
 * diffing a normal-UA fetch against a Googlebot-UA fetch of the same page.
 * Every request here uses a crawler UA to get the real links; nothing else
 * about the page differs (verified byte-identical chapter content either way).
 */
class NovelCoolProvider : MainProvider() {

    override val name = "NovelCool"
    override val mainUrl = "https://www.novelcool.com"
    override val iconRes = R.drawable.ic_provider_novelcool
    override val hasMainPage = true

    private val crawlerHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    )

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "Action"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Historical", "Historical"),
        FilterOption("Martial Arts", "Martial-Arts"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Romance", "Romance"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Slice of Life", "Slice-of-Life"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Xuanhuan", "Xuanhuan")
    )

    // The site doesn't paginate these category listings (query/page params
    // are ignored server-side; each is a fixed single-page list), so every
    // sort just maps to its own listing path.
    override val orderBys = listOf(
        FilterOption("Popular", "popular"),
        FilterOption("Latest Releases", "latest"),
        FilterOption("New Novels", "new_list"),
        FilterOption("Completed", "completed"),
        FilterOption("Original", "original")
    )

    private fun parseNovels(document: Document): List<Novel> =
        document.select("div.book-item").mapNotNull { parseNovelElement(it) }

    private fun parseNovelElement(element: Element): Novel? {
        val titleLink = element.selectFirstOrNull("div.book-pic a") ?: return null
        val name = element.selectFirstOrNull("[itemprop=name]")?.textOrNull()?.trim()
            ?: titleLink.attrOrNull("title")?.trim() ?: return null
        val novelUrl = fixUrl(titleLink.attrOrNull("href")) ?: return null
        val img = element.selectFirstOrNull("img")
        val posterUrl = img?.let { it.attrOrNull("lazy_url") ?: it.attrOrNull("src") }?.let { fixUrl(it) }
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val path = when {
            !tag.isNullOrBlank() -> "/category/$tag.html"
            else -> "/category/${orderBy?.takeIf { it.isNotBlank() } ?: "popular"}.html"
        }
        val url = "$mainUrl$path"
        val document = get(url, crawlerHeaders).document
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels, hasNextPage = false)
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val document = get("$mainUrl/search?wd=$encoded", crawlerHeaders).document
        return parseNovels(document)
    }

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url, crawlerHeaders).document
        val name = document.selectFirstOrNull("h1.bookinfo-title")?.textOrNull()?.trim() ?: return null

        val posterUrl = document.selectFirstOrNull("img.bookinfo-pic-img")
            ?.let { it.attrOrNull("src") ?: it.attrOrNull("lazy_url") }?.let { fixUrl(it) }
        val author = document.selectFirstOrNull("div.bookinfo-author")?.textOrNull()
            ?.removePrefix("Author:")?.trim()?.takeIf { it.isNotBlank() }
        val status = document.selectFirstOrNull("div.bk-cate-item.bk-cate-type1")?.textOrNull()?.trim()
        val genres = document.select("div.bk-cate-item [itemprop=keywords]").mapNotNull { it.textOrNull()?.trim() }
        val synopsis = document.selectFirstOrNull("span[itemprop=description]")?.textOrNull()?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("N/A", ignoreCase = true) }
        val rating = document.selectFirstOrNull("span.bookinfo-rate-val")?.textOrNull()?.trim()?.toFloatOrNull()
            ?.let { (it * 100).roundToInt() }
        val peopleVoted = document.selectFirstOrNull("span.bookinfo-vote-num")?.text()
            ?.filter { it.isDigit() }?.toIntOrNull()

        val chapters = parseChapters(document)

        return NovelDetails(
            url = url, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis, tags = genres.ifEmpty { null },
            rating = rating, peopleVoted = peopleVoted, status = status
        )
    }

    /** Site lists chapters newest-first; flip to reading order. */
    private fun parseChapters(document: Document): List<Chapter> {
        val chapters = document.select("div.chp-item a[href]").mapNotNull { link ->
            val chapterUrl = fixUrl(link.attrOrNull("href")) ?: return@mapNotNull null
            val chapterName = link.attrOrNull("title")?.trim()
                ?: link.selectFirstOrNull(".chapter-item-headtitle, .chapter-item-title")?.textOrNull()?.trim()
                ?: return@mapNotNull null
            val date = link.selectFirstOrNull(".chapter-item-time")?.textOrNull()?.trim()
            Chapter(name = chapterName, url = chapterUrl, dateOfRelease = date)
        }
        return chapters.reversed()
    }

    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url, crawlerHeaders).document
        document.selectFirstOrNull("p.chapter-start-mark") ?: return null
        val paragraphs = document.select("p.chapter-start-mark ~ p").filterNot { it.hasClass("chapter-end-mark") }
        if (paragraphs.isEmpty()) return null
        return paragraphs.joinToString("") { it.outerHtml() }.trim().takeIf { it.isNotBlank() }
    }
}
