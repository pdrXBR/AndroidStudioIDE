package com.androidide.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline, no-network fallback assistant using heuristic completions.
 */
@Singleton
class RuleBasedAssistant @Inject constructor() : AiCodeAssistant {

    override val name: String = "Rule-Based (Offline)"
    override val isAvailable: Boolean = true

    private val pythonSnippets = mapOf(
        "def " to "def function_name(args):\n    \"\"\"Docstring.\"\"\"\n    pass\n",
        "class " to "class ClassName:\n    def __init__(self):\n        pass\n",
        "for " to "for item in iterable:\n    pass\n",
        "if " to "if condition:\n    pass\n",
        "try" to "try:\n    pass\nexcept Exception as e:\n    print(f\"Error: {e}\")\n",
        "import" to "import module_name\n",
        "with " to "with open('file.txt', 'r') as f:\n    content = f.read()\n",
        "async def" to "async def function_name():\n    await some_coroutine()\n",
        "lambda" to "lambda x: x",
        "list comp" to "[item for item in iterable if condition]",
        "dict comp" to "{key: value for key, value in iterable}",
    )

    override suspend fun complete(prompt: String, context: EditorContext): Flow<String> = flow {
        val lastWord = context.textBefore.trimEnd().split(Regex("\\s+")).lastOrNull() ?: ""
        val snippet = pythonSnippets.entries.firstOrNull { lastWord.startsWith(it.key) }?.value
        emit(snippet ?: "")
    }

    override suspend fun explain(code: String, language: String): Flow<String> = flow {
        emit("(Offline mode) This is $language code with ${code.lines().size} lines. " +
             "Connect to an AI provider in Settings for detailed explanations.")
    }

    override suspend fun generate(instruction: String, context: EditorContext): Flow<String> = flow {
        emit("# TODO: $instruction\npass\n")
    }

    override suspend fun chat(messages: List<ChatMessage>): Flow<String> = flow {
        val last = messages.lastOrNull()?.content ?: ""
        emit("(Offline mode) I received: \"$last\". " +
             "Please configure an AI provider in Settings > AI Assistant for full chat support.")
    }
}
