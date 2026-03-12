package com.androidide.ui.filetree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.data.model.FileNode
import com.androidide.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileTreeUiState(
    val rootNode: FileNode? = null,
    val expandedPaths: Set<String> = emptySet(),
    val selectedPath: String? = null,
    val contextMenuFile: File? = null,
    val showContextMenu: Boolean = false,
    val showCreateDialog: Boolean = false,
    val createDialogParent: File? = null,
    val createDialogIsFolder: Boolean = false
)

@HiltViewModel
class FileTreeViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileTreeUiState())
    val uiState: StateFlow<FileTreeUiState> = _uiState.asStateFlow()

    fun setRoot(root: File) {
        viewModelScope.launch {
            val tree = fileRepository.buildFileTree(root)
            _uiState.update { it.copy(rootNode = tree) }
        }
    }

    fun toggleExpand(path: String) {
        _uiState.update { state ->
            val expanded = state.expandedPaths.toMutableSet()
            if (path in expanded) expanded.remove(path) else expanded.add(path)
            state.copy(expandedPaths = expanded)
        }
    }

    fun selectFile(path: String) { _uiState.update { it.copy(selectedPath = path) } }

    fun showContextMenu(file: File) {
        _uiState.update { it.copy(contextMenuFile = file, showContextMenu = true) }
    }

    fun dismissContextMenu() {
        _uiState.update { it.copy(showContextMenu = false, contextMenuFile = null) }
    }

    fun showCreateDialog(parent: File, isFolder: Boolean) {
        _uiState.update { it.copy(showCreateDialog = true, createDialogParent = parent, createDialogIsFolder = isFolder) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createDialogParent = null) }
    }
}
