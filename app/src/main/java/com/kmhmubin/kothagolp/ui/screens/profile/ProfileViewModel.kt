package com.kmhmubin.kothagolp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.local.dao.NovelReadingTime
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
    private val historyRepository = RepositoryProvider.getHistoryRepository()
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

                // Active novels (by reading time), used for both genres and reader type
                // so we only pay for the per-novel detail lookups once.
                val activeNovels = statsRepository.getMostReadNovels(limit = 20)
                val topGenres = loadTopGenres(activeNovels, _uiState.value.genreMode)
                val readerType = loadReaderType(activeNovels, totalChapters)

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
                        completedNovelsCount = completedCount,

                        topGenres = topGenres,
                        readerType = readerType
                    )
                }

                // Update reader level with the calculated total
                updateReaderLevel(totalReadingTime / 3600)

                // Recap has its own period/range toggle, loaded independently
                loadRecap()

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
                _events.send(ProfileEvent.ShowError("Failed to load stats: ${e.message}"))
            }
        }
    }

    // ============================================================================
    // Week/Month recap
    // ============================================================================

    fun setRecapPeriod(period: RecapPeriod) {
        if (period == _uiState.value.recapPeriod) return
        _uiState.update { it.copy(recapPeriod = period) }
        loadRecap()
    }

    fun setRecapRange(range: RecapRange) {
        if (range == _uiState.value.recapRange) return
        _uiState.update { it.copy(recapRange = range) }
        loadRecap()
    }

    private fun loadRecap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecapLoading = true) }

            val period = _uiState.value.recapPeriod
            val range = _uiState.value.recapRange
            val periodLength = if (period == RecapPeriod.WEEK) 7 else 30
            val today = LocalDate.now()

            // CURRENT week/month ends today; PREVIOUS is the equal-length block
            // immediately before it — "Last Week"/"Last Month" the way the
            // inspiration screenshots frame it, not a calendar-boundary month.
            val end = if (range == RecapRange.CURRENT) today else today.minusDays(periodLength.toLong())
            val start = end.minusDays((periodLength - 1).toLong())

            val rows = statsRepository.getDailyStats(start.toEpochDay(), end.toEpochDay())

            val totalMinutes = rows.sumOf { it.readingTimeSeconds } / 60
            val chaptersRead = rows.sumOf { it.chaptersRead }
            val minutesByDate: Map<Long, Long> = rows.groupBy { it.date }
                .mapValues { (_, dayRows) -> dayRows.sumOf { it.readingTimeSeconds } / 60 }
            val daysActive = minutesByDate.count { it.value > 0 }
            val avgPerDay = if (daysActive > 0) totalMinutes / daysActive else 0L

            val mostActiveEntry = minutesByDate.maxByOrNull { it.value }
            val mostActiveDay = mostActiveEntry?.let { (epochDay, minutes) ->
                if (minutes <= 0) null else RecapDayHighlight(
                    label = LocalDate.ofEpochDay(epochDay)
                        .format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    minutes = minutes
                )
            }

            val longestStreakDays = longestConsecutiveRun(start, end, minutesByDate)

            val dailyBars = if (period == RecapPeriod.WEEK) {
                (0 until periodLength).map { offset ->
                    minutesByDate[start.plusDays(offset.toLong()).toEpochDay()] ?: 0L
                }
            } else {
                emptyList()
            }

            // Grouped by title, not novelUrl — the same novel read from two
            // sources (or re-added with different casing) is still one book
            // to the reader, not two separate "top novels" entries.
            val topNovels = rows.groupBy { it.novelName.trim().lowercase() }
                .map { (_, titleRows) ->
                    val totalTime = titleRows.sumOf { it.readingTimeSeconds }
                    val totalChapters = titleRows.sumOf { it.chaptersRead }
                    // Representative url/name: whichever source got the most
                    // reading time within this title, used for cover + click-through.
                    val representative = titleRows.groupBy { it.novelUrl }
                        .maxByOrNull { (_, urlRows) -> urlRows.sumOf { it.readingTimeSeconds } }
                        ?.value?.first() ?: titleRows.first()
                    Triple(representative.novelUrl, representative.novelName, totalTime to totalChapters)
                }
                .sortedByDescending { it.third.first }
                .take(5)
                .map { (novelUrl, novelName, timeAndChapters) ->
                    enrichNovelStats(novelUrl, novelName, timeAndChapters.first, timeAndChapters.second)
                }

            val rangeLabel = "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – " +
                end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

            _uiState.update {
                it.copy(
                    isRecapLoading = false,
                    recap = RecapStats(
                        rangeLabel = rangeLabel,
                        totalMinutes = totalMinutes,
                        chaptersRead = chaptersRead,
                        daysActive = daysActive,
                        periodLengthDays = periodLength,
                        avgMinutesPerDay = avgPerDay,
                        mostActiveDay = mostActiveDay,
                        longestStreakDays = longestStreakDays,
                        dailyBars = dailyBars,
                        topNovels = topNovels
                    )
                )
            }
        }
    }

    /** Longest run of consecutive dates with any reading activity, within [start, end]. */
    private fun longestConsecutiveRun(
        start: LocalDate,
        end: LocalDate,
        minutesByDate: Map<Long, Long>
    ): Int {
        var longest = 0
        var current = 0
        var day = start
        while (!day.isAfter(end)) {
            if ((minutesByDate[day.toEpochDay()] ?: 0L) > 0) {
                current += 1
                longest = maxOf(longest, current)
            } else {
                current = 0
            }
            day = day.plusDays(1)
        }
        return longest
    }

    private suspend fun enrichNovelStats(
        novelUrl: String,
        novelName: String,
        totalTimeSeconds: Long,
        chaptersRead: Int
    ): NovelReadingStats {
        return try {
            val libraryItem = libraryRepository.getLibraryItem(novelUrl)
            val sourceName = libraryItem?.novel?.apiName ?: extractSourceFromUrl(novelUrl)
            val coverUrl = offlineRepository.getNovelDetails(novelUrl)?.posterUrl
                ?: libraryItem?.novel?.posterUrl

            NovelReadingStats(
                novelUrl = novelUrl,
                novelName = novelName,
                coverUrl = coverUrl,
                sourceName = sourceName,
                readingTimeMinutes = totalTimeSeconds / 60,
                chaptersRead = chaptersRead
            )
        } catch (e: Exception) {
            NovelReadingStats(
                novelUrl = novelUrl,
                novelName = novelName,
                coverUrl = null,
                sourceName = extractSourceFromUrl(novelUrl),
                readingTimeMinutes = totalTimeSeconds / 60,
                chaptersRead = chaptersRead
            )
        }
    }

    // ============================================================================
    // Top genres
    // ============================================================================

    fun setGenreMode(mode: GenreMode) {
        if (mode == _uiState.value.genreMode) return
        _uiState.update { it.copy(genreMode = mode) }
        viewModelScope.launch {
            val activeNovels = statsRepository.getMostReadNovels(limit = 20)
            _uiState.update { it.copy(topGenres = loadTopGenres(activeNovels, mode)) }
        }
    }

    private suspend fun loadTopGenres(
        activeNovels: List<NovelReadingTime>,
        mode: GenreMode
    ): List<GenreStat> {
        // Keyed by lowercased tag so sources that disagree on casing (e.g. "Action"
        // vs "ACTION") aggregate into one genre instead of splitting the count.
        val tally = mutableMapOf<String, Pair<Int, Long>>() // tagKey -> (titleCount, minutes)
        val displayNames = mutableMapOf<String, String>() // tagKey -> first-seen casing

        for (novel in activeNovels) {
            val tags = try {
                offlineRepository.getNovelDetails(novel.novelUrl)?.tags
            } catch (e: Exception) {
                null
            } ?: continue

            val minutes = novel.totalTime / 60
            tags.forEach { rawTag ->
                val tag = rawTag.trim()
                if (tag.isEmpty()) return@forEach
                val key = tag.lowercase()
                displayNames.putIfAbsent(key, tag)
                val (count, mins) = tally[key] ?: (0 to 0L)
                tally[key] = (count + 1) to (mins + minutes)
            }
        }

        if (tally.isEmpty()) return emptyList()

        val totalTitles = tally.values.sumOf { it.first }
        val totalMinutes = tally.values.sumOf { it.second }

        return tally.entries
            .map { (key, countAndMinutes) ->
                val (titleCount, minutes) = countAndMinutes
                val percentage = when (mode) {
                    GenreMode.TITLES -> if (totalTitles > 0) titleCount.toFloat() / totalTitles else 0f
                    GenreMode.TIME -> if (totalMinutes > 0) minutes.toFloat() / totalMinutes else 0f
                }
                GenreStat(name = displayNames[key] ?: key, titleCount = titleCount, minutes = minutes, percentage = percentage)
            }
            .sortedByDescending { if (mode == GenreMode.TITLES) it.titleCount else it.minutes.toInt() }
            .take(8)
    }

    // ============================================================================
    // Reader type badge
    // ============================================================================

    private suspend fun loadReaderType(
        activeNovels: List<NovelReadingTime>,
        totalChapters: Int
    ): ReaderTypeBadge? {
        if (activeNovels.isEmpty()) return null

        val completionRates = activeNovels.mapNotNull { novel ->
            try {
                val chapterCount = offlineRepository.getNovelChapterCount(novel.novelUrl)
                if (chapterCount <= 0) return@mapNotNull null
                val readCount = historyRepository.getReadChapterCount(novel.novelUrl)
                (readCount.toFloat() / chapterCount).coerceIn(0f, 1f)
            } catch (e: Exception) {
                null
            }
        }

        if (completionRates.isEmpty()) return null

        val avgCompletion = completionRates.average().toFloat()
        val state = _uiState.value
        val chaptersPerDay = state.chaptersPerDay
        val longestStreak = state.longestStreak

        val (title, tagline, description, icon) = when {
            avgCompletion >= 0.8f && completionRates.size >= 3 -> ReaderArchetype(
                "The Completionist",
                "NEVER LEAVES THINGS UNFINISHED",
                "Quitting just isn't in your playbook. ${(avgCompletion * 100).toInt()}% of every novel " +
                    "you've started is done and dusted — almost nothing left hanging.",
                "trophy"
            )
            longestStreak >= 30 -> ReaderArchetype(
                "The Devoted",
                "SHOWS UP EVERY SINGLE DAY",
                "A $longestStreak-day streak isn't luck, it's a habit. Reading is part of your routine now, " +
                    "not an occasional hobby.",
                "fire"
            )
            chaptersPerDay >= 8f -> ReaderArchetype(
                "The Speed Reader",
                "PAGES DON'T STAND A CHANCE",
                "Averaging ${String.format("%.1f", chaptersPerDay)} chapters a day, you tear through " +
                    "novels faster than most people can add them to a list.",
                "trending"
            )
            avgCompletion < 0.35f && completionRates.size >= 5 -> ReaderArchetype(
                "The Collector",
                "ALWAYS STARTING SOMETHING NEW",
                "${completionRates.size} novels in rotation and counting. You'd rather sample everything " +
                    "than commit to finishing one before starting the next.",
                "book"
            )
            else -> ReaderArchetype(
                "The Explorer",
                "STILL FINDING YOUR RHYTHM",
                "You're building your reading habits one chapter at a time — the stats will sharpen up " +
                    "the more you read.",
                "book"
            )
        }

        val traits = listOf(
            ReaderTrait(
                title = "${(avgCompletion * 100).toInt()}% Finished",
                description = "average progress across novels you've read",
                iconName = "trophy"
            ),
            ReaderTrait(
                title = "${String.format("%.1f", chaptersPerDay)} ch/day",
                description = "your average reading pace",
                iconName = "trending"
            ),
            ReaderTrait(
                title = "${state.libraryNovelsCount} novels",
                description = "currently in your library",
                iconName = "book"
            )
        )

        return ReaderTypeBadge(
            title = title,
            tagline = tagline,
            description = description,
            iconName = icon,
            traits = traits
        )
    }

    private data class ReaderArchetype(
        val title: String,
        val tagline: String,
        val description: String,
        val iconName: String
    )

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