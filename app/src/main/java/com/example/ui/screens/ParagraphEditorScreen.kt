package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import shopon.openstudy.examnotes.R
import com.example.domain.OutlineEngine
import com.example.model.Subject
import com.example.ui.components.minimalTextFieldColors
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceVoid

private val ParagraphSnippets = listOf(
    "Definition: ",
    "Materials: ",
    "Example: ",
    "Key Fact: ",
    "Note: "
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParagraphEditorScreen(
    subject: Subject?,
    chapterId: String,
    existingBlockId: String?,
    onBack: () -> Unit,
    onSave: (body: String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val chapterIdx = subject?.chapters?.indexOfFirst { it.id == chapterId } ?: -1
    val chapter = if (subject != null && chapterIdx >= 0) subject.chapters[chapterIdx] else null

    if (subject == null || chapter == null) {
        Scaffold(
            containerColor = SurfaceBase,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceBase,
                        titleContentColor = OnSurfacePrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = OnSurfacePrimary
                            )
                        }
                    },
                    title = { Text("(P)", maxLines = 1) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SurfaceBase),
                contentAlignment = Alignment.Center
            ) {
                Text("Chapter not found", color = MutedSlate)
            }
        }
        return
    }

    val existingIndex = existingBlockId?.let { id ->
        chapter.blocks.indexOfFirst { it.id == id }
    } ?: -1
    val existingBlock = if (existingIndex >= 0) chapter.blocks[existingIndex] else null
    val canMoveUp = existingBlock != null &&
        OutlineEngine.canMoveBlock(chapter.blocks, existingBlock.id, -1)
    val canMoveDown = existingBlock != null &&
        OutlineEngine.canMoveBlock(chapter.blocks, existingBlock.id, 1)

    val initialText = existingBlock?.body ?: ""
    var body by rememberSaveable(existingBlock?.id) { mutableStateOf(initialText) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = body.trim() != initialText.trim()
    val focusRequester = remember { FocusRequester() }

    val handleBack: () -> Unit = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(existingBlock?.id) {
        if (existingBlock == null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Scaffold(
        containerColor = SurfaceBase,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBase,
                    titleContentColor = OnSurfacePrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.testTag("paragraph_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = OnSurfacePrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "(P)",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfacePrimary,
                        maxLines = 1
                    )
                },
                actions = {
                    if (existingBlock != null) {
                        if (canMoveUp) {
                            IconButton(onClick = onMoveUp) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = "Move up",
                                    tint = MutedSlate
                                )
                            }
                        }
                        if (canMoveDown) {
                            IconButton(onClick = onMoveDown) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = "Move down",
                                    tint = MutedSlate
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                onDelete()
                                onBack()
                            },
                            modifier = Modifier.testTag("paragraph_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete paragraph",
                                tint = ErrorRed
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSave(body)
                            onBack()
                        },
                        enabled = body.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberFlare,
                            contentColor = SurfaceVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(38.dp)
                            .testTag("paragraph_save_button")
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParagraphSnippets.forEach { snippet ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    body = if (body.isBlank()) snippet else "$body\n$snippet"
                                },
                            color = SurfaceContainer,
                            border = BorderStroke(1.dp, AmberFlare.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+ ${snippet.trimEnd()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryAmber,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = {
                        Text(
                            text = "Write your note or definition…",
                            color = MutedSlate
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = false,
                    maxLines = Int.MAX_VALUE,
                    colors = minimalTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("paragraph_page_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tip: Labels like \"Definition:\" highlight automatically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        modifier = Modifier.weight(1f)
                    )
                    if (body.isNotEmpty()) {
                        Text(
                            text = "${body.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = SurfaceContainerHigh,
            title = { Text("Discard changes?") },
            text = {
                Text(
                    "Your paragraph edits have not been saved yet.",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = SurfaceVoid
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = MutedSlate)
                }
            }
        )
    }
}
