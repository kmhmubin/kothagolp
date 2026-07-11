package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * NovelBin moved to novel-bin.com (backup mirror: www.novelbin.cc — note the
 * mirror runs a different engine and does not share this markup). The new
 * site keeps the classic NovelBin markup (div.list > div.row, h3.novel-title,
 * #chr-content) but with different routes:
 *
 *  - lists: / (hot), /allvisit/, /dayvisit/, /monthvisit/, /full.html, ?page=N
 *  - genre: /genre/{Name}/?page=N
 *  - search: /search?keyword=
 *  - novel: /novel-bin/{slug}/ — full chapter list embedded (ul.list-chapter)
 *  - chapter: /novel-bin/{slug}/chapter-N — content in #chr-content
 */
class NovelBinProvider : MainProvider() {

    override val name = "NovelBin"
    override val mainUrl = "https://novel-bin.com"
    override val iconRes = R.drawable.ic_provider_novelbin
    override val hasMainPage = true

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

    override val orderBys = listOf(
        FilterOption("Hot", ""),
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

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src") ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null
        return fixUrl(rawSrc)
    }

    private fun parseNovels(document: Document): List<Novel> {
        // Skip sidebar lists (list-side): they carry hot-item rows with no covers
        val container = document.selectFirstOrNull("div.list.list-novel:not(.list-side)")
            ?: document.selectFirstOrNull("div.list")
            ?: return emptyList()
        return container.select("div.row").mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirstOrNull("h3.novel-title > a") ?: return null
        val name = titleElement.attrOrNull("title") ?: titleElement.textOrNull()?.trim() ?: return null
        val novelUrl = fixUrl(titleElement.attrOrNull("href")) ?: return null
        val posterUrl = fixPosterUrl(element.selectFirstOrNull("img"))
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val url = when {
            !tag.isNullOrBlank() -> "$mainUrl/genre/$tag/?page=$page"
            orderBy.isNullOrBlank() -> "$mainUrl/?page=$page"
            orderBy == "full.html" -> "$mainUrl/full.html?page=$page"
            else -> "$mainUrl/$orderBy/?page=$page"
        }
        val document = get(url).document
        val novels = parseNovels(document)
        val hasNext = document.selectFirstOrNull("ul.pagination a[href*='page=${page + 1}'], li.next:not(.disabled)") != null ||
            novels.size >= 20
        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext && novels.isNotEmpty())
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val document = get("$mainUrl/search?keyword=$encoded").document
        return parseNovels(document)
    }

    override suspend fun load(url: String): NovelDetails? {
        val document = get(url).document
        val name = document.selectFirstOrNull("h3.title")?.textOrNull()?.trim() ?: return null

        val posterUrl = fixPosterUrl(
            document.selectFirstOrNull("div.book img, div.books img, .info-holder img")
        )
        val synopsis = document.selectFirstOrNull("div.desc-text")?.let { element ->
            element.select("br").append("\\n")
            element.select("p").prepend("\\n")
            element.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
        }
        val author = document.selectFirstOrNull(
            "ul.info li:contains(Author) a, ul.info-meta li:contains(Author) a, a[href*='/a/'], a[href*='/author/']"
        )?.textOrNull()?.trim()
        val genres = document.select(
            "ul.info li:contains(Genre) a, ul.info-meta li:contains(Genre) a, a[href*='/genre/']"
        ).mapNotNull { it.textOrNull()?.trim() }.filter { it.isNotBlank() }.distinct()
        val status = document.selectFirstOrNull(
            "ul.info li:contains(Status) a, ul.info-meta li:contains(Status) a"
        )?.textOrNull()?.let { parseStatus(it) }

        val chapters = parseChapters(document)

        return NovelDetails(
            url = url, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis,
            tags = genres.ifEmpty { null }, status = status
        )
    }

    /** The details page embeds the complete chapter list. */
    private fun parseChapters(document: Document): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val seen = mutableSetOf<String>()
        val elements = document.select("ul.list-chapter li a, select > option[value*='chapter']")
        for (element in elements) {
            val href = element.attrOrNull("href") ?: element.attrOrNull("value") ?: continue
            if (!href.contains("chapter")) continue
            val chapterUrl = fixUrl(href) ?: continue
            if (!seen.add(chapterUrl)) continue
            val chapterName = element.attrOrNull("title")?.takeIf { it.isNotBlank() }
                ?: element.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                ?: "Chapter ${chapters.size + 1}"
            chapters.add(Chapter(name = chapterName, url = chapterUrl))
        }
        return chapters
    }

    override suspend fun loadChapterContent(url: String): String? {
        val document = get(url).document
        val contentElement = document.selectFirstOrNull("#chr-content, #chapter-content, .chr-c")
            ?: return null
        contentElement.select(
            ".unlock-buttons, .ads, .adsbygoogle, sub, script, style, iframe, " +
                ".ads-holder, .ads-middle, [id*='ads'], [class*='ads'], " +
                ".hidden, [style*='display:none'], [style*='display: none']"
        ).remove()
        val rawHtml = contentElement.html()
            .replace(Regex("\\s{3,}"), "\n\n")
            .replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return rawHtml.trim().takeIf { it.isNotBlank() }
    }
}
