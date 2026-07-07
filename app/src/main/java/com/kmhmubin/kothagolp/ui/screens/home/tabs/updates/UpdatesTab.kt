package com.kmhmubin.kothagolp.ui.screens.home.tabs.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.data.repository.LibraryItem
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.ui.components.NovelCard
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdatesTab(
    appSettings: AppSettings? = null,
    onNovelClick: (url: String, title: String) -> Unit,
    onNovelLongClick: (url: String, title: String) -> Unit
) {
    val viewModel: UpdatesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimensions = KothagolpTheme.dimensions
    val settings = appSettings ?: AppSettings()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (uiState.totalNewChapters > 0) {
                    Text(
                        text = "${uiState.totalNewChapters} new chapter${if (uiState.totalNewChapters != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }

        // Content
        val error = uiState.error
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error loading updates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            uiState.updates.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No new updates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = dimensions.gridPadding,
                        vertical = dimensions.cardSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
                ) {
                    items(
                        items = uiState.updates,
                        key = { item -> item.novel.url }
                    ) { item ->
                        UpdateCard(
                            item = item,
                            onNovelClick = {
                                onNovelClick(item.novel.url, item.novel.name)
                            },
                            onNovelLongClick = {
                                onNovelLongClick(item.novel.url, item.novel.name)
                            },
                            uiDensity = settings.uiDensity
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    item: LibraryItem,
    onNovelClick: () -> Unit,
    onNovelLongClick: () -> Unit,
    uiDensity: com.kmhmubin.kothagolp.domain.model.UiDensity
) {
    val dimensions = KothagolpTheme.dimensions
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = AppShape.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onNovelClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.cardSpacing),
            horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            NovelCard(
                novel = item.novel,
                onClick = onNovelClick,
                onLongClick = onNovelLongClick,
                isInLibrary = true,
                density = uiDensity,
                modifier = Modifier
                    .width(80.dp)
                    .height(100.dp)
            )

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.novel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.newChapterCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShape.medium
                        ) {
                            Text(
                                text = "+${item.newChapterCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(4.dp, 2.dp)
                            )
                        }
                    }
                    Text(
                        text = formatUpdateTime(item.lastUpdatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatUpdateTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} h ago"
        diff < 604_800_000 -> "${diff / 86_400_000} d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
