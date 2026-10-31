package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import com.kmhmubin.kothagolp.ui.theme.AppShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.data.local.entity.BlockedAuthorEntity
import com.kmhmubin.kothagolp.data.local.entity.HiddenNovelEntity
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy

/** "Hidden" from the For You menu: the two lists the user can bring back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSettingsSheet(
    hiddenNovels: List<HiddenNovelEntity>,
    blockedAuthors: List<BlockedAuthorEntity>,
    onUnhideNovel: (String) -> Unit,
    onUnblockAuthor: (String) -> Unit,
    onClearAllHidden: () -> Unit,
    onClearAllBlocked: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<SettingsSection?>(null) }
    fun toggle(s: SettingsSection) { expandedSection = if (expandedSection == s) null else s }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = ForYouCopy.MENU_HIDDEN,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExpandableSettingCard(
                    icon = Icons.Rounded.VisibilityOff,
                    title = ForYouCopy.HIDDEN_NOVELS,
                    subtitle = if (hiddenNovels.isEmpty()) ForYouCopy.NOTHING_HIDDEN else ForYouCopy.novels(hiddenNovels.size),
                    count = hiddenNovels.size,
                    isExpanded = expandedSection == SettingsSection.HIDDEN_NOVELS,
                    onToggleExpand = { toggle(SettingsSection.HIDDEN_NOVELS) }
                ) {
                    HiddenNovelsContent(hiddenNovels = hiddenNovels, onUnhide = onUnhideNovel, onClearAll = onClearAllHidden)
                }
            }
            item {
                ExpandableSettingCard(
                    icon = Icons.Rounded.PersonOff,
                    title = ForYouCopy.HIDDEN_AUTHORS,
                    subtitle = if (blockedAuthors.isEmpty()) ForYouCopy.NOTHING_HIDDEN else ForYouCopy.authors(blockedAuthors.size),
                    count = blockedAuthors.size,
                    isExpanded = expandedSection == SettingsSection.BLOCKED_AUTHORS,
                    onToggleExpand = { toggle(SettingsSection.BLOCKED_AUTHORS) }
                ) {
                    BlockedAuthorsContent(blockedAuthors = blockedAuthors, onUnblock = onUnblockAuthor, onClearAll = onClearAllBlocked)
                }
            }
        }
    }
}

private enum class SettingsSection {
    HIDDEN_NOVELS,
    BLOCKED_AUTHORS
}

@Composable
private fun ExpandableSettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    count: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Surface(
        shape = AppShape.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShape.extraLarge)
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = AppShape.medium,
                    color = if (count > 0)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (count > 0)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (count > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded content
            if (isExpanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                expandedContent()
            }
        }
    }
}

@Composable
private fun HiddenNovelsContent(
    hiddenNovels: List<HiddenNovelEntity>,
    onUnhide: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (hiddenNovels.isEmpty()) {
            Text(
                text = ForYouCopy.NOTHING_HIDDEN_BODY,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            if (hiddenNovels.size > 1) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Rounded.Visibility,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(ForYouCopy.SHOW_ALL_AGAIN, style = MaterialTheme.typography.labelMedium)
                }
            }

            hiddenNovels.forEach { novel ->
                HiddenNovelItem(novel = novel, onUnhide = { onUnhide(novel.novelUrl) })
            }
        }
    }
}

@Composable
private fun HiddenNovelItem(
    novel: HiddenNovelEntity,
    onUnhide: () -> Unit
) {
    Surface(
        shape = AppShape.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.novelName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FilledTonalButton(
                onClick = onUnhide,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = AppShape.medium
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(ForYouCopy.SHOW_AGAIN, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BlockedAuthorsContent(
    blockedAuthors: List<BlockedAuthorEntity>,
    onUnblock: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (blockedAuthors.isEmpty()) {
            Text(
                text = ForYouCopy.NOTHING_HIDDEN,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            if (blockedAuthors.size > 1) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Rounded.Visibility,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(ForYouCopy.SHOW_ALL_AGAIN, style = MaterialTheme.typography.labelMedium)
                }
            }

            blockedAuthors.forEach { author ->
                BlockedAuthorItem(author = author, onUnblock = { onUnblock(author.authorNormalized) })
            }
        }
    }
}

@Composable
private fun BlockedAuthorItem(
    author: BlockedAuthorEntity,
    onUnblock: () -> Unit
) {
    Surface(
        shape = AppShape.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Author avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = author.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = author.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = onUnblock,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = AppShape.medium
            ) {
                Icon(
                    Icons.Rounded.PersonAdd,
                    null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(ForYouCopy.SHOW_AGAIN, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}