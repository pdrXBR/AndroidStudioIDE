package com.androidide.termux

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TerminalSession"

/**
 * Manages a persistent interactive shell session.
 */
class TerminalSession(
    private val bridge: TermuxBridge,
    private val workDir: String? = null,
    private val scope: CoroutineScope
) {
    private var shellProcess: Process? = null
    private var writer: PrintWriter? = null

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun start() {
        scope.launch(Dispatchers.IO) {
            try {
                shellProcess = bridge.openShell(workDir)
                writer = PrintWriter(shellProcess!!.outputStream, true)
                _isRunning.value = true

                // Read stdout
                launch {
                    shellProcess!!.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            _output.value += line + "\n"
                        }
                    }
                }
                // Read stderr
                launch {
                    shellProcess!!.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            _output.value += line + "\n"
                        }
                    }
                }
                shellProcess!!.waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Shell error", e)
                _output.value += "\n[Session ended with error: ${e.message}]\n"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun sendInput(text: String) {
        scope.launch(Dispatchers.IO) {
            writer?.println(text)
        }
    }

    fun clear() { _output.value = "" }

    fun kill() {
        shellProcess?.destroy()
        _isRunning.value = false
    }
}
