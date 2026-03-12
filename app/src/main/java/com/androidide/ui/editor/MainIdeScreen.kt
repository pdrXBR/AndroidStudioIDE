package com.androidide.ui.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidide.ui.ai.AiChatPanel
import com.androidide.ui.components.CommandPalette
import com.androidide.ui.filetree.FileTreePanel
import com.androidide.ui.terminal.TerminalPanel
import com.androidide.ui.theme.EditorColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainIdeScreen(
    onOpenSettings: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabContents by viewModel.tabContents.collectAsState()
    val context = LocalContext.current

    var showFileTree by remember { mutableStateOf(true) }
    var showTerminal by remember { mutableStateOf(false) }
    var showAiPanel by remember { mutableStateOf(false) }

    // Folder picker
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = it.path?.replace("/tree/primary:", "/storage/emulated/0/") ?: return@let
            viewModel.openWorkspace(File(path))
        }
    }

    // Command palette actions
    val commandActions = listOf(
        "Open Workspace Folder" to { folderPicker.launch(null) },
        "Save File"             to { viewModel.saveActiveTab() },
        "Toggle File Tree"      to { showFileTree = !showFileTree },
        "Toggle Terminal"       to { showTerminal = !showTerminal },
        "Toggle AI Panel"       to { showAiPanel = !showAiPanel },
        "Run Python File"       to {
            val tab = viewModel.activeTab
            if (tab != null) showTerminal = true
        },
        "Settings"              to { onOpenSettings() }
    )

    Box(modifier = Modifier.fillMaxSize().background(EditorColors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top AppBar ──────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.activeTab?.name ?: "AndroidIDE",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCCCCCC)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3C3C3C)
                ),
                actions = {
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, "Open Folder", tint = Color(0xFFCCCCCC))
                    }
                    IconButton(onClick = { viewModel.saveActiveTab() }) {
                        Icon(Icons.Default.Save, "Save", tint = Color(0xFFCCCCCC))
                    }
                    IconButton(onClick = { viewModel.toggleCommandPalette() }) {
                        Icon(Icons.Default.Search, "Commands", tint = Color(0xFFCCCCCC))
                    }
                    IconButton(onClick = { showAiPanel = !showAiPanel }) {
                        Icon(Icons.Default.AutoAwesome, "AI", tint = if (showAiPanel) Color(0xFF4FC3F7) else Color(0xFFCCCCCC))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color(0xFFCCCCCC))
                    }
                }
            )

            // ── Tab Bar ─────────────────────────────────────────────────
            if (uiState.tabs.isNotEmpty()) {
                TabBar(
                    tabs = uiState.tabs,
                    activeIndex = uiState.activeTabIndex,
                    onTabSelected = viewModel::setActiveTab,
                    onTabClosed = viewModel::closeTab
                )
            }

            // ── Main Content ────────────────────────────────────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // File tree sidebar
                if (showFileTree) {
                    FileTreePanel(
                        fileTree = uiState.fileTree,
                        workspaceRoot = uiState.workspaceRoot,
                        selectedPath = viewModel.activeTab?.file?.absolutePath,
                        modifier = Modifier.width(220.dp).fillMaxHeight(),
                        onFileClick = { file -> viewModel.openFile(file) },
                        onCreateFile = { parent -> viewModel.createFile(parent, "new_file.py") },
                        onDeleteFile = viewModel::deleteFile,
                        onTogglePanel = { showFileTree = false }
                    )
                    VerticalDivider(color = Color(0xFF3C3C3C))
                }

                // Code editor area
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val activeTab = viewModel.activeTab
                    if (activeTab != null) {
                        val content = tabContents[activeTab.id] ?: ""
                        CodeEditorView(
                            content = content,
                            language = activeTab.language,
                            diagnostics = uiState.diagnostics[activeTab.file.absolutePath] ?: emptyList(),
                            aiSuggestion = uiState.aiSuggestion,
                            settings = uiState.settings,
                            modifier = Modifier.weight(1f),
                            onContentChange = { newContent ->
                                viewModel.onContentChanged(activeTab.id, newContent)
                            },
                            onCursorChange = { line, col ->
                                viewModel.requestCompletions(line, col)
                            },
                            onRunFile = {
                                showTerminal = true
                                // Terminal picks this up via TerminalViewModel
                            },
                            onAiRequest = { before, after ->
                                viewModel.requestAiCompletion(before, after)
                            }
                        )
                    } else {
                        EmptyEditorPlaceholder(
                            onOpenFolder = { folderPicker.launch(null) }
                        )
                    }

                    // Terminal panel (collapsible)
                    if (showTerminal) {
                        HorizontalDivider(color = Color(0xFF3C3C3C))
                        TerminalPanel(
                            modifier = Modifier.height(240.dp),
                            activeFilePath = viewModel.activeTab?.file?.absolutePath,
                            workspaceRoot = uiState.workspaceRoot?.absolutePath,
                            onClose = { showTerminal = false }
                        )
                    }
                }

                // AI panel
                if (showAiPanel) {
                    VerticalDivider(color = Color(0xFF3C3C3C))
                    AiChatPanel(
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                        currentFileContent = tabContents[viewModel.activeTab?.id] ?: "",
                        currentLanguage = viewModel.activeTab?.language ?: "python",
                        onClose = { showAiPanel = false }
                    )
                }
            }

            // ── Status Bar ──────────────────────────────────────────────
            StatusBar(
                activeTab = viewModel.activeTab,
                isTermuxAvailable = true,
                diagnosticCount = uiState.diagnostics.values.sumOf { it.size },
                onToggleFileTree = { showFileTree = !showFileTree },
                onToggleTerminal = { showTerminal = !showTerminal }
            )
        }

        // Command Palette Overlay
        if (uiState.commandPaletteOpen) {
            CommandPalette(
                actions = commandActions,
                onDismiss = viewModel::closeCommandPalette,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
            )
        }
    }
}

@Composable
private fun EmptyEditorPlaceholder(onOpenFolder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(EditorColors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AndroidIDE", fontSize = 24.sp, color = Color(0xFF569CD6), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text("VS Code for Android", fontSize = 14.sp, color = Color(0xFF858585))
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onOpenFolder) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Open Folder")
        }
    }
}
