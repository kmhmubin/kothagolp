package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.GenreOption
import com.kmhmubin.kothagolp.recommendation.model.OnboardingGenres
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.RecommendationViewModel
import com.kmhmubin.kothagolp.ui.theme.AppShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenrePreferencesSheet(
    viewModel: RecommendationViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var likedGenres by remember { mutableStateOf(setOf<TagCategory>()) }
    var dislikedGenres by remember { mutableStateOf(setOf<TagCategory>()) }
    var includeMature by remember { mutableStateOf(false) }
    var includeBL by remember { mutableStateOf(true) }
    var includeGL by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val (liked, disliked, content) = viewModel.getCurrentGenrePrefs()
        likedGenres = liked
        dislikedGenres = disliked
        includeMature = content.first
        includeBL = content.second
        includeGL = content.third
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Preferences",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Update your taste to improve recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(Icons.Rounded.Close, "Close", modifier = Modifier.size(18.dp))
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Genre sections
                    item {
                        GenrePrefSectionHeader(
                            title = "Core Genres",
                            subtitle = "Tap once to like, twice to dislike, again to clear"
                        )
                    }
                    item {
                        GenreGrid(
                            genres = OnboardingGenres.mainGenres,
                            likedGenres = likedGenres,
                            dislikedGenres = dislikedGenres,
                            onToggle = { cat ->
                                when {
                                    cat in likedGenres -> {
                                        likedGenres = likedGenres - cat
                                        dislikedGenres = dislikedGenres + cat
                                    }
                                    cat in dislikedGenres -> {
                                        dislikedGenres = dislikedGenres - cat
                                    }
                                    else -> {
                                        likedGenres = likedGenres + cat
                                    }
                                }
                            }
                        )
                    }
                    item {
                        GenrePrefSectionHeader(
                            title = "Web Novel Genres",
                            subtitle = null
                        )
                    }
                    item {
                        GenreGrid(
                            genres = OnboardingGenres.subGenres,
                            likedGenres = likedGenres,
                            dislikedGenres = dislikedGenres,
                            onToggle = { cat ->
                                when {
                                    cat in likedGenres -> {
                                        likedGenres = likedGenres - cat
                                        dislikedGenres = dislikedGenres + cat
                                    }
                                    cat in dislikedGenres -> {
                                        dislikedGenres = dislikedGenres - cat
                                    }
                                    else -> {
                                        likedGenres = likedGenres + cat
                                    }
                                }
                            }
                        )
                    }

                    // Content filters
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    item {
                        GenrePrefSectionHeader(title = "Content Filters", subtitle = null)
                    }
                    item {
                        ContentFilterCard(
                            title = "Mature Content",
                            subtitle = "Adult, NSFW, Smut",
                            enabled = includeMature,
                            onToggle = { includeMature = it }
                        )
                    }
                    item {
                        ContentFilterCard(
                            title = "Boys Love (BL)",
                            subtitle = "Yaoi, M/M romance",
                            enabled = includeBL,
                            onToggle = { includeBL = it }
                        )
                    }
                    item {
                        ContentFilterCard(
                            title = "Girls Love (GL)",
                            subtitle = "Yuri, F/F romance",
                            enabled = includeGL,
                            onToggle = { includeGL = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Apply button
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            viewModel.applyGenrePreferences(
                                liked = likedGenres,
                                disliked = dislikedGenres,
                                includeMature = includeMature,
                                includeBL = includeBL,
                                includeGL = includeGL
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = AppShape.medium
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Preferences", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreGrid(
    genres: List<GenreOption>,
    likedGenres: Set<TagCategory>,
    dislikedGenres: Set<TagCategory>,
    onToggle: (TagCategory) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            val isLiked = genre.category in likedGenres
            val isDisliked = genre.category in dislikedGenres

            val containerColor by animateColorAsState(
                targetValue = when {
                    isLiked -> MaterialTheme.colorScheme.primaryContainer
                    isDisliked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "chip_color"
            )
            val contentColor = when {
                isLiked -> MaterialTheme.colorScheme.onPrimaryContainer
                isDisliked -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = { onToggle(genre.category) },
                shape = AppShape.extraLarge,
                color = containerColor,
                border = if (!isLiked && !isDisliked) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLiked) {
                        Icon(
                            Icons.Rounded.ThumbUp,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = contentColor
                        )
                    } else if (isDisliked) {
                        Icon(
                            Icons.Rounded.ThumbDown,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = contentColor
                        )
                    } else {
                        genre.icon?.let { emoji ->
                            Text(emoji, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = genre.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLiked || isDisliked) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GenrePrefSectionHeader(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContentFilterCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = AppShape.large,
        color = if (enabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = { onToggle(!enabled) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
