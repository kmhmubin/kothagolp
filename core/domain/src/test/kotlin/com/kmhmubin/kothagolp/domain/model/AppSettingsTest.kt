package com.kmhmubin.kothagolp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultSettings_haveExpectedValues() {
        val settings = AppSettings()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.amoledBlack)
        assertFalse(settings.useDynamicColor)
    }

    @Test
    fun readingStatus_displayName_notBlank() {
        ReadingStatus.entries.forEach { status ->
            assertTrue("${status.name} should have a display name", status.displayName().isNotBlank())
        }
    }

    @Test
    fun readingStatus_fromString_roundtrip() {
        ReadingStatus.entries.forEach { status ->
            assertEquals(status, ReadingStatus.fromString(status.name))
        }
    }

    @Test
    fun readingStatus_fromString_unknownFallsToReading() {
        assertEquals(ReadingStatus.READING, ReadingStatus.fromString("UNKNOWN_STATUS"))
    }

    @Test
    fun chapterDisplayMode_enumValues() {
        assertEquals(2, ChapterDisplayMode.entries.size)
        assertTrue(ChapterDisplayMode.entries.contains(ChapterDisplayMode.SCROLL))
        assertTrue(ChapterDisplayMode.entries.contains(ChapterDisplayMode.PAGINATED))
    }

    @Test
    fun chaptersPerPage_fromValue_returnsCorrectEntry() {
        assertEquals(ChaptersPerPage.TWENTY_FIVE, ChaptersPerPage.fromValue(25))
        assertEquals(ChaptersPerPage.FIFTY, ChaptersPerPage.fromValue(50))
        assertEquals(ChaptersPerPage.HUNDRED, ChaptersPerPage.fromValue(100))
        assertEquals(ChaptersPerPage.FIFTY, ChaptersPerPage.fromValue(-999))
    }

    @Test
    fun paginationState_getTotalPages_calculatesCorrectly() {
        val state = PaginationState(currentPage = 1, chaptersPerPage = ChaptersPerPage.FIFTY)
        assertEquals(2, state.getTotalPages(75))
        assertEquals(1, state.getTotalPages(50))
        assertEquals(3, state.getTotalPages(101))
    }

    @Test
    fun paginationState_all_alwaysOnePage() {
        val state = PaginationState(currentPage = 1, chaptersPerPage = ChaptersPerPage.ALL)
        assertEquals(1, state.getTotalPages(1000))
    }

    @Test
    fun authorNoteDisplayMode_fromId_roundtrip() {
        AuthorNoteDisplayMode.entries.forEach { mode ->
            assertEquals(mode, AuthorNoteDisplayMode.fromId(mode.id))
        }
    }

    @Test
    fun authorNoteDisplayMode_fromId_unknownFallsToCollapsed() {
        assertEquals(AuthorNoteDisplayMode.COLLAPSED, AuthorNoteDisplayMode.fromId("nonexistent_id"))
    }

    @Test
    fun gridColumns_fixed_returnsCount() {
        val fixed = GridColumns.Fixed(4)
        assertEquals(4, fixed.count)
    }
}
