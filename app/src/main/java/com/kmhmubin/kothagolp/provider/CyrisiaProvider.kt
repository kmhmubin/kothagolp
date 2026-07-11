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
            val arr = JSONArray(get(apiUrl).text)
            val pageSize = 24
            val start = (page - 1) * pageSize
            val end = minOf(start + pageSize, arr.length())
            val novels = mutableListOf<Novel>()
            for (i in start until end) {
                val obj = arr.optJSONObject(i) ?: continue
                val seriesName = obj.optString("name", null) ?: continue
                val coverRaw = obj.optString("cover", null)
                val cover = coverRaw?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                novels.add(Novel(
                    name = seriesName,
                    url = "$mainUrl/series/${java.net.URLEncoder.encode(seriesName, "UTF-8").replace("+", "%20")}",
                    posterUrl = cover,
                    apiName = this.name
                ))
            }
            MainPageResult(url = apiUrl, novels = novels, hasNextPage = end < arr.length())
        } catch (_: Throwable) { MainPageResult(url = apiUrl, novels = emptyList()) }
    }

    override suspend fun search(query: String): List<Novel> {
        val lQuery = query.lowercase().trim()
        val apiUrl = "$mainUrl/api/bookshelf"
        return try {
            val arr = JSONArray(get(apiUrl).text)
            val novels = mutableListOf<Novel>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val seriesName = obj.optString("name", null) ?: continue
                if (!seriesName.lowercase().contains(lQuery)) continue
                val coverRaw = obj.optString("cover", null)
                val cover = coverRaw?.let { if (it.startsWith("http")) it else "$mainUrl$it" }
                novels.add(Novel(
                    name = seriesName,
                    url = "$mainUrl/series/${java.net.URLEncoder.encode(seriesName, "UTF-8").replace("+", "%20")}",
                    posterUrl = cover,
                    apiName = this.name
                ))
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

        val synopsis = document.selectFirstOrNull(".synopsis-trunc, .synopsis-full, [id^='syn-']")
            ?.ownText()?.trim()?.takeIf { it.isNotBlank() }
            ?: document.selectFirstOrNull(".synopsis-trunc, .synopsis-full, [id^='syn-']")
                ?.text()?.trim()

        val genres = document.select(".meta-chip").mapNotNull { it.textOrNull()?.trim() }

        // Try download buttons first (have data-epub-url and data-volume-name)
        val chapters = document.select(".download-volume-btn").mapNotNull { btn ->
            val epubPath = btn.attrOrNull("data-epub-url") ?: return@mapNotNull null
            val volName = btn.attrOrNull("data-volume-name") ?: return@mapNotNull null
            val epubUrl = if (epubPath.startsWith("http")) epubPath else "$mainUrl$epubPath"
            Chapter(name = volName, url = epubUrl)
        }.ifEmpty {
            // Fallback: a.vl hrefs (convert /read/ → /bibi-bookshelf/)
            document.select(".vl-wrap a.vl").mapNotNull { a ->
                val href = a.attrOrNull("href") ?: return@mapNotNull null
                val chName = a.ownText().trim().takeIf { it.isNotBlank() }
                    ?: a.textOrNull()?.trim()?.replace(Regex("^[s][0-9]+\\s*[○●]?\\s*"), "")
                    ?: return@mapNotNull null
                val bibiPath = href.replace("/read/", "/bibi-bookshelf/")
                val epubUrl = if (bibiPath.startsWith("http")) bibiPath else "$mainUrl$bibiPath"
                Chapter(name = chName, url = epubUrl)
            }
        }

        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            posterUrl = cover, synopsis = synopsis,
            tags = genres.ifEmpty { null }
        )
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
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
