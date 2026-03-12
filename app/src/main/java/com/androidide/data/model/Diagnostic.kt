package com.androidide.data.model

enum class DiagnosticSeverity { ERROR, WARNING, INFORMATION, HINT }

data class Diagnostic(
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val severity: DiagnosticSeverity,
    val message: String,
    val source: String = "lsp",
    val code: String? = null
)

data class CompletionItem(
    val label: String,
    val detail: String? = null,
    val documentation: String? = null,
    val insertText: String,
    val kind: CompletionItemKind = CompletionItemKind.TEXT
)

enum class CompletionItemKind {
    TEXT, METHOD, FUNCTION, CONSTRUCTOR, FIELD,
    VARIABLE, CLASS, INTERFACE, MODULE, PROPERTY,
    UNIT, VALUE, ENUM, KEYWORD, SNIPPET,
    COLOR, FILE, REFERENCE, FOLDER
}
