package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.ElementType
import com.example.model.NoteBlock
import com.example.model.RevisionStatus
import com.example.model.Subject
import com.example.model.SubjectAccent
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.EmeraldCore
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FormulaCodeStyle
import com.example.ui.theme.FormulaSurface
import com.example.ui.theme.GuideLineIndigo
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceVoid
import com.example.ui.theme.TertiaryMint

fun SubjectAccent.accentColor(): Color = when (this) {
    SubjectAccent.INDIGO -> PrimaryIndigo
    SubjectAccent.AMBER -> AmberFlare
    SubjectAccent.EMERALD -> EmeraldCore
}

@Composable
fun FormattedParagraphText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            val iterator = text.lineSequence().iterator()
            var isFirst = true
            while (iterator.hasNext()) {
                val line = iterator.next()
                if (!isFirst) {
                    append("\n")
                }
                isFirst = false
                val colonIndex = line.indexOf(':')
                if (colonIndex in 1..28 && !line.startsWith("http")) {
                    val keyPart = line.substring(0, colonIndex + 1)
                    val valuePart = line.substring(colonIndex + 1)
                    withStyle(
                        SpanStyle(
                            color = SecondaryAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(keyPart)
                    }
                    withStyle(SpanStyle(color = OnSurfacePrimary)) {
                        append(valuePart)
                    }
                } else {
                    withStyle(SpanStyle(color = OnSurfaceVariant)) {
                        append(line)
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

private val FormulaQuickSymbols = listOf("√", "²", "³", "·", "×", "÷", "±", "≈", "Δ", "π", "μ", "ε₀", "Ω", "→")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaEditorBottomSheet(
    existingBlock: NoteBlock? = null,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existingBlock) { mutableStateOf(existingBlock?.title ?: "") }
    var body by remember(existingBlock) { mutableStateOf(existingBlock?.body ?: "") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerHigh,
        contentColor = OnSurfacePrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (existingBlock == null) "Add Formula (F)" else "Edit Formula (F)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfacePrimary
                    )
                    Text(
                        text = "Monospace formula block",
                        style = MaterialTheme.typography.labelSmall,
                        color = TertiaryMint
                    )
                }

                if (existingBlock != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canMoveUp && onMoveUp != null) {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = "Move up",
                                    tint = MutedSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (canMoveDown && onMoveDown != null) {
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = "Move down",
                                    tint = MutedSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Label (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Enter formula label", color = MutedSlate) },
                    singleLine = true,
                    colors = minimalTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("formula_sheet_title_input")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Formula Expression *",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Enter formula expression", color = MutedSlate) },
                    textStyle = FormulaCodeStyle.copy(color = TertiaryMint),
                    minLines = 2,
                    maxLines = 5,
                    colors = minimalTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("formula_sheet_body_input")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormulaQuickSymbols.forEach { sym ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { body += sym },
                            color = FormulaSurface,
                            border = BorderStroke(1.dp, TertiaryMint.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = sym,
                                style = FormulaCodeStyle,
                                color = TertiaryMint,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existingBlock != null && onDelete != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .testTag("formula_sheet_delete_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            color = ErrorRed
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedSlate)
                    }
                    Button(
                        onClick = { onSave(title, body) },
                        enabled = body.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCore,
                            contentColor = SurfaceVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("formula_sheet_save_button")
                    ) {
                        Text(if (existingBlock == null) "Add Formula" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun InlineHeadingEditorCard(
    block: NoteBlock,
    numberLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleMastered: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onDone: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(block.id) { mutableStateOf(block.title) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(block.id) {
        runCatching { focusRequester.requestFocus() }
    }

    val headerLabel = when (block.type) {
        ElementType.H1 -> "Editing H1 ($numberLabel)"
        ElementType.H2 -> "Editing H2 ($numberLabel)"
        ElementType.H3 -> "Editing H3 ($numberLabel)"
        else -> "Editing Heading"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryIndigo
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isMastered = block.status == RevisionStatus.MASTERED
                    IconButton(
                        onClick = onToggleMastered,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isMastered) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Toggle mastered",
                            tint = if (isMastered) TertiaryMint else MutedSlate,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    if (canMoveUp) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Move up",
                                tint = MutedSlate,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (canMoveDown) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Move down",
                                tint = MutedSlate,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Heading title…") },
                singleLine = true,
                colors = minimalTextFieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("inline_heading_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("inline_delete_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancel),
                        color = SurfaceContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDone(title) }
                            .testTag("inline_done_button"),
                        color = PrimaryIndigo,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium,
                            color = SurfaceVoid,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectEditorDialog(
    existingSubject: Subject? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, accent: SubjectAccent) -> Unit
) {
    var name by remember(existingSubject) { mutableStateOf(existingSubject?.name ?: "") }
    var code by remember(existingSubject) { mutableStateOf(existingSubject?.code ?: "") }
    var accent by remember(existingSubject) {
        mutableStateOf(existingSubject?.accent ?: SubjectAccent.AMBER)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceContainerHigh,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (existingSubject == null) "New Subject" else "Edit Subject",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfacePrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Subject Name *",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Enter subject name", color = MutedSlate) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = minimalTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_name_input")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Course Code (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlate
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("Enter course code", color = MutedSlate) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = minimalTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_code_input")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlate
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectAccent.entries.forEach { option ->
                            val selected = accent == option
                            val chipColor = option.accentColor()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { accent = option },
                                color = if (selected) chipColor.copy(alpha = 0.18f) else SurfaceContainerLow,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) chipColor else Color.White.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) OnSurfacePrimary else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(name, code, accent) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberFlare,
                            contentColor = SurfaceVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("confirm_subject_button")
                    ) {
                        Text(if (existingSubject == null) "Create" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterEditorDialog(
    initialTitle: String = "",
    chapterNumber: Int? = null,
    isRename: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }

    val heading = when {
        isRename -> "Rename Chapter"
        chapterNumber != null -> "New Chapter $chapterNumber"
        else -> "New Chapter"
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceContainerHigh,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfacePrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Chapter Title *",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Enter chapter title", color = MutedSlate) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = minimalTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chapter_title_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(title) },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberFlare,
                            contentColor = SurfaceVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("confirm_chapter_button")
                    ) {
                        Text(if (isRename) "Save" else "Add")
                    }
                }
            }
        }
    }
}

@Composable
fun minimalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryIndigo.copy(alpha = 0.7f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
    focusedTextColor = OnSurfacePrimary,
    unfocusedTextColor = OnSurfacePrimary,
    focusedContainerColor = SurfaceBase,
    unfocusedContainerColor = SurfaceContainerLow,
    focusedLabelColor = PrimaryIndigo,
    unfocusedLabelColor = MutedSlate,
    cursorColor = AmberFlare
)
