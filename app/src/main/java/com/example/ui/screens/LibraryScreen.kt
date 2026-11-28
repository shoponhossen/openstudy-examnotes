package com.example.ui.screens

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import shopon.openstudy.examnotes.R
import com.example.model.RevisionStatus
import com.example.model.Subject
import com.example.model.SubjectAccent
import com.example.ui.LibraryUiState
import com.example.ui.components.SubjectEditorDialog
import com.example.ui.components.accentColor
import com.example.ui.components.minimalTextFieldColors
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GuideLineIndigo
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceVoid
import com.example.ui.theme.TertiaryMint
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onCreateOrUpdateSubject: (id: String?, name: String, code: String, accent: SubjectAccent) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenSubject: (subjectId: String) -> Unit,
    onExportSubject: (Subject) -> Unit,
    onLinkDocumentsFolder: (Uri, (Int) -> Unit) -> Unit,
    onUnlinkDocumentsFolder: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var deletingSubject by remember { mutableStateOf<Subject?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onLinkDocumentsFolder(uri) { restoredCount ->
                scope.launch {
                    val msg = if (restoredCount > 0) {
                        "Linked Documents folder · restored $restoredCount subject(s)"
                    } else {
                        "Linked Documents folder · notes will survive uninstall"
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        containerColor = SurfaceBase,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                // Minimal Top Header matching Screen 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceContainerHigh,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_examnest_foreground_1795703782456),
                                contentDescription = "ExamNotes logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "ExamNotes",
                                style = MaterialTheme.typography.titleLarge,
                                color = OnSurfacePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Open Study",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { showStorageDialog = true }
                                .testTag("documents_folder_button"),
                            color = SurfaceContainerHigh,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = "Documents Folder Storage",
                                    tint = if (state.linkedFolderName != null) TertiaryMint else MutedSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { showCreateDialog = true }
                                .testTag("add_subject_button"),
                            color = SurfaceContainerHigh,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Create New Subject",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "Filter subjects or chapters…",
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MutedSlate
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = MutedSlate
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = minimalTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_subjects_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (state.subjects.isEmpty() && state.isLoaded) {
                    if (state.searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching subjects found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    } else {
                        MinimalEmptySubjectsState(
                            onCreateSubject = { showCreateDialog = true },
                            onRestoreFromDocuments = {
                                val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    DocumentsContract.buildRootUri(
                                        "com.android.externalstorage.documents",
                                        "primary"
                                    )
                                } else null
                                folderPickerLauncher.launch(initialUri)
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.subjects, key = { it.id }) { subject ->
                            MinimalSubjectCard(
                                subject = subject,
                                onClick = { onOpenSubject(subject.id) },
                                onEdit = { editingSubject = subject },
                                onExport = { onExportSubject(subject) },
                                onDelete = { deletingSubject = subject }
                            )
                        }

                        item(key = "create_new_subject_footer") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showCreateDialog = true }
                                    .testTag("create_new_subject_card"),
                                color = SurfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircleOutline,
                                        contentDescription = null,
                                        tint = SecondaryAmber,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Create New Subject",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = OnSurfacePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            containerColor = SurfaceContainerHigh,
            title = { Text("Documents Folder Storage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (state.linkedFolderName != null) {
                            "Linked folder: ${state.linkedFolderName}\nAll subject JSON files are read and saved directly here and survive app uninstall."
                        } else {
                            "Notes automatically save to Documents/ExamNotes on Android 10+. Link your Documents/ExamNotes folder below so reinstalling the app can read your files back immediately."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStorageDialog = false
                        val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            DocumentsContract.buildRootUri(
                                "com.android.externalstorage.documents",
                                "primary"
                            )
                        } else null
                        folderPickerLauncher.launch(initialUri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberFlare,
                        contentColor = SurfaceVoid
                    )
                ) {
                    Text(if (state.linkedFolderName == null) "Choose Documents Folder" else "Change Folder")
                }
            },
            dismissButton = {
                Row {
                    if (state.linkedFolderName != null) {
                        TextButton(
                            onClick = {
                                onUnlinkDocumentsFolder()
                                showStorageDialog = false
                            }
                        ) {
                            Text("Unlink", color = ErrorRed)
                        }
                    }
                    TextButton(onClick = { showStorageDialog = false }) {
                        Text("Close", color = MutedSlate)
                    }
                }
            }
        )
    }

    if (showCreateDialog) {
        SubjectEditorDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, code, accent ->
                onCreateOrUpdateSubject(null, name, code, accent)
                showCreateDialog = false
            }
        )
    }

    editingSubject?.let { subject ->
        SubjectEditorDialog(
            existingSubject = subject,
            onDismiss = { editingSubject = null },
            onConfirm = { name, code, accent ->
                onCreateOrUpdateSubject(subject.id, name, code, accent)
                editingSubject = null
            }
        )
    }

    deletingSubject?.let { subject ->
        AlertDialog(
            onDismissRequest = { deletingSubject = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete \"${subject.name}\"?") },
            text = {
                Text(
                    "Removes this subject and its ${subject.chapters.size} chapters from Documents storage.",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(subject.id)
                        deletingSubject = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = SurfaceVoid
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubject = null }) {
                    Text("Cancel", color = MutedSlate)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalSubjectCard(
    subject: Subject,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dotColor = subject.accent.accentColor()

    val totalHeadings = remember(subject) {
        subject.chapters.sumOf { ch -> ch.blocks.count { it.type.isHeading } }
    }
    val masteredHeadings = remember(subject) {
        subject.chapters.sumOf { ch ->
            ch.blocks.count { it.type.isHeading && it.status == RevisionStatus.MASTERED }
        }
    }
    val progress = if (totalHeadings == 0) 0.08f else (masteredHeadings.toFloat() / totalHeadings).coerceAtLeast(0.05f)

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .testTag("subject_card_${subject.id}"),
            color = SurfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfacePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${subject.name}",
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${subject.chapters.size} ${if (subject.chapters.size == 1) "Chapter" else "Chapters"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = dotColor,
                    trackColor = SurfaceContainerHigh
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit Subject") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Export Markdown") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onExport()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Subject", color = ErrorRed) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorRed) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun MinimalEmptySubjectsState(
    onCreateSubject: () -> Unit,
    onRestoreFromDocuments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = SurfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No subjects yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfacePrimary
                )
                Text(
                    text = "Notes save to Documents/ExamNotes so they stay safe on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
                Button(
                    onClick = onCreateSubject,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberFlare,
                        contentColor = SurfaceVoid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("empty_create_subject_button")
                ) {
                    Text("Create New Subject")
                }
                OutlinedButton(
                    onClick = onRestoreFromDocuments,
                    border = BorderStroke(1.dp, GuideLineIndigo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Link / Restore Documents Folder")
                }
            }
        }
    }
}
