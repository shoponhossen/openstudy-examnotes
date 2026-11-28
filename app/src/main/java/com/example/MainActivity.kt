package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.OutlineEngine
import com.example.model.ElementType
import com.example.ui.ExamNestViewModel
import com.example.ui.screens.ChapterIndexScreen
import com.example.ui.screens.ChapterScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ParagraphEditorScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.theme.ExamNestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExamNestTheme {
                ExamNestApp()
            }
        }
    }
}

private object ExamNestRoutes {
    const val LIBRARY = "library"
    const val SUBJECT_INDEX = "subject/{subjectId}"
    const val CHAPTER_INDEX = "chapter_index/{subjectId}/{chapterId}"
    const val CHAPTER_CONTENT = "chapter/{subjectId}/{chapterId}?focusBlockId={focusBlockId}"
    const val PARAGRAPH_CREATE = "paragraph_new/{subjectId}/{chapterId}/{insertAfterBlockId}"
    const val PARAGRAPH_EDIT = "paragraph_edit/{subjectId}/{chapterId}/{blockId}"

    fun subjectIndex(subjectId: String): String = "subject/${Uri.encode(subjectId)}"

    fun chapterIndex(subjectId: String, chapterId: String): String =
        "chapter_index/${Uri.encode(subjectId)}/${Uri.encode(chapterId)}"

    fun chapterContent(subjectId: String, chapterId: String, focusBlockId: String? = null): String {
        val base = "chapter/${Uri.encode(subjectId)}/${Uri.encode(chapterId)}"
        return if (!focusBlockId.isNullOrBlank()) {
            "$base?focusBlockId=${Uri.encode(focusBlockId)}"
        } else {
            base
        }
    }

    fun paragraphCreate(
        subjectId: String,
        chapterId: String,
        insertAfterBlockId: String
    ): String = "paragraph_new/${Uri.encode(subjectId)}/${Uri.encode(chapterId)}/${Uri.encode(insertAfterBlockId)}"

    fun paragraphEdit(
        subjectId: String,
        chapterId: String,
        blockId: String
    ): String = "paragraph_edit/${Uri.encode(subjectId)}/${Uri.encode(chapterId)}/${Uri.encode(blockId)}"
}

