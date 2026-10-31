package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryRowModel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.sheetMeta

/**
 * Compact press-and-hold sheet: header (cover · title · chapter/source · progress/time · library
 * status), connected [Continue | Novel page], then Copy title · Select · Remove.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistorySheet(
    row: HistoryRowModel,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onNovelPage: () -> Unit,
    onAddToLibrary: () -> Unit,
    onCopyTitle: () -> Unit,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val (line1, line2) = sheetMeta(row)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Slide the sheet down first, then leave composition and run the action.
    fun hideThen(action: () -> Unit): () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = row.item.novel.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp, 68.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerHigh),
                )
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(line1, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    line2?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.outline, modifier = Modifier.padding(top = 2.dp)) }
                }
                // "＋ Library" morphs into the "In library" label once added.
                AnimatedContent(
                    targetState = row.inLibrary,
                    transitionSpec = {
                        (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.8f)) togetherWith
                            (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f))
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "library_status",
                ) { inLibrary ->
                    if (inLibrary) {
                        Surface(shape = RoundedCornerShape(10.dp), color = colors.secondaryContainer) {
                            Text(HistoryCopy.IN_LIBRARY, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    } else {
                        AssistChip(onClick = onAddToLibrary, label = { Text(HistoryCopy.ADD_LIBRARY, color = colors.primary, fontWeight = FontWeight.SemiBold) })
                    }
                }
            }

            ButtonGroup(
                overflowIndicator = {},
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                customItem(
                    buttonGroupContent = {
                        Button(
                            onClick = hideThen(onContinue),
                            shapes = ButtonDefaults.shapes(ButtonGroupDefaults.connectedLeadingButtonShape, ButtonGroupDefaults.connectedLeadingButtonPressShape),
                            modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                            Text(HistoryCopy.CONTINUE, modifier = Modifier.padding(start = 6.dp))
                        }
                    },
                    menuContent = {},
                )
                customItem(
                    buttonGroupContent = {
                        FilledTonalButton(
                            onClick = hideThen(onNovelPage),
                            shapes = ButtonDefaults.shapes(ButtonGroupDefaults.connectedTrailingButtonShape, ButtonGroupDefaults.connectedTrailingButtonPressShape),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(HistoryCopy.NOVEL_PAGE) }
                    },
                    menuContent = {},
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction(Icons.Rounded.ContentCopy, HistoryCopy.COPY_TITLE, colors.onSurfaceVariant, hideThen(onCopyTitle))
                SmallAction(Icons.Rounded.Checklist, HistoryCopy.SELECT, colors.onSurfaceVariant, hideThen(onSelect))
                SmallAction(Icons.Rounded.Delete, HistoryCopy.REMOVE, colors.error, hideThen(onRemove))
            }
            // Same state as the tab's host (which hides while the sheet is open): "Added to your library · Undo" stays reachable.
            SnackbarHost(snackbarHostState, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun RowScope.SmallAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = color),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
