package com.kmhmubin.kothagolp.ui.screens.home.tabs.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdatesViewModel : ViewModel() {

    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _uiState = MutableStateFlow(UpdatesUiState())
    val uiState: StateFlow<UpdatesUiState> = _uiState.asStateFlow()

    init {
        loadUpdates()
    }

    private fun loadUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                libraryRepository.observeNovelsWithNewChapters().collect { items ->
                    val sortedByUpdate = items.sortedByDescending { it.lastUpdatedAt }
                    val totalNew = items.sumOf { it.newChapterCount }
                    _uiState.update {
                        it.copy(
                            updates = sortedByUpdate,
                            totalNewChapters = totalNew,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load updates"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadUpdates()
    }
}
