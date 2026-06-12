package com.kmhmubin.kothagolp.ui.screens.tagexplorer

import com.kmhmubin.kothagolp.domain.model.DisplayMode
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.recommendation.TagNormalizer

data class TagExplorerUiState(
    val tag: TagNormalizer.TagCategory? = null,
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Filter & Sort
    val sortBy: SortOption = SortOption.RATING,
    val minRating: Float = 0f,
    val searchQuery: String = "",

    // Display
    val displayMode: DisplayMode = DisplayMode.GRID,
    val density: UiDensity = UiDensity.DEFAULT,

    // Provider filter
    val selectedProviders: Set<String> = emptySet(), // empty = all providers
    val availableProviders: List<String> = emptyList()
) {
    val filteredNovels: List<Novel>
        get() = novels
            .filter { novel ->
                // Search query
                if (searchQuery.isNotBlank()) {
                    novel.name.contains(searchQuery, ignoreCase = true)
                } else true
            }
            .filter { novel ->
                // Minimum rating
                (novel.rating ?: 0) >= (minRating * 200).toInt()
            }
            .filter { novel ->
                // Provider filter
                if (selectedProviders.isEmpty()) {
                    true
                } else {
                    novel.apiName in selectedProviders
                }
            }
            .sortedWith(getSortComparator())

    private fun getSortComparator(): Comparator<Novel> {
        return when (sortBy) {
            SortOption.RATING -> compareByDescending { it.rating ?: 0 }
            SortOption.NAME -> compareBy { it.name }
        }
    }

    val tagDisplayName: String
        get() = tag?.let { TagNormalizer.getDisplayName(it) } ?: "Unknown Tag"

    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
                minRating > 0f ||
                selectedProviders.isNotEmpty()

    // ADD THIS NEW PROPERTY
    val activeFilterCount: Int
        get() {
            var count = 0
            if (sortBy != SortOption.NAME) count++
            if (minRating > 0f) count++
            if (selectedProviders.isNotEmpty()) count++
            return count
        }
}

enum class SortOption(val displayName: String) {
    RATING("Rating (High to Low)"),
    NAME("Name (A-Z)")
}