package com.kmhmubin.kothagolp.ui.screens.migration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.local.entity.LibraryEntity
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MigrationNovelsUiState {
    object Loading : MigrationNovelsUiState()
    data class Success(val novels: List<LibraryEntity>) : MigrationNovelsUiState()
    object Empty : MigrationNovelsUiState()
    data class Error(val message: String) : MigrationNovelsUiState()
}

class MigrationNovelsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val db = RepositoryProvider.getDatabase()
    val sourceName: String = NavRoutes.decodeUrl(
        savedStateHandle.get<String>("sourceName") ?: ""
    )

    private val _uiState = MutableStateFlow<MigrationNovelsUiState>(MigrationNovelsUiState.Loading)
    val uiState: StateFlow<MigrationNovelsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MigrationNovelsUiState.Loading
            try {
                val novels = db.libraryDao().getBySourceName(sourceName)
                _uiState.value = if (novels.isEmpty()) MigrationNovelsUiState.Empty
                else MigrationNovelsUiState.Success(novels)
            } catch (e: Exception) {
                _uiState.value = MigrationNovelsUiState.Error(e.message ?: "Failed to load")
            }
        }
    }
}
