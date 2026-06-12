package com.kmhmubin.kothagolp.ui.screens.tagexplorer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.ui.components.NovelCard
import com.kmhmubin.kothagolp.ui.components.NovelGridSkeleton
import com.kmhmubin.kothagolp.ui.screens.tagexplorer.components.TagSelectorSheet
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import com.kmhmubin.kothagolp.util.calculateGridColumns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// Design Constants (matching ProviderBrowseScreen)
// ============================================================================

private object BrowseDesign {
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 20.dp
    val radiusXxl = 28.dp

    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 16.dp
    val spacingXl = 20.dp
    val spacingXxl = 24.dp

    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val iconXl = 40.dp

    val buttonHeight = 44.dp
    val chipHeight = 36.dp
    val fabSize = 56.dp
}

// ============================================================================
// Shimmer Effect Extension
// ============================================================================

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    )

    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnimation - 500f, 0f),
                end = Offset(translateAnimation, 0f)
            ),
            blendMode = BlendMode.SrcAtop
        )
    }
}

// ============================================================================
// Tag Colors
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
        TagNormalizer.TagCategory.MARTIAL_ARTS to (Color(0xFFDC2626) to Color(0xFFEF4444))
    )

    private val defaultColor = Color(0xFF6366F1) to Color(0xFF818CF8)

    fun getColors(tag: TagNormalizer.TagCategory?): Pair<Color, Color> {
        return tag?.let { colorPalette[it] } ?: defaultColor
    }

    fun getColor(tag: TagNormalizer.TagCategory?): Color = getColors(tag).first
}

// ============================================================================
// Main Tag Explorer Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagExplorerScreen(
    tagName: String,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onNovelClick: (novelUrl: String, providerName: String) -> Unit,
    viewModel: TagExplorerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tagNovelsCount by viewModel.tagNovelsCount.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showFilterOverlay by remember { mutableStateOf(false) }
    var showTagSelector by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val gridColumns = calculateGridColumns(appSettings.browseGridColumns)

    val (primaryColor, secondaryColor) = remember(uiState.tag) {
        TagColors.getColors(uiState.tag)
    }

    // Load novels when tag changes
    LaunchedEffect(tagName) {
        val tagCategory = try {
            TagNormalizer.TagCategory.valueOf(tagName)
        } catch (e: Exception) {
            null
        }

        tagCategory?.let {
            viewModel.loadNovelsForTag(it)
        }
    }

    // Auto-open filter overlay if no results and has active filters
    LaunchedEffect(uiState.filteredNovels.isEmpty(), uiState.hasActiveFilters) {
        if (uiState.filteredNovels.isEmpty() && uiState.hasActiveFilters && !uiState.isLoading) {
            delay(500)
            showFilterOverlay = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Clickable tag name to open selector
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTagSelector = true
                            },
                            color = Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(BrowseDesign.radiusSm))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    vertical = BrowseDesign.spacingXs,
                                    horizontal = BrowseDesign.spacingSm
                                )
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = primaryColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(8.dp)
                                ) {}

                                Text(
                                    text = uiState.tagDisplayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Change tag",
                                    modifier = Modifier.size(BrowseDesign.iconMd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = uiState.isLoading to uiState.filteredNovels.size,
                            label = "novel_count",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { (loading, count) ->
                            if (!loading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = BrowseDesign.spacingSm)
                                ) {
                                    Text(
                                        text = "$count novel${if (count != 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (uiState.hasActiveFilters) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Filtered",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = primaryColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    uiState.tag?.let { viewModel.loadNovelsForTag(it) }
                    delay(1000)
                    isRefreshing = false
                }
            },
            state = rememberPullToRefreshState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active filter chips
                    AnimatedVisibility(
                        visible = uiState.hasActiveFilters && !showFilterOverlay,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ActiveFiltersChipRow(
                            uiState = uiState,
                            primaryColor = primaryColor,
                            onSortClear = { viewModel.setSortOption(SortOption.NAME) },
                            onRatingClear = { viewModel.setMinRating(0f) },
                            onProvidersClear = { viewModel.clearProviderFilter() },
                            onClearAll = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.clearFilters()
                            }
                        )
                    }

                    // Content states
                    AnimatedContent(
                        targetState = Triple(
                            uiState.isLoading,
                            uiState.error != null && uiState.novels.isEmpty(),
                            uiState.filteredNovels.isEmpty() && !uiState.isLoading
                        ),
                        label = "content_state",
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        }
                    ) { (isLoading, hasError, isEmpty) ->
                        when {
                            isLoading -> {
                                LoadingState(
                                    tagName = uiState.tagDisplayName,
                                    color = primaryColor,
                                    gridColumns = gridColumns,
                                    density = appSettings.uiDensity
                                )
                            }

                            hasError -> {
                                ErrorState(
                                    tagName = uiState.tagDisplayName,
                                    message = uiState.error ?: "No novels found",
                                    color = primaryColor,
                                    onRetry = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        uiState.tag?.let { viewModel.loadNovelsForTag(it) }
                                    }
                                )
                            }

                            isEmpty -> {
                                NoResultsState(
                                    hasFilters = uiState.hasActiveFilters,
                                    color = primaryColor,
                                    onClearFilters = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.clearFilters()
                                    }
                                )
                            }

                            else -> {
                                MainContent(
                                    uiState = uiState,
                                    gridColumns = gridColumns,
                                    primaryColor = primaryColor,
                                    appSettings = appSettings,
                                    onNovelClick = { novel ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNovelClick(novel.url, novel.apiName)
                                    }
                                )
                            }
                        }
                    }
                }

                // Filter FAB
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = BrowseDesign.spacingXl,
                            bottom = BrowseDesign.spacingXl
                        )
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(
                        visible = !uiState.isLoading,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        FilterFab(
                            isOpen = showFilterOverlay,
                            hasActiveFilters = uiState.hasActiveFilters,
                            activeFilterCount = uiState.activeFilterCount,
                            color = primaryColor,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFilterOverlay = !showFilterOverlay
                            }
                        )
                    }
                }

                // Filter Overlay
                FilterOverlay(
                    visible = showFilterOverlay,
                    uiState = uiState,
                    primaryColor = primaryColor,
                    onDismiss = { showFilterOverlay = false },
                    onSortChange = { viewModel.setSortOption(it) },
                    onMinRatingChange = { viewModel.setMinRating(it) },
                    onToggleProvider = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleProvider(it)
                    },
                    onClearProviders = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearProviderFilter()
                    },
                    onClearFilters = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearFilters()
                        showFilterOverlay = false
                    },
                    onApply = { showFilterOverlay = false }
                )
            }
        }
    }

    // Tag Selector Sheet
    if (showTagSelector) {
        TagSelectorSheet(
            currentTag = uiState.tag,
            tagNovelsCount = tagNovelsCount,
            onDismiss = { showTagSelector = false },
            onTagSelected = { newTag ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.loadNovelsForTag(newTag)
                showTagSelector = false
            }
        )
    }
}

