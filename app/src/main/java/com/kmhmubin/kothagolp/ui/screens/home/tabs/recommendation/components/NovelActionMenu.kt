package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.recommendation.model.Recommendation
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy
import java.util.Locale

/** Long-press sheet: why a novel is here, Open | + Library, Not for me, Hide everything by this author. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NovelActionMenu(
    recommendation: Recommendation,
    onDismiss: () -> Unit,
    onNotInterested: () -> Unit,
    onBlockAuthor: (() -> Unit)?,
    onViewDetails: () -> Unit,
    onAddToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val novel = recommendation.novel
    val b = recommendation.scoreBreakdown
    fun act(action: () -> Unit) = { action(); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CoverImage(novel.posterUrl, novel.name, Modifier.width(76.dp).height(112.dp))
                Column {
                    Text(
                        novel.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            novel.apiName.takeIf { it.isNotBlank() },
                            novel.rating?.let { "★ " + String.format(Locale.US, "%.1f", it / 200f) }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        if (recommendation.score >= 0.75f) ForYouCopy.STRONG_MATCH else ForYouCopy.GOOD_MATCH,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(ForYouCopy.WHY_HEADER, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (recommendation.reason.isNotBlank()) {
                        Text(
                            recommendation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // AI picks carry no score breakdown: show the reason only.
                    if (b.tagSimilarity + b.authorMatch + b.providerBoost + b.ratingScore > 0f) {
                        WhyBar(ForYouCopy.WHY_YOUR_GENRES, b.tagSimilarity)
                        WhyBar(ForYouCopy.WHY_LIKE_FAVOURITES, b.authorMatch)
                        if (b.providerBoost > 0f) WhyBar(ForYouCopy.WHY_READERS, b.providerBoost)
                        WhyBar(ForYouCopy.WHY_RATING, b.ratingScore)
                    }
                }
            }

            ButtonGroup(
                overflowIndicator = {},
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
                customItem(
                    buttonGroupContent = {
                        Button(
                            onClick = act(onViewDetails),
                            shapes = ButtonDefaults.shapes(ButtonGroupDefaults.connectedLeadingButtonShape, ButtonGroupDefaults.connectedLeadingButtonPressShape),
                            modifier = Modifier.weight(1f)
                        ) { Text(ForYouCopy.ACTION_OPEN) }
                    },
                    menuContent = {}
                )
                customItem(
                    buttonGroupContent = {
                        FilledTonalButton(
                            onClick = act(onAddToLibrary),
                            shapes = ButtonDefaults.shapes(ButtonGroupDefaults.connectedTrailingButtonShape, ButtonGroupDefaults.connectedTrailingButtonPressShape),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Text(ForYouCopy.ACTION_ADD_LIBRARY, modifier = Modifier.padding(start = 6.dp))
                        }
                    },
                    menuContent = {}
                )
            }

            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val n = if (onBlockAuthor != null) 2 else 1
                ActionRow(Icons.Rounded.VisibilityOff, ForYouCopy.ACTION_NOT_FOR_ME, MaterialTheme.colorScheme.onSurface, top = true, bottom = n == 1, onClick = act(onNotInterested))
                if (onBlockAuthor != null) {
                    ActionRow(Icons.Rounded.PersonOff, ForYouCopy.ACTION_HIDE_AUTHOR, MaterialTheme.colorScheme.error, top = false, bottom = true, onClick = act(onBlockAuthor))
                }
            }
        }
    }
}

/** One row of a 2dp-gapped list whose outer corners are 20dp and inner corners 4dp. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, color: Color, top: Boolean, bottom: Boolean, onClick: () -> Unit) {
    val big = 20.dp
    val small = 4.dp
    Surface(
        shape = RoundedCornerShape(
            topStart = if (top) big else small, topEnd = if (top) big else small,
            bottomStart = if (bottom) big else small, bottomEnd = if (bottom) big else small
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = color,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WhyBar(label: String, value: Float) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), label = "why_$label")
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        LinearProgressIndicator(
            progress = { v },
            modifier = Modifier.weight(1f).height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {}
        )
    }
}
