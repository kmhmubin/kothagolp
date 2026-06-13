package com.kmhmubin.kothagolp.ui.screens.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SourceEntry(val sourceName: String, val novelCount: Int)

sealed class MigrationSourcesUiState {
    object Loading : MigrationSourcesUiState()
    data class Success(val sources: List<SourceEntry>) : MigrationSourcesUiState()
    object Empty : MigrationSourcesUiState()
    data class Error(val message: String) : MigrationSourcesUiState()
}

class MigrationSourcesViewModel : ViewModel() {

    private val db = RepositoryProvider.getDatabase()

    private val _uiState = MutableStateFlow<MigrationSourcesUiState>(MigrationSourcesUiState.Loading)
    val uiState: StateFlow<MigrationSourcesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MigrationSourcesUiState.Loading
            try {
                val allNovels = db.libraryDao().getAll()
                if (allNovels.isEmpty()) {
                    _uiState.value = MigrationSourcesUiState.Empty
                    return@launch
                }
                val sources = allNovels
                    .groupBy { it.apiName }
                    .map { (name, novels) -> SourceEntry(name, novels.size) }
                    .sortedBy { it.sourceName }
                _uiState.value = MigrationSourcesUiState.Success(sources)
            } catch (e: Exception) {
                _uiState.value = MigrationSourcesUiState.Error(e.message ?: "Failed to load")
            }
        }
    }
}
