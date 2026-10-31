package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.ai.OpenRouterService
import com.kmhmubin.kothagolp.data.local.entity.BlockedAuthorEntity
import com.kmhmubin.kothagolp.data.local.entity.HiddenNovelEntity
import com.kmhmubin.kothagolp.data.local.entity.HideReason
import com.kmhmubin.kothagolp.data.local.entity.TagFilterType
import com.kmhmubin.kothagolp.data.repository.LibraryItem
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.recommendation.RecommendationEngine
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.GenreOption
import com.kmhmubin.kothagolp.recommendation.model.LibrarySourceNovel
import com.kmhmubin.kothagolp.recommendation.model.NovelVector
import com.kmhmubin.kothagolp.recommendation.model.OnboardingGenres
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async

private const val TAG = "RecommendationViewModel"
private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

class RecommendationViewModel : ViewModel() {

    private val recommendationEngine = RepositoryProvider.getRecommendationEngine()
    private val userPreferenceManager = RepositoryProvider.getUserPreferenceManager()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val discoveryManager = RepositoryProvider.getDiscoveryManager()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val userFilterManager = RepositoryProvider.getUserFilterManager()
    private val tagEnhancementManager = RepositoryProvider.getTagEnhancementManager()
    private val authorPreferenceManager = RepositoryProvider.getAuthorPreferenceManager()
    private val historyRepository = RepositoryProvider.getHistoryRepository()

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private val _tagFilters = MutableStateFlow<Map<TagCategory, TagFilterType>>(emptyMap())
    val tagFilters: StateFlow<Map<TagCategory, TagFilterType>> = _tagFilters.asStateFlow()

    private val _hiddenNovels = MutableStateFlow<List<HiddenNovelEntity>>(emptyList())
    val hiddenNovels: StateFlow<List<HiddenNovelEntity>> = _hiddenNovels.asStateFlow()

    private val _blockedAuthors = MutableStateFlow<List<BlockedAuthorEntity>>(emptyList())
    val blockedAuthors: StateFlow<List<BlockedAuthorEntity>> = _blockedAuthors.asStateFlow()

    private val _favoriteAuthors = MutableStateFlow<List<String>>(emptyList())
    val favoriteAuthors: StateFlow<List<String>> = _favoriteAuthors.asStateFlow()

    // Track the last known disabled providers to detect changes
    private var lastKnownDisabledProviders: Set<String> = emptySet()

    // Cache for source-specific recommendations
    private val sourceRecommendationCache = mutableMapOf<String, List<Recommendation>>()

    init {
        // Observe tag filters
        viewModelScope.launch {
            userFilterManager.observeTagFilters().collect { filters ->
                _tagFilters.value = filters
            }
        }

        // Observe hidden novels
        viewModelScope.launch {
            userFilterManager.observeHiddenNovels().collect { novels ->
                _hiddenNovels.value = novels
            }
        }

        // Observe blocked authors
        viewModelScope.launch {
            userFilterManager.observeBlockedAuthors().collect { authors ->
                _blockedAuthors.value = authors
            }
        }

        // Observe app settings for disabled provider changes
        viewModelScope.launch {
            preferencesManager.appSettings
                .map { it.disabledProviders }
                .distinctUntilChanged()
                .collect { disabledProviders ->
                    val previousDisabled = lastKnownDisabledProviders
                    lastKnownDisabledProviders = disabledProviders

                    _uiState.update { it.copy(disabledProviderCount = disabledProviders.size) }

                    if (previousDisabled != disabledProviders && _uiState.value.hasRecommendations) {
                        Log.d(TAG, "Disabled providers changed, refreshing recommendations")
                        loadRecommendations(forceRefresh = true)
                    }
                }
        }

        // Observe library changes to update source novels
        viewModelScope.launch {
            libraryRepository.observeLibrary().collect { libraryItems ->
                updateLibrarySources(libraryItems)
            }
        }

        _uiState.update { it.copy(showLongPressHint = !preferencesManager.isForYouHintDismissed()) }
        checkAndSeed()
        checkGeminiKey()
    }

