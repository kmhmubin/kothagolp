package com.kmhmubin.kothagolp.ui.screens.profile

/**
 * UI State for the Profile/Statistics tab
 */
data class ProfileUiState(
    val isLoading: Boolean = true,

    // Streak
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isStreakActive: Boolean = false,
    val totalDaysRead: Int = 0,

    // Reading time (in seconds)
    val todayReadingTime: Long = 0,
    val weekReadingTime: Long = 0,
    val monthReadingTime: Long = 0,
    val totalReadingTime: Long = 0,

    // Chapters
    val todayChaptersRead: Int = 0,
    val weekChaptersRead: Int = 0,
    val monthChaptersRead: Int = 0,
    val totalChaptersRead: Int = 0,

    // Weekly activity (7 days, minutes per day; index 0 = 6 days ago, index 6 = today)
    val weeklyActivity: List<Long> = List(7) { 0L },

    // Most read novels
    val mostReadNovels: List<NovelReadingStats> = emptyList(),

    // Goals
    val dailyGoalMinutes: Int = 30,
    val weeklyGoalMinutes: Int = 180,

    // Achievements
    val achievements: List<Achievement> = emptyList(),

    // Yearly activity heatmap (epochDay -> minutes)
    val yearlyActivity: Map<Long, Long> = emptyMap(),

    // Reader level info
    val readerLevelName: String = "Novice",
    val readerLevel: Int = 1,
    val levelProgress: Float = 0f,
    val hoursToNextLevel: Int = 0,

    // Library counts for achievements
    val libraryNovelsCount: Int = 0,
    val completedNovelsCount: Int = 0
) {
    // Computed properties
    val todayMinutes: Long get() = todayReadingTime / 60
    val weekMinutes: Long get() = weekReadingTime / 60
    val monthMinutes: Long get() = monthReadingTime / 60
    val totalHours: Long get() = totalReadingTime / 3600

    val dailyGoalProgress: Float
        get() = if (dailyGoalMinutes > 0) {
            (todayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
        } else 0f

    val weeklyGoalProgress: Float
        get() = if (weeklyGoalMinutes > 0) {
            (weekMinutes.toFloat() / weeklyGoalMinutes).coerceIn(0f, 1f)
        } else 0f

    val hasAnyStats: Boolean
        get() = totalReadingTime > 0 || totalChaptersRead > 0 || totalDaysRead > 0

    val averageSessionMinutes: Long
        get() = if (totalDaysRead > 0) (totalReadingTime / 60) / totalDaysRead else 0L

    val chaptersPerDay: Float
        get() = if (totalDaysRead > 0) totalChaptersRead.toFloat() / totalDaysRead else 0f

    val totalReadingDays: Float
        get() = totalReadingTime / 86400f
}

data class NovelReadingStats(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String? = null,
    val sourceName: String = "",
    val readingTimeMinutes: Long,
    val chaptersRead: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val progress: Float = 0f,
    val unlockedAt: Long? = null
)

// One-time events
sealed interface ProfileEvent {
    data class ShareStats(val text: String) : ProfileEvent
    data class NavigateToNovel(val novelUrl: String, val sourceName: String) : ProfileEvent
    data class ShowError(val message: String) : ProfileEvent
}

// Predefined achievements
object Achievements {
    // ── Tier 1: First milestones ─────────────────────────────────────────────
    val FIRST_CHAPTER = Achievement(
        id = "first_chapter",
        title = "First Steps",
        description = "Read your first chapter",
        iconName = "book",
        isUnlocked = false
    )

    val BOOKWORM = Achievement(
        id = "bookworm",
        title = "Bookworm",
        description = "Read for 10 hours total",
        iconName = "schedule",
        isUnlocked = false
    )

    val HUNDRED_CHAPTERS = Achievement(
        id = "hundred_chapters",
        title = "Century",
        description = "Read 100 chapters",
        iconName = "menu_book",
        isUnlocked = false
    )

    // ── Tier 2: Streak mastery ────────────────────────────────────────────────
    val STREAK_7 = Achievement(
        id = "streak_7",
        title = "Week Warrior",
        description = "Maintain a 7-day streak",
        iconName = "fire",
        isUnlocked = false
    )

    val STREAK_30 = Achievement(
        id = "streak_30",
        title = "Monthly Master",
        description = "Maintain a 30-day streak",
        iconName = "fire",
        isUnlocked = false
    )

    val STREAK_100 = Achievement(
        id = "streak_100",
        title = "Centurion Streak",
        description = "Maintain a 100-day streak",
        iconName = "fire",
        isUnlocked = false
    )

    // ── Tier 3: Volume readers ────────────────────────────────────────────────
    val SPEED_READER = Achievement(
        id = "speed_reader",
        title = "Speed Reader",
        description = "Read 500 chapters",
        iconName = "trending",
        isUnlocked = false
    )

    val THOUSAND_CHAPTERS = Achievement(
        id = "thousand_chapters",
        title = "Unstoppable",
        description = "Read 1,000 chapters",
        iconName = "menu_book",
        isUnlocked = false
    )

    val FIVE_K_CHAPTERS = Achievement(
        id = "five_k_chapters",
        title = "Chapter God",
        description = "Read 5,000 chapters",
        iconName = "crown",
        isUnlocked = false
    )

    // ── Tier 4: Time invested ─────────────────────────────────────────────────
    val BIBLIOPHILE = Achievement(
        id = "bibliophile",
        title = "Bibliophile",
        description = "Read for 50 hours total",
        iconName = "schedule",
        isUnlocked = false
    )

    val MASTER_READER = Achievement(
        id = "master_reader",
        title = "Master Reader",
        description = "Read for 200 hours total",
        iconName = "schedule",
        isUnlocked = false
    )

    val LEGENDARY_TIME = Achievement(
        id = "legendary_time",
        title = "Legend",
        description = "Read for 500 hours total",
        iconName = "crown",
        isUnlocked = false
    )

    // ── Tier 5: Consistency ────────────────────────────────────────────────────
    val DEVOTED = Achievement(
        id = "devoted",
        title = "Devoted",
        description = "Read on 100 different days",
        iconName = "calendar",
        isUnlocked = false
    )

    // ── Tier 6: Library & completion ──────────────────────────────────────────
    val COLLECTOR = Achievement(
        id = "collector",
        title = "Collector",
        description = "Add 10 novels to your library",
        iconName = "book",
        isUnlocked = false
    )

    val COMPLETIONIST = Achievement(
        id = "completionist",
        title = "Completionist",
        description = "Complete 5 novels",
        iconName = "trophy",
        isUnlocked = false
    )

    val GRAND_LIBRARY = Achievement(
        id = "grand_library",
        title = "Grand Library",
        description = "Add 50 novels to your library",
        iconName = "book",
        isUnlocked = false
    )
}
