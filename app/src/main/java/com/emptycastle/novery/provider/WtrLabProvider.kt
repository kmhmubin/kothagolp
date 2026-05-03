package com.emptycastle.novery.provider

import android.webkit.CookieManager
import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.util.RatingUtils
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provider for wtr-lab.com
 *
 * CLOUDFLARE PROTECTION:
 * - This site uses Cloudflare Turnstile protection
 * - Users MUST solve the challenge in WebView before reading
 * - Cookies are valid for ~24 hours
 * - Desktop User-Agent is used to avoid mobile challenges
 */
class WtrLabProvider : MainProvider() {

    override val name = "WTR-LAB"
    override val mainUrl = "https://wtr-lab.com"
    override val hasMainPage = true
    override val hasReviews = false
    override val iconRes: Int = R.drawable.ic_provider_wtrlab
    override val rateLimitTime: Long = 3000L // Reduced from 12s
    override val ratingScale: RatingScale = RatingScale.FIVE_STAR

    private val lang = "en"
    private var decryptionKey: String? = null
    private var cachedBuildId: String? = null

    // Glossary terms cache
    private data class GlossaryTerm(val from: String, val to: String)
    private val userTermsCache = mutableMapOf<String, List<GlossaryTerm>>()
    private val storyTermsCache = mutableMapOf<String, List<GlossaryTerm>>()

    companion object {
        private const val BATCH_SIZE = 250

        // CRITICAL: Use desktop User-Agent to avoid mobile Cloudflare challenges
        // Mobile UA triggers more aggressive challenges
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

        private object Endpoints {
            const val API_CHAPTERS = "/api/chapters"
            const val API_READER_GET = "/api/reader/get"
            const val API_USER_CONFIG = "/api/v2/user/config"
            const val API_READER_TERMS = "/api/v2/reader/terms"
            const val NOVEL_FINDER = "/en/novel-finder"
        }

        private object Status {
            const val ONGOING = 0
            const val COMPLETED = 1
            const val HIATUS = 2
            const val DROPPED = 3
        }

        private object ErrorCodes {
            const val CHAPTER_LOCKED = "CHAPTER_LOCKED"
            const val TURNSTILE_REQUIRED = 1401
            const val UNAUTHORIZED = 401
            const val FORBIDDEN = 403
        }
    }

    // ================================================================
    // FILTER OPTIONS
    // ================================================================

    override val orderBys = listOf(
        FilterOption("Update Date", "update"),
        FilterOption("Addition Date", "date"),
        FilterOption("Weekly View", "weekly_rank"),
        FilterOption("Monthly View", "monthly_rank"),
        FilterOption("All-Time View", "view"),
        FilterOption("Chapter Count", "chapter"),
        FilterOption("Rating", "rating"),
        FilterOption("Name", "name")
    )

    override val tags = listOf(
        FilterOption("Male Protagonist", "417"),
        FilterOption("Female Protagonist", "275"),
        FilterOption("Transmigration", "717"),
        FilterOption("System", "696"),
        FilterOption("Cultivation", "169"),
        FilterOption("Reincarnation", "578"),
        FilterOption("Fantasy World", "265"),
        FilterOption("Overpowered Protagonist", "506"),
        FilterOption("Weak to Strong", "750"),
        FilterOption("Romance", "592"),
        FilterOption("Action", "1"),
        FilterOption("Adventure", "2"),
        FilterOption("Comedy", "3"),
        FilterOption("Drama", "4"),
        FilterOption("Fantasy", "5"),
        FilterOption("Harem", "6"),
        FilterOption("Martial Arts", "426"),
        FilterOption("Sci-fi", "13"),
        FilterOption("Xianxia", "20"),
        FilterOption("Xuanhuan", "21"),
        FilterOption("Game Elements", "297"),
        FilterOption("Kingdom Building", "379"),
        FilterOption("Time Travel", "710"),
        FilterOption("Apocalypse", "47"),
        FilterOption("Magic", "410"),
        FilterOption("Fanfiction", "263")
    )

