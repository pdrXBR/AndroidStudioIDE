package com.androidide.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidide.data.model.AiProvider
import com.androidide.data.model.EditorTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D))
            )
        },
        containerColor = Color(0xFF1E1E1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Editor ──────────────────────────────────────────────────
            SettingsSection("Editor") {
                SliderSetting("Font Size", settings.fontSize.toFloat(), 10f, 30f, 1f) { v ->
                    viewModel.updateSettings(settings.copy(fontSize = v.toInt()))
                }
                SwitchSetting("Show Line Numbers", settings.showLineNumbers) { v ->
                    viewModel.updateSettings(settings.copy(showLineNumbers = v))
                }
                SwitchSetting("Word Wrap", settings.wordWrap) { v ->
                    viewModel.updateSettings(settings.copy(wordWrap = v))
                }
                SwitchSetting("Auto Save", settings.autoSave) { v ->
                    viewModel.updateSettings(settings.copy(autoSave = v))
                }
                SliderSetting("Tab Size", settings.tabSize.toFloat(), 2f, 8f, 1f) { v ->
                    viewModel.updateSettings(settings.copy(tabSize = v.toInt()))
                }
                SwitchSetting("Use Tabs (not spaces)", settings.useTabs) { v ->
                    viewModel.updateSettings(settings.copy(useTabs = v))
                }
            }

            // ── AI Assistant ────────────────────────────────────────────
            SettingsSection("AI Assistant") {
                SwitchSetting("Enable AI", settings.aiEnabled) { v ->
                    viewModel.updateSettings(settings.copy(aiEnabled = v))
                }
                if (settings.aiEnabled) {
                    DropdownSetting(
                        label = "Provider",
                        options = AiProvider.values().map { it.name },
                        selected = settings.aiProvider.name,
                        onSelect = { v ->
                            viewModel.updateSettings(settings.copy(aiProvider = AiProvider.valueOf(v)))
                        }
                    )
                    if (settings.aiProvider == AiProvider.HUGGINGFACE) {
                        TextInputSetting(
                            label = "HuggingFace API Key",
                            value = settings.huggingFaceApiKey,
                            hint = "hf_...",
                            isPassword = true,
                            onChange = { viewModel.updateSettings(settings.copy(huggingFaceApiKey = it)) }
                        )
                        Text(
                            "Free tier available at huggingface.co. Leave empty to use public models (rate limited).",
                            fontSize = 11.sp, color = Color(0xFF858585),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    if (settings.aiProvider == AiProvider.OPENAI_COMPATIBLE) {
                        TextInputSetting("Base URL (e.g. http://localhost:11434/v1)",
                            "", "Ollama/LMStudio URL", false, {})
                        TextInputSetting("API Key", settings.openAiApiKey, "sk-...", true) {
                            viewModel.updateSettings(settings.copy(openAiApiKey = it))
                        }
                    }
                }
            }

            // ── Theme ───────────────────────────────────────────────────
            SettingsSection("Appearance") {
                DropdownSetting(
                    label = "Theme",
                    options = EditorTheme.values().map { it.name },
                    selected = settings.theme.name,
                    onSelect = { v ->
                        viewModel.updateSettings(settings.copy(theme = EditorTheme.valueOf(v)))
                    }
                )
            }

            // ── About ───────────────────────────────────────────────────
            SettingsSection("About") {
                Text("AndroidIDE v1.0.0", fontSize = 13.sp, color = Color(0xFFD4D4D4))
                Text("A VS Code-inspired IDE for Android", fontSize = 12.sp, color = Color(0xFF858585))
                Spacer(Modifier.height(4.dp))
                Text("Editor: Sora Editor (Rosemoe)", fontSize = 11.sp, color = Color(0xFF858585))
                Text("LSP: pylsp via Termux", fontSize = 11.sp, color = Color(0xFF858585))
                Text("AI: HuggingFace / Ollama / Rule-Based", fontSize = 11.sp, color = Color(0xFF858585))
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), fontSize = 11.sp, color = Color(0xFF007ACC),
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252526)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SwitchSetting(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFFD4D4D4))
        Switch(value, onChange, modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SliderSetting(label: String, value: Float, min: Float, max: Float, steps: Float, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = Color(0xFFD4D4D4))
            Text(value.toInt().toString(), fontSize = 13.sp, color = Color(0xFF4FC3F7))
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max,
            steps = ((max - min) / steps).toInt() - 1)
    }
}

@Composable
fun DropdownSetting(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFFD4D4D4))
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selected, fontSize = 12.sp) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF3C3C3C))) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, fontSize = 12.sp, color = Color(0xFFD4D4D4)) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun TextInputSetting(label: String, value: String, hint: String, isPassword: Boolean, onChange: (String) -> Unit) {
    var localValue by remember(value) { mutableStateOf(value) }
    Column {
        Text(label, fontSize = 12.sp, color = Color(0xFF858585))
        OutlinedTextField(
            value = localValue,
            onValueChange = { localValue = it; onChange(it) },
            placeholder = { Text(hint, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword)
                androidx.compose.ui.text.input.PasswordVisualTransformation() else
                androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}
