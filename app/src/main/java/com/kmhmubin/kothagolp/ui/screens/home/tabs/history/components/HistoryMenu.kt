package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy

/** ⋮ button + Expressive grouped menu: Select, then "Clear history…" kept apart in the error color. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryMenu(onSelect: () -> Unit, onClear: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledTonalIconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, HistoryCopy.CD_MORE) }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            val w = Modifier.width(220.dp)
            DropdownMenuGroup(modifier = w, shapes = MenuDefaults.groupShape(0, 2)) {
                DropdownMenuItem(
                    onClick = { open = false; onSelect() },
                    text = { Text(HistoryCopy.SELECT) },
                    shape = MenuDefaults.itemShape(0, 1).shape,
                    leadingIcon = { Icon(Icons.Rounded.Checklist, null) },
                )
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(modifier = w, shapes = MenuDefaults.groupShape(1, 2)) {
                val err = MaterialTheme.colorScheme.error
                DropdownMenuItem(
                    onClick = { open = false; onClear() },
                    text = { Text(HistoryCopy.CLEAR_MENU) },
                    shape = MenuDefaults.itemShape(0, 1).shape,
                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null) },
                    colors = MenuDefaults.itemColors(textColor = err, leadingIconColor = err),
                )
            }
        }
    }
}
