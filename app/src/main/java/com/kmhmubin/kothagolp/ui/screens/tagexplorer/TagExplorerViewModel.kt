package com.kmhmubin.kothagolp.ui.screens.tagexplorer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.DisplayMode
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TagExplorerViewModel"

class TagExplorerViewModel : ViewModel() {

    private val recommendationDao = RepositoryProvider.getDatabase().recommendationDao()
    private val offlineDao = RepositoryProvider.getDatabase().offlineDao()

    private val _uiState = MutableStateFlow(TagExplorerUiState())
    val uiState: StateFlow<TagExplorerUiState> = _uiState.asStateFlow()

    // Cache for tag novel counts
    private val _tagNovelsCount = MutableStateFlow<Map<TagNormalizer.TagCategory, Int>>(emptyMap())
    val tagNovelsCount: StateFlow<Map<TagNormalizer.TagCategory, Int>> = _tagNovelsCount.asStateFlow()

    init {
        loadTagCounts()
    }

    private fun loadTagCounts() {
        viewModelScope.launch {
            try {
                val counts = mutableMapOf<TagNormalizer.TagCategory, Int>()

                // Count from NovelDetailsEntity
                val detailsNovels = offlineDao.getAllNovelDetails()
                detailsNovels.forEach { entity ->
                    val tags = entity.tags?.let { TagNormalizer.normalizeAll(it) } ?: emptySet()
                    tags.forEach { tag ->
                        counts[tag] = (counts[tag] ?: 0) + 1
                    }
                }

                // Count from DiscoveredNovelEntity (only if not already counted)
                val discoveredNovels = recommendationDao.getAllDiscoveredNovels()
                val seenUrls = detailsNovels.map { it.url }.toSet()

                discoveredNovels.forEach { entity ->
                    if (entity.url !in seenUrls) {
                        val tags = TagNormalizer.normalizeAll(entity.tags)
                        tags.forEach { tag ->
                            counts[tag] = (counts[tag] ?: 0) + 1
                        }
                    }
                }

                _tagNovelsCount.value = counts
                Log.d(TAG, "Loaded counts for ${counts.size} tags")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tag counts", e)
            }
        }
    }

    fun loadNovelsForTag(tagCategory: TagNormalizer.TagCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                tag = tagCategory,
                isLoading = true,
                error = null
            )}

            try {
                val novels = mutableListOf<Novel>()
                val seenUrls = mutableSetOf<String>()

                // Load from NovelDetailsEntity first (higher quality data)
                offlineDao.getAllNovelDetails().forEach { entity ->
                    val entityTags = entity.tags?.let { TagNormalizer.normalizeAll(it) } ?: emptySet()

                    if (tagCategory in entityTags && entity.url !in seenUrls) {
                        novels.add(Novel(
                            name = entity.name,
                            url = entity.url,
                            posterUrl = entity.posterUrl,
                            rating = entity.rating,
                            apiName = entity.apiName.ifBlank { "Unknown" }
                        ))
                        seenUrls.add(entity.url)
                    }
                }

                // Load from DiscoveredNovelEntity
                recommendationDao.getAllDiscoveredNovels().forEach { entity ->
                    val entityTags = TagNormalizer.normalizeAll(entity.tags)

                    if (tagCategory in entityTags && entity.url !in seenUrls) {
                        novels.add(Novel(
                            name = entity.name,
                            url = entity.url,
                            posterUrl = entity.posterUrl,
                            rating = entity.rating,
                            apiName = entity.apiName
                        ))
                        seenUrls.add(entity.url)
                    }
                }

                val providers = novels.map { it.apiName }.distinct().sorted()

                _uiState.update { it.copy(
                    novels = novels,
                    availableProviders = providers,
                    isLoading = false,
                    error = if (novels.isEmpty()) "No novels found for this tag" else null
                )}

                Log.d(TAG, "Loaded ${novels.size} novels for tag: ${TagNormalizer.getDisplayName(tagCategory)}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading novels for tag", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load novels: ${e.message}"
                )}
            }
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortBy = option) }
    }

    fun setMinRating(rating: Float) {
        _uiState.update { it.copy(minRating = rating) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleProvider(provider: String) {
        _uiState.update { state ->
            val newProviders = if (provider in state.selectedProviders) {
                state.selectedProviders - provider
            } else {
                state.selectedProviders + provider
            }
            state.copy(selectedProviders = newProviders)
        }
    }

    fun clearProviderFilter() {
        _uiState.update { it.copy(selectedProviders = emptySet()) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(
            searchQuery = "",
            minRating = 0f,
            selectedProviders = emptySet()
        )}
    }

    fun setDisplayMode(mode: DisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
    }

    fun setDensity(density: UiDensity) {
        _uiState.update { it.copy(density = density) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshTagCounts() {
        loadTagCounts()
    }
}