package com.androidide.domain.usecase

import com.androidide.termux.TerminalOutput
import com.androidide.termux.TermuxBridge
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunPythonUseCase @Inject constructor(
    private val termuxBridge: TermuxBridge
) {
    operator fun invoke(
        scriptPath: String,
        args: List<String> = emptyList(),
        workDir: String? = null
    ): Flow<TerminalOutput> = termuxBridge.runPythonScript(scriptPath, args, workDir)

    val isAvailable: Boolean get() = termuxBridge.isTermuxInstalled
}
