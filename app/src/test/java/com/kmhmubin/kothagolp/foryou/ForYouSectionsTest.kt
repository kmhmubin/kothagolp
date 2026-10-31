package com.kmhmubin.kothagolp.foryou

import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.LibrarySourceNovel
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.recommendation.model.RecommendationGroup
import com.kmhmubin.kothagolp.recommendation.model.RecommendationType
import com.kmhmubin.kothagolp.recommendation.model.ScoreBreakdown
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.AiRecommendedNovel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.AiState
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouSection
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.RecommendationUiState
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.buildForYouSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForYouSectionsTest {
    private fun novel(n: String) = Novel(name = n, url = "u/$n", apiName = "api$n")
    private fun rec(n: String, type: RecommendationType) =
        Recommendation(novel(n), 0.5f, type, if (type == RecommendationType.FROM_AUTHORS_YOU_LIKE) "Author $n" else "r", ScoreBreakdown())
    private fun group(type: RecommendationType, title: String, vararg names: String, source: Novel? = null) =
        RecommendationGroup(type, title, null, names.map { rec(it, type) }, source)
    private fun lib(n: String) = LibrarySourceNovel(novel(n), 0L, 1, 10, 0.1f)
    private fun ai(n: String, url: String? = "u/$n") = AiRecommendedNovel(n, reason = "why$n", matchedNovelUrl = url, matchedApiName = "api$n")
    private val genres = listOf(TagCategory.FANTASY)

    private val fullGroups = listOf(
        group(RecommendationType.FOR_YOU, "t", "a", "b"),
        group(RecommendationType.BECAUSE_YOU_READ, "t", "c", source = novel("S")),
        group(RecommendationType.FROM_AUTHORS_YOU_LIKE, "t", "d"),
        group(RecommendationType.TRENDING_IN_YOUR_GENRES, "Fantasy", "e"),
        group(RecommendationType.EXPLORE_GENRE, "Drama", "f"),
        group(RecommendationType.NEW_FOR_YOU, "t", "g"),
    )
    private val fullState = RecommendationUiState(
        recommendationGroups = fullGroups, aiRecommendations = listOf(ai("x")), hasGeminiKey = true
    )

    private fun rows(s: RecommendationUiState) = buildForYouSections(s, genres)

    @Test fun orderFollowsSpec() {
        assertEquals(
            listOf("carousel", "more_like", "authors", "ai", "top_genre", "different", "just_added", "browse"),
            rows(fullState).map { it.key }
        )
    }

    @Test fun emptyGroupsHidden() {
        val s = fullState.copy(recommendationGroups = fullGroups.filter { it.type != RecommendationType.FROM_AUTHORS_YOU_LIKE })
        assertFalse(rows(s).any { it.key == "authors" })
    }

    @Test fun firstTimeEmpty() {
        assertEquals(listOf(ForYouSection.FirstTimeEmpty), rows(RecommendationUiState()))
    }

    @Test fun aiMergesPicksThenDiscoveryDeduped() {
        val s = RecommendationUiState(
            hasGeminiKey = true,
            aiRecommendations = listOf(ai("A"), ai("B"), ai("N", url = null)),
            aiTrendingRecommendations = listOf(ai("B"), ai("C"))
        )
        val row = rows(s).filterIsInstance<ForYouSection.AiRow>().single()
        assertEquals(listOf("u/A", "u/B", "u/C"), row.items.map { it.url })
        assertEquals(AiState.Ready, row.state)
        // Long-press needs a Recommendation on every AI cover, carrying the AI's reason.
        assertEquals(listOf("whyA", "whyB", "whyC"), row.items.map { it.recommendation!!.reason })
        assertEquals("apiA", row.items.first().recommendation!!.novel.apiName)
    }

    @Test fun aiOffWhenNoKey() {
        val row = rows(fullState.copy(hasGeminiKey = false, aiRecommendations = emptyList()))
            .filterIsInstance<ForYouSection.AiRow>().single()
        assertEquals(AiState.Off, row.state)
        assertTrue(row.items.isEmpty())
    }

    @Test fun captionsPerRow() {
        val sections = rows(fullState)
        val aiRow = sections.filterIsInstance<ForYouSection.AiRow>().single()
        assertEquals("whyx", aiRow.items.single().caption)
        val authors = sections.filterIsInstance<ForYouSection.Row>().first { it.key == "authors" }
        assertEquals("Author d", authors.items.single().caption)
        val other = sections.filterIsInstance<ForYouSection.Row>().first { it.key == "just_added" }
        assertEquals("apig", other.items.single().caption)
    }

    @Test fun rowTitles() {
        val r = rows(fullState).filterIsInstance<ForYouSection.Row>().associateBy { it.key }
        assertEquals("Top fantasy for you", r.getValue("top_genre").title)
        assertEquals(TagCategory.FANTASY, r.getValue("top_genre").seeAllTag)
        assertEquals("Something different", r.getValue("different").title)
        assertEquals("Drama you might enjoy", r.getValue("different").subtitle)
        assertEquals("Just added", r.getValue("just_added").title)
        assertEquals("By authors you like", r.getValue("authors").title)
    }

    @Test fun moreLikeOptions() {
        val s = fullState.copy(
            librarySourceNovels = listOf(lib("L"), lib("S")),
            selectedSourceNovel = lib("L"),
            sourceNovelRecommendations = listOf(rec("z", RecommendationType.BECAUSE_YOU_READ))
        )
        val m = rows(s).filterIsInstance<ForYouSection.MoreLike>().single()
        assertEquals(listOf("u/L", "u/S"), m.options.map { it.url })
        assertEquals("u/L", m.source.url)
        assertEquals(listOf("u/z"), m.items.map { it.novel.url })
    }
}
