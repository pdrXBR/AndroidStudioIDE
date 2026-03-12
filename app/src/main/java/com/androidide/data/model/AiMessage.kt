package com.androidide.data.model

import java.util.UUID

enum class MessageRole { USER, ASSISTANT, SYSTEM }

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val codeBlocks: List<CodeBlock> = emptyList()
)

data class CodeBlock(
    val language: String,
    val code: String,
    val startIndex: Int,
    val endIndex: Int
)
