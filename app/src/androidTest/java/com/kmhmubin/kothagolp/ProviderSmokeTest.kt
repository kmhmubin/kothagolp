package com.kmhmubin.kothagolp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kmhmubin.kothagolp.provider.AllNovelProvider
import com.kmhmubin.kothagolp.provider.CyrisiaProvider
import com.kmhmubin.kothagolp.provider.FenrirRealmProvider
import com.kmhmubin.kothagolp.provider.FreeWebNovelProvider
import com.kmhmubin.kothagolp.provider.FuckNovelPiaProvider
import com.kmhmubin.kothagolp.provider.LibReadProvider
import com.kmhmubin.kothagolp.provider.LightNovelTranslationsProvider
import com.kmhmubin.kothagolp.provider.LightNovelWorldProvider
import com.kmhmubin.kothagolp.provider.LnoriProvider
import com.kmhmubin.kothagolp.provider.MainProvider
import com.kmhmubin.kothagolp.provider.NovelArchiveProvider
import com.kmhmubin.kothagolp.provider.NovelArrowProvider
import com.kmhmubin.kothagolp.provider.NovelBinProvider
import com.kmhmubin.kothagolp.provider.NovelBuddyProvider
import com.kmhmubin.kothagolp.provider.NovelDexProvider
import com.kmhmubin.kothagolp.provider.NovelFireProvider
import com.kmhmubin.kothagolp.provider.PawReadProvider
import com.kmhmubin.kothagolp.provider.RoyalRoadProvider
import com.kmhmubin.kothagolp.provider.WebnovelProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live end-to-end smoke test of every bundled source, exercising the same
 * provider calls the UI makes: browse -> details -> chapter list ->
 * chapter content -> next chapter content.
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest \
 *        -Pandroid.testInstrumentationRunnerArguments.class=com.kmhmubin.kothagolp.ProviderSmokeTest
 */
@RunWith(AndroidJUnit4::class)
class ProviderSmokeTest {

    private val providers: List<MainProvider> = listOf(
        AllNovelProvider(),
        CyrisiaProvider(),
        FenrirRealmProvider(),
        FreeWebNovelProvider(),
        FuckNovelPiaProvider(),
        LibReadProvider(),
        LightNovelTranslationsProvider(),
        LightNovelWorldProvider(),
        LnoriProvider(),
        NovelArchiveProvider(),
        NovelArrowProvider(),
        NovelBinProvider(),
        NovelBuddyProvider(),
        NovelDexProvider(),
        NovelFireProvider(),
        PawReadProvider(),
        RoyalRoadProvider(),
        WebnovelProvider()
    )

    private data class SourceResult(
        val name: String,
        val browse: String,
        val details: String,
        val chapters: String,
        val content: String,
        val nextChapter: String,
        val failure: String? = null
    )

    @Test
    fun allSourcesEndToEnd() {
        val results = providers.map { provider -> runBlocking { smokeTest(provider) } }

        val report = buildString {
            appendLine()
            appendLine("========== SOURCE SMOKE TEST REPORT ==========")
            for (r in results) {
                val status = if (r.failure == null) "PASS" else "FAIL"
                appendLine("[$status] ${r.name}")
                appendLine("    browse=${r.browse} details=${r.details} chapters=${r.chapters}")
                appendLine("    content=${r.content} nextChapter=${r.nextChapter}")
                r.failure?.let { appendLine("    !! $it") }
            }
            appendLine("==============================================")
        }
        android.util.Log.i("ProviderSmokeTest", report)
        println(report)

        val failed = results.filter { it.failure != null }
        if (failed.isNotEmpty()) {
            throw AssertionError(
                "Sources failing: ${failed.joinToString { it.name }}\n$report"
            )
        }
    }

    private suspend fun smokeTest(provider: MainProvider): SourceResult {
        var browse = "-"
        var lastFailure: String? = null
        try {
            return withTimeout(180_000L) {
                // 1. Browse first page with default sort
                val page = provider.loadMainPage(
                    page = 1,
                    orderBy = provider.orderBys.firstOrNull()?.value,
                    tag = provider.tags.firstOrNull()?.value,
                    extraFilters = emptyMap()
                )
                if (page.novels.isEmpty()) error("browse returned 0 novels")
                browse = "${page.novels.size} novels"

                // 2-5. Try up to three novels: one broken item (paywalled
                // first chapter, image-only epub, ...) must not fail a source
                for (novel in page.novels.take(3)) {
                    try {
                        return@withTimeout testNovelFlow(provider, browse, novel.url)
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        lastFailure = "${t.javaClass.simpleName}: ${t.message?.take(140)}"
                    }
                }
                SourceResult(provider.name, browse, "-", "-", "-", "-", failure = lastFailure)
            }
        } catch (t: Throwable) {
            return SourceResult(
                provider.name, browse, "-", "-", "-", "-",
                failure = lastFailure ?: "${t.javaClass.simpleName}: ${t.message?.take(140)}"
            )
        }
    }

    private suspend fun testNovelFlow(
        provider: MainProvider,
        browse: String,
        novelUrl: String
    ): SourceResult {
        val novelDetails = provider.load(novelUrl) ?: error("details null for $novelUrl")
        val details = novelDetails.name.take(30)

        val chapterList = novelDetails.chapters
        if (chapterList.isEmpty()) error("0 chapters for ${novelDetails.name}")
        val chapters = "${chapterList.size}"

        val first = chapterList.first()
        val body = provider.loadChapterContent(first.url)
        if (body.isNullOrBlank() || body.length < 200) {
            error("chapter content too small (${body?.length ?: 0}) for ${first.url}")
        }
        val content = "${body.length} chars"

        // Next chapter = the reader auto-advance path
        val next = if (chapterList.size > 1) {
            val second = chapterList[1]
            val nextBody = provider.loadChapterContent(second.url)
            if (nextBody.isNullOrBlank() || nextBody.length < 200) {
                error("next chapter content too small (${nextBody?.length ?: 0}) for ${second.url}")
            }
            "${nextBody.length} chars"
        } else "single-chapter novel"

        return SourceResult(provider.name, browse, details, chapters, content, next)
    }
}
