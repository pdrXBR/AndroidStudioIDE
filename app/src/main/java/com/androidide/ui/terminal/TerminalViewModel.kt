package com.androidide.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.termux.TerminalOutput
import com.androidide.termux.TermuxBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val output: String = "",
    val isRunning: Boolean = false,
    val isTermuxAvailable: Boolean = false,
    val currentInput: String = "",
    val exitCode: Int? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val termuxBridge: TermuxBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState(
        isTermuxAvailable = termuxBridge.isTermuxInstalled
    ))
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var shellProcess: Process? = null
    private var shellWriter: java.io.PrintWriter? = null

    fun runPythonScript(scriptPath: String, args: List<String> = emptyList(), workDir: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, exitCode = null) }
            appendOutput("▶ Running: python3 $scriptPath\n")
            termuxBridge.runPythonScript(scriptPath, args, workDir)
                .collect { output: TerminalOutput ->
                    when {
                        output.isExit -> {
                            val msg = if (output.exitCode == 0)
                                "\n✓ Finished (exit 0)\n" else "\n✗ Exited with code ${output.exitCode}\n"
                            appendOutput(msg)
                            _uiState.update { it.copy(isRunning = false, exitCode = output.exitCode) }
                        }
                        output.isError -> appendOutput("\u001b[31m${output.text}\u001b[0m") // ANSI red
                        else -> appendOutput(output.text)
                    }
                }
        }
    }

    fun startInteractiveShell(workDir: String? = null) {
        viewModelScope.launch {
            killCurrentProcess()
            appendOutput("$ Starting shell...\n")
            try {
                val proc = termuxBridge.openShell(workDir)
                shellProcess = proc
                shellWriter = java.io.PrintWriter(proc.outputStream, true)
                _uiState.update { it.copy(isRunning = true) }
                // Read output in background
                launch {
                    proc.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { appendOutput(it + "\n") }
                    }
                    _uiState.update { it.copy(isRunning = false) }
                }
            } catch (e: Exception) {
                appendOutput("Shell error: ${e.message}\n")
                _uiState.update { it.copy(isRunning = false) }
            }
        }
    }

    fun sendInput(input: String) {
        shellWriter?.println(input)
        appendOutput("$ $input\n")
        _uiState.update { it.copy(currentInput = "") }
    }

    fun updateInput(input: String) { _uiState.update { it.copy(currentInput = input) } }

    fun clearOutput() { _uiState.update { it.copy(output = "") } }

    fun killCurrentProcess() {
        shellProcess?.destroy()
        shellProcess = null
        shellWriter = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun installPackage(name: String) {
        viewModelScope.launch {
            appendOutput("$ pip install $name\n")
            termuxBridge.installPackage(name).collect { output ->
                if (!output.isExit) appendOutput(output.text)
            }
        }
    }

    private fun appendOutput(text: String) {
        _uiState.update { it.copy(output = it.output + text) }
    }

    override fun onCleared() {
        killCurrentProcess()
        super.onCleared()
    }
}
