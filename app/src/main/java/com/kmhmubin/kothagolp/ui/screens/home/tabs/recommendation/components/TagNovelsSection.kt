package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kmhmubin.kothagolp.data.local.entity.DiscoveredNovelEntity
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.OnboardingGenres
import com.kmhmubin.kothagolp.ui.theme.AppShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagNovelsSection(
    topPreferences: List<String>,
    selectedTag: TagCategory?,
    tagNovels: List<DiscoveredNovelEntity>,
    isLoadingTagNovels: Boolean,
    onTagClick: (TagCategory) -> Unit,
    onNovelClick: (novelUrl: String, providerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Browse by Genre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (selectedTag != null) {
                TextButton(onClick = { onTagClick(selectedTag) }) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Tag chips horizontal scroll
        val tagCategories = topPreferences.mapNotNull { name ->
            TagNormalizer.normalize(name)
        }.take(10).ifEmpty {
            // Fallback: use a subset of all genres
            (OnboardingGenres.mainGenres + OnboardingGenres.subGenres).map { it.category }.take(8)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tagCategories, key = { it.name }) { cat ->
                val isSelected = selectedTag == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagClick(cat) },
                    label = {
                        Text(
                            text = TagNormalizer.getDisplayName(cat),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Expanded novel row
        AnimatedVisibility(
            visible = selectedTag != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (isLoadingTagNovels) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                } else if (tagNovels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No novels found for this genre yet. Browse more to build your pool.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tagNovels, key = { it.url }) { novel ->
                            TagNovelCard(
                                novel = novel,
                                onClick = {
                                    if (novel.url.isNotBlank() && novel.apiName.isNotBlank()) {
                                        onNovelClick(novel.url, novel.apiName)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagNovelCard(
    novel: DiscoveredNovelEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = AppShape.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.width(120.dp)
    ) {
        Column {
            AsyncImage(
                model = novel.posterUrl,
                contentDescription = novel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(AppShape.medium)
            )
            Text(
                text = novel.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
