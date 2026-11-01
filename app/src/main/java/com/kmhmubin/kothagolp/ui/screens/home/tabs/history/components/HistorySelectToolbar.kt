package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy

/** Select-mode floating toolbar: Select all · Remove N. Slides up + fades in, exits in reverse. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistorySelectToolbar(visible: Boolean, selectedCount: Int, onSelectAll: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    // Keep the last count on screen while the toolbar animates out after a remove.
    var shown by remember { mutableIntStateOf(0) }
    if (visible) shown = selectedCount
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) {
            FilledTonalButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.SelectAll, null, Modifier.size(18.dp))
                Text(HistoryCopy.SELECT_ALL, modifier = Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.size(6.dp))
            Button(
                onClick = onRemove,
                enabled = shown > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp))
                Text(HistoryCopy.removeN(shown), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