    // ================================================================
    // COOKIE & CLOUDFLARE MANAGEMENT
    // ================================================================

    /**
     * Build headers with desktop User-Agent and Cloudflare cookies
     */
    private fun buildHeaders(
        additionalHeaders: Map<String, String> = emptyMap(),
        isApiRequest: Boolean = false
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        // CRITICAL: Always use desktop User-Agent
        headers["User-Agent"] = DESKTOP_USER_AGENT

        // Get cookies from WebView CookieManager
        val cookies = getCookiesFromWebView()
        if (cookies.isNotBlank()) {
            headers["Cookie"] = cookies
        }

        // Standard browser headers
        if (isApiRequest) {
            headers["Accept"] = "application/json, text/plain, */*"
            headers["Accept-Language"] = "en-US,en;q=0.9"
        } else {
            headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            headers["Accept-Language"] = "en-US,en;q=0.9"
        }

        headers["Accept-Encoding"] = "gzip, deflate, br"
        headers["Connection"] = "keep-alive"
        headers["Upgrade-Insecure-Requests"] = "1"
        headers["Sec-Fetch-Dest"] = "document"
        headers["Sec-Fetch-Mode"] = "navigate"
        headers["Sec-Fetch-Site"] = "none"
        headers["Cache-Control"] = "max-age=0"

        // Add custom headers (can override defaults)
        headers.putAll(additionalHeaders)

        return headers
    }

    /**
     * Get cookies from WebView CookieManager for wtr-lab.com
     */
    private fun getCookiesFromWebView(): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(mainUrl) ?: ""

            android.util.Log.d("WtrLabProvider", "WebView cookies: ${cookies.take(100)}...")
            android.util.Log.d("WtrLabProvider", "Has cf_clearance: ${cookies.contains("cf_clearance")}")

