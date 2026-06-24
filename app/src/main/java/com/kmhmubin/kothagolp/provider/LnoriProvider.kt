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

class LnoriProvider : MainProvider() {

    override val name = "Lnori"
    override val mainUrl = "https://lnori.com"
    override val iconUrl = "https://www.google.com/s2/favicons?domain=lnori.com&sz=64"
    override val hasMainPage = true

    private val pageSize = 50

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Action", "action"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Dark Fantasy", "dark-fantasy"),
        FilterOption("Drama", "drama"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Female Protagonist", "female-protagonist"),
        FilterOption("Game Elements", "game-elements"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Harem", "harem"),
        FilterOption("Historical", "historical"),
        FilterOption("Horror", "horror"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("Magic", "magic"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Romance", "romance"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-Fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Tragedy", "tragedy"),
        FilterOption("Villainess", "villainess")
    )

    override val orderBys = listOf(FilterOption("Popular", ""))

    private fun parseNovelCard(element: Element): Novel? {
        val seriesId = element.attrOrNull("data-id") ?: return null
        val title = element.attrOrNull("data-t")?.trim()
        if (title.isNullOrBlank()) return null
        val linkElement = element.selectFirstOrNull("a.stretched-link[href^=\"/series/\"]")
            ?: element.selectFirstOrNull("a[href^=\"/series/\"]")
        val href = linkElement?.attrOrNull("href") ?: return null
        val imgElement = element.selectFirstOrNull("figure.card-cover img")
        val posterUrl = imgElement?.attrOrNull("src")
        val volumeCount = element.attrOrNull("data-v")?.toIntOrNull()
        val latestChapter = volumeCount?.let { "$it Volumes" }
        return Novel(
            name = title, url = fixUrl(href) ?: return null,
            posterUrl = posterUrl, latestChapter = latestChapter, apiName = this.name
        )
    }

    private fun parseTagsFromDocument(document: Document): List<String> {
        val tagsNav = document.selectFirstOrNull("nav.tags-box[data-tags]")
        val dataTagsJson = tagsNav?.attrOrNull("data-tags")
        if (!dataTagsJson.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(dataTagsJson)
                val tags = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val tagName = obj.optString("name", null)
                    if (!tagName.isNullOrBlank()) tags.add(tagName)
                }
                if (tags.isNotEmpty()) return tags
            } catch (_: Throwable) {}
        }
        return document.select("nav.tags-box a.tag").mapNotNull { it.textOrNull()?.trim() }.filter { it.isNotBlank() }
    }

    private fun parseVolumes(document: Document): List<Chapter> {
        return document.select("section.vol-grid article.card").mapNotNull { volumeCard ->
            val linkElement = volumeCard.selectFirstOrNull("a.stretched-link[href^=\"/book/\"]")
                ?: volumeCard.selectFirstOrNull("a[href^=\"/book/\"]")
            val href = linkElement?.attrOrNull("href") ?: return@mapNotNull null
            val volumeTitle = volumeCard.selectFirstOrNull("h3.card-title span")?.textOrNull()?.trim()
                ?: linkElement.attrOrNull("aria-label") ?: "Volume"
            Chapter(name = volumeTitle, url = fixUrl(href) ?: return@mapNotNull null)
        }
    }

    private fun cleanChapterHtml(html: String): String {
        var cleaned = html
        cleaned = cleaned.replace(Regex("(<hr\\s*/?>\\s*){2,}"), "<hr/>\n")
        cleaned = cleaned.replace(Regex("<p>\\s*</p>"), "")
        cleaned = cleaned.replace(Regex("<div>\\s*</div>"), "")
        cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")
        return cleaned.trim()
    }

    private fun parseSrcset(srcset: String): String? {
        if (srcset.isBlank()) return null
        val firstEntry = srcset.split(",").firstOrNull()?.trim() ?: return null
        val parts = firstEntry.split(Regex("\\s+"))
        val url = parts.firstOrNull()?.trim() ?: return null
        return if (url.isNotBlank() && url.startsWith("http")) url else null
    }

    private fun extractImageUrl(picture: Element): String? {
        val img = picture.selectFirstOrNull("img")
        val directSrc = img?.attrOrNull("src")
        if (!directSrc.isNullOrBlank() && directSrc.startsWith("http")) return directSrc
        val imgSrcset = img?.attrOrNull("srcset")
        if (!imgSrcset.isNullOrBlank()) {
            val url = parseSrcset(imgSrcset)
            if (!url.isNullOrBlank()) return url
        }
        for (source in picture.select("source")) {
            val srcset = source.attrOrNull("srcset")
            if (!srcset.isNullOrBlank()) {
                val url = parseSrcset(srcset)
                if (!url.isNullOrBlank()) return url
            }
        }
        return null
    }

    private fun processPictures(container: Element) {
        container.select("picture").forEach { picture ->
            val src = extractImageUrl(picture)
            val alt = picture.selectFirstOrNull("img")?.attrOrNull("alt") ?: "Image"
            if (!src.isNullOrBlank()) picture.html("<img src=\"$src\" alt=\"$alt\" />")
        }
        container.select("img").forEach { img ->
            val currentSrc = img.attrOrNull("src") ?: ""
            if (currentSrc.isBlank() || currentSrc.startsWith("data:image/gif") || currentSrc.length < 100) {
                val realSrc = img.attrOrNull("data-src") ?: img.attrOrNull("data-lazy-src")
                    ?: img.attrOrNull("data-original") ?: img.attrOrNull("data-image")
                if (!realSrc.isNullOrBlank() && realSrc.startsWith("http")) img.attr("src", realSrc)
            }
        }
    }

    private fun hasTextContent(element: Element): Boolean =
        element.text()?.trim()?.any { it.isLetter() } == true

    private fun hasImages(element: Element): Boolean =
        element.select("img, picture, figure, source").isNotEmpty()

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val url = if (tag.isNullOrBlank()) "$mainUrl/library" else "$mainUrl/genre/$tag"
        val document = get(url).document
        val allNovels = document.select("article.card").mapNotNull { parseNovelCard(it) }
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allNovels.size)
        val novels = if (startIndex < allNovels.size) allNovels.subList(startIndex, endIndex) else emptyList()
        return MainPageResult(url = url, novels = novels)
    }

    override suspend fun search(query: String): List<Novel> {
        val response = get("$mainUrl/library")
        val document = response.document
        val queryLower = query.lowercase().trim()
        return document.select("article.card").mapNotNull { element ->
            val title = element.attrOrNull("data-t")?.trim() ?: return@mapNotNull null
            val author = element.attrOrNull("data-a")?.trim() ?: ""
            val tags = element.attrOrNull("data-tags")?.lowercase() ?: ""
            if (title.lowercase().contains(queryLower) || author.lowercase().contains(queryLower) || tags.contains(queryLower))
                parseNovelCard(element) else null
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document
        val name = document.selectFirstOrNull("h1.s-title")?.textOrNull()?.trim() ?: return null
        val author = document.selectFirstOrNull("p.author")?.textOrNull()?.trim()
        val posterUrl = document.selectFirstOrNull("figure.cover-wrap img")?.attrOrNull("src")
        val synopsis = document.selectFirstOrNull("p.description.desc-wrapper")?.textOrNull()?.trim()
            ?: "No description available."
        val tags = parseTagsFromDocument(document)
        val chapters = parseVolumes(document)
        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters, author = author,
            posterUrl = posterUrl, synopsis = synopsis, tags = tags.ifEmpty { null }
        )
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document
        var chapterSections = document.select("section.chapter[id^=\"page\"]").let { if (it.isNotEmpty()) it else document.select("section.chapter") }
            .let { if (it.isNotEmpty()) it else document.select("section[id^=\"page\"]") }
            .let { if (it.isNotEmpty()) it else document.select("article.chapter, article[id^=\"page\"]") }

        if (chapterSections.isEmpty()) {
            val mainContent = document.selectFirstOrNull("div.main") ?: document.selectFirstOrNull("main")
                ?: document.selectFirstOrNull("article") ?: document.selectFirstOrNull("div.content")
            if (mainContent != null) {
                val clone = mainContent.clone()
                processPictures(clone)
                val html = clone.html().trim()
                return if (html.isNotBlank()) cleanChapterHtml(html) else null
            }
            return null
        }

        val contentBuilder = StringBuilder()
        for ((index, section) in chapterSections.withIndex()) {
            val sectionId = section.attrOrNull("id")?.lowercase() ?: ""
            val sectionClasses = section.attrOrNull("class")?.lowercase() ?: ""
            if (sectionId.contains("cover") || sectionClasses.contains("cover")) continue

            val innerContent = section.selectFirstOrNull("section.body-rw.Chapter-rw")
                ?: section.selectFirstOrNull("section.Chapter-rw")
                ?: section.selectFirstOrNull("div.galley-rw")
                ?: section.selectFirstOrNull("div.main")
                ?: section.selectFirstOrNull("div.content")

            if (innerContent != null) {
                val contentClone = innerContent.clone()
                processPictures(contentClone)
                contentClone.select("p, div, span, section").forEach { el ->
                    if (!hasTextContent(el) && !hasImages(el)) el.remove()
                }
                val html = contentClone.html().trim()
                if (html.isNotBlank()) { contentBuilder.append(html); contentBuilder.append("\n") }
            } else {
                val sectionClone = section.clone()
                processPictures(sectionClone)
                sectionClone.select("nav, header, footer, .nav, .header, .footer").remove()
                sectionClone.select("p, div, span, section").forEach { el ->
                    if (!hasTextContent(el) && !hasImages(el)) el.remove()
                }
                val html = sectionClone.html().trim()
                if (html.isNotBlank()) { contentBuilder.append(html); contentBuilder.append("\n") }
            }
            if (index < chapterSections.size - 1) contentBuilder.append("<hr/>\n")
        }

        val result = contentBuilder.toString().trim()
        return if (result.isBlank()) null else cleanChapterHtml(result)
    }
}
