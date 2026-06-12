package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.HistoryItem
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class HistoryDateGroup(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    EARLIER("Earlier")
}

data class HistoryUiState(
    val groupedItems: Map<HistoryDateGroup, List<HistoryItem>> = emptyMap(),
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val isLoading: Boolean = true,
    val showClearConfirmation: Boolean = false,
    // Search
    val searchQuery: String = "",
    // Selection
    val isSelectionMode: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val showDeleteSelectedConfirmation: Boolean = false
)

class HistoryViewModel : ViewModel() {

    private val historyRepository = RepositoryProvider.getHistoryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allItems: List<HistoryItem> = emptyList()

    init {
        observeHistory()
    }

    // ================================================================
    // DATA
    // ================================================================

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.observeHistory().collect { items ->
                allItems = items
                recompute()
            }
        }
    }

    private fun getFilteredItems(): List<HistoryItem> {
        val query = _uiState.value.searchQuery.trim()
        return if (query.isBlank()) allItems
        else allItems.filter { item ->
            item.novel.name.contains(query, ignoreCase = true) ||
                    item.chapterName.contains(query, ignoreCase = true) ||
                    item.novel.apiName.contains(query, ignoreCase = true)
        }
    }

    private fun recompute() {
        val filtered = getFilteredItems()
        val grouped = groupItemsByDate(filtered)

        val validUrls = allItems.map { it.novel.url }.toSet()
        val cleanedSelection = _uiState.value.selectedItems.intersect(validUrls)

        _uiState.update {
            it.copy(
                groupedItems = grouped,
                totalCount = allItems.size,
                filteredCount = filtered.size,
                isLoading = false,
                selectedItems = cleanedSelection,
                isSelectionMode = if (cleanedSelection.isEmpty()) false else it.isSelectionMode
            )
        }
    }

    private fun groupItemsByDate(items: List<HistoryItem>): Map<HistoryDateGroup, List<HistoryItem>> {
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        return items
            .sortedByDescending { it.timestamp }
            .groupBy { item ->
                val itemDate = Instant.ofEpochMilli(item.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()
                val daysBetween = ChronoUnit.DAYS.between(itemDate, now)
                when {
                    daysBetween == 0L -> HistoryDateGroup.TODAY
                    daysBetween == 1L -> HistoryDateGroup.YESTERDAY
                    daysBetween <= 7L -> HistoryDateGroup.THIS_WEEK
                    daysBetween <= 30L -> HistoryDateGroup.THIS_MONTH
                    else -> HistoryDateGroup.EARLIER
                }
            }
    }

    // ================================================================
    // SEARCH
    // ================================================================

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        recompute()
    }

    // ================================================================
    // SELECTION
    // ================================================================

    fun enterSelectionMode(initialNovelUrl: String? = null) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedItems = if (initialNovelUrl != null) setOf(initialNovelUrl) else emptySet()
            )
        }
    }

    fun exitSelectionMode() {
        _uiState.update {
            it.copy(isSelectionMode = false, selectedItems = emptySet())
        }
    }

    fun toggleSelection(novelUrl: String) {
        _uiState.update { state ->
            val newSelection = if (novelUrl in state.selectedItems) {
                state.selectedItems - novelUrl
            } else {
                state.selectedItems + novelUrl
            }
            if (newSelection.isEmpty()) {
                state.copy(isSelectionMode = false, selectedItems = emptySet())
            } else {
                state.copy(selectedItems = newSelection)
            }
        }
    }

    fun selectAllVisible() {
        val filteredUrls = getFilteredItems().map { it.novel.url }.toSet()
        val currentSelection = _uiState.value.selectedItems
        val allVisibleSelected =
            filteredUrls.isNotEmpty() && filteredUrls.all { it in currentSelection }

        val newSelection = if (allVisibleSelected) {
            currentSelection - filteredUrls
        } else {
            currentSelection + filteredUrls
        }

        _uiState.update {
            if (newSelection.isEmpty()) {
                it.copy(isSelectionMode = false, selectedItems = emptySet())
            } else {
                it.copy(selectedItems = newSelection)
            }
        }
    }

    fun requestDeleteSelected() {
        if (_uiState.value.selectedItems.isNotEmpty()) {
            _uiState.update { it.copy(showDeleteSelectedConfirmation = true) }
        }
    }

    fun confirmDeleteSelected() {
        viewModelScope.launch {
            val toDelete = _uiState.value.selectedItems.toList()
            toDelete.forEach { novelUrl ->
                historyRepository.removeFromHistory(novelUrl)
            }
            _uiState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedItems = emptySet(),
                    showDeleteSelectedConfirmation = false
                )
            }
        }
    }

    fun dismissDeleteSelectedConfirmation() {
        _uiState.update { it.copy(showDeleteSelectedConfirmation = false) }
    }

    // ================================================================
    // INDIVIDUAL ACTIONS
    // ================================================================

    fun removeFromHistory(novelUrl: String) {
        viewModelScope.launch {
            historyRepository.removeFromHistory(novelUrl)
        }
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.update {
                it.copy(
                    showClearConfirmation = false,
                    isSelectionMode = false,
                    selectedItems = emptySet(),
                    searchQuery = ""
                )
            }
        }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }
}