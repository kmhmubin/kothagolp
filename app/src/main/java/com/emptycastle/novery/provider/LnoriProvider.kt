package com.emptycastle.novery.provider

import com.emptycastle.novery.R
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FilterOption
import com.emptycastle.novery.domain.model.MainPageResult
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class LnoriProvider : MainProvider() {

    override val name = "Lnori"
    override val mainUrl = "https://lnori.com"
    override val hasMainPage = true
    override val hasReviews = false
    override val iconRes: Int = R.drawable.ic_provider_lnori

    private val pageSize = 50

    override val tags = listOf(
        FilterOption("All", ""),
        FilterOption("Academy", "academy"),
        FilterOption("Action", "action"),
        FilterOption("Adult Protagonist", "adult-protagonist"),
        FilterOption("Adventure", "adventure"),
        FilterOption("Age Gap", "age-gap"),
        FilterOption("Airhead", "airhead"),
        FilterOption("Alchemy", "alchemy"),
        FilterOption("Animals", "animals"),
        FilterOption("Anime Tie-In", "anime-tie-in"),
        FilterOption("Aristocracy", "aristocracy"),
        FilterOption("Battle", "battle"),
        FilterOption("Books", "books"),
        FilterOption("Boys Love", "boys-love"),
        FilterOption("Business", "business"),
        FilterOption("Camping", "camping"),
        FilterOption("Childhood Friend", "childhood-friend"),
        FilterOption("Chuunibyou", "chuunibyou"),
        FilterOption("Combat", "combat"),
        FilterOption("Comedy", "comedy"),
        FilterOption("Contract Marriage", "contract-marriage"),
        FilterOption("Cooking", "cooking"),
        FilterOption("Crime", "crime"),
        FilterOption("Cross-Dressing", "cross-dressing"),
        FilterOption("Dark", "dark"),
        FilterOption("Dark Fantasy", "dark-fantasy"),
        FilterOption("Demon Lord", "demon-lord"),
        FilterOption("Demons", "demons"),
        FilterOption("Dragons", "dragons"),
        FilterOption("Drama", "drama"),
        FilterOption("Dungeon", "dungeon"),
        FilterOption("Dungeon Diving", "dungeon-diving"),
        FilterOption("Dystopian", "dystopian"),
        FilterOption("Ecchi", "ecchi"),
        FilterOption("Elf", "elf"),
        FilterOption("Enemies to Lovers", "enemies-to-lovers"),
        FilterOption("Fairies", "fairies"),
        FilterOption("Familiars", "familiars"),
        FilterOption("Family", "family"),
        FilterOption("Fanservice", "fanservice"),
        FilterOption("Fantasy", "fantasy"),
        FilterOption("Fantasy World", "fantasy-world"),
        FilterOption("Female Protagonist", "female-protagonist"),
        FilterOption("First Person", "first-person"),
        FilterOption("Fish Out of Water", "fish-out-of-water"),
        FilterOption("Food", "food"),
        FilterOption("Friendship", "friendship"),
        FilterOption("Futuristic", "futuristic"),
        FilterOption("Game Elements", "game-elements"),
        FilterOption("Gamer Protagonist", "gamer-protagonist"),
        FilterOption("Gender Bender", "gender-bender"),
        FilterOption("Genius", "genius"),
        FilterOption("Girls Love", "girls-love"),
        FilterOption("Guns", "guns"),
        FilterOption("Harem", "harem"),
        FilterOption("Heartwarming", "heartwarming"),
        FilterOption("High Fantasy", "high-fantasy"),
        FilterOption("High School", "high-school"),
        FilterOption("Historical", "historical"),
        FilterOption("Historical Fantasy", "historical-fantasy"),
        FilterOption("Horror", "horror"),
        FilterOption("Humor", "humor"),
        FilterOption("Invention", "invention"),
        FilterOption("Isekai", "isekai"),
        FilterOption("Josei", "josei"),
        FilterOption("Knights", "knights"),
        FilterOption("LGBTQ", "lgbtq"),
        FilterOption("Lighthearted", "lighthearted"),
        FilterOption("Literary", "literary"),
        FilterOption("Magic", "magic"),
        FilterOption("Magic Academy", "magic-academy"),
        FilterOption("Magical Weapons", "magical-weapons"),
        FilterOption("Maid", "maid"),
        FilterOption("Male Protagonist", "male-protagonist"),
        FilterOption("Manga Tie-In", "manga-tie-in"),
        FilterOption("Marriage", "marriage"),
        FilterOption("Martial Arts", "martial-arts"),
        FilterOption("Master and Servant", "master-and-servant"),
        FilterOption("Mature", "mature"),
        FilterOption("Mecha", "mecha"),
        FilterOption("Medieval", "medieval"),
        FilterOption("Military", "military"),
        FilterOption("Modern Day", "modern-day"),
        FilterOption("Moe", "moe"),
        FilterOption("Monster Girls", "monster-girls"),
        FilterOption("Monster Taming", "monster-taming"),
        FilterOption("Monsters", "monsters"),
        FilterOption("Multiple POV", "multiple-pov"),
        FilterOption("Mystery", "mystery"),
        FilterOption("Nobility", "nobility"),
        FilterOption("Not the Hero", "not-the-hero"),
        FilterOption("OP Power", "op-power"),
        FilterOption("OP Protagonist", "op-protagonist"),
        FilterOption("Ordinary Protagonist", "ordinary-protagonist"),
        FilterOption("Otaku", "otaku"),
        FilterOption("Otome", "otome"),
        FilterOption("Otome Game", "otome-game"),
        FilterOption("Overpowered", "overpowered"),
        FilterOption("Paranormal", "paranormal"),
        FilterOption("Past Life", "past-life"),
        FilterOption("Period Piece", "period-piece"),
        FilterOption("Personal Growth", "personal-growth"),
        FilterOption("Political Marriage", "political-marriage"),
        FilterOption("Politics", "politics"),
        FilterOption("Princess", "princess"),
        FilterOption("Reincarnation", "reincarnation"),
        FilterOption("Revenge", "revenge"),
        FilterOption("Reverse Harem", "reverse-harem"),
        FilterOption("Rewriting History", "rewriting-history"),
        FilterOption("Romance", "romance"),
        FilterOption("Romantic Fantasy", "romantic-fantasy"),
        FilterOption("RPG", "rpg"),
        FilterOption("Satire", "satire"),
        FilterOption("School", "school"),
        FilterOption("School Life", "school-life"),
        FilterOption("Sci-Fi", "sci-fi"),
        FilterOption("Seinen", "seinen"),
        FilterOption("Shoujo", "shoujo"),
        FilterOption("Shounen", "shounen"),
        FilterOption("Slice of Life", "slice-of-life"),
        FilterOption("Slow Life", "slow-life"),
        FilterOption("Snarky Protagonist", "snarky-protagonist"),
        FilterOption("Sorcery", "sorcery"),
        FilterOption("Strategy", "strategy"),
        FilterOption("Strong Female Lead", "strong-female-lead"),
        FilterOption("Supernatural", "supernatural"),
        FilterOption("Superpowers", "superpowers"),
        FilterOption("Survival", "survival"),
        FilterOption("Sword and Sorcery", "sword-and-sorcery"),
        FilterOption("Thriller", "thriller"),
        FilterOption("Time Travel", "time-travel"),
        FilterOption("Tsundere", "tsundere"),
        FilterOption("Underdog", "underdog"),
        FilterOption("Unique Ability", "unique-ability"),
        FilterOption("Vampire", "vampire"),
        FilterOption("Video Game", "video-game"),
        FilterOption("Video Game Related", "video-game-related"),
        FilterOption("Video Game Tie-In", "video-game-tie-in"),
        FilterOption("Villainess", "villainess"),
        FilterOption("Violence", "violence"),
        FilterOption("VRMMO", "vrmmo"),
        FilterOption("War", "war"),
        FilterOption("Weak Protagonist", "weak-protagonist"),
        FilterOption("Witch", "witch"),
        FilterOption("Zero to Hero", "zero-to-hero")
    )

    override val orderBys = listOf(
        FilterOption("Popular", "")
    )

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun parseNovelCard(element: Element): Novel? {
        val seriesId = element.attrOrNull("data-id") ?: return null
        val title = element.attrOrNull("data-t")?.trim()
        if (title.isNullOrBlank()) return null

        val author = element.attrOrNull("data-a")?.trim()
        val volumeCount = element.attrOrNull("data-v")?.toIntOrNull()

        val linkElement = element.selectFirstOrNull("a.stretched-link[href^=\"/series/\"]")
            ?: element.selectFirstOrNull("a[href^=\"/series/\"]")
        val href = linkElement?.attrOrNull("href") ?: return null

        val imgElement = element.selectFirstOrNull("figure.card-cover img")
        val posterUrl = imgElement?.attrOrNull("src")

        val latestChapter = volumeCount?.let { "$it Volumes" }

        return Novel(
            name = title,
            url = fixUrl(href) ?: return null,
            posterUrl = posterUrl,
            latestChapter = latestChapter,
            apiName = this.name
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
                    if (!tagName.isNullOrBlank()) {
                        tags.add(tagName)
                    }
                }
                if (tags.isNotEmpty()) return tags
            } catch (_: Throwable) {
            }
        }

        return document.select("nav.tags-box a.tag").mapNotNull {
            it.textOrNull()?.trim()
        }.filter { it.isNotBlank() }
    }

    private fun parseVolumes(document: Document): List<Chapter> {
        return document.select("section.vol-grid article.card").mapNotNull { volumeCard ->
            val linkElement = volumeCard.selectFirstOrNull("a.stretched-link[href^=\"/book/\"]")
                ?: volumeCard.selectFirstOrNull("a[href^=\"/book/\"]")
            val href = linkElement?.attrOrNull("href") ?: return@mapNotNull null

            val volumeTitle = volumeCard.selectFirstOrNull("h3.card-title span")?.textOrNull()?.trim()
                ?: linkElement.attrOrNull("aria-label")
                ?: "Volume"

            Chapter(
                name = volumeTitle,
                url = fixUrl(href) ?: return@mapNotNull null
            )
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

    /**
     * Parse a srcset attribute value to extract the first URL
     * Format: "url1 1x, url2 2x" or "url1 400w, url2 800w" or just "url1"
     */
    private fun parseSrcset(srcset: String): String? {
        if (srcset.isBlank()) return null

        // Get first entry before comma
        val firstEntry = srcset.split(",").firstOrNull()?.trim() ?: return null

        // URL might be followed by descriptor like " 400w" or " 2x"
        val parts = firstEntry.split(Regex("\\s+"))
        val url = parts.firstOrNull()?.trim() ?: return null

        // Return if it's a real URL
        return if (url.isNotBlank() && url.startsWith("http")) url else null
    }

    /**
     * Extract image URL from a <picture> element
     */
    private fun extractImageUrl(picture: Element): String? {
        // 1. Try img src directly (if it's a real URL)
        val img = picture.selectFirstOrNull("img")
        val directSrc = img?.attrOrNull("src")
        if (!directSrc.isNullOrBlank() && directSrc.startsWith("http")) {
            return directSrc
        }

        // 2. Try img srcset
        val imgSrcset = img?.attrOrNull("srcset")
        if (!imgSrcset.isNullOrBlank()) {
            val url = parseSrcset(imgSrcset)
            if (!url.isNullOrBlank()) return url
        }

        // 3. Try <source> elements (usually in order of preference)
        for (source in picture.select("source")) {
            // Try srcset first
            val srcset = source.attrOrNull("srcset")
            if (!srcset.isNullOrBlank()) {
                val url = parseSrcset(srcset)
                if (!url.isNullOrBlank()) return url
            }

            // Try src
            val src = source.attrOrNull("src")
            if (!src.isNullOrBlank() && src.startsWith("http")) {
                return src
            }
        }

        // 4. Fallback: return data: URL if that's all we have (better than nothing)
        if (!directSrc.isNullOrBlank() && directSrc.startsWith("data:")) {
            return directSrc
        }

        return null
    }

    /**
     * Process all <picture> and <img> elements to ensure they have valid src attributes
     */
    private fun processPictures(container: Element) {
        // Process <picture> elements - convert to simple <img>
        container.select("picture").forEach { picture ->
            val src = extractImageUrl(picture)
            val alt = picture.selectFirstOrNull("img")?.attrOrNull("alt") ?: "Image"

            if (!src.isNullOrBlank()) {
                picture.html("<img src=\"$src\" alt=\"$alt\" />")
            }
            // IMPORTANT: Don't remove if extraction fails - keep original HTML intact
            // The reader app might be able to handle it
        }

        // Fix lazy-loaded standalone <img> tags
        container.select("img").forEach { img ->
            val currentSrc = img.attrOrNull("src") ?: ""

            // If src is empty, data:, or a tiny placeholder
            if (currentSrc.isBlank() || currentSrc.startsWith("data:image/gif") || currentSrc.length < 100) {
                // Try common lazy-load data attributes
                val realSrc = img.attrOrNull("data-src")
                    ?: img.attrOrNull("data-lazy-src")
                    ?: img.attrOrNull("data-original")
                    ?: img.attrOrNull("loading-src")
                    ?: img.attrOrNull("data-lazy")
                    ?: img.attrOrNull("data-image")

                if (!realSrc.isNullOrBlank() && realSrc.startsWith("http")) {
                    img.attr("src", realSrc)
                }
            }
        }

        // Also process standalone <source> elements that might be outside <picture>
        container.select("source").forEach { source ->
            val srcset = source.attrOrNull("srcset")
            if (!srcset.isNullOrBlank()) {
                val url = parseSrcset(srcset)
                if (!url.isNullOrBlank()) {
                    // Replace source with img
                    source.parent()?.let { parent ->
                        val alt = source.attrOrNull("alt") ?: "Image"
                        source.after("<img src=\"$url\" alt=\"$alt\" />")
                        source.remove()
                    }
                }
            }
        }
    }

    /**
     * Check if an element contains actual text content (at least one letter)
     */
    private fun hasTextContent(element: Element): Boolean {
        val text = element.text()?.trim() ?: return false
        return text.any { it.isLetter() }
    }

    /**
     * Check if an element contains any images
     */
    private fun hasImages(element: Element): Boolean {
        return element.select("img, picture, figure, source").isNotEmpty()
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
        val url = if (tag.isNullOrBlank()) {
            "$mainUrl/library"
        } else {
            "$mainUrl/genre/$tag"
        }

        val response = get(url)
        val document = response.document

        val allNovels = document.select("article.card").mapNotNull { element ->
            parseNovelCard(element)
        }

        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allNovels.size)

        val novels = if (startIndex < allNovels.size) {
            allNovels.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return MainPageResult(url = url, novels = novels)
    }

    // ================================================================
    // SEARCH
    // ================================================================

    override suspend fun search(query: String): List<Novel> {
        val response = get("$mainUrl/library")
        val document = response.document

        val queryLower = query.lowercase().trim()

        return document.select("article.card").mapNotNull { element ->
            val title = element.attrOrNull("data-t")?.trim() ?: return@mapNotNull null
            val author = element.attrOrNull("data-a")?.trim() ?: ""
            val tags = element.attrOrNull("data-tags")?.lowercase() ?: ""

            if (title.lowercase().contains(queryLower) ||
                author.lowercase().contains(queryLower) ||
                tags.contains(queryLower)
            ) {
                parseNovelCard(element)
            } else {
                null
            }
        }
    }

    // ================================================================
    // LOAD NOVEL DETAILS
    // ================================================================

    override suspend fun load(url: String): NovelDetails? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document

        val name = document.selectFirstOrNull("h1.s-title")?.textOrNull()?.trim()
            ?: return null

        val author = document.selectFirstOrNull("p.author")?.textOrNull()?.trim()
        val posterUrl = document.selectFirstOrNull("figure.cover-wrap img")?.attrOrNull("src")
        val synopsis = document.selectFirstOrNull("p.description.desc-wrapper")?.textOrNull()?.trim()
            ?: "No description available."
        val tags = parseTagsFromDocument(document)
        val chapters = parseVolumes(document)

        return NovelDetails(
            url = fullUrl,
            name = name,
            chapters = chapters,
            author = author,
            posterUrl = posterUrl,
            synopsis = synopsis,
            tags = tags.ifEmpty { null }
        )
    }

    // ================================================================
    // LOAD CHAPTER CONTENT (VOLUME)
    // ================================================================

    override suspend fun loadChapterContent(url: String): String? {
        val fullUrl = if (url.startsWith("http")) url else "$mainUrl$url"

        val response = get(fullUrl)
        val document = response.document

        // Try multiple selectors for chapter sections
        val chapterSections = document.select("section.chapter[id^=\"page\"]").let { sections ->
            if (sections.isNotEmpty()) sections
            else document.select("section.chapter")
        }.let { sections ->
            if (sections.isNotEmpty()) sections
            else document.select("section[id^=\"page\"]")
        }.let { sections ->
            if (sections.isNotEmpty()) sections
            else document.select("article.chapter, article[id^=\"page\"]")
        }

        // If no chapter sections found, try main content area
        if (chapterSections.isEmpty()) {
            val mainContent = document.selectFirst("div.main")
                ?: document.selectFirst("main")
                ?: document.selectFirst("article")
                ?: document.selectFirst("div.content")
                ?: document.selectFirst("div[role=\"main\"]")

            if (mainContent != null) {
                val clone = mainContent.clone()
                processPictures(clone)
                val html = clone.html().trim()
                return if (html.isNotBlank()) {
                    cleanChapterHtml(html)
                } else {
                    null
                }
            }
            return null
        }

        val contentBuilder = StringBuilder()

        for ((index, section) in chapterSections.withIndex()) {
            val sectionId = section.attrOrNull("id")?.lowercase() ?: ""
            val sectionClasses = section.attrOrNull("class")?.lowercase() ?: ""

            // Skip cover-only sections
            if (sectionId.contains("cover") || sectionClasses.contains("cover")) {
                continue
            }

            // Check for standalone chapter title
            val standaloneTitle = section.selectFirstOrNull("> h2.chapter-title, > h2:first-child")
            var titleAdded = false
            if (standaloneTitle != null) {
                val titleText = standaloneTitle.textOrNull()?.trim()
                if (!titleText.isNullOrBlank()) {
                    contentBuilder.append("<h2>${titleText}</h2>\n")
                    titleAdded = true
                }
            }

            // Try multiple content container selectors
            val innerContent = section.selectFirstOrNull("section.body-rw.Chapter-rw")
                ?: section.selectFirstOrNull("section.Chapter-rw")
                ?: section.selectFirstOrNull("section.body-rw")
                ?: section.selectFirstOrNull("div.galley-rw section")
                ?: section.selectFirstOrNull("div.galley-rw")
                ?: section.selectFirstOrNull("div.main")
                ?: section.selectFirstOrNull("div.content")
                ?: section.selectFirstOrNull("article")

            if (innerContent != null) {
                val contentClone = innerContent.clone()

                // Extract headers before removing
                val chapterNumber = contentClone.selectFirstOrNull("h2.chapter-number span")?.textOrNull()?.trim()
                val chapterTitle = contentClone.selectFirstOrNull("h2.chapter-title span")?.textOrNull()?.trim()

                // Remove header elements
                contentClone.select("h2.chapter-number, h2.chapter-title, h2:first-child").remove()

                // Add chapter header if not already added
                if (!titleAdded && (!chapterNumber.isNullOrBlank() || !chapterTitle.isNullOrBlank())) {
                    val headerText = listOfNotNull(chapterNumber, chapterTitle)
                        .filter { it.isNotBlank() }
                        .joinToString(": ")
                    if (headerText.isNotBlank()) {
                        contentBuilder.append("<h2>$headerText</h2>\n")
                    }
                }

                // Process images BEFORE any other cleanup
                processPictures(contentClone)

                // Remove only truly empty elements (no text AND no images)
                contentClone.select("p, div, span, section").forEach { el ->
                    if (!hasTextContent(el) && !hasImages(el)) {
                        el.remove()
                    }
                }

                val html = contentClone.html().trim()
                if (html.isNotBlank()) {
                    contentBuilder.append(html)
                    contentBuilder.append("\n")
                }

            } else {
                // Fallback: extract from section itself
                val sectionClone = section.clone()

                if (titleAdded) {
                    sectionClone.select("> h2.chapter-title, > h2:first-child").remove()
                }

                // Process images FIRST
                processPictures(sectionClone)

                // Remove only structural/navigation elements
                sectionClone.select("nav, header, footer, .nav, .header, .footer").remove()

                // Remove truly empty elements (no text AND no images)
                sectionClone.select("p, div, span, section").forEach { el ->
                    if (!hasTextContent(el) && !hasImages(el)) {
                        el.remove()
                    }
                }

                val html = sectionClone.html().trim()
                if (html.isNotBlank()) {
                    contentBuilder.append(html)
                    contentBuilder.append("\n")
                }
            }

            // Add separator between sections (not after last)
            if (index < chapterSections.size - 1) {
                contentBuilder.append("<hr/>\n")
            }
        }

        val result = contentBuilder.toString().trim()

        return if (result.isBlank()) {
            null
        } else {
            cleanChapterHtml(result)
        }
    }
}