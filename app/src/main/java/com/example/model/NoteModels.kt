package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class ElementType(val shortLabel: String, val displayName: String) {
    H1("H1", "Section Heading"),
    H2("H2", "Sub-heading"),
    H3("H3", "Sub-topic"),
    P("P", "Note / Definition"),
    F("F", "Formula");

    val isHeading: Boolean
        get() = this == H1 || this == H2 || this == H3

    val headingLevel: Int
        get() = when (this) {
            H1 -> 1
            H2 -> 2
            H3 -> 3
            P, F -> 0
        }
}

enum class RevisionStatus(val label: String) {
    PENDING("Review"),
    MASTERED("Mastered")
}

enum class SubjectAccent(val label: String) {
    INDIGO("Indigo"),
    AMBER("Amber"),
    EMERALD("Mint")
}

@JsonClass(generateAdapter = true)
data class NoteBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: ElementType,
    val title: String = "",
    val body: String = "",
    val isExpanded: Boolean = true,
    val status: RevisionStatus = RevisionStatus.PENDING
)

@JsonClass(generateAdapter = true)
data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val blocks: List<NoteBlock> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String = "",
    val examLabel: String = "",
    val accent: SubjectAccent = SubjectAccent.INDIGO,
    val chapters: List<Chapter> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SubjectIndexSnapshot(
    val subjectId: String,
    val updatedAt: Long
)
