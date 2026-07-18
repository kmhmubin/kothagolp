package com.kmhmubin.kothagolp.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.Success
import com.kmhmubin.kothagolp.ui.theme.Warning
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun ChapterItem(
    chapter: Chapter,
    index: Int,
    isRead: Boolean,
    isDownloaded: Boolean,
    isLastRead: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    /** 1..99 = partially read (shows a "N%" hint); null/0/100 shows nothing. */
    progressPercent: Int? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeToRead: (() -> Unit)? = null,
    onSwipeToDownload: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Stable callback refs — pointerInput only restarts when isSelectionMode changes,
    // not on every recompose (lambdas from parent are new instances each time).
    val currentOnTap = rememberUpdatedState(onTap)
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnSwipeToRead = rememberUpdatedState(onSwipeToRead)
    val currentOnSwipeToDownload = rememberUpdatedState(onSwipeToDownload)

    // Swipe state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val maxSwipe = with(density) { 100.dp.toPx() }

    // Animation states
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipe_offset"
    )

    // Colors computed once per state change — no per-frame animator overhead during scrolling
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val backgroundColor = remember(isSelectionMode, isSelected, isLastRead, isRead, primaryContainer, tertiaryContainer, surfaceContainer) {
        when {
            isSelectionMode && isSelected -> primaryContainer.copy(alpha = 0.6f)
            isLastRead -> tertiaryContainer.copy(alpha = 0.4f)
            isRead -> surfaceContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        }
    }

    val textColor = remember(isSelectionMode, isSelected, isRead, primaryColor, onSurfaceVariant, onSurface) {
        when {
            isSelectionMode && isSelected -> primaryColor
            isRead -> onSurfaceVariant.copy(alpha = 0.55f)
            else -> onSurface
        }
    }

    val secondaryTextColor = remember(isSelectionMode, isSelected, isRead, primaryColor, onSurfaceVariant) {
        when {
            isSelectionMode && isSelected -> primaryColor.copy(alpha = 0.7f)
            isRead -> onSurfaceVariant.copy(alpha = 0.4f)
            else -> onSurfaceVariant.copy(alpha = 0.6f)
        }
    }

    val border = remember(isLastRead, isSelectionMode, isSelected, tertiaryColor, primaryColor) {
        when {
            isLastRead -> BorderStroke(1.dp, tertiaryColor.copy(alpha = 0.5f))
            isSelectionMode && isSelected -> BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
            else -> null
        }
    }

    val elevation = when {
        isSelectionMode && isSelected -> 4.dp
        isLastRead -> 2.dp
        else -> 0.dp
    }

    val itemScale by animateFloatAsState(
        targetValue = if (isSelectionMode && isSelected) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "item_scale"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isSelectionMode) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkbox_scale"
    )

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selected_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        // Swipe action backgrounds — matchParentSize() avoids IntrinsicSize.Max double-measure
        if (!isSelectionMode && (onSwipeToRead != null || onSwipeToDownload != null)) {
            SwipeActionBackground(
                modifier = Modifier.matchParentSize(),
                offsetX = animatedOffset,
                swipeThreshold = swipeThreshold,
                isRead = isRead,
                isDownloaded = isDownloaded
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .scale(itemScale)
                // Key only on isSelectionMode — rememberUpdatedState keeps callbacks fresh
                // without restarting the gesture coroutine on every recompose.
                .pointerInput(isSelectionMode) {
                    if (!isSelectionMode) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val swipeRead = currentOnSwipeToRead.value
                                val swipeDownload = currentOnSwipeToDownload.value
                                when {
                                    offsetX > swipeThreshold && swipeRead != null -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeRead()
                                    }
                                    offsetX < -swipeThreshold && swipeDownload != null -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeDownload()
                                    }
                                }
                                offsetX = 0f
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = offsetX + dragAmount
                                offsetX = newOffset.coerceIn(-maxSwipe, maxSwipe)

                                if (offsetX.absoluteValue >= swipeThreshold * 0.9f &&
                                    (offsetX - dragAmount).absoluteValue < swipeThreshold * 0.9f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                }
                .pointerInput(isSelectionMode) {
                    detectTapGestures(
                        onTap = { currentOnTap.value() },
                        onLongPress = { currentOnLongPress.value() }
                    )
                },
            shape = AppShape.medium,
            color = backgroundColor,
            shadowElevation = elevation,
            border = border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Selection checkbox with animation
                    SelectionCheckbox(
                        isVisible = checkboxScale > 0.01f,
                        isSelected = isSelected,
                        selectedScale = selectedScale
                    )

                    // Last read indicator
                    LastReadIndicator(
                        isVisible = isLastRead && !isSelectionMode
                    )

                    // Chapter info
                    ChapterInfo(
                        chapter = chapter,
                        isLastRead = isLastRead,
                        isSelectionMode = isSelectionMode,
                        progressPercent = progressPercent.takeIf { !isRead },
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side status icons
                ChapterStatusIcons(
                    isDownloaded = isDownloaded,
                    isRead = isRead,
                    isSelectionMode = isSelectionMode
                )
            }
        }
    }
}

