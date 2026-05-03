package com.emptycastle.novery.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.util.SentenceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Handles loading chapter content for TTS playback in the background.
 * This operates independently of the UI and can load chapters
 * even when the screen is off.
 */
class TTSBackgroundLoader(private val context: Context) {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()

    private val loadMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var novelUrl: String = ""
    private var novelName: String = ""
    private var providerName: String = ""
    private var coverUrl: String? = null
    private var coverBitmap: Bitmap? = null
    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = -1

    private var cachedNextContent: TTSContent? = null
    private var cachedNextIndex: Int = -1

    // Paragraph break pause - slightly longer than sentence pause but not excessive
    private val PARAGRAPH_BREAK_PAUSE_MS = 100

    suspend fun configure(
        novelUrl: String,
        providerName: String,
        chapters: List<Chapter>,
        currentIndex: Int
    ) = withContext(Dispatchers.IO) {
        this@TTSBackgroundLoader.novelUrl = novelUrl
        this@TTSBackgroundLoader.providerName = providerName
        this@TTSBackgroundLoader.chapters = chapters
        this@TTSBackgroundLoader.currentChapterIndex = currentIndex

        val details = offlineRepository.getNovelDetails(novelUrl)
        novelName = details?.name ?: "Novel"
        coverUrl = details?.posterUrl

        coverBitmap = loadCoverBitmap(coverUrl)

        if (hasNextChapter()) {
            preloadNextChapter()
        }
    }

    /**
     * Update the current chapter index.
     */
    fun setCurrentChapterIndex(index: Int) {
        currentChapterIndex = index
    }

    fun getCurrentChapterIndex(): Int = currentChapterIndex
    fun getTotalChapters(): Int = chapters.size
    fun hasNextChapter(): Boolean = currentChapterIndex < chapters.size - 1
    fun hasPreviousChapter(): Boolean = currentChapterIndex > 0
    fun getCurrentChapterName(): String = chapters.getOrNull(currentChapterIndex)?.name ?: ""
    fun getCurrentChapterUrl(): String = chapters.getOrNull(currentChapterIndex)?.url ?: ""
    fun getNovelName(): String = novelName
    fun getNovelUrl(): String = novelUrl
    fun getCoverBitmap(): Bitmap? = coverBitmap

    /**
     * Load the next chapter content.
     * Uses pre-cached content if available.
     */
    suspend fun loadNextChapter(): TTSContent? = loadMutex.withLock {
        if (!hasNextChapter()) return@withLock null

        val nextIndex = currentChapterIndex + 1

        if (cachedNextContent != null && cachedNextIndex == nextIndex) {
            currentChapterIndex = nextIndex
            val content = cachedNextContent
            cachedNextContent = null
            cachedNextIndex = -1

            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }

            return@withLock content
        }

        val chapter = chapters.getOrNull(nextIndex) ?: return@withLock null
        val content = loadChapterInternal(nextIndex, chapter)

