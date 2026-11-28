package com.example.domain

import com.example.model.Chapter
import com.example.model.ElementType
import com.example.model.NoteBlock
import com.example.model.RevisionStatus
import com.example.model.Subject

data class NumberedBlock(
    val block: NoteBlock,
    val numberLabel: String,
    val indentDepth: Int,
    val isVisible: Boolean,
    val childCount: Int,
    val parentHeadingLevel: Int
)

data class ChapterIndexEntry(
    val blockId: String,
    val numberLabel: String,
    val title: String,
    val level: Int,
    val status: RevisionStatus,
    val childNotesCount: Int
)

data class SubjectIndexH1Entry(
    val blockId: String,
    val numberLabel: String,
    val title: String,
    val status: RevisionStatus
)

data class SubjectIndexChapter(
    val chapterId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val h1Entries: List<SubjectIndexH1Entry>
)

data class ChapterStats(
    val h1Count: Int,
    val h2Count: Int,
    val h3Count: Int,
    val noteCount: Int,
    val formulaCount: Int,
    val masteredHeadings: Int,
    val totalHeadings: Int
)

object OutlineEngine {

    fun computeChapterOutline(
        chapterIndex: Int,
        blocks: List<NoteBlock>
    ): List<NumberedBlock> {
        var h1 = 0
        var h2 = 0
        var h3 = 0

        var currentHeadingDepth = 0
        var h1Expanded = true
        var h2Expanded = true
        var h3Expanded = true

        val result = ArrayList<NumberedBlock>(blocks.size)

        for (i in blocks.indices) {
            val block = blocks[i]
            val childCount = if (block.type.isHeading) {
                countDescendants(blocks, i, block.type.headingLevel)
            } else {
                0
            }

            when (block.type) {
                ElementType.H1 -> {
                    h1 += 1
                    h2 = 0
                    h3 = 0
                    currentHeadingDepth = 1
                    h1Expanded = block.isExpanded
                    h2Expanded = true
                    h3Expanded = true
                    result.add(
                        NumberedBlock(
                            block = block,
                            numberLabel = "$chapterIndex.$h1",
                            indentDepth = 0,
                            isVisible = true,
                            childCount = childCount,
                            parentHeadingLevel = 0
                        )
                    )
                }

                ElementType.H2 -> {
                    h2 += 1
                    h3 = 0
                    val visible = h1Expanded
                    currentHeadingDepth = 2
                    h2Expanded = block.isExpanded
                    h3Expanded = true
                    result.add(
                        NumberedBlock(
                            block = block,
                            numberLabel = "$chapterIndex.$h1.$h2",
                            indentDepth = 1,
                            isVisible = visible,
                            childCount = childCount,
                            parentHeadingLevel = 1
                        )
                    )
                }

                ElementType.H3 -> {
                    h3 += 1
                    val visible = h1Expanded && h2Expanded
                    val h3Indent = if (h2 == 0) 1 else 2
                    currentHeadingDepth = h3Indent + 1
                    h3Expanded = block.isExpanded
                    result.add(
                        NumberedBlock(
                            block = block,
                            numberLabel = "$chapterIndex.$h1.$h2.$h3",
                            indentDepth = h3Indent,
                            isVisible = visible,
                            childCount = childCount,
                            parentHeadingLevel = if (h2 == 0) 1 else 2
                        )
                    )
                }

                ElementType.P, ElementType.F -> {
                    val visible = when (currentHeadingDepth) {
                        0 -> true
                        1 -> h1Expanded
                        2 -> h1Expanded && h2Expanded
                        else -> h1Expanded && h2Expanded && h3Expanded
                    }
                    result.add(
                        NumberedBlock(
                            block = block,
                            numberLabel = "",
                            indentDepth = currentHeadingDepth,
                            isVisible = visible,
                            childCount = 0,
                            parentHeadingLevel = currentHeadingDepth
                        )
                    )
                }
            }
        }
        return result
    }

