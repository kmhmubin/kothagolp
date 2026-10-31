package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation

import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.recommendation.model.RecommendationGroup
import com.kmhmubin.kothagolp.recommendation.model.RecommendationType
import com.kmhmubin.kothagolp.recommendation.model.ScoreBreakdown

enum class AiState { Off, Loading, Ready, Error }

data class ForYouItem(
    val url: String,
    val title: String,
    val posterUrl: String?,
    val apiName: String,
    val caption: String,
    val recommendation: Recommendation?
)

sealed interface ForYouSection {
    val key: String

    data class Carousel(val items: List<Recommendation>) : ForYouSection { override val key = "carousel" }
    data class MoreLike(val source: Novel, val options: List<Novel>, val items: List<Recommendation>) : ForYouSection { override val key = "more_like" }
    data class Row(
        override val key: String,
        val title: String,
        val subtitle: String?,
        val items: List<ForYouItem>,
        val seeAllTag: TagCategory?
    ) : ForYouSection
    data class AiRow(val items: List<ForYouItem>, val state: AiState) : ForYouSection { override val key = "ai" }
    data class BrowseGenres(val tags: List<TagCategory>) : ForYouSection { override val key = "browse" }
    data object FirstTimeEmpty : ForYouSection { override val key = "first_time_empty" }
}

private const val CAROUSEL_SIZE = 5
private const val AI_SCORE = 0.75f

private fun Recommendation.toItem() =
    ForYouItem(novel.url, novel.name, novel.posterUrl, novel.apiName, novel.apiName, this)

// ponytail: one row per type (first non-empty group); the engine can emit several genre rows, extras are dropped.
fun buildForYouSections(state: RecommendationUiState, allGenres: List<TagCategory>): List<ForYouSection> {
    val groups = state.recommendationGroups.filter { it.recommendations.isNotEmpty() }
    fun first(type: RecommendationType) = groups.firstOrNull { it.type == type }
    fun row(key: String, title: String, subtitle: String?, g: RecommendationGroup, tag: TagCategory? = null) =
        ForYouSection.Row(key, title, subtitle, g.recommendations.map { it.toItem() }, tag)

    val aiItems = (state.aiRecommendations + state.aiTrendingRecommendations)
        .filter { it.matchedNovelUrl != null }
        .distinctBy { it.matchedNovelUrl }
        .map {
            val novel = Novel(it.title, it.matchedNovelUrl!!, it.matchedPosterUrl, apiName = it.matchedApiName.orEmpty())
            // Lets AI covers open the long-press sheet like every other cover.
            val rec = Recommendation(novel, AI_SCORE, RecommendationType.FOR_YOU, it.reason, ScoreBreakdown())
            ForYouItem(novel.url, it.title, it.matchedPosterUrl, novel.apiName, it.reason, rec)
        }
    val aiState = when {
        !state.hasGeminiKey -> AiState.Off
        state.isLoadingAiRecs -> AiState.Loading
        state.aiRecsError != null && aiItems.isEmpty() -> AiState.Error
        else -> AiState.Ready
    }

    if (groups.isEmpty() && aiItems.isEmpty() && !state.hasLibrarySources && !state.isLoadingAiRecs) {
        return listOf(ForYouSection.FirstTimeEmpty)
    }

    return buildList {
        first(RecommendationType.FOR_YOU)?.let { add(ForYouSection.Carousel(it.recommendations.take(CAROUSEL_SIZE))) }

        val becauseGroups = groups.filter { it.type == RecommendationType.BECAUSE_YOU_READ }
        val source = state.selectedSourceNovel?.novel ?: becauseGroups.firstNotNullOfOrNull { it.sourceNovel }
        if (source != null) {
            val options = (listOf(source) + becauseGroups.mapNotNull { it.sourceNovel } + state.librarySourceNovels.map { it.novel })
                .distinctBy { it.url }
            val items = if (state.selectedSourceNovel != null) state.sourceNovelRecommendations
            else becauseGroups.first().recommendations
            add(ForYouSection.MoreLike(source, options, items))
        }

        first(RecommendationType.FROM_AUTHORS_YOU_LIKE)?.let {
            add(row("authors", ForYouCopy.BY_AUTHORS, null, it).let { r ->
                r.copy(items = r.items.map { i -> i.copy(caption = i.recommendation?.reason?.takeIf(String::isNotBlank) ?: i.apiName) })
            })
        }

        if (aiItems.isNotEmpty() || aiState != AiState.Ready) add(ForYouSection.AiRow(aiItems, aiState))

        first(RecommendationType.TRENDING_IN_YOUR_GENRES)?.let {
            val tag = tagFor(it)
            add(row("top_genre", tag?.let { t -> ForYouCopy.topGenre(TagNormalizer.getDisplayName(t)) } ?: it.title, null, it, tag))
        }
        first(RecommendationType.EXPLORE_GENRE)?.let {
            val sub = tagFor(it)?.let { t -> ForYouCopy.genreYouMightEnjoy(TagNormalizer.getDisplayName(t)) }
            add(row("different", ForYouCopy.SOMETHING_DIFFERENT, sub, it))
        }
        first(RecommendationType.NEW_FOR_YOU)?.let { add(row("just_added", ForYouCopy.JUST_ADDED, null, it)) }

        if (allGenres.isNotEmpty()) add(ForYouSection.BrowseGenres(allGenres))
    }
}

/** Longest display name the group title ends with (engine prefixes may change). */
private fun tagFor(g: RecommendationGroup): TagCategory? =
    TagCategory.entries
        .filter { g.title.endsWith(TagNormalizer.getDisplayName(it), ignoreCase = true) }
        .maxByOrNull { TagNormalizer.getDisplayName(it).length }
