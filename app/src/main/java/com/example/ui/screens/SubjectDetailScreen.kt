package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import shopon.openstudy.examnotes.R
import com.example.domain.OutlineEngine
import com.example.model.Chapter
import com.example.model.ElementType
import com.example.model.Subject
import com.example.ui.components.ChapterEditorDialog
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GuideLineIndigo
import com.example.ui.theme.IndexMonoStyle
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.PrimaryContainerIndigo
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceVoid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: Subject?,
    onBack: () -> Unit,
    onAddChapter: (title: String) -> Unit,
    onRenameChapter: (chapterId: String, newTitle: String) -> Unit,
    onMoveChapter: (chapterId: String, direction: Int) -> Unit,
    onDeleteChapter: (chapterId: String) -> Unit,
    onOpenChapterIndex: (chapterId: String) -> Unit,
    onOpenChapterBlock: (chapterId: String, focusBlockId: String) -> Unit,
    onExportSubject: (Subject) -> Unit
) {
    if (subject == null) {
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
                    title = { Text("Subject Index", maxLines = 1) }
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
                Text("Subject not found", color = MutedSlate)
            }
        }
        return
    }

    var showAddChapterDialog by remember { mutableStateOf(false) }
    var renamingChapter by remember { mutableStateOf<Chapter?>(null) }
    var deletingChapter by remember { mutableStateOf<Chapter?>(null) }
    val expandedState = remember(subject.id) {
        mutableStateMapOf<String, Boolean>()
    }

    val subjectIndex = remember(subject) { OutlineEngine.computeSubjectIndex(subject) }

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
                        onClick = onBack,
                        modifier = Modifier.testTag("subject_back_button")
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
                        text = "Subject Index",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { onExportSubject(subject) }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Export Markdown",
                            tint = MutedSlate
                        )
                    }
                    IconButton(
                        onClick = { showAddChapterDialog = true },
                        modifier = Modifier.testTag("add_chapter_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add Chapter",
                            tint = PrimaryIndigo
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "subject_index_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryAmber
                        )
                        Text(
                            text = "Curriculum Structure",
                            style = MaterialTheme.typography.headlineLarge,
                            color = OnSurfacePrimary
                        )
                    }
                }

                if (subject.chapters.isEmpty()) {
                    item(key = "empty_chapters") {
                        Surface(
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No chapters yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurfacePrimary
                                )
                                Text(
                                    text = "Add Chapter 1 to start structuring your H1 revision topics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                                Button(
                                    onClick = { showAddChapterDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberFlare,
                                        contentColor = SurfaceVoid
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Chapter")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = subjectIndex,
                        key = { _, item -> item.chapterId }
                    ) { idx, indexChapter ->
                        val chapter = subject.chapters.getOrNull(idx) ?: return@itemsIndexed
                        val isExpanded = expandedState[chapter.id] ?: (idx == subject.chapters.lastIndex)

                        SubjectIndexChapterCard(
                            chapterIndex = indexChapter.chapterIndex,
                            chapter = chapter,
                            h1Entries = indexChapter.h1Entries.map { entry ->
                                val summary = extractFirstParagraphSummary(chapter, entry.blockId)
                                entry to summary
                            },
                            isExpanded = isExpanded,
                            canMoveUp = idx > 0,
                            canMoveDown = idx < subject.chapters.lastIndex,
                            onToggleExpand = {
                                expandedState[chapter.id] = !isExpanded
                            },
                            onOpenChapterIndex = { onOpenChapterIndex(chapter.id) },
                            onOpenH1 = { blockId -> onOpenChapterBlock(chapter.id, blockId) },
                            onRename = { renamingChapter = chapter },
                            onMoveUp = { onMoveChapter(chapter.id, -1) },
                            onMoveDown = { onMoveChapter(chapter.id, 1) },
                            onDelete = { deletingChapter = chapter }
                        )
                    }

                    item(key = "add_chapter_footer") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showAddChapterDialog = true }
                                .testTag("add_chapter_footer_button"),
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = null,
                                    tint = SecondaryAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Chapter",
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

    if (showAddChapterDialog) {
        ChapterEditorDialog(
            chapterNumber = subject.chapters.size + 1,
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { title ->
                onAddChapter(title)
                showAddChapterDialog = false
            }
        )
    }

    renamingChapter?.let { chapter ->
        ChapterEditorDialog(
            initialTitle = chapter.title,
            isRename = true,
            onDismiss = { renamingChapter = null },
            onConfirm = { newTitle ->
                onRenameChapter(chapter.id, newTitle)
                renamingChapter = null
            }
        )
    }

    deletingChapter?.let { chapter ->
        AlertDialog(
            onDismissRequest = { deletingChapter = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete \"${chapter.title}\"?") },
            text = {
                Text(
                    "All headings and notes inside this chapter will be deleted.",
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChapter(chapter.id)
                        deletingChapter = null
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
                TextButton(onClick = { deletingChapter = null }) {
                    Text("Cancel", color = MutedSlate)
                }
            }
        )
    }
}

private fun extractFirstParagraphSummary(chapter: Chapter, h1BlockId: String): String {
    val idx = chapter.blocks.indexOfFirst { it.id == h1BlockId }
    if (idx < 0) return ""
    for (j in (idx + 1) until chapter.blocks.size) {
        val next = chapter.blocks[j]
        if (next.type == ElementType.H1) break
        if (next.type == ElementType.P && next.body.isNotBlank()) {
            return next.body.lineSequence().firstOrNull()?.take(180)
                ?.removePrefix("Definition:")?.trim().orEmpty()
        }
    }
    return ""
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectIndexChapterCard(
    chapterIndex: Int,
    chapter: Chapter,
    h1Entries: List<Pair<com.example.domain.SubjectIndexH1Entry, String>>,
    isExpanded: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleExpand: () -> Unit,
    onOpenChapterIndex: () -> Unit,
    onOpenH1: (blockId: String) -> Unit,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val chevronRot by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chapter_chevron"
    )
    val badgeNum = chapterIndex.toString().padStart(2, '0')
    val guideLineColor = AmberFlare.copy(alpha = 0.4f)

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onToggleExpand,
                    onLongClick = { menuExpanded = true }
                )
                .testTag("subject_chapter_card_$chapterIndex"),
            color = PrimaryContainerIndigo.copy(alpha = 0.72f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = AmberFlare,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badgeNum,
                                style = IndexMonoStyle,
                                color = SurfaceVoid,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurfacePrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleExpand),
                            color = SurfaceContainerHigh.copy(alpha = 0.75f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Toggle chapter preview",
                                    tint = SecondaryAmber,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevronRot)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            if (h1Entries.isEmpty()) {
                                Text(
                                    text = "No H1 headings added yet — open chapter to start writing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            val x = 10.dp.toPx()
                                            drawLine(
                                                color = guideLineColor,
                                                start = Offset(x, 6.dp.toPx()),
                                                end = Offset(x, size.height - 6.dp.toPx()),
                                                strokeWidth = 1.5.dp.toPx()
                                            )
                                        }
                                        .padding(start = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    h1Entries.forEach { (entry, summary) ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onOpenH1(entry.blockId) }
                                                .testTag("subject_index_h1_${entry.numberLabel}"),
                                            color = SurfaceBase.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = entry.numberLabel,
                                                    style = IndexMonoStyle,
                                                    color = SecondaryAmber,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = entry.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = OnSurfacePrimary
                                                    )
                                                    if (summary.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = summary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MutedSlate,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onOpenChapterIndex,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberFlare,
                                    contentColor = SurfaceVoid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_chapter_index_btn_$chapterIndex")
                            ) {
                                Text(
                                    text = "Open Chapter Index",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename Chapter") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onRename()
                }
            )
            if (canMoveUp) {
                DropdownMenuItem(
                    text = { Text("Move Up") },
                    leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMoveUp()
                    }
                )
            }
            if (canMoveDown) {
                DropdownMenuItem(
                    text = { Text("Move Down") },
                    leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMoveDown()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete Chapter", color = ErrorRed) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorRed) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}
