package com.kmhmubin.kothagolp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.ui.theme.AppElevation
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.StatusCompleted
import com.kmhmubin.kothagolp.ui.theme.StatusDROPPED
import com.kmhmubin.kothagolp.ui.theme.StatusOnHold
import com.kmhmubin.kothagolp.ui.theme.StatusPlanToRead
import com.kmhmubin.kothagolp.ui.theme.StatusReading
import com.kmhmubin.kothagolp.ui.theme.StatusSpicy

// ══════════════════════════════════════════════════════════════════════════════
// Design Tokens
// ══════════════════════════════════════════════════════════════════════════════

private object ListItemTokens {
    val CardShape = AppShape.large
    val ImageShape = AppShape.medium
    val BadgeShape = AppShape.small
    val PillShape = AppShape.pill

    object Height {
        val Compact = 100.dp
        val Default = 120.dp
        val Comfortable = 140.dp
    }

    object ImageWidth {
        val Compact = 70.dp
        val Default = 85.dp
        val Comfortable = 100.dp
    }

    object Padding {
        val Compact = 10.dp
        val Default = 12.dp
        val Comfortable = 14.dp
        val Badge = 6.dp
    }

    object Elevation {
        val Resting = AppElevation.sm
        val Pressed = 1.dp
        val Selected = AppElevation.md
        val Badge = AppElevation.md
    }

    object Animation {
        const val PressScale = 0.98f
        const val ShimmerDuration = 1400
    }

    val StatusDotSize = 10.dp
    val BadgeIconSize = 12.dp
}

