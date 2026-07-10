package com.kmhmubin.kothagolp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.domain.model.UiDensity
import com.kmhmubin.kothagolp.ui.screens.details.util.StatusUtils
import com.kmhmubin.kothagolp.ui.theme.AppShape

// ══════════════════════════════════════════════════════════════════════════════
// Design Tokens — Komikku/Mihon-style flat grid item
// ══════════════════════════════════════════════════════════════════════════════

private object NovelCardTokens {
    /** Outer selectable clip — Komikku clips the whole grid cell */
    val ItemShape = AppShape.medium          // 12dp
    /** Cover corner rounding */
    val CoverShape = AppShape.small          // 8dp
    /** Badge group rounding (badges inside share one clipped row) */
    val BadgeGroupShape = AppShape.extraSmall // 4dp

    val AspectRatio = 2f / 3f
    /** Inner padding: makes room for the selection frame, doubles as grid gap */
    val ItemPadding = 4.dp
    val BadgeHeight = 18.dp
    val BadgeIconSize = 12.dp

    /** Komikku dims the cover when the item is selected */
    const val SelectedCoverAlpha = 0.76f
    const val PressScale = 0.97f

    const val ShimmerDuration = 1400
}

// ══════════════════════════════════════════════════════════════════════════════
// Main Entry Point
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelCard(
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
    val semanticsLabel = buildString {
        append(novel.name)
        readingStatus?.let { append(", ${it.displayName()}") }
        if (newChapterCount > 0) append(", $newChapterCount new chapters")
        if (isInLibrary) append(", in library")
        lastReadChapter?.let { append(", last read: $it") }
    }
    val semanticsModifier = modifier.semantics {
        contentDescription = semanticsLabel
        role = Role.Button
    }

    when (density) {
        UiDensity.COMFORTABLE -> ComfortableNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = semanticsModifier,
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName,
            isSelected = isSelected,
            isInLibrary = isInLibrary
        )
        else -> CompactNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = semanticsModifier,
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName,
            isSelected = isSelected,
            isInLibrary = isInLibrary
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Selectable wrapper (Komikku GridItemSelectable)
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) NovelCardTokens.PressScale else 1f
    val selectionColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(NovelCardTokens.ItemShape)
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
            )
            .drawBehind { if (isSelected) drawRect(color = selectionColor) }
            .padding(NovelCardTokens.ItemPadding)
    ) {
        content()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Compact Layout — title overlays the cover (Komikku compact grid)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CompactNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    newChapterCount: Int,
    readingStatus: ReadingStatus?,
    lastReadChapter: String?,
    showApiName: Boolean,
    isSelected: Boolean,
    isInLibrary: Boolean
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        NovelGridCover(
            novel = novel,
            coverAlpha = if (isSelected) NovelCardTokens.SelectedCoverAlpha else 1f,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            isInLibrary = isInLibrary
        ) {
            CoverTextOverlay(
                title = novel.name,
                subtitle = lastReadChapter?.takeIf { it.isNotBlank() }
                    ?: novel.apiName.takeIf { showApiName && it.isNotBlank() }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Comfortable Layout — title below the cover (Komikku comfortable grid)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ComfortableNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    newChapterCount: Int,
    readingStatus: ReadingStatus?,
    lastReadChapter: String?,
    showApiName: Boolean,
    isSelected: Boolean,
    isInLibrary: Boolean
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        Column {
            NovelGridCover(
                novel = novel,
                coverAlpha = if (isSelected) NovelCardTokens.SelectedCoverAlpha else 1f,
                newChapterCount = newChapterCount,
                readingStatus = readingStatus,
                isInLibrary = isInLibrary
            )
            Text(
                text = novel.name,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            lastReadChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                Text(
                    text = chapter,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showApiName && novel.apiName.isNotBlank()) {
                Text(
                    text = novel.apiName,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Cover with badge groups (Komikku MangaGridCover)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NovelGridCover(
    novel: Novel,
    coverAlpha: Float,
    newChapterCount: Int,
    readingStatus: ReadingStatus?,
    isInLibrary: Boolean,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(NovelCardTokens.AspectRatio)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(coverAlpha)
                .clip(NovelCardTokens.CoverShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            NovelCoverImage(
                url = novel.posterUrl,
                title = novel.name,
                modifier = Modifier.fillMaxSize()
            )
            content?.invoke()
        }

        // Top-start: new chapter count (Komikku unread badge)
        if (newChapterCount > 0) {
            BadgeGroup(modifier = Modifier.align(Alignment.TopStart)) {
                Badge(text = if (newChapterCount > 99) "99+" else "$newChapterCount")
            }
        }

        // Top-end: shelf icon + in-library marker
        if (readingStatus != null || isInLibrary) {
            BadgeGroup(modifier = Modifier.align(Alignment.TopEnd)) {
                if (readingStatus != null) {
                    Badge(
                        imageVector = StatusUtils.getStatusIcon(readingStatus),
                        color = StatusUtils.getStatusColor(readingStatus),
                        contentColor = Color.White,
                        contentDescription = readingStatus.displayName()
                    )
                }
                if (isInLibrary) {
                    Badge(
                        imageVector = Icons.Rounded.Bookmark,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        contentDescription = "In library"
                    )
                }
            }
        }
    }
}

/**
 * Bottom gradient + title, Komikku compact-grid style.
 */
@Composable
private fun CoverTextOverlay(
    title: String,
    subtitle: String? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xAA000000)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    shadow = Shadow(color = Color.Black, blurRadius = 4f)
                ),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.78f),
                        shadow = Shadow(color = Color.Black, blurRadius = 4f)
                    ),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Badges (Komikku Badge / BadgeGroup)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BadgeGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .height(NovelCardTokens.BadgeHeight)
            .clip(NovelCardTokens.BadgeGroupShape),
        content = content
    )
}

@Composable
private fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun Badge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(NovelCardTokens.BadgeIconSize)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Cover image with placeholder / fallback
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NovelCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val context = LocalContext.current

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
                CoverPlaceholder()
            }
            imageState is AsyncImagePainter.State.Error || url.isNullOrBlank() -> {
                CoverFallback(title = title)
            }
        }
    }
}

@Composable
private fun CoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .shimmerEffect(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoStories,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer { alpha = 0.2f },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoverFallback(title: String, modifier: Modifier = Modifier) {
    val colorHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorHighest = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val gradient = Brush.verticalGradient(colors = listOf(colorHigh, colorHighest))
                onDrawBehind { drawRect(gradient) }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = title.take(20).ifEmpty { "Novel" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Skeleton / Loading State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelCardSkeleton(
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT
) {
    Box(modifier = modifier.padding(NovelCardTokens.ItemPadding)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(NovelCardTokens.AspectRatio)
                    .clip(NovelCardTokens.CoverShape)
                    .shimmerEffect()
            )
            if (density == UiDensity.COMFORTABLE) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 4.dp)
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(AppShape.extraSmall)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp)
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(AppShape.extraSmall)
                        .shimmerEffect()
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shimmer Effect
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHighest
    val surfaceMid = MaterialTheme.colorScheme.surfaceContainerHigh
    val shimmerColors = remember(surfaceHigh, surfaceMid) {
        listOf(surfaceHigh, surfaceMid.copy(alpha = 0.7f), surfaceHigh)
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = NovelCardTokens.ShimmerDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return this.drawBehind {
        val shimmerWidth = size.width * 0.4f
        val startX = -shimmerWidth + (size.width + shimmerWidth * 2) * translateAnim
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(startX, 0f),
            end = Offset(startX + shimmerWidth, size.height)
        )
        drawRect(brush)
    }
}
