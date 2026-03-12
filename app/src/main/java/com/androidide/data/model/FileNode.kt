package com.androidide.data.model

import java.io.File

data class FileNode(
    val file: File,
    val level: Int = 0,
    val isExpanded: Boolean = false,
    val children: List<FileNode> = emptyList()
) {
    val name: String get() = file.name
    val isDirectory: Boolean get() = file.isDirectory
    val extension: String get() = file.extension.lowercase()
    val path: String get() = file.absolutePath
}
