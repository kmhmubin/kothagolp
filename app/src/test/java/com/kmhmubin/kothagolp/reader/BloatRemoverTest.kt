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
}
