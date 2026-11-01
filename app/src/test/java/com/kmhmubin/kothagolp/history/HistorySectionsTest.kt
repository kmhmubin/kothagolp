package com.kmhmubin.kothagolp.history

import com.kmhmubin.kothagolp.data.repository.HistoryItem
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.LibraryInfo
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.buildHistoryGroups
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.sheetMeta
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.shortTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HistorySectionsTest {
    private val zone = ZoneId.of("Asia/Dhaka")

    // Saturday 2026-10-31 15:00 Dhaka. Week starts Monday 26 Oct; yesterday is Fri 30 Oct.
    private val now = at(2026, 10, 31, 15, 0)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        name: String, ts: Long, chapter: String = "Chapter 1", source: String = "Src", url: String = name,
    ) = HistoryItem(Novel(name = name, url = url, apiName = source), chapter, "$url/c", ts)

    private fun groups(items: List<HistoryItem>, lib: Map<String, LibraryInfo> = emptyMap(), q: String = "", h24: Boolean = false) =
        buildHistoryGroups(items, lib, q, now, zone, h24)

    @Test fun groupsByDateAndSortsNewestFirst() {
        val items = listOf(
            item("earlier", at(2026, 9, 30, 10, 0)),
            item("month", at(2026, 10, 10, 10, 0)),
            item("week", at(2026, 10, 27, 10, 0)),
            item("yest", at(2026, 10, 30, 21, 40)),
            item("today-old", at(2026, 10, 31, 1, 0)),
            item("today-new", at(2026, 10, 31, 14, 59)),
        )
        val g = groups(items)
        assertEquals(listOf("today", "yesterday", "week", "month", "earlier"), g.map { it.key })
        assertEquals(listOf("Today", "Yesterday", "This week", "This month", "Earlier"), g.map { it.label })
        assertEquals(listOf("today-new", "today-old"), g[0].rows.map { it.title })
    }

    @Test fun emptyGroupsDropped() {
        val g = groups(listOf(item("a", at(2026, 10, 31, 10, 0)), item("b", at(2025, 1, 1, 10, 0))))
        assertEquals(listOf("today", "earlier"), g.map { it.key })
    }

    @Test fun shortTimes() {
        fun t(ts: Long, h24: Boolean = false) = shortTime(ts, now, zone, h24)
        assertEquals("Just now", t(now - 30_000))
        assertEquals("5m ago", t(now - 5 * 60_000))
        assertEquals("2h ago", t(now - 2 * 3_600_000))
        assertEquals("9:40 pm", t(at(2026, 10, 30, 21, 40)))
        assertEquals("21:40", t(at(2026, 10, 30, 21, 40), true))
        assertEquals("12:05 am", t(at(2026, 10, 30, 0, 5)))
        assertEquals("Tue", t(at(2026, 10, 27, 8, 0)))
        assertEquals("12 Oct", t(at(2026, 10, 12, 8, 0)))
        assertEquals("12 Oct 2025", t(at(2025, 10, 12, 8, 0)))
    }

    @Test fun chapterShortening() {
        val g = groups(listOf(
            item("a", now - 1000, chapter = "Chapter 808 - 737: Jealous?"),
            item("b", now - 2000, chapter = "Prologue"),
        ))
        assertEquals("Ch. 808 - 737: Jealous?", g[0].rows[0].chapterLine)
        assertEquals("Prologue", g[0].rows[1].chapterLine)
    }

    @Test fun metaLineAndProgress() {
        val lib = mapOf(
            "a" to LibraryInfo(true, 60, 100),
            "b" to LibraryInfo(true, 5, 0),
            "c" to LibraryInfo(true, 500, 100),
        )
        val rows = groups(listOf(
            item("a", now - 1000, source = "LibRead"),
            item("b", now - 2000, source = "LibRead"),
            item("c", now - 3000, source = "LibRead"),
            item("d", now - 4000, source = "LibRead"),
        ), lib)[0].rows
        assertEquals("LibRead · 61%", rows[0].metaLine)
        assertEquals(61, rows[0].progressPercent)
        assertEquals("LibRead", rows[1].metaLine)
        assertNull(rows[1].progressPercent)
        assertEquals(100, rows[2].progressPercent)
        assertEquals("LibRead", rows[3].metaLine)
        assertNull(rows[3].progressPercent)
        assertEquals(false, rows[3].inLibrary)
        assertEquals(true, rows[0].inLibrary)
    }

    @Test fun searchIsCaseInsensitiveWithRange() {
        val g = groups(listOf(item("The Great Tale", now - 1000), item("Other", now - 2000), item("great two", now - 3000)), q = "GREAT")
        assertEquals(1, g.size)
        assertEquals("results", g[0].key)
        assertEquals("2 results", g[0].label)
        assertEquals(4..8, g[0].rows[0].titleMatch)
        assertEquals(0..4, g[0].rows[1].titleMatch)
    }

    @Test fun searchSpecialCharsDoNotCrash() {
        val items = listOf(item("Wow (x) * a\\b", now - 1000))
        assertEquals(4..6, groups(items, q = "(x)")[0].rows[0].titleMatch)
        assertEquals(8..8, groups(items, q = "*")[0].rows[0].titleMatch)
        assertEquals(11..11, groups(items, q = "\\")[0].rows[0].titleMatch)
        assertEquals(emptyList<Any>(), groups(items, q = "zzz"))
    }

    @Test fun sheetMetaLines() {
        val lib = mapOf("a" to LibraryInfo(true, 60, 100))
        val rows = groups(listOf(item("a", now - 5 * 60_000, source = "S"), item("b", now - 5 * 60_000, chapter = "Chapter 9: X", source = "S")), lib)[0].rows
        assertEquals("Ch. 61 of 100 · S" to "61% read · 5m ago", sheetMeta(rows[0]))
        assertEquals("Ch. 9: X · S" to "5m ago", sheetMeta(rows[1]))
    }

    @Test fun copy() {
        assertEquals("1 novel you've opened", HistoryCopy.subtitle(1))
        assertEquals("3 novels you've opened", HistoryCopy.subtitle(3))
        assertTrue(HistoryCopy.clearBody(1).startsWith("This removes the 1 novel from History."))
        assertTrue(HistoryCopy.clearBody(4).startsWith("This removes all 4 novels from History."))
    }

    private fun groupAndTime(ts: Long, nowMs: Long, h24: Boolean = false): Pair<String, String> {
        val g = buildHistoryGroups(listOf(item("x", ts)), emptyMap(), "", nowMs, zone, h24)
        return g.single().key to shortTime(ts, nowMs, zone, h24)
    }

    @Test fun midnightEdges() {
        val n = at(2026, 10, 31, 0, 30)
        assertEquals("yesterday" to "11:59 pm", groupAndTime(at(2026, 10, 30, 23, 59), n))
        assertEquals("today" to "Just now", groupAndTime(at(2026, 10, 31, 0, 1) + 30_000, at(2026, 10, 31, 0, 2)))
        assertEquals("today" to "29m ago", groupAndTime(at(2026, 10, 31, 0, 1), n))
    }

    @Test fun weekMondayAndMonthEdges() {
        val thu = at(2026, 11, 5, 12, 0) // Mon is 2 Nov
        assertEquals("yesterday" to "4:00 pm", groupAndTime(at(2026, 11, 4, 16, 0), thu))
        assertEquals("week" to "Mon", groupAndTime(at(2026, 11, 2, 0, 0), thu))
        assertEquals("month" to "1 Nov", groupAndTime(at(2026, 11, 1, 23, 59), thu))
        assertEquals("earlier" to "31 Oct", groupAndTime(at(2026, 10, 31, 23, 59), thu))
        assertEquals("earlier" to "30 Oct", groupAndTime(at(2026, 10, 30, 12, 0), thu)) // 6 days ago
        assertEquals("earlier" to "28 Oct", groupAndTime(at(2026, 10, 28, 12, 0), thu)) // 8 days ago
        val thu2 = at(2026, 10, 22, 12, 0) // Mon is 19 Oct
        assertEquals("week" to "Tue", groupAndTime(at(2026, 10, 20, 12, 0), thu2))
        assertEquals("month" to "18 Oct", groupAndTime(at(2026, 10, 18, 12, 0), thu2)) // last Sunday
        assertEquals("month" to "14 Oct", groupAndTime(at(2026, 10, 14, 12, 0), thu2)) // 8 days ago
    }

    @Test fun chapterNumberClamped() {
        val lib = mapOf(
            "hi" to LibraryInfo(true, 500, 100),
            "neg" to LibraryInfo(true, -1, 100),
        )
        val rows = groups(listOf(item("hi", now - 1000, source = "S"), item("neg", now - 2000, chapter = "Chapter 3", source = "S")), lib)[0].rows
        assertEquals("Ch. 100 of 100 · S", sheetMeta(rows[0]).first)
        assertEquals("Ch. 3 · S", sheetMeta(rows[1]).first)
    }
}
