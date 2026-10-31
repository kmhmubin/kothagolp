package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy

/** ⋮ button + Expressive grouped vertical menu. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForYouMenu(
    hiddenNovels: Int,
    hiddenAuthors: Int,
    useAllSources: Boolean,
    aiOn: Boolean,
    onRefresh: () -> Unit,
    onTaste: () -> Unit,
    onShowMoreLess: () -> Unit,
    onHidden: () -> Unit,
    onToggleAllSources: () -> Unit,
    onAiPicks: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledTonalIconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, ForYouCopy.MORE_OPTIONS)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            fun close(action: () -> Unit) = { open = false; action() }

            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 3)) {
                Item(0, 1, Icons.Rounded.Refresh, ForYouCopy.MENU_REFRESH, onClick = close(onRefresh))
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 3)) {
                Item(0, 3, Icons.Rounded.Favorite, ForYouCopy.MENU_TASTE, ForYouCopy.MENU_TASTE_SUPPORTING, close(onTaste))
                Item(1, 3, Icons.Rounded.Tune, ForYouCopy.MENU_SHOW_MORE_LESS, ForYouCopy.MENU_SHOW_MORE_LESS_SUPPORTING, close(onShowMoreLess))
                Item(2, 3, Icons.Rounded.VisibilityOff, ForYouCopy.MENU_HIDDEN, ForYouCopy.menuHiddenSupporting(hiddenNovels, hiddenAuthors), close(onHidden))
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(2, 3)) {
                Item(
                    0, 2, Icons.Rounded.Layers, ForYouCopy.MENU_USE_ALL_SOURCES,
                    supporting = if (useAllSources) null else ForYouCopy.MENU_USE_ALL_SOURCES_OFF,
                    onClick = onToggleAllSources,
                    trailing = { Switch(checked = useAllSources, onCheckedChange = { onToggleAllSources() }) }
                )
                Item(
                    1, 2, Icons.Rounded.AutoAwesome, ForYouCopy.MENU_AI_PICKS,
                    if (aiOn) ForYouCopy.MENU_AI_ON_FREE else ForYouCopy.MENU_OFF, close(onAiPicks)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Item(
    i: Int,
    n: Int,
    icon: ImageVector,
    text: String,
    supporting: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) = DropdownMenuItem(
    onClick = onClick,
    text = { Text(text) },
    shape = MenuDefaults.itemShape(i, n).shape,
    leadingIcon = { Icon(icon, null) },
    trailingContent = trailing,
    supportingText = supporting?.let { { Text(it) } }
)
