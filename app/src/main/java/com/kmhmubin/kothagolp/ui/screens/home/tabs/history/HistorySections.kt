package com.kmhmubin.kothagolp.ui.screens.home.tabs.history

import com.kmhmubin.kothagolp.data.repository.HistoryItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class LibraryInfo(val inLibrary: Boolean, val lastReadChapterIndex: Int, val totalChapters: Int)

data class HistoryRowModel(
    val item: HistoryItem,
    val title: String,
    val time: String,
    val chapterLine: String,
    val metaLine: String,
    val progressPercent: Int?,
    val inLibrary: Boolean,
    val titleMatch: IntRange?,
    /** Chapter number read (index + 1) and total, for the sheet; total 0 = unknown. */
    val readChapter: Int = 0,
    val totalChapters: Int = 0,
)

data class HistoryGroup(val key: String, val label: String, val rows: List<HistoryRowModel>)

private enum class Bucket(val key: String, val label: String) {
    TODAY("today", HistoryCopy.TODAY),
    YESTERDAY("yesterday", HistoryCopy.YESTERDAY),
    WEEK("week", HistoryCopy.THIS_WEEK),
    MONTH("month", HistoryCopy.THIS_MONTH),
    EARLIER("earlier", HistoryCopy.EARLIER),
}

/**
 * Today = same local date (or future); Yesterday = the day before; This week = dates since this
 * Monday (older than yesterday); This month = same calendar month; else Earlier.
 */
private fun bucketOf(date: LocalDate, today: LocalDate): Bucket {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return when {
        !date.isBefore(today) -> Bucket.TODAY
        date == today.minusDays(1) -> Bucket.YESTERDAY
        !date.isBefore(weekStart) -> Bucket.WEEK
        YearMonth.from(date) == YearMonth.from(today) -> Bucket.MONTH
        else -> Bucket.EARLIER
    }
}

fun shortTime(timestamp: Long, now: Long, zone: ZoneId, use24h: Boolean): String {
    val at = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = at.toLocalDate()
    return when (bucketOf(date, today)) {
        Bucket.TODAY -> {
            val mins = ((now - timestamp) / 60_000).coerceAtLeast(0)
            when {
                mins < 1 -> HistoryCopy.JUST_NOW
                mins < 60 -> HistoryCopy.minutesAgo(mins)
                else -> HistoryCopy.hoursAgo(mins / 60)
            }
        }
        Bucket.YESTERDAY -> {
            val h = at.hour
            val mm = "%02d".format(at.minute)
            if (use24h) "%02d:%s".format(h, mm)
            else "${if (h % 12 == 0) 12 else h % 12}:$mm ${if (h < 12) "am" else "pm"}"
        }
        Bucket.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        else -> {
            val md = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
            if (date.year == today.year) md else "$md ${date.year}"
        }
    }
}

private fun shortChapter(name: String) = if (name.startsWith("Chapter ")) "Ch. " + name.removePrefix("Chapter ") else name

private fun progressOf(info: LibraryInfo?): Int? =
    info?.takeIf { it.totalChapters > 0 }
        ?.let { ((it.lastReadChapterIndex + 1) * 100 / it.totalChapters).coerceIn(0, 100) }

fun buildHistoryGroups(
    items: List<HistoryItem>,
    library: Map<String, LibraryInfo>,
    query: String,
    now: Long,
    zone: ZoneId,
    use24h: Boolean,
): List<HistoryGroup> {
    val q = query.trim()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    fun row(item: HistoryItem, match: IntRange?): HistoryRowModel {
        val info = library[item.novel.url]
        val pct = progressOf(info)
        val total = info?.totalChapters ?: 0
        return HistoryRowModel(
            item = item,
            title = item.novel.name,
            time = shortTime(item.timestamp, now, zone, use24h),
            chapterLine = shortChapter(item.chapterName),
            metaLine = if (pct != null) "${item.novel.apiName} · $pct%" else item.novel.apiName,
            progressPercent = pct,
            inLibrary = info?.inLibrary == true,
            titleMatch = match,
            readChapter = if (total > 0) info!!.lastReadChapterIndex + 1 else 0,
            totalChapters = total,
        )
    }

    val sorted = items.sortedByDescending { it.timestamp }
    if (q.isNotEmpty()) {
        // Literal substring match; never builds a Regex from user input.
        val rows = sorted.mapNotNull { item ->
            val i = item.novel.name.indexOf(q, ignoreCase = true)
            if (i < 0) null else row(item, i until i + q.length)
        }
        return if (rows.isEmpty()) emptyList() else listOf(HistoryGroup("results", HistoryCopy.results(rows.size), rows))
    }
    return sorted
        .groupBy { bucketOf(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate(), today) }
        .toSortedMap()
        .map { (b, list) -> HistoryGroup(b.key, b.label, list.map { row(it, null) }) }
}

/** (line 1, optional line 2) for the press-and-hold sheet header. */
fun sheetMeta(row: HistoryRowModel): Pair<String, String?> {
    val source = row.item.novel.apiName
    val first = if (row.totalChapters > 0) "${HistoryCopy.chapterOf(row.readChapter, row.totalChapters)} · $source"
    else "${row.chapterLine} · $source"
    val second = row.progressPercent?.let { "${HistoryCopy.percentRead(it)} · ${row.time}" } ?: row.time
    return first to second
}
