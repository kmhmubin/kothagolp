package com.kmhmubin.kothagolp.ui.screens.tagexplorer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import kotlinx.coroutines.launch

// ============================================================================
// Tag Colors (matching TagExplorerScreen)
// ============================================================================

private object TagColors {
    private val colorPalette = mapOf(
        TagNormalizer.TagCategory.ACTION to (Color(0xFFEF4444) to Color(0xFFF87171)),
        TagNormalizer.TagCategory.ADVENTURE to (Color(0xFFF97316) to Color(0xFFFB923C)),
        TagNormalizer.TagCategory.ROMANCE to (Color(0xFFEC4899) to Color(0xFFF472B6)),
        TagNormalizer.TagCategory.FANTASY to (Color(0xFF8B5CF6) to Color(0xFFA78BFA)),
        TagNormalizer.TagCategory.SCI_FI to (Color(0xFF06B6D4) to Color(0xFF22D3EE)),
        TagNormalizer.TagCategory.MYSTERY to (Color(0xFF6366F1) to Color(0xFF818CF8)),
        TagNormalizer.TagCategory.HORROR to (Color(0xFF7C3AED) to Color(0xFF9333EA)),
        TagNormalizer.TagCategory.COMEDY to (Color(0xFFF59E0B) to Color(0xFFFBBF24)),
        TagNormalizer.TagCategory.DRAMA to (Color(0xFF10B981) to Color(0xFF34D399)),
        TagNormalizer.TagCategory.MARTIAL_ARTS to (Color(0xFFDC2626) to Color(0xFFEF4444)),
        TagNormalizer.TagCategory.CULTIVATION to (Color(0xFF8B5CF6) to Color(0xFFA78BFA)),
        TagNormalizer.TagCategory.ISEKAI to (Color(0xFF3B82F6) to Color(0xFF60A5FA)),
        TagNormalizer.TagCategory.LITRPG to (Color(0xFF10B981) to Color(0xFF34D399)),
        TagNormalizer.TagCategory.BL to (Color(0xFFEC4899) to Color(0xFFF472B6)),
        TagNormalizer.TagCategory.GL to (Color(0xFFEC4899) to Color(0xFFF472B6))
    )

    private val defaultColor = Color(0xFF6366F1) to Color(0xFF818CF8)

    fun getColors(tag: TagNormalizer.TagCategory): Pair<Color, Color> {
        return colorPalette[tag] ?: defaultColor
    }

    fun getColor(tag: TagNormalizer.TagCategory): Color = getColors(tag).first
}

