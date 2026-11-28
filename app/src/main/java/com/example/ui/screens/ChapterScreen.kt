package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.domain.NumberedBlock
import com.example.domain.OutlineEngine
import com.example.model.Chapter
import com.example.model.ElementType
import com.example.model.NoteBlock
import com.example.model.Subject
import com.example.ui.components.FormattedParagraphText
import com.example.ui.components.FormulaEditorBottomSheet
import com.example.ui.components.InlineHeadingEditorCard
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.FormulaCodeStyle
import com.example.ui.theme.FormulaSurface
import com.example.ui.theme.GuideLineIndigo
import com.example.ui.theme.IndexMonoStyle
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.TertiaryMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FormulaSheetRequest(
    val existingBlock: NoteBlock? = null,
    val insertAfterBlockId: String? = null
)

@Composable
fun ChapterScreen(
    subject: Subject?,
    chapterId: String,
    initialFocusBlockId: String?,
    onBackToChapterIndex: () -> Unit,
    onOpenParagraphEditor: (existingBlockId: String?, insertAfterBlockId: String?) -> Unit,
    onSaveFormula: (existingBlockId: String?, insertAfterBlockId: String?, title: String, body: String) -> Unit,
    onInsertHeadingAndGetId: (insertAfterBlockId: String?, type: ElementType) -> String,
    onUpdateHeadingTitle: (blockId: String, title: String) -> Unit,
    onToggleExpand: (blockId: String) -> Unit,
    onSetAllExpanded: (expanded: Boolean) -> Unit,
    onExpandAncestors: (blockId: String) -> Unit,
    onToggleStatus: (blockId: String) -> Unit,
    onMoveBlock: (blockId: String, direction: Int) -> Unit,
    onDeleteBlock: (blockId: String) -> Unit,
    onExportChapter: (chapterIndex: Int, chapter: Chapter) -> Unit
) {
    val chapterIdx = subject?.chapters?.indexOfFirst { it.id == chapterId } ?: -1
    val chapter = if (subject != null && chapterIdx >= 0) subject.chapters[chapterIdx] else null

    if (subject == null || chapter == null) {
        Scaffold(containerColor = SurfaceBase) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(18.dp)
            ) {
                IconButton(onClick = onBackToChapterIndex) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SecondaryAmber
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chapter not found", color = MutedSlate)
                }
            }
        }
        return
    }

    val chapterNumber = chapterIdx + 1
    var activeAddMenuBlockId by remember { mutableStateOf<String?>(null) }
    var editingHeadingId by remember { mutableStateOf<String?>(null) }
    var formulaSheetRequest by remember { mutableStateOf<FormulaSheetRequest?>(null) }
    var highlightedBlockId by remember { mutableStateOf<String?>(null) }

    val outlinedBlocks = remember(chapterNumber, chapter.blocks) {
        OutlineEngine.computeChapterOutline(chapterNumber, chapter.blocks)
    }
    val visibleBlocks = remember(outlinedBlocks) {
        outlinedBlocks.filter { it.isVisible }
    }
    val allCollapsed = remember(chapter.blocks) {
        val headings = chapter.blocks.filter { it.type.isHeading }
        headings.isNotEmpty() && headings.none { it.isExpanded }
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialFocusBlockId, chapterId) {
        if (!initialFocusBlockId.isNullOrBlank()) {
            onExpandAncestors(initialFocusBlockId)
            highlightedBlockId = initialFocusBlockId
            delay(150)
            val updatedOutline = OutlineEngine.computeChapterOutline(
                chapterNumber,
                OutlineEngine.expandParentsForBlock(chapter.blocks, initialFocusBlockId)
            ).filter { it.isVisible }
            val visibleIdx = updatedOutline.indexOfFirst { it.block.id == initialFocusBlockId }
            if (visibleIdx >= 0) {
                listState.animateScrollToItem(visibleIdx + 1)
            }
            delay(1500)
            highlightedBlockId = null
        }
    }

    Scaffold(
        containerColor = SurfaceBase,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Top Bar: "← Ch. X Index" + "Collapse" pill + Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBackToChapterIndex)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("chapter_back_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Ch. $chapterNumber Index",
                            tint = SecondaryAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ch. $chapterNumber Index",
                            style = MaterialTheme.typography.titleSmall,
                            color = SecondaryAmber
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetAllExpanded(allCollapsed) }
                                .testTag("toggle_all_expand_button"),
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (allCollapsed) Icons.Outlined.UnfoldMore else Icons.Outlined.UnfoldLess,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (allCollapsed) "Expand" else "Collapse",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfacePrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onExportChapter(chapterNumber, chapter) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Export Markdown",
                                tint = MutedSlate,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "chapter_title_header") {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = OnSurfacePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + on any heading or block to add elements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }

                    itemsIndexed(
                        items = visibleBlocks,
                        key = { _, item -> item.block.id }
                    ) { _, item ->
                        val block = item.block
                        val fullIdx = chapter.blocks.indexOfFirst { it.id == block.id }
                        val isEditingHeading = block.type.isHeading && editingHeadingId == block.id
                        val isAddMenuOpen = activeAddMenuBlockId == block.id
                        val isHighlighted = highlightedBlockId == block.id

                        HierarchicalBlockNode(
                            item = item,
                            isEditingHeading = isEditingHeading,
                            isAddMenuOpen = isAddMenuOpen,
                            isHighlighted = isHighlighted,
                            canMoveUp = OutlineEngine.canMoveBlock(chapter.blocks, block.id, -1),
                            canMoveDown = OutlineEngine.canMoveBlock(chapter.blocks, block.id, 1),
                            onToggleExpand = { onToggleExpand(block.id) },
                            onToggleAddMenu = {
                                activeAddMenuBlockId = if (isAddMenuOpen) null else block.id
                            },
                            onInsertElement = { elementType ->
                                activeAddMenuBlockId = null
                                when (elementType) {
                                    ElementType.P -> {
                                        onOpenParagraphEditor(null, block.id)
                                    }
                                    ElementType.F -> {
                                        formulaSheetRequest = FormulaSheetRequest(
                                            existingBlock = null,
                                            insertAfterBlockId = block.id
                                        )
                                    }
                                    ElementType.H1, ElementType.H2, ElementType.H3 -> {
                                        val newId = onInsertHeadingAndGetId(block.id, elementType)
                                        editingHeadingId = newId
                                    }
                                }
                            },
                            onStartEdit = {
                                activeAddMenuBlockId = null
                                when (block.type) {
                                    ElementType.P -> onOpenParagraphEditor(block.id, null)
                                    ElementType.F -> formulaSheetRequest = FormulaSheetRequest(existingBlock = block)
                                    ElementType.H1, ElementType.H2, ElementType.H3 -> editingHeadingId = block.id
                                }
                            },
                            onCancelHeadingEdit = {
                                if (block.title.isBlank()) {
                                    onDeleteBlock(block.id)
                                }
                                editingHeadingId = null
                            },
                            onSaveHeadingEdit = { newTitle ->
                                onUpdateHeadingTitle(block.id, newTitle)
                                editingHeadingId = null
                            },
                            onToggleStatus = { onToggleStatus(block.id) },
                            onMoveUp = { onMoveBlock(block.id, -1) },
                            onMoveDown = { onMoveBlock(block.id, 1) },
                            onDeleteHeading = {
                                editingHeadingId = null
                                onDeleteBlock(block.id)
                            },
                            onCopyFormula = { formulaText ->
                                clipboard.setText(AnnotatedString(formulaText))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Formula copied")
                                }
                            }
                        )
                    }

                    item(key = "add_new_h1_footer") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    activeAddMenuBlockId = null
                                    val newId = onInsertHeadingAndGetId(null, ElementType.H1)
                                    editingHeadingId = newId
                                }
                                .testTag("add_new_h1_button"),
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp),
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
                                    text = "New H1 Heading",
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

    formulaSheetRequest?.let { req ->
        FormulaEditorBottomSheet(
            existingBlock = req.existingBlock,
            canMoveUp = req.existingBlock?.let {
                OutlineEngine.canMoveBlock(chapter.blocks, it.id, -1)
            } ?: false,
            canMoveDown = req.existingBlock?.let {
                OutlineEngine.canMoveBlock(chapter.blocks, it.id, 1)
            } ?: false,
            onMoveUp = {
                req.existingBlock?.let { onMoveBlock(it.id, -1) }
                formulaSheetRequest = null
            },
            onMoveDown = {
                req.existingBlock?.let { onMoveBlock(it.id, 1) }
                formulaSheetRequest = null
            },
            onDelete = {
                req.existingBlock?.let { onDeleteBlock(it.id) }
                formulaSheetRequest = null
            },
            onDismiss = { formulaSheetRequest = null },
            onSave = { title, body ->
                onSaveFormula(req.existingBlock?.id, req.insertAfterBlockId, title, body)
                formulaSheetRequest = null
            }
        )
    }
}

