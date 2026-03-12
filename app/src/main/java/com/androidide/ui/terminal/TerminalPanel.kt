package com.androidide.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    activeFilePath: String? = null,
    workspaceRoot: String? = null,
    onClose: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var isInteractive by remember { mutableStateOf(false) }

    // Auto scroll to bottom on new output
    LaunchedEffect(uiState.output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
            .fillMaxWidth()
    ) {
        // Terminal toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TERMINAL", fontSize = 11.sp, color = Color(0xFFBBBBBB), letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))

            if (activeFilePath?.endsWith(".py") == true) {
                TextButton(
                    onClick = {
                        activeFilePath.let { path ->
                            viewModel.runPythonScript(path, workDir = workspaceRoot)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null,
                        tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run", fontSize = 11.sp, color = Color(0xFF4CAF50))
                }
            }

            IconButton(onClick = {
                isInteractive = !isInteractive
                if (isInteractive) viewModel.startInteractiveShell(workspaceRoot)
            }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Terminal, "Shell",
                    tint = if (isInteractive) Color(0xFF4FC3F7) else Color(0xFF858585),
                    modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = viewModel::clearOutput, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, "Clear", tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp))
            }

            if (uiState.isRunning) {
                IconButton(onClick = viewModel::killCurrentProcess, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Stop, "Stop", tint = Color(0xFFF48771),
                        modifier = Modifier.size(16.dp))
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Close", tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp))
            }
        }

        // Output area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!uiState.isTermuxAvailable) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFCCA700),
                        modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Termux is not installed", color = Color(0xFFCCA700), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Install Termux from F-Droid and run: pkg install python",
                        color = Color(0xFF858585), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                Text(
                    text = uiState.output.takeLast(8000), // Limit for perf
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFD4D4D4),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                )
            }
        }

        // Interactive input
        if (isInteractive && uiState.isTermuxAvailable) {
            HorizontalDivider(color = Color(0xFF3C3C3C))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252526))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$ ", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF4CAF50))
                BasicTextField(
                    value = uiState.currentInput,
                    onValueChange = viewModel::updateInput,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFD4D4D4)
                    ),
                    cursorBrush = SolidColor(Color(0xFFAEAFAD)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendInput(uiState.currentInput) }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.sendInput(uiState.currentInput) },
                    modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Send, null, tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