    private fun countDescendants(
        blocks: List<NoteBlock>,
        headingIndex: Int,
        headingLevel: Int
    ): Int {
        var count = 0
        for (j in (headingIndex + 1) until blocks.size) {
            val next = blocks[j]
            if (next.type.isHeading && next.type.headingLevel <= headingLevel) {
                break
            }
            count += 1
        }
        return count
    }

    /**
     * Auto-generated Chapter Index: up to 2 heading levels (H1 and H2).
     */
    fun computeChapterIndex(
        chapterIndex: Int,
        chapter: Chapter
    ): List<ChapterIndexEntry> {
        val outlined = computeChapterOutline(chapterIndex, chapter.blocks)
        return outlined.mapIndexedNotNull { idx, item ->
            val type = item.block.type
            if (type == ElementType.H1 || type == ElementType.H2) {
                val directChildNotes = countDirectNotesUntilNextH1OrH2(chapter.blocks, idx)
                ChapterIndexEntry(
                    blockId = item.block.id,
                    numberLabel = item.numberLabel,
                    title = item.block.title.ifBlank { "Untitled ${type.shortLabel}" },
                    level = type.headingLevel,
                    status = item.block.status,
                    childNotesCount = directChildNotes
                )
            } else {
                null
            }
        }
    }

    private fun countDirectNotesUntilNextH1OrH2(
        blocks: List<NoteBlock>,
        startIndex: Int
    ): Int {
        val level = blocks[startIndex].type.headingLevel
        var count = 0
        for (j in (startIndex + 1) until blocks.size) {
            val next = blocks[j]
            if (next.type.isHeading && next.type.headingLevel <= level) {
                break
            }
            if (!next.type.isHeading) {
                count += 1
            }
        }
        return count
    }

    /**
     * Auto-generated Subject Index: up to 1 heading level (H1 only per chapter).
     */
    fun computeSubjectIndex(subject: Subject): List<SubjectIndexChapter> {
        return subject.chapters.mapIndexed { idx, chapter ->
            val chapterNum = idx + 1
            val outlined = computeChapterOutline(chapterNum, chapter.blocks)
            val h1s = outlined
                .filter { it.block.type == ElementType.H1 }
                .map { item ->
                    SubjectIndexH1Entry(
                        blockId = item.block.id,
                        numberLabel = item.numberLabel,
                        title = item.block.title.ifBlank { "Untitled Section" },
                        status = item.block.status
                    )
                }
            SubjectIndexChapter(
                chapterId = chapter.id,
                chapterIndex = chapterNum,
                chapterTitle = chapter.title,
                h1Entries = h1s
            )
        }
    }

    fun computeChapterStats(chapter: Chapter): ChapterStats {
        var h1 = 0
        var h2 = 0
        var h3 = 0
        var notes = 0
        var formulas = 0
        var mastered = 0
        for (block in chapter.blocks) {
            when (block.type) {
                ElementType.H1 -> {
                    h1++
                    if (block.status == RevisionStatus.MASTERED) mastered++
                }
                ElementType.H2 -> {
                    h2++
                    if (block.status == RevisionStatus.MASTERED) mastered++
                }
                ElementType.H3 -> {
                    h3++
                    if (block.status == RevisionStatus.MASTERED) mastered++
                }
                ElementType.P -> notes++
                ElementType.F -> formulas++
            }
        }
        return ChapterStats(
            h1Count = h1,
            h2Count = h2,
            h3Count = h3,
            noteCount = notes,
            formulaCount = formulas,
            masteredHeadings = mastered,
            totalHeadings = h1 + h2 + h3
        )
    }

