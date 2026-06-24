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
import org.jsoup.nodes.Element

class FenrirRealmProvider : MainProvider() {

    override val name = "Fenrir Realm"
    override val mainUrl = "https://fenrirealm.com"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=fenrirealm.com&sz=64"
    override val hasMainPage = true

    private val apiBase = "$mainUrl/api/new/v2"

    private val jsonHeaders = mapOf(
        "Accept" to "application/json",
        "Content-Type" to "application/json"
    )

    override val orderBys = listOf(
        FilterOption("Popular", "popular"),
        FilterOption("Latest", "latest"),
        FilterOption("Updated", "updated")
    )

    override val tags = listOf(
        FilterOption("All", "any"),
        FilterOption("Ongoing", "on-going"),
        FilterOption("Completed", "completed")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy?.takeIf { it.isNotBlank() } ?: "popular"
        val status = tag?.takeIf { it.isNotBlank() } ?: "any"
        val genre = extraFilters["genre"]

        val url = buildString {
            append("$apiBase/series?page=$page&per_page=20&status=$status&sort=$sort")
            if (!genre.isNullOrBlank()) append("&genres[]=$genre")
        }
        val response = get(url, jsonHeaders)
        var novels: List<Novel> = emptyList()
        var hasNext = false
        try {
            val json = JSONObject(response.text)
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            val meta = json.optJSONObject("meta")
            val currentPage = meta?.optInt("current_page", 1) ?: 1
            val lastPage = meta?.optInt("last_page", 1) ?: 1
            novels = parseBrowseNovels(dataArray)
            hasNext = currentPage < lastPage
        } catch (_: Throwable) {}
        return MainPageResult(url = url, novels = novels, hasNextPage = hasNext)
    }

    private fun parseBrowseNovels(dataArray: JSONArray): List<Novel> {
        val novels = mutableListOf<Novel>()
        for (i in 0 until dataArray.length()) {
            val obj = dataArray.optJSONObject(i) ?: continue
            val title = obj.optString("title", null) ?: continue
            val slug = obj.optString("slug", null) ?: continue
            val cover = obj.optString("cover", null)
            val posterUrl = buildCoverUrl(cover)
            val novelUrl = "$mainUrl/$slug"
            novels.add(Novel(name = title, url = novelUrl, posterUrl = posterUrl, apiName = this.name))
        }
        return novels
    }

    private fun buildCoverUrl(cover: String?): String? {
        if (cover.isNullOrBlank()) return null
        return if (cover.startsWith("http")) cover else "$mainUrl/$cover".replace("//", "/").replace(":/", "://")
    }

    override suspend fun search(query: String): List<Novel> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$apiBase/series?page=1&per_page=20&search=$encodedQuery"
        val response = get(url, jsonHeaders)
        return try {
            val json = JSONObject(response.text)
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            parseBrowseNovels(dataArray)
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun load(url: String): NovelDetails? {
        val slug = url.trimEnd('/').substringAfterLast("/")
        val metaUrl = "$mainUrl/api/new/v2/series/$slug"
        val metaResponse = get(metaUrl, jsonHeaders)
        val metaJson = try { JSONObject(metaResponse.text) } catch (_: Throwable) { return null }
        val title = metaJson.optString("title", null) ?: return null
        val cover = metaJson.optString("cover", null)
        val posterUrl = buildCoverUrl(cover)
        val descriptionHtml = metaJson.optString("description", null)
        val synopsis = if (!descriptionHtml.isNullOrBlank()) {
            try { org.jsoup.Jsoup.parse(descriptionHtml).body().text().trim() } catch (_: Throwable) { descriptionHtml }
        } else null
        val status = metaJson.optString("status", null)?.replaceFirstChar { it.uppercase() }
        val genresArray = metaJson.optJSONArray("genres")
        val tags = if (genresArray != null) {
            (0 until genresArray.length()).mapNotNull { genresArray.optJSONObject(it)?.optString("name", null) }
        } else emptyList()
        val userObj = metaJson.optJSONObject("user")
        val author = userObj?.optString("name", null) ?: userObj?.optString("username", null)
        val chapters = loadChapters(slug)
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$slug"
        return NovelDetails(
            url = fullUrl, name = title, chapters = chapters,
            author = author, posterUrl = posterUrl, synopsis = synopsis,
            tags = tags.ifEmpty { null }, status = status
        )
    }

    private suspend fun loadChapters(slug: String): List<Chapter> {
        return try {
            val url = "$mainUrl/api/new/v2/series/$slug/chapters"
            val response = get(url, jsonHeaders)
            val chaptersArray = JSONArray(response.text)
            val chapters = mutableListOf<Chapter>()
            for (i in 0 until chaptersArray.length()) {
                val obj = chaptersArray.optJSONObject(i) ?: continue
                val chapterId = obj.optInt("id", -1).takeIf { it >= 0 } ?: continue
                val chapterTitle = obj.optString("title", null) ?: "Chapter ${obj.optInt("number", i + 1)}"
                val chapterSlug = obj.optString("slug", null) ?: continue
                val groupObj = obj.optJSONObject("group")
                val groupSlug = groupObj?.optString("slug", null)
                val date = obj.optString("created_at", null)
                val chapterPath = buildString {
                    append(slug)
                    append("/")
                    if (!groupSlug.isNullOrBlank()) {
                        append(groupSlug)
                        append("/")
                    }
                    append(chapterSlug)
                    append("~~")
                    append(chapterId)
                }
                val chapterUrl = "$mainUrl/$chapterPath"
                chapters.add(Chapter(name = chapterTitle, url = chapterUrl, dateOfRelease = date))
            }
            chapters
        } catch (_: Throwable) { emptyList() }
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl/$url"
        val parts = fullUrl.split("~~")
        val chapterId = parts.getOrNull(1)?.filter { it.isDigit() }
        if (!chapterId.isNullOrBlank()) {
            val apiUrl = "$mainUrl/api/new/v2/chapters/$chapterId"
            val response = get(apiUrl, jsonHeaders)
            try {
                val json = JSONObject(response.text)
                val contentRaw = json.optString("content", null)
                if (!contentRaw.isNullOrBlank()) {
                    return parseTipTapJson(contentRaw)
                }
            } catch (_: Throwable) {}
        }
        val htmlPath = parts.getOrNull(0) ?: fullUrl
        val document = get(htmlPath).document
        val contentEl = document.selectFirstOrNull("div#chapter-content, div.chapter-content, div.content")
            ?: return null
        contentEl.select("script, style, .ads, .adsbygoogle, [class*='ads']").remove()
        return contentEl.html().trim().takeIf { it.isNotBlank() }
    }

    private fun parseTipTapJson(contentRaw: String): String? {
        return try {
            val doc = JSONObject(contentRaw)
            val contentArray = doc.optJSONArray("content") ?: return null
            val sb = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val node = contentArray.optJSONObject(i) ?: continue
                val nodeHtml = renderTipTapNode(node)
                if (nodeHtml.isNotBlank()) sb.append(nodeHtml).append("\n")
            }
            sb.toString().trim().takeIf { it.isNotBlank() }
        } catch (_: Throwable) { null }
    }