// ============================================================================
// Filter FAB
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterFab(
    isOpen: Boolean,
    hasActiveFilters: Boolean,
    activeFilterCount: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_rotation"
    )

    val fabColor by animateColorAsState(
        targetValue = when {
            isOpen -> MaterialTheme.colorScheme.primary
            hasActiveFilters -> color
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "fab_color"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isOpen -> MaterialTheme.colorScheme.onPrimary
            hasActiveFilters -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "fab_icon_color"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(BrowseDesign.radiusLg),
            color = fabColor,
            shadowElevation = if (isOpen) 12.dp else 6.dp,
            modifier = Modifier
                .size(BrowseDesign.fabSize)
                .semantics {
                    contentDescription = if (isOpen) "Close filters" else "Open filters"
                    role = Role.Button
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isOpen) Icons.Rounded.Close else Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(BrowseDesign.iconLg)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }

        // Badge
        AnimatedVisibility(
            visible = activeFilterCount > 0 && !isOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = activeFilterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.dp.value.toInt().sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// Filter Overlay (Unified Sort + Filters)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterOverlay(
    visible: Boolean,
    uiState: TagExplorerUiState,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSortChange: (SortOption) -> Unit,
    onMinRatingChange: (Float) -> Unit,
    onToggleProvider: (String) -> Unit,
    onClearProviders: () -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit
) {
    // Scrim backdrop
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // Bottom sheet
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(150)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(
                    topStart = BrowseDesign.radiusXxl,
                    topEnd = BrowseDesign.radiusXxl
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(vertical = BrowseDesign.spacingMd),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    // Header
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = BrowseDesign.spacingXxl,
                                        vertical = BrowseDesign.spacingLg
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Filters & Sort",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    AnimatedContent(
                                        targetState = uiState.activeFilterCount,
                                        transitionSpec = {
                                            (slideInVertically { -it } + fadeIn())
                                                .togetherWith(slideOutVertically { it } + fadeOut())
                                        },
                                        label = "filter_count"
                                    ) { count ->
                                        Text(
                                            text = when {
                                                count == 0 -> "No active filters"
                                                count == 1 -> "1 filter applied"
                                                else -> "$count filters applied"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (count > 0)
                                                primaryColor
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Reset button
                                AnimatedVisibility(
                                    visible = uiState.hasActiveFilters,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut()
                                ) {
                                    Surface(
                                        onClick = onClearFilters,
                                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = BrowseDesign.spacingMd,
                                                vertical = BrowseDesign.spacingSm
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Reset filters",
                                                modifier = Modifier.size(BrowseDesign.iconSm),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "Reset",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = BrowseDesign.spacingXxl)
                            .padding(top = BrowseDesign.spacingXl, bottom = BrowseDesign.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXxl)
                    ) {

                        // ─── Sort Section ───
                        OverlayFilterSection(
                            title = "Sort By",
                            icon = Icons.AutoMirrored.Rounded.Sort,
                            accentColor = MaterialTheme.colorScheme.primary
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                            ) {
                                SortOption.entries.forEach { option ->
                                    OverlayFilterChip(
                                        text = option.displayName,
                                        selected = uiState.sortBy == option,
                                        onClick = { onSortChange(option) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // ─── Rating Section ───
                        OverlayFilterSection(
                            title = "Minimum Rating",
                            icon = Icons.Rounded.Star,
                            accentColor = Color(0xFFFBBF24)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (uiState.minRating > 0) {
                                            "%.1f★ and above".format(uiState.minRating)
                                        } else {
                                            "Any rating"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.minRating > 0) {
                                            Color(0xFFFBBF24)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Slider(
                                    value = uiState.minRating,
                                    onValueChange = onMinRatingChange,
                                    valueRange = 0f..5f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFBBF24),
                                        activeTrackColor = Color(0xFFFBBF24),
                                        inactiveTrackColor = Color(0xFFFBBF24).copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }

                        // ─── Providers Section ───
                        if (uiState.availableProviders.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            OverlayFilterSection(
                                title = "Sources",
                                icon = Icons.Rounded.Category,
                                accentColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
                                    if (uiState.selectedProviders.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${uiState.selectedProviders.size} selected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                onClick = onClearProviders,
                                                shape = RoundedCornerShape(BrowseDesign.radiusSm),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = "Clear",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(
                                                        horizontal = BrowseDesign.spacingSm,
                                                        vertical = BrowseDesign.spacingXs
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                                    ) {
                                        uiState.availableProviders.forEach { provider ->
                                            val isSelected = provider in uiState.selectedProviders
                                            val showSelected = isSelected || uiState.selectedProviders.isEmpty()

                                            OverlayFilterChip(
                                                text = provider,
                                                selected = showSelected,
                                                onClick = { onToggleProvider(provider) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(BrowseDesign.spacingXl))
                    }

                    // Apply Button
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Button(
                                onClick = onApply,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(BrowseDesign.spacingXxl)
                                    .height(BrowseDesign.buttonHeight),
                                shape = RoundedCornerShape(BrowseDesign.radiusMd),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconMd)
                                )
                                Spacer(Modifier.width(BrowseDesign.spacingSm))
                                Text(
                                    text = "Apply Filters",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helper Composables
// ============================================================================

@Composable
private fun OverlayFilterSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(BrowseDesign.radiusSm),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = accentColor
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "chip_text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = bgColor,
        modifier = Modifier.height(BrowseDesign.chipHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally() + fadeIn() + scaleIn(),
                exit = shrinkHorizontally() + fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(BrowseDesign.iconSm),
                    tint = textColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ============================================================================
// Active Filters Chip Row
// ============================================================================

@Composable
private fun ActiveFiltersChipRow(
    uiState: TagExplorerUiState,
    primaryColor: Color,
    onSortClear: () -> Unit,
    onRatingClear: () -> Unit,
    onProvidersClear: () -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BrowseDesign.spacingLg, vertical = BrowseDesign.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Active:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sort chip
            if (uiState.sortBy != SortOption.NAME) {
                item {
                    DismissibleChip(
                        label = uiState.sortBy.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        onDismiss = onSortClear
                    )
                }
            }

            // Rating chip
            if (uiState.minRating > 0) {
                item {
                    DismissibleChip(
                        label = "%.1f★+".format(uiState.minRating),
                        color = Color(0xFFFBBF24),
                        onDismiss = onRatingClear
                    )
                }
            }

            // Providers chip
            if (uiState.selectedProviders.isNotEmpty()) {
                item {
                    DismissibleChip(
                        label = "${uiState.selectedProviders.size} source${if (uiState.selectedProviders.size != 1) "s" else ""}",
                        color = MaterialTheme.colorScheme.secondary,
                        onDismiss = onProvidersClear
                    )
                }
            }

            // Clear all
            item {
                Surface(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(BrowseDesign.radiusSm),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Clear all",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(
                            horizontal = BrowseDesign.spacingSm,
                            vertical = BrowseDesign.spacingXs
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleChip(
    label: String,
    color: Color,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusSm),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(
                start = BrowseDesign.spacingSm,
                end = BrowseDesign.spacingXs,
                top = BrowseDesign.spacingXs,
                bottom = BrowseDesign.spacingXs
            ),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(10.dp),
                        tint = color
                    )
                }
            }
        }
    }
}

// ============================================================================
// Main Content
// ============================================================================

@Composable
private fun MainContent(
    uiState: TagExplorerUiState,
    gridColumns: Int,
    primaryColor: Color,
    appSettings: AppSettings,
    onNovelClick: (com.kmhmubin.kothagolp.domain.model.Novel) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions

    Column(modifier = Modifier.fillMaxSize()) {
        // Stats header
        TagStatsHeader(
            totalNovels = uiState.novels.size,
            filteredNovels = uiState.filteredNovels.size,
            providersCount = uiState.availableProviders.size,
            color = primaryColor,
            modifier = Modifier.padding(
                horizontal = BrowseDesign.spacingLg,
                vertical = BrowseDesign.spacingMd
            )
        )

        // Novel grid/list
        when (appSettings.browseDisplayMode) {
            com.kmhmubin.kothagolp.domain.model.DisplayMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                    verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "spacer_top") {
                        Spacer(Modifier.height(BrowseDesign.spacingSm))
                    }
                    items(items = uiState.filteredNovels, key = { it.url }) { novel ->
                        NovelCard(
                            novel = novel,
                            onClick = { onNovelClick(novel) },
                            onLongClick = { },
                            density = appSettings.uiDensity,
                            modifier = Modifier.padding(horizontal = dimensions.gridPadding / 2)
                        )
                    }
                }
            }

            com.kmhmubin.kothagolp.domain.model.DisplayMode.LIST -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
                ) {
                    item(key = "spacer_top") {
                        Spacer(Modifier.height(BrowseDesign.spacingSm))
                    }
                    items(uiState.filteredNovels, key = { it.url }) { novel ->
                        com.kmhmubin.kothagolp.ui.components.NovelListItem(
                            novel = novel,
                            onClick = { onNovelClick(novel) },
                            onLongClick = { },
                            density = appSettings.uiDensity,
                            modifier = Modifier.padding(horizontal = dimensions.gridPadding / 2)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Tag Stats Header
// ============================================================================

@Composable
private fun TagStatsHeader(
    totalNovels: Int,
    filteredNovels: Int,
    providersCount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
    ) {
        StatBadge(
            value = "$filteredNovels",
            label = if (totalNovels != filteredNovels) "of $totalNovels" else "novels",
            color = color,
            modifier = Modifier.weight(1f)
        )

        StatBadge(
            value = "$providersCount",
            label = "sources",
            color = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBadge(
    value: String,
    label: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = BrowseDesign.spacingMd,
                vertical = BrowseDesign.spacingMd
            ),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// Loading State
// ============================================================================

@Composable
private fun LoadingState(
    tagName: String,
    color: Color,
    gridColumns: Int,
    density: com.kmhmubin.kothagolp.domain.model.UiDensity
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BrowseDesign.spacingLg),
        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg),
                modifier = Modifier.padding(vertical = BrowseDesign.spacingXxl)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = rotation },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = color
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs)
                ) {
                    Text(
                        text = "Discovering $tagName novels...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Searching across all sources",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        NovelGridSkeleton(
            columns = gridColumns,  // USE 'columns' instead
            count = 12,
            density = density,
            contentPadding = PaddingValues(0.dp)
        )
    }
}

// ============================================================================
// Error State
// ============================================================================

@Composable
private fun ErrorState(
    tagName: String,
    message: String,
    color: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(BrowseDesign.spacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(BrowseDesign.radiusXl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(BrowseDesign.spacingXxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconXl),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                ) {
                    Text(
                        text = "No novels found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "We couldn't find any novels tagged with \"$tagName\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(BrowseDesign.radiusMd),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    contentPadding = PaddingValues(
                        horizontal = BrowseDesign.spacingXl,
                        vertical = BrowseDesign.spacingMd
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                    Text("Try Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ============================================================================
// No Results State
// ============================================================================

@Composable
private fun NoResultsState(
    hasFilters: Boolean,
    color: Color,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(BrowseDesign.spacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(BrowseDesign.radiusXl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(BrowseDesign.spacingXxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (hasFilters) {
                                Icons.Rounded.FilterAltOff
                            } else {
                                Icons.Rounded.SearchOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconXl),
                            tint = color
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                ) {
                    Text(
                        text = if (hasFilters) "No matching novels" else "No results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (hasFilters) {
                            "Try adjusting your filters to see more results"
                        } else {
                            "No novels match your current criteria"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (hasFilters) {
                    Button(
                        onClick = onClearFilters,
                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        contentPadding = PaddingValues(
                            horizontal = BrowseDesign.spacingXl,
                            vertical = BrowseDesign.spacingMd
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                        Text("Clear Filters", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}