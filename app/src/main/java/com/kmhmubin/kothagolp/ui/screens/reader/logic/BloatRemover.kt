package com.kmhmubin.kothagolp.ui.screens.reader.logic

import org.jsoup.Jsoup

/**
 * "Remove Bloat" (inspired by QuickNovel Enhanced): strips per-chapter noise
 * that translation sites bake into the content itself —
 *
 *  - the chapter title duplicated as the first heading/paragraph
 *    (the reader already shows the chapter name in its own chrome)
 *  - translator / editor / proofreader credit lines
 *
 * Removal works on whole block elements, never on substrings, so a credit
 * word appearing mid-story can't damage real prose.
 */
object BloatRemover {

    /** Credit line: block starts with a role label followed by a colon. */
    private val creditLineRegex = Regex(
        "^\\s*(translator|translated by|translation|editor|edited by|" +
            "proofreader|proofread by|proofer|tl|tlc|ed|pr)\\s*[:：]",
        RegexOption.IGNORE_CASE
    )

    /** "Chapter 12", "Chapter 12: Title", "Ch. 12 - Title" style headings. */
    private val chapterHeadingRegex = Regex(
        "^\\s*(chapter|ch\\.?)\\s*\\d+\\s*([:.\\-–—].*)?$",
        RegexOption.IGNORE_CASE
    )

    private const val MAX_CREDIT_LINE_LENGTH = 120
    private const val LEADING_BLOCKS_TO_CHECK = 3

    fun strip(html: String, chapterName: String?): String {
        return try {
            val document = Jsoup.parse(html)
            val normalizedTitle = chapterName?.let(::normalize)

            // Duplicated chapter title anywhere in the content (QuickNovel rule)
            if (!normalizedTitle.isNullOrBlank()) {
                document.body().allElements
                    .firstOrNull { el ->
                        el.childrenSize() <= 1 && el.hasText() &&
                            normalize(el.text()) == normalizedTitle
                    }
                    ?.remove()
            }

            val blocks = document.body().select("p, h1, h2, h3, h4, h5, h6, div, blockquote")

            // Generic "Chapter N ..." heading among the first few blocks
            blocks.take(LEADING_BLOCKS_TO_CHECK)
                .firstOrNull { el ->
                    el.childrenSize() == 0 && el.hasText() &&
                        chapterHeadingRegex.matches(el.text().trim())
                }
                ?.remove()

            // Credit lines: short leaf blocks starting with a role label
            for (el in blocks) {
                if (el.parents().isEmpty()) continue // already removed with parent
                val text = el.text()
                if (text.length <= MAX_CREDIT_LINE_LENGTH &&
                    el.select("p, div, blockquote").size <= 1 &&
                    creditLineRegex.containsMatchIn(text)
                ) {
                    el.remove()
                }
            }

            document.body().html()
        } catch (_: Throwable) {
            html
        }
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("\\s+"), " ").trim()
}
