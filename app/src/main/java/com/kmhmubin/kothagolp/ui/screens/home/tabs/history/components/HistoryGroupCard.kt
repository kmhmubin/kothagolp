package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryGroup
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryRowModel

private val CardRadius = 24.dp
private val CoverShape = RoundedCornerShape(10.dp)

/**
 * One date group: a header, then its rows drawn as one rounded card. Each row is its own lazy
 * item (so removes, undos and filters animate with animateItem); the first and last rows carry
 * the card's top and bottom corners, so a card that empties simply collapses away.
 */
fun LazyListScope.historyGroup(
    group: HistoryGroup,
    isSelecting: Boolean,
    selectedUrls: Set<String>,
    onClick: (HistoryRowModel) -> Unit,
    onLongClick: (HistoryRowModel) -> Unit,
    onSwipeRemove: (HistoryRowModel) -> Unit,
) {
    item(key = "header_${group.key}", contentType = "header") {
        Text(
            group.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = itemMotion().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        )
    }
    itemsIndexed(group.rows, key = { _, r -> r.item.novel.url }, contentType = { _, _ -> "row" }) { i, row ->
        HistoryRow(
            row = row,
            first = i == 0,
            last = i == group.rows.lastIndex,
            isSelecting = isSelecting,
            selected = row.item.novel.url in selectedUrls,
            onClick = { onClick(row) },
            onLongClick = { onLongClick(row) },
            onSwipeRemove = { onSwipeRemove(row) },
            modifier = itemMotion(),
        )
    }
}

/** Spring fade + placement from the Expressive motion scheme. */
@Composable
private fun LazyItemScope.itemMotion(): Modifier {
    val m = MaterialTheme.motionScheme
    return Modifier.animateItem(fadeInSpec = m.defaultEffectsSpec(), placementSpec = m.defaultSpatialSpec(), fadeOutSpec = m.fastEffectsSpec())
}

@Composable
private fun HistoryRow(
    row: HistoryRowModel,
    first: Boolean,
    last: Boolean,
    isSelecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val haptics = LocalHapticFeedback.current
    val cornerSpec = motion.defaultSpatialSpec<Dp>()
    val top by animateDpAsState(if (first) CardRadius else 0.dp, cornerSpec, label = "top_corner")
    val bottom by animateDpAsState(if (last) CardRadius else 0.dp, cornerSpec, label = "bottom_corner")

    // "Past halfway, the row slides away."
    // Not saveable: lazy items restore rememberSaveable state by key, so an Undo-restored row would
    // come back already dismissed and remove itself again.
    lateinit var dismiss: SwipeToDismissBoxState
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        dismiss = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.5f })
    }
    var width by remember { mutableIntStateOf(1) }
    val pastThreshold = dismiss.targetValue == SwipeToDismissBoxValue.EndToStart
    val pop = remember { Animatable(1f) }
    val popUp = motion.fastSpatialSpec<Float>()
    val popDown = motion.defaultSpatialSpec<Float>()
    LaunchedEffect(pastThreshold) {
        if (pastThreshold) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            pop.animateTo(1.2f, popUp)
            pop.animateTo(1f, popDown)
        }
    }

    Column(
        modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
            .background(colors.surfaceContainerLow)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
    ) {
        SwipeToDismissBox(
            state = dismiss,
            enableDismissFromStartToEnd = false,
            gesturesEnabled = !isSelecting,
            onDismiss = { onSwipeRemove() },
            backgroundContent = {
                // Read the offset here so only the background recomposes while dragging.
                val offset = runCatching { dismiss.requireOffset() }.getOrDefault(0f)
                val f = (-offset / width).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxSize().background(lerp(colors.surfaceContainerLow, colors.errorContainer, (f * 3f).coerceAtMost(1f))).padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            val s = (0.6f + f * 0.8f).coerceAtMost(1f) * pop.value
                            scaleX = s
                            scaleY = s
                        },
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = colors.onErrorContainer)
                        Text(HistoryCopy.REMOVE, style = MaterialTheme.typography.labelLarge, color = colors.onErrorContainer)
                    }
                }
            },
        ) {
            RowContent(row, isSelecting, selected, onClick, onLongClick)
        }
        if (!last) {
            val inset by animateDpAsState(if (isSelecting) 106.dp else 70.dp, motion.defaultSpatialSpec(), label = "divider_inset")
            HorizontalDivider(Modifier.padding(start = inset), thickness = Dp.Hairline, color = colors.outlineVariant)
        }
    }
}

@Composable
private fun RowContent(row: HistoryRowModel, isSelecting: Boolean, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, motion.fastSpatialSpec(), label = "row_press")
    val bg by animateColorAsState(if (selected) colors.secondaryContainer else colors.surfaceContainerLow, motion.defaultEffectsSpec(), label = "row_bg")

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bg)
            // combinedClickable performs the long-press haptic itself (hapticFeedbackEnabled defaults to true).
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                role = if (isSelecting) Role.Checkbox else null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            // TalkBack announces checked / not checked in select mode.
            .then(if (isSelecting) Modifier.semantics { toggleableState = ToggleableState(selected) } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = isSelecting,
            enter = fadeIn(motion.defaultEffectsSpec()) + expandHorizontally(motion.defaultSpatialSpec()),
            exit = fadeOut(motion.fastEffectsSpec()) + shrinkHorizontally(motion.fastSpatialSpec()),
        ) { SelectMark(selected) }
        AsyncImage(
            model = row.item.novel.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp, 64.dp).clip(CoverShape).background(colors.surfaceContainerHigh),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    highlighted(row.title, row.titleMatch, colors.primary),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(row.time, style = MaterialTheme.typography.labelMedium, color = colors.outline)
            }
            Text(
                row.chapterLine,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(row.metaLine, style = MaterialTheme.typography.bodySmall, color = colors.outline, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun highlighted(title: String, match: IntRange?, color: Color) = buildAnnotatedString {
    if (match == null || match.last >= title.length) {
        append(title)
        return@buildAnnotatedString
    }
    append(title.substring(0, match.first))
    withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(title.substring(match.first, match.last + 1)) }
    append(title.substring(match.last + 1))
}

/** Select-mode tick: a circle that morphs into a rounded square, with the check scaling in. */
@Composable
private fun SelectMark(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val radius by animateDpAsState(if (selected) 8.dp else 12.dp, motion.defaultSpatialSpec(), label = "mark_radius")
    val fill by animateColorAsState(if (selected) colors.primary else Color.Transparent, motion.defaultEffectsSpec(), label = "mark_fill")
    val edge by animateColorAsState(if (selected) colors.primary else colors.outline, motion.defaultEffectsSpec(), label = "mark_edge")
    val check by animateFloatAsState(if (selected) 1f else 0f, motion.fastSpatialSpec(), label = "mark_check")
    val shape = RoundedCornerShape(radius)
    Box(Modifier.size(24.dp).clip(shape).background(fill).border(2.dp, edge, shape), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.Check,
            null,
            tint = colors.onPrimary,
            modifier = Modifier.size(16.dp).graphicsLayer { scaleX = check; scaleY = check },
        )
    }
}
