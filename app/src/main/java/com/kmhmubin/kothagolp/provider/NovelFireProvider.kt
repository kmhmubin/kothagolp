package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class NovelFireProvider : MainProvider() {

    override val name = "NovelFire"
    override val mainUrl = "https://novelfire.net"
    override val iconRes = R.drawable.ic_provider_novelfire
    override val hasMainPage = true

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "3"),
        FilterOption("Adult", "28"),
        FilterOption("Adventure", "4"),
        FilterOption("Comedy", "5"),
        FilterOption("Drama", "24"),
        FilterOption("Eastern", "44"),
        FilterOption("Ecchi", "26"),
        FilterOption("Fantasy", "6"),
        FilterOption("Game", "19"),
        FilterOption("Gender Bender", "25"),
        FilterOption("Harem", "7"),
        FilterOption("Historical", "12"),
        FilterOption("Horror", "37"),
        FilterOption("Isekai", "49"),
        FilterOption("Josei", "2"),
        FilterOption("Magic", "50"),
        FilterOption("Martial Arts", "15"),
        FilterOption("Mature", "8"),
        FilterOption("Mecha", "34"),
        FilterOption("Mystery", "16"),
        FilterOption("Psychological", "9"),
        FilterOption("Reincarnation", "43"),
        FilterOption("Romance", "1"),
        FilterOption("School Life", "21"),
        FilterOption("Sci-fi", "20"),
        FilterOption("Seinen", "10"),
        FilterOption("Shoujo", "38"),
        FilterOption("Shounen", "17"),
        FilterOption("Slice of Life", "13"),
        FilterOption("Smut", "29"),
        FilterOption("Sports", "42"),
        FilterOption("Supernatural", "18"),
        FilterOption("System", "58"),
        FilterOption("Tragedy", "32"),
        FilterOption("Urban", "63"),
        FilterOption("Wuxia", "31"),
        FilterOption("Xianxia", "23"),
        FilterOption("Xuanhuan", "22"),
        FilterOption("Yaoi", "14"),
        FilterOption("Yuri", "62")
    )

    override val orderBys = listOf(
        FilterOption("Rank (Top)", "rank-top"),
        FilterOption("Rating Score (Top)", "rating-score-top"),
        FilterOption("Last Updated (Newest)", "date"),
        FilterOption("Total Views (Most)", "total-view"),
        FilterOption("Monthly Views (Most)", "monthly-view"),
        FilterOption("Chapter Count (Most)", "chapter-count-most")
    )

    private object Selectors {
        val novelContainers = listOf(".novel-item", ".novel-list .novel-item")
        val novelTitle = listOf(".novel-title > a", "a[title]")
        val novelDetailTitle = listOf(".novel-title", ".cover > img[alt]")
        val chapterContent = listOf("#content", ".chapter-content")
        val synopsis = listOf(".summary .content", ".description")
        val poster = listOf(".cover > img", ".novel-cover > img")
        val author = listOf(".author .property-item > span", ".author span")
        val genres = listOf(".categories .property-item", ".genres a")
        val rating = listOf(".nub", ".rating-value")
        val views = listOf(".header-stats span:has(i.icon-eye) strong")
    }

    private fun Document.selectFirst(selectors: List<String>): Element? {
        for (s in selectors) { val e = this.selectFirstOrNull(s); if (e != null) return e }
        return null
    }
    private fun Element.selectFirst(selectors: List<String>): Element? {
        for (s in selectors) { val e = this.selectFirstOrNull(s); if (e != null) return e }
        return null
    }
    private fun Document.selectAny(selectors: List<String>): Elements {
        for (s in selectors) { val e = this.select(s); if (e.isNotEmpty()) return e }
        return Elements()
    }

    private fun deSlash(url: String): String = if (url.startsWith("/")) url.substring(1) else url

    private fun fixPosterUrl(imgElement: Element?): String? {
        if (imgElement == null) return null
        val rawSrc = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src") ?: return null
        if (rawSrc.isBlank() || rawSrc.contains("data:image/gif")) return null
        val cleanedSrc = deSlash(rawSrc)
        return if (cleanedSrc.startsWith("http")) cleanedSrc else "$mainUrl/$cleanedSrc"
    }

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "hiatus", "on hiatus" -> "On Hiatus"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun parseViewCountToInt(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.replace(Regex("[^0-9.KMBkmb]"), "").trim().uppercase()
        return when {
            cleaned.endsWith("K") -> cleaned.dropLast(1).toFloatOrNull()?.times(1_000)?.toInt()
            cleaned.endsWith("M") -> cleaned.dropLast(1).toFloatOrNull()?.times(1_000_000)?.toInt()
            else -> cleaned.replace(".", "").toIntOrNull()
        }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace(Regex("\\s{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(<br\\s*/?>\\s*){3,}"), "<br/><br/>")
        return cleaned.trim()
    }

    private fun extractNovelSlug(url: String): String {
        return url.replace(mainUrl, "").replace("$mainUrl/", "")
            .removePrefix("/").removePrefix("book/").removeSuffix("/").split("/").firstOrNull() ?: url
    }

    private fun parseNovels(document: Document): List<Novel> {
        val elements = document.selectAny(Selectors.novelContainers)
        return elements.mapNotNull { parseNovelElement(it) }
    }

    private fun parseNovelElement(element: Element): Novel? {
        val titleElement = element.selectFirst(Selectors.novelTitle) ?: return null
        val name = titleElement.attrOrNull("title") ?: titleElement.textOrNull()?.trim()
        if (name.isNullOrBlank()) return null
        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(deSlash(href.replace(mainUrl, "").replace("$mainUrl/", ""))) ?: return null
        val imgElement = element.selectFirstOrNull(".novel-cover > img") ?: element.selectFirstOrNull("img")
        val posterUrl = fixPosterUrl(imgElement)
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val params = mutableListOf<String>()
        if (!tag.isNullOrEmpty()) params.add("categories[]=$tag")
        params.add("ctgcon=and")
        params.add("totalchapter=0")
        params.add("ratcon=min")
        params.add("rating=0")
        params.add("status=-1")
        val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "rank-top"
        params.add("sort=$sort")
        params.add("page=$page")
        val url = "$mainUrl/search-adv?${params.joinToString("&")}"
        val document = get(url).document
        val novels = parseNovels(document)
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search?keyword=$encodedQuery&page=1"
        val document = get(url).document
        val elements = document.select(".novel-list.chapters .novel-item")
        return elements.mapNotNull { element ->
            val linkElement = element.selectFirstOrNull("a") ?: return@mapNotNull null
            val name = linkElement.attrOrNull("title") ?: linkElement.textOrNull()?.trim()
            if (name.isNullOrBlank()) return@mapNotNull null
            val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(deSlash(href.replace(mainUrl, "").replace("$mainUrl/", ""))) ?: return@mapNotNull null
            val imgElement = element.selectFirstOrNull(".novel-cover > img") ?: element.selectFirstOrNull("img")
            val posterUrl = fixPosterUrl(imgElement)
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        val novelPath = deSlash(url.replace(mainUrl, "").replace("$mainUrl/", ""))
        val novelSlug = extractNovelSlug(novelPath)
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$novelPath"
        val response = get(fullUrl)
        val document = response.document
        val name = document.selectFirst(Selectors.novelDetailTitle)?.textOrNull()?.trim()
            ?: document.selectFirstOrNull(".cover > img")?.attrOrNull("alt") ?: "Unknown"
        val postId = document.selectFirstOrNull("#novel-report[report-post_id]")
            ?.attrOrNull("report-post_id")
        val chapters = loadChaptersWithDates(novelSlug, postId)
        val metadata = extractMetadata(document)
        val relatedNovels = loadRelatedNovels(postId)
        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters, author = metadata.author,
            posterUrl = metadata.posterUrl, synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null }, rating = metadata.rating,
            status = metadata.status, views = metadata.views,
            relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null, val synopsis: String? = null,
        val tags: List<String> = emptyList(), val rating: Int? = null,
        val status: String? = null, val views: Int? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val author = document.selectFirst(Selectors.author)?.textOrNull()?.trim()
        val posterUrl = document.selectFirst(Selectors.poster)?.let { imgElement ->
            val src = imgElement.attrOrNull("data-src") ?: imgElement.attrOrNull("src")
            if (!src.isNullOrBlank() && !src.contains("data:image")) {
                if (src.startsWith("http")) src else "$mainUrl/${deSlash(src)}"
            } else null
        }
        val synopsis = document.selectFirst(Selectors.synopsis)?.let { element ->
            element.text().replace("Show More", "").trim().takeIf { it.isNotBlank() }
        } ?: "No Summary Found"
        val tags = document.selectAny(Selectors.genres)
            .mapNotNull { it.textOrNull()?.trim() }.filter { it.isNotBlank() }.distinct()
        val statusText = document.selectFirstOrNull(".header-stats .ongoing")?.textOrNull()
            ?: document.selectFirstOrNull(".header-stats .completed")?.textOrNull()
        val status = parseStatus(statusText)
        val ratingText = document.selectFirst(Selectors.rating)?.textOrNull()
        val rating = ratingText?.toFloatOrNull()?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
        val viewsText = document.selectFirst(Selectors.views)?.textOrNull()?.trim()
        val views = parseViewCountToInt(viewsText)
        return NovelMetadata(author, posterUrl, synopsis, tags, rating, status, views)
    }

    private suspend fun loadRelatedNovels(postId: String?): List<Novel> {
        if (postId.isNullOrBlank()) return emptyList()
        return try {
            val url = "$mainUrl/ajax/novelYouMayLike?post_id=$postId"
            val response = get(url, mapOf("Accept" to "application/json", "X-Requested-With" to "XMLHttpRequest"))
            val json = JSONObject(response.text)
            val html = json.optString("html", "")
            if (html.isBlank()) return emptyList()
            val document = Jsoup.parse(html)
            document.select("li.novel-item").mapNotNull { item ->
                val linkElement = item.selectFirstOrNull("a") ?: return@mapNotNull null
                val href = linkElement.attrOrNull("href") ?: return@mapNotNull null
                val title = item.selectFirstOrNull("h5.novel-title")?.textOrNull()?.trim() ?: return@mapNotNull null
                val imgElement = item.selectFirstOrNull("figure.novel-cover img")
                val posterUrl = fixPosterUrl(imgElement)
                val novelUrl = fixUrl(deSlash(href.removePrefix(mainUrl).removePrefix("/"))) ?: return@mapNotNull null
                Novel(name = title, url = novelUrl, posterUrl = posterUrl, apiName = this.name)
            }
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun loadChaptersWithDates(novelSlug: String, postId: String?): List<Chapter> {
        if (postId.isNullOrBlank()) return loadChaptersFromHtml(novelSlug)
        try {
            val ajaxUrl = "$mainUrl/listChapterDataAjax?post_id=$postId"
            val response = get(ajaxUrl)
            val responseText = response.text
            if (responseText.contains("rate limited") || responseText.contains("Cloudflare") || responseText.contains("Page Not Found"))
                return loadChaptersFromHtml(novelSlug)
            val json = JSONObject(responseText)
            val dataArray = json.optJSONArray("data")
            val recordsTotal = json.optInt("recordsTotal", 0)
            if (dataArray == null || dataArray.length() == 0) return loadChaptersFromHtml(novelSlug)
            if (dataArray.length() < recordsTotal) return loadChaptersFromHtml(novelSlug)
            val chapters = mutableListOf<Chapter>()
            for (i in 0 until dataArray.length()) {
                val chapterObj = dataArray.getJSONObject(i)
                val nSort = chapterObj.optInt("n_sort", i + 1)
                val title = chapterObj.optString("title", "")
                val slug = chapterObj.optString("slug", "")
                val createdAt = chapterObj.optString("created_at", null)
                val chapterName = if (title.isNotBlank()) Jsoup.parse(title).text()
                    else if (slug.isNotBlank()) Jsoup.parse(slug).text() else "Chapter $nSort"
                val chapterPath = "book/$novelSlug/chapter-$nSort"
                val chapterUrl = fixUrl(chapterPath) ?: continue
                chapters.add(Chapter(name = chapterName, url = chapterUrl, dateOfRelease = createdAt))
            }
            chapters.sortBy { chapter ->
                Regex("chapter-(\\d+)").find(chapter.url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }
            return chapters
        } catch (_: Exception) { return loadChaptersFromHtml(novelSlug) }
    }

    private suspend fun loadChaptersFromHtml(novelSlug: String): List<Chapter> {
        try {
            val firstPageUrl = "$mainUrl/book/$novelSlug/chapters"
            val firstPageDoc = get(firstPageUrl).document
            val pagination = firstPageDoc.selectFirstOrNull("div.pagenav div.pagination-container nav ul.pagination")
            if (pagination != null) {
                val pageItems = pagination.select("li")
                val lastPageNumber = pageItems.getOrNull(pageItems.size - 2)?.text()?.toIntOrNull() ?: 1
                if (lastPageNumber > 1) {
                    val lastPageDoc = get("$mainUrl/book/$novelSlug/chapters?page=$lastPageNumber").document
                    val lastChapterLink = lastPageDoc.select("ul.chapter-list li a").lastOrNull()?.attr("href") ?: ""
                    val totalChapters = Regex("chapter-(\\d+)").find(lastChapterLink)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    if (totalChapters != null && totalChapters > 0) {
                        return (1..totalChapters).map { chapterNumber ->
                            Chapter(
                                name = "Chapter $chapterNumber",
                                url = fixUrl("book/$novelSlug/chapter-$chapterNumber") ?: "$mainUrl/book/$novelSlug/chapter-$chapterNumber"
                            )
                        }
                    }
                }
            }
            val chapters = mutableListOf<Chapter>()
            val seenUrls = mutableSetOf<String>()
            val chapterItems = firstPageDoc.select("ul.chapter-list li")
            for (item in chapterItems) {
                val linkElement = item.selectFirstOrNull("a") ?: continue
                val href = linkElement.attrOrNull("href") ?: continue
                if (!href.contains("/chapter-")) continue
                val titleAttr = linkElement.attrOrNull("title")
                val chapterTitle = titleAttr ?: linkElement.textOrNull()?.trim() ?: continue
                val timeElement = item.selectFirstOrNull("time.chapter-update")
                val dateOfRelease = timeElement?.textOrNull()?.trim()
                val chapterUrl = fixUrl(deSlash(href.removePrefix(mainUrl).removePrefix("/"))) ?: continue
                if (seenUrls.add(chapterUrl)) {
                    chapters.add(Chapter(name = chapterTitle, url = chapterUrl, dateOfRelease = dateOfRelease))
                }
            }
            chapters.sortBy { chapter ->
                Regex("chapter-(\\d+)").find(chapter.url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }
            return chapters
        } catch (_: Exception) { return emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val document = get(fullUrl).document
        val contentElement = document.selectFirst(Selectors.chapterContent) ?: return null
        contentElement.select(
            ".ads, .adsbygoogle, script, style, .ads-holder, .ads-middle, " +
                "[id*='ads'], [class*='ads'], .hidden, [style*='display:none'], [style*='display: none']"
        ).remove()
        val rawHtml = contentElement.html()
        return cleanChapterHtml(rawHtml)
    }
}
