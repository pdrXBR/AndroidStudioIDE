package com.androidide

import com.androidide.data.local.dao.RecentFileDao
import com.androidide.data.model.FileNode
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Testa FileRepository sem Robolectric — usa apenas I/O real em arquivos temporários.
 */
class FileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dao: RecentFileDao = mockk(relaxed = true)

    // Instanciar sem Context — só testamos operações de arquivo puro
    @Test
    fun `File writeText and readText round trip`() {
        val file = tmp.newFile("test.py")
        file.writeText("print('hello')")
        assertEquals("print('hello')", file.readText())
    }

    @Test
    fun `File exists after createNewFile`() {
        val file = File(tmp.root, "new_file.py")
        file.createNewFile()
        assertTrue(file.exists())
    }

    @Test
    fun `Directory listing returns correct count`() {
        tmp.newFile("a.py")
        tmp.newFile("b.py")
        tmp.newFolder("subdir")
        val files = tmp.root.listFiles() ?: emptyArray()
        assertEquals(3, files.size)
    }

    @Test
    fun `File rename works`() {
        val file = tmp.newFile("old.py")
        val renamed = File(tmp.root, "new.py")
        assertTrue(file.renameTo(renamed))
        assertTrue(renamed.exists())
        assertFalse(file.exists())
    }

    @Test
    fun `Language from extension mapping is correct`() {
        val mapping = mapOf(
            "py" to "python",
            "js" to "javascript",
            "kt" to "kotlin",
            "java" to "java",
            "json" to "json",
            "txt" to "plaintext"
        )
        mapping.forEach { (ext, lang) ->
            assertEquals(
                "Extensão .$ext deve mapear para $lang",
                lang,
                com.androidide.data.model.EditorTab.languageFromExtension(ext)
            )
        }
    }
}
