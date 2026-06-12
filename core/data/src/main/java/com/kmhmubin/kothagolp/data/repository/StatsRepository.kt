package com.kmhmubin.kothagolp.data.repository

import com.kmhmubin.kothagolp.data.local.dao.AggregatedStats
import com.kmhmubin.kothagolp.data.local.dao.NovelReadingTime
import com.kmhmubin.kothagolp.data.local.dao.RecentNovelInfo
import com.kmhmubin.kothagolp.data.local.dao.StatsDao
import com.kmhmubin.kothagolp.data.local.entity.ReadingStatsEntity
import com.kmhmubin.kothagolp.data.local.entity.ReadingStreakEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for reading statistics and streaks
 */
class StatsRepository(
    private val statsDao: StatsDao
) {

    // ================================================================
    // READING SESSION TRACKING
    // ================================================================

    /**
     * Record reading time for a novel.
     * Call this periodically while reading (e.g., every minute)
     */
    suspend fun recordReadingTime(
        novelUrl: String,
        novelName: String,
        durationSeconds: Long,
        wordsRead: Long = 0
    ) = withContext(Dispatchers.IO) {
        if (durationSeconds <= 0) return@withContext

        val today = getCurrentEpochDay()

        val existing = statsDao.getStatsForDay(novelUrl, today)

        if (existing != null) {
            val updated = existing.copy(
                readingTimeSeconds = existing.readingTimeSeconds + durationSeconds,
                wordsRead = existing.wordsRead + wordsRead,
                updatedAt = System.currentTimeMillis()
            )
            statsDao.insertStats(updated)
        } else {
            val newStats = ReadingStatsEntity(
                novelUrl = novelUrl,
                novelName = novelName,
                date = today,
                readingTimeSeconds = durationSeconds,
                wordsRead = wordsRead,
                sessionsCount = 1
            )
            statsDao.insertStats(newStats)
        }

        // Update streak with accumulated reading time
        updateStreak(today, durationSeconds)
    }

    /**
     * Record chapter completion
     */
    suspend fun recordChapterRead(
        novelUrl: String,
        novelName: String,
        wordsInChapter: Long = 0
    ) = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()

        val existing = statsDao.getStatsForDay(novelUrl, today)

        if (existing != null) {
            val updated = existing.copy(
                chaptersRead = existing.chaptersRead + 1,
                wordsRead = existing.wordsRead + wordsInChapter,
                updatedAt = System.currentTimeMillis()
            )
            statsDao.insertStats(updated)
        } else {
            val newStats = ReadingStatsEntity(
                novelUrl = novelUrl,
                novelName = novelName,
                date = today,
                chaptersRead = 1,
                wordsRead = wordsInChapter,
                sessionsCount = 1
            )
            statsDao.insertStats(newStats)
        }

        // Also update streak for chapter completion (no additional time)
        updateStreak(today, 0)
    }

    // ================================================================
    // STREAK MANAGEMENT
    // ================================================================

    /**
     * Update streak with accumulated reading time
     */
    private suspend fun updateStreak(currentDay: Long, addedSeconds: Long) {
        val streak = statsDao.getStreak() ?: ReadingStreakEntity()

        val newStreak = when {
            // Same day - just add reading time
            streak.lastReadDate == currentDay -> streak.copy(
                totalReadingTimeSeconds = streak.totalReadingTimeSeconds + addedSeconds,
                updatedAt = System.currentTimeMillis()
            )

            // Consecutive day - increment streak and add time
            streak.lastReadDate == currentDay - 1 -> streak.copy(
                currentStreak = streak.currentStreak + 1,
                longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
                lastReadDate = currentDay,
                totalDaysRead = streak.totalDaysRead + 1,
                totalReadingTimeSeconds = streak.totalReadingTimeSeconds + addedSeconds,
                updatedAt = System.currentTimeMillis()
            )

            // Gap in reading - reset streak but keep total time
            else -> streak.copy(
                currentStreak = 1,
                lastReadDate = currentDay,
                totalDaysRead = streak.totalDaysRead + 1,
                totalReadingTimeSeconds = streak.totalReadingTimeSeconds + addedSeconds,
                updatedAt = System.currentTimeMillis()
            )
        }

        statsDao.updateStreak(newStreak)
    }

    fun observeStreak(): Flow<ReadingStreakEntity?> = statsDao.observeStreak()

    suspend fun getStreak(): ReadingStreakEntity? = withContext(Dispatchers.IO) {
        statsDao.getStreak()
    }

    // ================================================================
    // STATISTICS QUERIES
    // ================================================================

    /**
     * Get stats for today
     */
    suspend fun getTodayStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        statsDao.getAggregatedStats(today, today)
    }

    /**
     * Get stats for this week (last 7 days)
     */
    suspend fun getWeekStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val weekStart = today - 6  // Last 7 days including today
        statsDao.getAggregatedStats(weekStart, today)
    }

    /**
     * Get stats for this month (last 30 days)
     */
    suspend fun getMonthStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        val today = getCurrentEpochDay()
        val monthStart = today - 29  // Last 30 days including today
        statsDao.getAggregatedStats(monthStart, today)
    }

    /**
     * Get ALL TIME stats - useful for calculating total reading time
     */
    suspend fun getAllTimeStats(): AggregatedStats? = withContext(Dispatchers.IO) {
        // Use a very wide range to capture all stats
        statsDao.getAggregatedStats(0, Long.MAX_VALUE)
    }

    /**
     * Get daily breakdown for a date range
     */
    suspend fun getDailyStats(startDate: Long, endDate: Long): List<ReadingStatsEntity> =
        withContext(Dispatchers.IO) {
            statsDao.getStatsInRange(startDate, endDate)
        }

    /**
     * Get stats for a specific novel
     */
    fun observeNovelStats(novelUrl: String): Flow<List<ReadingStatsEntity>> {
        return statsDao.observeNovelStats(novelUrl)
    }

    /**
     * Get most read novels
     */
    suspend fun getMostReadNovels(limit: Int = 10): List<NovelReadingTime> =
        withContext(Dispatchers.IO) {
            statsDao.getMostReadNovels(limit)
        }

    /**
     * Get recently read novels
     */
    suspend fun getRecentlyReadNovels(limit: Int = 10): List<RecentNovelInfo> =
        withContext(Dispatchers.IO) {
            statsDao.getRecentlyReadNovels(limit)
        }

    // ================================================================
    // DATA REPAIR / MIGRATION
    // ================================================================

    /**
     * Recalculate and repair streak's totalReadingTimeSeconds from reading_stats.
     * Call this once to fix existing data.
     */
    suspend fun repairStreakTotalTime() = withContext(Dispatchers.IO) {
        val allTimeStats = getAllTimeStats()
        val totalSeconds = allTimeStats?.totalTime ?: 0L

        val streak = statsDao.getStreak() ?: return@withContext

        if (streak.totalReadingTimeSeconds != totalSeconds) {
            val repaired = streak.copy(
                totalReadingTimeSeconds = totalSeconds,
                updatedAt = System.currentTimeMillis()
            )
            statsDao.updateStreak(repaired)
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun getCurrentEpochDay(): Long {
        return LocalDate.now().toEpochDay()
    }

    /**
     * Format reading time for display
     */
    fun formatReadingTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}