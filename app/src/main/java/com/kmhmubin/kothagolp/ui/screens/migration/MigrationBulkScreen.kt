package com.kmhmubin.kothagolp.ui.screens.migration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationBulkScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: MigrationBulkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.progress) {
        if (uiState.progress is BulkMigrationProgress.Done) {
            val done = uiState.progress as BulkMigrationProgress.Done
            if (done.succeeded > 0) onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Migrate all from ${viewModel.sourceName}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.targetSource != null) {
                            Text(
                                text = "→ ${uiState.targetSource}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.targetSource != null && !uiState.isSearching &&
                uiState.progress is BulkMigrationProgress.Idle
            ) {
                Surface(shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (uiState.foundCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.selectedCount} of ${uiState.foundCount} selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                )
                                if (uiState.notFoundCount > 0) {
                                    Text(
                                        text = "${uiState.notFoundCount} not found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { viewModel.showConfirmDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.selectedCount > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Migrate ${uiState.selectedCount} novels")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val prog = uiState.progress) {
                is BulkMigrationProgress.Running -> {
                    MigrationRunningScreen(done = prog.done, total = prog.total)
                }
                is BulkMigrationProgress.Done -> {
                    MigrationDoneScreen(
                        succeeded = prog.succeeded,
                        failed = prog.failed,
                        onDone = onComplete
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            TargetSourceSelector(
                                availableSources = uiState.availableSources,
                                selectedSource = uiState.targetSource,
                                isLoading = uiState.isLoadingNovels,
                                onSelect = { viewModel.setTargetSource(it) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (uiState.targetSource != null) {
                            if (uiState.isSearching) {
                                item {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Searching ${uiState.searchingCount} novels…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            items(uiState.novels) { item ->
                                BulkMigrationRow(
                                    item = item,
                                    onToggle = { viewModel.toggleSelection(item.fromEntry.url) }
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Select a target source above",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.showConfirmDialog) {
                BulkConfirmDialog(
                    fromSource = viewModel.sourceName,
                    toSource = uiState.targetSource ?: "",
                    count = uiState.selectedCount,
                    onConfirm = { viewModel.startBulkMigration() },
                    onDismiss = { viewModel.dismissConfirmDialog() }
                )
            }
        }
    }
}

@Composable
private fun TargetSourceSelector(
    availableSources: List<String>,
    selectedSource: String?,
    isLoading: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Target source",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedSource ?: "Select source…",
                        modifier = Modifier.weight(1f),
                        color = if (selectedSource != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableSources.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source) },
                            onClick = {
                                expanded = false
                                onSelect(source)
                            },
                            trailingIcon = if (source == selectedSource) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkMigrationRow(
    item: BulkMigrationItem,
    onToggle: () -> Unit
) {
    val canSelect = item.searchState is BulkSearchState.Found

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.searchState) {
                is BulkSearchState.Found -> if (item.selected)
                    MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canSelect, onClick = onToggle)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection toggle
            if (canSelect) {
                Icon(
                    imageVector = if (item.selected) Icons.Rounded.CheckCircle
                    else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            // Cover
            val coverUrl = item.fromEntry.customCoverUrl ?: item.fromEntry.posterUrl
            if (coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(coverUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 36.dp, height = 48.dp).clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 36.dp, height = 48.dp).clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Bookmarks, contentDescription = null,
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.width(10.dp))

            // From name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fromEntry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (!item.selected && canSelect) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                AnimatedContent(
                    targetState = item.searchState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "search_state"
                ) { state ->
                    when (state) {
                        is BulkSearchState.Idle, is BulkSearchState.Searching -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("Searching…", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        is BulkSearchState.Found -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null,
                                    modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = state.match.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        is BulkSearchState.NotFound -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Close, contentDescription = null,
                                    modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text("Not found in target source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is BulkSearchState.Error -> {
                            Text("Search error: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkConfirmDialog(
    fromSource: String,
    toSource: String,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null) },
        title = { Text("Migrate $count novels?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(fromSource, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(toSource, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
                Text(
                    text = "Reading progress and history will be transferred for each novel. Original entries will be removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Migrate all")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MigrationRunningScreen(done: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text("Migrating novels…", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("$done / $total", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) done.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MigrationDoneScreen(
    succeeded: Int,
    failed: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Migration complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("$succeeded migrated", style = MaterialTheme.typography.bodyLarge)
        if (failed > 0) {
            Text("$failed failed", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}
