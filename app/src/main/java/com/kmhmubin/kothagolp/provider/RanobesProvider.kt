package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document

class RanobesProvider : MainProvider() {

    override val name = "Ranobes"
    override val mainUrl = "https://ranobes.net"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=ranobes.net&sz=64"
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Popular (Ranking)", "ranking"),
        FilterOption("Latest Updates", "updates"),
        FilterOption("Ongoing", "ongoing"),
        FilterOption("Completed", "completed")
    )

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "Action"),
        FilterOption("Adult", "Adult"),
        FilterOption("Adventure", "Adventure"),
        FilterOption("Comedy", "Comedy"),
        FilterOption("Drama", "Drama"),
        FilterOption("Ecchi", "Ecchi"),
        FilterOption("Fantasy", "Fantasy"),
        FilterOption("Game", "Game"),
        FilterOption("Gender Bender", "Gender Bender"),
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
        FilterOption("Shounen Ai", "Shounen Ai"),
        FilterOption("Slice of Life", "Slice of Life"),
        FilterOption("Smut", "Smut"),
        FilterOption("Sports", "Sports"),
        FilterOption("Supernatural", "Supernatural"),
        FilterOption("Tragedy", "Tragedy"),
        FilterOption("Wuxia", "Wuxia"),
        FilterOption("Xianxia", "Xianxia"),
        FilterOption("Xuanhuan", "Xuanhuan"),
        FilterOption("Yaoi", "Yaoi"),
        FilterOption("Yuri", "Yuri")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val genre = tag?.takeIf { it.isNotBlank() }
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "ranking"

        val url = when {
            !genre.isNullOrBlank() -> {
                val encodedGenre = genre.replace(" ", "%20")
                if (page <= 1) "$mainUrl/tags/genre/$encodedGenre/"
                else "$mainUrl/tags/genre/$encodedGenre/page/$page/"
            }
            sort == "updates" -> "$mainUrl/updates/page/$page/"
            sort == "ongoing" -> "$mainUrl/tags/status-end/Ongoing/${if (page > 1) "page/$page/" else ""}"
            sort == "completed" -> "$mainUrl/tags/status-end/Completed/${if (page > 1) "page/$page/" else ""}"
            else -> if (page <= 1) "$mainUrl/ranking/" else "$mainUrl/ranking/page/$page/"
        }

        val document = get(url).document
        val novels = parseNovels(document)
        val hasNext = document.selectFirstOrNull("div.pages a.next, a[rel=next]") != null
            || document.select("div.pages a").any { it.textOrNull()?.toIntOrNull()?.let { n -> n > page } == true }

        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    private fun parseNovels(document: Document): List<Novel> {
        // Combined selector covers: ranking, updates, genre, search, shortstory pages
        val elements = document.select(
            "article.rank-story, article.block.story.shortstory, " +
            "div.block.story_line.story_line-img, div.short-cont"
        )
        return elements.mapNotNull { el ->
            val link = el.selectFirstOrNull("h2.title a") ?: el.selectFirstOrNull("h3.title a")
                ?: el.selectFirstOrNull("a[href*='/novels/']") ?: return@mapNotNull null
            val title = link.textOrNull()?.trim() ?: return@mapNotNull null
            val href = link.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = if (href.startsWith("http")) href else "$mainUrl$href"
            val cover = el.selectFirstOrNull("figure img")?.attrOrNull("src")
                ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                ?: el.selectFirstOrNull("figure, figure.cover")?.attrOrNull("style")
                    ?.let { extractBgUrl(it) }
                ?: el.selectFirstOrNull("i.image.cover")?.attrOrNull("style")
                    ?.let { extractBgUrl(it) }
            Novel(name = title, url = novelUrl, posterUrl = cover, apiName = this.name)
        }
    }

    override suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$mainUrl/search/$encoded/"
        return try {
            val document = get(url).document
            parseNovels(document)
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document

        val title = document.selectFirstOrNull("h1.title")?.ownText()?.trim() ?: return null

        val cover = document.selectFirstOrNull("div.poster img")?.attrOrNull("src")
            ?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
            ?: document.selectFirstOrNull("div.poster figure.cover")?.attrOrNull("style")
                ?.let { extractBgUrl(it) }

        val author = document.selectFirstOrNull("li:contains(Authors) span.tag_list a")?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("span.tag_list a")?.textOrNull()?.trim()

        val statusCoo = document.selectFirstOrNull("li:contains(Status in COO) a")?.textOrNull()?.lowercase() ?: ""
        val statusTrans = document.selectFirstOrNull("li:contains(Translation) a")?.textOrNull()?.lowercase() ?: ""
        val status = when {
            statusTrans.contains("completed") || statusCoo.contains("completed") -> "Completed"
            statusTrans.contains("ongoing") || statusCoo.contains("ongoing") -> "Ongoing"
            else -> null
        }

        val synopsis = document.selectFirstOrNull("div.moreless__full")?.let { el ->
            el.select("a").remove()
            el.text().replace("Collapse", "").trim()
        } ?: document.selectFirstOrNull("div.moreless__short")?.let { el ->
            el.select("a").remove()
            el.text().replace("Read more", "").trim()
        }

        val genres = document.select("#mc-fs-genre a").mapNotNull { it.textOrNull()?.trim() }

        // Extract novel ID from /chapters/{id}/ links
        val novelId = document.select("a[href*='/chapters/']")
            .mapNotNull { Regex("""/chapters/(\d+)/""").find(it.attrOrNull("href") ?: "")?.groupValues?.get(1) }
            .firstOrNull()
            ?: Regex("""/novels/(\d+)""").find(fullUrl)?.groupValues?.get(1)

        val chapters = if (novelId != null) loadChapters(novelId) else emptyList()

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = cover, synopsis = synopsis,
            tags = genres.ifEmpty { null }, status = status
        )
    }

    private suspend fun loadChapters(novelId: String): List<Chapter> {
        return try {
            val firstUrl = "$mainUrl/chapters/$novelId/"
            val firstDoc = get(firstUrl).document
            val data = extractWindowData(firstDoc)
            val pagesCount = data?.optInt("pages_count", 1) ?: 1
            val chapters = mutableListOf<Chapter>()
            chapters.addAll(parseChaptersFromDoc(firstDoc))

            for (page in 2..pagesCount) {
                try {
                    val pageDoc = get("$mainUrl/chapters/$novelId/page/$page/").document
                    chapters.addAll(parseChaptersFromDoc(pageDoc))
                } catch (_: Throwable) {}
            }

            chapters.reversed()
        } catch (_: Throwable) { emptyList() }
    }

    private fun extractWindowData(document: Document): JSONObject? {
        val script = document.select("script").find { it.data().contains("window.__DATA__") }
            ?.data() ?: return null
        val jsonStr = Regex("""window\.__DATA__\s*=\s*(\{.+?\})\s*;?\s*\n""", RegexOption.DOT_MATCHES_ALL)
            .find(script)?.groupValues?.get(1)
            ?: Regex("""window\.__DATA__\s*=\s*(\{.+\})""", RegexOption.DOT_MATCHES_ALL)
                .find(script)?.groupValues?.get(1)
            ?: return null
        return try { JSONObject(jsonStr) } catch (_: Throwable) { null }
    }

    private fun parseChaptersFromDoc(document: Document): List<Chapter> {
        val data = extractWindowData(document)
        val chaptersJson = data?.optJSONArray("chapters")
        if (chaptersJson != null && chaptersJson.length() > 0) {
            return (0 until chaptersJson.length()).mapNotNull { i ->
                val obj = chaptersJson.optJSONObject(i) ?: return@mapNotNull null
                val chUrl = obj.optString("link", "").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val chTitle = obj.optString("title", "Chapter").takeIf { it.isNotBlank() } ?: "Chapter"
                val date = obj.optString("date", null)
                Chapter(name = chTitle, url = chUrl, dateOfRelease = date)
            }
        }

        // Fallback: HTML chapter links
        return document.select("div.cat_block.cat_line a").mapNotNull { link ->
            val href = link.attrOrNull("href") ?: return@mapNotNull null
            val chTitle = link.selectFirstOrNull("h6.title")?.textOrNull()?.trim()
                ?: link.attrOrNull("title")?.trim()
                ?: return@mapNotNull null
            Chapter(name = chTitle, url = href)
        }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val response = get(fullUrl)
        if (!response.isSuccessful) return null
        val document = response.document

        val title = document.selectFirstOrNull("h1")?.ownText()?.trim()
        val contentEl = document.selectFirstOrNull("div.text#arrticle") ?: return null
        contentEl.select("script, style, .ads, iframe").remove()

        val sb = StringBuilder()
        if (!title.isNullOrBlank()) sb.append("<h2>$title</h2>\n")
        sb.append(contentEl.html().trim())
        return sb.toString().takeIf { it.isNotBlank() }
    }

    private fun extractBgUrl(style: String): String? {
        val path = Regex("""url\(([^)]+)\)""").find(style)?.groupValues?.get(1)?.trim() ?: return null
        return when {
            path.startsWith("http") -> path
            path.startsWith("/") -> "$mainUrl$path"
            else -> "$mainUrl/$path"
        }
    }
}