    private fun renderTipTapNode(node: JSONObject): String {
        val type = node.optString("type", "")
        val children = node.optJSONArray("content")
        val attrs = node.optJSONObject("attrs")
        return when (type) {
            "doc" -> {
                if (children == null) return ""
                val sb = StringBuilder()
                for (i in 0 until children.length()) {
                    val child = children.optJSONObject(i) ?: continue
                    sb.append(renderTipTapNode(child)).append("\n")
                }
                sb.toString()
            }
            "paragraph" -> {
                val inner = renderInlineNodes(children)
                if (inner.isBlank()) "<p></p>" else "<p>$inner</p>"
            }
            "heading" -> {
                val level = attrs?.optInt("level", 1) ?: 1
                val clampedLevel = level.coerceIn(1, 6)
                val inner = renderInlineNodes(children)
                "<h$clampedLevel>$inner</h$clampedLevel>"
            }
            "blockquote" -> {
                val inner = if (children != null) {
                    val sb = StringBuilder()
                    for (i in 0 until children.length()) {
                        val child = children.optJSONObject(i) ?: continue
                        sb.append(renderTipTapNode(child))
                    }
                    sb.toString()
                } else ""
                "<blockquote>$inner</blockquote>"
            }
            "bulletList" -> {
                val items = renderListItems(children)
                "<ul>$items</ul>"
            }
            "orderedList" -> {
                val items = renderListItems(children)
                "<ol>$items</ol>"
            }
            "listItem" -> {
                val inner = if (children != null) {
                    val sb = StringBuilder()
                    for (i in 0 until children.length()) {
                        val child = children.optJSONObject(i) ?: continue
                        sb.append(renderTipTapNode(child))
                    }
                    sb.toString()
                } else ""
                "<li>$inner</li>"
            }
            "hardBreak" -> "<br/>"
            "horizontalRule" -> "<hr/>"
            "text" -> renderInlineNodes(null, node)
            else -> {
                if (children != null) {
                    val sb = StringBuilder()
                    for (i in 0 until children.length()) {
                        val child = children.optJSONObject(i) ?: continue
                        sb.append(renderTipTapNode(child))
                    }
                    sb.toString()
                } else ""
            }
        }
    }

    private fun renderListItems(children: JSONArray?): String {
        if (children == null) return ""
        val sb = StringBuilder()
        for (i in 0 until children.length()) {
            val child = children.optJSONObject(i) ?: continue
            sb.append(renderTipTapNode(child))
        }
        return sb.toString()
    }

    private fun renderInlineNodes(children: JSONArray?, singleNode: JSONObject? = null): String {
        val nodes = if (singleNode != null) listOf(singleNode)
        else if (children != null) (0 until children.length()).mapNotNull { children.optJSONObject(it) }
        else return ""
        val sb = StringBuilder()
        for (node in nodes) {
            val type = node.optString("type", "")
            if (type == "text") {
                val text = node.optString("text", "")
                val marks = node.optJSONArray("marks")
                val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val wrappedText = if (marks != null) applyMarks(escapedText, marks) else escapedText
                sb.append(wrappedText)
            } else if (type == "hardBreak") {
                sb.append("<br/>")
            } else {
                sb.append(renderTipTapNode(node))
            }
        }
        return sb.toString()
    }

    private fun applyMarks(text: String, marks: JSONArray): String {
        var result = text
        for (i in 0 until marks.length()) {
            val mark = marks.optJSONObject(i) ?: continue
            result = when (mark.optString("type", "")) {
                "bold" -> "<b>$result</b>"
                "italic" -> "<i>$result</i>"
                "underline" -> "<u>$result</u>"
                "strike" -> "<strike>$result</strike>"
                "code" -> "<code>$result</code>"
                "link" -> {
                    val href = mark.optJSONObject("attrs")?.optString("href", null)
                    if (!href.isNullOrBlank()) "<a href=\"$href\">$result</a>" else result
                }
                else -> result
            }
        }
        return result
    }
}
