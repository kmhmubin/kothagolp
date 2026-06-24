package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class RoyalRoadProvider : MainProvider() {

    override val name = "Royal Road"
    override val mainUrl = "https://www.royalroad.com"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=royalroad.com&sz=64"
    override val hasMainPage = true
    override val rateLimitTime: Long = 500L

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Contemporary", "contemporary"),
        FilterOption("Drama", "drama"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Romance", "romance"),
        FilterOption("Satire", "satire"),
        FilterOption("Sci-fi", "sci_fi"),
        FilterOption("Short Story", "one_shot"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Anti-Hero Lead", "anti-hero_lead"),
        FilterOption("Dungeon", "dungeon"),
        FilterOption("Female Lead", "female_lead"),
        FilterOption("GameLit", "gamelit"),
        FilterOption("Gender Bender", "gender_bender"),
        FilterOption("Grimdark", "grimdark"),
        FilterOption("Harem", "harem"),
        FilterOption("High Fantasy", "high_fantasy"),
        FilterOption("LitRPG", "litrpg"),
        FilterOption("Magic", "magic"),
        FilterOption("Male Lead", "male_lead"),
        FilterOption("Martial Arts", "martial_arts"),
        FilterOption("Mythos", "mythos"),
        FilterOption("Portal Fantasy / Isekai", "summoned_hero"),
        FilterOption("Post Apocalyptic", "post_apocalyptic"),
        FilterOption("Progression", "progression"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("School Life", "school_life"),
        FilterOption("Slice of Life", "slice_of_life"),
        FilterOption("Space Opera", "space_opera"),
        FilterOption("Sports", "sports"),
        FilterOption("Steampunk", "steampunk"),
        FilterOption("Super Heroes", "super_heroes"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Time Loop", "loop"),
        FilterOption("Time Travel", "time_travel"),
        FilterOption("Urban Fantasy", "urban_fantasy"),
        FilterOption("Virtual Reality", "virtual_reality"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia")
    )

    override val orderBys = listOf(
        FilterOption("Best Rated", "best-rated"),
        FilterOption("Ongoing", "active-popular"),
        FilterOption("Completed", "complete"),
        FilterOption("Popular this week", "weekly-popular"),
        FilterOption("Latest Updates", "latest-updates"),
        FilterOption("New Releases", "new-releases"),
        FilterOption("Trending", "trending"),
        FilterOption("Rising Stars", "rising-stars")
    )

    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null
        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "hiatus", "on hiatus" -> "On Hiatus"
            "dropped", "stub" -> "Dropped"
            else -> null
        }
    }

    private fun extractFictionId(responseText: String): Int? {
        return try {
            responseText.substringAfter("window.fictionId = ").substringBefore(";").trim().toIntOrNull()
        } catch (_: Throwable) { null }
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val order = orderBy.takeUnless { it.isNullOrEmpty() } ?: "best-rated"
        if (page > 1 && (order == "trending" || order == "rising-stars")) {
            return MainPageResult(url = "", novels = emptyList())
        }
        val tagParam = if (tag.isNullOrEmpty()) "" else "&genre=$tag"
        val url = "$mainUrl/fictions/$order?page=$page$tagParam"
        val document = get(url).document
        val novels = document.select("div.fiction-list-item").mapNotNull { parseNovelFromList(it, order) }
        return MainPageResult(url = url, novels = novels)
    }

    private fun parseNovelFromList(element: Element, orderBy: String? = null): Novel? {
        val head = element.selectFirstOrNull("> div") ?: return null
        val titleLink = head.selectFirstOrNull("> h2.fiction-title > a") ?: return null
        val name = titleLink.textOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val href = titleLink.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(href) ?: return null
        val posterUrl = element.selectFirstOrNull("> figure > a > img")?.attrOrNull("src")
        val latestChapter = try {
            if (orderBy == "latest-updates")
                head.selectFirstOrNull("> ul.list-unstyled > li.list-item > a > span")?.textOrNull()
            else null
        } catch (_: Throwable) { null }
        val rating = head.selectFirstOrNull("> div.stats")
            ?.select("> div")?.getOrNull(1)
            ?.selectFirstOrNull("> span")
            ?.attrOrNull("title")
            ?.toFloatOrNull()
            ?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
        return Novel(name = name, url = novelUrl, posterUrl = posterUrl, latestChapter = latestChapter, rating = rating, apiName = this.name)
    }

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/fictions/search?page=1&title=$encodedQuery&globalFilters=true"
        val document = get(url).document
        return document.select("div.fiction-list-item").mapNotNull { element ->
            val head = element.selectFirstOrNull("> div.search-content")
                ?: element.selectFirstOrNull("> div")
                ?: return@mapNotNull null
            val titleLink = head.selectFirstOrNull("> h2.fiction-title > a") ?: return@mapNotNull null
            val name = titleLink.textOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val href = titleLink.attrOrNull("href") ?: return@mapNotNull null
            val novelUrl = fixUrl(href) ?: return@mapNotNull null
            val posterUrl = element.selectFirstOrNull("> figure.text-center > a > img")?.attrOrNull("src")
                ?: element.selectFirstOrNull("> figure > a > img")?.attrOrNull("src")
            val rating = head.selectFirstOrNull("> div.stats")
                ?.select("> div")?.getOrNull(1)
                ?.selectFirstOrNull("> span")
                ?.attrOrNull("title")
                ?.toFloatOrNull()
                ?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
            Novel(name = name, url = novelUrl, posterUrl = posterUrl, rating = rating, apiName = this.name)
        }
    }

    private suspend fun loadRelatedNovels(fictionId: Int?): List<Novel> {
        if (fictionId == null) return emptyList()
        return try {
            val url = "$mainUrl/fictions/similar?fictionId=$fictionId"
            val jsonArray = JSONArray(get(url).text)
            val novels = mutableListOf<Novel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", null) ?: continue
                val novelUrl = obj.optString("url", null) ?: continue
                val cover = obj.optString("cover", null)
                novels.add(Novel(
                    name = title, url = fixUrl(novelUrl) ?: continue,
                    posterUrl = cover?.let { fixUrl(it) }, apiName = this.name
                ))
            }
            novels
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val response = get(fullUrl)
        val document = response.document
        val responseText = response.text
        val name = document.selectFirstOrNull("h1.font-white")?.textOrNull()?.trim() ?: return null
        val fictionId = extractFictionId(responseText)
        val chapters = parseChaptersFromScript(responseText) ?: parseChaptersFromTable(document)
        val relatedNovels = loadRelatedNovels(fictionId)
        val metadata = extractMetadata(document)
        val views = extractViews(document)
        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters,
            author = metadata.author, posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis, tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating, peopleVoted = metadata.peopleVoted,
            status = metadata.status, views = views,
            relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    private fun parseChaptersFromScript(responseText: String): List<Chapter>? {
        return try {
            val chaptersMatch = Regex("""window\.chapters\s*=\s*(\[.*?\]);""", RegexOption.DOT_MATCHES_ALL)
                .find(responseText) ?: return null
            val jsonArray = JSONArray(chaptersMatch.groupValues[1])
            val chapters = mutableListOf<Chapter>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", null) ?: continue
                val chapterUrl = obj.optString("url", null) ?: continue
                val date = obj.optString("date", null)
                val urlParts = chapterUrl.split("/")
                val chapterPath = if (urlParts.size >= 6)
                    "${urlParts[1]}/${urlParts[2]}/${urlParts[4]}/${urlParts[5]}"
                else chapterUrl.removePrefix("/")
                chapters.add(Chapter(name = title, url = fixUrl(chapterPath) ?: continue, dateOfRelease = date))
            }
            chapters
        } catch (_: Throwable) { null }
    }

    private fun parseChaptersFromTable(document: Document): List<Chapter> {
        return document.select("div.portlet-body > table > tbody > tr").mapNotNull { row ->
            val chapterUrl = row.attrOrNull("data-url") ?: return@mapNotNull null
            val cells = row.select("> td")
            val chapterName = cells.getOrNull(0)?.selectFirstOrNull("> a")?.textOrNull()?.trim()
                ?: return@mapNotNull null
            val dateOfRelease = cells.getOrNull(1)?.selectFirstOrNull("> a > time")?.textOrNull()?.trim()
            Chapter(name = chapterName, url = fixUrl(chapterUrl) ?: return@mapNotNull null, dateOfRelease = dateOfRelease)
        }
    }

    private data class NovelMetadata(
        val author: String? = null, val posterUrl: String? = null,
        val synopsis: String? = null, val tags: List<String> = emptyList(),
        val rating: Int? = null, val peopleVoted: Int? = null, val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val author = document.selectFirstOrNull("h4.font-white > span > a")?.textOrNull()?.trim()
        val posterUrl = document.selectFirstOrNull("div.fic-header > div > .cover-art-container > img")?.attrOrNull("src")
        val synoDescript = document.selectFirstOrNull("div.description > div")
        val synoParts = synoDescript?.select("> p")
        val synopsis = if (synoParts.isNullOrEmpty() && synoDescript?.hasText() == true) {
            synoDescript.text().replace("\n", "\n\n")
        } else {
            synoParts?.joinToString(separator = "\n\n") { it.text() }
        }
        val tags = document.select("span.tags > a").mapNotNull { it.textOrNull()?.trim() }.filter { it.isNotBlank() }
        var status: String? = null
        for (s in document.select("div.col-md-8 > div.margin-bottom-10 > span.label")) {
            val parsed = parseStatus(s.textOrNull()?.trim())
            if (parsed != null) { status = parsed; break }
        }
        val ratingAttr = document.selectFirstOrNull("span.font-red-sunglo")?.attrOrNull("data-content")
        val rating = try {
            ratingAttr?.substringBefore('/')?.trim()?.toFloatOrNull()?.let { (it / 5f * 1000f).toInt().coerceIn(0, 1000) }
        } catch (_: Throwable) { null }
        val peopleVoted = try {
            document.selectFirstOrNull("span.font-red-sunglo")
                ?.attrOrNull("data-original-title")
                ?.let { title -> Regex("([\\d,]+)\\s*rating").find(title)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull() }
        } catch (_: Throwable) { null }
        return NovelMetadata(author, posterUrl, synopsis?.takeIf { it.isNotBlank() }, tags, rating, peopleVoted, status)
    }

    private fun extractViews(document: Document): Int? {
        return try {
            val statsList = document.select("ul.list-unstyled").getOrNull(1)
            statsList?.select("> li")?.getOrNull(1)?.textOrNull()
                ?.replace(",", "")?.replace(".", "")?.filter { it.isDigit() }?.toIntOrNull()
        } catch (_: Throwable) { null }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val response = get(fullUrl)
        val document = response.document
        val responseText = response.text
        val chapterContent = document.selectFirstOrNull("div.chapter-content") ?: return null
        // Remove hidden elements using CSS style regex (from LNReader technique)
        try {
            val styleRegex = Regex("""<style>\s+\.(.+?)\{[^{]+?display:\s*none;""", RegexOption.MULTILINE)
            val hiddenClass = styleRegex.find(responseText)?.groupValues?.getOrNull(1)?.trim()
            if (!hiddenClass.isNullOrBlank()) chapterContent.select(".$hiddenClass").remove()
        } catch (_: Throwable) {}
        // Author notes
        val notesBefore = StringBuilder()
        val notesAfter = StringBuilder()
        try {
            val chapterParent = chapterContent.parent()
            if (chapterParent != null) {
                document.select("div.author-note").forEach { authorNote ->
                    val noteContainer = authorNote.parent() ?: return@forEach
                    val noteParent = noteContainer.parent() ?: return@forEach
                    if (noteParent == chapterParent) {
                        val isBefore = noteContainer.elementSiblingIndex() < chapterContent.elementSiblingIndex()
                        val noteContent = authorNote.html().trim()
                        if (noteContent.isNotBlank()) {
                            if (isBefore) notesBefore.append(noteContent) else notesAfter.append(noteContent)
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        val parts = mutableListOf<String>()
        if (notesBefore.isNotBlank()) parts.add("""<div class="author-note-before">${notesBefore}</div>""")
        parts.add(chapterContent.html())
        if (notesAfter.isNotBlank()) parts.add("""<div class="author-note-after">${notesAfter}</div>""")
        return parts.joinToString(separator = "\n<hr class=\"notes-separator\">\n")
    }
}
