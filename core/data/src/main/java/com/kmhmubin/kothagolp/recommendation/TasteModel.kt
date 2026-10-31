package com.kmhmubin.kothagolp.recommendation

import com.kmhmubin.kothagolp.recommendation.model.NovelVector
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Sparse feature vector: feature key → weight, L2-normalised.
 * Keys are prefixed by kind: "t:" tag category, "k:" synopsis/title keyword, "a:" author.
 */
typealias Features = Map<String, Float>

/**
 * Pure, Android-free maths behind For You.
 *
 * Every novel becomes a TF-IDF-weighted bag of tags, keywords and author, so rare
 * signals ("necromancer") outweigh ubiquitous ones ("cultivation"). The user's taste
 * is the weighted sum of the novels they interacted with (negative for drops and
 * "Not interested"), and candidates are ranked by cosine similarity to it.
 */
object TasteModel {

    private const val TAG_WEIGHT = 1.0f
    private const val KEYWORD_WEIGHT = 0.45f
    private const val AUTHOR_WEIGHT = 0.8f

    /** Raw (un-IDF'd) features of one novel. */
    fun rawFeatures(novel: NovelVector): Map<String, Float> {
        val f = HashMap<String, Float>()
        novel.tags.forEach { f["t:${it.name}"] = TAG_WEIGHT }
        // Title words are left out: "system", "lord", "lost" match everything and say little.
        novel.synopsisKeywords.forEach { f["k:$it"] = KEYWORD_WEIGHT }
        novel.authorNormalized?.let { f["a:$it"] = AUTHOR_WEIGHT }
        return f
    }

    /**
     * How far to trust a similarity computed from [features]: a novel known only by
     * two or three features would otherwise score as a perfect match by accident.
     */
    fun evidence(features: Features, full: Int = 8): Float = (features.size / full.toFloat()).coerceAtMost(1f)

    /** Smoothed inverse document frequency over a corpus of raw feature maps. */
    fun idf(corpus: Collection<Map<String, Float>>): Map<String, Float> {
        val df = HashMap<String, Int>()
        corpus.forEach { doc -> doc.keys.forEach { df[it] = (df[it] ?: 0) + 1 } }
        val n = corpus.size.toFloat()
        return df.mapValues { (_, d) -> ln((n + 1f) / (d + 1f)) + 1f }
    }

    fun vectorize(raw: Map<String, Float>, idf: Map<String, Float>): Features =
        normalize(raw.mapValues { (k, w) -> w * (idf[k] ?: 1f) })

    fun normalize(v: Map<String, Float>): Features {
        val norm = sqrt(v.values.sumOf { (it * it).toDouble() }).toFloat()
        if (norm == 0f) return emptyMap()
        return v.mapValues { it.value / norm }
    }

    fun cosine(a: Features, b: Features): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val (small, large) = if (a.size <= b.size) a to b else b to a
        var dot = 0f
        for ((k, w) in small) large[k]?.let { dot += w * it }
        return dot
    }

    /** Weighted sum of vectors (weights may be negative), normalised. */
    fun combine(weighted: List<Pair<Features, Float>>): Features {
        val acc = HashMap<String, Float>()
        for ((vec, w) in weighted) for ((k, x) in vec) acc[k] = (acc[k] ?: 0f) + x * w
        // Negative evidence only pushes features down; never recommend "anti-taste".
        return normalize(acc.filterValues { it > 0f })
    }

    /**
     * How much a library novel should shape the profile.
     * Status sets direction, progress and recency set strength.
     */
    fun seedWeight(
        status: String,
        chaptersRead: Int,
        lastActivityMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Float {
        val base = when (status) {
            "COMPLETED" -> 1.0f
            "READING", "SPICY" -> 0.8f
            "ON_HOLD" -> 0.3f
            "PLAN_TO_READ" -> 0.25f
            "DROPPED" -> -0.7f
            else -> 0.4f
        }
        if (base < 0f) return base
        // Engagement: 0.5 for an untouched entry, up to 1.0 after ~50 chapters.
        val engagement = 0.5f + 0.5f * (chaptersRead / 50f).coerceIn(0f, 1f)
        // Recency: half-life ~60 days, but old favourites never fall below 30%.
        val days = ((nowMs - lastActivityMs).coerceAtLeast(0L)) / 86_400_000f
        val recency = max(0.3f, exp(-days / 87f))
        return base * engagement * recency
    }

    /**
     * Bayesian-shrunk rating in 0..1: few votes pull toward a neutral prior,
     * so a 5★ novel with 3 votes doesn't beat a 4.6★ one with 2,000.
     */
    fun quality(rating: Int?, votes: Int?, prior: Float = 0.6f, priorVotes: Int = 25): Float {
        if (rating == null) return prior * 0.9f
        val r = (rating / 1000f).coerceIn(0f, 1f)
        val v = (votes ?: priorVotes).coerceAtLeast(0).toFloat()
        return (v * r + priorVotes * prior) / (v + priorVotes)
    }

    /**
     * Maximal Marginal Relevance: greedy pick balancing score against similarity to
     * already-picked items, so a row isn't ten copies of the same story.
     */
    fun <T> diversify(
        scored: List<Pair<T, Float>>,
        features: (T) -> Features,
        limit: Int,
        lambda: Float = 0.75f
    ): List<Pair<T, Float>> {
        val remaining = scored.sortedByDescending { it.second }.take(limit * 4).toMutableList()
        val picked = mutableListOf<Pair<T, Float>>()
        while (picked.size < limit && remaining.isNotEmpty()) {
            val best = remaining.maxBy { (item, score) ->
                val f = features(item)
                val redundancy = picked.maxOfOrNull { cosine(f, features(it.first)) } ?: 0f
                lambda * score - (1 - lambda) * redundancy
            }
            picked += best
            remaining -= best
        }
        return picked
    }

    /** Positive features shared by both vectors, strongest first (for "why" text). */
    fun sharedFeatures(a: Features, b: Features, prefix: String, n: Int): List<String> =
        a.keys.filter { it.startsWith(prefix) && it in b }
            .sortedByDescending { (a[it] ?: 0f) * (b[it] ?: 0f) }
            .take(n)
            .map { it.removePrefix(prefix) }
}
