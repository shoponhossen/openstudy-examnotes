package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import shopon.openstudy.examnotes.R
import com.example.domain.ChapterIndexEntry
import com.example.domain.OutlineEngine
import com.example.model.Subject
import com.example.ui.components.minimalTextFieldColors
import com.example.ui.theme.AmberFlare
import com.example.ui.theme.GuideLineIndigo
import com.example.ui.theme.IndexMonoStyle
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OnSurfacePrimary
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceVoid

private data class ChapterIndexGroup(
    val h1: ChapterIndexEntry,
    val h2Children: List<ChapterIndexEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterIndexScreen(
    subject: Subject?,
    chapterId: String,
    onBack: () -> Unit,
    onOpenNotes: (focusBlockId: String?) -> Unit
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
                    title = { Text("Chapter Index", maxLines = 1) }
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

    val chapterNumber = chapterIdx + 1
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val groups = remember(chapterNumber, chapter, searchQuery) {
        val flat = OutlineEngine.computeChapterIndex(chapterNumber, chapter)
        val built = mutableListOf<ChapterIndexGroup>()
        var currentH1: ChapterIndexEntry? = null
        var currentH2s = mutableListOf<ChapterIndexEntry>()

        for (entry in flat) {
            if (entry.level == 1) {
                if (currentH1 != null) {
                    built.add(ChapterIndexGroup(currentH1, currentH2s))
                }
                currentH1 = entry
                currentH2s = mutableListOf()
            } else if (entry.level == 2) {
                currentH2s.add(entry)
            }
        }
        if (currentH1 != null) {
            built.add(ChapterIndexGroup(currentH1, currentH2s))
        }

        val q = searchQuery.trim()
        if (q.isEmpty()) {
            built
        } else {
            built.mapNotNull { group ->
                val h1Matches = group.h1.title.contains(q, ignoreCase = true) ||
                    group.h1.numberLabel.contains(q, ignoreCase = true)
                val matchingH2s = group.h2Children.filter {
                    it.title.contains(q, ignoreCase = true) || it.numberLabel.contains(q, ignoreCase = true)
                }
                when {
                    h1Matches -> group
                    matchingH2s.isNotEmpty() -> group.copy(h2Children = matchingH2s)
                    else -> null
                }
            }
        }
    }

    val stats = remember(chapter) { OutlineEngine.computeChapterStats(chapter) }

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
                        modifier = Modifier.testTag("chapter_index_back_button")
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
                        text = "Chapter Index",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onOpenNotes(null) },
                        modifier = Modifier.testTag("open_chapter_notes_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Open Chapter Notes",
                            tint = SecondaryAmber
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
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "chapter_meta_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${subject.name.uppercase()} · CHAPTER $chapterNumber",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    showSearch = !showSearch
                                    if (!showSearch) searchQuery = ""
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                                    contentDescription = "Search headings",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        if (showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filter H1 or H2 headings…", color = MutedSlate) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = minimalTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item(key = "chapter_hero_card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenNotes(null) },
                        color = SurfaceContainerLow,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = OnSurfacePrimary
                            )
                            Text(
                                text = "${stats.h1Count} sections · ${stats.h2Count} subtopics · ${stats.noteCount + stats.formulaCount} notes & formulas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }
                }

                if (groups.isEmpty()) {
                    item(key = "empty_chapter_index") {
                        Surface(
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching headings" else "No headings yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurfacePrimary
                                )
                                Text(
                                    text = "Open the chapter notes page to start adding H1 and H2 headings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                                Button(
                                    onClick = { onOpenNotes(null) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberFlare,
                                        contentColor = SurfaceVoid
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("empty_open_notes_button")
                                ) {
                                    Text("Open Chapter Notes")
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = groups,
                        key = { it.h1.blockId }
                    ) { group ->
                        ChapterIndexH1Card(
                            group = group,
                            onOpenH1 = { onOpenNotes(group.h1.blockId) },
                            onOpenH2 = { h2Id -> onOpenNotes(h2Id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterIndexH1Card(
    group: ChapterIndexGroup,
    onOpenH1: () -> Unit,
    onOpenH2: (String) -> Unit
) {
    val lineColor = GuideLineIndigo.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenH1)
                    .padding(vertical = 2.dp)
                    .testTag("chapter_index_h1_${group.h1.numberLabel}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = SurfaceContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Text(
                        text = group.h1.numberLabel,
                        style = IndexMonoStyle,
                        color = SecondaryAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = group.h1.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfacePrimary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${group.h2Children.size} subtopic${if (group.h2Children.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${group.h1.title}",
                        tint = MutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (group.h2Children.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                        .drawBehind {
                            val trunkX = 4.dp.toPx()
                            drawLine(
                                color = lineColor,
                                start = Offset(trunkX, 0f),
                                end = Offset(trunkX, size.height - 18.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    group.h2Children.forEach { h2 ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val trunkX = 4.dp.toPx()
                                    val midY = size.height / 2f
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(trunkX, midY),
                                        end = Offset(18.dp.toPx(), midY),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .padding(start = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenH2(h2.blockId) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("chapter_index_h2_${h2.numberLabel}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = h2.numberLabel,
                                style = IndexMonoStyle,
                                color = MutedSlate
                            )
                            Text(
                                text = h2.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfacePrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MutedSlate,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
