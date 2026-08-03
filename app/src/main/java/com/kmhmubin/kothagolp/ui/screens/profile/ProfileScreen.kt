package com.kmhmubin.kothagolp.ui.screens.profile

import android.content.Intent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.draw.alpha
import coil.compose.AsyncImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.ui.components.KothagolpPullToRefreshBox
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import com.kmhmubin.kothagolp.ui.theme.NewChapters
import com.kmhmubin.kothagolp.ui.theme.AppOrange
import com.kmhmubin.kothagolp.ui.theme.Error
import com.kmhmubin.kothagolp.ui.theme.Info
import com.kmhmubin.kothagolp.ui.theme.StatusOnHold
import com.kmhmubin.kothagolp.ui.theme.StatusPlanToRead
import com.kmhmubin.kothagolp.ui.theme.Success
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

// ============================================================================
// Color tokens
// ============================================================================

private object ProfileColors {
    val StreakOrange = AppOrange
    val GoalPrimary  = Color(0xFF6366F1)
    val ChapterBlue  = Info
    val TimeGreen    = NewChapters
    val AchievementGold = Color(0xFFFFD700)

    fun getLevelColor(level: Int): Color = when (level) {
        1    -> Color(0xFF94A3B8)
        2    -> Success
        3    -> Info
        4    -> StatusPlanToRead
        5    -> StatusOnHold
        6    -> Error
        7    -> AchievementGold
        else -> Color(0xFFE879F9)
    }
}

// ============================================================================
// Screen shell
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onNovelClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadStats()
        viewModel.events.collectLatest { event ->
            when (event) {
                is ProfileEvent.ShareStats -> {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "Share your reading stats"))
                }
                is ProfileEvent.NavigateToNovel -> onNovelClick(event.novelUrl, event.sourceName)
                is ProfileEvent.ShowError -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Stats", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onShareStats() }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share stats")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        KothagolpPullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (!uiState.hasAnyStats && !uiState.isLoading) {
                ProfileEmptyState(Modifier.fillMaxSize())
            } else {
                ProfileContent(
                    uiState = uiState,
                    onNovelClick = { viewModel.onNovelClick(it) },
                    onRecapPeriodChange = { viewModel.setRecapPeriod(it) },
                    onRecapRangeChange = { viewModel.setRecapRange(it) },
                    onGenreModeChange = { viewModel.setGenreMode(it) }
                )
            }
        }
    }
}

