package com.kmhmubin.kothagolp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.repository.RepositoryProvider
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProfileViewModel : ViewModel() {

    private val statsRepository = RepositoryProvider.getStatsRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // One-time events channel
    private val _events = Channel<ProfileEvent>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    // Track if we've done initial repair
    private var hasRepairedStats = false

    // Reader level definitions
    private data class ReaderLevel(
        val level: Int,
        val title: String,
        val minHours: Int,
        val maxHours: Int
    )

    private val readerLevels = listOf(
        ReaderLevel(1, "Novice", 0, 5),
        ReaderLevel(2, "Apprentice", 5, 15),
        ReaderLevel(3, "Bookworm", 15, 30),
        ReaderLevel(4, "Scholar", 30, 60),
        ReaderLevel(5, "Sage", 60, 100),
        ReaderLevel(6, "Master", 100, 200),
        ReaderLevel(7, "Grand Master", 200, 500),
        ReaderLevel(8, "Legendary", 500, Int.MAX_VALUE)
    )

    init {
        loadStats()
        observeStreak()
    }

    private fun observeStreak() {
        viewModelScope.launch {
            statsRepository.observeStreak().collect { streak ->
                if (streak != null) {
                    val today = LocalDate.now().toEpochDay()
                    val isActive = streak.lastReadDate >= today - 1

                    _uiState.update {
                        it.copy(
                            currentStreak = streak.currentStreak,
                            longestStreak = streak.longestStreak,
                            isStreakActive = isActive,
                            totalDaysRead = streak.totalDaysRead,
                            // Use streak's totalReadingTimeSeconds if available,
                            // otherwise keep the calculated value from loadStats
                            totalReadingTime = if (streak.totalReadingTimeSeconds > 0) {
                                streak.totalReadingTimeSeconds
                            } else {
                                it.totalReadingTime
                            }
                        )
                    }

                    // Update reader level based on total reading time
                    val totalSeconds = _uiState.value.totalReadingTime
                    updateReaderLevel(totalSeconds / 3600)
                }
            }
        }
    }

    private fun updateReaderLevel(totalHours: Long) {
        val level = readerLevels.lastOrNull { totalHours >= it.minHours } ?: readerLevels.first()
        val progress = if (level.maxHours == Int.MAX_VALUE) {
            1f
        } else {
            val progressInLevel = totalHours - level.minHours
            val levelRange = level.maxHours - level.minHours
            (progressInLevel.toFloat() / levelRange).coerceIn(0f, 1f)
        }

        val hoursToNext = if (level.maxHours == Int.MAX_VALUE) {
            0
        } else {
            (level.maxHours - totalHours).toInt().coerceAtLeast(0)
        }

        _uiState.update {
            it.copy(
                readerLevelName = level.title,
                readerLevel = level.level,
                levelProgress = progress,
                hoursToNextLevel = hoursToNext
            )
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Repair streak total time on first load (one-time migration)
                if (!hasRepairedStats) {
                    statsRepository.repairStreakTotalTime()
                    hasRepairedStats = true
                }

                // Load today's stats
                val todayStats = statsRepository.getTodayStats()

                // Load week stats
                val weekStats = statsRepository.getWeekStats()

                // Load month stats
                val monthStats = statsRepository.getMonthStats()

                // Load ALL TIME stats for total reading time
                val allTimeStats = statsRepository.getAllTimeStats()

                // Load weekly activity (last 7 days)
                val weeklyActivity = loadWeeklyActivity()

                // Load yearly activity for heatmap (last 52 weeks)
                val yearlyActivity = loadYearlyActivity()

                // Load most read novels
                val mostReadNovels = loadMostReadNovels()

                // Load goals from preferences
                val dailyGoal = preferencesManager.getDailyReadingGoal()
                val weeklyGoal = preferencesManager.getWeeklyReadingGoal()

                // Load library counts for achievements
                val library = libraryRepository.getLibrary()
                val libraryCount = library.size
                val completedCount = library.count { it.readingStatus == ReadingStatus.COMPLETED }

                // Calculate achievements
                val achievements = calculateAchievements(libraryCount, completedCount)

                // Calculate total chapters
                val totalChapters = calculateTotalChapters()

                // Use all-time stats for total reading time as primary source
                val totalReadingTime = allTimeStats?.totalTime ?: 0L

                _uiState.update {
                    it.copy(
                        isLoading = false,

                        todayReadingTime = todayStats?.totalTime ?: 0,
                        todayChaptersRead = todayStats?.totalChapters ?: 0,

                        weekReadingTime = weekStats?.totalTime ?: 0,
                        weekChaptersRead = weekStats?.totalChapters ?: 0,

                        monthReadingTime = monthStats?.totalTime ?: 0,
                        monthChaptersRead = monthStats?.totalChapters ?: 0,

                        totalChaptersRead = totalChapters,

                        // Use calculated total reading time from all stats
                        totalReadingTime = totalReadingTime,

                        weeklyActivity = weeklyActivity,
                        yearlyActivity = yearlyActivity,
                        mostReadNovels = mostReadNovels,

                        dailyGoalMinutes = dailyGoal,
                        weeklyGoalMinutes = weeklyGoal,

                        achievements = achievements,
                        libraryNovelsCount = libraryCount,
                        completedNovelsCount = completedCount
                    )
                }

                // Update reader level with the calculated total
                updateReaderLevel(totalReadingTime / 3600)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
                _events.send(ProfileEvent.ShowError("Failed to load stats: ${e.message}"))
            }
        }
    }

    private suspend fun loadYearlyActivity(): Map<Long, Long> {
        val today = LocalDate.now()
        val yearStart = today.minusDays(363)
        val stats = statsRepository.getDailyStats(
            startDate = yearStart.toEpochDay(),
            endDate = today.toEpochDay()
        )
        return stats.groupBy { it.date }
            .mapValues { (_, dayStats) -> dayStats.sumOf { it.readingTimeSeconds } / 60 }
    }

    private suspend fun loadWeeklyActivity(): List<Long> {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)

        val stats = statsRepository.getDailyStats(
            startDate = weekStart.toEpochDay(),
            endDate = today.toEpochDay()
        )

        // Create a map of date to total minutes
        val statsByDay = stats.groupBy { it.date }
            .mapValues { (_, dayStats) ->
                dayStats.sumOf { it.readingTimeSeconds } / 60
            }

        // Build list for 7 days
        return (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong()).toEpochDay()
            statsByDay[date] ?: 0L
        }
    }

    private suspend fun loadMostReadNovels(): List<NovelReadingStats> {
        val topNovels = statsRepository.getMostReadNovels(limit = 5)

        return topNovels.mapNotNull { novel ->
            try {
                // Try to get from library first
                val libraryItem = libraryRepository.getLibraryItem(novel.novelUrl)

                // Get source name
                val sourceName = libraryItem?.novel?.apiName
                    ?: extractSourceFromUrl(novel.novelUrl)

                // Get cover URL
                val coverUrl = offlineRepository.getNovelDetails(novel.novelUrl)?.posterUrl
                    ?: libraryItem?.novel?.posterUrl

                // Get chapter count for this novel
                val novelStats = statsRepository.getDailyStats(0, Long.MAX_VALUE)
                    .filter { it.novelUrl == novel.novelUrl }
                val chaptersRead = novelStats.sumOf { it.chaptersRead }

                NovelReadingStats(
                    novelUrl = novel.novelUrl,
                    novelName = novel.novelName,
                    coverUrl = coverUrl,
                    sourceName = sourceName,
                    readingTimeMinutes = novel.totalTime / 60,
                    chaptersRead = chaptersRead
                )
            } catch (e: Exception) {
                // If we can't get details, still include with basic info
                NovelReadingStats(
                    novelUrl = novel.novelUrl,
                    novelName = novel.novelName,
                    coverUrl = null,
                    sourceName = extractSourceFromUrl(novel.novelUrl),
                    readingTimeMinutes = novel.totalTime / 60,
                    chaptersRead = 0
                )
            }
        }
    }

    /**
     * Extract source name from URL as fallback
     */
    private fun extractSourceFromUrl(url: String): String {
        return try {
            val host = url.removePrefix("https://").removePrefix("http://")
                .substringBefore("/")
                .removePrefix("www.")
            host.substringBefore(".").replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private suspend fun calculateTotalChapters(): Int {
        return try {
            val allStats = statsRepository.getDailyStats(0, Long.MAX_VALUE)
            allStats.sumOf { it.chaptersRead }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun calculateAchievements(
        libraryCount: Int = 0,
        completedCount: Int = 0
    ): List<Achievement> {
        val totalChapters = calculateTotalChapters()
        val allTimeStats = statsRepository.getAllTimeStats()
        val totalHours = (allTimeStats?.totalTime ?: 0) / 3600
        val streak = statsRepository.getStreak()
        val longestStreak = streak?.longestStreak ?: 0
        val totalDays = streak?.totalDaysRead ?: 0

        return listOf(
            // Tier 1 — first milestones
            Achievements.FIRST_CHAPTER.copy(
                isUnlocked = totalChapters >= 1,
                progress = if (totalChapters >= 1) 1f else 0f
            ),
            Achievements.HUNDRED_CHAPTERS.copy(
                isUnlocked = totalChapters >= 100,
                progress = (totalChapters.toFloat() / 100).coerceIn(0f, 1f)
            ),
            Achievements.BOOKWORM.copy(
                isUnlocked = totalHours >= 10,
                progress = (totalHours.toFloat() / 10).coerceIn(0f, 1f)
            ),
            // Tier 2 — streak mastery
            Achievements.STREAK_7.copy(
                isUnlocked = longestStreak >= 7,
                progress = (longestStreak.toFloat() / 7).coerceIn(0f, 1f)
            ),
            Achievements.STREAK_30.copy(
                isUnlocked = longestStreak >= 30,
                progress = (longestStreak.toFloat() / 30).coerceIn(0f, 1f)
            ),
            Achievements.STREAK_100.copy(
                isUnlocked = longestStreak >= 100,
                progress = (longestStreak.toFloat() / 100).coerceIn(0f, 1f)
            ),
            // Tier 3 — volume readers
            Achievements.SPEED_READER.copy(
                isUnlocked = totalChapters >= 500,
                progress = (totalChapters.toFloat() / 500).coerceIn(0f, 1f)
            ),
            Achievements.THOUSAND_CHAPTERS.copy(
                isUnlocked = totalChapters >= 1000,
                progress = (totalChapters.toFloat() / 1000).coerceIn(0f, 1f)
            ),
            Achievements.FIVE_K_CHAPTERS.copy(
                isUnlocked = totalChapters >= 5000,
                progress = (totalChapters.toFloat() / 5000).coerceIn(0f, 1f)
            ),
            // Tier 4 — time invested
            Achievements.BIBLIOPHILE.copy(
                isUnlocked = totalHours >= 50,
                progress = (totalHours.toFloat() / 50).coerceIn(0f, 1f)
            ),
            Achievements.MASTER_READER.copy(
                isUnlocked = totalHours >= 200,
                progress = (totalHours.toFloat() / 200).coerceIn(0f, 1f)
            ),
            Achievements.LEGENDARY_TIME.copy(
                isUnlocked = totalHours >= 500,
                progress = (totalHours.toFloat() / 500).coerceIn(0f, 1f)
            ),
            // Tier 5 — consistency
            Achievements.DEVOTED.copy(
                isUnlocked = totalDays >= 100,
                progress = (totalDays.toFloat() / 100).coerceIn(0f, 1f)
            ),
            // Tier 6 — library & completion
            Achievements.COLLECTOR.copy(
                isUnlocked = libraryCount >= 10,
                progress = (libraryCount.toFloat() / 10).coerceIn(0f, 1f)
            ),
            Achievements.COMPLETIONIST.copy(
                isUnlocked = completedCount >= 5,
                progress = (completedCount.toFloat() / 5).coerceIn(0f, 1f)
            ),
            Achievements.GRAND_LIBRARY.copy(
                isUnlocked = libraryCount >= 50,
                progress = (libraryCount.toFloat() / 50).coerceIn(0f, 1f)
            ),
        )
    }

    // ============================================================================
    // Actions
    // ============================================================================

    fun onNovelClick(novel: NovelReadingStats) {
        viewModelScope.launch {
            val sourceName = if (novel.sourceName.isNotBlank()) {
                novel.sourceName
            } else {
                // Try to find source name from library
                val libraryItem = libraryRepository.getLibraryItem(novel.novelUrl)
                libraryItem?.novel?.apiName ?: extractSourceFromUrl(novel.novelUrl)
            }
            _events.send(ProfileEvent.NavigateToNovel(novel.novelUrl, sourceName))
        }
    }

    fun onShareStats() {
        viewModelScope.launch {
            val shareText = generateShareText()
            _events.send(ProfileEvent.ShareStats(shareText))
        }
    }

    private fun generateShareText(): String {
        val state = _uiState.value
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

        return buildString {
            appendLine("📚 My Reading Stats - $today")
            appendLine()

            // Level
            appendLine("🎖️ ${state.readerLevelName} (Level ${state.readerLevel})")
            appendLine()

            // Streak
            if (state.currentStreak > 0) {
                appendLine("🔥 ${state.currentStreak} day streak!")
            }
            if (state.longestStreak > state.currentStreak) {
                appendLine("🏆 Best: ${state.longestStreak} days")
            }
            appendLine()

            // Stats
            appendLine("📖 ${state.totalChaptersRead} chapters read")
            appendLine("⏱️ ${state.totalHours} hours total")
            appendLine("📅 ${state.totalDaysRead} days reading")
            appendLine()

            // Goals
            val dailyProgress = (state.dailyGoalProgress * 100).toInt()
            val weeklyProgress = (state.weeklyGoalProgress * 100).toInt()
            appendLine("Daily goal: $dailyProgress%")
            appendLine("Weekly goal: $weeklyProgress%")
            appendLine()

            // Most read
            if (state.mostReadNovels.isNotEmpty()) {
                appendLine("📕 Currently reading:")
                state.mostReadNovels.take(3).forEach { novel ->
                    appendLine("  • ${novel.novelName}")
                }
            }

            appendLine()
            appendLine("Currently reading on Kothagolp 📖")
        }
    }

    fun setDailyGoal(minutes: Int) {
        preferencesManager.setDailyReadingGoal(minutes)
        _uiState.update { it.copy(dailyGoalMinutes = minutes) }
    }

    fun setWeeklyGoal(minutes: Int) {
        preferencesManager.setWeeklyReadingGoal(minutes)
        _uiState.update { it.copy(weeklyGoalMinutes = minutes) }
    }

    fun refresh() {
        loadStats()
    }
}