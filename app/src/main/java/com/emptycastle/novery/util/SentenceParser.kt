package com.emptycastle.novery.util

/**
 * Represents a sentence with its position and TTS hints
 */
data class ParsedSentence(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val sentenceIndex: Int,
    val pauseAfterMs: Int = DEFAULT_PAUSE_MS
) {
    companion object {
        const val DEFAULT_PAUSE_MS = 80    // Reduced from 200
        const val SHORT_PAUSE_MS = 40      // Reduced from 100
        const val LONG_PAUSE_MS = 120      // Reduced from 300
        const val ELLIPSIS_PAUSE_MS = 150  // Reduced from 350
        const val COLON_PAUSE_MS = 60      // New - for colon splits
    }
}

/**
 * Represents a paragraph with its sentences parsed
 */
data class ParsedParagraph(
    val fullText: String,
    val sentences: List<ParsedSentence>
) {
    val sentenceCount: Int get() = sentences.size

    fun getSentence(index: Int): ParsedSentence? = sentences.getOrNull(index)
}

/**
 * TTS-optimized sentence parser with intelligent handling of:
 * - Ellipsis (... and …)
 * - Multiple punctuation
 * - Abbreviations
 * - Decimal numbers
 * - Initials
 * - Colons (as dividers, except in time/ratio patterns)
 * - Quotation marks (stripped for Google TTS compatibility)
 */
object SentenceParser {

