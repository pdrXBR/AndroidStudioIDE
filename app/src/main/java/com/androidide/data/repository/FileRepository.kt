package com.androidide.data.repository

import android.content.Context
import com.androidide.data.local.dao.RecentFileDao
import com.androidide.data.local.entity.RecentFileEntity
import com.androidide.data.model.FileNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentFileDao: RecentFileDao
) {

    fun getRecentFiles(): Flow<List<RecentFileEntity>> = recentFileDao.getRecentFiles()

    suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching { File(path).readText() }
    }

    suspend fun writeFile(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { File(path).writeText(content) }
    }

    suspend fun createFile(path: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.createNewFile()
            file
        }
    }

    suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
            Unit
        }
    }

    suspend fun renameFile(oldPath: String, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val old = File(oldPath)
            val new = File(old.parent, newName)
            old.renameTo(new)
            new
        }
    }

    fun buildFileTree(root: File, maxDepth: Int = 5): FileNode {
        return buildNode(root, 0, maxDepth)
    }

    private fun buildNode(file: File, level: Int, maxDepth: Int): FileNode {
        if (!file.isDirectory || level >= maxDepth) return FileNode(file, level)
        val children = file.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { buildNode(it, level + 1, maxDepth) }
            ?: emptyList()
        return FileNode(file, level, false, children)
    }

    suspend fun addToRecent(file: File, cursorLine: Int = 0, cursorCol: Int = 0) {
        recentFileDao.insertOrUpdate(
            RecentFileEntity(
                path = file.absolutePath,
                name = file.name,
                lastOpenedAt = System.currentTimeMillis(),
                cursorLine = cursorLine,
                cursorColumn = cursorCol,
                isWorkspace = file.isDirectory
            )
        )
    }

    suspend fun searchFiles(root: File, query: String): List<File> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        root.walkTopDown()
            .filter { it.isFile && it.name.contains(query, ignoreCase = true) }
            .take(100)
            .forEach { results.add(it) }
        results
    }

    suspend fun searchInFiles(root: File, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        root.walkTopDown()
            .filter { it.isFile && it.extension.isNotEmpty() }
            .forEach { file ->
                runCatching {
                    file.bufferedReader().useLines { lines ->
                        lines.forEachIndexed { lineIndex, line ->
                            if (line.contains(query, ignoreCase = true)) {
                                results.add(SearchResult(file, lineIndex + 1, line.trim(), query))
                            }
                        }
                    }
                }
            }
        results.take(500)
    }
}

data class SearchResult(
    val file: File,
    val lineNumber: Int,
    val lineContent: String,
    val matchText: String
)