            cookies
        } catch (e: Exception) {
            android.util.Log.e("WtrLabProvider", "Failed to get WebView cookies", e)
            ""
        }
    }

    /**
     * Check if user has valid Cloudflare cookies
     */
    private fun hasCloudflareCookies(): Boolean {
        val cookies = getCookiesFromWebView()
        val hasCfClearance = cookies.contains("cf_clearance")

        android.util.Log.d("WtrLabProvider", "hasCloudflareCookies: $hasCfClearance")
        return hasCfClearance
    }

    /**
     * Check if response is Cloudflare challenge
     */
    private fun isCloudflareChallenge(html: String, statusCode: Int): Boolean {
        val cfMarkers = listOf(
            "cf-browser-verification",
            "cf_chl_opt",
            "challenge-platform",
            "Checking your browser",
            "Just a moment",
            "Verify you are human",
            "cf-turnstile",
            "challenges.cloudflare.com"
        )

        val isChallenge = (statusCode == 403 || statusCode == 503) &&
                cfMarkers.any { html.contains(it, ignoreCase = true) }

        if (isChallenge) {
            android.util.Log.w("WtrLabProvider", "Cloudflare challenge detected (code: $statusCode)")
        }

        return isChallenge
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun extractNextData(document: Document): JSONObject? {
        val script = document.selectFirstOrNull("script#__NEXT_DATA__")
        val jsonText = script?.data() ?: return null
        return try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractBuildId(nextData: JSONObject): String? {
        return nextData.optString("buildId", null)?.takeIf { it.isNotBlank() }
    }

    private fun parseStatus(status: Int?): String? {
        return when (status) {
            Status.ONGOING -> "Ongoing"
            Status.COMPLETED -> "Completed"
            Status.HIATUS -> "Hiatus"
            Status.DROPPED -> "Dropped"
            else -> null
        }
    }

    private suspend fun getBuildId(): String {
        cachedBuildId?.let { return it }

        try {
            val response = get("$mainUrl${Endpoints.NOVEL_FINDER}", buildHeaders())

            // Check for Cloudflare
            if (response.code == 403 || response.code == 503) {
                if (isCloudflareChallenge(response.text, response.code)) {
                    throw CloudflareException()
                }
            }

            val nextData = extractNextData(response.document)
            val buildId = nextData?.let { extractBuildId(it) }
                ?: throw Exception("Could not extract buildId from page")

            cachedBuildId = buildId
            return buildId
        } catch (e: CloudflareException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Failed to get buildId: ${e.message}")
        }
    }

    // ================================================================
    // DECRYPTION
    // ================================================================

    private suspend fun getDecryptionKey(document: Document): String {
        decryptionKey?.let { return it }

        try {
            val searchPattern = "TextEncoder().encode(\""
            val scripts = document.select("head script[src]")

            for (script in scripts) {
                val src = script.attr("src")
                if (src.isBlank()) continue

                val scriptUrl = when {
                    src.startsWith("http") -> src
                    src.startsWith("//") -> "https:$src"
                    src.startsWith("/") -> "$mainUrl$src"
                    else -> "$mainUrl/$src"
                }

                try {
                    val scriptContent = get(scriptUrl, buildHeaders()).text

                    val keyIndex = scriptContent.indexOf(searchPattern)
                    if (keyIndex >= 0) {
                        val keyStart = keyIndex + searchPattern.length
                        if (keyStart + 32 <= scriptContent.length) {
                            val extractedKey = scriptContent.substring(keyStart, keyStart + 32)
                            decryptionKey = extractedKey
                            return extractedKey
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback key
        return "IJAFUUxjM25hyzL2AZrn0wl7cESED6Ru"
    }

    private fun decryptContent(encryptedText: String, key: String): List<String> {
        if (encryptedText.isBlank()) return emptyList()

        var isArray = false
        var rawText = encryptedText

        when {
            encryptedText.startsWith("arr:") -> {
                isArray = true
                rawText = encryptedText.removePrefix("arr:")
            }
            encryptedText.startsWith("str:") -> {
                rawText = encryptedText.removePrefix("str:")
            }
        }

        val parts = rawText.split(":")
        if (parts.size != 3) {
            return listOf(encryptedText)
        }

        return try {
            val ivBytes = Base64.getDecoder().decode(parts[0])
            val shortCipher = Base64.getDecoder().decode(parts[1])
            val longCipher = Base64.getDecoder().decode(parts[2])

            val cipherBytes = ByteArray(longCipher.size + shortCipher.size)
            System.arraycopy(longCipher, 0, cipherBytes, 0, longCipher.size)
            System.arraycopy(shortCipher, 0, cipherBytes, longCipher.size, shortCipher.size)

            val keyBytes = key.substring(0, 32).toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            val decryptedText = decryptedBytes.toString(Charsets.UTF_8)

            if (isArray) {
                val jsonArray = JSONArray(decryptedText)
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } else {
                listOf(decryptedText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(encryptedText)
        }
    }

    // ================================================================
    // GLOSSARY METHODS (keeping your original implementations)
    // ================================================================

    private suspend fun getUserTerms(serieId: String): List<GlossaryTerm> {
        userTermsCache[serieId]?.let { return it }

        return try {
            val response = get("$mainUrl${Endpoints.API_USER_CONFIG}", buildHeaders(isApiRequest = true))

            val json = JSONObject(response.text)
            val config = json.optJSONObject("config")
            val termsArray = config?.optJSONArray("terms")

            val terms = mutableListOf<GlossaryTerm>()

            if (termsArray != null) {
                for (i in 0 until termsArray.length()) {
                    val termArray = termsArray.optJSONArray(i) ?: continue

                    val applicableSeries = termArray.optJSONArray(4)
                    val appliesToThisSerie = applicableSeries == null ||
                            (0 until applicableSeries.length()).any {
                                applicableSeries.optString(it) == serieId
                            }

                    if (!appliesToThisSerie) continue

                    val to = termArray.optString(1, null)?.takeIf { it.isNotBlank() } ?: continue
                    val fromString = termArray.optString(2, null)?.takeIf { it.isNotBlank() } ?: continue

                    val fromList = fromString.split("|").filter { it.isNotBlank() }

                    fromList.forEach { from ->
                        terms.add(GlossaryTerm(from = from, to = to))
                    }
                }
            }

            userTermsCache[serieId] = terms
            terms
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun getStoryTerms(rawId: String): List<GlossaryTerm> {
        storyTermsCache[rawId]?.let { return it }

        return try {
            val response = get(
                "$mainUrl${Endpoints.API_READER_TERMS}/$rawId.json",
                buildHeaders(isApiRequest = true)
            )

            val json = JSONObject(response.text)
            val glossaries = json.optJSONArray("glossaries")

            val termsMap = mutableMapOf<String, String>()

            if (glossaries != null) {
                for (i in 0 until glossaries.length()) {
                    val glossary = glossaries.optJSONObject(i) ?: continue
                    val data = glossary.optJSONObject("data") ?: continue
                    val termsArray = data.optJSONArray("terms") ?: continue

                    for (j in 0 until termsArray.length()) {
                        val term = termsArray.optJSONArray(j) ?: continue
                        if (term.length() < 2) continue

                        val toArray = term.optJSONArray(0)
                        val from = term.optString(1, null)?.takeIf { it.isNotBlank() } ?: continue
                        val to = toArray?.optString(0, null)?.takeIf { it.isNotBlank() } ?: continue

                        termsMap[from] = to
                    }
                }
            }

            val terms = termsMap.map { GlossaryTerm(from = it.key, to = it.value) }
            storyTermsCache[rawId] = terms
            terms
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun applyGlossaryTerms(
        text: String,
        chapterTerms: Map<String, String>,
        storyTerms: List<GlossaryTerm>,
        userTerms: List<GlossaryTerm>
    ): String {
        var result = text

        for ((marker, chapterReplacement) in chapterTerms) {
            var finalReplacement = chapterReplacement

            storyTerms.find { it.from == chapterReplacement }?.let {
                finalReplacement = it.to
            }

            userTerms.find { it.from == chapterReplacement }?.let {
                finalReplacement = it.to
            }

            result = result.replace(marker, finalReplacement)
        }

        for (term in userTerms) {
            result = result.replace(term.from, term.to)
        }

        return result
    }

    private fun applyPatches(text: String, patches: JSONArray?): String {
        if (patches == null) return text

        var result = text
        for (i in 0 until patches.length()) {
            val patch = patches.optJSONObject(i) ?: continue
            val zh = patch.optString("zh", null)?.takeIf { it.isNotBlank() } ?: continue
            val en = patch.optString("en", null)?.takeIf { it.isNotBlank() } ?: continue

            result = result.replace(zh, " $en")
        }

        return result
    }

    // ================================================================
    // NOVEL PARSING
    // ================================================================

    private fun parseNovelFromJson(seriesObj: JSONObject): Novel? {
        val rawId = seriesObj.optLong("raw_id", 0)
        if (rawId == 0L) return null

        val slug = seriesObj.optString("slug", "")
        val data = seriesObj.optJSONObject("data") ?: return null

        val title = data.optString("title", "").takeIf { it.isNotBlank() } ?: return null
        val image = data.optString("image", null)?.takeIf { it.isNotBlank() }

        val novelUrl = "/$lang/serie-$rawId/$slug"

        val chapterCount = seriesObj.optInt("chapter_count", 0)
        val latestChapter = if (chapterCount > 0) "$chapterCount Chapters" else null

        val rating = seriesObj.optDouble("rating", Double.NaN)
            .takeIf { !it.isNaN() && it > 0 }
            ?.let { RatingUtils.from5Stars(it.toFloat()) }

        return Novel(
            name = title,
            url = fixUrl(novelUrl) ?: return null,
            posterUrl = image,
            latestChapter = latestChapter,
            rating = rating,
            apiName = this.name
        )
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
        // Check for Cloudflare cookies first
        if (!hasCloudflareCookies()) {
            throw CloudflareException()
        }

        val buildId = getBuildId()

        val params = buildList {
            add("orderBy=${orderBy ?: "update"}")
            add("order=desc")
            add("status=all")
            add("release_status=all")
            add("addition_age=all")
            add("page=$page")

            if (!tag.isNullOrEmpty()) {
                add("gi=$tag")
                add("gc=or")
            }
        }

        val queryString = params.joinToString("&")
        val url = "$mainUrl/_next/data/$buildId/$lang/novel-finder.json?$queryString"

        val response = get(url, buildHeaders(isApiRequest = true))

        // Check for Cloudflare
        if (response.code == 403 || response.code == 503) {
            if (isCloudflareChallenge(response.text, response.code)) {
                throw CloudflareException()
            }
        }

        val json = JSONObject(response.text)
        val pageProps = json.optJSONObject("pageProps")
            ?: return MainPageResult(url, emptyList())

        val seriesArray = pageProps.optJSONArray("series")
            ?: return MainPageResult(url, emptyList())

        val novels = mutableListOf<Novel>()
        val seenIds = mutableSetOf<Long>()

        for (i in 0 until seriesArray.length()) {
            val seriesObj = seriesArray.getJSONObject(i)
            val rawId = seriesObj.optLong("raw_id", 0)

            if (rawId != 0L && seenIds.add(rawId)) {
                parseNovelFromJson(seriesObj)?.let { novels.add(it) }
            }
        }

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        if (!hasCloudflareCookies()) {
            throw CloudflareException()
        }

        val buildId = getBuildId()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/_next/data/$buildId/$lang/novel-finder.json?text=$encodedQuery"

        val response = get(url, buildHeaders(isApiRequest = true))

        if (response.code == 403 || response.code == 503) {
            if (isCloudflareChallenge(response.text, response.code)) {
                throw CloudflareException()
            }
        }

        val json = JSONObject(response.text)
        val pageProps = json.optJSONObject("pageProps") ?: return emptyList()
        val seriesArray = pageProps.optJSONArray("series") ?: return emptyList()

        val novels = mutableListOf<Novel>()
        val seenIds = mutableSetOf<Long>()

        for (i in 0 until seriesArray.length()) {
            val seriesObj = seriesArray.getJSONObject(i)
            val rawId = seriesObj.optLong("raw_id", 0)

            if (rawId != 0L && seenIds.add(rawId)) {
                parseNovelFromJson(seriesObj)?.let { novels.add(it) }
            }
        }

        return novels
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        if (!hasCloudflareCookies()) {
            throw CloudflareException()
        }

        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl, buildHeaders())

        if (response.code == 403 || response.code == 503) {
            if (isCloudflareChallenge(response.text, response.code)) {
                throw CloudflareException()
            }
        }

        val document = response.document
        val nextData = extractNextData(document) ?: return null
        val props = nextData.optJSONObject("props") ?: return null
        val pageProps = props.optJSONObject("pageProps") ?: return null
        val serie = pageProps.optJSONObject("serie") ?: return null
        val serieData = serie.optJSONObject("serie_data") ?: return null

        val rawId = serieData.optLong("raw_id", 0)
        if (rawId == 0L) return null

        val slug = serieData.optString("slug", "")
        val data = serieData.optJSONObject("data")

        val title = data?.optString("title")?.takeIf { it.isNotBlank() }
            ?: return null

        val author = data.optString("author")?.takeIf { it.isNotBlank() }
        val description = data.optString("description")?.takeIf { it.isNotBlank() }
        val image = data.optString("image")?.takeIf { it.isNotBlank() }

        val status = parseStatus(serieData.optInt("status", -1))
        val rawChapterCount = serieData.optLong("raw_chapter_count", 0)

        val rating = serieData.optDouble("rating", Double.NaN)
            .takeIf { !it.isNaN() && it > 0 }
            ?.let { RatingUtils.from5Stars(it.toFloat()) }

        val peopleVoted = serieData.optInt("total_rate", 0).takeIf { it > 0 }
        val views = serieData.optInt("view", 0).takeIf { it > 0 }

        val tags = mutableListOf<String>()
        val tagsArray = pageProps.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                val tagObj = tagsArray.optJSONObject(i)
                val tagTitle = tagObj?.optString("title")
                if (!tagTitle.isNullOrBlank()) {
                    tags.add(tagTitle)
                }
            }
        }

        val chapters = loadChapters(rawId, rawChapterCount, slug)

        val recommendations = serie.optJSONArray("recommendation")
        val relatedNovels = if (recommendations != null) {
            (0 until recommendations.length()).mapNotNull { i ->
                val recObj = recommendations.getJSONObject(i)
                parseNovelFromJson(recObj)
            }
        } else {
            emptyList()
        }

        return NovelDetails(
            url = fullUrl,
            name = title,
            chapters = chapters,
            author = author,
            posterUrl = image,
            synopsis = description ?: "No description available.",
            tags = tags.ifEmpty { null },
            rating = rating,
            peopleVoted = peopleVoted,
            status = status,
            views = views,
            relatedNovels = relatedNovels.ifEmpty { null }
        )
    }

    private suspend fun loadChapters(
        rawId: Long,
        totalChapters: Long,
        slug: String
    ): List<Chapter> {
        if (totalChapters <= 0) return emptyList()

        val chapters = mutableListOf<Chapter>()
        var start = 1L

        while (start <= totalChapters) {
            val end = minOf(start + BATCH_SIZE - 1, totalChapters)

            try {
                val url = "$mainUrl${Endpoints.API_CHAPTERS}/$rawId?start=$start&end=$end"
                val response = get(url, buildHeaders(isApiRequest = true))

                val json = JSONObject(response.text)
                val chaptersArray = json.optJSONArray("chapters")

                if (chaptersArray == null || chaptersArray.length() == 0) break

                for (i in 0 until chaptersArray.length()) {
                    val chapterObj = chaptersArray.getJSONObject(i)
                    val order = chapterObj.optLong("order", 0)
                    val title = chapterObj.optString("title", "").trim()
                    val updatedAt = chapterObj.optString("updated_at", null)

                    val chapterName = if (title.isNotBlank()) {
                        "#$order: $title"
                    } else {
                        "Chapter $order"
                    }

                    val chapterUrl = "/$lang/serie-$rawId/$slug/chapter-$order"

                    chapters.add(
                        Chapter(
                            name = chapterName,
                            url = fixUrl(chapterUrl) ?: continue,
                            dateOfRelease = updatedAt?.take(10)
                        )
                    )
                }

                if (chaptersArray.length() < BATCH_SIZE) break
                start += BATCH_SIZE

            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }

        return chapters.sortedBy { chapter ->
            Regex("chapter-(\\d+)").find(chapter.url)
                ?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0
        }
    }

    // ================================================================
    // LOAD CHAPTER CONTENT
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        if (!hasCloudflareCookies()) {
            return buildCloudflareErrorHtml()
        }

        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val regex = Regex("/serie-(\\d+)/[^/]+/chapter-(\\d+)")
        val match = regex.find(fullUrl)
            ?: throw Exception("Invalid chapter URL format: $fullUrl")

        val rawId = match.groupValues[1]
        val chapterNo = match.groupValues[2]

        val userTerms = getUserTerms(rawId)
        val storyTerms = getStoryTerms(rawId)

        // Try AI translation first, then web
        val services = listOf("ai", "web")
        var lastError: String? = null
        var pageDocument: Document? = null
        var usedService: String? = null

        for (service in services) {
            try {
                val jsonBody = JSONObject().apply {
                    put("translate", service)
                    put("language", lang)
                    put("raw_id", rawId.toLong())
                    put("chapter_no", chapterNo.toLong())
                    put("retry", false)
                    put("force_retry", false)
                }

                val response = postJson(
                    url = "$mainUrl${Endpoints.API_READER_GET}",
                    json = jsonBody.toString(),
                    headers = buildHeaders(
                        additionalHeaders = mapOf(
                            "Referer" to fullUrl,
                            "Origin" to mainUrl
                        ),
                        isApiRequest = true
                    )
                )

                // Check for Cloudflare
                if (response.code == 403 || response.code == 503) {
                    if (isCloudflareChallenge(response.text, response.code)) {
                        return buildCloudflareErrorHtml()
                    }
                }

                val json = JSONObject(response.text)

                // Check for Turnstile
                if (json.optBoolean("requireTurnstile", false) ||
                    json.optInt("code", 0) == ErrorCodes.TURNSTILE_REQUIRED) {
                    return buildCloudflareErrorHtml()
                }

                if (json.optString("code", null) == ErrorCodes.CHAPTER_LOCKED) {
                    lastError = "Chapter is locked or not AI translated yet."
                    continue
                }

                val code = json.optInt("code", 0)
                if (code == ErrorCodes.UNAUTHORIZED || code == ErrorCodes.FORBIDDEN) {
                    lastError = "Authentication required for AI translation."
                    continue
                }

                if (!json.optBoolean("success", true)) {
                    val message = json.optString("message", "Unknown error")
                    lastError = message
                    continue
                }

                val dataObj = json.optJSONObject("data")?.optJSONObject("data")
                if (dataObj == null) {
                    lastError = "No content data in response"
                    continue
                }

                val bodyContent = dataObj.opt("body")
                if (bodyContent == null) {
                    lastError = "No body content"
                    continue
                }

                usedService = service

                val paragraphs: List<String> = when (bodyContent) {
                    is String -> {
                        if (bodyContent.startsWith("arr:") || bodyContent.startsWith("str:")) {
                            if (pageDocument == null) {
                                pageDocument = get(fullUrl, buildHeaders()).document
                            }
                            val key = getDecryptionKey(pageDocument!!)
                            decryptContent(bodyContent, key)
                        } else {
                            listOf(bodyContent)
                        }
                    }
                    is JSONArray -> {
                        (0 until bodyContent.length()).map { bodyContent.getString(it) }
                    }
                    else -> {
                        lastError = "Unexpected body content type"
                        continue
                    }
                }

                val chapterTerms = mutableMapOf<String, String>()
                val glossaryData = dataObj.optJSONObject("glossary_data")
                val termsArray = glossaryData?.optJSONArray("terms")

                if (termsArray != null) {
                    for (i in 0 until termsArray.length()) {
                        val term = termsArray.optJSONArray(i) ?: continue
                        val replacement = term.optString(0, null)?.takeIf { it.isNotBlank() }
                        if (replacement != null) {
                            chapterTerms["※${i}⛬"] = replacement
                            chapterTerms["※${i}〓"] = replacement
                        }
                    }
                }

                val patchArray = dataObj.optJSONArray("patch")

                return buildChapterHtml(
                    chapterNo = chapterNo,
                    chapterTitle = json.optJSONObject("chapter")?.optString("title"),
                    paragraphs = paragraphs,
                    images = dataObj.optJSONArray("images"),
                    chapterTerms = chapterTerms,
                    storyTerms = storyTerms,
                    userTerms = userTerms,
                    patches = patchArray,
                    usedService = usedService
                )

            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                e.printStackTrace()
                continue
            }
        }

        return buildErrorHtml(lastError)
    }

    private fun buildChapterHtml(
        chapterNo: String,
        chapterTitle: String?,
        paragraphs: List<String>,
        images: JSONArray?,
        chapterTerms: Map<String, String>,
        storyTerms: List<GlossaryTerm>,
        userTerms: List<GlossaryTerm>,
        patches: JSONArray?,
        usedService: String?
    ): String {
        val html = StringBuilder()

        if (!chapterTitle.isNullOrBlank()) {
            html.append("<h1>#$chapterNo: $chapterTitle</h1>\n")
        }

        if (usedService == "web") {
            html.append("""
                <div style="background: #fff3cd; padding: 12px; margin-bottom: 16px; 
                    border-radius: 8px; border-left: 4px solid #ffc107;">
                    <strong>⚠️ Web Translation (Machine Translation)</strong><br/>
                    <span style="font-size: 0.9em;">
                        This is basic machine translation. AI translation may be unavailable.
                    </span>
                </div>
            """.trimIndent())
        }

        var imageIndex = 0

        for (paragraph in paragraphs) {
            if (paragraph == "[image]") {
                val imageUrl = images?.optString(imageIndex)
                if (!imageUrl.isNullOrBlank()) {
                    html.append("""
                        <p><img src="$imageUrl" style="max-width: 100%; height: auto;" /></p>
                    """.trimIndent())
                }
                imageIndex++
            } else {
                var processedText = applyGlossaryTerms(
                    text = paragraph,
                    chapterTerms = chapterTerms,
                    storyTerms = storyTerms,
                    userTerms = userTerms
                )

                processedText = applyPatches(processedText, patches)

                html.append("<p>$processedText</p>\n")
            }
        }

        return html.toString()
    }

    private fun buildErrorHtml(errorMessage: String?): String {
        return buildString {
            append("<div style=\"padding: 20px; text-align: center;\">")
            append("<h2>❌ Failed to load chapter</h2>")
            append("<p style=\"color: #666; margin: 16px 0;\">")
            append(errorMessage ?: "Unknown error occurred")
            append("</p>")
            append("<p style=\"font-size: 0.9em; color: #888;\">")
            append("This chapter may not be translated yet or the server is under heavy load.")
            append("</p>")
            append("</div>")
        }
    }

    private fun buildCloudflareErrorHtml(): String {
        return buildString {
            append("<div style=\"padding: 20px; text-align: center;\">")
            append("<h2>🔒 Cloudflare Protection Active</h2>")
            append("<p style=\"margin: 16px 0; line-height: 1.6;\">")
            append("WTR-LAB uses Cloudflare to protect against bots.<br/>")
            append("You need to solve the verification challenge first.")
            append("</p>")
            append("<hr style=\"margin: 20px 0; border: none; border-top: 1px solid #ddd;\"/>")
            append("<h3>📱 How to unlock:</h3>")
            append("<ol style=\"text-align: left; display: inline-block; margin: 16px 0; line-height: 1.8;\">")
            append("<li>Tap the <strong>globe icon</strong> (🌐) at the top-right</li>")
            append("<li>Wait for the Cloudflare challenge to appear</li>")
            append("<li>Check the \"I'm human\" box if prompted</li>")
            append("<li>Wait for the challenge to complete</li>")
            append("<li>Close the browser and return to reading</li>")
            append("</ol>")
            append("<div style=\"background: #e3f2fd; padding: 12px; border-radius: 8px; margin: 16px 0;\">")
            append("<strong>💡 Tip:</strong> Cookies last for 24 hours, so you won't need to do this often!")
            append("</div>")
            append("</div>")
        }
    }

    /**
     * Custom exception for Cloudflare challenges
     */
    class CloudflareException : Exception("Cloudflare verification required. Please open in WebView.")
}