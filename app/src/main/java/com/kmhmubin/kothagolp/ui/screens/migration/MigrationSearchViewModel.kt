package com.kmhmubin.kothagolp.ui.screens.migration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.data.migration.MigrationUseCase
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderSearchResult(
    val providerName: String,
    val novels: List<Novel>? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class MigrationResult {
    data class Success(val transferredChapters: Int) : MigrationResult()
    data class Error(val message: String) : MigrationResult()
}

data class MigrationSearchUiState(
    val sourceEntry: LibraryEntity? = null,
    val sourceLoading: Boolean = true,
    val providerResults: List<ProviderSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedNovel: Novel? = null,
    val showConfirmDialog: Boolean = false,
    val isMigrating: Boolean = false,
    val migrationResult: MigrationResult? = null
)

class MigrationSearchViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val db = RepositoryProvider.getDatabase()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val migrationUseCase = MigrationUseCase(db)

    val novelUrl: String = NavRoutes.decodeUrl(
        savedStateHandle.get<String>("novelUrl") ?: ""
    )
    val fromSourceName: String = NavRoutes.decodeUrl(
        savedStateHandle.get<String>("sourceName") ?: ""
    )

    private val _uiState = MutableStateFlow(MigrationSearchUiState())
    val uiState: StateFlow<MigrationSearchUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = db.libraryDao().getByUrl(novelUrl)
            _uiState.update { it.copy(sourceEntry = entry, sourceLoading = false) }
            if (entry != null) searchAll(entry.name)
        }
    }

    private fun searchAll(query: String) {
        viewModelScope.launch {
            val providers = novelRepository.getProviders()
            val initial = providers.map { ProviderSearchResult(it.name) }
            _uiState.update { it.copy(providerResults = initial, isSearching = true) }

            novelRepository.searchAllStreaming(query).collect { (providerName, result) ->
                _uiState.update { state ->
                    val updated = state.providerResults.map { r ->
                        if (r.providerName == providerName) {
                            result.fold(
                                onSuccess = { novels -> r.copy(novels = novels, isLoading = false) },
                                onFailure = { err -> r.copy(isLoading = false, error = err.message) }
                            )
                        } else r
                    }
                    val allDone = updated.none { it.isLoading }
                    state.copy(providerResults = updated, isSearching = !allDone)
                }
            }
        }
    }

    fun selectNovel(novel: Novel) {
        _uiState.update { it.copy(selectedNovel = novel, showConfirmDialog = true) }
    }

    fun dismissConfirm() {
        _uiState.update { it.copy(showConfirmDialog = false, selectedNovel = null) }
    }

    fun confirmMigration() {
        val entry = _uiState.value.sourceEntry ?: return
        val target = _uiState.value.selectedNovel ?: return
        _uiState.update { it.copy(showConfirmDialog = false, isMigrating = true) }

        viewModelScope.launch {
            val provider = novelRepository.getProvider(target.apiName)
            val chapters = try {
                provider?.load(target.url)?.chapters ?: emptyList()
            } catch (_: Exception) { emptyList() }

            val result = migrationUseCase.migrate(entry, target, chapters)
            _uiState.update {
                it.copy(
                    isMigrating = false,
                    migrationResult = when (result) {
                        is MigrationUseCase.Result.Success ->
                            MigrationResult.Success(result.migratedReadCount)
                        is MigrationUseCase.Result.Error ->
                            MigrationResult.Error(result.message)
                    }
                )
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(migrationResult = null) }
    }
}
