package com.androidide.data.model

data class WorkspaceSettings(
    val fontFamily: String = "JetBrains Mono",
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val useTabs: Boolean = false,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val showMinimap: Boolean = true,
    val autoSave: Boolean = true,
    val autoSaveDelay: Int = 1000,
    val theme: EditorTheme = EditorTheme.DARK,
    val aiEnabled: Boolean = true,
    val aiProvider: AiProvider = AiProvider.HUGGINGFACE,
    val huggingFaceApiKey: String = "",
    val openAiApiKey: String = "",
    val localModelPath: String = "",
    val termuxPath: String = "/data/data/com.termux/files"
)

enum class EditorTheme { DARK, LIGHT, HIGH_CONTRAST }
enum class AiProvider { HUGGINGFACE, OPENAI_COMPATIBLE, LOCAL, RULE_BASED }
