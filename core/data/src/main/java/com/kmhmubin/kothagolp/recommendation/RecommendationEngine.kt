package com.kmhmubin.kothagolp.recommendation

import android.util.Log
import com.kmhmubin.kothagolp.data.local.dao.HistoryDao
import com.kmhmubin.kothagolp.data.local.dao.OfflineDao
import com.kmhmubin.kothagolp.data.local.dao.RecommendationDao
import com.kmhmubin.kothagolp.data.local.entity.AuthorPreferenceEntity
import com.kmhmubin.kothagolp.data.local.entity.DiscoveredNovelEntity
import com.kmhmubin.kothagolp.data.local.entity.NovelDetailsEntity
import com.kmhmubin.kothagolp.data.repository.LibraryRepository
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.recommendation.model.AiBrief
import com.kmhmubin.kothagolp.recommendation.model.AiCandidate
import com.kmhmubin.kothagolp.recommendation.model.NovelVector
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.recommendation.model.RecommendationGroup
import com.kmhmubin.kothagolp.recommendation.model.RecommendationType
import com.kmhmubin.kothagolp.recommendation.model.ScoreBreakdown
import com.kmhmubin.kothagolp.recommendation.model.UserTasteProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "RecommendationEngine"

/**
 * Generates the For You rows.
 *
 * Ranking is content-based (see [TasteModel]): the user's library is turned into a
 * TF-IDF taste vector — completed novels pull hardest, drops and "Not interested"
 * push away — and candidates are scored on taste match, closeness to individual
 * favourites, the sources' own "related novels" lists, and vote-weighted rating.
 * Rows are diversified with MMR and the top row carries "wildcard" picks so the
 * profile keeps learning beyond what the user already reads.
 */
