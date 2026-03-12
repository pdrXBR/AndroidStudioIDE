package com.androidide.lsp

import android.util.Log
import com.androidide.data.model.CompletionItem
import com.androidide.data.model.Diagnostic
import com.androidide.data.model.DiagnosticSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "LspClient"

class LspClient(
    private val process: Process,
    private val scope: CoroutineScope
) {
    private val stdin = PrintWriter(process.outputStream, true)
    private val stdout = BufferedReader(InputStreamReader(process.inputStream))
    private val idCounter = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JSONObject?>>()

    private val _diagnostics = MutableSharedFlow<List<Diagnostic>>(replay = 1)
    val diagnostics: SharedFlow<List<Diagnostic>> = _diagnostics

    init {
        scope.launch(Dispatchers.IO) { readLoop() }
    }

    private suspend fun readLoop() {
        try {
            while (isActive) {
                val header = StringBuilder()
                var line: String
                // Read headers until blank line
                while (stdout.readLine().also { line = it ?: "" } != "") {
                    header.append(line).append("\r\n")
                }
                val lengthMatch = Regex("Content-Length: (\\d+)").find(header)
                val length = lengthMatch?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val body = CharArray(length)
                stdout.read(body, 0, length)
                handleMessage(JSONObject(String(body)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "LSP read loop error", e)
        }
    }

    private fun handleMessage(msg: JSONObject) {
        when {
            msg.has("id") && msg.has("result") -> {
                val id = msg.getInt("id")
                pendingRequests[id]?.complete(msg.getJSONObject("result"))
                pendingRequests.remove(id)
            }
            msg.has("id") && msg.has("error") -> {
                val id = msg.getInt("id")
                pendingRequests[id]?.complete(null)
                pendingRequests.remove(id)
            }
            msg.has("method") -> handleNotification(msg)
        }
    }

    private fun handleNotification(msg: JSONObject) {
        when (msg.optString("method")) {
            "textDocument/publishDiagnostics" -> {
                val params = msg.getJSONObject("params")
                val uri = params.getString("uri")
                val diags = params.getJSONArray("diagnostics")
                val filePath = uri.removePrefix("file://")
                val parsed = (0 until diags.length()).map { i ->
                    val d = diags.getJSONObject(i)
                    val range = d.getJSONObject("range")
                    val start = range.getJSONObject("start")
                    val end = range.getJSONObject("end")
                    Diagnostic(
                        filePath = filePath,
                        startLine = start.getInt("line"),
                        startColumn = start.getInt("character"),
                        endLine = end.getInt("line"),
                        endColumn = end.getInt("character"),
                        severity = when (d.optInt("severity", 1)) {
                            1 -> DiagnosticSeverity.ERROR
                            2 -> DiagnosticSeverity.WARNING
                            3 -> DiagnosticSeverity.INFORMATION
                            else -> DiagnosticSeverity.HINT
                        },
                        message = d.optString("message"),
                        source = d.optString("source", "lsp")
                    )
                }
                scope.launch { _diagnostics.emit(parsed) }
            }
        }
    }

    private suspend fun sendRequest(method: String, params: JSONObject? = null): JSONObject? {
        val id = idCounter.getAndIncrement()
        val deferred = CompletableDeferred<JSONObject?>()
        pendingRequests[id] = deferred
        val message = LspProtocol.createRequest(id, method, params)
        withContext(Dispatchers.IO) { stdin.print(message); stdin.flush() }
        return withTimeoutOrNull(5000) { deferred.await() }
    }

    private fun sendNotification(method: String, params: JSONObject? = null) {
        val message = LspProtocol.createNotification(method, params)
        stdin.print(message); stdin.flush()
    }

    suspend fun initialize(rootUri: String): Boolean {
        val result = sendRequest("initialize", LspProtocol.initializeParams(rootUri))
        if (result != null) sendNotification("initialized")
        return result != null
    }

    suspend fun openDocument(uri: String, language: String, version: Int, text: String) {
        sendNotification("textDocument/didOpen", JSONObject().apply {
            put("textDocument", LspProtocol.textDocumentItem(uri, language, version, text))
        })
    }

    suspend fun changeDocument(uri: String, version: Int, text: String) {
        sendNotification("textDocument/didChange", JSONObject().apply {
            put("textDocument", JSONObject().apply {
                put("uri", uri); put("version", version)
            })
            put("contentChanges", JSONArray().apply {
                put(JSONObject().apply { put("text", text) })
            })
        })
    }

    suspend fun closeDocument(uri: String) {
        sendNotification("textDocument/didClose", JSONObject().apply {
            put("textDocument", LspProtocol.textDocumentIdentifier(uri))
        })
    }

    suspend fun getCompletions(uri: String, line: Int, col: Int): List<CompletionItem> {
        val params = LspProtocol.textDocumentPositionParams(uri, line, col).apply {
            put("context", JSONObject().apply { put("triggerKind", 1) })
        }
        val result = sendRequest("textDocument/completion", params) ?: return emptyList()
        val items: JSONArray = when {
            result.has("items") -> result.getJSONArray("items")
            result is JSONArray -> result as JSONArray
            else -> JSONArray()
        }
        return (0 until items.length()).mapNotNull { i ->
            runCatching {
                val item = items.getJSONObject(i)
                CompletionItem(
                    label = item.optString("label"),
                    detail = item.optString("detail").takeIf { it.isNotBlank() },
                    documentation = item.optJSONObject("documentation")?.optString("value"),
                    insertText = item.optString("insertText").ifBlank { item.optString("label") }
                )
            }.getOrNull()
        }
    }

    suspend fun getHover(uri: String, line: Int, col: Int): String? {
        val result = sendRequest("textDocument/hover",
            LspProtocol.textDocumentPositionParams(uri, line, col)) ?: return null
        return result.optJSONObject("contents")?.optString("value")
            ?: result.optString("contents").takeIf { it.isNotBlank() }
    }

    fun shutdown() {
        scope.launch(Dispatchers.IO) {
            runCatching { sendRequest("shutdown") }
            sendNotification("exit")
            process.destroy()
        }
    }
}
