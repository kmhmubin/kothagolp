package com.kmhmubin.kothagolp.recommendation

import com.kmhmubin.kothagolp.data.local.entity.NovelDetailsEntity
import com.kmhmubin.kothagolp.domain.model.Novel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteModelTest {

    private val day = 86_400_000L
    private val now = 1_000 * day

    @Test
    fun seedWeight_ordersStatusesAndDecaysWithTime() {
        val completed = TasteModel.seedWeight("COMPLETED", 100, now, now)
        val reading = TasteModel.seedWeight("READING", 100, now, now)
        val plan = TasteModel.seedWeight("PLAN_TO_READ", 0, now, now)
        val dropped = TasteModel.seedWeight("DROPPED", 100, now, now)
        assertTrue(completed > reading && reading > plan && plan > 0f && dropped < 0f)

        val old = TasteModel.seedWeight("COMPLETED", 100, now - 400 * day, now)
        assertTrue(old < completed)
        assertEquals(0.3f, old, 0.001f) // floor: old favourites still count
    }

    @Test
    fun quality_trustsVotes() {
        val fewVotesPerfect = TasteModel.quality(1000, 3)
        val manyVotesGreat = TasteModel.quality(920, 2000)
        assertTrue(manyVotesGreat > fewVotesPerfect)
    }

    @Test
    fun idf_rareFeaturesOutweighCommonOnes() {
        val corpus = List(20) { mapOf("t:FANTASY" to 1f) } + mapOf("t:FANTASY" to 1f, "k:necromancer" to 1f)
        val v = TasteModel.vectorize(corpus.last(), TasteModel.idf(corpus))
        assertTrue(v.getValue("k:necromancer") > v.getValue("t:FANTASY"))
    }

    @Test
    fun combine_negativeSeedsSubtractButNeverGoBelowZero() {
        val liked = mapOf("t:ROMANCE" to 0.7f, "t:FANTASY" to 0.7f)
        val disliked = mapOf("t:FANTASY" to 1f)
        val profile = TasteModel.combine(listOf(liked to 1f, disliked to -1f))
        assertEquals(setOf("t:ROMANCE"), profile.keys)
        assertEquals(1f, TasteModel.cosine(profile, profile), 0.001f)
    }

    @Test
    fun diversify_skipsNearDuplicates() {
        val a = mapOf("t:A" to 1f)
        val b = mapOf("t:B" to 1f)
        val items = listOf("a1" to a, "a2" to a, "b1" to b).associate { it }
        val picked = TasteModel.diversify(
            listOf("a1" to 0.9f, "a2" to 0.89f, "b1" to 0.8f),
            { items.getValue(it) }, limit = 2
        ).map { it.first }
        assertEquals(listOf("a1", "b1"), picked)
    }

    @Test
    fun evidence_discountsThinVectors() {
        assertEquals(0.25f, TasteModel.evidence(mapOf("a" to 1f, "b" to 1f)), 0.001f)
        assertEquals(1f, TasteModel.evidence((1..12).associate { "k:$it" to 1f }), 0.001f)
    }

    @Test
    fun relatedNovels_roundTrip() {
        val novels = listOf(Novel(name = "X", url = "https://a/x", posterUrl = null, rating = 812, apiName = "LibRead"))
        val json = NovelDetailsEntity.serializeRelatedNovels(novels)
        assertNotEquals("[]", json)
        assertEquals(novels, NovelDetailsEntity.parseRelatedNovels(json))
    }
}
