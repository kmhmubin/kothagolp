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
 * NovelNice runs the Madara WordPress theme (novel slug "/read/{slug}/",
 * genre taxonomy "/novel-genre/{tag}/", standard WP "/page/N/" pagination).
 *
 * The chapter list is NOT in the initial page HTML — it's fetched by the
 * page's own JS via POST to "{novelUrl}ajax/chapters/?t=1", which returns
 * the full <li class="wp-manga-chapter"> list (newest first) in one shot,
 * no 30-item cap like some sites. Chapter reader markup is a plain
 * div.reading-content > div.text-left with <p> paragraphs.
 */
class NovelNiceProvider : MainProvider() {

    override val name = "NovelNice"
    override val mainUrl = "https://novelnice.com"
    override val iconRes = R.drawable.ic_provider_novelnice
    override val hasMainPage = true

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Horror", "horror"),
        FilterOption("Romance", "romance"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Urban", "urban")
    )

    override val orderBys = listOf(
        FilterOption("Latest Update", ""),
        FilterOption("New", "new-manga"),
        FilterOption("Most Read", "views"),
        FilterOption("Top Rated", "rating"),
        FilterOption("Completed", "completed")
    )

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "on hold", "hiatus" -> "On Hiatus"
            "canceled", "cancelled", "dropped" -> "Cancelled"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun parseNovels(document: Document): List<Novel> =
        document.select("div.page-item-detail").mapNotNull { parseNovelElement(it) }

    private fun parseNovelElement(element: Element): Novel? {
        val titleLink = element.selectFirstOrNull("div.post-title a") ?: return null
        val name = titleLink.textOrNull()?.trim() ?: return null
        val novelUrl = fixUrl(titleLink.attrOrNull("href")) ?: return null
        val img = element.selectFirstOrNull("div.item-thumb img")
        val posterUrl = img?.let { it.attrOrNull("data-src") ?: it.attrOrNull("src") }?.let { fixUrl(it) }
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val base = when {
            orderBy == "completed" -> "/novel/completed/"
            !tag.isNullOrBlank() -> "/novel-genre/$tag/"
            else -> "/read/"
        }
        val sort = orderBy?.takeIf { it.isNotBlank() && it != "completed" }
        val path = buildString {
            append(base)
            if (page > 1) append("page/$page/")
            if (sort != null) append("?m_orderby=$sort")
        }
        val url = "$mainUrl$path"
        val document = get(url).document
        val novels = parseNovels(document)
        val hasNext = document.selectFirstOrNull("div.nav-previous a") != null
        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val document = get("$mainUrl/?s=$encoded&post_type=wp-manga").document
        return parseNovels(document)
    }

    /** Reads a `<h5>label</h5> ... <div class="summary-content">value</div>` metadata block. */
    private fun metaContent(document: Document, label: String): Element? =
        document.select("div.post-content_item").find {
            it.selectFirstOrNull("div.summary-heading h5")?.text()?.trim().equals(label, ignoreCase = true)
        }?.selectFirstOrNull("div.summary-content")

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url).document
        val name = document.selectFirstOrNull("div.post-title h1")?.textOrNull()?.trim() ?: return null

        val posterUrl = document.selectFirstOrNull("div.summary_image img")
            ?.let { it.attrOrNull("data-src") ?: it.attrOrNull("src") }?.let { fixUrl(it) }
        // #editdescription holds only the real synopsis; the site's "You're
        // reading ... on X" attribution line is a sibling, not inside it.
        val synopsis = document.selectFirstOrNull("div#editdescription")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val author = metaContent(document, "Author(s)")?.select("a")
            ?.mapNotNull { it.textOrNull()?.trim() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
        val genres = metaContent(document, "Genre(s)")?.select("a")?.mapNotNull { it.textOrNull()?.trim() } ?: emptyList()
        val status = parseStatus(metaContent(document, "Status")?.textOrNull()?.trim())
        val rating = document.selectFirstOrNull("span[property=ratingValue]")?.textOrNull()?.trim()?.toFloatOrNull()
            ?.let { (it * 100).roundToInt() }
        val peopleVoted = document.selectFirstOrNull("span[property=ratingCount]")?.textOrNull()?.trim()?.toIntOrNull()

        val chapters = loadChapters(url)

        return NovelDetails(
            url = url, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis, tags = genres.ifEmpty { null },
            rating = rating, peopleVoted = peopleVoted, status = status
        )
    }

    /** The full chapter list isn't in the page HTML — it's fetched with this POST call. */
    private suspend fun loadChapters(novelUrl: String): List<Chapter> {
        val base = novelUrl.trimEnd('/')
        return try {
            val document = post("$base/ajax/chapters/?t=1", headers = mapOf("Referer" to "$base/")).document
            document.select("li.wp-manga-chapter a").mapNotNull { link ->
                val chapterUrl = fixUrl(link.attrOrNull("href")) ?: return@mapNotNull null
                val chapterName = link.textOrNull()?.trim() ?: return@mapNotNull null
                val date = link.parent()?.selectFirstOrNull("span.chapter-release-date i")?.textOrNull()?.trim()
                Chapter(name = chapterName, url = chapterUrl, dateOfRelease = date)
            }.reversed() // endpoint returns newest-first; flip to reading order
        } catch (_: Exception) { emptyList() }
    }

    // The site appends a "Content source: X" attribution line to every
    // chapter's text; strip it, it's not part of the chapter.
    private val sourceAttribution = Regex("(?i)<p[^>]*>\\s*[\"“]?Content source:[^<]*</p>")

    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url).document
        val contentElement = document.selectFirstOrNull("div.reading-content div.text-left")
            ?: document.selectFirstOrNull("div.reading-content")
            ?: return null
        contentElement.select("script, style, .mts-text-settings, .code-block, .adsbygoogle, ins, iframe").remove()
        val rawHtml = contentElement.html()
            .replace(sourceAttribution, "")
            .replace(Regex("\\s{3,}"), "\n\n")
        return rawHtml.trim().takeIf { it.isNotBlank() }
    }
}
