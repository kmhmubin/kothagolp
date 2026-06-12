package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterGroup
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Provider for EmpireNovel.com
 *
 * Features:
 * - Instant chapter generation (0 network requests for chapters)
 * - JSON search API
 * - Lazy-loaded image support
 * - Status filtering
 */
class EmpireNovelProvider : MainProvider() {

    override val name = "EmpireNovel"
    override val mainUrl = "https://www.empirenovel.com"
    override val hasMainPage = true
    override val iconRes: Int = R.drawable.ic_provider_empirenovel
    override val rateLimitTime: Long = 750L

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adult", "adult"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Animals", "animals"),
        FilterOption("Arts", "arts"),
        FilterOption("Bender", "bender"),
        FilterOption("Biographies", "biographies"),
        FilterOption("Business", "business"),
        FilterOption("Chinese", "chinese"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Drama", "drama"),
        FilterOption("Eastern", "eastern"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Education", "education"),
        FilterOption("Entertainment", "entertainment"),
        FilterOption("Fanfiction", "fanfiction"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Fiction", "fiction"),
        FilterOption("Game", "game"),
        FilterOption("Gender", "gender"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("History", "history"),
        FilterOption("Home", "home"),
        FilterOption("Horror", "horror"),
        FilterOption("Humor", "humor"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("Korean", "korean"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Memoirs", "memoirs"),
        FilterOption("Modern Life", "modern-life"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Original", "original"),
        FilterOption("Other Books", "other-books"),
        FilterOption("Philosophy", "philosophy"),
        FilterOption("Photography", "photography"),
        FilterOption("Politics", "politics"),
        FilterOption("Professional", "professional"),
        FilterOption("Psychological", "psychological"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Religion", "religion"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("School Stories", "school-stories"),
        FilterOption("Sci-fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Short Stories", "short-stories"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shoujo Ai", "shoujo-ai"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Shounen Ai", "shounen-ai"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Smut", "smut"),
        FilterOption("Social Science", "social-science"),
        FilterOption("Spirituality", "spirituality"),
        FilterOption("Sports", "sports"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("System", "system"),
        FilterOption("Technical", "technical"),
        FilterOption("Technology", "technology"),
        FilterOption("Thriller", "thriller"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Transmigration", "transmigration"),
        FilterOption("Urban", "urban"),
        FilterOption("Virtual Reality", "virtual-reality"),
        FilterOption("Wuxia", "wuxia"),
        FilterOption("Xianxia", "xianxia"),
        FilterOption("Xuanhuan", "xuanhuan"),
        FilterOption("Yaoi", "yaoi"),
        FilterOption("Yuri", "yuri")
    )

    override val orderBys = listOf(
        FilterOption("Latest Updates", "updated_at"),
        FilterOption("Name (A-Z)", "name")
    )

    // FIXED: Use FilterGroup instead of ExtraFilterGroup
    override val extraFilterGroups = listOf(
        FilterGroup(
            key = "status",
            label = "Status",
            options = listOf(
                FilterOption("All", ""),
                FilterOption("Ongoing", "1"),
                FilterOption("Completed", "2"),
                FilterOption("Abandoned", "3")
            ),
            defaultValue = ""
        )
    )

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    /**
     * Extract image URL with lazy loading support
     * Per blueprint: ALWAYS check data-src first, then fall back to src
     */
    private fun extractImageUrl(imgElement: Element?): String? {
        if (imgElement == null) return null

        // Priority: data-src > src
        val dataSrc = imgElement.attrOrNull("data-src")?.takeIf { it.isNotBlank() }
        val src = imgElement.attrOrNull("src")?.takeIf { it.isNotBlank() }

        val rawUrl = dataSrc ?: src ?: return null

        // If URL doesn't start with http, prepend base URL
        return if (rawUrl.startsWith("http")) {
            rawUrl
        } else {
            "$mainUrl/${rawUrl.removePrefix("/")}".replace("//", "/").replace(":/", "://")
        }
    }

    /**
     * Parse status from text
     */
    private fun parseStatus(statusText: String?): String? {
        if (statusText.isNullOrBlank()) return null

        return when (statusText.lowercase().trim()) {
            "ongoing" -> "Ongoing"
            "completed" -> "Completed"
            "abandoned" -> "Abandoned"
            else -> statusText.trim().replaceFirstChar { it.uppercase() }
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
        val params = mutableListOf<String>()

        // Category filter
        if (!tag.isNullOrEmpty()) {
            params.add("category=$tag")
        }

        // Sort parameters
        val sortBy = orderBy.takeUnless { it.isNullOrEmpty() } ?: "updated_at"
        params.add("sort_by=$sortBy")

        // Sort direction: name = asc, updated_at = desc
        val sortDir = if (sortBy == "name") "asc" else "desc"
        params.add("sort_dir=$sortDir")

        // Status filter
        val status = extraFilters["status"]
        if (!status.isNullOrEmpty()) {
            params.add("status=$status")
        }

        // Pagination
        params.add("page=$page")

        val queryString = params.joinToString("&")
        val url = "$mainUrl/novels-list?$queryString"

        val response = get(url)
        val document = response.document

        val novels = document.select(".novellist_item").mapNotNull { element ->
            parseNovelFromList(element)
        }

        return MainPageResult(url = url, novels = novels)
    }

    private fun parseNovelFromList(element: Element): Novel? {
        // Title and URL
        val titleElement = element.selectFirstOrNull("h2.fs-6 a") ?: return null
        val name = titleElement.textOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val href = titleElement.attrOrNull("href") ?: return null
        val novelUrl = fixUrl(href) ?: return null

        // Poster with lazy loading support
        val imgElement = element.selectFirstOrNull("img.rounded-3")
        val posterUrl = extractImageUrl(imgElement)

        return Novel(
            name = name,
            url = novelUrl,
            posterUrl = posterUrl,
            apiName = this.name
        )
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/search-live?q=$encodedQuery"

        // Required headers for JSON API
        val response = get(url, mapOf(
            "Accept" to "application/json",
            "X-Requested-With" to "XMLHttpRequest"
        ))

        val jsonArray = JSONArray(response.text)
        val novels = mutableListOf<Novel>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val name = obj.optString("name", null)?.takeIf { it.isNotBlank() } ?: continue
            val slug = obj.optString("slug", null)?.takeIf { it.isNotBlank() } ?: continue

            // Construct URL manually (per blueprint)
            val novelUrl = fixUrl("/novel/$slug") ?: continue

            // Construct poster URL from slug (API doesn't return full URL)
            val posterUrl = "$mainUrl/uploads/novel/$slug/cover/cover_thumb.jpg"

            novels.add(
                Novel(
                    name = name,
                    url = novelUrl,
                    posterUrl = posterUrl,
                    apiName = this.name
                )
            )
        }

        return novels
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document

        // Novel title
        val name = document.selectFirstOrNull("h1[itemprop='name']")?.textOrNull()?.trim()
            ?: return null

        // Extract metadata
        val metadata = extractMetadata(document)

        // Generate chapters using the INSTANT GENERATION TRICK
        val chapters = generateChaptersInstantly(document, url)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = metadata.author,
            posterUrl = metadata.posterUrl,
            synopsis = metadata.synopsis,
            tags = metadata.tags.ifEmpty { null },
            status = metadata.status
        )
    }

    private data class NovelMetadata(
        val author: String? = null,
        val posterUrl: String? = null,
        val synopsis: String? = null,
        val tags: List<String> = emptyList(),
        val status: String? = null
    )

    private fun extractMetadata(document: Document): NovelMetadata {
        // Author
        val author = document.selectFirstOrNull("span[itemprop='author']")?.textOrNull()?.trim()

        // Poster with lazy loading support
        val posterImg = document.selectFirstOrNull("img[itemprop='image']")
        val posterUrl = extractImageUrl(posterImg)

        // Genres/Tags
        val tags = document.select("a[itemprop='genre']")
            .mapNotNull { it.textOrNull()?.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // Status - dynamic extraction per blueprint
        var status: String? = null
        document.select("div.d-flex.justify-content-between").forEach { div ->
            val text = div.ownText().trim()
            if (text.startsWith("Status", ignoreCase = true)) {
                status = parseStatus(div.selectFirstOrNull("span")?.textOrNull())
            }
        }

        // Synopsis with cleanup (remove show more artifacts)
        val synopsisElement = document.selectFirstOrNull("dd[itemprop='description']")
        synopsisElement?.select("#dots, #read_more, #more")?.remove()
        val synopsis = synopsisElement?.text()?.trim()?.takeIf { it.isNotBlank() }

        return NovelMetadata(
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags,
            status = status
        )
    }

    /**
     * ⚡ INSTANT CHAPTER GENERATION TRICK ⚡
     *
     * Instead of scraping 40+ paginated chapter list pages (taking minutes),
     * we extract the first and last chapter numbers and generate all chapters
     * in-between instantly.
     *
     * Network requests: 0
     * Time: < 1 millisecond
     * Trade-off: No release dates (but 99.9% faster)
     *
     * How it works:
     * 1. Find "First Chapter" link -> /novel/slug/0
     * 2. Find "Last Chapter" link -> /novel/slug/1243
     * 3. Extract numbers: 0 and 1243
     * 4. Generate chapters 0-1243 in code
     */
    private fun generateChaptersInstantly(document: Document, novelUrl: String): List<Chapter> {
        try {
            // Find First Chapter link
            val firstChapterLink = document.select("a").find {
                it.text().contains("First Chapter", ignoreCase = true)
            }
            val firstChapterUrl = firstChapterLink?.attrOrNull("href") ?: return emptyList()

            // Find Last Chapter link
            val lastChapterLink = document.select("a").find {
                it.text().contains("Last Chapter", ignoreCase = true)
            }
            val lastChapterUrl = lastChapterLink?.attrOrNull("href") ?: return emptyList()

            // Extract chapter numbers from URLs
            // URL format: /novel/some-slug/0, /novel/some-slug/1243
            val firstParts = firstChapterUrl.split("/")
            val lastParts = lastChapterUrl.split("/")

            val firstNumber = firstParts.lastOrNull()?.toIntOrNull() ?: return emptyList()
            val lastNumber = lastParts.lastOrNull()?.toIntOrNull() ?: return emptyList()

            // Validate range
            if (firstNumber < 0 || lastNumber < firstNumber || lastNumber > 10000) {
                return emptyList() // Sanity check
            }

            // Extract base path (everything except the chapter number)
            val basePath = firstParts.dropLast(1).joinToString("/")

            // Generate all chapters instantly (no network requests!)
            val chapters = mutableListOf<Chapter>()
            for (chapterNumber in firstNumber..lastNumber) {
                val chapterUrl = fixUrl("$basePath/$chapterNumber") ?: continue

                chapters.add(
                    Chapter(
                        name = "Chapter $chapterNumber",
                        url = chapterUrl,
                        dateOfRelease = null // Trade-off: no dates for instant speed
                    )
                )
            }

            return chapters

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document

        val contentElement = document.selectFirstOrNull("#read-novel") ?: return null

        // CRITICAL CLEANUP (per blueprint):
        // Remove age verification overlays that block reading
        contentElement.select("div.wrapper").remove()  // "I am 18" popups
        contentElement.select("script, style").remove()  // Scripts/styles

        return contentElement.html()
    }
}