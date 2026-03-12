package com.androidide.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.ai.AiRepository
import com.androidide.ai.EditorContext
import com.androidide.data.model.*
import com.androidide.data.repository.FileRepository
import com.androidide.data.repository.SearchResult
import com.androidide.data.repository.SettingsRepository
import com.androidide.lsp.LspClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class EditorUiState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val workspaceRoot: File? = null,
    val fileTree: FileNode? = null,
    val diagnostics: Map<String, List<Diagnostic>> = emptyMap(),
    val completions: List<CompletionItem> = emptyList(),
    val hoverText: String? = null,
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val aiSuggestion: String = "",
    val isAiLoading: Boolean = false,
    val settings: WorkspaceSettings = WorkspaceSettings(),
    val commandPaletteOpen: Boolean = false
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // File contents for each open tab
    private val _tabContents = MutableStateFlow<Map<String, String>>(emptyMap())
    val tabContents: StateFlow<Map<String, String>> = _tabContents.asStateFlow()

    private var lspClient: LspClient? = null
    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    // ── Workspace ──────────────────────────────────────────────────────────

    fun openWorkspace(root: File) {
        viewModelScope.launch {
            val tree = fileRepository.buildFileTree(root)
            _uiState.update { it.copy(workspaceRoot = root, fileTree = tree) }
            fileRepository.addToRecent(root)
        }
    }

    fun refreshFileTree() {
        val root = _uiState.value.workspaceRoot ?: return
        viewModelScope.launch {
            val tree = fileRepository.buildFileTree(root)
            _uiState.update { it.copy(fileTree = tree) }
        }
    }

    // ── Tabs ───────────────────────────────────────────────────────────────

    fun openFile(file: File) {
        viewModelScope.launch {
            // Check if already open
            val existingIndex = _uiState.value.tabs.indexOfFirst { it.file == file }
            if (existingIndex >= 0) {
                _uiState.update { it.copy(activeTabIndex = existingIndex) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val content = fileRepository.readFile(file.absolutePath).getOrElse { "" }
            val tab = EditorTab(
                id = UUID.randomUUID().toString(),
                file = file
            )
            val newTabs = _uiState.value.tabs + tab
            _tabContents.update { it + (tab.id to content) }
            _uiState.update { state ->
                state.copy(
                    tabs = newTabs,
                    activeTabIndex = newTabs.lastIndex,
                    isLoading = false
                )
            }
            fileRepository.addToRecent(file)
            notifyLspDocumentOpen(tab, content)
        }
    }

    fun closeTab(tabId: String) {
        val tabs = _uiState.value.tabs
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return

        val tab = tabs[index]
        viewModelScope.launch {
            lspClient?.closeDocument("file://${tab.file.absolutePath}")
        }
        val newTabs = tabs.toMutableList().also { it.removeAt(index) }
        val newIndex = when {
            newTabs.isEmpty()     -> 0
            index >= newTabs.size -> newTabs.lastIndex
            else                  -> index
        }
        _tabContents.update { it - tabId }
        _uiState.update { it.copy(tabs = newTabs, activeTabIndex = newIndex) }
    }

    fun setActiveTab(index: Int) {
        _uiState.update { it.copy(activeTabIndex = index) }
    }

    // ── Content editing ────────────────────────────────────────────────────

    fun onContentChanged(tabId: String, newContent: String) {
        _tabContents.update { it + (tabId to newContent) }
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        _uiState.update { state ->
            val newTabs = state.tabs.map {
                if (it.id == tabId) it.copy(isModified = true) else it
            }
            state.copy(tabs = newTabs)
        }
        scheduleAutoSave(tabId)
        notifyLspDocumentChange(tab, newContent)
    }

    private fun scheduleAutoSave(tabId: String) {
        if (!_uiState.value.settings.autoSave) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(_uiState.value.settings.autoSaveDelay.toLong())
            saveTab(tabId)
        }
    }

    fun saveTab(tabId: String) {
        viewModelScope.launch {
            val tab = _uiState.value.tabs.find { it.id == tabId } ?: return@launch
            val content = _tabContents.value[tabId] ?: return@launch
            fileRepository.writeFile(tab.file.absolutePath, content)
            _uiState.update { state ->
                val newTabs = state.tabs.map {
                    if (it.id == tabId) it.copy(isModified = false) else it
                }
                state.copy(tabs = newTabs)
            }
        }
    }

    fun saveActiveTab() {
        val tab = activeTab ?: return
        saveTab(tab.id)
    }

    // ── File operations ────────────────────────────────────────────────────

    fun createFile(parentDir: File, name: String) {
        viewModelScope.launch {
            val newFile = File(parentDir, name)
            fileRepository.createFile(newFile.absolutePath)
                .onSuccess { openFile(it); refreshFileTree() }
                .onFailure { _uiState.update { s -> s.copy(errorMessage = it.message) } }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            // Close tabs for this file
            _uiState.value.tabs.filter { it.file == file || it.file.absolutePath.startsWith(file.absolutePath) }
                .forEach { closeTab(it.id) }
            fileRepository.deleteFile(file.absolutePath)
            refreshFileTree()
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(file.absolutePath, newName)
                .onSuccess { refreshFileTree() }
                .onFailure { _uiState.update { s -> s.copy(errorMessage = it.message) } }
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────

    fun setSearchOpen(open: Boolean) { _uiState.update { it.copy(isSearchOpen = open) } }

    fun searchInProject(query: String) {
        val root = _uiState.value.workspaceRoot ?: return
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) { _uiState.update { it.copy(searchResults = emptyList()) }; return }
        viewModelScope.launch {
            val results = fileRepository.searchInFiles(root, query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    // ── AI ──────────────────────────────────────────────────────────────────

    fun requestAiCompletion(textBefore: String, textAfter: String) {
        val tab = activeTab ?: return
        if (!_uiState.value.settings.aiEnabled) return
        _uiState.update { it.copy(isAiLoading = true, aiSuggestion = "") }
        viewModelScope.launch {
            val ctx = EditorContext(
                filePath = tab.file.absolutePath,
                language = tab.language,
                currentLine = 0,
                currentColumn = 0,
                textBefore = textBefore,
                textAfter = textAfter,
                openFiles = _tabContents.value.entries.associate { (id, content) ->
                    val t = _uiState.value.tabs.find { it.id == id }
                    (t?.file?.absolutePath ?: id) to content
                }
            )
            aiRepository.complete(ctx)
                .catch { _uiState.update { s -> s.copy(isAiLoading = false) } }
                .collect { suggestion ->
                    _uiState.update { it.copy(aiSuggestion = suggestion, isAiLoading = false) }
                }
        }
    }

    fun dismissAiSuggestion() { _uiState.update { it.copy(aiSuggestion = "") } }

    // ── LSP ────────────────────────────────────────────────────────────────

    fun setLspClient(client: LspClient?) {
        lspClient = client
        client?.let { lsp ->
            viewModelScope.launch {
                lsp.diagnostics.collect { diags ->
                    val grouped = diags.groupBy { it.filePath }
                    _uiState.update { it.copy(diagnostics = grouped) }
                }
            }
        }
    }

    fun requestCompletions(line: Int, col: Int) {
        val tab = activeTab ?: return
        viewModelScope.launch {
            val completions = lspClient?.getCompletions("file://${tab.file.absolutePath}", line, col)
            _uiState.update { it.copy(completions = completions ?: emptyList()) }
        }
    }

    fun requestHover(line: Int, col: Int) {
        val tab = activeTab ?: return
        viewModelScope.launch {
            val hover = lspClient?.getHover("file://${tab.file.absolutePath}", line, col)
            _uiState.update { it.copy(hoverText = hover) }
        }
    }

    // ── Command palette ────────────────────────────────────────────────────
    fun toggleCommandPalette() { _uiState.update { it.copy(commandPaletteOpen = !it.commandPaletteOpen) } }
    fun closeCommandPalette() { _uiState.update { it.copy(commandPaletteOpen = false) } }

    // ── Helpers ────────────────────────────────────────────────────────────
    val activeTab: EditorTab?
        get() {
            val state = _uiState.value
            return state.tabs.getOrNull(state.activeTabIndex)
        }

    private fun notifyLspDocumentOpen(tab: EditorTab, content: String) {
        lspClient?.let { client ->
            viewModelScope.launch {
                client.openDocument("file://${tab.file.absolutePath}", tab.language, 1, content)
            }
        }
    }

    private fun notifyLspDocumentChange(tab: EditorTab, content: String) {
        lspClient?.let { client ->
            viewModelScope.launch {
                client.changeDocument("file://${tab.file.absolutePath}", 2, content)
            }
        }
    }
}