// ============================================================================
// Content — 4 sections, all gamification-purpose
//   1. Hero         → who you are (level + XP)
//   2. Streak+Today → daily motivation hook + today's progress
//   3. Heatmap      → 52-week proof of consistency (no scroll)
//   4. Achievements → reward system
// ============================================================================

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNovelClick: (NovelReadingStats) -> Unit,
    onRecapPeriodChange: (RecapPeriod) -> Unit,
    onRecapRangeChange: (RecapRange) -> Unit,
    onGenreModeChange: (GenreMode) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "hero") {
            ProfileHeroSection(uiState)
        }

        uiState.readerType?.let { readerType ->
            item(key = "reader_type") {
                ReaderTypeSection(
                    readerType = readerType,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }

        item(key = "quick_stats") {
            QuickStatsSection(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "streak_today") {
            Row(
                modifier = Modifier
                    .padding(horizontal = dimensions.gridPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreakCard(
                    currentStreak  = uiState.currentStreak,
                    longestStreak  = uiState.longestStreak,
                    isStreakActive = uiState.isStreakActive,
                    modifier       = Modifier.weight(1f)
                )
                TodayCard(
                    todayChapters    = uiState.todayChaptersRead,
                    todayMinutes     = uiState.todayMinutes,
                    dailyGoalMinutes = uiState.dailyGoalMinutes,
                    isStreakActive   = uiState.isStreakActive,
                    currentStreak    = uiState.currentStreak,
                    modifier         = Modifier.weight(1f)
                )
            }
        }

        item(key = "recap") {
            RecapSection(
                recap = uiState.recap,
                period = uiState.recapPeriod,
                range = uiState.recapRange,
                isLoading = uiState.isRecapLoading,
                onPeriodChange = onRecapPeriodChange,
                onRangeChange = onRecapRangeChange,
                onNovelClick = onNovelClick,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "heatmap") {
            ActivityHeatmapSection(
                yearlyActivity = uiState.yearlyActivity,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        if (uiState.topGenres.isNotEmpty()) {
            item(key = "top_genres") {
                TopGenresSection(
                    genres = uiState.topGenres,
                    mode = uiState.genreMode,
                    onModeChange = onGenreModeChange,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }

        if (uiState.mostReadNovels.isNotEmpty()) {
            item(key = "top_novels") {
                TopNovelsSection(
                    novels   = uiState.mostReadNovels,
                    onNovelClick = onNovelClick,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }

        if (uiState.achievements.isNotEmpty()) {
            item(key = "achievements") {
                AchievementsSection(
                    achievements = uiState.achievements,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }
    }
}

// ============================================================================
// 1. Hero — level identity + XP bar
// ============================================================================

@Composable
private fun ProfileHeroSection(uiState: ProfileUiState) {
    val levelColor = ProfileColors.getLevelColor(uiState.readerLevel)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        levelColor.copy(alpha = 0.15f),
                        levelColor.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar + level ring
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(88.dp)) {
                        val sw = 4.dp.toPx()
                        val r = (size.minDimension - sw) / 2
                        val c = Offset(size.width / 2, size.height / 2)
                        drawCircle(color = levelColor.copy(alpha = 0.2f), radius = r, center = c, style = Stroke(sw))
                        drawArc(
                            color = levelColor, startAngle = -90f,
                            sweepAngle = 360f * uiState.levelProgress, useCenter = false,
                            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                            style = Stroke(sw, cap = StrokeCap.Round)
                        )
                    }
                    Surface(shape = CircleShape, color = levelColor.copy(alpha = 0.15f), modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.AutoStories, null, Modifier.size(36.dp), tint = levelColor)
                        }
                    }
                    Surface(
                        shape = CircleShape, color = levelColor,
                        modifier = Modifier.size(28.dp).align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("${uiState.readerLevel}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Level info + XP bar
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(uiState.readerLevelName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = levelColor)
                    Text("Level ${uiState.readerLevel} Reader", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${uiState.totalHours}h read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.hoursToNextLevel > 0)
                            Text("${uiState.hoursToNextLevel}h to next", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val animProg by animateFloatAsState(uiState.levelProgress, spring(stiffness = Spring.StiffnessLow), label = "xp")
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(AppShape.extraSmall).background(levelColor.copy(alpha = 0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(animProg).height(6.dp).clip(AppShape.extraSmall)
                            .background(Brush.horizontalGradient(listOf(levelColor, levelColor.copy(alpha = 0.7f)))))
                    }
                }
            }
        }
    }
}

// ============================================================================
// 1b. Reader Type — personality badge ("Your reader type")
// ============================================================================

@Composable
private fun ReaderTypeSection(
    readerType: ReaderTypeBadge,
    modifier: Modifier = Modifier
) {
    val icon = remember(readerType.iconName) { getAchievementIcon(readerType.iconName) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Your Reader Type", Icons.Rounded.Psychology)

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = ProfileColors.AchievementGold.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ProfileColors.AchievementGold.copy(alpha = 0.18f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(icon, null, Modifier.size(28.dp), tint = ProfileColors.AchievementGold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = readerType.tagline,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = ProfileColors.AchievementGold
                        )
                        Text(
                            text = readerType.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = readerType.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    readerType.traits.forEach { trait ->
                        ReaderTraitChip(trait = trait, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTraitChip(trait: ReaderTrait, modifier: Modifier = Modifier) {
    val icon = remember(trait.iconName) { getAchievementIcon(trait.iconName) }
    Column(
        modifier = modifier
            .clip(AppShape.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = trait.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = trait.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

// ============================================================================
// 2a. Streak card
// ============================================================================

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    isStreakActive: Boolean,
    modifier: Modifier = Modifier
) {
    val streakMsg = when {
        currentStreak == 0  -> "Start today!"
        currentStreak < 3   -> "Great start!"
        currentStreak < 7   -> "Building momentum!"
        currentStreak < 30  -> "On fire!"
        currentStreak < 100 -> "Legendary!"
        else                -> "Unstoppable!"
    }

    Card(
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isStreakActive) ProfileColors.StreakOrange.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Streak",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isStreakActive) ProfileColors.StreakOrange else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "days",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = streakMsg,
                style = MaterialTheme.typography.bodySmall,
                color = if (isStreakActive) ProfileColors.StreakOrange.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (longestStreak > 0) {
                Text(
                    text = "Best: $longestStreak days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ============================================================================
// 2b. Today card
// ============================================================================

@Composable
private fun TodayCard(
    todayChapters: Int,
    todayMinutes: Long,
    dailyGoalMinutes: Int,
    isStreakActive: Boolean,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val dailyProgress = if (dailyGoalMinutes > 0)
        (todayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f) else 0f
    val animDailyProg by animateFloatAsState(dailyProgress, spring(stiffness = Spring.StiffnessLow), label = "goal")

    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "fire_scale"
    )

    val timeStr = remember(todayMinutes) {
        when {
            todayMinutes < 60 -> "${todayMinutes}m"
            else              -> "${todayMinutes / 60}h ${todayMinutes % 60}m"
        }
    }

    Card(
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).then(
                        if (isStreakActive && currentStreak > 0)
                            Modifier.graphicsLayer { scaleX = fireScale; scaleY = fireScale }
                        else Modifier
                    ),
                    tint = if (isStreakActive && currentStreak > 0) ProfileColors.StreakOrange
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, Modifier.size(13.dp), tint = ProfileColors.ChapterBlue)
                Text(
                    text = "$todayChapters chapters",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileColors.ChapterBlue
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Schedule, null, Modifier.size(13.dp), tint = ProfileColors.TimeGreen)
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileColors.TimeGreen
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(AppShape.extraSmall)
                        .background(ProfileColors.GoalPrimary.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animDailyProg).height(4.dp)
                            .clip(AppShape.extraSmall)
                            .background(ProfileColors.GoalPrimary)
                    )
                }
                Text(
                    text = "${(dailyProgress * 100).toInt()}% daily goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ============================================================================
// 2c. Quick Stats — 3-tile summary row (chapters / hours / days)
// ============================================================================

@Composable
private fun QuickStatsSection(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatTile(
            value = formatCompact(uiState.totalChaptersRead.toLong()),
            label = "Chapters",
            icon = Icons.Rounded.MenuBook,
            color = ProfileColors.ChapterBlue,
            modifier = Modifier.weight(1f)
        )
        QuickStatTile(
            value = "${uiState.totalHours}h",
            label = "Total time",
            icon = Icons.Rounded.Schedule,
            color = ProfileColors.TimeGreen,
            modifier = Modifier.weight(1f)
        )
        QuickStatTile(
            value = "${uiState.totalDaysRead}",
            label = "Days read",
            icon = Icons.Rounded.CalendarMonth,
            color = ProfileColors.GoalPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatTile(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun formatCompact(n: Long): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    else           -> n.toString()
}

// ============================================================================
// 2d. Recap — "Your Week/Month, Recapped" (Trakt-style toggleable recap)
// ============================================================================

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(AppShape.pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.pill)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecapSection(
    recap: RecapStats,
    period: RecapPeriod,
    range: RecapRange,
    isLoading: Boolean,
    onPeriodChange: (RecapPeriod) -> Unit,
    onRangeChange: (RecapRange) -> Unit,
    onNovelClick: (NovelReadingStats) -> Unit,
    modifier: Modifier = Modifier
) {
    val periodWord = if (period == RecapPeriod.WEEK) "Week" else "Month"

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Your $periodWord, Recapped", Icons.Rounded.BarChart)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SegmentedToggle(
                options = listOf("Week", "Month"),
                selectedIndex = if (period == RecapPeriod.WEEK) 0 else 1,
                onSelect = { onPeriodChange(if (it == 0) RecapPeriod.WEEK else RecapPeriod.MONTH) },
                modifier = Modifier.weight(1f)
            )
            SegmentedToggle(
                options = listOf("Last $periodWord", "This $periodWord"),
                selectedIndex = if (range == RecapRange.PREVIOUS) 0 else 1,
                onSelect = { onRangeChange(if (it == 0) RecapRange.PREVIOUS else RecapRange.CURRENT) },
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .alpha(if (isLoading) 0.5f else 1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (recap.rangeLabel.isNotEmpty()) {
                    Text(
                        text = recap.rangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }

                // Big headline
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = ProfileColors.TimeGreen.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Schedule, null, Modifier.size(20.dp), tint = ProfileColors.TimeGreen)
                        }
                    }
                    Column {
                        Text(
                            text = "SPENT READING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatMinutes(recap.totalMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Stat chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecapStatChip(
                        value = "${recap.chaptersRead}",
                        label = "Chapters",
                        modifier = Modifier.weight(1f)
                    )
                    RecapStatChip(
                        value = "${recap.daysActive}/${recap.periodLengthDays}",
                        label = "Active days",
                        modifier = Modifier.weight(1f)
                    )
                    RecapStatChip(
                        value = formatMinutes(recap.avgMinutesPerDay),
                        label = "Avg / day",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Highlights
                if (recap.mostActiveDay != null || recap.longestStreakDays > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recap.mostActiveDay?.let { day ->
                            RecapHighlightRow(
                                icon = Icons.Rounded.WorkspacePremium,
                                tint = ProfileColors.AchievementGold,
                                text = "Busiest day: ${day.label} · ${formatMinutes(day.minutes)}"
                            )
                        }
                        if (recap.longestStreakDays > 1) {
                            RecapHighlightRow(
                                icon = Icons.Rounded.LocalFireDepartment,
                                tint = ProfileColors.StreakOrange,
                                text = "${recap.longestStreakDays}-day reading streak this $periodWord"
                            )
                        }
                    }
                }

                // Mini 7-day bar chart (week view only)
                if (period == RecapPeriod.WEEK && recap.dailyBars.isNotEmpty()) {
                    RecapWeekBars(recap.dailyBars)
                }

                // Top novels this period
                if (recap.topNovels.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Top novels this $periodWord".lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        recap.topNovels.forEachIndexed { index, novel ->
                            RecapNovelRow(
                                rank = index + 1,
                                novel = novel,
                                onClick = { onNovelClick(novel) }
                            )
                        }
                    }
                }

                if (recap.totalMinutes <= 0) {
                    Text(
                        text = "No reading recorded for this period yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecapStatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(AppShape.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

@Composable
private fun RecapHighlightRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecapWeekBars(dailyBars: List<Long>) {
    val today = remember { java.time.LocalDate.now() }
    val dayLabels = remember(today) {
        (0..6).map { i ->
            when (today.minusDays((6 - i).toLong()).dayOfWeek) {
                java.time.DayOfWeek.MONDAY    -> "M"
                java.time.DayOfWeek.TUESDAY   -> "T"
                java.time.DayOfWeek.WEDNESDAY -> "W"
                java.time.DayOfWeek.THURSDAY  -> "T"
                java.time.DayOfWeek.FRIDAY    -> "F"
                java.time.DayOfWeek.SATURDAY  -> "S"
                java.time.DayOfWeek.SUNDAY    -> "S"
                else -> "?"
            }
        }
    }
    val maxMinutes = remember(dailyBars) { dailyBars.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceHigh  = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface    = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val maxBarHeight = 64.dp
        Row(
            modifier = Modifier.fillMaxWidth().height(maxBarHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            dailyBars.forEachIndexed { index, minutes ->
                val isToday = index == 6
                val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
                val animFrac by animateFloatAsState(fraction, spring(stiffness = Spring.StiffnessLow), label = "bar_$index")
                val barHeight = (maxBarHeight * animFrac.coerceAtLeast(0.04f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (isToday) primaryColor
                            else if (minutes > 0) primaryColor.copy(alpha = 0.45f)
                            else surfaceHigh
                        )
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayLabels.forEachIndexed { index, label ->
                val isToday = index == 6
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) primaryColor else onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun RecapNovelRow(
    rank: Int,
    novel: NovelReadingStats,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (rank == 1) ProfileColors.AchievementGold.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (rank == 1) ProfileColors.AchievementGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(AppShape.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (!novel.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = novel.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(AppShape.small),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = novel.novelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${novel.chaptersRead} ch · ${formatMinutes(novel.readingTimeMinutes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

private fun formatMinutes(minutes: Long): String = when {
    minutes <= 0   -> "0m"
    minutes < 60   -> "${minutes}m"
    else           -> "${minutes / 60}h ${minutes % 60}m"
}

// ============================================================================
// 2e. Top Novels — most-read novels horizontal scroll
// ============================================================================

@Composable
private fun TopNovelsSection(
    novels: List<NovelReadingStats>,
    onNovelClick: (NovelReadingStats) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Your Top Novels", Icons.Rounded.Star)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(novels) { novel ->
                TopNovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    modifier = Modifier.width(130.dp)
                )
            }
        }
    }
}

@Composable
private fun TopNovelCard(
    novel: NovelReadingStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = AppShape.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(AppShape.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!novel.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(AppShape.medium),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        null,
                        Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Title
            Text(
                text = novel.novelName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stats
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(10.dp), tint = ProfileColors.TimeGreen)
                    Text(
                        text = formatMinutes(novel.readingTimeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = ProfileColors.TimeGreen,
                        fontSize = 10.sp
                    )
                }
                if (novel.chaptersRead > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, null, Modifier.size(10.dp), tint = ProfileColors.ChapterBlue)
                        Text(
                            text = "${novel.chaptersRead} ch",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfileColors.ChapterBlue,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. Activity Heatmap — 52-week calendar
//    Fixed 10dp cells (clearly visible). Horizontally scrollable; auto-scrolls
//    to today so the user sees recent activity immediately on open.
//    Day labels are pinned on the left outside the scroll area.
// ============================================================================

private val MONTH_SHORT = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

@Composable
private fun ActivityHeatmapSection(
    yearlyActivity: Map<Long, Long>,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    val startDate = remember(today) {
        // Align to Monday so each column is Mon→Sun
        val daysBack = 51 * 7 + (today.dayOfWeek.value - 1)
        today.minusDays(daysBack.toLong())
    }

    val weekData: List<List<Pair<LocalDate, Long>>> = remember(yearlyActivity, startDate, today) {
        (0 until 52).map { w ->
            (0 until 7).mapNotNull { d ->
                val date = startDate.plusDays(w * 7L + d)
                if (!date.isAfter(today)) date to (yearlyActivity[date.toEpochDay()] ?: 0L) else null
            }
        }
    }

    // Month label: show name at the first week of each month
    val monthLabels: Map<Int, String> = remember(startDate) {
        buildMap {
            var last = -1
            repeat(52) { w ->
                val m = startDate.plusDays(w * 7L).monthValue
                if (m != last) { put(w, MONTH_SHORT[m - 1]); last = m }
            }
        }
    }

    // Auto-scroll to the rightmost column (today)
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    // Fixed cell dimensions — 10dp is clearly visible without squinting
    val cellDp   = 13.dp
    val gapDp    = 3.dp
    val rowSlot  = cellDp + gapDp   // height of one row + gap

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Activity", Icons.Rounded.CalendarMonth)
            Text(
                text = "Last 12 months",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val emptyColor   = MaterialTheme.colorScheme.surfaceContainerHighest
                val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {

                    // Pinned day-of-week labels (outside scroll)
                    Column(
                        modifier = Modifier.width(20.dp).padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(gapDp)
                    ) {
                        listOf("M", "", "W", "", "F", "", "S").forEach { label ->
                            Box(
                                modifier = Modifier.height(cellDp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (label.isNotEmpty()) {
                                    Text(label, fontSize = 8.sp, color = labelColor)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Scrollable grid (month labels + cell columns)
                    Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

                            // Month label row
                            Row(
                                modifier = Modifier.height(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(gapDp)
                            ) {
                                repeat(52) { w ->
                                    Box(modifier = Modifier.width(cellDp)) {
                                        monthLabels[w]?.let { name ->
                                            Text(
                                                text = name,
                                                fontSize = 9.sp,
                                                color = labelColor.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Week columns
                            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                                weekData.forEach { week ->
                                    Column(
                                        modifier = Modifier.width(cellDp),
                                        verticalArrangement = Arrangement.spacedBy(gapDp)
                                    ) {
                                        week.forEach { (date, minutes) ->
                                            val isToday = date == today
                                            val cellColor = when {
                                                minutes == 0L  -> emptyColor
                                                minutes < 15L  -> primaryColor.copy(alpha = 0.25f)
                                                minutes < 30L  -> primaryColor.copy(alpha = 0.50f)
                                                minutes < 60L  -> primaryColor.copy(alpha = 0.75f)
                                                else           -> primaryColor
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(cellDp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (isToday) Modifier.border(
                                                            1.dp, primaryColor, RoundedCornerShape(2.dp)
                                                        ) else Modifier
                                                    )
                                            )
                                        }
                                        repeat(7 - week.size) { Spacer(Modifier.size(cellDp)) }
                                    }
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Less", fontSize = 9.sp, color = labelColor)
                    Spacer(Modifier.width(4.dp))
                    listOf(0f, 0.25f, 0.50f, 0.75f, 1f).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (alpha == 0f) emptyColor else primaryColor.copy(alpha = alpha))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("More", fontSize = 9.sp, color = labelColor)
                }
            }
        }
    }
}

// ============================================================================
// 3b. Top Genres — segmented bar, sized by titles or time
// ============================================================================

private val GenrePalette = listOf(
    Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFFFFCA28), Color(0xFFFF7043),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFF29B6F6), Color(0xFF26A69A)
)

@Composable
private fun TopGenresSection(
    genres: List<GenreStat>,
    mode: GenreMode,
    onModeChange: (GenreMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Your Top Genres", Icons.Rounded.Category)
            SegmentedToggle(
                options = listOf("Titles", "Time"),
                selectedIndex = if (mode == GenreMode.TITLES) 0 else 1,
                onSelect = { onModeChange(if (it == 0) GenreMode.TITLES else GenreMode.TIME) },
                modifier = Modifier.width(140.dp)
            )
        }

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Segmented bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(AppShape.small),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    genres.forEachIndexed { index, genre ->
                        val animFrac by animateFloatAsState(
                            genre.percentage.coerceIn(0.01f, 1f),
                            spring(stiffness = Spring.StiffnessLow),
                            label = "genre_$index"
                        )
                        Box(
                            modifier = Modifier
                                .weight(animFrac)
                                .fillMaxHeight()
                                .background(GenrePalette[index % GenrePalette.size]),
                            contentAlignment = Alignment.Center
                        ) {
                            if (genre.percentage >= 0.08f) {
                                Text(
                                    text = "${(genre.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Legend — 2 columns
                val rows = genres.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowGenres ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowGenres.forEach { genre ->
                                val index = genres.indexOf(genre)
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GenrePalette[index % GenrePalette.size])
                                    )
                                    Column {
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (mode == GenreMode.TITLES)
                                                "${genre.titleCount} titles" else formatMinutes(genre.minutes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            if (rowGenres.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. Achievements — trophy case
// ============================================================================

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Achievements", Icons.Rounded.EmojiEvents)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            // Show unlocked first
            val sorted = achievements.sortedByDescending { it.isUnlocked }
            items(sorted) { achievement ->
                AchievementCard(achievement = achievement, modifier = Modifier.width(112.dp))
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val icon = remember(achievement.iconName) { getAchievementIcon(achievement.iconName) }
    val animProg by animateFloatAsState(achievement.progress, spring(stiffness = Spring.StiffnessLow), label = "ach")

    Card(
        modifier = modifier,
        shape = AppShape.large,
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                ProfileColors.AchievementGold.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (achievement.isUnlocked) ProfileColors.AchievementGold.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon, contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (achievement.isUnlocked) ProfileColors.AchievementGold
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (!achievement.isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .clip(AppShape.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animProg).height(3.dp)
                            .clip(AppShape.extraSmall)
                            .background(ProfileColors.GoalPrimary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

// ============================================================================
// Shared
// ============================================================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ProfileEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.AutoStories, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Start Your Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Read your first chapter to unlock\nyour reader profile and stats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
                Surface(shape = AppShape.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Explore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Browse novels to get started", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun getAchievementIcon(iconName: String): ImageVector = when (iconName) {
    "book"      -> Icons.Rounded.AutoStories
    "schedule"  -> Icons.Rounded.Schedule
    "fire"      -> Icons.Rounded.LocalFireDepartment
    "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
    "trending"  -> Icons.AutoMirrored.Rounded.TrendingUp
    "crown"     -> Icons.Rounded.WorkspacePremium
    "trophy"    -> Icons.Rounded.EmojiEvents
    "calendar"  -> Icons.Rounded.CalendarMonth
    else        -> Icons.Rounded.AutoStories
}