// ============================================================================
// Tag Selector Bottom Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSelectorSheet(
    currentTag: TagNormalizer.TagCategory?,
    tagNovelsCount: Map<TagNormalizer.TagCategory, Int>,
    onDismiss: () -> Unit,
    onTagSelected: (TagNormalizer.TagCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<TagNormalizer.TagGroup?>(null) }

    val listState = rememberLazyListState()

    // Get all tags organized by group
    val groupedTags = remember { TagNormalizer.getAllTagsByGroup() }

    // Get popular tags (tags with most novels)
    val popularTags = remember(tagNovelsCount) {
        tagNovelsCount.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { it.key }
    }

    // Get recently browsed tags (you can enhance this with actual history)
    val suggestedTags = remember {
        listOf(
            TagNormalizer.TagCategory.CULTIVATION,
            TagNormalizer.TagCategory.ISEKAI,
            TagNormalizer.TagCategory.LITRPG,
            TagNormalizer.TagCategory.ROMANCE,
            TagNormalizer.TagCategory.ACTION
        )
    }

    // Filter tags based on search and selected group - FIXED VERSION
    val filteredGroups by remember(searchQuery, selectedGroup, groupedTags) {
        derivedStateOf {
            val groups = selectedGroup?.let { group ->
                // Safe access - only include the selected group
                mapOf(group to (groupedTags[group] ?: emptyList()))
            } ?: groupedTags  // If null, use all groups

            if (searchQuery.isBlank()) {
                groups
            } else {
                groups.mapValues { (_, tags) ->
                    tags.filter { tag ->
                        TagNormalizer.getDisplayName(tag)
                            .contains(searchQuery, ignoreCase = true)
                    }
                }.filterValues { it.isNotEmpty() }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Browse Tags",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tagNovelsCount.size} tags available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedGroup != null) {
                    TextButton(onClick = {
                        selectedGroup = null
                        searchQuery = ""
                    }) {
                        Text("Show All", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search tags...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Content
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Popular tags section (only show when not searching/filtering)
                if (searchQuery.isBlank() && selectedGroup == null && popularTags.isNotEmpty()) {
                    item(key = "popular_section") {
                        TagQuickSection(
                            title = "Popular",
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            color = MaterialTheme.colorScheme.primary,
                            tags = popularTags,
                            currentTag = currentTag,
                            tagNovelsCount = tagNovelsCount,
                            onTagClick = { tag ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTagSelected(tag)
                                scope.launch { sheetState.hide() }
                                onDismiss()
                            }
                        )
                    }
                }

                // Suggested/Quick access (only show when not searching)
                if (searchQuery.isBlank() && selectedGroup == null) {
                    item(key = "suggested_section") {
                        TagQuickSection(
                            title = "Quick Access",
                            icon = Icons.Rounded.AutoAwesome,
                            color = MaterialTheme.colorScheme.secondary,
                            tags = suggestedTags,
                            currentTag = currentTag,
                            tagNovelsCount = tagNovelsCount,
                            onTagClick = { tag ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTagSelected(tag)
                                scope.launch { sheetState.hide() }
                                onDismiss()
                            }
                        )
                    }
                }

                // Group filters (only show when not searching)
                if (searchQuery.isBlank() && selectedGroup == null) {
                    item(key = "group_filters") {
                        TagGroupFilters(
                            selectedGroup = selectedGroup,
                            onGroupSelected = { group ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedGroup = group
                            }
                        )
                    }
                }

                // All tags grouped
                filteredGroups.forEach { (group, tags) ->
                    if (tags.isNotEmpty()) {
                        item(key = "group_${group.name}") {
                            TagGroupSection(
                                group = group,
                                tags = tags,
                                currentTag = currentTag,
                                tagNovelsCount = tagNovelsCount,
                                onTagClick = { tag ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTagSelected(tag)
                                    scope.launch { sheetState.hide() }
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                // Empty state
                if (filteredGroups.all { it.value.isEmpty() }) {
                    item(key = "empty") {
                        EmptyTagSearchState(
                            query = searchQuery,
                            onClearSearch = { searchQuery = "" }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ============================================================================
// Quick Access Section
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagQuickSection(
    title: String,
    icon: ImageVector,
    color: Color,
    tags: List<TagNormalizer.TagCategory>,
    currentTag: TagNormalizer.TagCategory?,
    tagNovelsCount: Map<TagNormalizer.TagCategory, Int>,
    onTagClick: (TagNormalizer.TagCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                TagChip(
                    tag = tag,
                    novelCount = tagNovelsCount[tag] ?: 0,
                    isSelected = tag == currentTag,
                    onClick = { onTagClick(tag) }
                )
            }
        }
    }
}

// ============================================================================
// Group Filters
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGroupFilters(
    selectedGroup: TagNormalizer.TagGroup?,
    onGroupSelected: (TagNormalizer.TagGroup) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Browse by Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show most relevant groups
            val displayGroups = listOf(
                TagNormalizer.TagGroup.MAIN_GENRES,
                TagNormalizer.TagGroup.EASTERN,
                TagNormalizer.TagGroup.ISEKAI_REINCARNATION,
                TagNormalizer.TagGroup.LITRPG_GAMELIT,
                TagNormalizer.TagGroup.ROMANCE_TYPES,
                TagNormalizer.TagGroup.LGBTQ,
                TagNormalizer.TagGroup.SETTING,
                TagNormalizer.TagGroup.THEMES
            )

            displayGroups.forEach { group ->
                FilterChip(
                    selected = selectedGroup == group,
                    onClick = { onGroupSelected(group) },
                    label = {
                        Text(
                            group.displayName,
                            fontWeight = if (selectedGroup == group) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

// ============================================================================
// Tag Group Section
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGroupSection(
    group: TagNormalizer.TagGroup,
    tags: List<TagNormalizer.TagCategory>,
    currentTag: TagNormalizer.TagCategory?,
    tagNovelsCount: Map<TagNormalizer.TagCategory, Int>,
    onTagClick: (TagNormalizer.TagCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Group header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
            )
        }

        // Tags grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                TagChip(
                    tag = tag,
                    novelCount = tagNovelsCount[tag] ?: 0,
                    isSelected = tag == currentTag,
                    onClick = { onTagClick(tag) }
                )
            }
        }
    }
}

// ============================================================================
// Tag Chip Component
// ============================================================================

@Composable
private fun TagChip(
    tag: TagNormalizer.TagCategory,
    novelCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (primaryColor, secondaryColor) = remember(tag) {
        TagColors.getColors(tag)
    }

    val containerColor = if (isSelected) {
        primaryColor.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val borderColor = if (isSelected) {
        primaryColor.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.height(if (novelCount > 0) 48.dp else 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Tag name
            Text(
                text = TagNormalizer.getDisplayName(tag),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
            )

            // Novel count badge
            if (novelCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) {
                        primaryColor.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Text(
                        text = "$novelCount",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// Empty Search State
// ============================================================================

@Composable
private fun EmptyTagSearchState(
    query: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "No tags found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "No tags match \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            TextButton(onClick = onClearSearch) {
                Text("Clear Search")
            }
        }
    }
}