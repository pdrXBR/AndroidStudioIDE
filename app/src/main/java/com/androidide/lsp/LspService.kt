package com.androidide.lsp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

private const val TAG = "LspService"

@AndroidEntryPoint
class LspService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lspClient: LspClient? = null

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Starts pylsp (python -m pylsp) which must be installed in the Termux environment.
     * Falls back gracefully if the server is not available.
     */
    fun startPythonLsp(workspaceRoot: String): LspClient? {
        return try {
            val termuxPython = "/data/data/com.termux/files/usr/bin/python3"
            val pb = ProcessBuilder(termuxPython, "-m", "pylsp").apply {
                directory(File(workspaceRoot))
                environment()["PYTHONPATH"] = workspaceRoot
                redirectErrorStream(false)
            }
            val process = pb.start()
            LspClient(process, scope).also { client ->
                scope.launch {
                    val rootUri = "file://$workspaceRoot"
                    val ok = client.initialize(rootUri)
                    Log.d(TAG, "Python LSP initialized: $ok")
                }
                lspClient = client
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start pylsp: ${e.message}. LSP features disabled.")
            null
        }
    }

    fun getClient(): LspClient? = lspClient

    override fun onDestroy() {
        lspClient?.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
