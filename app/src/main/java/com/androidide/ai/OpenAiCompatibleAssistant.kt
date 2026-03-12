package com.androidide.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatible with any OpenAI-spec API: Ollama, LM Studio, OpenAI, etc.
 */
@Singleton
class OpenAiCompatibleAssistant @Inject constructor(
    private val httpClient: OkHttpClient
) : AiCodeAssistant {

    var baseUrl: String = "http://localhost:11434/v1" // Default: Ollama
    var model: String = "codellama:7b"
    var apiKey: String = "ollama" // Ollama doesn't need a real key

    override val name: String get() = "OpenAI-Compatible ($model)"
    override val isAvailable: Boolean = true // Assumes local server is up

    override suspend fun complete(prompt: String, context: EditorContext): Flow<String> = flow {
        emit(chatCompletion(listOf(
            ChatMessage("system", "You are a code completion engine. Complete the code after the cursor."),
            ChatMessage("user", context.textBefore.takeLast(2000))
        )))
    }.flowOn(Dispatchers.IO)

    override suspend fun explain(code: String, language: String): Flow<String> = flow {
        emit(chatCompletion(listOf(
            ChatMessage("system", "Explain code clearly and concisely."),
            ChatMessage("user", "Explain this $language code:\n```$language\n$code\n```")
        )))
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(instruction: String, context: EditorContext): Flow<String> = flow {
        emit(chatCompletion(listOf(
            ChatMessage("system", "Generate ${context.language} code based on the instruction."),
            ChatMessage("user", instruction)
        )))
    }.flowOn(Dispatchers.IO)

    override suspend fun chat(messages: List<ChatMessage>): Flow<String> = flow {
        val systemMsg = listOf(ChatMessage("system",
            "You are an expert Android / Python coding assistant in AndroidIDE."))
        emit(chatCompletion(systemMsg + messages))
    }.flowOn(Dispatchers.IO)

    private fun chatCompletion(messages: List<ChatMessage>): String {
        val msgsJson = JSONArray().apply {
            messages.forEach { msg ->
                put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }
        val body = JSONObject().apply {
            put("model", model)
            put("messages", msgsJson)
            put("max_tokens", 512)
            put("temperature", 0.2)
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        return httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            runCatching {
                val obj = JSONObject(response.body?.string() ?: "")
                obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }.getOrDefault("")
        }
    }
}
