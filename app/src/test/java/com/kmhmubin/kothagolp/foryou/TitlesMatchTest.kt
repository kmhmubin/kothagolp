package com.kmhmubin.kothagolp.foryou

import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.titlesMatch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitlesMatchTest {
    @Test fun exact() = assertTrue(titlesMatch("overlord", "overlord"))
    @Test fun shortPrefixOfLongerTitleRejected() = assertFalse(titlesMatch("overlord", "overlord of the system"))
    @Test fun subtitleVariantAccepted() = assertTrue(titlesMatch("the legendary mechanic", "legendary mechanic"))
    @Test fun eitherOrder() = assertTrue(titlesMatch("legendary mechanic", "the legendary mechanic"))
    @Test fun unrelated() = assertFalse(titlesMatch("lord of mysteries", "shadow slave"))
    @Test fun emptyNeverMatchesNonEmpty() = assertFalse(titlesMatch("", "shadow slave"))
}
