package com.example

import com.example.domain.OutlineEngine
import com.example.model.Chapter
import com.example.model.ElementType
import com.example.model.NoteBlock
import com.example.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlineEngineTest {

    @Test
    fun `auto numbering generates chapter-prefixed H1 H2 H3 numbers and resets properly`() {
        val blocks = listOf(
            NoteBlock(id = "h1a", type = ElementType.H1, title = "Conductor"),
            NoteBlock(id = "p1", type = ElementType.P, body = "Free electrons"),
            NoteBlock(id = "h1b", type = ElementType.H1, title = "Semiconductor"),
            NoteBlock(id = "h2a", type = ElementType.H2, title = "Intrinsic"),
            NoteBlock(id = "h3a", type = ElementType.H3, title = "Silicon Lattice"),
            NoteBlock(id = "f1", type = ElementType.F, title = "Law", body = "n * p = ni^2"),
            NoteBlock(id = "h2b", type = ElementType.H2, title = "Extrinsic"),
            NoteBlock(id = "h3b", type = ElementType.H3, title = "N-Type"),
            NoteBlock(id = "h1c", type = ElementType.H1, title = "Insulator")
        )

        val outlined = OutlineEngine.computeChapterOutline(chapterIndex = 2, blocks = blocks)
        val numbers = outlined.associate { it.block.id to it.numberLabel }

        assertEquals("2.1", numbers["h1a"])
        assertEquals("", numbers["p1"])
        assertEquals("2.2", numbers["h1b"])
        assertEquals("2.2.1", numbers["h2a"])
        assertEquals("2.2.1.1", numbers["h3a"])
        assertEquals("", numbers["f1"])
        assertEquals("2.2.2", numbers["h2b"])
        assertEquals("2.2.2.1", numbers["h3b"])
        assertEquals("2.3", numbers["h1c"])
    }

    @Test
    fun `collapsing H1 hides H2 H3 P F until next H1`() {
        val blocks = listOf(
            NoteBlock(id = "h1a", type = ElementType.H1, title = "Semiconductor", isExpanded = false),
            NoteBlock(id = "p1", type = ElementType.P, body = "Definition"),
            NoteBlock(id = "h2a", type = ElementType.H2, title = "Intrinsic", isExpanded = true),
            NoteBlock(id = "f1", type = ElementType.F, body = "n = p"),
            NoteBlock(id = "h1b", type = ElementType.H1, title = "Insulator", isExpanded = true),
            NoteBlock(id = "p2", type = ElementType.P, body = "Wide band gap")
        )

        val outlined = OutlineEngine.computeChapterOutline(2, blocks).associateBy { it.block.id }

        assertTrue(outlined.getValue("h1a").isVisible)
        assertFalse(outlined.getValue("p1").isVisible)
        assertFalse(outlined.getValue("h2a").isVisible)
        assertFalse(outlined.getValue("f1").isVisible)
        assertTrue(outlined.getValue("h1b").isVisible)
        assertTrue(outlined.getValue("p2").isVisible)
        assertEquals(3, outlined.getValue("h1a").childCount)
    }

    @Test
    fun `chapter index includes up to 2 headings and subject index includes up to 1 heading`() {
        val ch1 = Chapter(
            id = "c1",
            title = "Bonding",
            blocks = listOf(
                NoteBlock(id = "c1h1", type = ElementType.H1, title = "Covalent"),
                NoteBlock(id = "c1h2", type = ElementType.H2, title = "Silicon")
            )
        )
        val ch2 = Chapter(
            id = "c2",
            title = "Conductor, Semiconductor & Insulator",
            blocks = listOf(
                NoteBlock(id = "c2h1", type = ElementType.H1, title = "Conductor"),
                NoteBlock(id = "c2h1_2", type = ElementType.H1, title = "Semiconductor"),
                NoteBlock(id = "c2h2", type = ElementType.H2, title = "Intrinsic"),
                NoteBlock(id = "c2h3", type = ElementType.H3, title = "Lattice"),
                NoteBlock(id = "c2p", type = ElementType.P, body = "Details")
            )
        )
        val subject = Subject(id = "s1", name = "Physics", chapters = listOf(ch1, ch2))

        val chapterIndex = OutlineEngine.computeChapterIndex(2, ch2)
        assertEquals(listOf("2.1", "2.2", "2.2.1"), chapterIndex.map { it.numberLabel })
        assertEquals(listOf(1, 1, 2), chapterIndex.map { it.level })

        val subjectIndex = OutlineEngine.computeSubjectIndex(subject)
        assertEquals(2, subjectIndex.size)
        assertEquals(listOf("1.1"), subjectIndex[0].h1Entries.map { it.numberLabel })
        assertEquals(listOf("2.1", "2.2"), subjectIndex[1].h1Entries.map { it.numberLabel })
    }

    @Test
    fun `allowedChildren enforces strict chapter-heading-paragraph-formula hierarchy`() {
        assertEquals(listOf(ElementType.H1), OutlineEngine.allowedChildren(null))
        assertEquals(
            listOf(ElementType.H2, ElementType.H3, ElementType.P, ElementType.F),
            OutlineEngine.allowedChildren(ElementType.H1)
        )
        assertEquals(
            listOf(ElementType.H3, ElementType.P, ElementType.F),
            OutlineEngine.allowedChildren(ElementType.H2)
        )
        assertEquals(
            listOf(ElementType.P, ElementType.F),
            OutlineEngine.allowedChildren(ElementType.H3)
        )
        assertEquals(listOf(ElementType.F), OutlineEngine.allowedChildren(ElementType.P))
        assertEquals(emptyList<ElementType>(), OutlineEngine.allowedChildren(ElementType.F))
    }

    @Test
    fun `moveBlock never places P F H2 or H3 under root chapter and moves heading sections cleanly`() {
        val h1a = NoteBlock(id = "h1a", type = ElementType.H1, title = "Conductor")
        val p1 = NoteBlock(id = "p1", type = ElementType.P, body = "Free electrons")
        val h2a = NoteBlock(id = "h2a", type = ElementType.H2, title = "Band")
        val f1 = NoteBlock(id = "f1", type = ElementType.F, body = "E = 0")
        val h1b = NoteBlock(id = "h1b", type = ElementType.H1, title = "Semiconductor")
        val p2 = NoteBlock(id = "p2", type = ElementType.P, body = "Silicon")
        val blocks = listOf(h1a, p1, h2a, f1, h1b, p2)

        // P at index 1 cannot move up above first H1 at index 0
        assertFalse(OutlineEngine.canMoveBlock(blocks, "p1", -1))
        assertEquals(blocks, OutlineEngine.moveBlock(blocks, "p1", -1))

        // First H1 moves down WITH its subtree, never stranding p1 at root chapter
        val movedH1Down = OutlineEngine.moveBlock(blocks, "h1a", 1)
        assertEquals(listOf("h1b", "p2", "h1a", "p1", "h2a", "f1"), movedH1Down.map { it.id })
        assertEquals(ElementType.H1, movedH1Down.first().type)

        // Second H1 moves up WITH its subtree
        val movedH1Up = OutlineEngine.moveBlock(blocks, "h1b", -1)
        assertEquals(movedH1Down, movedH1Up)

        // H2 cannot move up past its parent H1 or down past its parent H1
        assertFalse(OutlineEngine.canMoveBlock(blocks, "h2a", 1))
        val movedH2Up = OutlineEngine.moveBlock(blocks, "h2a", -1)
        assertEquals(listOf("h1a", "h2a", "f1", "p1", "h1b", "p2"), movedH2Up.map { it.id })
        assertFalse(OutlineEngine.canMoveBlock(movedH2Up, "h2a", -1))
    }

    @Test
    fun `very long paragraph preserves full text and expands collapsed parent heading`() {
        val longBody = buildString {
            append("Definition: ")
            repeat(500) { idx ->
                append("Line $idx of detailed exam revision notes covering theory, proof, and edge cases.\n")
            }
        }
        assertTrue(longBody.length > 25_000)

        val h1 = NoteBlock(id = "h1", type = ElementType.H1, title = "Matrix", isExpanded = false)
        val pLong = NoteBlock(id = "p_long", type = ElementType.P, body = longBody)
        val expanded = OutlineEngine.expandParentsForBlock(listOf(h1, pLong), "p_long")

        val outlined = OutlineEngine.computeChapterOutline(1, expanded).associateBy { it.block.id }
        assertTrue(outlined.getValue("h1").block.isExpanded)
        assertTrue(outlined.getValue("p_long").isVisible)
        assertEquals(longBody, outlined.getValue("p_long").block.body)
    }
}
