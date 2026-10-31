package com.kmhmubin.kothagolp.recommendation

import com.kmhmubin.kothagolp.recommendation.RecommendationEngine.RecommendationConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationConfigTest {
    @Test fun allSourcesOnAllowsEveryEnabledSource() {
        val c = RecommendationConfig(preferredProvider = "A", disabledProviders = setOf("C"))
        assertTrue(c.allowsProvider("A"))
        assertTrue(c.allowsProvider("B"))
        assertFalse(c.allowsProvider("C"))
    }

    @Test fun allSourcesOffKeepsOnlyPreferred() {
        val c = RecommendationConfig(includeCrossProvider = false, preferredProvider = "A")
        assertTrue(c.allowsProvider("A"))
        assertFalse(c.allowsProvider("B"))
    }

    @Test fun allSourcesOffWithoutPreferredKeepsAll() =
        assertTrue(RecommendationConfig(includeCrossProvider = false).allowsProvider("B"))
}
