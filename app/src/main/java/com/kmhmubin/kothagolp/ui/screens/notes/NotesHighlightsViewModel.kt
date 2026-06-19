package com.kmhmubin.kothagolp.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.local.entity.BookmarkEntity
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnotationItem(
    val id: Long,
    val novelUrl: String,
    val novelName: String,
    val chapterUrl: String,
    val chapterName: String,
    val highlightedText: String,
    val userNote: String?,
    val color: String,
    val providerName: String?,
    val createdAt: Long
)

enum class AnnotationFilter { ALL, WITH_NOTES, HIGHLIGHTS_ONLY }

data class NotesHighlightsUiState(
    val isLoading: Boolean = true,
    val items: List<AnnotationItem> = emptyList(),
    val filter: AnnotationFilter = AnnotationFilter.ALL,
    val searchQuery: String = "",
    val novelUrlFilter: String? = null,
    val groupedItems: Map<String, List<AnnotationItem>> = emptyMap()
)

class NotesHighlightsViewModel : ViewModel() {

    private val bookmarkRepository = RepositoryProvider.getBookmarkRepository()

    private val _uiState = MutableStateFlow(NotesHighlightsUiState())
    val uiState: StateFlow<NotesHighlightsUiState> = _uiState.asStateFlow()

    private var allItems: List<AnnotationItem> = emptyList()

    init {
        bookmarkRepository.observeAllAnnotations()
            .onEach { entities ->
                allItems = entities.mapNotNull { it.toAnnotationItem() }
                applyFilterAndSearch()
            }
            .launchIn(viewModelScope)
    }

    fun setNovelUrlFilter(novelUrl: String?) {
        _uiState.update { it.copy(novelUrlFilter = novelUrl) }
        applyFilterAndSearch()
    }

    fun setFilter(filter: AnnotationFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilterAndSearch()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSearch()
    }

    fun deleteAnnotation(id: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteHighlight(id)
        }
    }

    fun updateNote(id: Long, note: String?) {
        viewModelScope.launch {
            bookmarkRepository.updateUserNote(id, note?.ifBlank { null })
        }
    }

    private fun applyFilterAndSearch() {
        val state = _uiState.value
        var filtered = allItems

        state.novelUrlFilter?.let { url ->
            filtered = filtered.filter { it.novelUrl == url }
        }

        filtered = when (state.filter) {
            AnnotationFilter.ALL -> filtered
            AnnotationFilter.WITH_NOTES -> filtered.filter { !it.userNote.isNullOrBlank() }
            AnnotationFilter.HIGHLIGHTS_ONLY -> filtered.filter { it.userNote.isNullOrBlank() }
        }

        val query = state.searchQuery.trim()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.highlightedText.contains(query, ignoreCase = true) ||
                item.userNote?.contains(query, ignoreCase = true) == true ||
                item.novelName.contains(query, ignoreCase = true) ||
                item.chapterName.contains(query, ignoreCase = true)
            }
        }

        val grouped = filtered.groupBy { it.novelName.ifBlank { it.novelUrl } }

        _uiState.update { it.copy(items = filtered, groupedItems = grouped, isLoading = false) }
    }

    private fun BookmarkEntity.toAnnotationItem(): AnnotationItem? {
        val snippet = textSnippet?.takeIf { it.isNotBlank() } ?: return null
        return AnnotationItem(
            id = id,
            novelUrl = novelUrl,
            novelName = novelName.ifBlank { novelUrl },
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            highlightedText = snippet,
            userNote = userNote,
            color = color ?: "#FFD54F",
            providerName = providerName,
            createdAt = createdAt
        )
    }
}
