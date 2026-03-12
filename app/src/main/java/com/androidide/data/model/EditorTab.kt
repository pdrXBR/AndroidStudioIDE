package com.androidide.data.model

import java.io.File

data class EditorTab(
    val id: String,
    val file: File,
    val isModified: Boolean = false,
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
    val scrollY: Int = 0
) {
    val name: String get() = file.name
    val language: String get() = languageFromExtension(file.extension)

    companion object {
        fun languageFromExtension(ext: String): String = when (ext.lowercase()) {
            "py"          -> "python"
            "js", "mjs"   -> "javascript"
            "ts"          -> "typescript"
            "kt", "kts"   -> "kotlin"
            "java"        -> "java"
            "json"        -> "json"
            "xml"         -> "xml"
            "md"          -> "markdown"
            "html", "htm" -> "html"
            "css"         -> "css"
            "sh", "bash"  -> "shellscript"
            "c", "h"      -> "c"
            "cpp","hpp"   -> "cpp"
            "rs"          -> "rust"
            "go"          -> "go"
            "yaml","yml"  -> "yaml"
            "toml"        -> "toml"
            else          -> "plaintext"
        }
    }
}
