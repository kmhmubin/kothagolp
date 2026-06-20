package com.kmhmubin.kothagolp.ui.screens.reader.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.ProgressStyle
import com.kmhmubin.kothagolp.ui.screens.reader.theme.ReaderColors
import com.kmhmubin.kothagolp.ui.screens.reader.theme.ReaderDefaults
import com.kmhmubin.kothagolp.ui.theme.AppShape

// =============================================================================
// TOP BAR
// =============================================================================

@Composable
fun ReaderTopBar(
    chapterTitle: String,
    chapterNumber: Int,
    totalChapters: Int,
    isBookmarked: Boolean,
    chapterProgress: Float,
    estimatedTimeLeft: String?,
    progressStyle: ProgressStyle = ProgressStyle.BAR,
    largerTouchTargets: Boolean = false,
    isAutoScrollActive: Boolean = false,
    onBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAutoScrollClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val buttonSize = if (largerTouchTargets) 56.dp else 48.dp

    val displayTimeLeft = estimatedTimeLeft?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = ReaderDefaults.TopBarElevation,
        shadowElevation = ReaderDefaults.TopBarElevation
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding.calculateTopPadding())
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgressIndicator(
                            progress = chapterProgress,
                            chapterNumber = chapterNumber,
                            totalChapters = totalChapters,
                            estimatedTimeLeft = displayTimeLeft,
                            style = progressStyle
                        )
                    }
                }

                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = if (isBookmarked)
                            Icons.Default.Bookmark
                        else
                            Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAutoScrollClick,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = if (isAutoScrollActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoScrollActive) "Stop auto-scroll" else "Start auto-scroll",
                        tint = if (isAutoScrollActive) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (progressStyle == ProgressStyle.BAR) {
                LinearProgressIndicator(
                    progress = { chapterProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicator(
    progress: Float,
    chapterNumber: Int,
    totalChapters: Int,
    estimatedTimeLeft: String?,
    style: ProgressStyle
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    when (style) {
        ProgressStyle.NONE -> Unit

        ProgressStyle.BAR -> {
            Text(
                text = "$chapterNumber / $totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = secondary
            )
            Text(
                text = " • ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = secondary
            )
            if (estimatedTimeLeft != null) {
                Text(text = " • ", style = MaterialTheme.typography.labelSmall, color = secondary)
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = secondary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = estimatedTimeLeft, style = MaterialTheme.typography.labelSmall, color = secondary)
            }
        }

        ProgressStyle.PERCENTAGE -> {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = primary
            )
            Text(text = " • Ch. $chapterNumber/$totalChapters", style = MaterialTheme.typography.labelSmall, color = secondary)
            if (estimatedTimeLeft != null) {
                Text(text = " • $estimatedTimeLeft", style = MaterialTheme.typography.labelSmall, color = secondary)
            }
        }

        ProgressStyle.PAGES -> {
            Text(
                text = "~${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = secondary
            )
            Text(text = " • Ch. $chapterNumber/$totalChapters", style = MaterialTheme.typography.labelSmall, color = secondary)
            if (estimatedTimeLeft != null) {
                Text(text = " • $estimatedTimeLeft left", style = MaterialTheme.typography.labelSmall, color = secondary)
            }
        }

        ProgressStyle.DOTS -> {
            DotsProgressIndicator(progress = progress)
            if (estimatedTimeLeft != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = estimatedTimeLeft, style = MaterialTheme.typography.labelSmall, color = secondary)
            }
        }

        ProgressStyle.TIME_LEFT -> {
            if (estimatedTimeLeft != null) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = estimatedTimeLeft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = primary)
            } else {
                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = primary)
            }
            Text(text = " • Ch. $chapterNumber", style = MaterialTheme.typography.labelSmall, color = secondary)
        }
    }
}

@Composable
private fun DotsProgressIndicator(
    progress: Float,
    dotCount: Int = 5
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val dotProgress = (index + 1).toFloat() / dotCount
            val isActive = progress >= dotProgress
            val isPartial = !isActive && progress > (index.toFloat() / dotCount)

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> primary
                            isPartial -> primary.copy(alpha = 0.4f)
                            else -> track
                        }
                    )
            )
        }
    }
}

// =============================================================================
// TTS ACTIVE INDICATOR
// =============================================================================

@Composable
fun TTSActiveIndicator(
    colors: ReaderColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShape.large,
        color = colors.accent.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AudioWaveIndicator(color = colors.accent)
            Text(
                text = "Playing",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun AudioWaveIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedHeight by animateFloatAsState(
                targetValue = when ((System.currentTimeMillis() / 200 + index) % 3) {
                    0L -> 8f
                    1L -> 12f
                    else -> 6f
                },
                animationSpec = tween(150),
                label = "wave_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}