package com.kmhmubin.kothagolp.reader

import com.kmhmubin.kothagolp.ui.screens.reader.logic.BloatRemover
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloatRemoverTest {

    @Test
    fun `strips duplicated chapter title and credit lines`() {
        val html = """
            <div>
            <p>Chapter 1: Starting Over</p>
            <p><strong>Translator:</strong> Hellscythe_</p>
            <p><strong>Editor:</strong> Dank_Frank</p>
            <p>The story begins on a cold morning.</p>
            <p>He said: "the translator of this ancient text was long dead" and walked on.</p>
            </div>
        """.trimIndent()

        val out = BloatRemover.strip(html, "Chapter 1: Starting Over")

        assertFalse(out.contains("Starting Over"))
        assertFalse(out.contains("Hellscythe_"))
        assertFalse(out.contains("Dank_Frank"))
        assertTrue(out.contains("cold morning"))
        // credit word mid-sentence must survive
        assertTrue(out.contains("ancient text"))
    }

    @Test
    fun `removes generic chapter heading when name differs`() {
        val html = "<h3>Chapter 42 - The Fall</h3><p>Real content here.</p>"
        val out = BloatRemover.strip(html, "The Fall")
        assertFalse(out.contains("Chapter 42"))
        assertTrue(out.contains("Real content"))
    }

    @Test
    fun `long paragraphs mentioning translator survive`() {
        val filler = "word ".repeat(40)
        val html = "<p>Translator: this is actually part of the story where a character reads a note aloud, $filler</p>"
        val out = BloatRemover.strip(html, null)
        assertTrue(out.contains("part of the story"))
    }

    @Test
    fun `null chapter name and broken html are safe`() {
        val out = BloatRemover.strip("<p>Only content</p>", null)
        assertTrue(out.contains("Only content"))
    }

    @Test
    fun `strips quality checker and decorated credit lines`() {
        val html = """
            <p>— TL: SomeGuy</p>
            <p>[Editor: OtherGuy]</p>
            <p>Quality Checker: ThirdGuy</p>
            <p>QC: FourthGuy</p>
            <p>Proofread by: FifthGuy</p>
            <p>Actual story text goes here.</p>
        """.trimIndent()
        val out = BloatRemover.strip(html, null)
        assertFalse(out.contains("SomeGuy"))
        assertFalse(out.contains("OtherGuy"))
        assertFalse(out.contains("ThirdGuy"))
        assertFalse(out.contains("FourthGuy"))
        assertFalse(out.contains("FifthGuy"))
        assertTrue(out.contains("Actual story text"))
    }

    @Test
    fun `fuzzy title match survives chapter number prefix differences`() {
        // source list name has the prefix, content heading does not (and vice versa)
        val html = "<p><strong>The Hunt Begins</strong></p><p>The forest was quiet.</p>"
        val out = BloatRemover.strip(html, "Chapter 12 - The Hunt Begins")
        assertFalse(out.contains("Hunt Begins"))
        assertTrue(out.contains("forest was quiet"))
    }

    @Test
    fun `title inside wrapper divs still removed`() {
        val html = """
            <div class="outer"><div class="inner">
            <h2><span>Chapter 7: Rebirth</span></h2>
            <p>Content paragraph one.</p>
            </div></div>
        """.trimIndent()
        val out = BloatRemover.strip(html, "Chapter 7: Rebirth")
        assertFalse(out.contains("Rebirth</span>"))
        assertTrue(out.contains("Content paragraph one"))
    }

    @Test
    fun `strips combined and note-style credit lines`() {
        val html = """
            <p>Translator/Editor: ComboGuy</p>
            <p>Raw/Consultant: RawGuy</p>
            <p>TL &amp; ED: TeamGuy</p>
            <p>Editor Notes: fixed some typos this week</p>
            <p>Translator's Note: names use pinyin</p>
            <p>The real chapter text continues here.</p>
        """.trimIndent()
        val out = BloatRemover.strip(html, null)
        assertFalse(out.contains("ComboGuy"))
        assertFalse(out.contains("RawGuy"))
        assertFalse(out.contains("TeamGuy"))
        assertFalse(out.contains("fixed some typos"))
        assertFalse(out.contains("pinyin"))
        assertTrue(out.contains("real chapter text"))
    }

    @Test
    fun `words merely starting with a role prefix are kept`() {
        val html = "<p>Transformation: the ancient art he studied for years, was finally complete.</p>"
        val out = BloatRemover.strip(html, null)
        assertTrue(out.contains("ancient art"))
    }

    @Test
    fun `prose paragraph starting with chapter word is kept`() {
        val long = "Chapter 3 had been the hardest week of his life, he thought, " +
            "as he stared out over the ruined city and remembered everything that led here."
        val out = BloatRemover.strip("<p>$long</p>", "Chapter 99: Elsewhere")
        assertTrue(out.contains("ruined city"))
    }
}
