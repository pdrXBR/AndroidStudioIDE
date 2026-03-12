package com.androidide.lsp

import org.json.JSONObject

/** Minimal JSON-RPC / LSP message builder and parser. */
object LspProtocol {

    fun createRequest(id: Int, method: String, params: JSONObject? = null): String {
        val obj = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            if (params != null) put("params", params)
        }
        val body = obj.toString()
        return "Content-Length: ${body.length}\r\n\r\n$body"
    }

    fun createNotification(method: String, params: JSONObject? = null): String {
        val obj = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            if (params != null) put("params", params)
        }
        val body = obj.toString()
        return "Content-Length: ${body.length}\r\n\r\n$body"
    }

    fun parseMessage(raw: String): JSONObject? = runCatching {
        val bodyStart = raw.indexOf("\r\n\r\n")
        if (bodyStart == -1) return null
        JSONObject(raw.substring(bodyStart + 4))
    }.getOrNull()

    // ── Helpers ──────────────────────────────────────────────────────────

    fun initializeParams(rootUri: String): JSONObject = JSONObject().apply {
        put("processId", android.os.Process.myPid())
        put("rootUri", rootUri)
        put("capabilities", JSONObject().apply {
            put("textDocument", JSONObject().apply {
                put("completion", JSONObject().apply {
                    put("completionItem", JSONObject().apply {
                        put("snippetSupport", true)
                        put("documentationFormat", listOf("markdown", "plaintext").let {
                            org.json.JSONArray().apply { it.forEach { s -> put(s) } }
                        })
                    })
                })
                put("hover", JSONObject().apply { put("contentFormat", org.json.JSONArray().apply { put("markdown") }) })
                put("definition", JSONObject())
                put("references", JSONObject())
                put("rename", JSONObject())
                put("publishDiagnostics", JSONObject())
            })
            put("workspace", JSONObject().apply {
                put("applyEdit", true)
                put("symbol", JSONObject())
            })
        })
        put("initializationOptions", JSONObject())
    }

    fun textDocumentItem(uri: String, language: String, version: Int, text: String): JSONObject =
        JSONObject().apply {
            put("uri", uri)
            put("languageId", language)
            put("version", version)
            put("text", text)
        }

    fun position(line: Int, character: Int): JSONObject = JSONObject().apply {
        put("line", line)
        put("character", character)
    }

    fun textDocumentIdentifier(uri: String): JSONObject = JSONObject().apply { put("uri", uri) }

    fun textDocumentPositionParams(uri: String, line: Int, col: Int): JSONObject = JSONObject().apply {
        put("textDocument", textDocumentIdentifier(uri))
        put("position", position(line, col))
    }
}
