package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy

/**
 * The search field that replaces the top bar. It stays collapsed: results are the History list
 * itself, filtered live into one "N results" card. ← closes (Back is handled by the tab).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistorySearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    SearchBar(
        state = rememberSearchBarState(),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { focusManager.clearFocus() },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.focusRequester(focus),
                placeholder = { Text(HistoryCopy.SEARCH_HINT) },
                leadingIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, HistoryCopy.CD_BACK) }
                },
                trailingIcon = if (query.isEmpty()) null else {
                    { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, HistoryCopy.CD_CLEAR_QUERY) } }
                },
            )
        },
    )
}
