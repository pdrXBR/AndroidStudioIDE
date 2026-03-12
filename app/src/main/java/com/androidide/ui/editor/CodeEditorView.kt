package com.androidide.ui.editor

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidide.data.model.Diagnostic
import com.androidide.data.model.WorkspaceSettings
import com.androidide.ui.theme.EditorColors
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

@Composable
fun CodeEditorView(
    content: String,
    language: String,
    diagnostics: List<Diagnostic>,
    aiSuggestion: String,
    settings: WorkspaceSettings,
    modifier: Modifier = Modifier,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    onRunFile: () -> Unit,
    onAiRequest: (String, String) -> Unit
) {
    Box(modifier = modifier.background(EditorColors.background)) {
        AndroidView(
            factory = { ctx ->
                CodeEditor(ctx).apply {
                    setTextSize(settings.fontSize.toFloat())
                    typefaceText = Typeface.MONOSPACE
                    typefaceLineNumber = Typeface.MONOSPACE
                    isLineNumberEnabled = settings.showLineNumbers
                    isWordwrap = settings.wordWrap
                    colorScheme = buildColorScheme()
                    setText(content)

                    subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                        onContentChange(this.text.toString())
                    }
                    subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                        onCursorChange(
                            this.cursor.leftLine,
                            this.cursor.leftColumn
                        )
                    }

                    // Aplica linguagem via TextMate se o asset existir
                    tryApplyTextMateLanguage(this, language, ctx)
                }
            },
            update = { editor ->
                val editorText = editor.text.toString()
                if (editorText != content) {
                    val line = minOf(editor.cursor.leftLine, maxOf(0, editor.lineCount - 1))
                    val col  = editor.cursor.leftColumn
                    editor.setText(content)
                    runCatching { editor.setSelection(line, col) }
                }
                editor.setTextSize(settings.fontSize.toFloat())
                editor.isLineNumberEnabled = settings.showLineNumbers
                editor.isWordwrap = settings.wordWrap
            },
            modifier = Modifier.fillMaxSize()
        )

        // AI ghost-text overlay
        if (aiSuggestion.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xFF2A2D2E).copy(alpha = 0.97f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        "AI · Tab para aceitar · Esc para descartar",
                        fontSize = 10.sp, color = Color(0xFF858585)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        aiSuggestion.take(300),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF9CDCFE)
                    )
                }
            }
        }
    }
}

private fun buildColorScheme(): EditorColorScheme = EditorColorScheme().apply {
    setColor(EditorColorScheme.WHOLE_BACKGROUND,       0xFF1E1E1E.toInt())
    setColor(EditorColorScheme.TEXT_NORMAL,            0xFFD4D4D4.toInt())
    setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF1E1E1E.toInt())
    setColor(EditorColorScheme.LINE_NUMBER,            0xFF858585.toInt())
    setColor(EditorColorScheme.LINE_NUMBER_CURRENT,    0xFFCCCCCC.toInt())
    setColor(EditorColorScheme.CURRENT_LINE,           0xFF2A2D2E.toInt())
    setColor(EditorColorScheme.SELECTION_INSERT,       0xFFAEAFAD.toInt())
    setColor(EditorColorScheme.SELECTION_HIGHLIGHT,    0xFF264F78.toInt())
    setColor(EditorColorScheme.KEYWORD,                0xFF569CD6.toInt())
    setColor(EditorColorScheme.LITERAL,                0xFFCE9178.toInt())
    setColor(EditorColorScheme.COMMENT,                0xFF6A9955.toInt())
    setColor(EditorColorScheme.FUNCTION_NAME,          0xFFDCDCAA.toInt())
    setColor(EditorColorScheme.IDENTIFIER_NAME,        0xFF9CDCFE.toInt())
    setColor(EditorColorScheme.OPERATOR,               0xFFD4D4D4.toInt())
    setColor(EditorColorScheme.BLOCK_LINE,             0xFF3C3C3C.toInt())
}

private fun tryApplyTextMateLanguage(editor: CodeEditor, language: String, ctx: android.content.Context) {
    runCatching {
        val grammarFile = when (language) {
            "python"      -> "textmate/python.tmLanguage.json"
            "javascript"  -> "textmate/javascript.tmLanguage.json"
            "typescript"  -> "textmate/typescript.tmLanguage.json"
            "kotlin"      -> "textmate/kotlin.tmLanguage.json"
            "java"        -> "textmate/java.tmLanguage.json"
            "json"        -> "textmate/json.tmLanguage.json"
            "xml"         -> "textmate/xml.tmLanguage.json"
            "html"        -> "textmate/html.tmLanguage.json"
            "css"         -> "textmate/css.tmLanguage.json"
            "markdown"    -> "textmate/markdown.tmLanguage.json"
            "shellscript" -> "textmate/shellscript.tmLanguage.json"
            else -> return
        }
        // Só aplica se o asset existir (não obrigatório para o build)
        ctx.assets.open(grammarFile).close()
        val fileProvider = io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry.getInstance()
        fileProvider.addFileProvider(
            io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver(ctx.assets)
        )
        editor.setEditorLanguage(
            io.github.rosemoe.sora.langs.textmate.TextMateLanguage.create(grammarFile, true)
        )
    }
    // Silenciosamente ignora se o asset não existir — editor funciona sem highlighting
}
