package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class FreeWebNovelProvider : MainProvider() {

    override val name = "FreeWebNovel"
    override val mainUrl = "https://freewebnovel.com"
    override val iconRes = R.drawable.ic_provider_freewebnovel
    override val hasMainPage = true

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
        FilterOption("Gender Bender", "Gender%2BBender"),
        FilterOption("Harem", "Harem"),
        FilterOption("Historical", "Historical"),
        FilterOption("Horror", "Horror"),
        FilterOption("Josei", "Josei"),
        FilterOption("Martial Arts", "Martial%2BArts"),
        FilterOption("Mature", "Mature"),
        FilterOption("Mecha", "Mecha"),
        FilterOption("Mystery", "Mystery"),
        FilterOption("Psychological", "Psychological"),
        FilterOption("Reincarnation", "Reincarnation"),
        FilterOption("Romance", "Romance"),
        FilterOption("School Life", "School%2BLife"),
        FilterOption("Sci-fi", "Sci-fi"),
        FilterOption("Seinen", "Seinen"),
        FilterOption("Shoujo", "Shoujo"),
        FilterOption("Shounen", "Shounen"),
        FilterOption("Shounen Ai", "Shounen%2BAi"),
        FilterOption("Slice of Life", "Slice%2Bof%2BLife"),
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
        FilterOption("Latest Release", "sort/latest-release"),
        FilterOption("Latest Novel", "sort/latest-novel"),
        FilterOption("Most Popular", "sort/most-popular"),
        FilterOption("Completed", "sort/completed-novel"),
        FilterOption("Chinese Novel", "sort/latest-release/chinese-novel"),
        FilterOption("Korean Novel", "sort/latest-release/korean-novel"),
        FilterOption("Japanese Novel", "sort/latest-release/japanese-novel"),
        FilterOption("English Novel", "sort/latest-release/english-novel")
    )

    private fun deSlash(url: String): String = if (url.startsWith("/")) url.substring(1) else url

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src") ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image")) return null
        val cleanedSrc = deSlash(rawSrc)
        return if (cleanedSrc.startsWith("http")) cleanedSrc else "$mainUrl/$cleanedSrc"
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
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.select("div.li-row")
        return elements.mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirstOrNull("h3.tit > a") ?: return null
        val name = titleElement.attrOrNull("title") ?: titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null
        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(deSlash(href)) ?: return null
        val imgElement = element.selectFirstOrNull("div.pic > a > img")
        val posterUrl = fixPosterUrl(imgElement)
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "sort/latest-release"
        val url = when {
            !tag.isNullOrEmpty() -> "$mainUrl/genre/$tag/$page"
            sort == "sort/most-popular" -> "$mainUrl/$sort"
            else -> "$mainUrl/$sort/$page"
        }
        val document = get(url).document
        val novels = parseNovels(document)
        val hasNext = when {
            sort == "sort/most-popular" -> false
            else -> document.selectFirstOrNull("a[rel=next], .pagination .next, a:contains(Next)") != null
                || novels.size >= 20
        }
        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search/?searchkey=$encodedQuery"
        val document = get(url).document
        val elements = document.select("div.li-row")
        return elements.mapNotNull { parseNovelElement(it) }
    }

    override suspend fun load(url: String): NovelDetails? {
        val novelPath = deSlash(url.replace(mainUrl, ""))
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$novelPath"
        val document = get(fullUrl).document
        val name = document.selectFirstOrNull("h1.tit")?.textOrNull()?.trim() ?: return null
        val metadata = extractMetadata(document)
        val chapters = loadChapters(document, novelPath)
        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters,
            author = metadata.author, posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis, tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating, peopleVoted = metadata.peopleVoted, status = metadata.status
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null, val synopsis: String? = null,
        val tags: List<String> = emptyList(), val rating: Int? = null,
        val peopleVoted: Int? = null, val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val posterUrl = document.selectFirstOrNull("div.pic > img")?.let { fixPosterUrl(it) }
        val synopsis = document.selectFirstOrNull("div.inner")?.text()?.trim() ?: "No Summary Found"
        val author = document.selectFirstOrNull("span.glyphicon.glyphicon-user")
            ?.nextElementSibling()?.textOrNull()?.trim()
        val tags = document.selectFirstOrNull("span.glyphicon.glyphicon-th-list")
            ?.nextElementSiblings()?.getOrNull(0)?.text()?.split(",")
            ?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val statusElement = document.selectFirstOrNull("span.s1.s2, span.s1.s3")
        val status = statusElement?.selectFirstOrNull("a")?.textOrNull()?.let { parseStatus(it) }
        var rating: Int? = null
        var peopleVoted: Int? = null
        try {
            val ratingText = document.selectFirstOrNull("div.m-desc > div.score > p:nth-child(2)")?.textOrNull()
            if (ratingText != null) {
                val ratingValue = ratingText.substringBefore("/").trim().toFloatOrNull()
                if (ratingValue != null) {
                    // from 5 stars -> 0..1000
                    rating = (ratingValue / 5f * 1000f).toInt().coerceIn(0, 1000)
                }
                val votedMatch = Regex("\\((\\d+)\\)").find(ratingText)
                peopleVoted = votedMatch?.groupValues?.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
            }
        } catch (_: Exception) {}
        return NovelMetadata(author, posterUrl, synopsis, tags, rating, peopleVoted, status)
    }

    /**
     * The site paginates the chapter list (window.chapterPagination, 40/page),
     * so the details page HTML only carries the first page. Full list comes
     * from the chapterlist API, with the paginated ajax endpoint as backup.
     */
    private suspend fun loadChapters(document: Document, novelPath: String): List<Chapter> {
        val totalChapters = parseTotalChapters(document)
        val ajaxChapters = tryLoadChaptersViaAjax(document, novelPath)
        if (ajaxChapters.isNotEmpty() && ajaxChapters.size >= (totalChapters ?: 0)) return ajaxChapters
        val paginatedChapters = tryLoadChaptersPaginated(novelPath, totalChapters)
        if (paginatedChapters.size > ajaxChapters.size) return paginatedChapters
        if (ajaxChapters.isNotEmpty()) return ajaxChapters
        return loadChaptersFromHtml(document)
    }

    /** Reads totalChapters from the inline `window.chapterPagination` script. */
    private fun parseTotalChapters(document: Document): Int? {
        val scriptText = document.select("script").joinToString("\n") { it.html() }
        return Regex("totalChapters\\s*:\\s*(\\d+)").find(scriptText)
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private suspend fun tryLoadChaptersViaAjax(document: Document, novelPath: String): List<Chapter> {
        return try {
            val scriptText = document.select("script").joinToString("\n") { it.html() }
            val aidMatch = Regex("(\\d+)s\\.jpg").find(scriptText)
            val aid = aidMatch?.groupValues?.getOrNull(1) ?: return emptyList()
            // acode is the novel slug; the old script-scraping regex broke when
            // the site markup changed, so derive it from the novel URL instead.
            val acode = novelPath.substringAfterLast("/").ifBlank { return emptyList() }
            val ajaxUrl = "$mainUrl/api/chapterlist.php"
            val response = post(url = ajaxUrl, data = mapOf("acode" to acode, "aid" to aid))
            val html = response.text.replace("""\\""", "")
            val parsed = Jsoup.parse(html)
            val options = parsed.select("option")
            val chapters = mutableListOf<Chapter>()
            for (option in options) {
                val value = option.attrOrNull("value") ?: continue
                // Broken links like /novel//chapter-1 mean the acode was wrong
                if (value.contains("//")) return emptyList()
                val chapterUrl = fixUrl(deSlash(value)) ?: continue
                val chapterName = option.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Chapter ${chapters.size + 1}"
                chapters.add(Chapter(name = chapterName, url = chapterUrl))
            }
            chapters
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Walks the site's own pagination endpoint
     * (`<novelUrl>?ajax=chapters&page=N&pageSize=200`, JSON `{code, html}`).
     * Server caps pageSize at 200.
     */
    private suspend fun tryLoadChaptersPaginated(novelPath: String, totalChapters: Int?): List<Chapter> {
        return try {
            val pageSize = 200
            val maxPages = totalChapters?.let { (it + pageSize - 1) / pageSize } ?: 100
            val chapters = mutableListOf<Chapter>()
            val seenUrls = mutableSetOf<String>()
            for (page in 1..maxPages) {
                val responseText = get("$mainUrl/$novelPath?ajax=chapters&page=$page&pageSize=$pageSize").text
                val html = try {
                    org.json.JSONObject(responseText).optString("html", "")
                } catch (_: Exception) {
                    responseText
                }
                if (html.isBlank()) break
                val links = Jsoup.parse(html).select("a[href*=chapter]")
                var added = false
                for (link in links) {
                    val href = link.attrOrNull("href") ?: continue
                    val chapterUrl = fixUrl(deSlash(href)) ?: continue
                    if (!seenUrls.add(chapterUrl)) continue
                    val chapterName = link.attrOrNull("title")?.takeIf { it.isNotBlank() }
                        ?: link.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Chapter ${chapters.size + 1}"
                    chapters.add(Chapter(name = chapterName, url = chapterUrl))
                    added = true
                }
                if (!added) break
            }
            chapters
        } catch (_: Exception) { emptyList() }
    }

    private fun loadChaptersFromHtml(document: Document): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val chapterElements = document.select("ul#idData li, ul.chapter-list li")
        for (element in chapterElements) {
            val linkElement = element.selectFirstOrNull("a") ?: continue
            val href = linkElement.attrOrNull("href") ?: continue
            val chapterUrl = fixUrl(deSlash(href)) ?: continue
            val chapterName = linkElement.attrOrNull("title") ?: linkElement.textOrNull()?.trim()
                ?: "Chapter ${chapters.size + 1}"
            chapters.add(Chapter(name = chapterName, url = chapterUrl))
        }
        return chapters
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val response = get(fullUrl)
        val cleanedHtml = response.text
            .replace("New novel chapters are published on Freewebnovel.com.", "")
            .replace("The source of this content is Freewebnᴏvel.com.", "")
            .replace("☞ We are moving Freewebnovel.com to Libread.com, Please visit libread.com for more chapters! ☜", "")
        val document = Jsoup.parse(cleanedHtml)
        document.select("div.txt > .notice-text").remove()
        val contentElement = document.selectFirstOrNull("div.txt") ?: return null
        contentElement.select(".ads, .adsbygoogle, script, style, .ads-holder, .ads-middle, [id*='ads'], [class*='ads']").remove()
        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}
