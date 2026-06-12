package com.kmhmubin.kothagolp.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagNormalizerTest {

    @Test
    fun normalize_exactAlias_returnsCategory() {
        assertEquals(TagNormalizer.TagCategory.FANTASY, TagNormalizer.normalize("fantasy"))
        assertEquals(TagNormalizer.TagCategory.ROMANCE, TagNormalizer.normalize("romance"))
        assertEquals(TagNormalizer.TagCategory.ACTION, TagNormalizer.normalize("action"))
        assertEquals(TagNormalizer.TagCategory.LITRPG, TagNormalizer.normalize("litrpg"))
    }

    @Test
    fun normalize_caseInsensitive_returnsCategory() {
        assertEquals(TagNormalizer.TagCategory.FANTASY, TagNormalizer.normalize("Fantasy"))
        assertEquals(TagNormalizer.TagCategory.FANTASY, TagNormalizer.normalize("FANTASY"))
        assertEquals(TagNormalizer.TagCategory.XIANXIA, TagNormalizer.normalize("Xianxia"))
    }

    @Test
    fun normalize_providerVariant_mapsToCanonical() {
        assertEquals(TagNormalizer.TagCategory.SCI_FI, TagNormalizer.normalize("sci-fi"))
        assertEquals(TagNormalizer.TagCategory.SCI_FI, TagNormalizer.normalize("science fiction"))
        assertEquals(TagNormalizer.TagCategory.CULTIVATION, TagNormalizer.normalize("eastern fantasy"))
        assertEquals(TagNormalizer.TagCategory.BL, TagNormalizer.normalize("yaoi"))
        assertEquals(TagNormalizer.TagCategory.BL, TagNormalizer.normalize("boys love"))
        assertEquals(TagNormalizer.TagCategory.GL, TagNormalizer.normalize("yuri"))
        assertEquals(TagNormalizer.TagCategory.ISEKAI, TagNormalizer.normalize("another world"))
    }

    @Test
    fun normalize_unknownTag_returnsNull() {
        assertNull(TagNormalizer.normalize("completely_unknown_xyz"))
        assertNull(TagNormalizer.normalize(""))
        assertNull(TagNormalizer.normalize("random gibberish"))
    }

    @Test
    fun normalizeAll_emptyList_returnsEmptySet() {
        assertTrue(TagNormalizer.normalizeAll(emptyList()).isEmpty())
    }

    @Test
    fun normalizeAll_mixedList_returnsOnlyKnown() {
        val result = TagNormalizer.normalizeAll(listOf("fantasy", "unknown_xyz", "romance"))
        assertEquals(2, result.size)
        assertTrue(result.contains(TagNormalizer.TagCategory.FANTASY))
        assertTrue(result.contains(TagNormalizer.TagCategory.ROMANCE))
    }

    @Test
    fun normalizeAll_deduplicatesAliases() {
        // "yaoi" and "boys love" both map to BL
        val result = TagNormalizer.normalizeAll(listOf("yaoi", "boys love", "bl"))
        assertEquals(1, result.size)
        assertTrue(result.contains(TagNormalizer.TagCategory.BL))
    }

    @Test
    fun calculateTagSimilarity_identicalSets_returns1() {
        val tags = setOf(TagNormalizer.TagCategory.FANTASY, TagNormalizer.TagCategory.ACTION)
        val score = TagNormalizer.calculateTagSimilarity(tags, tags)
        assertTrue("Expected ≥ 1.0 but got $score", score >= 1.0f)
    }

    @Test
    fun calculateTagSimilarity_disjointSets_returnsLow() {
        val tags1 = setOf(TagNormalizer.TagCategory.ROMANCE, TagNormalizer.TagCategory.FLUFFY)
        val tags2 = setOf(TagNormalizer.TagCategory.ACTION, TagNormalizer.TagCategory.MILITARY)
        val score = TagNormalizer.calculateTagSimilarity(tags1, tags2)
        assertTrue("Expected low score but got $score", score < 0.5f)
    }

    @Test
    fun calculateTagSimilarity_emptySets_returns0() {
        val score = TagNormalizer.calculateTagSimilarity(emptySet(), setOf(TagNormalizer.TagCategory.FANTASY))
        assertEquals(0f, score)
    }

    @Test
    fun calculateTagSimilarity_relatedTags_boostsScore() {
        // XIANXIA has CULTIVATION as a related tag
        val tags1 = setOf(TagNormalizer.TagCategory.XIANXIA)
        val tags2 = setOf(TagNormalizer.TagCategory.CULTIVATION)
        val score = TagNormalizer.calculateTagSimilarity(tags1, tags2)
        assertTrue("Related tags should boost score above 0, got $score", score > 0f)
    }

    @Test
    fun hasMatureContent_withMatureTag_returnsTrue() {
        val tags = setOf(TagNormalizer.TagCategory.MATURE, TagNormalizer.TagCategory.FANTASY)
        assertTrue(TagNormalizer.hasMatureContent(tags))
    }

    @Test
    fun hasMatureContent_withoutMatureTag_returnsFalse() {
        val tags = setOf(TagNormalizer.TagCategory.FANTASY, TagNormalizer.TagCategory.ACTION)
        assertFalse(TagNormalizer.hasMatureContent(tags))
    }

    @Test
    fun hasLGBTContent_withBLTag_returnsTrue() {
        val tags = setOf(TagNormalizer.TagCategory.BL, TagNormalizer.TagCategory.ROMANCE)
        assertTrue(TagNormalizer.hasLGBTContent(tags))
    }

    @Test
    fun hasLGBTContent_withoutLGBTTag_returnsFalse() {
        val tags = setOf(TagNormalizer.TagCategory.ROMANCE, TagNormalizer.TagCategory.FANTASY)
        assertFalse(TagNormalizer.hasLGBTContent(tags))
    }

    @Test
    fun getRelatedTags_knownCategory_returnsNonEmpty() {
        val related = TagNormalizer.getRelatedTags(TagNormalizer.TagCategory.XIANXIA)
        assertTrue(related.isNotEmpty())
        assertTrue(related.contains(TagNormalizer.TagCategory.CULTIVATION))
    }

    @Test
    fun getRelatedTags_unknownCategory_returnsEmpty() {
        // Categories with no related tags return empty
        val related = TagNormalizer.getRelatedTags(TagNormalizer.TagCategory.COOKING)
        assertTrue(related.isEmpty())
    }

    @Test
    fun getDisplayName_specialCases_returnsCorrect() {
        assertEquals("Boys Love (BL)", TagNormalizer.getDisplayName(TagNormalizer.TagCategory.BL))
        assertEquals("Girls Love (GL)", TagNormalizer.getDisplayName(TagNormalizer.TagCategory.GL))
        assertEquals("LGBTQ+", TagNormalizer.getDisplayName(TagNormalizer.TagCategory.LGBT))
        assertEquals("LitRPG", TagNormalizer.getDisplayName(TagNormalizer.TagCategory.LITRPG))
    }

    @Test
    fun getTagGroup_knownCategory_returnsGroup() {
        assertNotNull(TagNormalizer.getTagGroup(TagNormalizer.TagCategory.FANTASY))
        assertNotNull(TagNormalizer.getTagGroup(TagNormalizer.TagCategory.BL))
        assertNotNull(TagNormalizer.getTagGroup(TagNormalizer.TagCategory.XIANXIA))
    }

    @Test
    fun getAllTagsByGroup_returnsAllGroups() {
        val byGroup = TagNormalizer.getAllTagsByGroup()
        assertTrue(byGroup.isNotEmpty())
        assertTrue(byGroup.containsKey(TagNormalizer.TagGroup.MAIN_GENRES))
        assertTrue(byGroup.containsKey(TagNormalizer.TagGroup.LGBTQ))
    }
}
