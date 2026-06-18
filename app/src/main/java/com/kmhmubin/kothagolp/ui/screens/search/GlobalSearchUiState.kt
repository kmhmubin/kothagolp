package com.kmhmubin.kothagolp.ui.screens.search

import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.ProviderSearchState
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.SearchFilters
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.SearchSortOrder

data class GlobalSearchUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val providerSearchStates: Map<String, ProviderSearchState> = emptyMap(),
    val filters: SearchFilters = SearchFilters(),
    val showFilters: Boolean = false,
    val hideEmptyProviders: Boolean = false,
    val libraryUrls: Set<String> = emptySet(),
    val quickSaveMessage: String? = null
) {
    val totalResults: Int
        get() = providerSearchStates.values.sumOf { state ->
            when (state) {
                is ProviderSearchState.Success -> state.novels.size
                else -> 0
            }
        }

    val providersWithResults: Int
        get() = providerSearchStates.count { (_, state) ->
            state is ProviderSearchState.Success && state.novels.isNotEmpty()
        }

    val totalProviders: Int
        get() = providerSearchStates.size

    val completedProviders: Int
        get() = providerSearchStates.count { it.value !is ProviderSearchState.Loading }

    val isSearchEmpty: Boolean
        get() = hasSearched && !isSearching && providerSearchStates.isNotEmpty() &&
                providerSearchStates.values.all { state ->
                    state is ProviderSearchState.Success && state.novels.isEmpty() ||
                            state is ProviderSearchState.Error
                }

    val filteredProviderStates: Map<String, ProviderSearchState>
        get() {
            var states = if (filters.selectedProviders.isEmpty()) {
                providerSearchStates
            } else {
                providerSearchStates.filterKeys { it in filters.selectedProviders }
            }
            if (hideEmptyProviders) {
                states = states.filter { (_, state) ->
                    state is ProviderSearchState.Loading ||
                            (state is ProviderSearchState.Success && state.novels.isNotEmpty())
                }
            }
            return states
        }

    val hasActiveFilters: Boolean
        get() = filters.selectedProviders.isNotEmpty() ||
                filters.sortOrder != SearchSortOrder.RELEVANCE ||
                hideEmptyProviders
}
