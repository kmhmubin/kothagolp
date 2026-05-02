package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReviewScore
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.util.RatingUtils
import com.emptycastle.novery.util.toRelativeTime
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Provider for RoyalRoad.com
 * Based on LNReader implementation v2.3.0
 */
class RoyalRoadProvider : MainProvider() {

    override val name = "Royal Road"
    override val mainUrl = "https://www.royalroad.com"
    override val hasMainPage = true
    override val hasReviews = true
    override val iconRes: Int = R.drawable.ic_provider_royalroad
    override val rateLimitTime: Long = 500L

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", ""),
        // Genres
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
        // Tags
        FilterOption("Anti-Hero Lead", "anti-hero_lead"),
        FilterOption("Artificial Intelligence", "artificial_intelligence"),
        FilterOption("Attractive Lead", "attractive_lead"),
        FilterOption("Cyberpunk", "cyberpunk"),
        FilterOption("Dungeon", "dungeon"),
        FilterOption("Dystopia", "dystopia"),
        FilterOption("Female Lead", "female_lead"),
        FilterOption("First Contact", "first_contact"),
        FilterOption("GameLit", "gamelit"),
        FilterOption("Gender Bender", "gender_bender"),
        FilterOption("Genetically Engineered", "genetically_engineered"),
        FilterOption("Grimdark", "grimdark"),
        FilterOption("Hard Sci-fi", "hard_sci-fi"),
        FilterOption("Harem", "harem"),
        FilterOption("High Fantasy", "high_fantasy"),
        FilterOption("LitRPG", "litrpg"),
        FilterOption("Low Fantasy", "low_fantasy"),
        FilterOption("Magic", "magic"),
        FilterOption("Male Lead", "male_lead"),
        FilterOption("Martial Arts", "martial_arts"),
        FilterOption("Multiple Lead Characters", "multiple_lead"),
        FilterOption("Mythos", "mythos"),
        FilterOption("Non-Human Lead", "non-human_lead"),
        FilterOption("Portal Fantasy / Isekai", "summoned_hero"),
        FilterOption("Post Apocalyptic", "post_apocalyptic"),
        FilterOption("Progression", "progression"),
        FilterOption("Reader Interactive", "reader_interactive"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Ruling Class", "ruling_class"),
        FilterOption("School Life", "school_life"),
        FilterOption("Secret Identity", "secret_identity"),
        FilterOption("Slice of Life", "slice_of_life"),
        FilterOption("Soft Sci-fi", "soft_sci-fi"),
        FilterOption("Space Opera", "space_opera"),
        FilterOption("Sports", "sports"),
        FilterOption("Steampunk", "steampunk"),
        FilterOption("Strategy", "strategy"),
        FilterOption("Strong Lead", "strong_lead"),
        FilterOption("Super Heroes", "super_heroes"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Technologically Engineered", "technologically_engineered"),
        FilterOption("Time Loop", "loop"),
        FilterOption("Time Travel", "time_travel"),
        FilterOption("Urban Fantasy", "urban_fantasy"),
        FilterOption("Villainous Lead", "villainous_lead"),
        FilterOption("Virtual Reality", "virtual_reality"),
        FilterOption("War and Military", "war_and_military"),
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
        FilterOption("Rising Stars", "rising-stars"),
        FilterOption("Writathon", "writathon")
    )

    // ================================================================
    // UTILITY METHODS
    // ================================================================

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
            responseText
                .substringAfter("window.fictionId = ")
                .substringBefore(";")
                .trim()
                .toIntOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseStarScore(ariaLabel: String?): Int? {
        return ariaLabel?.replace("stars", "")?.trim()?.toFloatOrNull()?.times(200)?.toInt()
    }

    private fun parseStarScoreFromClass(element: Element?): Int? {
        if (element == null) return null
        val starClasses = listOf(50, 45, 40, 35, 30, 25, 20, 15, 10, 5)
        for (stars in starClasses) {
            if (element.hasClass("star-$stars")) {
                return stars * 20
            }
        }
        return null
    }

    /**
     * Convert date string to relative time format
     * Handles multiple date formats from RoyalRoad
     */
    private fun convertToRelativeTime(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            // Try multiple date formats that RoyalRoad might use
            val formats = listOf(
                "MMM dd, yyyy hh:mm a",        // "Dec 15, 2023 03:30 PM"
                "MMM dd, yyyy",                 // "Dec 15, 2023"
                "yyyy-MM-dd HH:mm:ss",         // "2023-12-15 15:30:00"
                "yyyy-MM-dd'T'HH:mm:ss",       // "2023-12-15T15:30:00"
                "yyyy-MM-dd'T'HH:mm:ss.SSS",   // "2023-12-15T15:30:00.000"
                "yyyy-MM-dd'T'HH:mm:ss'Z'",    // "2023-12-15T15:30:00Z"
                "EEE, dd MMM yyyy HH:mm:ss",   // "Fri, 15 Dec 2023 15:30:00"
            )

            for (formatStr in formats) {
                try {
                    val sdf = SimpleDateFormat(formatStr, Locale.ENGLISH)
                    val parsed = sdf.parse(dateStr)
                    if (parsed != null) {
                        return parsed.time.toRelativeTime()
                    }
                } catch (e: Exception) {
                    // Try next format
                    continue
                }
            }

            // If no format worked, return the original string
            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    // ================================================================
    // MAIN PAGE
    // ================================================================

    override suspend fun loadMainPage(
        page: Int,
        orderBy: String?,
        tag: String?,
        extraFilters: Map<String, String>
    ): MainPageResult {
        val order = orderBy.takeUnless { it.isNullOrEmpty() } ?: "best-rated"

        if (page > 1 && (order == "trending" || order == "rising-stars")) {
            return MainPageResult(url = "", novels = emptyList())
        }

        val tagParam = if (tag.isNullOrEmpty()) "" else "&genre=$tag"
        val url = "$mainUrl/fictions/$order?page=$page$tagParam"

        val response = get(url)
        val document = response.document

        val novels = document.select("div.fiction-list-item").mapNotNull { element ->
            parseNovelFromList(element, order)
        }

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
            if (orderBy == "latest-updates") {
                head.selectFirstOrNull("> ul.list-unstyled > li.list-item > a > span")?.textOrNull()
            } else {
                element.select("div.stats > div.col-sm-6 > span").getOrNull(4)?.textOrNull()
            }
        } catch (_: Throwable) {
            null
        }

        val rating = head.selectFirstOrNull("> div.stats")
            ?.select("> div")?.getOrNull(1)
            ?.selectFirstOrNull("> span")
            ?.attrOrNull("title")
            ?.toFloatOrNull()
            ?.let { RatingUtils.from5Stars(it) }

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            latestChapter = latestChapter,
            rating = rating,
            apiName = this.name
        )
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/fictions/search?page=1&title=$encodedQuery&globalFilters=true"

        val response = get(url)
        val document = response.document

        return document.select("div.fiction-list-item").mapNotNull { element ->
            parseNovelFromSearch(element)
        }
    }

