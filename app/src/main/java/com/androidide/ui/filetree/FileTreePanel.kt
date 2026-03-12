package com.androidide.ui.filetree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.data.model.FileNode
import java.io.File

@Composable
fun FileTreePanel(
    fileTree: FileNode?,
    workspaceRoot: File?,
    selectedPath: String?,
    modifier: Modifier = Modifier,
    onFileClick: (File) -> Unit,
    onCreateFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onTogglePanel: () -> Unit
) {
    var expandedPaths by remember { mutableStateOf(setOf<String>()) }
    var contextMenuFile by remember { mutableStateOf<File?>(null) }

    Column(modifier = modifier.background(Color(0xFF252526))) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER",
                fontSize = 11.sp,
                color = Color(0xFFBBBBBB),
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onTogglePanel, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ChevronLeft, "Collapse", tint = Color(0xFFBBBBBB),
                    modifier = Modifier.size(16.dp))
            }
        }

        if (fileTree == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF858585),
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("No folder opened", fontSize = 12.sp, color = Color(0xFF858585))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                fun collectNodes(node: FileNode): List<FileNode> {
                    val nodes = mutableListOf(node)
                    if (node.isDirectory && node.path in expandedPaths) {
                        node.children.forEach { nodes.addAll(collectNodes(it)) }
                    }
                    return nodes
                }
                val flatNodes = fileTree.children.flatMap { collectNodes(it) }
                items(flatNodes, key = { it.path }) { node ->
                    FileTreeItem(
                        node = node,
                        isExpanded = node.path in expandedPaths,
                        isSelected = node.path == selectedPath,
                        onClick = {
                            if (node.isDirectory) {
                                expandedPaths = if (node.path in expandedPaths)
                                    expandedPaths - node.path
                                else expandedPaths + node.path
                            } else {
                                onFileClick(node.file)
                            }
                        },
                        onLongClick = { contextMenuFile = node.file }
                    )
                }
            }
        }
    }

    // Context menu
    contextMenuFile?.let { file ->
        AlertDialog(
            onDismissRequest = { contextMenuFile = null },
            title = { Text(file.name, fontSize = 14.sp) },
            text = {
                Column {
                    if (file.isDirectory) {
                        TextButton(onClick = { onCreateFile(file); contextMenuFile = null }) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                            Text("New File")
                        }
                    }
                    TextButton(onClick = { onDeleteFile(file); contextMenuFile = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF48771))) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { contextMenuFile = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val indent = (node.level * 16).dp
    val bg = if (isSelected) Color(0xFF37373D) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(start = 8.dp + indent, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = fileIcon(node),
            contentDescription = null,
            tint = fileIconColor(node),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = node.name,
            fontSize = 13.sp,
            fontFamily = FontFamily.Default,
            color = if (isSelected) Color.White else Color(0xFFCCCCCC),
            maxLines = 1
        )
    }
}

private fun fileIcon(node: FileNode): ImageVector = when {
    node.isDirectory -> Icons.Default.Folder
    node.extension == "py" -> Icons.Default.Code
    node.extension in listOf("js","ts","jsx","tsx") -> Icons.Default.Code
    node.extension == "kt" -> Icons.Default.Code
    node.extension in listOf("json","yaml","yml","toml") -> Icons.Default.DataObject
    node.extension == "md" -> Icons.Default.Article
    node.extension in listOf("png","jpg","jpeg","gif","svg","webp") -> Icons.Default.Image
    else -> Icons.Default.InsertDriveFile
}

private fun fileIconColor(node: FileNode): Color = when {
    node.isDirectory -> Color(0xFFDCB67A)
    node.extension == "py" -> Color(0xFF3572A5)
    node.extension in listOf("js","jsx") -> Color(0xFFF7DF1E)
    node.extension in listOf("ts","tsx") -> Color(0xFF3178C6)
    node.extension == "kt" -> Color(0xFF7F52FF)
    node.extension == "java" -> Color(0xFFB07219)
    node.extension == "md" -> Color(0xFF519ABA)
    node.extension in listOf("json","yaml","yml") -> Color(0xFFCBCB41)
    else -> Color(0xFFCCCCCC)
}