    // ================================================================
    // LIBRARY SOURCE MANAGEMENT (NEW)
    // ================================================================

    /**
     * Update the list of library novels that can be used as recommendation sources
     */
    private suspend fun updateLibrarySources(libraryItems: List<LibraryItem>) {
        // Get read chapter counts for each novel
        val sourceNovels = libraryItems
            .filter { it.lastReadPosition != null } // Only novels that have been read
            .map { item ->
                val chaptersRead = getChaptersReadCount(item.novel.url)
                LibrarySourceNovel.fromLibraryItem(item, chaptersRead)
            }
            .sortedByDescending { it.lastReadAt } // Most recently read first

        val currentSelected = _uiState.value.selectedSourceNovel
        val defaultSource = sourceNovels.firstOrNull()

        // If no source is selected, select the most recently read
        val newSelected = when {
            currentSelected != null && sourceNovels.any { it.novel.url == currentSelected.novel.url } -> {
                // Keep current selection if still valid
                sourceNovels.find { it.novel.url == currentSelected.novel.url }
            }
            else -> defaultSource
        }

        _uiState.update { state ->
            state.copy(
                librarySourceNovels = sourceNovels,
                selectedSourceNovel = newSelected
            )
        }

        // Load recommendations for the selected source if changed
        if (newSelected != null && currentSelected?.novel?.url != newSelected.novel.url) {
            loadRecommendationsForSource(newSelected.novel.url)
        }
    }