        if (content != null) {
            currentChapterIndex = nextIndex

            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    /**
     * Load the previous chapter content.
     */
    suspend fun loadPreviousChapter(): TTSContent? = loadMutex.withLock {
        if (!hasPreviousChapter()) return@withLock null

        val prevIndex = currentChapterIndex - 1
        val chapter = chapters.getOrNull(prevIndex) ?: return@withLock null

        val content = loadChapterInternal(prevIndex, chapter)

        if (content != null) {
            currentChapterIndex = prevIndex
            cachedNextContent = null
            cachedNextIndex = -1

            // Pre-load next chapter
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    /**
     * Load a specific chapter by index.
     */
    suspend fun loadChapter(index: Int): TTSContent? = loadMutex.withLock {
        if (index < 0 || index >= chapters.size) return@withLock null

        val chapter = chapters.getOrNull(index) ?: return@withLock null
        val content = loadChapterInternal(index, chapter)

        if (content != null) {
            currentChapterIndex = index

            // Invalidate cache
            cachedNextContent = null
            cachedNextIndex = -1

            // Pre-load next
            if (hasNextChapter()) {
                preloadNextChapterAsync()
            }
        }

        content
    }

    private suspend fun loadChapterInternal(index: Int, chapter: Chapter): TTSContent? {
        return withContext(Dispatchers.IO) {
            try {
                val provider = novelRepository.getProvider(providerName) ?: return@withContext null

                // Try cache first, then network
                val rawContent = offlineRepository.getChapterContent(chapter.url)
                    ?: novelRepository.loadChapterContent(provider, chapter.url).getOrNull()
                    ?: return@withContext null

                // Parse content into segments
                val segments = parseContent(rawContent)

                if (segments.isEmpty()) return@withContext null

                TTSContent(
                    novelName = novelName,
                    novelUrl = novelUrl,
                    chapterName = chapter.name,
                    chapterUrl = chapter.url,
                    segments = segments,
                    coverUrl = coverUrl,
                    chapterIndex = index,
                    totalChapters = chapters.size,
                    hasNextChapter = index < chapters.size - 1,
                    hasPreviousChapter = index > 0
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseContent(rawContent: String): List<TTSSegment> {
        // Clean HTML and parse into sentences
        val cleanText = cleanHtml(rawContent)

        if (cleanText.isBlank()) return emptyList()

        // Split by paragraph markers and parse each paragraph separately
        val paragraphs = cleanText.split("\u00B6").filter { it.isNotBlank() }

        if (paragraphs.isEmpty()) {
            // Fallback: parse as single block
            return parseSingleBlock(cleanText)
        }

        val segments = mutableListOf<TTSSegment>()
        var isFirstParagraph = true

        for (paragraph in paragraphs) {
            val paragraphSegments = parseSingleBlock(paragraph)

            if (paragraphSegments.isEmpty()) continue

            // Add paragraph break pause before this paragraph (except the first one)
            if (!isFirstParagraph && segments.isNotEmpty()) {
                // Update the last segment's pause to include paragraph break
                val lastSegment = segments.last()
                segments[segments.lastIndex] = TTSSegment(
                    lastSegment.text,
                    PARAGRAPH_BREAK_PAUSE_MS
                )
            }

            segments.addAll(paragraphSegments)
            isFirstParagraph = false
        }

        return segments
    }

    private fun parseSingleBlock(text: String): List<TTSSegment> {
        if (text.isBlank()) return emptyList()

        val parsedParagraph = SentenceParser.parse(text)
        return parsedParagraph.sentences.map { sentence ->
            TTSSegment(sentence.text, sentence.pauseAfterMs)
        }
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\u00B6")  // Use pilcrow as paragraph marker
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "\u00B6")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#\\d+;")) { match ->
                val code = match.value.drop(2).dropLast(1).toIntOrNull()
                code?.toChar()?.toString() ?: ""
            }
            // Convert newlines to paragraph markers (but not multiple in a row)
            .replace(Regex("\\n+"), "\u00B6")
            // Clean up multiple paragraph markers
            .replace(Regex("\u00B6+"), "\u00B6")
            // Normalize spaces (but preserve paragraph markers)
            .replace(Regex("[ \\t]+"), " ")
            // Remove spaces around paragraph markers
            .replace(Regex(" ?\u00B6 ?"), "\u00B6")
            .trim('\u00B6', ' ', '\n', '\t')
    }

    private suspend fun preloadNextChapter() {
        if (!hasNextChapter()) return

        val nextIndex = currentChapterIndex + 1
        val chapter = chapters.getOrNull(nextIndex) ?: return

        val content = loadChapterInternal(nextIndex, chapter)
        if (content != null) {
            cachedNextContent = content
            cachedNextIndex = nextIndex
        }
    }

    private fun preloadNextChapterAsync() {
        // Launch preload without blocking using the background scope
        backgroundScope.launch {
            preloadNextChapter()
        }
    }

    private suspend fun loadCoverBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                BitmapFactory.decodeStream(connection.getInputStream())
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clear all cached data.
     */
    fun clear() {
        novelUrl = ""
        novelName = ""
        providerName = ""
        coverUrl = null
        coverBitmap = null
        chapters = emptyList()
        currentChapterIndex = -1
        cachedNextContent = null
        cachedNextIndex = -1
    }
}