@Composable
private fun HierarchicalBlockNode(
    item: NumberedBlock,
    isEditingHeading: Boolean,
    isAddMenuOpen: Boolean,
    isHighlighted: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleExpand: () -> Unit,
    onToggleAddMenu: () -> Unit,
    onInsertElement: (ElementType) -> Unit,
    onStartEdit: () -> Unit,
    onCancelHeadingEdit: () -> Unit,
    onSaveHeadingEdit: (title: String) -> Unit,
    onToggleStatus: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeleteHeading: () -> Unit,
    onCopyFormula: (String) -> Unit
) {
    val block = item.block
    val indentDp = (item.indentDepth * 14).dp
    val guideColor = GuideLineIndigo.copy(alpha = 0.55f)

    val addOptions = remember(block.type) { OutlineEngine.allowedChildren(block.type) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stepPx = 14.dp.toPx()
                val strokePx = 1.dp.toPx()
                for (level in 0 until item.indentDepth) {
                    val x = level * stepPx + 4.dp.toPx()
                    drawLine(
                        color = guideColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokePx
                    )
                }
            }
            .padding(start = indentDp)
    ) {
        if (isEditingHeading) {
            InlineHeadingEditorCard(
                block = block,
                numberLabel = item.numberLabel,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onToggleMastered = onToggleStatus,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDeleteHeading,
                onCancel = onCancelHeadingEdit,
                onDone = onSaveHeadingEdit,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            val highlightBorder by animateColorAsState(
                targetValue = if (isHighlighted) AmberFlare.copy(alpha = 0.55f) else Color.Transparent,
                label = "block_highlight"
            )

            if (block.type.isHeading) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (block.isExpanded) 0f else -90f,
                    label = "heading_chevron"
                )
                val numberColor = when (block.type) {
                    ElementType.H1 -> SecondaryAmber
                    ElementType.H2 -> SecondaryAmber.copy(alpha = 0.9f)
                    else -> MutedSlate
                }
                val titleStyle = when (block.type) {
                    ElementType.H1 -> MaterialTheme.typography.titleLarge
                    ElementType.H2 -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodyMedium
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isHighlighted) {
                                Modifier.background(highlightBorder.copy(alpha = 0.12f))
                            } else Modifier
                        )
                        .clickable(onClick = onStartEdit)
                        .padding(vertical = if (block.type == ElementType.H1) 8.dp else 5.dp, horizontal = 4.dp)
                        .testTag("heading_row_${item.numberLabel}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.numberLabel,
                        style = IndexMonoStyle,
                        color = numberColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = block.title,
                        style = titleStyle,
                        color = OnSurfacePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!block.isExpanded && item.childCount > 0) {
                        Text(
                            text = "${item.childCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleAddMenu,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("add_below_btn_${item.numberLabel}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add element under ${item.numberLabel}",
                            tint = if (isAddMenuOpen) SecondaryAmber else MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("expand_btn_${item.numberLabel}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Expand or collapse ${block.title}",
                            tint = OnSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            } else if (block.type == ElementType.P) {
                // Paragraph (P) — NO expand/collapse button; tap opens separate Paragraph Editor page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onStartEdit)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag("paragraph_row_${block.id}"),
                    verticalAlignment = Alignment.Top
                ) {
                    FormattedParagraphText(
                        text = block.body,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleAddMenu,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add element below paragraph",
                            tint = if (isAddMenuOpen) SecondaryAmber else MutedSlate.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Formula (F) — tap opens Formula Bottom Sheet (leaf block, no children)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onStartEdit)
                        .testTag("formula_row_${block.id}"),
                    color = FormulaSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (block.title.isNotBlank()) {
                            Text(
                                text = block.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = block.body,
                                style = FormulaCodeStyle,
                                color = TertiaryMint,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onCopyFormula(block.body) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isAddMenuOpen && addOptions.isNotEmpty()) {
                InlineAddElementStrip(
                    options = addOptions,
                    onSelect = onInsertElement,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InlineAddElementStrip(
    options: List<ElementType>,
    onSelect: (ElementType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ADD:",
                style = IndexMonoStyle,
                color = MutedSlate,
                maxLines = 1,
                softWrap = false
            )
            options.forEach { element ->
                val accent = when (element) {
                    ElementType.H1, ElementType.H2, ElementType.H3 -> SecondaryAmber
                    ElementType.P -> OnSurfacePrimary
                    ElementType.F -> TertiaryMint
                }
                val label = when (element) {
                    ElementType.H1 -> "H1"
                    ElementType.H2 -> "Subheading"
                    ElementType.H3 -> "Subtopic"
                    ElementType.P -> "Paragraph"
                    ElementType.F -> "Formula"
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(element) }
                        .testTag("inline_add_${element.shortLabel}"),
                    color = SurfaceContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = element.shortLabel,
                            style = IndexMonoStyle,
                            color = accent,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (element != ElementType.H1) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