@Composable
private fun SwipeActionBackground(
    modifier: Modifier = Modifier,
    offsetX: Float,
    swipeThreshold: Float,
    isRead: Boolean,
    isDownloaded: Boolean
) {
    val leftProgress = (offsetX / swipeThreshold).coerceIn(0f, 1f)
    val rightProgress = (-offsetX / swipeThreshold).coerceIn(0f, 1f)

    Row(
        modifier = modifier.clip(AppShape.medium),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left action (mark as read/unread)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (isRead) Warning.copy(alpha = leftProgress * 0.25f)
                    else Success.copy(alpha = leftProgress * 0.25f)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .graphicsLayer {
                        alpha = leftProgress
                        scaleX = 0.6f + leftProgress * 0.4f
                        scaleY = 0.6f + leftProgress * 0.4f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isRead) Icons.Outlined.VisibilityOff else Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isRead) Warning else Success
                )
                Text(
                    text = if (isRead) "Unread" else "Read",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRead) Warning else Success,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right action (download/delete)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (isDownloaded) Color.Red.copy(alpha = rightProgress * 0.25f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = rightProgress * 0.25f)
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .graphicsLayer {
                        alpha = rightProgress
                        scaleX = 0.6f + rightProgress * 0.4f
                        scaleY = 0.6f + rightProgress * 0.4f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isDownloaded) "Delete" else "Download",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDownloaded) Color.Red else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isDownloaded) Icons.Filled.Delete else Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDownloaded) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckbox(
    isVisible: Boolean,
    isSelected: Boolean,
    selectedScale: Float
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .scale(selectedScale)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent
                    )
                    .then(
                        if (!isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            val s = if (isSelected) selectedScale else 0f
                            scaleX = s
                            scaleY = s
                            alpha = s
                        },
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun LastReadIndicator(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(3.dp, 24.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        AppShape.extraSmall
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun ChapterInfo(
    chapter: Chapter,
    isLastRead: Boolean,
    isSelectionMode: Boolean,
    progressPercent: Int?,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val showProgress = !isSelectionMode && progressPercent != null && progressPercent in 1..99
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Chapter name
        Text(
            text = chapter.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isLastRead) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Fixed-height secondary row — always reserves 16dp so all items have consistent
        // height, preventing LazyList from re-measuring items during scroll.
        val hasSecondaryInfo = chapter.dateOfRelease != null || (isLastRead && !isSelectionMode) || showProgress
        Box(modifier = Modifier.height(16.dp)) {
            if (hasSecondaryInfo) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // In-progress hint ("read 50%") for a started-but-unfinished chapter
                    if (showProgress) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        if (chapter.dateOfRelease != null || (isLastRead && !isSelectionMode)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor
                            )
                        }
                    }

                    // Release date with icon
                    chapter.dateOfRelease?.let { date ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = secondaryTextColor
                            )
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor
                            )
                        }
                    }

                    // Separator dot
                    if (chapter.dateOfRelease != null && isLastRead && !isSelectionMode) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                        )
                    }

                    // Continue reading hint
                    if (isLastRead && !isSelectionMode) {
                        Text(
                            text = "Continue reading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterStatusIcons(
    isDownloaded: Boolean,
    isRead: Boolean,
    isSelectionMode: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Download status indicator
        if (isDownloaded) {
            Surface(
                shape = CircleShape,
                color = Success.copy(alpha = if (isRead) 0.12f else 0.18f)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Downloaded",
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp),
                    tint = Success.copy(alpha = if (isRead) 0.6f else 1f)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = "Not downloaded",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isRead) 0.3f else 0.4f
                )
            )
        }

        // Read status (only when not in selection mode)
        if (!isSelectionMode && isRead) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Read",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}