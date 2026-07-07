package com.kmhmubin.kothagolp.ui.screens.home.tabs.updates

import com.kmhmubin.kothagolp.data.repository.LibraryItem

data class UpdatesUiState(
    val isLoading: Boolean = false,
    val updates: List<LibraryItem> = emptyList(),
    val totalNewChapters: Int = 0,
    val error: String? = null
)