    /**
     * Get the number of chapters read for a novel
     */
    private suspend fun getChaptersReadCount(novelUrl: String): Int {
        return try {
            val dao = RepositoryProvider.getDatabase().historyDao()
            dao.getReadChapterCount(novelUrl)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Select a library novel as the source for recommendations
     */
    fun selectSourceNovel(sourceNovel: LibrarySourceNovel) {
        if (_uiState.value.selectedSourceNovel?.novel?.url == sourceNovel.novel.url) {
            return // Already selected
        }

        _uiState.update { it.copy(
            selectedSourceNovel = sourceNovel,
            isSourceSelectorExpanded = false
        )}

        viewModelScope.launch {
            loadRecommendationsForSource(sourceNovel.novel.url)
        }
    }

    /**
     * Toggle the source selector expansion
     */
    fun toggleSourceSelector() {
        _uiState.update { it.copy(isSourceSelectorExpanded = !it.isSourceSelectorExpanded) }
    }

    /**
     * Collapse the source selector
     */
    fun collapseSourceSelector() {
        _uiState.update { it.copy(isSourceSelectorExpanded = false) }
    }

    /**
     * Load recommendations for a specific source novel
     */
    private suspend fun loadRecommendationsForSource(novelUrl: String) {
        // Check cache first
        sourceRecommendationCache[novelUrl]?.let { cached ->
            _uiState.update { it.copy(sourceNovelRecommendations = cached) }
            return
        }

        _uiState.update { it.copy(isLoadingSourceRecommendations = true) }

        try {
            val disabledProviders = getDisabledProviders()

            suspend fun similar() = recommendationEngine.getSimilarTo(
                novelUrl = novelUrl,
                limit = 15,
                excludeLibrary = true,
                disabledProviders = disabledProviders
            )
            var recommendations = similar()
            // Restored/imported novels have no cached details to compare against: fetch once, retry.
            if (recommendations.isEmpty() &&
                RepositoryProvider.getDatabase().offlineDao().getNovelDetails(novelUrl) == null
            ) {
                runCatching { libraryRepository.fetchAndCacheNovelDetails(novelUrl) }
                recommendations = similar()
            }

            // Cache only real results so an empty one gets retried later
            if (recommendations.isNotEmpty()) sourceRecommendationCache[novelUrl] = recommendations

            _uiState.update { it.copy(
                sourceNovelRecommendations = recommendations,
                isLoadingSourceRecommendations = false
            )}

        } catch (e: Exception) {
            Log.e(TAG, "Error loading recommendations for source $novelUrl", e)
            _uiState.update { it.copy(
                sourceNovelRecommendations = emptyList(),
                isLoadingSourceRecommendations = false
            )}
        }
    }

    /**
     * Clear the recommendation cache (call when data changes significantly)
     */
    fun clearRecommendationCache() {
        sourceRecommendationCache.clear()
    }

    // ================================================================
    // INITIALIZATION & SEEDING
    // ================================================================

    private fun checkAndSeed() {
        viewModelScope.launch {
            try {
                val poolSize = discoveryManager.getPoolSize()
                _uiState.update { it.copy(poolSize = poolSize) }

                if (discoveryManager.needsSeeding()) {
                    seedDiscoveryPool()
                } else {
                    enhanceTagsIfNeeded()
                    loadUserProfile()
                    loadRecommendations()
                    loadLibrarySources()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndSeed", e)
                _uiState.update {
                    it.copy(isLoading = false, error = ForYouCopy.LOAD_ERROR)
                }
            }
        }
    }

    /**
     * Load library sources on init
     */
    private suspend fun loadLibrarySources() {
        val libraryItems = libraryRepository.getLibrary()
        updateLibrarySources(libraryItems)
    }

    private suspend fun seedDiscoveryPool() {
        _uiState.update { it.copy(isSeeding = true, isLoading = true) }

        try {
            val disabledProviders = getDisabledProviders()

            val result = discoveryManager.seedDiscoveryPool(
                disabledProviders = disabledProviders
            ) { provider, current, total ->
                _uiState.update {
                    it.copy(
                        seedingProgress = SeedingProgress(
                            currentProvider = provider,
                            currentIndex = current,
                            totalProviders = total
                        )
                    )
                }
            }

            Log.d(TAG, "Seeding complete: ${result.totalDiscovered} novels discovered")

            val newPoolSize = discoveryManager.getPoolSize()
            val poolByProvider = discoveryManager.getPoolSizeByProvider()

            _uiState.update {
                it.copy(
                    isSeeding = false,
                    seedingProgress = null,
                    poolSize = newPoolSize,
                    poolByProvider = poolByProvider
                )
            }

            val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
            Log.d(TAG, "Tag enhancement: ${enhancementResult.novelsEnhanced} novels, " +
                    "${enhancementResult.tagsAdded} tags added")

            logTagCoverageStats()

            loadUserProfile()
            loadRecommendations()
            loadLibrarySources()

        } catch (e: Exception) {
            Log.e(TAG, "Error seeding discovery pool", e)
            _uiState.update {
                it.copy(
                    isSeeding = false,
                    isLoading = false,
                    seedingProgress = null,
                    error = ForYouCopy.LOAD_ERROR
                )
            }
        }
    }

    private suspend fun enhanceTagsIfNeeded() {
        try {
            val stats = tagEnhancementManager.getTagCoverageStats()

            if (stats.tagCoveragePercent < 70f) {
                Log.d(TAG, "Tag coverage is ${stats.tagCoveragePercent.toInt()}%, running enhancement...")

                val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                Log.d(TAG, "Tag enhancement: ${enhancementResult.novelsEnhanced} novels, " +
                        "${enhancementResult.tagsAdded} tags added")
            } else {
                Log.d(TAG, "Tag coverage is ${stats.tagCoveragePercent.toInt()}%, no enhancement needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/running tag enhancement", e)
        }
    }

    private suspend fun logTagCoverageStats() {
        try {
            val stats = tagEnhancementManager.getTagCoverageStats()
            Log.d(TAG, """
                Tag Coverage Stats:
                - Total novels: ${stats.totalNovels}
                - With tags: ${stats.withTags} (${stats.tagCoveragePercent.toInt()}%)
                - With synopsis: ${stats.withSynopsis}
                - Top tags: ${stats.topTags.take(5).map { it.first.name }}
            """.trimIndent())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tag coverage stats", e)
        }
    }

    fun getTagCoverageStats() {
        viewModelScope.launch {
            logTagCoverageStats()
        }
    }

    fun forceTagEnhancement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val result = tagEnhancementManager.enhanceNovelsWithSynopsis(forceReprocess = true)
                Log.d(TAG, "Force tag enhancement: ${result.novelsEnhanced} novels, " +
                        "${result.tagsAdded} tags added")

                logTagCoverageStats()

                // Clear cache since data changed
                clearRecommendationCache()

                loadRecommendations(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error in force tag enhancement", e)
                _uiState.update {
                    it.copy(isRefreshing = false, error = ForYouCopy.LOAD_ERROR)
                }
            }
        }
    }

    // ================================================================
    // USER FILTERS (Tags, Hiding, Blocking)
    // ================================================================

    fun setTagFilter(tag: TagCategory, filterType: TagFilterType) {
        viewModelScope.launch {
            userFilterManager.setTagFilter(tag, filterType)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun hideNovel(novelUrl: String, novelName: String) {
        viewModelScope.launch {
            userFilterManager.hideNovel(novelUrl, novelName, HideReason.NOT_INTERESTED)
            removeNovelFromRecommendations(novelUrl)
        }
    }

    fun unhideNovel(novelUrl: String) {
        viewModelScope.launch {
            userFilterManager.unhideNovel(novelUrl)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun blockAuthor(authorNormalized: String, displayName: String) {
        viewModelScope.launch {
            userFilterManager.blockAuthor(authorNormalized, displayName)
            removeNovelsByAuthor(authorNormalized)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun unblockAuthor(authorNormalized: String) {
        viewModelScope.launch {
            userFilterManager.unblockAuthor(authorNormalized)
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllHiddenNovels() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllHiddenNovels()
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    fun clearAllBlockedAuthors() {
        viewModelScope.launch {
            val dao = RepositoryProvider.getDatabase().userFilterDao()
            dao.clearAllBlockedAuthors()
            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    suspend fun getAuthorForNovel(novelUrl: String): Pair<String, String>? {
        return try {
            val discovered = RepositoryProvider.getDatabase().recommendationDao()
                .getDiscoveredNovel(novelUrl)

            val discoveredAuthor = discovered?.author
            if (discoveredAuthor != null) {
                val normalized = NovelVector.normalizeAuthor(discoveredAuthor)
                if (normalized != null) {
                    return normalized to discoveredAuthor
                }
            }

            val details = RepositoryProvider.getDatabase().offlineDao()
                .getNovelDetails(novelUrl)

            val detailsAuthor = details?.author
            if (detailsAuthor != null) {
                val normalized = NovelVector.normalizeAuthor(detailsAuthor)
                if (normalized != null) {
                    return normalized to detailsAuthor
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun removeNovelFromRecommendations(novelUrl: String) {
        _uiState.update { state ->
            state.copy(
                recommendationGroups = state.recommendationGroups.map { group ->
                    group.copy(
                        recommendations = group.recommendations.filter {
                            it.novel.url != novelUrl
                        }
                    )
                }.filter { it.recommendations.isNotEmpty() },
                // Also remove from source recommendations
                sourceNovelRecommendations = state.sourceNovelRecommendations.filter {
                    it.novel.url != novelUrl
                }
            )
        }

        // Update cache
        sourceRecommendationCache.forEach { (key, recommendations) ->
            sourceRecommendationCache[key] = recommendations.filter { it.novel.url != novelUrl }
        }
    }

    private fun removeNovelsByAuthor(authorNormalized: String) {
        _uiState.update { state ->
            state.copy(
                recommendationGroups = state.recommendationGroups.map { group ->
                    group.copy(
                        recommendations = group.recommendations.filter { rec ->
                            true // TODO: Filter by author when we have author info in Recommendation
                        }
                    )
                }
            )
        }
    }

    // ================================================================
    // PROFILE LOADING
    // ================================================================

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = userPreferenceManager.getUserProfile()

                val topPrefs = profile.getTopPreferences(5).map { tag ->
                    TagNormalizer.getDisplayName(tag)
                }

                val favAuthors = authorPreferenceManager.getFavoriteAuthors(5)
                    .map { it.displayName }

                _uiState.update {
                    it.copy(
                        profileMaturity = profile.maturity,
                        topPreferences = topPrefs,
                        novelsInProfile = profile.sampleSize
                    )
                }

                _favoriteAuthors.value = favAuthors

            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
            }
        }
    }

    fun getAuthorStats() {
        viewModelScope.launch {
            try {
                val stats = authorPreferenceManager.getAuthorStats()
                Log.d(TAG, """
                    Author Stats:
                    - Total authors tracked: ${stats.totalAuthors}
                    - Favorites: ${stats.favoriteCount}
                    - Liked: ${stats.likedCount}
                    - Avg affinity: ${stats.averageAffinity}
                """.trimIndent())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting author stats", e)
            }
        }
    }

    // ================================================================
    // RECOMMENDATIONS LOADING
    // ================================================================

    fun loadRecommendations(forceRefresh: Boolean = false) {
        val state = _uiState.value

        if (state.isSeeding) return

        val now = System.currentTimeMillis()
        val cacheAge = now - state.lastRefreshTime
        val isCacheValid = cacheAge < CACHE_DURATION_MS && state.hasRecommendations

        if (isCacheValid && !forceRefresh) {
            _uiState.update { it.copy(isLoading = false, isCacheStale = false) }
            return
        }

        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val disabledProviders = getDisabledProviders()
                val preferredProvider = determinePreferredProvider()

                val config = RecommendationEngine.RecommendationConfig(
                    maxPerGroup = 12,
                    minScore = 0.3f,
                    includeCrossProvider = _uiState.value.showCrossProvider,
                    preferredProvider = preferredProvider,
                    disabledProviders = disabledProviders
                )

                val groups = recommendationEngine.generateRecommendations(config)
                val poolSize = discoveryManager.getPoolSize()

                _uiState.update {
                    it.copy(
                        recommendationGroups = groups,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        preferredProvider = preferredProvider,
                        lastRefreshTime = System.currentTimeMillis(),
                        isCacheStale = false,
                        poolSize = poolSize,
                        disabledProviderCount = disabledProviders.size
                    )
                }

                loadUserProfile()
                launchTasteDiscovery(config)

                // Also refresh source recommendations if we have a selected source
                _uiState.value.selectedSourceNovel?.let { source ->
                    loadRecommendationsForSource(source.novel.url)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommendations", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = ForYouCopy.LOAD_ERROR
                    )
                }
            }
        }
    }

    fun refresh() {
        clearRecommendationCache()
        loadRecommendations(forceRefresh = true)
    }

    fun refreshDiscoveryPool() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val disabledProviders = getDisabledProviders()
                val newCount = discoveryManager.refreshPool(disabledProviders = disabledProviders)
                Log.d(TAG, "Discovered $newCount new novels")

                if (newCount > 0) {
                    val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                    Log.d(TAG, "Enhanced ${enhancementResult.novelsEnhanced} new novels")
                }

                val poolSize = discoveryManager.getPoolSize()
                _uiState.update { it.copy(poolSize = poolSize) }

                clearRecommendationCache()
                loadRecommendations(forceRefresh = true)

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing discovery pool", e)
                _uiState.update {
                    it.copy(isRefreshing = false, error = ForYouCopy.LOAD_ERROR)
                }
            }
        }
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    fun toggleCrossProvider() {
        _uiState.update { it.copy(showCrossProvider = !it.showCrossProvider) }
        clearRecommendationCache()
        loadRecommendations(forceRefresh = true)
    }

    // ================================================================
    // GENRE PREFERENCES
    // ================================================================

    suspend fun getCurrentGenrePrefs(): Triple<Set<TagCategory>, Set<TagCategory>, Triple<Boolean, Boolean, Boolean>> {
        val boosted = userFilterManager.getBoostedTags()
        val genreSet = (OnboardingGenres.mainGenres + OnboardingGenres.subGenres).map { it.category }.toSet()
        val liked = boosted.filter { it in genreSet }.toSet()

        val allFilters = userFilterManager.getFilterState()
        val disliked = allFilters.blockedTags.filter { it in genreSet }.toSet()

        val matureBlocked = listOf(TagCategory.MATURE, TagCategory.ADULT, TagCategory.SMUT, TagCategory.GORE)
            .all { it in allFilters.blockedTags }
        val blBlocked = TagCategory.BL in allFilters.blockedTags
        val glBlocked = TagCategory.GL in allFilters.blockedTags

        return Triple(liked, disliked, Triple(!matureBlocked, !blBlocked, !glBlocked))
    }

    fun applyGenrePreferences(
        liked: Set<TagCategory>,
        disliked: Set<TagCategory>,
        includeMature: Boolean,
        includeBL: Boolean,
        includeGL: Boolean
    ) {
        viewModelScope.launch {
            val allGenreCategories = (OnboardingGenres.mainGenres + OnboardingGenres.subGenres)
                .map { it.category }
            allGenreCategories.forEach { userFilterManager.clearTagFilter(it) }
            listOf(TagCategory.MATURE, TagCategory.ADULT, TagCategory.SMUT, TagCategory.GORE,
                TagCategory.BL, TagCategory.GL).forEach { userFilterManager.clearTagFilter(it) }

            liked.forEach { userFilterManager.setTagFilter(it, TagFilterType.BOOSTED) }
            disliked.forEach { userFilterManager.setTagFilter(it, TagFilterType.BLOCKED) }

            if (!includeMature) {
                listOf(TagCategory.MATURE, TagCategory.ADULT, TagCategory.SMUT, TagCategory.GORE)
                    .forEach { userFilterManager.setTagFilter(it, TagFilterType.BLOCKED) }
            }
            if (!includeBL) userFilterManager.setTagFilter(TagCategory.BL, TagFilterType.BLOCKED)
            if (!includeGL) userFilterManager.setTagFilter(TagCategory.GL, TagFilterType.BLOCKED)

            clearRecommendationCache()
            loadRecommendations(forceRefresh = true)
        }
    }

    // ================================================================
    // TAG-CURATED SECTION
    // ================================================================

    fun selectTagForSection(tag: TagCategory?) {
        if (tag == null) {
            _uiState.update { it.copy(selectedTagCategory = null, novelsForSelectedTag = emptyList()) }
            return
        }
        if (_uiState.value.selectedTagCategory == tag) {
            // Deselect
            _uiState.update { it.copy(selectedTagCategory = null, novelsForSelectedTag = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(selectedTagCategory = tag, isLoadingTagNovels = true, novelsForSelectedTag = emptyList()) }
            try {
                val novels = discoveryManager.getNovelsForTag(tag, limit = 12)
                _uiState.update { it.copy(novelsForSelectedTag = novels, isLoadingTagNovels = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingTagNovels = false) }
            }
        }
    }

    // ================================================================
    // AI RECOMMENDATIONS (OpenRouter)
    // ================================================================

    private val openRouterService = OpenRouterService()

    private fun aiConfig() = RecommendationEngine.RecommendationConfig(disabledProviders = getDisabledProviders())

    /**
     * "AI Picks": the engine hands the model its 40 best real candidates plus a taste
     * summary; the model chooses and explains. Every card opens a novel on a source.
     */
    fun loadAiRecommendations(force: Boolean = false) {
        val apiKey = preferencesManager.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(hasGeminiKey = false) }
            return
        }
        val state = _uiState.value
        if (state.isLoadingAiRecs) return
        val model = preferencesManager.getSelectedAiModel()
        val age = System.currentTimeMillis() - state.aiRecsLastUpdated
        if (!force && model == state.lastLoadedAiModel) {
            // 30-min cache for results, 60-s cooldown after an error
            if (state.aiRecommendations.isNotEmpty() && age < 30 * 60_000L) return
            if (state.aiRecsLastUpdated > 0 && age < 60_000L) return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAiRecs = true, aiRecsError = null, hasGeminiKey = true) }
            val result = runCatching {
                val brief = recommendationEngine.aiBrief(aiConfig())
                if (brief == null || brief.liked.isEmpty() || brief.candidates.size < 8) return@runCatching null
                val byId = brief.candidates.associateBy { it.id }
                openRouterService.rerank(apiKey, brief, model).getOrThrow().mapNotNull { pick ->
                    val c = byId[pick.id] ?: return@mapNotNull null
                    val novel = c.recommendation.novel
                    AiRecommendedNovel(
                        title = novel.name, reason = pick.reason, genres = c.tags.take(3),
                        matchedNovelUrl = novel.url, matchedPosterUrl = novel.posterUrl, matchedApiName = novel.apiName
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isLoadingAiRecs = false,
                    hasReadingHistory = result.getOrNull() != null || result.isFailure,
                    aiRecommendations = result.getOrNull() ?: it.aiRecommendations,
                    aiRecsError = result.exceptionOrNull()?.let { e -> e.message ?: ForYouCopy.AI_UNREADABLE },
                    aiRecsLastUpdated = System.currentTimeMillis(),
                    lastLoadedAiModel = model
                )
            }
        }
    }

    fun saveGeminiApiKey(key: String) {
        preferencesManager.setGeminiApiKey(key)
        _uiState.update { it.copy(hasGeminiKey = key.isNotBlank(), aiRecommendations = emptyList(), aiRecsLastUpdated = 0L) }
        if (key.isNotBlank()) loadAiRecommendations()
    }

    fun checkGeminiKey() {
        val key = preferencesManager.getGeminiApiKey()
        _uiState.update { it.copy(hasGeminiKey = !key.isNullOrBlank()) }
    }

    fun clearAiRecsError() {
        _uiState.update { it.copy(aiRecsError = null) }
    }

    /**
     * "AI Discovery": the model suggests well-known novels outside the pool from its own
     * knowledge; each title is verified on the user's sources and unverified ones dropped.
     */
    fun loadAiTrendingRecommendations(force: Boolean = false) {
        val apiKey = preferencesManager.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(hasGeminiKey = false) }
            return
        }
        val state = _uiState.value
        if (state.isLoadingTrending) return
        val model = preferencesManager.getSelectedAiModel()
        val age = System.currentTimeMillis() - state.trendingLastUpdated
        if (!force && model == state.lastLoadedTrendingModel) {
            if (state.aiTrendingRecommendations.isNotEmpty() && age < 30 * 60_000L) return
            if (state.trendingLastUpdated > 0 && age < 60_000L) return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTrending = true, trendingError = null, hasGeminiKey = true) }
            val result = runCatching {
                val brief = recommendationEngine.aiBrief(aiConfig()) ?: error(ForYouCopy.AI_NEED_BROWSE)
                val library = libraryRepository.getLibrary()
                val known = library.map { it.novel.name } + brief.candidates.map { it.recommendation.novel.name }
                val suggestions = openRouterService.suggestTitles(apiKey, brief, known, model).getOrThrow()
                verifyOnSources(suggestions, library.map { normalizeTitle(it.novel.name) }.toSet())
            }
            _uiState.update {
                it.copy(
                    isLoadingTrending = false,
                    aiTrendingRecommendations = result.getOrNull() ?: it.aiTrendingRecommendations,
                    trendingError = result.exceptionOrNull()?.let { e -> e.message ?: ForYouCopy.AI_UNREADABLE },
                    trendingLastUpdated = System.currentTimeMillis(),
                    lastLoadedTrendingModel = model
                )
            }
        }
    }

    /** Find each suggestion in the pool, else search the user's most-used sources. */
    private suspend fun verifyOnSources(
        suggestions: List<OpenRouterService.Suggestion>,
        libraryTitles: Set<String>
    ): List<AiRecommendedNovel> = coroutineScope {
        val pool = RepositoryProvider.getDatabase().recommendationDao().getAllDiscoveredNovels()
            .associateBy { normalizeTitle(it.name) }
        val disabled = getDisabledProviders()
        val novelRepository = RepositoryProvider.getNovelRepository()
        val providers = libraryRepository.getLibrary()
            .groupingBy { it.novel.apiName }.eachCount().entries
            .sortedByDescending { it.value }
            .mapNotNull { novelRepository.getProvider(it.key) }
            .filter { it.name !in disabled }
            .take(3)
        val gate = Semaphore(4)
        suggestions.filter { normalizeTitle(it.title) !in libraryTitles }.map { s ->
            async {
                gate.withPermit {
                    val key = normalizeTitle(s.title)
                    val found = pool[key]?.toNovel() ?: providers.firstNotNullOfOrNull { provider ->
                        withTimeoutOrNull(15_000L) {
                            novelRepository.searchInProvider(provider, s.title).getOrNull()
                                ?.firstOrNull { titlesMatch(key, normalizeTitle(it.name)) }
                                ?.let { it.copy(apiName = it.apiName.ifBlank { provider.name }) }
                        }
                    } ?: return@withPermit null
                    AiRecommendedNovel(
                        title = found.name, author = s.author, reason = s.reason,
                        matchedNovelUrl = found.url, matchedPosterUrl = found.posterUrl, matchedApiName = found.apiName
                    )
                }
            }
        }.awaitAll().filterNotNull().distinctBy { it.matchedNovelUrl }
    }

    private fun normalizeTitle(title: String) =
        title.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Exact, or one title contains the other (subtitle/translation variants), never on short fragments. */
    private fun titlesMatch(a: String, b: String) =
        a == b || (minOf(a.length, b.length) >= 8 && (a.contains(b) || b.contains(a)))

    // ================================================================
    // HELPERS
    // ================================================================

    private var tasteDiscoveryJob: Job? = null

    /**
     * Background: fetch more novels matching the user's top tags plus the sources'
     * related lists, then quietly re-rank. Throttled inside DiscoveryManager.
     */
    private fun launchTasteDiscovery(config: RecommendationEngine.RecommendationConfig) {
        if (tasteDiscoveryJob?.isActive == true) return
        tasteDiscoveryJob = viewModelScope.launch {
            try {
                // Restored/imported libraries have no cached details, so the engine can't
                // see their tags yet: fill in a batch first (capped, recent reads first).
                val backfilled = RepositoryProvider.getMetadataBackfillManager().backfillMissing()
                val topTags = recommendationEngine.topTasteTags()
                val seeds = libraryRepository.getLibrary()
                    .sortedByDescending { it.lastReadPosition?.timestamp ?: it.addedAt }
                    .map { it.novel }
                if (topTags.isEmpty() && seeds.isEmpty()) return@launch
                val added = discoveryManager.discoverForTaste(topTags, seeds, config.disabledProviders)
                if (added <= 0 && backfilled <= 0 && seeds.isEmpty()) return@launch
                val groups = recommendationEngine.generateRecommendations(config)
                if (groups.isNotEmpty()) {
                    _uiState.update {
                        it.copy(recommendationGroups = groups, poolSize = discoveryManager.getPoolSize())
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Taste discovery failed", e)
            }
        }
    }

    private fun getDisabledProviders(): Set<String> {
        return preferencesManager.appSettings.value.disabledProviders
    }

    private suspend fun determinePreferredProvider(): String? {
        return try {
            val disabledProviders = getDisabledProviders()
            val library = libraryRepository.getLibrary()
            library
                .filter { it.novel.apiName !in disabledProviders }
                .groupingBy { it.novel.apiName }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        } catch (e: Exception) {
            null
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissLongPressHint() {
        preferencesManager.setForYouHintDismissed()
        _uiState.update { it.copy(showLongPressHint = false) }
    }

    fun addToLibrary(rec: Recommendation) {
        viewModelScope.launch {
            libraryRepository.addToLibrary(rec.novel)
            _uiState.update { it.copy(message = ForYouCopy.ADDED_TO_LIBRARY) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onRecommendationClicked(novelUrl: String) {
        // Could track which recommendations are clicked for analytics
    }
}