// ══════════════════════════════════════════════════════════════════════════════
// Main List Item Component
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelListItem(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT,
    onLongClick: (() -> Unit)? = null,
    newChapterCount: Int = 0,
    readingStatus: ReadingStatus? = null,
    lastReadChapter: String? = null,
    showApiName: Boolean = false,
    isSelected: Boolean = false,
    isInLibrary: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) ListItemTokens.Animation.PressScale else 1f
    val elevation = when {
        isPressed -> ListItemTokens.Elevation.Pressed
        isSelected -> ListItemTokens.Elevation.Selected
        else -> ListItemTokens.Elevation.Resting
    }

    val cardHeight = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Height.Compact
        UiDensity.DEFAULT -> ListItemTokens.Height.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Height.Comfortable
    }

    val imageWidth = when (density) {
        UiDensity.COMPACT -> ListItemTokens.ImageWidth.Compact
        UiDensity.DEFAULT -> ListItemTokens.ImageWidth.Default
        UiDensity.COMFORTABLE -> ListItemTokens.ImageWidth.Comfortable
    }

    val contentPadding = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Padding.Compact
        UiDensity.DEFAULT -> ListItemTokens.Padding.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Padding.Comfortable
    }

    val semanticsLabel = buildString {
        append(novel.name)
        readingStatus?.let { append(", ${it.displayName()}") }
        if (newChapterCount > 0) append(", $newChapterCount new chapters")
        if (isInLibrary) append(", in library")
        lastReadChapter?.let { append(", last read: $it") }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isSelected) {
                    Modifier.listItemBorder(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        cornerRadius = 16.dp
                    )
                } else Modifier
            )
            .semantics {
                contentDescription = semanticsLabel
                role = Role.Button
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        shape = ListItemTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image with overlay
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f, matchHeightConstraintsFirst = true)
                    .clip(ListItemTokens.ImageShape)
            ) {
                ListItemCoverImage(
                    url = novel.posterUrl,
                    title = novel.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle vignette for badge contrast
                ListItemVignette(modifier = Modifier.fillMaxSize())

                // Library and new chapter badges on image
                if (isInLibrary || newChapterCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(ListItemTokens.Padding.Badge),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isInLibrary) ListLibraryBookmarkBadge(compact = density == UiDensity.COMPACT)
                        if (newChapterCount > 0) ListNewChaptersBadge(count = newChapterCount, compact = density == UiDensity.COMPACT)
                    }
                }

                // Status indicator at bottom-left of image
                if (readingStatus != null && density == UiDensity.COMPACT) {
                    ListStatusDot(
                        status = readingStatus,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(ListItemTokens.Padding.Badge)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title
                    Text(
                        text = novel.name,
                        style = when (density) {
                            UiDensity.COMPACT -> MaterialTheme.typography.bodyMedium
                            UiDensity.DEFAULT -> MaterialTheme.typography.titleSmall
                            UiDensity.COMFORTABLE -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = when (density) {
                            UiDensity.COMPACT -> 16.sp
                            UiDensity.DEFAULT -> 18.sp
                            UiDensity.COMFORTABLE -> 22.sp
                        }
                    )

                    // Source
                    if (showApiName && novel.apiName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                            Text(
                                text = novel.apiName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom row: Status + Last read chapter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge (not in compact mode - it's shown on image)
                    if (readingStatus != null && density != UiDensity.COMPACT) {
                        ListStatusBadge(status = readingStatus, compact = false)
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Last read chapter with styled indicator
                    lastReadChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                        ListChapterProgress(
                            chapterName = chapter,
                            compact = density == UiDensity.COMPACT,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // Animated chevron indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = if (isPressed) 2.dp.toPx() else 0f
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isPressed) 0.8f else 0.6f
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Cover Image Component
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListItemCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val context = LocalContext.current
    val colorHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorHighest = MaterialTheme.colorScheme.surfaceContainerHighest
    val errorGradient = remember(colorHigh, colorHighest) {
        Brush.verticalGradient(colors = listOf(colorHigh, colorHighest))
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { imageState = it }
        )
        when {
            imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { alpha = 0.2f },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            imageState is AsyncImagePainter.State.Error || url.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(errorGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = title.take(8),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Overlays
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListItemVignette(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            val gradient = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = 0.15f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.2f)
                )
            )
            onDrawBehind { drawRect(gradient) }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Status Badges
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListStatusBadge(
    status: ReadingStatus,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusColor = remember(status) {
        when (status) {
            ReadingStatus.READING -> StatusReading
            ReadingStatus.SPICY -> StatusSpicy
            ReadingStatus.COMPLETED -> StatusCompleted
            ReadingStatus.ON_HOLD -> StatusOnHold
            ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
            ReadingStatus.DROPPED -> StatusDROPPED
        }
    }

    if (compact) {
        ListStatusDot(status = status, modifier = modifier)
    } else {
        Surface(
            modifier = modifier,
            shape = ListItemTokens.BadgeShape,
            color = statusColor.copy(alpha = 0.15f),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Small dot indicator
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = status.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ListStatusDot(
    status: ReadingStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = remember(status) {
        when (status) {
            ReadingStatus.READING -> StatusReading
            ReadingStatus.SPICY -> StatusSpicy
            ReadingStatus.COMPLETED -> StatusCompleted
            ReadingStatus.ON_HOLD -> StatusOnHold
            ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
            ReadingStatus.DROPPED -> StatusDROPPED
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(ListItemTokens.StatusDotSize)
                    .drawWithCache {
                        val r = size.minDimension / 2
                        val c1 = statusColor.copy(alpha = 0.15f)
                        val c2 = statusColor.copy(alpha = 0.35f)
                        val solid = statusColor
                        onDrawBehind {
                            drawCircle(color = c1, radius = r * 2.2f)
                            drawCircle(color = c2, radius = r * 1.5f)
                            drawCircle(color = solid, radius = r)
                        }
                    }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// New Chapters Badge
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListLibraryBookmarkBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // Reuse the details cover treatment so "in library" reads consistently across screens.
    Surface(
        modifier = modifier.size(if (compact) 20.dp else 22.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = "In library",
                modifier = Modifier.size(if (compact) 11.dp else 13.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ListNewChaptersBadge(
    count: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val displayText = remember(count) {
        when {
            count > 99 -> "99+"
            else -> "+$count"
        }
    }

    Surface(
        modifier = modifier,
        shape = if (compact) CircleShape else AppShape.extraSmall,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 0.dp
    ) {
        if (compact) {
            Box(
                modifier = Modifier.padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 8.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(ListItemTokens.BadgeIconSize),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Chapter Progress
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListChapterProgress(
    chapterName: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Reading progress indicator dot
        Box(
            modifier = Modifier
                .size(if (compact) 4.dp else 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = chapterName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (compact) 10.sp else 11.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Skeleton Loading State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelListItemSkeleton(
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT
) {
    val cardHeight = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Height.Compact
        UiDensity.DEFAULT -> ListItemTokens.Height.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Height.Comfortable
    }

    val imageWidth = when (density) {
        UiDensity.COMPACT -> ListItemTokens.ImageWidth.Compact
        UiDensity.DEFAULT -> ListItemTokens.ImageWidth.Default
        UiDensity.COMFORTABLE -> ListItemTokens.ImageWidth.Comfortable
    }

    val contentPadding = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Padding.Compact
        UiDensity.DEFAULT -> ListItemTokens.Padding.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Padding.Comfortable
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = ListItemTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ListItemTokens.Elevation.Resting)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f, matchHeightConstraintsFirst = true)
                    .clip(ListItemTokens.ImageShape)
                    .shimmerEffect()
            )

            // Content placeholders
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Title line 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(AppShape.extraSmall)
                            .shimmerEffect()
                    )
                    // Title line 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(AppShape.extraSmall)
                            .shimmerEffect()
                    )
                    // Source
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(AppShape.extraSmall)
                            .shimmerEffect()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status placeholder
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(22.dp)
                            .clip(AppShape.small)
                            .shimmerEffect()
                    )
                    // Chapter placeholder
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(AppShape.extraSmall)
                            .shimmerEffect()
                    )
                }
            }

            // Chevron placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Utilities
// ══════════════════════════════════════════════════════════════════════════════

private fun Modifier.listItemBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
): Modifier = this.then(
    Modifier.drawWithCache {
        onDrawBehind {
            drawRoundRect(
                color = color,
                style = Stroke(width.toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
)
