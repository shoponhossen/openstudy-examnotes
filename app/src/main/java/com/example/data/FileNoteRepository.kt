package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.model.Subject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class FileNoteRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("examnotes_storage_prefs", Context.MODE_PRIVATE)
    private val cacheDir = File(context.filesDir, "examnotes_subjects").apply {
        if (!exists()) mkdirs()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val subjectAdapter = moshi.adapter(Subject::class.java).indent("  ")
    private val writeMutex = Mutex()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _linkedFolderName = MutableStateFlow<String?>(resolveLinkedFolderName())
    val linkedFolderName: StateFlow<String?> = _linkedFolderName.asStateFlow()

    private fun getSavedTreeUri(): Uri? {
        val raw = prefs.getString(KEY_TREE_URI, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()?.takeIf { uri ->
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission && it.isWritePermission
            }
        }
    }

    private fun resolveLinkedFolderName(): String? {
        val uri = getSavedTreeUri() ?: return null
        return runCatching {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val tail = docId.substringAfterLast(':').ifBlank { docId }
            tail
        }.getOrNull() ?: "Documents"
    }

    suspend fun loadAll() = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val treeUri = getSavedTreeUri()
            val merged = LinkedHashMap<String, Subject>()

            // 1. Load from local mirror first
            loadFromLocalMirror().forEach { merged[it.id] = it }

            // 2. Load from MediaStore Documents/ExamNotes (owned files on API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                loadFromMediaStoreDocuments().forEach { subject ->
                    val existing = merged[subject.id]
                    if (existing == null || subject.updatedAt > existing.updatedAt) {
                        merged[subject.id] = subject
                    }
                }
            }

            // 3. Load from user-linked Documents folder (survives uninstall & reinstall)
            if (treeUri != null) {
                loadFromSafTree(treeUri).forEach { subject ->
                    val existing = merged[subject.id]
                    if (existing == null || subject.updatedAt >= existing.updatedAt) {
                        merged[subject.id] = subject
                    }
                }
            }

            _subjects.value = merged.values.sortedByDescending { it.updatedAt }
            _linkedFolderName.value = resolveLinkedFolderName()
            _isLoaded.value = true
        }
    }

    suspend fun linkDocumentsFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply()

        var importedCount = 0
        writeMutex.withLock {
            val existingInFolder = loadFromSafTree(treeUri).associateBy { it.id }
            val merged = LinkedHashMap<String, Subject>()

            _subjects.value.forEach { merged[it.id] = it }
            existingInFolder.values.forEach { restored ->
                val current = merged[restored.id]
                if (current == null || restored.updatedAt >= current.updatedAt) {
                    merged[restored.id] = restored
                    importedCount++
                }
            }

            // Sync any subjects not yet written to the selected Documents folder
            merged.values.forEach { subject ->
                if (!existingInFolder.containsKey(subject.id)) {
                    writeToSafTree(treeUri, subject)
                }
                writeToLocalMirror(subject)
            }

            _subjects.value = merged.values.sortedByDescending { it.updatedAt }
            _linkedFolderName.value = resolveLinkedFolderName()
        }
        importedCount
    }

    suspend fun unlinkDocumentsFolder() = withContext(Dispatchers.IO) {
        getSavedTreeUri()?.let { uri ->
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, flags) }
        }
        prefs.edit().remove(KEY_TREE_URI).apply()
        _linkedFolderName.value = null
    }

    private val stateMutex = Any()

    suspend fun saveSubject(subject: Subject) {
        val updated = subject.copy(updatedAt = System.currentTimeMillis())
        synchronized(stateMutex) {
            applyInMemoryLocked(updated)
        }
        persistSubject(updated)
    }

    suspend fun mutateSubject(
        subjectId: String,
        transform: (Subject) -> Subject
    ) {
        val updated: Subject = synchronized(stateMutex) {
            val current = _subjects.value.firstOrNull { it.id == subjectId } ?: return
            val next = transform(current)
            if (next == current) return
            val stamped = next.copy(updatedAt = System.currentTimeMillis())
            applyInMemoryLocked(stamped)
            stamped
        }
        persistSubject(updated)
    }

    private fun applyInMemoryLocked(updated: Subject) {
        val current = _subjects.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == updated.id }
        if (existingIdx >= 0) {
            current[existingIdx] = updated
        } else {
            current.add(0, updated)
        }
        _subjects.value = current.sortedByDescending { it.updatedAt }
    }

    private suspend fun persistSubject(snapshot: Subject) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            // Always persist the latest in-memory version so older snapshots never overwrite newer edits.
            val latest = synchronized(stateMutex) {
                _subjects.value.firstOrNull { it.id == snapshot.id } ?: snapshot
            }
            writeToLocalMirror(latest)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeToMediaStoreDocuments(latest)
            }
            getSavedTreeUri()?.let { treeUri ->
                writeToSafTree(treeUri, latest)
            }
        }
    }

    suspend fun deleteSubject(subjectId: String) {
        synchronized(stateMutex) {
            _subjects.value = _subjects.value.filterNot { it.id == subjectId }
        }
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val localFile = localSubjectFile(subjectId)
                if (localFile.exists()) localFile.delete()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    deleteFromMediaStoreDocuments(subjectId)
                }
                getSavedTreeUri()?.let { treeUri ->
                    deleteFromSafTree(treeUri, subjectId)
                }
            }
        }
    }

    private fun subjectFileName(subjectId: String): String {
        val safeId = subjectId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "subject_$safeId.json"
    }

    private fun localSubjectFile(subjectId: String): File =
        File(cacheDir, subjectFileName(subjectId))

    private fun loadFromLocalMirror(): List<Subject> {
        return cacheDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json") && !file.name.endsWith(".tmp.json")
        }?.mapNotNull { file ->
            runCatching {
                subjectAdapter.fromJson(file.readText(Charsets.UTF_8))
            }.getOrNull()
        } ?: emptyList()
    }

    private fun writeToLocalMirror(subject: Subject) {
        val target = localSubjectFile(subject.id)
        val temp = File(cacheDir, "${target.name}.tmp.json")
        val json = subjectAdapter.toJson(subject)
        temp.writeText(json, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            target.writeText(json, Charsets.UTF_8)
            temp.delete()
        }
    }

    private fun loadFromMediaStoreDocuments(): List<Subject> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val results = mutableListOf<Subject>()
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/$DOCS_SUBFOLDER%", "subject_%.json")

        runCatching {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                            subjectAdapter.fromJson(reader.readText())?.let { results.add(it) }
                        }
                    }
                }
            }
        }
        return results
    }

    private fun writeToMediaStoreDocuments(subject: Subject) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val fileName = subjectFileName(subject.id)
        val json = subjectAdapter.toJson(subject)
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        runCatching {
            val existingUri = findMediaStoreUri(fileName)
            if (existingUri != null) {
                resolver.openOutputStream(existingUri, "wt")?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            } else {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$DOCS_SUBFOLDER")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val newUri = resolver.insert(collection, values)
                if (newUri != null) {
                    val wrote = runCatching {
                        resolver.openOutputStream(newUri, "wt")?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (wrote) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(newUri, values, null, null)
                    } else {
                        runCatching { resolver.delete(newUri, null, null) }
                    }
                }
            }
        }
    }

    private fun deleteFromMediaStoreDocuments(subjectId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val fileName = subjectFileName(subjectId)
        runCatching {
            findMediaStoreUri(fileName)?.let { uri ->
                context.contentResolver.delete(uri, null, null)
            }
        }
    }

    private fun findMediaStoreUri(fileName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val args = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/$DOCS_SUBFOLDER%", fileName)
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun loadFromSafTree(treeUri: Uri): List<Subject> {
        val results = mutableListOf<Subject>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol).orEmpty()
                    if (name.endsWith(".json", ignoreCase = true)) {
                        val docId = cursor.getString(idCol)
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        runCatching {
                            context.contentResolver.openInputStream(docUri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                                subjectAdapter.fromJson(reader.readText())?.let { results.add(it) }
                            }
                        }
                    }
                }
            }
        }
        return results
    }

    private fun writeToSafTree(treeUri: Uri, subject: Subject) {
        val fileName = subjectFileName(subject.id)
        val json = subjectAdapter.toJson(subject)
        val resolver = context.contentResolver

        runCatching {
            val existingUri = findSafDocumentUri(treeUri, fileName)
            val targetUri = existingUri ?: run {
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                DocumentsContract.createDocument(resolver, parentUri, "application/json", fileName)
            }
            if (targetUri != null) {
                val stream = runCatching { resolver.openOutputStream(targetUri, "rwt") }.getOrNull()
                    ?: runCatching { resolver.openOutputStream(targetUri, "wt") }.getOrNull()
                    ?: resolver.openOutputStream(targetUri, "w")
                stream?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    private fun deleteFromSafTree(treeUri: Uri, subjectId: String) {
        val fileName = subjectFileName(subjectId)
        runCatching {
            findSafDocumentUri(treeUri, fileName)?.let { docUri ->
                DocumentsContract.deleteDocument(context.contentResolver, docUri)
            }
        }
    }

    private fun findSafDocumentUri(treeUri: Uri, fileName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol).orEmpty()
                if (name == fileName) {
                    val docId = cursor.getString(idCol)
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                }
            }
        }
        return null
    }

    companion object {
        private const val KEY_TREE_URI = "documents_tree_uri"
        private const val DOCS_SUBFOLDER = "ExamNotes"
    }
}
