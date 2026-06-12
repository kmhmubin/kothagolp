package com.kmhmubin.kothagolp.epub

/**
 * Represents a chapter in the EPUB
 */
data class EpubChapter(
    val index: Int,
    val title: String,
    val content: String,
    val originalUrl: String = ""
) {
    val fileName: String get() = "chapter_${String.format("%04d", index + 1)}.xhtml"
    val id: String get() = "chapter_${index + 1}"

    /**
     * Clean and convert content to valid XHTML
     */
    fun toXhtml(): String {
        return ContentCleaner.cleanContent(content)
    }
}

/**
 * Utility for cleaning and converting chapter content to XHTML
 * Preserves formatting (bold, italic, etc.) and converts Markdown
 */
object ContentCleaner {

    // Tags we want to KEEP (inline formatting)
    private val ALLOWED_TAGS = setOf(
        "strong", "b", "em", "i", "u", "s", "strike", "del",
        "sub", "sup", "small", "mark", "code", "kbd", "var",
        "cite", "q", "abbr", "span", "a"
    )

    // Tags that indicate paragraph breaks
    private val BLOCK_TAGS = setOf(
        "p", "div", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "ul", "ol", "li", "table", "tr", "td", "th"
    )

    /**
     * Clean raw content and convert to valid XHTML paragraphs
     * Preserves inline formatting and converts Markdown
     */
    fun cleanContent(content: String): String {
        var processed = content

        // Step 1: Normalize line endings
        processed = processed.replace("\r\n", "\n").replace("\r", "\n")

        // Step 2: Convert Markdown to HTML (before any HTML processing)
        processed = convertMarkdownToHtml(processed)

        // Step 3: Convert block tags to paragraph markers
        processed = convertBlockTagsToParagraphs(processed)

        // Step 4: Split into paragraphs
        val paragraphs = processed
            .split(Regex("\n{2,}|<p>|</p>", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Step 5: Process each paragraph
        val cleanedParagraphs = paragraphs.map { paragraph ->
            cleanParagraph(paragraph)
        }.filter { it.isNotBlank() }

        // Step 6: Wrap in <p> tags
        return cleanedParagraphs.joinToString("\n") { "<p>$it</p>" }
    }

    /**
     * Convert Markdown formatting to HTML
     */
    private fun convertMarkdownToHtml(text: String): String {
        var result = text

        // Bold: **text** or __text__
        result = result.replace(Regex("\\*\\*(.+?)\\*\\*")) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }
        result = result.replace(Regex("__(.+?)__")) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }

        // Italic: *text* or _text_ (but not inside words for underscores)
        result = result.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { match ->
            "<em>${match.groupValues[1]}</em>"
        }
        result = result.replace(Regex("(?<!\\w)_(.+?)_(?!\\w)")) { match ->
            "<em>${match.groupValues[1]}</em>"
        }

        // Bold + Italic: ***text*** or ___text___
        result = result.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*")) { match ->
            "<strong><em>${match.groupValues[1]}</em></strong>"
        }
        result = result.replace(Regex("___(.+?)___")) { match ->
            "<strong><em>${match.groupValues[1]}</em></strong>"
        }

        // Strikethrough: ~~text~~
        result = result.replace(Regex("~~(.+?)~~")) { match ->
            "<del>${match.groupValues[1]}</del>"
        }

        // Inline code: `code`
        result = result.replace(Regex("`([^`]+)`")) { match ->
            "<code>${match.groupValues[1]}</code>"
        }

        // Horizontal rule: --- or *** or ___
        result = result.replace(Regex("^([-*_]){3,}\\s*$", RegexOption.MULTILINE), "<hr/>")

