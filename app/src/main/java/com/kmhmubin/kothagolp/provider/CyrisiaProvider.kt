package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class CyrisiaProvider : MainProvider() {

    override val name = "Cyrisia"
    override val mainUrl = "https://cyrisia.com"
    override val iconRes = R.drawable.ic_provider_cyrisia
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Default", "")
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val apiUrl = "$mainUrl/api/bookshelf"
        return try {
            val arr = bookshelf()
            val pageSize = 24
            val start = (page - 1) * pageSize
            val end = minOf(start + pageSize, arr.length())
            val novels = mutableListOf<Novel>()
            for (i in start until end) {
                arr.optJSONObject(i)?.let(::toNovel)?.let(novels::add)
            }
            MainPageResult(url = apiUrl, novels = novels, hasNextPage = end < arr.length())
        } catch (_: Throwable) { MainPageResult(url = apiUrl, novels = emptyList()) }
    }

    // /api/bookshelf is the whole catalog (~8.7 MB, ~20s on mobile) with no paging or search,
    // so keep one copy in memory instead of refetching for every page and query.
    @Volatile private var bookshelfCache: Pair<Long, JSONArray>? = null

    private suspend fun bookshelf(): JSONArray {
        bookshelfCache?.let { (at, arr) -> if (System.currentTimeMillis() - at < 15 * 60_000) return arr }
        return JSONArray(get("$mainUrl/api/bookshelf").text).also {
            bookshelfCache = System.currentTimeMillis() to it
        }
    }

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    // Bookshelf API has no cover field; site serves covers from /api/cover keyed by series + first epub.
    private fun toNovel(obj: JSONObject): Novel? {
        if (obj.optString("kind") == "comic") return null // ponytail: image-only manga, no text reader for it
        val seriesName = obj.optString("name").takeIf { it.isNotBlank() } ?: return null
        val firstEpub = obj.optJSONArray("epubs")?.optString(0)?.takeIf { it.isNotBlank() }
        return Novel(
            name = seriesName,
            url = "$mainUrl/series/${encode(seriesName)}",
            posterUrl = firstEpub?.let { "$mainUrl/api/cover?series=${encode(seriesName)}&v=${encode(it)}" },
            apiName = this.name
        )
    }

    override suspend fun search(query: String): List<Novel> {
        val lQuery = query.lowercase().trim()
        val apiUrl = "$mainUrl/api/bookshelf"
        return try {
            val arr = bookshelf()
            val novels = mutableListOf<Novel>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (!obj.optString("name").lowercase().contains(lQuery)) continue
                toNovel(obj)?.let(novels::add)
            }
            novels
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document

        val title = document.selectFirstOrNull("meta[property='og:title']")?.attrOrNull("content")?.trim()
            ?: document.selectFirstOrNull(".stitle")?.textOrNull()
            ?: document.title().replace(" - cyrisia", "").trim()
            ?: return null

        val cover = document.selectFirstOrNull("meta[property='og:image']")?.attrOrNull("content")

        val synopsis = document.selectFirstOrNull(".series-meta p")?.textOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: document.selectFirstOrNull("meta[property='og:description']")?.attrOrNull("content")?.trim()
            ?: document.selectFirstOrNull(".synopsis-trunc, .synopsis-full, [id^='syn-']")
            ?.ownText()?.trim()?.takeIf { it.isNotBlank() }
            ?: document.selectFirstOrNull(".synopsis-trunc, .synopsis-full, [id^='syn-']")
                ?.text()?.trim()

        val genres = document.select(".meta-chip").mapNotNull { it.textOrNull()?.trim() }

        // Web novels: per-chapter .txt list embedded as JSON rows [file, title, date]
        val webNovelChapters = document.selectFirstOrNull("script#ch-data")?.data()?.let { raw ->
            runCatching {
                val data = JSONObject(raw)
                val series = data.optString("series").ifBlank { title }
                val rows = data.getJSONArray("rows")
                (0 until rows.length()).mapNotNull { i ->
                    val row = rows.optJSONArray(i) ?: return@mapNotNull null
                    val file = row.optString(0).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    Chapter(
                        name = row.optString(1).ifBlank { file.removeSuffix(".txt") },
                        url = "$mainUrl/read/${encode(series)}/${encode(file)}",
                        dateOfRelease = row.optString(2).ifBlank { null }
                    )
                }
            }.getOrNull()
        }

        // Light novels: download buttons (have data-epub-url and data-volume-name)
        val chapters = webNovelChapters?.takeIf { it.isNotEmpty() } ?: document.select(".download-volume-btn").mapNotNull { btn ->
            val epubPath = btn.attrOrNull("data-epub-url") ?: return@mapNotNull null
            val volName = btn.attrOrNull("data-volume-name") ?: return@mapNotNull null
            val epubUrl = if (epubPath.startsWith("http")) epubPath else "$mainUrl$epubPath"
            Chapter(name = volName, url = epubUrl)
        }.distinctBy { it.url }.ifEmpty { // page renders every button twice (grid + list view)
            // Fallback: a.vl hrefs (convert /read/ → /bibi-bookshelf/)
            document.select(".vl-wrap a.vl").mapNotNull { a ->
                val href = a.attrOrNull("href") ?: return@mapNotNull null
                val chName = a.ownText().trim().takeIf { it.isNotBlank() }
                    ?: a.textOrNull()?.trim()?.replace(Regex("^[s][0-9]+\\s*[○●]?\\s*"), "")
                    ?: return@mapNotNull null
                val bibiPath = href.replace("/read/", "/bibi-bookshelf/")
                val epubUrl = if (bibiPath.startsWith("http")) bibiPath else "$mainUrl$bibiPath"
                Chapter(name = chName, url = epubUrl)
            }.distinctBy { it.url }
        }

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            posterUrl = cover, synopsis = synopsis,
            tags = genres.ifEmpty { null }
        )
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        if (fullUrl.contains("/read/") && fullUrl.endsWith(".txt")) {
            return get(fullUrl).document.selectFirstOrNull("#ch-text")?.html()?.trim()?.takeIf { it.isNotBlank() }
        }
        val referer = fullUrl.replace("/bibi-bookshelf/", "/read/")
        return withContext(Dispatchers.IO) { downloadAndParseEpub(fullUrl, referer) }
    }

    private fun downloadAndParseEpub(epubUrl: String, referer: String): String? {
        val request = Request.Builder()
            .url(epubUrl)
            .header("Referer", referer)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val bytes = response.body?.bytes() ?: return null
        return parseEpub(bytes)
    }

    private fun parseEpub(bytes: ByteArray): String? {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val containerXml = entries["META-INF/container.xml"]?.toString(Charsets.UTF_8) ?: return null
        val opfPath = Regex("""full-path="([^"]+\.opf)"""").find(containerXml)
            ?.groupValues?.get(1) ?: return null
        val opfText = entries[opfPath]?.toString(Charsets.UTF_8) ?: return null
        val opfDir = opfPath.substringBeforeLast('/', "")

        val manifestItems = mutableMapOf<String, String>()
        Regex("""<item\b[^>]+\bid="([^"]+)"[^>]+\bhref="([^"#"]+)"""").findAll(opfText).forEach {
            manifestItems[it.groupValues[1]] = it.groupValues[2]
        }

        val spineIds = Regex("""<itemref\b[^>]+\bidref="([^"]+)"""")
            .findAll(opfText).map { it.groupValues[1] }.toList()

        val skipPatterns = listOf("cover", "titlepage", "title-page", "copyright", "colophon", "nav", "toc")

        val sb = StringBuilder()
        for (id in spineIds) {
            val href = manifestItems[id] ?: continue
            val hrefLower = href.lowercase()
            if (skipPatterns.any { hrefLower.contains(it) }) continue

            val entryPath = if (opfDir.isNotEmpty()) "$opfDir/$href" else href
            val xhtml = (entries[entryPath] ?: entries[href])?.toString(Charsets.UTF_8) ?: continue

            val body = Regex("""<body[^>]*>(.*?)</body>""", RegexOption.DOT_MATCHES_ALL)
                .find(xhtml)?.groupValues?.get(1) ?: continue
            if (body.isBlank()) continue
            sb.append(body.trim()).append("\n")
        }

        return sb.toString().trim().takeIf { it.isNotBlank() }
    }
}