    fun expandParentsForBlock(
        blocks: List<NoteBlock>,
        targetBlockId: String
    ): List<NoteBlock> {
        val targetIndex = blocks.indexOfFirst { it.id == targetBlockId }
        if (targetIndex == -1) return blocks

        var parentH1Id: String? = null
        var parentH2Id: String? = null
        var parentH3Id: String? = null

        for (i in 0..targetIndex) {
            val block = blocks[i]
            when (block.type) {
                ElementType.H1 -> {
                    parentH1Id = block.id
                    parentH2Id = null
                    parentH3Id = null
                }
                ElementType.H2 -> {
                    parentH2Id = block.id
                    parentH3Id = null
                }
                ElementType.H3 -> {
                    parentH3Id = block.id
                }
                else -> Unit
            }
        }

        val idsToExpand = setOfNotNull(parentH1Id, parentH2Id, parentH3Id, targetBlockId)
        return blocks.map { block ->
            if (block.type.isHeading && block.id in idsToExpand && !block.isExpanded) {
                block.copy(isExpanded = true)
            } else {
                block
            }
        }
    }

    fun exportChapterMarkdown(chapterIndex: Int, chapter: Chapter): String {
        val sb = StringBuilder()
        sb.appendLine("# Chapter $chapterIndex: ${chapter.title}")
        sb.appendLine()
        val outlined = computeChapterOutline(chapterIndex, chapter.blocks)
        for (item in outlined) {
            val b = item.block
            when (b.type) {
                ElementType.H1 -> sb.appendLine("## ${item.numberLabel}: ${b.title}")
                ElementType.H2 -> sb.appendLine("### ${item.numberLabel} ${b.title}")
                ElementType.H3 -> sb.appendLine("#### ${item.numberLabel} ${b.title}")
                ElementType.P -> {
                    b.body.lines().forEach { line ->
                        if (line.isNotBlank()) sb.appendLine("  $line")
                    }
                    sb.appendLine()
                }
                ElementType.F -> {
                    if (b.title.isNotBlank()) sb.appendLine("  **Formula (${b.title}):**")
                    sb.appendLine("  ```")
                    sb.appendLine("  ${b.body}")
                    sb.appendLine("  ```")
                    sb.appendLine()
                }
            }
        }
        return sb.toString().trimEnd()
    }

