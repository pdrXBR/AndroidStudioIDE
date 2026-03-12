package com.androidide.ai

import kotlinx.coroutines.flow.Flow

interface AiCodeAssistant {
    suspend fun complete(prompt: String, context: EditorContext): Flow<String>
    suspend fun explain(code: String, language: String): Flow<String>
    suspend fun generate(instruction: String, context: EditorContext): Flow<String>
    suspend fun chat(messages: List<ChatMessage>): Flow<String>
    val name: String
    val isAvailable: Boolean
}

data class EditorContext(
    val filePath: String,
    val language: String,
    val currentLine: Int,
    val currentColumn: Int,
    val textBefore: String,
    val textAfter: String,
    val openFiles: Map<String, String> = emptyMap()
)

data class ChatMessage(val role: String, val content: String)
