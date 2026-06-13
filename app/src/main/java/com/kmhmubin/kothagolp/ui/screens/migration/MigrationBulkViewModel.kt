package com.kmhmubin.kothagolp.ui.screens.migration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.data.migration.MigrationUseCase
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.ui.navigation.NavRoutes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class BulkSearchState {
    object Idle : BulkSearchState()
    object Searching : BulkSearchState()
    data class Found(val match: Novel) : BulkSearchState()
    object NotFound : BulkSearchState()
    data class Error(val message: String) : BulkSearchState()
}

data class BulkMigrationItem(
    val fromEntry: LibraryEntity,
    val searchState: BulkSearchState = BulkSearchState.Idle,
    val selected: Boolean = true
)

sealed class BulkMigrationProgress {
    object Idle : BulkMigrationProgress()
    data class Running(val done: Int, val total: Int) : BulkMigrationProgress()
    data class Done(val succeeded: Int, val failed: Int, val skipped: Int) : BulkMigrationProgress()
}

data class MigrationBulkUiState(
    val sourceName: String = "",
    val novels: List<BulkMigrationItem> = emptyList(),
    val isLoadingNovels: Boolean = true,
    val availableSources: List<String> = emptyList(),
    val targetSource: String? = null,
    val isSearching: Boolean = false,
    val progress: BulkMigrationProgress = BulkMigrationProgress.Idle,
    val showConfirmDialog: Boolean = false
) {
    val selectedCount get() = novels.count { it.selected && it.searchState is BulkSearchState.Found }
    val foundCount get() = novels.count { it.searchState is BulkSearchState.Found }
    val notFoundCount get() = novels.count { it.searchState is BulkSearchState.NotFound }
    val searchingCount get() = novels.count { it.searchState is BulkSearchState.Searching }
}

class MigrationBulkViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val db = RepositoryProvider.getDatabase()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val migrationUseCase = MigrationUseCase(db)

    val sourceName: String = NavRoutes.decodeUrl(
        savedStateHandle.get<String>("sourceName") ?: ""
    )

    private val _uiState = MutableStateFlow(MigrationBulkUiState(sourceName = sourceName))
    val uiState: StateFlow<MigrationBulkUiState> = _uiState.asStateFlow()

    init {
        loadNovels()
    }

    private fun loadNovels() {
        viewModelScope.launch {
            val novels = db.libraryDao().getBySourceName(sourceName)
            val sources = novelRepository.getProviders()
                .map { it.name }
                .filter { it != sourceName }
                .sorted()
            _uiState.update {
                it.copy(
                    novels = novels.map { entry -> BulkMigrationItem(entry) },
                    isLoadingNovels = false,
                    availableSources = sources
                )
            }
        }
    }

    fun setTargetSource(targetName: String) {
        _uiState.update { it.copy(targetSource = targetName, isSearching = true) }
        searchAll(targetName)
    }

    private fun searchAll(targetName: String) {
        viewModelScope.launch {
            val provider = novelRepository.getProvider(targetName) ?: run {
                _uiState.update { it.copy(isSearching = false) }
                return@launch
            }

            val novels = _uiState.value.novels

            // Mark all as searching
            _uiState.update { state ->
                state.copy(novels = state.novels.map { it.copy(searchState = BulkSearchState.Searching) })
            }

            // Search all in parallel (capped concurrency via async)
            val jobs = novels.map { item ->
                async {
                    val result = try {
                        val results = provider.search(item.fromEntry.name)
                        val best = results.firstOrNull()
                        if (best != null) BulkSearchState.Found(best)
                        else BulkSearchState.NotFound
                    } catch (e: Exception) {
                        BulkSearchState.Error(e.message ?: "Search failed")
                    }
                    item.fromEntry.url to result
                }
            }

            // Collect results as they come in
            jobs.forEach { deferred ->
                val (url, state) = deferred.await()
                _uiState.update { uiState ->
                    uiState.copy(
                        novels = uiState.novels.map { item ->
                            if (item.fromEntry.url == url) item.copy(searchState = state)
                            else item
                        }
                    )
                }
            }

            _uiState.update { it.copy(isSearching = false) }
        }
    }

    fun toggleSelection(novelUrl: String) {
        _uiState.update { state ->
            state.copy(novels = state.novels.map { item ->
                if (item.fromEntry.url == novelUrl) item.copy(selected = !item.selected)
                else item
            })
        }
    }

    fun overrideMatch(novelUrl: String, newMatch: Novel) {
        _uiState.update { state ->
            state.copy(novels = state.novels.map { item ->
                if (item.fromEntry.url == novelUrl) item.copy(searchState = BulkSearchState.Found(newMatch))
                else item
            })
        }
    }

    fun showConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun startBulkMigration() {
        val targetName = _uiState.value.targetSource ?: return
        val toMigrate = _uiState.value.novels.filter {
            it.selected && it.searchState is BulkSearchState.Found
        }

        _uiState.update { it.copy(showConfirmDialog = false, progress = BulkMigrationProgress.Running(0, toMigrate.size)) }

        viewModelScope.launch {
            val provider = novelRepository.getProvider(targetName)
            var succeeded = 0
            var failed = 0

            toMigrate.forEachIndexed { index, item ->
                val targetNovel = (item.searchState as BulkSearchState.Found).match
                val chapters = try {
                    provider?.load(targetNovel.url)?.chapters ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val result = migrationUseCase.migrate(item.fromEntry, targetNovel, chapters)
                if (result is MigrationUseCase.Result.Success) succeeded++ else failed++

                _uiState.update { it.copy(progress = BulkMigrationProgress.Running(index + 1, toMigrate.size)) }
            }

            val skipped = toMigrate.size - succeeded - failed
            _uiState.update { it.copy(progress = BulkMigrationProgress.Done(succeeded, failed, skipped)) }
        }
    }
}
