package com.kmhmubin.kothagolp.ui.screens.profile

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kmhmubin.kothagolp.ui.components.KothagolpPullToRefreshBox
import com.kmhmubin.kothagolp.ui.screens.profile.Achievement
import com.kmhmubin.kothagolp.ui.screens.profile.NovelReadingStats
import com.kmhmubin.kothagolp.ui.screens.profile.ProfileEvent
import com.kmhmubin.kothagolp.ui.screens.profile.ProfileUiState
import com.kmhmubin.kothagolp.ui.screens.profile.ProfileViewModel
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import com.kmhmubin.kothagolp.ui.theme.NewChapters
import com.kmhmubin.kothagolp.ui.theme.AccentCyan
import com.kmhmubin.kothagolp.ui.theme.AppOrange
import com.kmhmubin.kothagolp.ui.theme.Error
import com.kmhmubin.kothagolp.ui.theme.Info
import com.kmhmubin.kothagolp.ui.theme.StatusOnHold
import com.kmhmubin.kothagolp.ui.theme.StatusPlanToRead
import com.kmhmubin.kothagolp.ui.theme.Success
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

// ============================================================================
// Color Constants
// ============================================================================

private object ProfileColors {
    val StreakOrange = AppOrange
    val StreakYellow = Color(0xFFFFB800)
    val GoalPrimary = Color(0xFF6366F1)
    val GoalSecondary = StatusPlanToRead
    val ChapterBlue = Info
    val TimeGreen = NewChapters
    val DaysAmber = StatusOnHold
    val AchievementGold = Color(0xFFFFD700)
    val LevelPurple = Color(0xFF9333EA)
    val InsightCyan = AccentCyan

    fun getLevelColor(level: Int): Color = when (level) {
        1 -> Color(0xFF94A3B8)
        2 -> Success
        3 -> Info
        4 -> StatusPlanToRead
        5 -> StatusOnHold
        6 -> Error
        7 -> AchievementGold
        else -> Color(0xFFE879F9)
    }
}

// ============================================================================
// Main Profile Screen
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
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share your reading stats"))
                }
                is ProfileEvent.NavigateToNovel -> onNovelClick(event.novelUrl, event.sourceName)
                is ProfileEvent.ShowError -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Reading Stats", fontWeight = FontWeight.SemiBold)
                },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.hasAnyStats && !uiState.isLoading) {
                ProfileEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                ProfileContent(
                    uiState = uiState,
                    onNovelClick = { novel -> viewModel.onNovelClick(novel) }
                )
            }
        }
    }
}