    // Common abbreviations that shouldn't end sentences
    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "rev", "hon",
        "capt", "col", "gen", "lt", "sgt",
        "vs", "etc", "inc", "ltd", "co", "corp",
        "st", "ave", "blvd", "rd", "ft", "mt", "apt",
        "vol", "pg", "pp", "ch", "pt", "no", "nos",
        "fig", "figs", "approx", "dept", "est", "govt", "misc",
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    )

    private val SENTENCE_ENDINGS = charArrayOf('.', '!', '?', ':')  // Added ':'
    private val CLOSING_QUOTES = charArrayOf('"', '\u201D', '\'', '\u2019', '\u300D', '\u300F', ')', ']', '\u00BB')
    private val OPENING_QUOTES = charArrayOf('"', '\u201C', '\'', '\u2018', '\u300C', '\u300E', '(', '[', '\u00AB')

    // Pattern to detect sentences that are just punctuation/ellipsis/quotes (no actual letters)
    private val PUNCTUATION_ONLY_REGEX = Regex(
        "^[\"'\u201C\u201D\u2018\u2019\u201E\u201A" +
                "\u300C\u300D\u300E\u300F()\\[\\]\u00AB\u00BB" +
                ".,!?\u2026:;\\-\u2014\u2013_\\s\\d]+$"  // Added \\d to catch numbers-only
    )

    /**
     * Parse text into sentences
     */
    fun parse(text: String): ParsedParagraph {
        if (text.isBlank()) {
            return ParsedParagraph(text, emptyList())
        }

        val cleanedText = preprocessText(text.trim())
        val sentences = mutableListOf<ParsedSentence>()

        var sentenceStart = 0
        var i = 0
        var sentenceIndex = 0
        var lastI = -1

        while (i < cleanedText.length) {
            // Safety: ensure we always advance
            if (i == lastI) {
                i++
                continue
            }
            lastI = i

            val char = cleanedText[i]

            // Check for ellipsis first
            if (char == '.' || char == '\u2026') {
                val ellipsisEnd = findEllipsisEnd(cleanedText, i)

                if (ellipsisEnd > i) {
                    val shouldBreak = shouldBreakAtEllipsis(cleanedText, ellipsisEnd)

                    if (shouldBreak) {
                        var endIndex = ellipsisEnd
                        endIndex = skipClosingQuotes(cleanedText, endIndex)

                        val sentenceText = cleanedText.substring(sentenceStart, endIndex).trim()
                        if (isValidSentence(sentenceText)) {
                            val normalized = normalizeSentence(sentenceText)
                            if (normalized.isNotBlank() && hasLetter(normalized)) {
                                sentences.add(
                                    ParsedSentence(
                                        text = normalized,
                                        startIndex = sentenceStart,
                                        endIndex = endIndex,
                                        sentenceIndex = sentenceIndex,
                                        pauseAfterMs = ParsedSentence.ELLIPSIS_PAUSE_MS
                                    )
                                )
                                sentenceIndex++
                            }
                        }

                        sentenceStart = skipWhitespace(cleanedText, endIndex)
                        i = sentenceStart
                    } else {
                        i = ellipsisEnd
                    }
                    continue
                }
            }

            // Check for regular sentence endings (including colon)
            if (char in SENTENCE_ENDINGS) {
                if (isSentenceEnd(cleanedText, i)) {
                    var endIndex = i + 1
                    // Skip multiple ending punctuation (but not colons after other punctuation)
                    while (endIndex < cleanedText.length && cleanedText[endIndex] in SENTENCE_ENDINGS && cleanedText[endIndex] != ':') {
                        endIndex++
                    }
                    endIndex = skipClosingQuotes(cleanedText, endIndex)

                    val sentenceText = cleanedText.substring(sentenceStart, endIndex).trim()
                    if (isValidSentence(sentenceText)) {
                        val normalized = normalizeSentence(sentenceText)
                        if (normalized.isNotBlank() && hasLetter(normalized)) {
                            sentences.add(
                                ParsedSentence(
                                    text = normalized,
                                    startIndex = sentenceStart,
                                    endIndex = endIndex,
                                    sentenceIndex = sentenceIndex,
                                    pauseAfterMs = calculatePause(sentenceText)
                                )
                            )
                            sentenceIndex++
                        }
                    }

                    sentenceStart = skipWhitespace(cleanedText, endIndex)
                    i = sentenceStart
                    continue
                }
            }

            i++
        }

        // Add remaining text as final sentence
        if (sentenceStart < cleanedText.length) {
            val remainingText = cleanedText.substring(sentenceStart).trim()
            if (isValidSentence(remainingText)) {
                val normalized = normalizeSentence(remainingText)
                if (normalized.isNotBlank() && hasLetter(normalized)) {
                    sentences.add(
                        ParsedSentence(
                            text = normalized,
                            startIndex = sentenceStart,
                            endIndex = cleanedText.length,
                            sentenceIndex = sentenceIndex
                        )
                    )
                }
            }
        }

        // Fallback: if no sentences found, use whole text (only if it has letters)
        if (sentences.isEmpty() && cleanedText.isNotBlank() && hasLetter(cleanedText)) {
            val normalized = normalizeSentence(cleanedText)
            if (normalized.isNotBlank()) {
                sentences.add(
                    ParsedSentence(
                        text = normalized,
                        startIndex = 0,
                        endIndex = cleanedText.length,
                        sentenceIndex = 0
                    )
                )
            }
        }

        return ParsedParagraph(cleanedText, sentences)
    }

    /**
     * Check if text contains at least one letter (any script)
     */
    private fun hasLetter(text: String): Boolean {
        return text.any { it.isLetter() }
    }

    /**
     * Check if text is valid sentence (not blank, has actual content with letters)
     */
    private fun isValidSentence(text: String): Boolean {
        return text.isNotBlank() && !isPunctuationOnly(text) && hasLetter(text)
    }

    /**
     * Check if text is only punctuation, quotes, ellipsis, numbers, and whitespace (no actual words)
     */
    private fun isPunctuationOnly(text: String): Boolean {
        return PUNCTUATION_ONLY_REGEX.matches(text)
    }

    /**
     * Find the end of an ellipsis starting at index
     */
    private fun findEllipsisEnd(text: String, index: Int): Int {
        return when {
            // Unicode ellipsis
            text[index] == '\u2026' -> index + 1

            // ASCII ellipsis (...) - must be exactly 3 or more dots
            text[index] == '.' &&
                    index + 2 < text.length &&
                    text[index + 1] == '.' &&
                    text[index + 2] == '.' -> {
                var end = index + 3
                while (end < text.length && text[end] == '.') end++
                end
            }

            else -> index
        }
    }

    /**
     * Determine if we should break the sentence at an ellipsis
     */
    private fun shouldBreakAtEllipsis(text: String, afterEllipsis: Int): Boolean {
        // Skip closing quotes
        var i = afterEllipsis
        while (i < text.length && text[i] in CLOSING_QUOTES) i++

        // Skip whitespace
        val hadWhitespace = i < text.length && text[i].isWhitespace()
        while (i < text.length && text[i].isWhitespace()) i++

        // End of text = break
        if (i >= text.length) return true

        val nextChar = text[i]

        return when {
            // Capital letter after space = new sentence
            nextChar.isUpperCase() && hadWhitespace -> true

            // Opening quote after space = likely new sentence
            nextChar in OPENING_QUOTES && hadWhitespace -> true

            // Lowercase = continuation (e.g., "He was... uncertain")
            nextChar.isLowerCase() -> false

            // Digit after ellipsis without space = continuation
            nextChar.isDigit() && !hadWhitespace -> false

            // Default: break if there was whitespace
            else -> hadWhitespace
        }
    }

    /**
     * Check if punctuation at index is a real sentence end
     */
    private fun isSentenceEnd(text: String, index: Int): Boolean {
        val char = text[index]

        // Handle colon - split unless it's a time/ratio pattern (e.g., 10:30, 3:2)
        if (char == ':') {
            // Don't split on time/ratio patterns: digit followed by colon followed by digit
            if (index > 0 && index < text.length - 1 &&
                text[index - 1].isDigit() && text[index + 1].isDigit()) {
                return false
            }
            // All other colons are dividers
            return true
        }

        if (char == '.') {
            // Ellipsis check
            if (index + 2 < text.length && text[index + 1] == '.' && text[index + 2] == '.') {
                return false
            }

            // Abbreviation check
            val wordStart = findWordStart(text, index)
            if (wordStart < index) {
                val word = text.substring(wordStart, index).lowercase()
                if (word in ABBREVIATIONS) {
                    val nextNonSpace = skipWhitespace(text, index + 1)
                    if (nextNonSpace < text.length && text[nextNonSpace].isLowerCase()) {
                        return false
                    }
                }
            }

            // Decimal number check
            if (index > 0 && index < text.length - 1 &&
                text[index - 1].isDigit() && text[index + 1].isDigit()
            ) {
                return false
            }

            // Initial check
            if (isInitial(text, index)) return false
        }

        // Check what follows
        var nextIndex = index + 1
        while (nextIndex < text.length && text[nextIndex] == char) nextIndex++
        nextIndex = skipClosingQuotes(text, nextIndex)
        nextIndex = skipWhitespace(text, nextIndex)

        // End of text = sentence end
        if (nextIndex >= text.length) return true

        val nextChar = text[nextIndex]

        // Uppercase or opening quote = new sentence
        if (nextChar.isUpperCase() || nextChar in OPENING_QUOTES) return true

        // For ! and ? always split
        if (char == '!' || char == '?') return true

        // Period with space = likely end
        if (char == '.' && index + 1 < text.length && text[index + 1].isWhitespace()) {
            return true
        }

        return false
    }

    /**
     * Check if period at index is part of initials
     */
    private fun isInitial(text: String, periodIndex: Int): Boolean {
        if (periodIndex < 1) return false

        val charBefore = text[periodIndex - 1]
        if (!charBefore.isUpperCase()) return false

        if (periodIndex >= 2 && text[periodIndex - 2].isLetter()) return false

        val nextNonSpace = skipWhitespace(text, periodIndex + 1)
        if (nextNonSpace >= text.length) return false

        val nextChar = text[nextNonSpace]

        if (nextChar.isUpperCase()) {
            val afterNext = nextNonSpace + 1
            if (afterNext < text.length && text[afterNext] == '.') return true
            if (afterNext < text.length && text[afterNext].isLowerCase()) return true
        }

        return false
    }

    // ==================== Text Processing ====================

    private fun preprocessText(text: String): String {
        var result = text

        // Normalize line breaks within paragraph to space
        result = result.replace(Regex("[ \\t]*\\n[ \\t]*"), " ")

        // Normalize multiple spaces
        result = result.replace(Regex(" {2,}"), " ")

        // Normalize dashes
        result = result.replace("---", "\u2014")
        result = result.replace("--", "\u2014")

        return result.trim()
    }

    private fun normalizeSentence(text: String): String {
        var result = text.trim()

        // Remove all quotation marks
        result = result.replace(Regex("[\"'\u201C\u201D\u2018\u2019\u300C\u300D\u300E\u300F\u00AB\u00BB]"), "")

        // Normalize ellipsis
        result = result.replace("\u2026", "...")
        result = result.replace(Regex("\\.{4,}"), "...")

        // Remove leading ellipsis
        result = result.replace(Regex("^\\.{3}\\s*"), "")

        // Remove trailing ellipsis
        result = result.replace(Regex("\\s*\\.{3}$"), "")

        // Replace mid-sentence ellipsis with comma
        result = result.replace(Regex("\\s*\\.{3}\\s*(?=[A-Za-z])"), ", ")

        // Remove trailing colon (we split on it, no need to speak it)
        result = result.replace(Regex(":\\s*$"), "")

        // Remove leading colon (rare but possible)
        result = result.replace(Regex("^\\s*:"), "")

        // Clean up artifacts
        result = result.replace(Regex("^[,;:\\s]+"), "")
        result = result.replace(Regex("[,;:\\s]+$"), "")
        result = result.replace(Regex(",\\s*,"), ",")
        result = result.replace(Regex(" {2,}"), " ")

        return result.trim()
    }

    private fun calculatePause(sentence: String): Int {
        val trimmed = sentence.trim()
        if (trimmed.isEmpty()) return ParsedSentence.DEFAULT_PAUSE_MS

        // Check for ellipsis at end
        if (trimmed.endsWith("\u2026") || trimmed.endsWith("...")) {
            return ParsedSentence.ELLIPSIS_PAUSE_MS
        }

        val lastChar = trimmed.last()

        // Unwrap closing quotes to find actual ending punctuation
        var effectiveLast = lastChar
        var lookIndex = trimmed.length - 2
        while (effectiveLast in CLOSING_QUOTES && lookIndex >= 0) {
            effectiveLast = trimmed[lookIndex]
            lookIndex--
        }

        return when (effectiveLast) {
            ':' -> ParsedSentence.COLON_PAUSE_MS         // 60ms
            '?' -> ParsedSentence.DEFAULT_PAUSE_MS + 20  // 100ms
            '!' -> ParsedSentence.DEFAULT_PAUSE_MS + 10  // 90ms
            '\u2014', '\u2013' -> ParsedSentence.SHORT_PAUSE_MS
            '\u2026' -> ParsedSentence.ELLIPSIS_PAUSE_MS
            else -> ParsedSentence.DEFAULT_PAUSE_MS      // 80ms
        }
    }

    // ==================== Utility Functions ====================

    private fun findWordStart(text: String, endIndex: Int): Int {
        var start = endIndex - 1
        while (start >= 0 && text[start].isLetter()) start--
        return start + 1
    }

    private fun skipWhitespace(text: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < text.length && text[i].isWhitespace()) i++
        return i
    }

    private fun skipClosingQuotes(text: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < text.length && text[i] in CLOSING_QUOTES) i++
        return i
    }

    // ==================== Public API ====================

    /**
     * Split text into sentences (simple list)
     */
    fun splitIntoSentences(text: String): List<String> {
        return parse(text).sentences.map { it.text }
    }

    /**
     * Count sentences in text
     */
    fun countSentences(text: String): Int {
        return parse(text).sentenceCount
    }

    /**
     * Get sentences with pause hints for TTS
     */
    fun getSentencesWithPauses(text: String): List<Pair<String, Int>> {
        return parse(text).sentences.map { it.text to it.pauseAfterMs }
    }
}