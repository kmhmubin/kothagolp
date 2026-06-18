package com.kmhmubin.kothagolp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.ui.screens.home.shared.ActionSheetManager
import com.kmhmubin.kothagolp.ui.screens.home.shared.ActionSheetSource
import com.kmhmubin.kothagolp.ui.screens.home.shared.ActionSheetState
import com.kmhmubin.kothagolp.ui.screens.home.shared.LibraryStateHolder
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.ProviderSearchState
import com.kmhmubin.kothagolp.ui.screens.home.tabs.browse.SearchFilters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GlobalSearchViewModel(
    private val initialQuery: String = ""
) : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _uiState = MutableStateFlow(GlobalSearchUiState(searchQuery = initialQuery))
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private val actionSheetManager = ActionSheetManager()
    val actionSheetState: StateFlow<ActionSheetState> = actionSheetManager.state

    private var currentSearchJob: Job? = null

    init {
        viewModelScope.launch {
            libraryRepository.observeLibraryUrls().collect { urls ->
                _uiState.update { it.copy(libraryUrls = urls) }
            }
        }

        if (initialQuery.isNotBlank()) {
            search(initialQuery)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search(query: String = _uiState.value.searchQuery.trim()) {
        if (query.isBlank()) return

        currentSearchJob?.cancel()

        currentSearchJob = viewModelScope.launch {
            val allProviders = try {
                novelRepository.getProviders().map { it.name }
            } catch (e: Exception) {
                emptyList()
            }

            val initialStates = allProviders.associateWith { ProviderSearchState.Loading }

            _uiState.update {
                it.copy(
                    searchQuery = query,
                    isSearching = true,
                    providerSearchStates = initialStates,
                    hasSearched = true
                )
            }

            var totalResults = 0

            try {
                novelRepository.searchAllStreaming(query).collect { (providerName, result) ->
                    _uiState.update { state ->
                        val newStates = state.providerSearchStates.toMutableMap()

                        result.fold(
                            onSuccess = { novels ->
                                newStates[providerName] = ProviderSearchState.Success(novels)
                                totalResults += novels.size
                            },
                            onFailure = { error ->
                                newStates[providerName] = ProviderSearchState.Error(
                                    error.message ?: "Search failed"
                                )
                            }
                        )

                        val allCompleted = newStates.values.none { it is ProviderSearchState.Loading }
                        state.copy(
                            providerSearchStates = newStates,
                            isSearching = !allCompleted
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }

            preferencesManager.addSearchHistoryItem(
                query = query,
                providerName = null,
                resultCount = totalResults
            )
        }
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                providerSearchStates = emptyMap(),
                hasSearched = false,
                isSearching = false
            )
        }
    }

    fun toggleHideEmptyProviders() {
        _uiState.update { it.copy(hideEmptyProviders = !it.hideEmptyProviders) }
    }

    fun updateFilters(filters: SearchFilters) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun toggleFiltersPanel() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = SearchFilters(), showFilters = false, hideEmptyProviders = false) }
    }

    fun quickSaveToLibrary(novel: Novel, status: ReadingStatus) {
        viewModelScope.launch {
            actionSheetManager.addToLibrary(novel, status)
            _uiState.update { it.copy(quickSaveMessage = "Saved to ${status.displayName()}") }
            delay(2000L)
            _uiState.update { it.copy(quickSaveMessage = null) }
        }
    }

    fun showActionSheet(novel: Novel) {
        val libraryItem = LibraryStateHolder.getLibraryItem(novel.url)
        actionSheetManager.show(
            novel = novel,
            source = ActionSheetSource.BROWSE,
            libraryItem = libraryItem
        )
    }

    fun hideActionSheet() = actionSheetManager.hide()

    fun updateReadingStatus(status: ReadingStatus) {
        actionSheetManager.updateReadingStatus(status)
    }

    fun addToLibrary(novel: Novel, status: ReadingStatus) {
        viewModelScope.launch { actionSheetManager.addToLibrary(novel, status) }
    }

    fun addDuplicateAnyway() {
        viewModelScope.launch { actionSheetManager.addDuplicateAnyway() }
    }

    fun dismissDuplicateWarning() {
        actionSheetManager.dismissDuplicateWarning()
    }

    fun removeFromLibrary(novelUrl: String) {
        viewModelScope.launch { actionSheetManager.removeFromLibrary(novelUrl) }
    }

    suspend fun getContinueReadingChapter(novelUrl: String) =
        actionSheetManager.getContinueReadingChapter(novelUrl)

    override fun onCleared() {
        super.onCleared()
        currentSearchJob?.cancel()
    }

    class Factory(private val initialQuery: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalSearchViewModel(initialQuery) as T
        }
    }
}
