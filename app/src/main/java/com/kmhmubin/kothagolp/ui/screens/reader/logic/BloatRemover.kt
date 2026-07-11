package com.kmhmubin.kothagolp.ui.screens.reader.logic

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * "Remove Bloat" (inspired by QuickNovel Enhanced): strips per-chapter noise
 * that translation sites bake into the content itself —
 *
 *  - the chapter title duplicated at the top of the content
 *    (the reader already shows the chapter name in its own chrome)
 *  - translator / editor / proofreader / quality-checker credit lines
 *
 * Removal works on whole leaf blocks, never on substrings, so a credit word
 * appearing mid-story can't damage real prose.
 */
object BloatRemover {

    private const val ROLES =
        "translator|translated by|translation|trans|editor|edited by|" +
            "proofreader|proofread by|proofer|quality checker|quality check|" +
            "checker|qc|tl|tlc|ed|pr|raw provider|raws|raw|scheduler|typesetter"

    /** Credit line: optionally decorated block starting with a role label + colon. */
    private val creditLineRegex = Regex(
        "^\\s*[\\[({\\-–—~*_•>«\"']*\\s*($ROLES)\\s*[:：]",
        RegexOption.IGNORE_CASE
    )

    /** "Chapter 12", "Chapter 12: Title", "Ch. 12 - Title", "Episode 3" headings. */
    private val chapterHeadingRegex = Regex(
        "^\\s*(chapter|ch\\.?|episode|ep\\.?|vol\\.?\\s*\\d+\\s*(chapter|ch\\.?))\\s*\\d+([\\s:.\\-–—~|,].*)?$",
        RegexOption.IGNORE_CASE
    )

    /** Strips "Chapter 12:" style prefixes for fuzzy title comparison. */
    private val chapterPrefixRegex = Regex(
        "^\\s*(chapter|ch\\.?|episode|ep\\.?)\\s*\\d+\\s*[:.\\-–—~|,]?\\s*",
        RegexOption.IGNORE_CASE
    )

    private const val MAX_CREDIT_LINE_LENGTH = 160
    private const val MAX_TITLE_LENGTH = 120
    private const val LEADING_BLOCKS_TO_CHECK = 6

    fun strip(html: String, chapterName: String?): String {
        return try {
            val document = Jsoup.parse(html)

            // Leaf blocks: elements that contain text directly, not through
            // nested block children. Wrapper divs are excluded so they don't
            // consume the "leading blocks" window.
            val leafBlocks = document.body()
                .select("p, h1, h2, h3, h4, h5, h6, div, blockquote, li")
                .filter { el ->
                    el.hasText() && el.select("p, div, blockquote, h1, h2, h3, h4, h5, h6").size <= 1
                }

            stripTitle(leafBlocks, chapterName)
            stripCreditLines(leafBlocks)

            document.body().html()
        } catch (_: Throwable) {
            html
        }
    }

    private fun stripTitle(leafBlocks: List<Element>, chapterName: String?) {
        val normalizedTitle = chapterName?.let(::normalize)
        val titleCore = normalizedTitle?.replace(chapterPrefixRegex, "")?.trim()

        for (el in leafBlocks.take(LEADING_BLOCKS_TO_CHECK)) {
            val text = el.text().trim()
            if (text.length > MAX_TITLE_LENGTH) continue
            val normalized = normalize(text)
            val core = normalized.replace(chapterPrefixRegex, "").trim()

            val isTitle = when {
                // Exact or fuzzy match against the chapter name from the source
                !normalizedTitle.isNullOrBlank() && normalized == normalizedTitle -> true
                !titleCore.isNullOrBlank() && core.isNotBlank() && core == titleCore -> true
                // Generic "Chapter N ..." heading near the top; headings get more
                // leeway, plain paragraphs must be short to avoid eating prose
                chapterHeadingRegex.matches(text) &&
                    (el.tagName().startsWith("h") || text.length <= 80) -> true
                else -> false
            }
            if (isTitle) {
                el.remove()
                // Don't break: some sources duplicate the title twice
            }
        }
    }

    private fun stripCreditLines(leafBlocks: List<Element>) {
        for (el in leafBlocks) {
            val text = el.text()
            if (text.length <= MAX_CREDIT_LINE_LENGTH && creditLineRegex.containsMatchIn(text)) {
                el.remove()
            }
        }
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("\\s+"), " ").trim()
}