    fun exportSubjectMarkdown(subject: Subject): String {
        val sb = StringBuilder()
        val header = if (subject.code.isNotBlank()) "${subject.name} (${subject.code})" else subject.name
        sb.appendLine("# $header")
        sb.appendLine()
        sb.appendLine("## Subject Index")
        computeSubjectIndex(subject).forEach { ch ->
            sb.appendLine("- Chapter ${ch.chapterIndex}: ${ch.chapterTitle}")
            ch.h1Entries.forEach { h1 ->
                sb.appendLine("  - ${h1.numberLabel}: ${h1.title}")
            }
        }
        sb.appendLine()
        subject.chapters.forEachIndexed { index, chapter ->
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine(exportChapterMarkdown(index + 1, chapter))
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    /**
     * Strict hierarchy rules:
     * - Chapter root (null): H1 only
     * - H1: H2, H3, P, F
     * - H2: H3, P, F
     * - H3: P, F
     * - P: F
     * - F: none
     */
    fun allowedChildren(parentType: ElementType?): List<ElementType> = when (parentType) {
        null -> listOf(ElementType.H1)
        ElementType.H1 -> listOf(ElementType.H2, ElementType.H3, ElementType.P, ElementType.F)
        ElementType.H2 -> listOf(ElementType.H3, ElementType.P, ElementType.F)
        ElementType.H3 -> listOf(ElementType.P, ElementType.F)
        ElementType.P -> listOf(ElementType.F)
        ElementType.F -> emptyList()
    }

    fun sanitizeHierarchy(blocks: List<NoteBlock>): List<NoteBlock> {
        return blocks.dropWhile { it.type != ElementType.H1 }
    }

    fun canMoveBlock(
        blocks: List<NoteBlock>,
        blockId: String,
        direction: Int
    ): Boolean = moveBlock(blocks, blockId, direction) != blocks

    fun moveBlock(
        blocks: List<NoteBlock>,
        blockId: String,
        direction: Int
    ): List<NoteBlock> {
        if (direction == 0 || blocks.size < 2) return blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return blocks
        val target = blocks[idx]

        return if (target.type.isHeading) {
            moveHeadingSection(blocks, idx, target.type.headingLevel, direction)
        } else {
            moveLeafBlock(blocks, idx, direction)
        }
    }

    private fun moveHeadingSection(
        blocks: List<NoteBlock>,
        idx: Int,
        level: Int,
        direction: Int
    ): List<NoteBlock> {
        var sectionEnd = idx
        while (sectionEnd + 1 < blocks.size) {
            val next = blocks[sectionEnd + 1]
            if (next.type.isHeading && next.type.headingLevel <= level) break
            sectionEnd++
        }

        val parentStart: Int
        val parentEnd: Int
        if (level == 1) {
            parentStart = 0
            parentEnd = blocks.lastIndex
        } else {
            var parentIdx = -1
            for (k in (idx - 1) downTo 0) {
                val candidate = blocks[k]
                if (candidate.type.isHeading && candidate.type.headingLevel < level) {
                    parentIdx = k
                    break
                }
            }
            if (parentIdx < 0) return blocks
            parentStart = parentIdx + 1

            var end = sectionEnd
            while (end + 1 < blocks.size) {
                val next = blocks[end + 1]
                if (next.type.isHeading && next.type.headingLevel < level) break
                end++
            }
            parentEnd = end
        }

        val result = blocks.toMutableList()
        if (direction < 0) {
            if (idx <= parentStart) return blocks
            val prevUnitStart = findPreviousSiblingStart(blocks, idx, level, parentStart)
                ?: return blocks
            val moving = result.subList(idx, sectionEnd + 1).toList()
            val preceding = result.subList(prevUnitStart, idx).toList()
            val combined = moving + preceding
            for (offset in combined.indices) {
                result[prevUnitStart + offset] = combined[offset]
            }
            return result
        } else {
            if (sectionEnd >= parentEnd) return blocks
            val nextStart = sectionEnd + 1
            var nextEnd = nextStart
            while (nextEnd + 1 <= parentEnd) {
                val next = blocks[nextEnd + 1]
                if (next.type.isHeading && next.type.headingLevel <= level) break
                nextEnd++
            }
            val moving = result.subList(idx, sectionEnd + 1).toList()
            val following = result.subList(nextStart, nextEnd + 1).toList()
            val combined = following + moving
            for (offset in combined.indices) {
                result[idx + offset] = combined[offset]
            }
            return result
        }
    }

    private fun findPreviousSiblingStart(
        blocks: List<NoteBlock>,
        idx: Int,
        level: Int,
        parentStart: Int
    ): Int? {
        if (idx <= parentStart) return null
        if (level == 1) {
            for (k in (idx - 1) downTo parentStart) {
                if (blocks[k].type == ElementType.H1) return k
            }
            return null
        }

        // If a sibling heading of the exact same level exists between parentStart and idx,
        // move above the nearest sibling heading section.
        for (k in (idx - 1) downTo parentStart) {
            if (blocks[k].type.isHeading && blocks[k].type.headingLevel == level) {
                return k
            }
        }

        // Otherwise we're preceded by direct children (e.g. P, F, or H3 under H1).
        val immediatePrev = blocks[idx - 1]
        if (!immediatePrev.type.isHeading) {
            // Check if immediatePrev sits inside a deeper heading (e.g. H3 directly under H1 while moving H2)
            for (k in (idx - 1) downTo parentStart) {
                val candidate = blocks[k]
                if (candidate.type.isHeading && candidate.type.headingLevel > level) {
                    return k
                }
            }
            return idx - 1
        }
        return idx - 1
    }

    private fun moveLeafBlock(
        blocks: List<NoteBlock>,
        idx: Int,
        direction: Int
    ): List<NoteBlock> {
        val targetIdx = idx + direction
        if (targetIdx !in blocks.indices) return blocks
        // Never allow P or F to be placed at index 0 (root chapter must start with H1).
        if (direction < 0 && targetIdx == 0) return blocks
        // Never swap past the root H1 at index 0.
        if (blocks[0].type == ElementType.H1 && targetIdx == 0) return blocks

        val result = blocks.toMutableList()
        val item = result.removeAt(idx)
        result.add(targetIdx, item)
        return result
    }
}
