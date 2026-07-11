package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.R
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.FilterOption
import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import com.kmhmubin.kothagolp.provider.MainProvider

class LightNovelTranslationsProvider : MainProvider() {

    override val name = "Light Novel Translations"
    override val mainUrl = "https://lightnovelstranslations.com"
    override val iconRes = R.drawable.ic_provider_lightnoveltranslations
    override val hasMainPage = true

    override val orderBys = listOf(
        FilterOption("Most Liked", "most-liked"),
        FilterOption("Most Recent", "most-recent")
    )

    override val tags = listOf(
        FilterOption("All", "all"),
        FilterOption("Ongoing", "ongoing"),
        FilterOption("Completed", "completed")
    )

    override suspend fun loadMainPage(
        page: Int, orderBy: String?, tag: String?, extraFilters: Map<String, String>
    ): MainPageResult {
        val sort = orderBy.takeUnless { it.isNullOrEmpty() } ?: "most-liked"
        val status = tag.takeUnless { it.isNullOrEmpty() } ?: "all"

        // Page 1 → fetch site pages 1-3 (~18 novels). Page N → fetch site page N+2.
        val range = if (page == 1) 1..3 else {
            val sitePage = page + 2
            sitePage..sitePage
        }

        val novels = mutableListOf<Novel>()
        var lastUrl = ""
        var hasNext = false
        for (i in range) {
            val url = "$mainUrl/read/page/$i?sortby=$sort&status=$status"
            lastUrl = url
            val document = get(url).document
            novels.addAll(document.select("div.read_list-story-item").mapNotNull { el ->
                val link = el.selectFirstOrNull(".item_thumb a") ?: return@mapNotNull null
                val href = link.attrOrNull("href") ?: return@mapNotNull null
                val title = link.attrOrNull("title")?.ifBlank { null } ?: link.textOrNull() ?: return@mapNotNull null
                val posterUrl = fixUrl(el.selectFirstOrNull(".item_thumb img")?.attrOrNull("src"))
                Novel(name = title, url = fixUrl(href) ?: href, posterUrl = posterUrl, apiName = this.name)
            })
            if (i == range.last) {
                hasNext = document.selectFirstOrNull("a.next.page-numbers, a:contains(Next), a.page-link[rel=next]") != null
                    || novels.size >= 18
            }
        }

        return MainPageResult(url = lastUrl, novels = novels, hasNextPage = hasNext)
    }

    override suspend fun search(query: String): List<Novel> {
        val response = post("$mainUrl/read", data = mapOf("field-search" to query.trim()))
        return response.document.select("div.read_list-story-item").mapNotNull { el ->
            val link = el.selectFirstOrNull(".item_thumb a") ?: return@mapNotNull null
            val href = link.attrOrNull("href") ?: return@mapNotNull null
            val title = link.attrOrNull("title")?.ifBlank { null } ?: link.textOrNull() ?: return@mapNotNull null
            val posterUrl = fixUrl(el.selectFirstOrNull(".item_thumb img")?.attrOrNull("src"))
            Novel(name = title, url = fixUrl(href) ?: href, posterUrl = posterUrl, apiName = this.name)
        }
    }

    override suspend fun load(url: String): NovelDetails? {
        // Chapters are on ?tab=table_contents; synopsis+genres on the base overview URL
        val chaptersUrl = if (url.contains("?tab=")) url
            else if (url.contains("?")) "$url&tab=table_contents"
            else "$url?tab=table_contents"
        val fullUrl = if (chaptersUrl.startsWith("http")) chaptersUrl else "$mainUrl$chaptersUrl"
        val overviewUrl = fullUrl.substringBefore("?")

        val document = get(fullUrl).document
        val name = document.selectFirstOrNull("div.novel_title h3")?.textOrNull()?.trim()
            ?: document.selectFirstOrNull("h1")?.textOrNull()?.trim()
            ?: return null

        val posterUrl = fixUrl(
            document.selectFirstOrNull("div.novel-image img, div.novel_image img")
                ?.let { it.attrOrNull("src") ?: it.attrOrNull("data-src") }
        )
        val author = document.selectFirstOrNull("div.novel_detail_info li:contains(Author)")
            ?.textOrNull()?.substringAfter("Author")?.replace(":", "")?.trim()
        val status = document.selectFirstOrNull("div.novel_status")?.textOrNull()?.trim()

        val chapters = document.select("li.chapter-item.unlock > a, li.chapter-item.unlock a").mapNotNull { link ->
            val href = link.attrOrNull("href") ?: return@mapNotNull null
            val chapterUrl = fixUrl(href) ?: href
            val chapterName = link.textOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Chapter(name = chapterName, url = chapterUrl)
        }

        // Second request for synopsis and genres (on overview tab, not chapters tab)
        var synopsis: String? = null
        var tags: List<String>? = null
        try {
            val overviewDoc = get(overviewUrl).document
            synopsis = overviewDoc.selectFirstOrNull("div.novel_text p, div.novel_detail_body p")
                ?.textOrNull()?.trim()
            tags = overviewDoc.select("div.novel_detail_info li:contains(Genre) a, div.novel_detail_info li:contains(Categories) a, div.novel_tags a")
                .mapNotNull { it.textOrNull()?.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { null }
        } catch (_: Exception) {}

        return NovelDetails(
            url = fullUrl, name = name, chapters = chapters,
            author = author, posterUrl = posterUrl, synopsis = synopsis,
            tags = tags, status = status
        )
    }

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"
        val document = get(fullUrl).document
        val content = document.selectFirstOrNull("div.text_story") ?: return null
        content.select("div.ads_content, script, style, .adsbygoogle, [class*='ads'], ins").remove()
        return content.html().trim().takeIf { it.isNotBlank() }
    }
}
