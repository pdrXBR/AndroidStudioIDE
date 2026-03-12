package com.androidide.termux

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TermuxBridge"

data class TerminalOutput(
    val text: String,
    val isError: Boolean = false,
    val isExit: Boolean = false,
    val exitCode: Int = 0
)

@Singleton
class TermuxBridge @Inject constructor() {

    private val termuxHome = "/data/data/com.termux/files"
    private val termuxBin  = "$termuxHome/usr/bin"
    private val termuxLib  = "$termuxHome/usr/lib"

    val isTermuxInstalled: Boolean
        get() = File(termuxBin, "python3").exists() || File(termuxBin, "python").exists()

    private val pythonPath: String
        get() = when {
            File(termuxBin, "python3").exists() -> "$termuxBin/python3"
            File(termuxBin, "python").exists()  -> "$termuxBin/python"
            else                                -> "python3"
        }

    /**
     * Runs a Python script via Termux and emits output line-by-line.
     */
    fun runPythonScript(
        scriptPath: String,
        args: List<String> = emptyList(),
        workDir: String? = null,
        env: Map<String, String> = emptyMap()
    ): Flow<TerminalOutput> = flow {
        val file = File(scriptPath)
        if (!file.exists()) {
            emit(TerminalOutput("Error: File not found: $scriptPath", isError = true))
            emit(TerminalOutput("", isExit = true, exitCode = 1))
            return@flow
        }

        val cmd = mutableListOf(pythonPath, scriptPath) + args
        val pb = ProcessBuilder(cmd).apply {
            if (workDir != null) directory(File(workDir))
            else directory(file.parentFile)
            environment().putAll(buildEnv())
            environment().putAll(env)
            redirectErrorStream(false)
        }

        var process: Process? = null
        try {
            process = pb.start()
            val stdoutJob = CoroutineScope(Dispatchers.IO).launch {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { emit(TerminalOutput(it + "\n")) }
                }
            }
            val stderrJob = CoroutineScope(Dispatchers.IO).launch {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { emit(TerminalOutput(it + "\n", isError = true)) }
                }
            }
            stdoutJob.join(); stderrJob.join()
            val exitCode = process.waitFor()
            emit(TerminalOutput("", isExit = true, exitCode = exitCode))
        } catch (e: Exception) {
            Log.e(TAG, "Error running script", e)
            emit(TerminalOutput("Error: ${e.message}", isError = true))
            emit(TerminalOutput("", isExit = true, exitCode = -1))
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Opens an interactive shell. Returns the Process for bidirectional I/O.
     */
    fun openShell(workDir: String? = null): Process {
        val shell = "$termuxBin/bash"
        val cmd = if (File(shell).exists()) listOf(shell, "-i") else listOf("/bin/sh", "-i")
        return ProcessBuilder(cmd).apply {
            if (workDir != null) directory(File(workDir))
            environment().putAll(buildEnv())
            redirectErrorStream(false)
        }.start()
    }

    suspend fun installPackage(packageName: String): Flow<TerminalOutput> = flow {
        val pip = when {
            File(termuxBin, "pip3").exists() -> "$termuxBin/pip3"
            File(termuxBin, "pip").exists()  -> "$termuxBin/pip"
            else -> { emit(TerminalOutput("pip not found. Install Python in Termux first.", isError = true)); return@flow }
        }
        val pb = ProcessBuilder(pip, "install", packageName).apply {
            environment().putAll(buildEnv())
            redirectErrorStream(true)
        }
        val process = pb.start()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { emit(TerminalOutput(it + "\n")) }
        }
        val exitCode = process.waitFor()
        emit(TerminalOutput("", isExit = true, exitCode = exitCode))
    }.flowOn(Dispatchers.IO)

    private fun buildEnv(): Map<String, String> = mapOf(
        "PATH"            to "$termuxBin:/usr/bin:/bin",
        "HOME"            to "$termuxHome/home",
        "LD_LIBRARY_PATH" to termuxLib,
        "PYTHONPATH"      to "$termuxHome/usr/lib/python3",
        "TERM"            to "xterm-256color",
        "LANG"            to "en_US.UTF-8",
    )
}
