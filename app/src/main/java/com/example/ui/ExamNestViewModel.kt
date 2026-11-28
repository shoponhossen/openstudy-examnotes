package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileNoteRepository
import com.example.domain.OutlineEngine
import com.example.model.Chapter
import com.example.model.ElementType
import com.example.model.NoteBlock
import com.example.model.RevisionStatus
import com.example.model.Subject
import com.example.model.SubjectAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class LibraryUiState(
    val subjects: List<Subject> = emptyList(),
    val searchQuery: String = "",
    val isLoaded: Boolean = false,
    val linkedFolderName: String? = null
)

class ExamNestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileNoteRepository(application.applicationContext)
    private val searchQuery = MutableStateFlow("")

    val libraryState: StateFlow<LibraryUiState> = combine(
        repository.subjects,
        searchQuery,
        repository.isLoaded,
        repository.linkedFolderName
    ) { subjects, query, loaded, linkedFolder ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            subjects
        } else {
            subjects.filter { subject ->
                subject.name.contains(trimmed, ignoreCase = true) ||
                    subject.code.contains(trimmed, ignoreCase = true) ||
                    subject.chapters.any { chapter ->
                        chapter.title.contains(trimmed, ignoreCase = true) ||
                            chapter.blocks.any { block ->
                                block.title.contains(trimmed, ignoreCase = true) ||
                                    block.body.contains(trimmed, ignoreCase = true)
                            }
                    }
            }
        }
        LibraryUiState(
            subjects = filtered,
            searchQuery = query,
            isLoaded = loaded,
            linkedFolderName = linkedFolder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    val allSubjects: StateFlow<List<Subject>> = repository.subjects

    init {
        viewModelScope.launch {
            repository.loadAll()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun linkDocumentsFolder(uri: android.net.Uri, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val restored = repository.linkDocumentsFolder(uri)
            onComplete(restored)
        }
    }

    fun unlinkDocumentsFolder() {
        viewModelScope.launch {
            repository.unlinkDocumentsFolder()
        }
    }

    fun createOrUpdateSubject(
        existingId: String? = null,
        name: String,
        code: String = "",
        accent: SubjectAccent
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val existing = existingId?.let { id ->
                repository.subjects.value.firstOrNull { it.id == id }
            }
            val updated = existing?.copy(
                name = cleanName,
                code = code.trim(),
                accent = accent
            ) ?: Subject(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                code = code.trim(),
                accent = accent
            )
            repository.saveSubject(updated)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    fun addChapter(subjectId: String, title: String, onCreated: (String) -> Unit = {}) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val newChapter = Chapter(
            id = UUID.randomUUID().toString(),
            title = cleanTitle
        )
        viewModelScope.launch {
            repository.mutateSubject(subjectId) { subject ->
                subject.copy(chapters = subject.chapters + newChapter)
            }
            onCreated(newChapter.id)
        }
    }

    fun renameChapter(subjectId: String, chapterId: String, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) return
        updateChapter(subjectId, chapterId) { it.copy(title = cleanTitle) }
    }

    fun deleteChapter(subjectId: String, chapterId: String) {
        viewModelScope.launch {
            repository.mutateSubject(subjectId) { subject ->
                subject.copy(chapters = subject.chapters.filterNot { it.id == chapterId })
            }
        }
    }

    fun moveChapter(subjectId: String, chapterId: String, direction: Int) {
        viewModelScope.launch {
            repository.mutateSubject(subjectId) { subject ->
                val idx = subject.chapters.indexOfFirst { it.id == chapterId }
                val targetIdx = idx + direction
                if (idx < 0 || targetIdx !in subject.chapters.indices) return@mutateSubject subject
                val mutable = subject.chapters.toMutableList()
                val item = mutable.removeAt(idx)
                mutable.add(targetIdx, item)
                subject.copy(chapters = mutable)
            }
        }
    }

    fun insertBlockAndGetId(
        subjectId: String,
        chapterId: String,
        insertAfterBlockId: String?,
        type: ElementType
    ): String {
        val newId = UUID.randomUUID().toString()
        val newBlock = NoteBlock(
            id = newId,
            type = type,
            title = "",
            body = "",
            isExpanded = true
        )
        updateChapter(subjectId, chapterId) { chapter ->
            val currentBlocks = chapter.blocks.toMutableList()
            val anchorIdx = insertAfterBlockId?.let { anchor ->
                currentBlocks.indexOfFirst { it.id == anchor }
            } ?: -1
            val parentType = if (anchorIdx >= 0) currentBlocks[anchorIdx].type else null
            if (type !in OutlineEngine.allowedChildren(parentType)) {
                return@updateChapter chapter
            }
            if (anchorIdx >= 0) {
                currentBlocks.add(anchorIdx + 1, newBlock)
            } else {
                currentBlocks.add(newBlock)
            }
            val expandedBlocks = OutlineEngine.expandParentsForBlock(currentBlocks, newBlock.id)
            chapter.copy(blocks = expandedBlocks)
        }
        return newId
    }

    fun saveBlock(
        subjectId: String,
        chapterId: String,
        existingBlockId: String?,
        insertAfterBlockId: String?,
        type: ElementType,
        title: String,
        body: String
    ) {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (type.isHeading && cleanTitle.isEmpty()) return
        if (!type.isHeading && cleanBody.isEmpty()) return

        updateChapter(subjectId, chapterId) { chapter ->
            val currentBlocks = chapter.blocks.toMutableList()
            val finalBlocks = if (existingBlockId != null) {
                val idx = currentBlocks.indexOfFirst { it.id == existingBlockId }
                if (idx >= 0) {
                    val prev = currentBlocks[idx]
                    currentBlocks[idx] = prev.copy(
                        type = type,
                        title = if (type.isHeading || type == ElementType.F) cleanTitle else "",
                        body = if (!type.isHeading) cleanBody else ""
                    )
                }
                currentBlocks
            } else {
                val anchorIdx = insertAfterBlockId?.let { anchor ->
                    currentBlocks.indexOfFirst { it.id == anchor }
                } ?: -1
                val parentType = if (anchorIdx >= 0) currentBlocks[anchorIdx].type else null
                if (type !in OutlineEngine.allowedChildren(parentType)) {
                    return@updateChapter chapter
                }
                val newBlock = NoteBlock(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    title = if (type.isHeading || type == ElementType.F) cleanTitle else "",
                    body = if (!type.isHeading) cleanBody else "",
                    isExpanded = true
                )
                if (anchorIdx >= 0) {
                    currentBlocks.add(anchorIdx + 1, newBlock)
                } else {
                    currentBlocks.add(newBlock)
                }
                OutlineEngine.expandParentsForBlock(currentBlocks, newBlock.id)
            }
            chapter.copy(blocks = finalBlocks)
        }
    }

    fun updateBlockContent(
        subjectId: String,
        chapterId: String,
        blockId: String,
        title: String,
        body: String
    ) {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        updateChapter(subjectId, chapterId) { chapter ->
            val block = chapter.blocks.firstOrNull { it.id == blockId } ?: return@updateChapter chapter
            val isEmpty = if (block.type.isHeading) cleanTitle.isEmpty() else cleanBody.isEmpty()
            if (isEmpty) {
                chapter.copy(blocks = chapter.blocks.filterNot { it.id == blockId })
            } else {
                chapter.copy(
                    blocks = chapter.blocks.map { item ->
                        if (item.id == blockId) {
                            item.copy(
                                title = if (item.type.isHeading) cleanTitle else item.title,
                                body = if (!item.type.isHeading) cleanBody else ""
                            )
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    fun toggleBlockExpanded(subjectId: String, chapterId: String, blockId: String) {
        updateChapter(subjectId, chapterId) { chapter ->
            chapter.copy(
                blocks = chapter.blocks.map { block ->
                    if (block.id == blockId && block.type.isHeading) {
                        block.copy(isExpanded = !block.isExpanded)
                    } else {
                        block
                    }
                }
            )
        }
    }

    fun setAllHeadingsExpanded(subjectId: String, chapterId: String, expanded: Boolean) {
        updateChapter(subjectId, chapterId) { chapter ->
            chapter.copy(
                blocks = chapter.blocks.map { block ->
                    if (block.type.isHeading) block.copy(isExpanded = expanded) else block
                }
            )
        }
    }

    fun expandAncestorsForBlock(subjectId: String, chapterId: String, blockId: String) {
        updateChapter(subjectId, chapterId) { chapter ->
            chapter.copy(blocks = OutlineEngine.expandParentsForBlock(chapter.blocks, blockId))
        }
    }

    fun toggleHeadingStatus(subjectId: String, chapterId: String, blockId: String) {
        updateChapter(subjectId, chapterId) { chapter ->
            chapter.copy(
                blocks = chapter.blocks.map { block ->
                    if (block.id == blockId && block.type.isHeading) {
                        val next = if (block.status == RevisionStatus.MASTERED) {
                            RevisionStatus.PENDING
                        } else {
                            RevisionStatus.MASTERED
                        }
                        block.copy(status = next)
                    } else {
                        block
                    }
                }
            )
        }
    }

    fun moveBlock(subjectId: String, chapterId: String, blockId: String, direction: Int) {
        updateChapter(subjectId, chapterId) { chapter ->
            val moved = OutlineEngine.moveBlock(chapter.blocks, blockId, direction)
            if (moved == chapter.blocks) return@updateChapter chapter
            chapter.copy(blocks = moved)
        }
    }

    fun deleteBlock(subjectId: String, chapterId: String, blockId: String) {
        updateChapter(subjectId, chapterId) { chapter ->
            val remaining = chapter.blocks.filterNot { it.id == blockId }
            chapter.copy(blocks = OutlineEngine.sanitizeHierarchy(remaining))
        }
    }

    private fun updateChapter(
        subjectId: String,
        chapterId: String,
        transform: (Chapter) -> Chapter
    ) {
        viewModelScope.launch {
            repository.mutateSubject(subjectId) { subject ->
                val updatedChapters = subject.chapters.map { chapter ->
                    if (chapter.id == chapterId) {
                        val next = transform(chapter)
                        if (next != chapter) next.copy(updatedAt = System.currentTimeMillis()) else chapter
                    } else {
                        chapter
                    }
                }
                subject.copy(chapters = updatedChapters)
            }
        }
    }
}
