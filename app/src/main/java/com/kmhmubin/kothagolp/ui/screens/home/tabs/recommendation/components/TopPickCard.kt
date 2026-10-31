package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.ui.theme.AppShape

/** Hero card for the single best For You pick, with its reason spelled out. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopPickCard(
    recommendation: Recommendation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onNotInterested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(AppShape.extraLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row {
            AsyncImage(
                model = recommendation.novel.posterUrl,
                contentDescription = recommendation.novel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(118.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Rounded.AutoAwesome, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "TOP PICK", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    recommendation.novel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Box(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    recommendation.novel.rating?.let {
                        Icon(
                            Icons.Filled.Star, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp)
                        )
                        Text(
                            " %.1f · ".format(it / 200f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        recommendation.novel.apiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    TextButton(onClick = onNotInterested) {
                        Text("Not interested", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