// ============================================================================
// Profile Content — information hierarchy (gamified, top→bottom)
// 1. Identity   : hero level ring + XP
// 2. Motivation : streak banner (the dopamine hook)
// 3. Today      : quest card (daily + weekly goals)
// 4. Legacy     : lifetime strip (all-time totals)
// 5. Proof      : yearly activity heatmap
// 6. Rhythm     : this-week bar chart
// 7. Insights   : pace / session / best day
// 8. Collection : most-read novels
// 9. Trophy     : achievements
// ============================================================================

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNovelClick: (NovelReadingStats) -> Unit
) {
    val dimensions = KothagolpTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "hero") {
            ProfileHeroSection(uiState = uiState)
        }

        item(key = "streak") {
            StreakBannerSection(
                currentStreak = uiState.currentStreak,
                longestStreak = uiState.longestStreak,
                isStreakActive = uiState.isStreakActive,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "quest") {
            QuestCard(
                todayMinutes = uiState.todayMinutes.toInt(),
                todayChapters = uiState.todayChaptersRead,
                weekMinutes = uiState.weekMinutes.toInt(),
                dailyGoal = uiState.dailyGoalMinutes,
                weeklyGoal = uiState.weeklyGoalMinutes,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "lifetime") {
            LifetimeStripSection(
                totalChapters = uiState.totalChaptersRead,
                totalDays = uiState.totalDaysRead,
                totalHours = uiState.totalHours,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "heatmap") {
            ActivityHeatmapSection(
                yearlyActivity = uiState.yearlyActivity,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "weekly_activity") {
            WeeklyActivitySection(
                dailyMinutes = uiState.weeklyActivity,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "insights") {
            ReadingInsightsSection(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        if (uiState.mostReadNovels.isNotEmpty()) {
            item(key = "most_read") {
                MostReadSection(
                    novels = uiState.mostReadNovels,
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
// 1. Profile Hero Section
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
                        levelColor.copy(alpha = 0.05f),
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
                // Avatar with level progress ring
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(88.dp)) {
                        val strokeWidth = 4.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            color = levelColor.copy(alpha = 0.2f),
                            radius = radius, center = center,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = levelColor, startAngle = -90f,
                            sweepAngle = 360f * uiState.levelProgress, useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = levelColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Rounded.AutoStories, null,
                                modifier = Modifier.size(36.dp), tint = levelColor
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape, color = levelColor,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "${uiState.readerLevel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.readerLevelName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = "Level ${uiState.readerLevel} Reader",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${uiState.totalHours}h read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.hoursToNextLevel > 0) {
                                Text(
                                    text = "${uiState.hoursToNextLevel}h to next",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(AppShape.extraSmall)
                                .background(levelColor.copy(alpha = 0.2f))
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = uiState.levelProgress,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "level_progress"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(6.dp)
                                    .clip(AppShape.extraSmall)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(levelColor, levelColor.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. Streak Banner — the main motivational hook, prominent and animated
// ============================================================================

@Composable
private fun StreakBannerSection(
    currentStreak: Int,
    longestStreak: Int,
    isStreakActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_scale"
    )

    val streakMessage = when {
        currentStreak == 0 -> "Start reading today!"
        currentStreak < 3  -> "Great start, keep going!"
        currentStreak < 7  -> "Building momentum!"
        currentStreak < 30 -> "You're on fire!"
        currentStreak < 100 -> "Legendary streak!"
        else -> "Unstoppable!"
    }

    Card(
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isStreakActive)
                ProfileColors.StreakOrange.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Reading Streak",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$currentStreak",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isStreakActive) ProfileColors.StreakOrange
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "days",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = streakMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isStreakActive) ProfileColors.StreakOrange.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (longestStreak > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Best: $longestStreak days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .then(
                        if (isStreakActive && currentStreak > 0)
                            Modifier.graphicsLayer { scaleX = fireScale; scaleY = fireScale }
                        else Modifier
                    ),
                tint = if (isStreakActive && currentStreak > 0)
                    ProfileColors.StreakOrange
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            )
        }
    }
}

// ============================================================================
// 3. Quest Card — daily + weekly goals in one card
// ============================================================================

@Composable
private fun QuestCard(
    todayMinutes: Int,
    todayChapters: Int,
    weekMinutes: Int,
    dailyGoal: Int,
    weeklyGoal: Int,
    modifier: Modifier = Modifier
) {
    val dailyProgress = if (dailyGoal > 0) (todayMinutes.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val weeklyProgress = if (weeklyGoal > 0) (weekMinutes.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 0f

    val animatedDailyProgress by animateFloatAsState(
        targetValue = dailyProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "daily_progress"
    )
    val animatedWeeklyProgress by animateFloatAsState(
        targetValue = weeklyProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "weekly_progress"
    )

    val isDailyDone = todayMinutes >= dailyGoal
    val ringColor = ProfileColors.GoalPrimary

    Card(
        shape = AppShape.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's Quest",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Daily goal ring
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            color = ringColor.copy(alpha = 0.15f),
                            radius = radius, center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor, startAngle = -90f,
                            sweepAngle = 360f * animatedDailyProgress, useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    if (isDailyDone) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(30.dp), tint = ringColor)
                    } else {
                        Text(
                            text = "${(dailyProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ringColor
                        )
                    }
                }

                // Stats + weekly bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$todayChapters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ProfileColors.ChapterBlue
                            )
                            Text(
                                text = "chapters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val displayMin = when {
                                todayMinutes < 60 -> "${todayMinutes}m"
                                else -> "${todayMinutes / 60}h ${todayMinutes % 60}m"
                            }
                            Text(
                                text = displayMin,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ProfileColors.TimeGreen
                            )
                            Text(
                                text = "read today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Daily: $todayMinutes / $dailyGoal min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )

                    // Weekly goal thin bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weekly goal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(weeklyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ProfileColors.GoalSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(AppShape.extraSmall)
                                .background(ProfileColors.GoalSecondary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedWeeklyProgress)
                                    .height(5.dp)
                                    .clip(AppShape.extraSmall)
                                    .background(ProfileColors.GoalSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. Lifetime Strip — all-time totals (chapters · days · hours)
// ============================================================================

@Composable
private fun LifetimeStripSection(
    totalChapters: Int,
    totalDays: Int,
    totalHours: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LifetimePill(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            value = if (totalChapters >= 1000) "${totalChapters / 1000}k" else "$totalChapters",
            label = "chapters",
            color = ProfileColors.ChapterBlue,
            modifier = Modifier.weight(1f)
        )
        LifetimePill(
            icon = Icons.Rounded.CalendarMonth,
            value = "$totalDays",
            label = "days read",
            color = ProfileColors.DaysAmber,
            modifier = Modifier.weight(1f)
        )
        LifetimePill(
            icon = Icons.Rounded.Schedule,
            value = "${totalHours}h",
            label = "total time",
            color = ProfileColors.TimeGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LifetimePill(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShape.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// 5. Activity Heatmap — full 52-week GitHub-style calendar
// ============================================================================

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

@Composable
private fun ActivityHeatmapSection(
    yearlyActivity: Map<Long, Long>,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    // Align start to Monday 51 full weeks back so today is always rightmost
    val startDate = remember(today) {
        val daysBack = 51 * 7 + today.dayOfWeek.value - 1
        today.minusDays(daysBack.toLong())
    }
    val totalWeeks = 52

    // Build 52 × 7 grid: each cell = (date, minutes) or null for future
    val weekData: List<List<Pair<LocalDate, Long>>> = remember(yearlyActivity, startDate, today) {
        (0 until totalWeeks).map { w ->
            (0 until 7).mapNotNull { d ->
                val date = startDate.plusDays((w * 7 + d).toLong())
                if (!date.isAfter(today)) date to (yearlyActivity[date.toEpochDay()] ?: 0L)
                else null
            }
        }
    }

    // Month label: first week where a new month starts
    val monthLabels: Map<Int, String> = remember(startDate) {
        buildMap {
            var lastMonth = -1
            for (w in 0 until totalWeeks) {
                val m = startDate.plusDays((w * 7).toLong()).monthValue
                if (m != lastMonth) {
                    put(w, MONTH_NAMES[m - 1])
                    lastMonth = m
                }
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest

    // Scroll to rightmost (most recent weeks) after layout
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Activity", icon = Icons.Rounded.CalendarMonth)

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Day-of-week labels fixed on left
                    Column(
                        modifier = Modifier.padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("Mon", "", "Wed", "", "Fri", "", "Sun").forEach { label ->
                            Box(
                                modifier = Modifier
                                    .height(11.dp)
                                    .width(26.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (label.isNotEmpty()) {
                                    Text(
                                        text = label,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Scrollable week columns
                    Box(modifier = Modifier.horizontalScroll(scrollState)) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            // Month label row
                            Row(
                                modifier = Modifier.height(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(totalWeeks) { w ->
                                    Box(modifier = Modifier.width(13.dp)) {
                                        monthLabels[w]?.let { name ->
                                            Text(
                                                text = name,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Week columns
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                weekData.forEach { week ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                                    .size(11.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (isToday) Modifier.border(
                                                            1.dp, primaryColor, RoundedCornerShape(2.dp)
                                                        ) else Modifier
                                                    )
                                            )
                                        }
                                        // Spacer for future days (keep column height uniform)
                                        repeat(7 - week.size) { Spacer(Modifier.size(11.dp)) }
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
                    Text(
                        text = "Less",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.width(4.dp))
                    listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (alpha == 0f) emptyColor else primaryColor.copy(alpha = alpha))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "More",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. Weekly Activity Bar Chart
// ============================================================================

@Composable
private fun WeeklyActivitySection(
    dailyMinutes: List<Long>,
    modifier: Modifier = Modifier
) {
    val dayLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val maxMinutes = remember(dailyMinutes) { dailyMinutes.maxOrNull()?.coerceAtLeast(1L) ?: 1L }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "This Week", icon = Icons.Rounded.CalendarMonth)

        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyMinutes.forEachIndexed { index, minutes ->
                    val heightFraction = if (maxMinutes > 0) (minutes.toFloat() / maxMinutes) else 0f
                    val animatedHeight by animateFloatAsState(
                        targetValue = heightFraction.coerceIn(0.08f, 1f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height_$index"
                    )
                    val isToday = index == dailyMinutes.lastIndex
                    val barColor = when {
                        isToday -> ProfileColors.ChapterBlue
                        minutes > 0 -> ProfileColors.ChapterBlue.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(60.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((60 * animatedHeight).dp)
                                    .clip(AppShape.extraSmall)
                                    .background(barColor)
                            )
                        }
                        Text(
                            text = dayLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. Reading Insights
// ============================================================================

@Composable
private fun ReadingInsightsSection(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    val bestDay = remember(uiState.weeklyActivity) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val maxIndex = uiState.weeklyActivity.indexOf(uiState.weeklyActivity.maxOrNull() ?: 0L)
        if (maxIndex >= 0 && uiState.weeklyActivity[maxIndex] > 0) days[maxIndex] else null
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Insights", icon = Icons.Rounded.Insights)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Avg Session",
                value = "${uiState.averageSessionMinutes}m",
                subtitle = "per day",
                color = ProfileColors.InsightCyan,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Pace",
                value = String.format("%.1f", uiState.chaptersPerDay),
                subtitle = "ch/day",
                color = ProfileColors.GoalSecondary,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Best Day",
                value = bestDay ?: "—",
                subtitle = "this week",
                color = ProfileColors.DaysAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = AppShape.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================================
// 8. Most Read Novels
// ============================================================================

@Composable
private fun MostReadSection(
    novels: List<NovelReadingStats>,
    onNovelClick: (NovelReadingStats) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Most Read", icon = Icons.Rounded.AutoStories)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            novels.forEachIndexed { index, novel ->
                MostReadNovelCard(
                    rank = index + 1,
                    novel = novel,
                    onClick = { onNovelClick(novel) }
                )
            }
        }
    }
}

@Composable
private fun MostReadNovelCard(
    rank: Int,
    novel: NovelReadingStats,
    onClick: () -> Unit
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        shape = AppShape.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = rankColor.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            }

            if (novel.coverUrl != null) {
                AsyncImage(
                    model = novel.coverUrl, contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(AppShape.small),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = AppShape.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.AutoStories, null,
                            Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = novel.novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timeDisplay = remember(novel.readingTimeMinutes) {
                        when {
                            novel.readingTimeMinutes < 60 -> "${novel.readingTimeMinutes}m"
                            else -> "${novel.readingTimeMinutes / 60}h ${novel.readingTimeMinutes % 60}m"
                        }
                    }
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfileColors.TimeGreen
                    )
                    if (novel.sourceName.isNotBlank()) {
                        Text(
                            text = "• ${novel.sourceName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. Achievements
// ============================================================================

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Achievements", icon = Icons.Rounded.EmojiEvents)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement = achievement, modifier = Modifier.width(120.dp))
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
    val animatedProgress by animateFloatAsState(
        targetValue = achievement.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "achievement_progress"
    )

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (achievement.isUnlocked)
                    ProfileColors.AchievementGold.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon, contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (achievement.isUnlocked) ProfileColors.AchievementGold
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!achievement.isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(AppShape.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(4.dp)
                            .background(ProfileColors.GoalPrimary)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Shared: Section Header
// ============================================================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ============================================================================
// Empty State
// ============================================================================

@Composable
private fun ProfileEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = AppShape.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.AutoStories, null,
                            Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Start Your Journey",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Read your first chapter to unlock\nyour reader profile and stats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Surface(
                    shape = AppShape.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Explore, null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Browse novels to get started",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

private fun getAchievementIcon(iconName: String): ImageVector = when (iconName) {
    "book"      -> Icons.Rounded.AutoStories
    "schedule"  -> Icons.Rounded.Schedule
    "fire"      -> Icons.Rounded.LocalFireDepartment
    "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
    "trending"  -> Icons.AutoMirrored.Rounded.TrendingUp
    else        -> Icons.Rounded.AutoStories
}
