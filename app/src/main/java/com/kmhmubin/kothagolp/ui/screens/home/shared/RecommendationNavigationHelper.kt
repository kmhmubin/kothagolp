package com.kmhmubin.kothagolp.ui.screens.home.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kmhmubin.kothagolp.recommendation.TagNormalizer

/**
 * Helper to coordinate navigation to recommendations with tag filters
 */
object RecommendationNavigationHelper {
    var pendingTagFilter by mutableStateOf<TagNormalizer.TagCategory?>(null)
        private set

    var shouldNavigateToForYou by mutableStateOf(false)
        private set

    fun navigateWithTag(tag: TagNormalizer.TagCategory) {
        pendingTagFilter = tag
        shouldNavigateToForYou = true
    }

    fun consumePendingTag(): TagNormalizer.TagCategory? {
        val tag = pendingTagFilter
        pendingTagFilter = null
        return tag
    }

    fun consumeNavigationRequest(): Boolean {
        val should = shouldNavigateToForYou
        shouldNavigateToForYou = false
        return should
    }

    fun clear() {
        pendingTagFilter = null
        shouldNavigateToForYou = false
    }
}