    private fun parseNovelFromSearch(element: Element): Novel? {
        val head = element.selectFirstOrNull("> div.search-content")
            ?: element.selectFirstOrNull("> div")
            ?: return null

        val titleLink = head.selectFirstOrNull("> h2.fiction-title > a") ?: return null

        val name = titleLink.textOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val href = titleLink.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(href) ?: return null

        val posterUrl = element.selectFirstOrNull("> figure.text-center > a > img")?.attrOrNull("src")
            ?: element.selectFirstOrNull("> figure > a > img")?.attrOrNull("src")

        val rating = head.selectFirstOrNull("> div.stats")
            ?.select("> div")?.getOrNull(1)
            ?.selectFirstOrNull("> span")
            ?.attrOrNull("title")
            ?.toFloatOrNull()
            ?.let { RatingUtils.from5Stars(it) }

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            rating = rating,
            apiName = this.name
        )
    }

    // ================================================================
    // LOAD RELATED/SIMILAR FICTIONS
    // ================================================================

    private suspend fun loadRelatedNovels(fictionId: Int?): List<Novel> {
        if (fictionId == null) return emptyList()

        return try {
            val url = "$mainUrl/fictions/similar?fictionId=$fictionId"
            val response = get(url)
            val jsonArray = JSONArray(response.text)

            val novels = mutableListOf<Novel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val title = obj.optString("title", null) ?: continue
                val novelUrl = obj.optString("url", null) ?: continue
                val cover = obj.optString("cover", null)

                novels.add(
                    Novel(
                        name = title,
                        url = fixUrl(novelUrl) ?: continue,
                        posterUrl = cover?.let { fixUrl(it) },
                        apiName = this.name
                    )
                )
            }
            novels
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ================================================================
    // LOAD REVIEWS
    // ================================================================

    override suspend fun loadReviews(
        url: String,
        page: Int,
        showSpoilers: Boolean
    ): List<UserReview> {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val reviewUrl = "$fullUrl?sorting=top&reviews=$page"

        val response = get(reviewUrl)
        val document = response.document

        val reviews = document.select("div.reviews-container > div.review")

        return reviews.mapNotNull { reviewElement ->
            parseReview(reviewElement, showSpoilers)
        }
    }

    private fun parseReview(reviewElement: Element, showSpoilers: Boolean): UserReview? {
        val textContent = reviewElement.selectFirstOrNull("> div.review-right-content")
        val scoreContent = reviewElement.selectFirstOrNull("> div.review-side")

        val scoreHeader = scoreContent?.selectFirstOrNull("> div.scores > div")
        var overallScore = parseOverallScore(scoreHeader)
        if (overallScore == null) {
            overallScore = parseOverallScoreFromStarClass(scoreHeader)
        }

        val avatar = scoreContent?.selectFirstOrNull("> div.avatar-container-general > img")
        val avatarUrl = avatar?.attrOrNull("src")

        val advancedScores = parseAdvancedScores(scoreHeader)

        val reviewHeader = textContent?.selectFirstOrNull("> div.review-header")
        val reviewMeta = reviewHeader?.selectFirstOrNull("> div.review-meta")

        val reviewTitle = reviewHeader?.selectFirstOrNull("> div > div > h4")?.textOrNull()?.trim()
        val username = reviewMeta?.selectFirstOrNull("> span > a")?.textOrNull()?.trim()

        // FIXED: Convert review time to relative format
        val reviewTime = parseReviewTimeToRelative(reviewMeta)

        val reviewContent = textContent?.selectFirstOrNull("> div.review-content")

        // Check if the review contains spoilers before removing them
        val hasSpoilers = reviewContent?.select(".spoiler")?.isNotEmpty() == true

        if (!showSpoilers) {
            reviewContent?.select(".spoiler")?.remove()
        }

        val reviewText = reviewContent?.html() ?: return null

        // Generate a unique ID for the review
        val reviewId = buildString {
            append(username ?: "anon")
            append("_")
            append(reviewTime ?: System.currentTimeMillis().toString())
            append("_")
            append(reviewText.hashCode())
        }

        return UserReview(
            id = reviewId,
            content = reviewText,
            title = reviewTitle,
            username = username,
            time = reviewTime,
            avatarUrl = avatarUrl?.let { fixUrl(it) },
            overallScore = overallScore,
            advancedScores = advancedScores,
            isSpoiler = hasSpoilers
        )
    }

    private fun parseOverallScore(scoreHeader: Element?): Int? {
        val overallContainer = scoreHeader?.selectFirstOrNull("> div.overall-score-container")
        val ariaLabel = overallContainer?.select("> div")?.getOrNull(1)?.attrOrNull("aria-label")
        return parseStarScore(ariaLabel)
    }

    private fun parseOverallScoreFromStarClass(scoreHeader: Element?): Int? {
        val divHeader = scoreHeader?.selectFirstOrNull("> div.overall-score-container")
        val starDiv = divHeader?.select("> div")?.getOrNull(1)?.selectFirstOrNull("> div")
        return parseStarScoreFromClass(starDiv)
    }

    private fun parseAdvancedScores(scoreHeader: Element?): List<ReviewScore> {
        val scores = scoreHeader?.select("> div.advanced-score") ?: return emptyList()

        return scores.mapNotNull { scoreElement ->
            val divs = scoreElement.select("> div")
            if (divs.size < 2) return@mapNotNull null

            val categoryName = divs.getOrNull(0)?.textOrNull()?.trim() ?: return@mapNotNull null
            val scoreValue = parseStarScore(divs.getOrNull(1)?.attrOrNull("aria-label"))
                ?: return@mapNotNull null

            ReviewScore(
                category = categoryName,
                score = scoreValue
            )
        }
    }

    /**
     * Parse review time and convert to relative format (e.g., "2d ago", "1w ago")
     */
    private fun parseReviewTimeToRelative(reviewMeta: Element?): String? {
        val unixTime = reviewMeta
            ?.selectFirstOrNull("> span > a > time")
            ?.attrOrNull("unixtime")
            ?.toLongOrNull()
            ?: return null

        return try {
            // Convert Unix timestamp (seconds) to milliseconds and then to relative time
            (unixTime * 1000).toRelativeTime()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document
        val responseText = response.text

        val name = document.selectFirstOrNull("h1.font-white")?.textOrNull()?.trim()
            ?: return null

        val fictionId = extractFictionId(responseText)
        val chapters = parseChaptersFromScript(responseText) ?: parseChaptersFromTable(document)
        val relatedNovels = loadRelatedNovels(fictionId)
        val metadata = extractMetadata(document)
        val views = extractViews(document)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            rating = metadata.rating,
            peopleVoted = metadata.peopleVoted,
            status = metadata.status,
            views = views,
            relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    /**
     * Parse chapters from window.chapters JavaScript object
     * This is more reliable than parsing the table
     * FIXED: Converts dates to relative time format
     */
    private fun parseChaptersFromScript(responseText: String): List<Chapter>? {
        return try {
            // Extract window.chapters = [...];
            val chaptersMatch = Regex("""window\.chapters\s*=\s*(\[.*?\]);""", RegexOption.DOT_MATCHES_ALL)
                .find(responseText) ?: return null

            val chaptersJson = chaptersMatch.groupValues[1]
            val jsonArray = JSONArray(chaptersJson)

            val chapters = mutableListOf<Chapter>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val title = obj.optString("title", null) ?: continue
                val chapterUrl = obj.optString("url", null) ?: continue
                val date = obj.optString("date", null)

                // FIXED: Convert date to relative time format
                val relativeDate = convertToRelativeTime(date)

                // Extract chapter path from URL: /fiction/12345/title/chapter/67890/chapter-name
                val urlParts = chapterUrl.split("/")
                val chapterPath = if (urlParts.size >= 6) {
                    "${urlParts[1]}/${urlParts[2]}/${urlParts[4]}/${urlParts[5]}"
                } else {
                    chapterUrl.removePrefix("/")
                }

                chapters.add(
                    Chapter(
                        name = title,
                        url = fixUrl(chapterPath) ?: continue,
                        dateOfRelease = relativeDate
                    )
                )
            }

            chapters
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fallback: Parse chapters from the table (old method)
     * FIXED: Converts dates to relative time format
     */
    private fun parseChaptersFromTable(document: Document): List<Chapter> {
        return document.select("div.portlet-body > table > tbody > tr").mapNotNull { row ->
            val chapterUrl = row.attrOrNull("data-url") ?: return@mapNotNull null
            val cells = row.select("> td")

            val chapterName = cells.getOrNull(0)
                ?.selectFirstOrNull("> a")
                ?.textOrNull()
                ?.trim()
                ?: return@mapNotNull null

            val dateOfRelease = cells.getOrNull(1)
                ?.selectFirstOrNull("> a > time")
                ?.textOrNull()
                ?.trim()

            // FIXED: Convert date to relative time format
            val relativeDate = convertToRelativeTime(dateOfRelease)

            Chapter(
                name = chapterName,
                url = fixUrl(chapterUrl) ?: return@mapNotNull null,
                dateOfRelease = relativeDate
            )
        }
    }

    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val rating: Int? = null,
        val peopleVoted: Int? = null,
        val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        val author = document.selectFirstOrNull("h4.font-white > span > a")?.textOrNull()?.trim()

        val posterUrl = document.selectFirstOrNull("div.fic-header > div > .cover-art-container > img")
            ?.attrOrNull("src")

        val synoDescript = document.selectFirstOrNull("div.description > div")
        val synoParts = synoDescript?.select("> p")
        val synopsis = if (synoParts.isNullOrEmpty() && synoDescript?.hasText() == true) {
            synoDescript.text().replace("\n", "\n\n")
        } else {
            synoParts?.joinToString(separator = "\n\n") { it.text() }
        }

        val tags = document.select("span.tags > a")
            .mapNotNull { it.textOrNull()?.trim() }
            .filter { it.isNotBlank() }

        var status: String? = null
        val statusElements = document.select("div.col-md-8 > div.margin-bottom-10 > span.label")
        for (s in statusElements) {
            val text = s.textOrNull()?.trim()
            val parsed = parseStatus(text)
            if (parsed != null) {
                status = parsed
                break
            }
        }

        val ratingAttr = document.selectFirstOrNull("span.font-red-sunglo")?.attrOrNull("data-content")
        val rating = try {
            ratingAttr?.substringBefore('/')?.trim()?.toFloatOrNull()?.let {
                RatingUtils.from5Stars(it)
            }
        } catch (_: Throwable) {
            null
        }

        val peopleVoted = try {
            document.selectFirstOrNull("span.font-red-sunglo")
                ?.attrOrNull("data-original-title")
                ?.let { title ->
                    Regex("([\\d,]+)\\s*rating").find(title)
                        ?.groupValues?.getOrNull(1)
                        ?.replace(",", "")
                        ?.toIntOrNull()
                }
        } catch (_: Throwable) {
            null
        }

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis?.takeIf { it.isNotBlank() },
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status
        )
    }

    private fun extractViews(document: Document): Int? {
        return try {
            val statsList = document.select("ul.list-unstyled").getOrNull(1)
            val stats = statsList?.select("> li")
            stats?.getOrNull(1)
                ?.textOrNull()
                ?.replace(",", "")
                ?.replace(".", "")
                ?.filter { it.isDigit() }
                ?.toIntOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document
        val responseText = response.text

        val chapterContent = document.selectFirstOrNull("div.chapter-content") ?: return null

        // Remove hidden elements using style parsing (like LNReader)
        removeHiddenElements(chapterContent, responseText)

        // Get author notes
        val (notesBefore, notesAfter) = extractAuthorNotes(document, chapterContent)

        // Build final content
        val contentParts = mutableListOf<String>()

        if (notesBefore.isNotBlank()) {
            contentParts.add("""<div class="author-note-before">$notesBefore</div>""")
        }

        contentParts.add(chapterContent.html())

        if (notesAfter.isNotBlank()) {
            contentParts.add("""<div class="author-note-after">$notesAfter</div>""")
        }

        return contentParts.joinToString(separator = "\n<hr class=\"notes-separator\">\n")
    }

    /**
     * Remove hidden elements using CSS style parsing (from LNReader)
     * Looks for .className { display: none; } patterns
     */
    private fun removeHiddenElements(chapter: Element, responseText: String) {
        try {
            // Find hidden class pattern: .className { ... display: none; ... }
            val styleRegex = Regex("""<style>\s+\.(.+?)\{[^{]+?display:\s*none;""", RegexOption.MULTILINE)
            val match = styleRegex.find(responseText)
            val hiddenClass = match?.groupValues?.getOrNull(1)?.trim()

            if (!hiddenClass.isNullOrBlank()) {
                // Remove elements with this class
                chapter.select(".$hiddenClass").remove()
            }
        } catch (e: Throwable) {
            // Ignore parsing errors
        }
    }

    /**
     * Extract author notes before and after chapter (like LNReader)
     */
    private fun extractAuthorNotes(document: Document, chapterContent: Element): Pair<String, String> {
        val notesBefore = StringBuilder()
        val notesAfter = StringBuilder()

        try {
            val chapterParent = chapterContent.parent() ?: return Pair("", "")

            document.select("div.author-note").forEach { authorNote ->
                val noteContainer = authorNote.parent() ?: return@forEach
                val noteParent = noteContainer.parent() ?: return@forEach

                // Check if note is in same parent as chapter
                if (noteParent == chapterParent) {
                    val isNoteBefore = noteContainer.elementSiblingIndex() < chapterContent.elementSiblingIndex()
                    val noteContent = authorNote.html().trim()

                    if (noteContent.isNotBlank()) {
                        if (isNoteBefore) {
                            notesBefore.append(noteContent)
                        } else {
                            notesAfter.append(noteContent)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return Pair(notesBefore.toString(), notesAfter.toString())
    }
}