        // Headers: # Header (convert to bold for chapter content)
        result = result.replace(Regex("^#{1,6}\\s+(.+)$", RegexOption.MULTILINE)) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }

        // Blockquotes: > text (convert to italic with indent)
        result = result.replace(Regex("^>\\s*(.+)$", RegexOption.MULTILINE)) { match ->
            "<em>「${match.groupValues[1]}」</em>"
        }

        return result
    }

    /**
     * Convert block-level HTML tags to paragraph separators
     */
    private fun convertBlockTagsToParagraphs(text: String): String {
        var result = text

        // Replace <br> tags with newlines
        result = result.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

        // Replace block tags with double newlines (paragraph breaks)
        BLOCK_TAGS.forEach { tag ->
            result = result.replace(Regex("</?$tag[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        }

        return result
    }

    /**
     * Clean a single paragraph while preserving allowed formatting tags
     */
    private fun cleanParagraph(paragraph: String): String {
        val result = StringBuilder()
        var i = 0
        val text = paragraph

        while (i < text.length) {
            if (text[i] == '<') {
                // Found a tag
                val tagEnd = text.indexOf('>', i)
                if (tagEnd == -1) {
                    // Malformed tag, escape and continue
                    result.append("&lt;")
                    i++
                    continue
                }

                val tagContent = text.substring(i + 1, tagEnd)
                val tagInfo = parseTag(tagContent)

                if (tagInfo != null && ALLOWED_TAGS.contains(tagInfo.name.lowercase())) {
                    // Keep this tag
                    result.append(text.substring(i, tagEnd + 1))
                } else if (tagInfo != null && tagInfo.name.equals("hr", ignoreCase = true)) {
                    // Convert <hr> to a visual separator
                    result.append("—————")
                }
                // Otherwise, skip the tag entirely

                i = tagEnd + 1
            } else if (text[i] == '&') {
                // Check for HTML entities
                val entityEnd = text.indexOf(';', i)
                if (entityEnd != -1 && entityEnd - i < 10) {
                    val entity = text.substring(i, entityEnd + 1)
                    if (isValidHtmlEntity(entity)) {
                        result.append(entity)
                        i = entityEnd + 1
                        continue
                    }
                }
                // Not a valid entity, escape it
                result.append("&amp;")
                i++
            } else if (text[i] == '<') {
                result.append("&lt;")
                i++
            } else if (text[i] == '>') {
                result.append("&gt;")
                i++
            } else {
                result.append(text[i])
                i++
            }
        }

        // Clean up whitespace
        return result.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Parse a tag to extract its name and whether it's a closing tag
     */
    private fun parseTag(tagContent: String): TagInfo? {
        val trimmed = tagContent.trim()
        if (trimmed.isEmpty()) return null

        val isClosing = trimmed.startsWith("/")
        val content = if (isClosing) trimmed.substring(1) else trimmed

        // Extract tag name (first word)
        val nameEnd = content.indexOfFirst { it.isWhitespace() || it == '/' || it == '>' }
        val name = if (nameEnd == -1) content else content.substring(0, nameEnd)

        if (name.isEmpty()) return null

        return TagInfo(name, isClosing)
    }

    private data class TagInfo(val name: String, val isClosing: Boolean)

    /**
     * Check if a string is a valid HTML entity
     */
    private fun isValidHtmlEntity(entity: String): Boolean {
        if (!entity.startsWith("&") || !entity.endsWith(";")) return false

        val content = entity.substring(1, entity.length - 1)

        // Numeric entity: &#123; or &#x1F600;
        if (content.startsWith("#")) {
            val numPart = content.substring(1)
            return if (numPart.startsWith("x", ignoreCase = true)) {
                numPart.substring(1).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            } else {
                numPart.all { it.isDigit() }
            }
        }

        // Named entities
        val namedEntities = setOf(
            "amp", "lt", "gt", "quot", "apos", "nbsp", "mdash", "ndash",
            "ldquo", "rdquo", "lsquo", "rsquo", "hellip", "copy", "reg",
            "trade", "deg", "plusmn", "times", "divide", "frac12", "frac14",
            "frac34", "cent", "pound", "euro", "yen", "sect", "para",
            "dagger", "Dagger", "bull", "prime", "Prime", "laquo", "raquo"
        )

        return content.lowercase() in namedEntities
    }
}