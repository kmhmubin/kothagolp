package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouItem

val CoverShape = RoundedCornerShape(16.dp)

/** Row title (20sp semibold) with an optional filled tonal "see all" arrow, and an optional subtitle. */
@Composable
fun ForYouRowHeader(
    title: String,
    subtitle: String?,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onSeeAll != null) {
                FilledTonalIconButton(onClick = onSeeAll, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, ForYouCopy.SEE_ALL, Modifier.size(20.dp))
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 106dp cover, 156dp tall, 16dp corners, 2-line title and 2-line caption. No badges. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForYouCover(
    item: ForYouItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier
            .width(106.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    { haptic.performHapticFeedback(HapticFeedbackType.LongPress); it() }
                }
            )
    ) {
        CoverImage(item.posterUrl, item.title, Modifier.fillMaxWidth().height(156.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (item.caption.isNotBlank()) {
            Text(
                text = item.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun CoverImage(url: String?, title: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CoverShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/** Horizontal row of covers. Items without a [ForYouItem.recommendation] (AI picks) have no long-press. */
@Composable
fun ForYouCoverRow(
    items: List<ForYouItem>,
    onOpen: (ForYouItem) -> Unit,
    onLongPress: (ForYouItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            ForYouCover(
                item = item,
                onClick = { onOpen(item) },
                onLongClick = item.recommendation?.let { { onLongPress(item) } }
            )
        }
    }
}
