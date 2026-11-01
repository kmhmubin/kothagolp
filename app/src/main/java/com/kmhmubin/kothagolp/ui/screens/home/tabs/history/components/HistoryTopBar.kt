package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.kmhmubin.kothagolp.ui.components.AlignedLargeTopBar
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy

/**
 * Large title with ⌕/⋮ on the same row (AlignedLargeTopBar). Title crossfades "History" ↔ "N selected" (the count rolls up/down);
 * when search opens, the bar is replaced by the search field, which expands from the ⌕ side.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryTopBar(
    totalCount: Int,
    isLoading: Boolean,
    isSelecting: Boolean,
    selectedCount: Int,
    isSearchOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onDoneSelecting: () -> Unit,
    onSelect: () -> Unit,
    onClear: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        // Selecting (e.g. from the sheet while searching) shows the "N selected" bar over search.
        targetState = isSearchOpen && !isSelecting,
        transitionSpec = {
            if (targetState) {
                (fadeIn(motion.defaultEffectsSpec()) +
                    expandHorizontally(motion.defaultSpatialSpec<IntSize>(), expandFrom = Alignment.End)) togetherWith
                    fadeOut(motion.fastEffectsSpec())
            } else {
                fadeIn(motion.defaultEffectsSpec()) togetherWith
                    (fadeOut(motion.fastEffectsSpec()) +
                        shrinkHorizontally(motion.fastSpatialSpec<IntSize>(), shrinkTowards = Alignment.End))
            }
        },
        contentAlignment = Alignment.TopEnd,
        label = "history_topbar_search",
    ) { searching ->
        if (searching) {
            HistorySearchBar(query = query, onQueryChange = onQueryChange, onClose = onCloseSearch, modifier = Modifier.fillMaxWidth())
        } else {
            AlignedLargeTopBar(
                title = {
                    // -1 = not selecting.
                    AnimatedContent(
                        targetState = if (isSelecting) selectedCount else -1,
                        transitionSpec = {
                            if (initialState >= 0 && targetState >= 0) {
                                val up = targetState > initialState
                                (slideInVertically(motion.defaultSpatialSpec()) { if (up) it else -it } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                                    (slideOutVertically(motion.defaultSpatialSpec()) { if (up) -it else it } + fadeOut(motion.fastEffectsSpec()))
                            } else {
                                fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                            }
                        },
                        label = "history_title",
                    ) { n ->
                        Text(if (n < 0) HistoryCopy.SCREEN_TITLE else HistoryCopy.selected(n))
                    }
                },
                subtitle = when {
                    isSelecting -> null
                    isLoading -> { { Text(" ") } }
                    totalCount > 0 -> { { Text(HistoryCopy.subtitle(totalCount)) } }
                    else -> null
                },
                navigationIcon = if (isSelecting) {
                    {
                        FilledTonalIconButton(onClick = onDoneSelecting) {
                            Icon(Icons.Rounded.Close, HistoryCopy.CD_DONE_SELECTING)
                        }
                    }
                } else null,
                actions = {
                    if (!isSelecting && totalCount > 0) {
                        FilledTonalIconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, HistoryCopy.CD_SEARCH) }
                        HistoryMenu(onSelect = onSelect, onClear = onClear)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    }
}