@Composable
fun ExamNestApp(
    viewModel: ExamNestViewModel = viewModel()
) {
    val navController = rememberNavController()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun shareText(title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Export Revision Notes"))
        }
    }

    NavHost(
        navController = navController,
        startDestination = ExamNestRoutes.LIBRARY
    ) {
        // Screen 1: Subjects List
        composable(ExamNestRoutes.LIBRARY) {
            LibraryScreen(
                state = libraryState,
                onSearchChange = viewModel::updateSearchQuery,
                onCreateOrUpdateSubject = { id, name, code, accent ->
                    viewModel.createOrUpdateSubject(id, name, code, accent)
                },
                onDeleteSubject = viewModel::deleteSubject,
                onOpenSubject = { subjectId ->
                    navController.navigate(ExamNestRoutes.subjectIndex(subjectId))
                },
                onExportSubject = { subject ->
                    shareText(
                        title = "${subject.name} Revision Notes",
                        body = OutlineEngine.exportSubjectMarkdown(subject)
                    )
                },
                onLinkDocumentsFolder = viewModel::linkDocumentsFolder,
                onUnlinkDocumentsFolder = viewModel::unlinkDocumentsFolder
            )
        }

        // Screen 2: Subject Index (Curriculum Structure, up to H1)
        composable(
            route = ExamNestRoutes.SUBJECT_INDEX,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val subject = allSubjects.firstOrNull { it.id == subjectId }

            SubjectDetailScreen(
                subject = subject,
                onBack = { navController.popBackStack() },
                onAddChapter = { title ->
                    viewModel.addChapter(subjectId, title)
                },
                onRenameChapter = { chapterId, newTitle ->
                    viewModel.renameChapter(subjectId, chapterId, newTitle)
                },
                onMoveChapter = { chapterId, direction ->
                    viewModel.moveChapter(subjectId, chapterId, direction)
                },
                onDeleteChapter = { chapterId ->
                    viewModel.deleteChapter(subjectId, chapterId)
                },
                onOpenChapterIndex = { chapterId ->
                    navController.navigate(ExamNestRoutes.chapterIndex(subjectId, chapterId))
                },
                onOpenChapterBlock = { chapterId, focusBlockId ->
                    navController.navigate(
                        ExamNestRoutes.chapterContent(subjectId, chapterId, focusBlockId)
                    )
                },
                onExportSubject = { targetSubject ->
                    shareText(
                        title = "${targetSubject.name} Revision Notes",
                        body = OutlineEngine.exportSubjectMarkdown(targetSubject)
                    )
                }
            )
        }

        // Screen 3: Chapter Index (Up to 2 headings: H1 & H2)
        composable(
            route = ExamNestRoutes.CHAPTER_INDEX,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val chapterId = backStackEntry.arguments?.getString("chapterId").orEmpty()
            val subject = allSubjects.firstOrNull { it.id == subjectId }

            ChapterIndexScreen(
                subject = subject,
                chapterId = chapterId,
                onBack = { navController.popBackStack() },
                onOpenNotes = { focusBlockId ->
                    navController.navigate(
                        ExamNestRoutes.chapterContent(subjectId, chapterId, focusBlockId)
                    )
                }
            )
        }

        // Screen 4: Chapter Content Hierarchical
        composable(
            route = ExamNestRoutes.CHAPTER_CONTENT,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("focusBlockId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val chapterId = backStackEntry.arguments?.getString("chapterId").orEmpty()
            val focusBlockId = backStackEntry.arguments?.getString("focusBlockId")?.takeIf { it.isNotBlank() }
            val subject = allSubjects.firstOrNull { it.id == subjectId }

            ChapterScreen(
                subject = subject,
                chapterId = chapterId,
                initialFocusBlockId = focusBlockId,
                onBackToChapterIndex = {
                    val popped = navController.popBackStack(
                        route = ExamNestRoutes.CHAPTER_INDEX,
                        inclusive = false
                    )
                    if (!popped) {
                        navController.navigate(ExamNestRoutes.chapterIndex(subjectId, chapterId)) {
                            popUpTo(ExamNestRoutes.SUBJECT_INDEX) { inclusive = false }
                        }
                    }
                },
                onOpenParagraphEditor = { existingBlockId, insertAfterBlockId ->
                    val route = when {
                        !existingBlockId.isNullOrBlank() ->
                            ExamNestRoutes.paragraphEdit(subjectId, chapterId, existingBlockId)
                        !insertAfterBlockId.isNullOrBlank() ->
                            ExamNestRoutes.paragraphCreate(subjectId, chapterId, insertAfterBlockId)
                        else -> null
                    }
                    if (route != null) {
                        navController.navigate(route)
                    }
                },
                onSaveFormula = { existingBlockId, insertAfterBlockId, title, body ->
                    viewModel.saveBlock(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        existingBlockId = existingBlockId,
                        insertAfterBlockId = insertAfterBlockId,
                        type = ElementType.F,
                        title = title,
                        body = body
                    )
                },
                onInsertHeadingAndGetId = { insertAfterId, type ->
                    viewModel.insertBlockAndGetId(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        insertAfterBlockId = insertAfterId,
                        type = type
                    )
                },
                onUpdateHeadingTitle = { blockId, title ->
                    viewModel.updateBlockContent(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        blockId = blockId,
                        title = title,
                        body = ""
                    )
                },
                onToggleExpand = { blockId ->
                    viewModel.toggleBlockExpanded(subjectId, chapterId, blockId)
                },
                onSetAllExpanded = { expanded ->
                    viewModel.setAllHeadingsExpanded(subjectId, chapterId, expanded)
                },
                onExpandAncestors = { blockId ->
                    viewModel.expandAncestorsForBlock(subjectId, chapterId, blockId)
                },
                onToggleStatus = { blockId ->
                    viewModel.toggleHeadingStatus(subjectId, chapterId, blockId)
                },
                onMoveBlock = { blockId, direction ->
                    viewModel.moveBlock(subjectId, chapterId, blockId, direction)
                },
                onDeleteBlock = { blockId ->
                    viewModel.deleteBlock(subjectId, chapterId, blockId)
                },
                onExportChapter = { chapterNum, chapter ->
                    shareText(
                        title = "Chapter $chapterNum: ${chapter.title}",
                        body = OutlineEngine.exportChapterMarkdown(chapterNum, chapter)
                    )
                }
            )
        }

        // Separate Page for Adding a Paragraph (P)
        composable(
            route = ExamNestRoutes.PARAGRAPH_CREATE,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("insertAfterBlockId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val chapterId = backStackEntry.arguments?.getString("chapterId").orEmpty()
            val insertAfterBlockId = backStackEntry.arguments?.getString("insertAfterBlockId").orEmpty()
            val subject = allSubjects.firstOrNull { it.id == subjectId }

            ParagraphEditorScreen(
                subject = subject,
                chapterId = chapterId,
                existingBlockId = null,
                onBack = { navController.popBackStack() },
                onSave = { body ->
                    viewModel.saveBlock(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        existingBlockId = null,
                        insertAfterBlockId = insertAfterBlockId,
                        type = ElementType.P,
                        title = "",
                        body = body
                    )
                },
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {}
            )
        }

        // Separate Page for Editing an existing Paragraph (P)
        composable(
            route = ExamNestRoutes.PARAGRAPH_EDIT,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("blockId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val chapterId = backStackEntry.arguments?.getString("chapterId").orEmpty()
            val blockId = backStackEntry.arguments?.getString("blockId").orEmpty()
            val subject = allSubjects.firstOrNull { it.id == subjectId }

            ParagraphEditorScreen(
                subject = subject,
                chapterId = chapterId,
                existingBlockId = blockId,
                onBack = { navController.popBackStack() },
                onSave = { body ->
                    viewModel.saveBlock(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        existingBlockId = blockId,
                        insertAfterBlockId = null,
                        type = ElementType.P,
                        title = "",
                        body = body
                    )
                },
                onMoveUp = {
                    viewModel.moveBlock(subjectId, chapterId, blockId, -1)
                },
                onMoveDown = {
                    viewModel.moveBlock(subjectId, chapterId, blockId, 1)
                },
                onDelete = {
                    viewModel.deleteBlock(subjectId, chapterId, blockId)
                }
            )
        }
    }
}
