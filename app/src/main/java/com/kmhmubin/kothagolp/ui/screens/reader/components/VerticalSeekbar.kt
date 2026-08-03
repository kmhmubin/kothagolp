package com.kmhmubin.kothagolp.ui.screens.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmhmubin.kothagolp.ui.screens.reader.model.ReaderDisplayItem
import com.kmhmubin.kothagolp.ui.screens.reader.model.chapterRange
import com.kmhmubin.kothagolp.ui.theme.AppShape
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A thumb-draggable vertical progress bar for the current chapter, pinned to
 * the right edge (inspired by LNReader's seekbar). Two jobs in one control:
 * an always-visible glance at how far into the chapter you are, and a way to
 * jump anywhere inside it in one drag instead of flinging through dozens of
 * screens.
 *
 * Scoped to the CURRENT chapter's own item range (via [chapterRange]), not
 * the whole infinite-scroll display list — dragging to the bottom lands at
 * the end of this chapter, not the end of whatever else happens to be
 * preloaded around it.
 */
@Composable
fun VerticalSeekbar(
    listState: LazyListState,
    displayItems: List<ReaderDisplayItem>,
    currentChapterIndex: Int,
    progress: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    // The chapter can grow (infinite scroll preloading a neighbor) or shift
    // while a drag is in progress; re-resolve the range on every seek instead
    // of capturing it once at gesture start.
    val latestDisplayItems by rememberUpdatedState(displayItems)
    val latestChapterIndex by rememberUpdatedState(currentChapterIndex)

    fun seekTo(ratio: Float) {
        val clamped = ratio.coerceIn(0f, 1f)
        val range = latestDisplayItems.chapterRange(latestChapterIndex) ?: return
        val count = range.last - range.first + 1
        if (count <= 1) return
        val targetIndex = range.first + (clamped * (count - 1)).roundToInt()
        dragRatio = clamped
        scope.launch { listState.scrollToItem(targetIndex) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val displayRatio = if (isDragging) dragRatio else progress.coerceIn(0f, 1f)
        val displayPercent = (displayRatio * 100).roundToInt()

        Surface(
            modifier = Modifier
                .padding(end = 12.dp)
                .width(36.dp)
                .fillMaxHeight(0.5f)
                .heightIn(min = 220.dp),
            shape = AppShape.pill,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$displayPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (trackHeightPx > 0f) seekTo(offset.y / trackHeightPx)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (trackHeightPx > 0f) seekTo(offset.y / trackHeightPx)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (trackHeightPx > 0f) seekTo(change.position.y / trackHeightPx)
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Track line
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight()
                            .width(2.dp)
                            .clip(AppShape.pill)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )

                    // Fill + thumb: fill grows top-down with progress, thumb rides its
                    // bottom edge — same trick LNReader's CSS uses (top: 100%).
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(displayRatio.coerceIn(0.02f, 1f))
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxHeight()
                                .width(2.dp)
                                .clip(AppShape.pill)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(14.dp)
                                .width(14.dp)
                                .clip(AppShape.circle)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Text(
                    text = "100",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