class RecommendationEngine(
    private val userPreferenceManager: UserPreferenceManager,
    private val libraryRepository: LibraryRepository,
    private val offlineDao: OfflineDao,
    private val recommendationDao: RecommendationDao,
    private val userFilterManager: UserFilterManager,
    private val authorPreferenceManager: AuthorPreferenceManager,
    private val historyDao: HistoryDao
) {

    data class RecommendationConfig(
        val maxPerGroup: Int = 12,
        val minScore: Float = 0.25f,
        val sameProviderBoost: Float = 1.15f,
        val includeCrossProvider: Boolean = true,
        val preferredProvider: String? = null,
        /** Allow some overlap between groups (0 = no overlap, 0.3 = 30% can overlap) */
        val allowedOverlapRatio: Float = 0.2f,
        // Provider names that are disabled in settings — their novels won't appear
        val disabledProviders: Set<String> = emptySet()
    )

    /** A library (or hidden) novel that shapes the taste profile. */
    private data class Seed(val url: String, val name: String, val weight: Float, val features: Features)

    /** Everything a row generator needs, computed once per refresh. */
    private inner class Context(
        val candidates: List<NovelVector>,
        val features: Map<String, Features>,
        val profile: Features,
        val positiveSeeds: List<Seed>,
        val negativeSeeds: List<Seed>,
        /** candidate url → seeds whose source page lists it as related */
        val relatedBy: Map<String, List<Seed>>,
        val authorAffinities: Map<String, Float>,
        val favoriteAuthors: Set<String>,
        val filterState: UserFilterManager.FilterState,
        val userProfile: UserTasteProfile
    ) {
        fun f(n: NovelVector): Features = features[n.url].orEmpty()
        fun quality(n: NovelVector) = TasteModel.quality(n.rating, n.peopleVoted)
        fun taste(n: NovelVector) = f(n).let { TasteModel.cosine(profile, it) * TasteModel.evidence(it) }
        fun related(n: NovelVector) =
            (relatedBy[n.url]?.sumOf { it.weight.toDouble() }?.toFloat() ?: 0f).coerceAtMost(1f)

        /** Closest positive seed and how close, scaled by how much that seed matters. */
        fun bestSeed(n: NovelVector): Pair<Seed?, Float> {
            val fn = f(n)
            var best: Seed? = null
            var bestSim = 0f
            for (s in positiveSeeds) {
                val sim = TasteModel.cosine(s.features, fn) * TasteModel.evidence(fn) * (s.weight / 0.8f).coerceAtMost(1f)
                if (sim > bestSim) { bestSim = sim; best = s }
            }
            return best to bestSim
        }

        fun dislike(n: NovelVector): Float {
            val fn = f(n)
            return negativeSeeds.maxOfOrNull { TasteModel.cosine(it.features, fn) } ?: 0f
        }
    }

    // ================================================================
    // MAIN RECOMMENDATION GENERATION
    // ================================================================

    suspend fun generateRecommendations(
        config: RecommendationConfig = RecommendationConfig()
    ): List<RecommendationGroup> = withContext(Dispatchers.IO) {
        val ctx = buildContext(config) ?: return@withContext emptyList()
        val groups = mutableListOf<RecommendationGroup>()
        val used = mutableSetOf<String>()

        fun add(group: RecommendationGroup?) {
            if (group == null) return
            groups += group
            used += group.recommendations.map { it.novel.url }
        }

        add(topPicksGroup(ctx, config, used))
        // Strongest seeds, skipping near-copies (same novel saved from two sources).
        val rowSeeds = mutableListOf<Seed>()
        for (seed in ctx.positiveSeeds.filter { it.features.isNotEmpty() }.sortedByDescending { it.weight }) {
            if (rowSeeds.size == 3) break
            if (rowSeeds.none { TasteModel.cosine(it.features, seed.features) > 0.8f }) rowSeeds += seed
        }
        rowSeeds.forEach { add(becauseYouReadGroup(ctx, it, config, used)) }

        val likedAuthors = authorPreferenceManager.getLikedAuthors(50)
        if (likedAuthors.any { it.isFavorite } || likedAuthors.size >= 2) {
            add(generateFromAuthorsYouLikeGroup(ctx.candidates, likedAuthors, config, used, 0, ctx.filterState))
        }

        favouriteTags(ctx).take(2).forEach { add(bestInGenreGroup(ctx, it, config, used)) }
        add(exploreGroup(ctx, config, used))
        add(generateNewArrivalsGroup(ctx, config, used))

        Log.d(TAG, "Rows: ${groups.joinToString { "${it.title} (${it.recommendations.size})" }}")
        Log.d(TAG, "Generated ${groups.size} groups with ${used.size} unique novels " +
                "from ${ctx.candidates.size} candidates, ${ctx.positiveSeeds.size} seeds")
        groups
    }

    /** Top tag categories in the taste profile, strongest first. */
    private fun favouriteTags(ctx: Context): List<TagNormalizer.TagCategory> =
        ctx.profile.entries
            .filter { it.key.startsWith("t:") }
            .sortedByDescending { it.value }
            .mapNotNull { runCatching { TagNormalizer.TagCategory.valueOf(it.key.removePrefix("t:")) }.getOrNull() }

    /** Top tags the user is into — used by discovery to fetch more of what they like. */
    suspend fun topTasteTags(n: Int = 3): List<TagNormalizer.TagCategory> = withContext(Dispatchers.IO) {
        buildContext(RecommendationConfig())?.let { favouriteTags(it).take(n) } ?: emptyList()
    }

    private suspend fun buildContext(config: RecommendationConfig): Context? {
        val library = libraryRepository.getLibrary()
        val libraryUrls = library.map { it.novel.url }.toSet()
        val filterState = userFilterManager.getFilterState()
        val userProfile = userPreferenceManager.getUserProfile()
        val likedAuthors = authorPreferenceManager.getLikedAuthors(50)

        // Same novel saved from another source has a different URL: match on title too.
        val libraryTitles = library.map { normalizeTitleForDedup(it.novel.name) }.toSet()
        val candidates = loadAllCandidates(
            libraryUrls, filterState, config.disabledProviders, config.preferredProvider
        ).filterTo(mutableListOf()) { normalizeTitleForDedup(it.name) !in libraryTitles }

        // Seeds: library entries weighted by status/progress/recency, plus "Not interested".
        val now = System.currentTimeMillis()
        val seedVectors = library.mapNotNull { item ->
            val vector = loadNovelVector(item.novel.url) ?: return@mapNotNull null
            val chapters = runCatching { historyDao.getReadChapterCount(item.novel.url) }.getOrDefault(0)
            val status = if (item.isSpicy) "SPICY" else item.readingStatus.name
            val weight = TasteModel.seedWeight(status, chapters, item.lastReadPosition?.timestamp ?: item.addedAt, now)
            Triple(vector, item.novel.name, weight)
        } + filterState.hiddenNovelUrls.mapNotNull { url ->
            loadNovelVector(url)?.let { Triple(it, it.name, -0.6f) }
        }

        // Sources' "related novels" lists for the novels the user liked.
        val related = HashMap<String, MutableList<String>>() // candidate url → seed urls
        val known = candidates.associateBy { it.url }.toMutableMap()
        for ((vector, _, weight) in seedVectors) {
            if (weight <= 0f) continue
            val json = offlineDao.getNovelDetails(vector.url)?.relatedNovelsJson ?: continue
            val list = runCatching { NovelDetailsEntity.parseRelatedNovels(json) }.getOrNull() ?: continue
            for (novel in list) {
                if (novel.url in libraryUrls || normalizeTitleForDedup(novel.name) in libraryTitles) continue
                if (novel.url !in known) {
                    val minimal = NovelVector(
                        url = novel.url, name = novel.name,
                        providerName = novel.apiName.ifBlank { extractProviderFromUrl(novel.url) },
                        tags = emptySet(), rawTags = emptyList(), authorNormalized = null,
                        rating = novel.rating, chapterCount = 0, isCompleted = false,
                        synopsisKeywords = emptySet(), titleKeywords = NovelVector.extractTitleKeywords(novel.name),
                        posterUrl = novel.posterUrl
                    )
                    if (minimal.providerName in config.disabledProviders ||
                        userFilterManager.shouldFilter(minimal, filterState)) continue
                    known[novel.url] = minimal
                    candidates += minimal
                }
                related.getOrPut(novel.url) { mutableListOf() } += vector.url
            }
        }

        if (candidates.isEmpty()) return null

        val raw = (candidates + seedVectors.map { it.first }).associate { it.url to TasteModel.rawFeatures(it) }
        val idf = TasteModel.idf(raw.values)
        val features = raw.mapValues { TasteModel.vectorize(it.value, idf) }

        val seeds = seedVectors.map { (v, name, w) -> Seed(v.url, name, w, features[v.url].orEmpty()) }
        val positive = seeds.filter { it.weight > 0f }
        val negative = seeds.filter { it.weight < 0f }

        // Explicit genre picks (onboarding / genre sheet) and boosted tags, so a new user isn't cold.
        val explicit = TasteModel.normalize(
            userProfile.preferredTags.associate { "t:${it.tag.name}" to it.score * it.confidence } +
                filterState.boostedTags.associate { "t:${it.name}" to 1f }
        )
        val explicitWeight = if (positive.isEmpty()) 1f else 0.35f
        val profile = TasteModel.combine(
            seeds.map { it.features to it.weight } + (explicit to explicitWeight)
        )

        val seedByUrl = seeds.associateBy { it.url }
        val relatedBy = related.mapValues { (_, urls) -> urls.distinct().mapNotNull { seedByUrl[it] } }

        return Context(
            candidates = candidates,
            features = features,
            profile = profile,
            positiveSeeds = positive,
            negativeSeeds = negative,
            relatedBy = relatedBy,
            authorAffinities = likedAuthors.associate { it.authorNormalized to it.affinityScore / 1000f },
            favoriteAuthors = likedAuthors.filter { it.isFavorite }.map { it.authorNormalized }.toSet(),
            filterState = filterState,
            userProfile = userProfile
        )
    }

    // ================================================================
    // ROWS
    // ================================================================

    /** The main personal score: taste, closest favourite, related lists, quality, author. */
    private fun forYouScore(ctx: Context, n: NovelVector): Float {
        var score = if (ctx.profile.isNotEmpty()) {
            val (_, seedSim) = ctx.bestSeed(n)
            val author = n.authorNormalized?.let { ctx.authorAffinities[it] } ?: 0f
            0.45f * ctx.taste(n) + 0.20f * seedSim + 0.15f * ctx.related(n) +
                0.12f * ctx.quality(n) + 0.08f * author - 0.3f * ctx.dislike(n)
        } else {
            ctx.quality(n) // brand-new user with no picks: best-rated first
        }
        if (n.authorNormalized in ctx.favoriteAuthors) score *= 1.15f
        return score * userFilterManager.calculateBoostFactor(n, ctx.filterState)
    }

    /**
     * What the AI re-ranker sees: a compact taste summary plus the engine's best
     * [maxCandidates] real novels. The AI only chooses among these, so every pick opens.
     */
    suspend fun aiBrief(
        config: RecommendationConfig = RecommendationConfig(),
        maxCandidates: Int = 40
    ): AiBrief? = withContext(Dispatchers.IO) {
        val ctx = buildContext(config) ?: return@withContext null
        val picks = TasteModel.diversify(ctx.candidates.map { it to forYouScore(ctx, it) }, ctx::f, maxCandidates, lambda = 0.7f)
        fun tagsOf(f: Features) = f.entries.filter { it.key.startsWith("t:") }
            .sortedByDescending { it.value }.take(4).map { tagName(it.key.removePrefix("t:")) }
        AiBrief(
            liked = ctx.positiveSeeds.sortedByDescending { it.weight }.take(15).map { seed ->
                tagsOf(seed.features).let { if (it.isEmpty()) seed.name else "${seed.name} (${it.joinToString()})" }
            },
            disliked = ctx.negativeSeeds.take(10).map { it.name },
            topTags = favouriteTags(ctx).take(6).map { TagNormalizer.getDisplayName(it) },
            candidates = picks.mapIndexed { i, (n, score) ->
                AiCandidate(
                    id = "n${i + 1}",
                    recommendation = recommendation(ctx, n, RecommendationType.FOR_YOU, score, forYouReason(ctx, n)),
                    tags = n.tags.map { TagNormalizer.getDisplayName(it) }.take(6),
                    synopsis = n.synopsis?.replace(Regex("\\s+"), " ")?.take(240)
                )
            }
        )
    }

    private fun topPicksGroup(ctx: Context, config: RecommendationConfig, used: Set<String>): RecommendationGroup? {
        val hasTaste = ctx.profile.isNotEmpty()
        val scored = ctx.candidates.filter { it.url !in used }.map { it to forYouScore(ctx, it) }
        if (scored.isEmpty()) return null

        val picks = TasteModel.diversify(scored, ctx::f, config.maxPerGroup).toMutableList()

        // Wildcards: well-rated novels just outside the user's usual taste, so the
        // profile can learn new likes instead of narrowing forever.
        if (hasTaste && picks.size >= 6) {
            val pickedUrls = picks.map { it.first.url }.toSet()
            val wild = ctx.candidates
                .filter { it.url !in pickedUrls && it.url !in used && it.tags.isNotEmpty() }
                .filter { ctx.taste(it) in 0.02f..0.25f && ctx.dislike(it) < 0.3f && ctx.quality(it) >= 0.7f }
                .sortedByDescending { ctx.quality(it) }
                .take(2)
            listOf(3, 8).zip(wild).forEach { (i, n) ->
                if (i <= picks.size) picks.add(i, n to -1f) // -1 marks a wildcard
            }
        }

        val recs = picks.take(config.maxPerGroup).map { (n, score) ->
            val wildcard = score < 0f
            recommendation(ctx, n, RecommendationType.FOR_YOU, if (wildcard) ctx.quality(n) else score,
                reason = if (wildcard) wildcardReason(ctx, n) else forYouReason(ctx, n))
        }
        return RecommendationGroup(
            type = RecommendationType.FOR_YOU,
            title = if (hasTaste) "Top picks" else "Popular right now",
            subtitle = if (hasTaste) "Ranked from what you read and finish" else "Read a few novels and this learns your taste",
            recommendations = recs
        )
    }

    private fun becauseYouReadGroup(
        ctx: Context, seed: Seed, config: RecommendationConfig, used: Set<String>
    ): RecommendationGroup? {
        val scored = ctx.candidates.filter { it.url !in used }.mapNotNull { n ->
            val sim = ctx.f(n).let { TasteModel.cosine(seed.features, it) * TasteModel.evidence(it) }
            val listed = ctx.relatedBy[n.url]?.any { it.url == seed.url } == true
            val score = 0.55f * sim + (if (listed) 0.30f else 0f) + 0.15f * ctx.quality(n) - 0.3f * ctx.dislike(n)
            if (sim < 0.12f && !listed) null else n to score
        }
        if (scored.size < 3) return null
        val recs = TasteModel.diversify(scored, ctx::f, config.maxPerGroup).map { (n, score) ->
            val listed = ctx.relatedBy[n.url]?.any { it.url == seed.url } == true
            val shared = TasteModel.sharedFeatures(seed.features, ctx.f(n), "t:", 2).map(::tagName)
            recommendation(ctx, n, RecommendationType.BECAUSE_YOU_READ, score,
                reason = when {
                    listed -> "Readers of ${seed.name.ellipsize(22)} read this too"
                    shared.isNotEmpty() -> "Because you liked ${seed.name.ellipsize(26)}"
                    else -> "Because you liked ${seed.name.ellipsize(26)}"
                },
                sourceNovelUrl = seed.url, sourceNovelName = seed.name)
        }
        return RecommendationGroup(
            type = RecommendationType.BECAUSE_YOU_READ,
            title = "More like ${seed.name.ellipsize(25)}",
            subtitle = "Similar stories you might enjoy",
            recommendations = recs,
            sourceNovel = Novel(name = seed.name, url = seed.url)
        )
    }

    private fun bestInGenreGroup(
        ctx: Context, tag: TagNormalizer.TagCategory, config: RecommendationConfig, used: Set<String>
    ): RecommendationGroup? {
        val scored = ctx.candidates
            .filter { tag in it.tags && it.url !in used }
            .map { it to 0.55f * ctx.quality(it) + 0.45f * ctx.taste(it) - 0.3f * ctx.dislike(it) }
        if (scored.size < 4) return null
        val name = TagNormalizer.getDisplayName(tag)
        val recs = TasteModel.diversify(scored, ctx::f, config.maxPerGroup).map { (n, score) ->
            recommendation(ctx, n, RecommendationType.TRENDING_IN_YOUR_GENRES, score,
                reason = "Top ${name.lowercase()} for you")
        }
        return RecommendationGroup(
            type = RecommendationType.TRENDING_IN_YOUR_GENRES,
            title = name, // bare genre name: ForYouSections builds the visible title from it
            subtitle = "Top ${name.lowercase()} for you",
            recommendations = recs
        )
    }

    /** One genre the user hasn't explored but that sits close to what they like. */
    private fun exploreGroup(ctx: Context, config: RecommendationConfig, used: Set<String>): RecommendationGroup? {
        if (ctx.profile.isEmpty()) return null
        val avoided = ctx.userProfile.avoidedTags.map { it.tag }.toSet() + ctx.filterState.blockedTags
        val byTag = ctx.candidates.filter { it.url !in used }
            .flatMap { n -> n.tags.map { it to n } }
            .groupBy({ it.first }, { it.second })
        val tag = byTag
            .filter { (t, novels) ->
                novels.size >= 6 && t !in avoided && (ctx.profile["t:${t.name}"] ?: 0f) < 0.05f &&
                    !TagNormalizer.hasMatureContent(setOf(t))
            }
            .maxByOrNull { (_, novels) -> novels.map { ctx.taste(it) }.sorted().takeLast(5).average() }
            ?.key ?: return null
        val scored = byTag.getValue(tag).map { it to 0.6f * ctx.quality(it) + 0.4f * ctx.taste(it) }
        val name = TagNormalizer.getDisplayName(tag)
        val recs = TasteModel.diversify(scored, ctx::f, config.maxPerGroup).map { (n, score) ->
            recommendation(ctx, n, RecommendationType.EXPLORE_GENRE, score, reason = "Something new: ${name.lowercase()}")
        }
        return RecommendationGroup(
            type = RecommendationType.EXPLORE_GENRE,
            title = name,
            subtitle = "$name you might enjoy",
            recommendations = recs
        )
    }

    private fun generateNewArrivalsGroup(ctx: Context, config: RecommendationConfig, used: Set<String>): RecommendationGroup? {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val fresh = ctx.candidates
            .filter { it.discoveredAt > thirtyDaysAgo && !it.isCompleted && it.url !in used }
            .map { it to 0.6f * ctx.taste(it) + 0.4f * ctx.quality(it) - 0.3f * ctx.dislike(it) }
        if (fresh.size < 3) return null
        val recs = TasteModel.diversify(fresh, ctx::f, config.maxPerGroup).map { (n, score) ->
            recommendation(ctx, n, RecommendationType.NEW_FOR_YOU, score, reason = "Just added")
        }
        return RecommendationGroup(
            type = RecommendationType.NEW_FOR_YOU,
            title = "Just added",
            subtitle = "Recently found, matched to your taste",
            recommendations = recs
        )
    }

    // ================================================================
    // EXPLANATIONS
    // ================================================================

    private fun forYouReason(ctx: Context, n: NovelVector): String {
        ctx.relatedBy[n.url]?.maxByOrNull { it.weight }?.let { return "Readers of ${it.name.ellipsize(22)} read this too" }
        val (seed, sim) = ctx.bestSeed(n)
        if (seed != null && sim >= 0.3f) return "Because you liked ${seed.name.ellipsize(26)}"
        val shared = TasteModel.sharedFeatures(ctx.profile, ctx.f(n), "t:", 2).map(::tagName)
        if (shared.isNotEmpty()) return "Matches your love of ${shared.joinToString(" and ") { it.lowercase() }}"
        return n.rating?.let { "Rated %.1f on %s".format(it / 200f, n.providerName) } ?: "Recommended for you"
    }

    private fun wildcardReason(ctx: Context, n: NovelVector): String {
        val tag = n.tags.firstOrNull { (ctx.profile["t:${it.name}"] ?: 0f) < 0.05f } ?: n.tags.first()
        return "Something new: ${TagNormalizer.getDisplayName(tag).lowercase()}"
    }

    private fun tagName(key: String): String =
        runCatching { TagNormalizer.getDisplayName(TagNormalizer.TagCategory.valueOf(key)) }.getOrDefault(key)

    private fun String.ellipsize(max: Int) = if (length > max) take(max - 1).trimEnd() + "…" else this

    private fun recommendation(
        ctx: Context, n: NovelVector, type: RecommendationType, score: Float, reason: String,
        sourceNovelUrl: String? = null, sourceNovelName: String? = null
    ): Recommendation {
        val (_, seedSim) = ctx.bestSeed(n)
        return Recommendation(
            novel = Novel(name = n.name, url = n.url, posterUrl = n.posterUrl, rating = n.rating, apiName = n.providerName),
            // Raw scores sit around 0.2–0.6; sqrt keeps the order but reads sensibly as "% match".
            score = kotlin.math.sqrt(score.coerceIn(0f, 1f)),
            type = type,
            reason = reason,
            scoreBreakdown = ScoreBreakdown(
                tagSimilarity = ctx.taste(n),
                synopsisMatch = seedSim,
                popularityScore = ctx.related(n),
                ratingScore = ctx.quality(n),
                authorMatch = n.authorNormalized?.let { ctx.authorAffinities[it] } ?: 0f
            ),
            sourceNovelUrl = sourceNovelUrl,
            sourceNovelName = sourceNovelName
        )
    }

    // ================================================================
    // CANDIDATE LOADING - Cross-source title deduplication
    // ================================================================

    private suspend fun loadAllCandidates(
        excludeUrls: Set<String>,
        filterState: UserFilterManager.FilterState? = null,
        disabledProviders: Set<String> = emptySet(),
        preferredProvider: String? = null
    ): List<NovelVector> {
        // url → vector (for fast lookup)
        val byUrl = mutableMapOf<String, NovelVector>()
        // normalizedTitle → url of currently-kept entry (cross-source title dedup)
        val byNormalizedTitle = mutableMapOf<String, String>()

        fun tryAdd(vector: NovelVector) {
            if (vector.providerName in disabledProviders) return
            if (filterState != null && userFilterManager.shouldFilter(vector, filterState)) return

            val normalizedTitle = normalizeTitleForDedup(vector.name)
            val existingUrl = byNormalizedTitle[normalizedTitle]

            if (existingUrl == null) {
                byUrl[vector.url] = vector
                byNormalizedTitle[normalizedTitle] = vector.url
            } else {
                val existing = byUrl[existingUrl] ?: return
                // Keep the version from the preferred provider; otherwise keep higher-rated one
                val keepNew = when {
                    preferredProvider != null &&
                        vector.providerName == preferredProvider &&
                        existing.providerName != preferredProvider -> true
                    preferredProvider != null &&
                        existing.providerName == preferredProvider -> false
                    (vector.rating ?: 0) > (existing.rating ?: 0) + 50 -> true
                    else -> false
                }
                if (keepNew) {
                    byUrl.remove(existingUrl)
                    byUrl[vector.url] = vector
                    byNormalizedTitle[normalizedTitle] = vector.url
                }
            }
        }

        // Priority 1: NovelDetailsEntity (full details, richer data)
        offlineDao.getAllNovelDetailsWithoutChapters().forEach { entity ->
            if (entity.url !in excludeUrls) {
                entityToVector(entity)?.let { tryAdd(it) }
            }
        }

        // Priority 2: DiscoveredNovelEntity (skip if URL already indexed above)
        recommendationDao.getAllDiscoveredNovels().forEach { entity ->
            if (entity.url !in excludeUrls && entity.url !in byUrl) {
                discoveredToVector(entity)?.let { tryAdd(it) }
            }
        }

        return byUrl.values.toList()
    }

    /** Normalize a novel title for cross-source deduplication.
     *  "The Legendary Mechanic" == "legendary mechanic" == "Legendary Mechanic, The"
     */
    private fun normalizeTitleForDedup(name: String): String {
        val stopWords = setOf("the", "a", "an", "of", "in", "on", "at", "to", "and", "by")
        return name
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
            .sorted()      // order-insensitive: "Sword Saint" == "Saint Sword" won't match, but normalizes minor word order
            .joinToString(" ")
            .trim()
    }


    private fun discoveredToVector(entity: DiscoveredNovelEntity): NovelVector? {
        // Start with normalized provider tags
        var tags = TagNormalizer.normalizeAll(entity.tags)

        // Extract tags from title
        val titleTags = SynopsisTagExtractor.extractFromTitle(entity.name)

        // If no/few tags, extract from synopsis
        if (tags.size < 3) {
            val synopsisTags = SynopsisTagExtractor.extractTags(entity.synopsis, maxTags = 8)
            tags = tags + titleTags + synopsisTags
        } else {
            // Still add title tags as they're high confidence
            tags = tags + titleTags
        }

        // Extract keywords for text-based matching
        val synopsisKeywords = SynopsisTagExtractor.extractContentKeywords(entity.synopsis, maxKeywords = 25)
        val titleKeywords = NovelVector.extractTitleKeywords(entity.name)

        return NovelVector(
            url = entity.url,
            name = entity.name,
            providerName = entity.apiName,
            tags = tags,
            rawTags = entity.tags,
            authorNormalized = NovelVector.normalizeAuthor(entity.author),
            rating = entity.rating,
            chapterCount = 0,
            isCompleted = entity.status?.lowercase() == "completed",
            synopsisKeywords = synopsisKeywords,
            titleKeywords = titleKeywords,
            posterUrl = entity.posterUrl,
            synopsis = entity.synopsis,
            discoveredAt = entity.discoveredAt
        )
    }

    private fun entityToVector(entity: NovelDetailsEntity): NovelVector? {
        // Start with normalized provider tags
        var tags = entity.tags?.let { TagNormalizer.normalizeAll(it) } ?: emptySet()

        // Extract tags from title
        val titleTags = SynopsisTagExtractor.extractFromTitle(entity.name)

        // If no/few tags, extract from synopsis
        if (tags.size < 3) {
            val synopsisTags = SynopsisTagExtractor.extractTags(entity.synopsis, maxTags = 8)
            tags = tags + titleTags + synopsisTags
        } else {
            tags = tags + titleTags
        }

        // Extract keywords
        val synopsisKeywords = SynopsisTagExtractor.extractContentKeywords(entity.synopsis, maxKeywords = 25)
        val titleKeywords = NovelVector.extractTitleKeywords(entity.name)

        return NovelVector(
            url = entity.url,
            name = entity.name,
            providerName = entity.apiName.ifBlank { extractProviderFromUrl(entity.url) },
            tags = tags,
            rawTags = entity.tags ?: emptyList(),
            authorNormalized = NovelVector.normalizeAuthor(entity.author),
            rating = entity.rating,
            chapterCount = entity.chapterCount,
            isCompleted = entity.status?.lowercase() == "completed",
            synopsisKeywords = synopsisKeywords,
            titleKeywords = titleKeywords,
            posterUrl = entity.posterUrl,
            synopsis = entity.synopsis,
            peopleVoted = entity.peopleVoted
        )
    }

    private fun extractProviderFromUrl(url: String): String {
        return when {
            url.contains("novelfire") -> "NovelFire"
            url.contains("royalroad") -> "Royal Road"
            url.contains("webnovel") -> "Webnovel"
            url.contains("novelarrow") -> "NovelArrow"
            url.contains("novelbin") || url.contains("novel-bin") -> "NovelBin"
            url.contains("libread") -> "LibRead"
            url.contains("novelsonline") -> "NovelsOnline"
            else -> "Unknown"
        }
    }

    private fun generateFromAuthorsYouLikeGroup(
        candidates: List<NovelVector>,
        likedAuthors: List<AuthorPreferenceEntity>,
        config: RecommendationConfig,
        usedUrls: Set<String>,
        maxOverlap: Int,
        filterState: UserFilterManager.FilterState
    ): RecommendationGroup? {
        if (likedAuthors.isEmpty()) return null

        val likedAuthorNames = likedAuthors
            .filter { it.affinityScore >= 600 }
            .map { it.authorNormalized }
            .toSet()

        if (likedAuthorNames.isEmpty()) return null

        val novelsByLikedAuthors = candidates
            .filter { novel ->
                novel.authorNormalized != null &&
                        novel.authorNormalized in likedAuthorNames &&
                        novel.url !in usedUrls &&
                        !userFilterManager.shouldFilter(novel, filterState)
            }
            .sortedWith(
                compareByDescending<NovelVector> {
                    likedAuthors.find { a -> a.authorNormalized == it.authorNormalized }?.affinityScore ?: 0
                }.thenByDescending { it.rating ?: 0 }
            )
            .take(config.maxPerGroup)

        if (novelsByLikedAuthors.size < 2) return null

        val authorDisplayNames = likedAuthors.associate { it.authorNormalized to it.displayName }

        val recommendations = novelsByLikedAuthors.map { novel ->
            val authorName = authorDisplayNames[novel.authorNormalized] ?: novel.authorNormalized ?: "Unknown"
            val authorEntity = likedAuthors.find { it.authorNormalized == novel.authorNormalized }

            val breakdown = ScoreBreakdown(
                authorMatch = (authorEntity?.affinityScore ?: 500) / 1000f,
                ratingScore = (novel.rating ?: 0) / 1000f,
                tagSimilarity = 0.5f,
                userPreferenceMatch = 0.6f
            )

            Recommendation(
                novel = Novel(
                    name = novel.name,
                    url = novel.url,
                    posterUrl = novel.posterUrl,
                    rating = novel.rating,
                    apiName = novel.providerName
                ),
                score = breakdown.total,
                type = RecommendationType.FROM_AUTHORS_YOU_LIKE,
                reason = authorName, // bare name: the authors row shows it as the caption
                scoreBreakdown = breakdown,
                isCrossProvider = false
            )
        }

        return RecommendationGroup(
            type = RecommendationType.FROM_AUTHORS_YOU_LIKE,
            title = "By authors you like",
            subtitle = "More from authors you've enjoyed",
            recommendations = recommendations
        )
    }

    // ================================================================
    // SINGLE NOVEL RECOMMENDATIONS
    // ================================================================

    /** "Because you read" for one chosen library novel (the source picker on For You). */
    suspend fun getSimilarTo(
        novelUrl: String,
        limit: Int = 10,
        @Suppress("UNUSED_PARAMETER") excludeLibrary: Boolean = true, // library is always excluded
        disabledProviders: Set<String> = emptySet()
    ): List<Recommendation> = withContext(Dispatchers.IO) {
        val config = RecommendationConfig(maxPerGroup = limit, disabledProviders = disabledProviders)
        val ctx = buildContext(config) ?: return@withContext emptyList()
        val seed = ctx.positiveSeeds.find { it.url == novelUrl }
            ?: loadNovelVector(novelUrl)?.let { v ->
                Seed(v.url, v.name, 0.8f, ctx.features[v.url] ?: TasteModel.normalize(TasteModel.rawFeatures(v)))
            }
            ?: return@withContext emptyList()
        if (seed.features.isEmpty()) return@withContext emptyList()
        becauseYouReadGroup(ctx, seed, config, setOf(novelUrl))?.recommendations ?: emptyList()
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private suspend fun loadNovelVector(url: String): NovelVector? {
        offlineDao.getNovelDetails(url)?.let { return entityToVector(it) }
        recommendationDao.getDiscoveredNovel(url)?.let { return discoveredToVector(it) }
        return null
    }
}
