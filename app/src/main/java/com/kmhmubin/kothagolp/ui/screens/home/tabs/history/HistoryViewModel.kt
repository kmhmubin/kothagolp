package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.HistoryItem
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.Novel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

/** One-shot message the UI shows as a snackbar, then acknowledges with [HistoryViewModel.snackbarShown]. */
sealed interface HistorySnack {
    data class Removed(val items: List<HistoryItem>) : HistorySnack
    data object TitleCopied : HistorySnack
    data class AddedToLibrary(val novel: Novel) : HistorySnack
}

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val query: String = "",
    val isSearchOpen: Boolean = false,
    val selectedUrls: Set<String> = emptySet(),
    val isSelecting: Boolean = false,
    val showClearDialog: Boolean = false,
    val snackbar: HistorySnack? = null,
)

class HistoryViewModel : ViewModel() {

    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allItems: List<HistoryItem> = emptyList()

    /** Keyed by novel url. */
    private var library: Map<String, LibraryInfo> = emptyMap()
    private var now: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            historyRepository.observeHistory().collect { items ->
                allItems = items
                recompute()
            }
        }
        viewModelScope.launch {
            libraryRepository.observeLibrary().collect { items ->
                library = items.associate {
                    it.novel.url to LibraryInfo(true, it.lastReadChapterIndex, it.totalChapterCount)
                }
                recompute()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                now = System.currentTimeMillis()
                recompute()
            }
        }
    }

    private fun recompute() {
        val use24h = DateFormat.is24HourFormat(RepositoryProvider.getAppContext())
        _uiState.update { s ->
            val groups = buildHistoryGroups(allItems, library, s.query, now, ZoneId.systemDefault(), use24h)
            val selected = s.selectedUrls.intersect(allItems.map { it.novel.url }.toSet())
            s.copy(
                groups = groups,
                totalCount = allItems.size,
                isLoading = false,
                selectedUrls = selected,
            )
        }
    }

    // ---- remove / undo ----

    fun remove(urls: List<String>) {
        val removed = allItems.filter { it.novel.url in urls }
        if (removed.isEmpty()) return
        viewModelScope.launch {
            removed.forEach { historyRepository.removeFromHistory(it.novel.url) }
            _uiState.update { it.copy(selectedUrls = emptySet(), isSelecting = false, snackbar = HistorySnack.Removed(removed)) }
        }
    }

    fun undoRemove(items: List<HistoryItem>) {
        viewModelScope.launch { historyRepository.restore(items) }
    }

    // ---- library / copy ----

    fun addToLibrary(novel: Novel) {
        viewModelScope.launch {
            libraryRepository.addToLibrary(novel)
            _uiState.update { it.copy(snackbar = HistorySnack.AddedToLibrary(novel)) }
        }
    }

    fun undoAddToLibrary(novel: Novel) {
        viewModelScope.launch { libraryRepository.removeFromLibrary(novel.url) }
    }

    /** The clipboard write itself is UI work; this only raises the confirmation. */
    fun titleCopied() = _uiState.update { it.copy(snackbar = HistorySnack.TitleCopied) }

    fun snackbarShown() = _uiState.update { it.copy(snackbar = null) }

    // ---- selection ----

    fun startSelecting(url: String?) =
        _uiState.update { it.copy(isSelecting = true, selectedUrls = if (url != null) setOf(url) else emptySet()) }

    fun stopSelecting() = _uiState.update { it.copy(isSelecting = false, selectedUrls = emptySet()) }

    fun toggle(url: String) = _uiState.update {
        it.copy(selectedUrls = if (url in it.selectedUrls) it.selectedUrls - url else it.selectedUrls + url)
    }

    fun selectAll() = _uiState.update { s ->
        s.copy(selectedUrls = s.groups.flatMap { g -> g.rows.map { it.item.novel.url } }.toSet())
    }

    // ---- search ----

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        recompute()
    }

    fun openSearch() = _uiState.update { it.copy(isSearchOpen = true) }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchOpen = false, query = "") }
        recompute()
    }

    // ---- clear ----

    fun requestClear() = _uiState.update { it.copy(showClearDialog = true) }

    fun dismissClear() = _uiState.update { it.copy(showClearDialog = false) }

    fun confirmClear() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.update {
                it.copy(showClearDialog = false, isSelecting = false, selectedUrls = emptySet(), query = "", isSearchOpen = false)
            }
        }
    }
}
