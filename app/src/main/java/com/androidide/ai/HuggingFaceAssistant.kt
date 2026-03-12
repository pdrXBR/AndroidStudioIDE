package com.androidide.ai

import com.androidide.BuildConfig
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HuggingFaceAssistant @Inject constructor(
    private val httpClient: OkHttpClient
) : AiCodeAssistant {

    private var apiKey: String = ""
    private val model = "bigcode/starcoder2-3b"
    private val baseUrl = "https://api-inference.huggingface.co/models/"

    fun setApiKey(key: String) { apiKey = key }

    override val name: String get() = "HuggingFace ($model)"
    override val isAvailable: Boolean get() = apiKey.isNotBlank()

    override suspend fun complete(prompt: String, context: EditorContext): Flow<String> = flow {
        val fullPrompt = buildCompletionPrompt(context)
        val response = callApi(fullPrompt)
        emit(response)
    }.flowOn(Dispatchers.IO)

    override suspend fun explain(code: String, language: String): Flow<String> = flow {
        val prompt = "Explain the following $language code concisely:\n\n```$language\n$code\n```\n\nExplanation:"
        emit(callApi(prompt))
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(instruction: String, context: EditorContext): Flow<String> = flow {
        val prompt = """
            |# Language: ${context.language}
            |# File: ${context.filePath}
            |# Instruction: $instruction
            |
            |${context.textBefore}
        """.trimMargin()
        emit(callApi(prompt))
    }.flowOn(Dispatchers.IO)

    override suspend fun chat(messages: List<ChatMessage>): Flow<String> = flow {
        val systemPrompt = "You are an expert coding assistant integrated into AndroidIDE. " +
            "Help with code, debugging, explanations, and best practices."
        val prompt = messages.joinToString("\n") { "${it.role}: ${it.content}" } + "\nAssistant:"
        emit(callApi("$systemPrompt\n\n$prompt"))
    }.flowOn(Dispatchers.IO)

    private fun buildCompletionPrompt(ctx: EditorContext): String {
        val recentContext = ctx.textBefore.takeLast(2000)
        return recentContext
    }

    private fun callApi(inputs: String): String {
        val json = JSONObject().apply {
            put("inputs", inputs)
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 256)
                put("temperature", 0.2)
                put("top_p", 0.95)
                put("do_sample", true)
                put("return_full_text", false)
            })
            put("options", JSONObject().apply {
                put("wait_for_model", true)
                put("use_cache", true)
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$baseUrl$model")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .post(body)
            .build()

        return httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            val responseText = response.body?.string() ?: return@use ""
            runCatching {
                val arr = JSONArray(responseText)
                arr.getJSONObject(0).optString("generated_text", "")
            }.getOrDefault("")
        }
    }
}
