package com.androidide.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidide.data.model.AiMessage
import com.androidide.data.model.MessageRole

@Composable
fun AiChatPanel(
    modifier: Modifier = Modifier,
    currentFileContent: String,
    currentLanguage: String,
    onClose: () -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(modifier = modifier.background(Color(0xFF252526))) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("AI Assistant", fontSize = 13.sp, color = Color(0xFFCCCCCC),
                modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::clearHistory, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.DeleteOutline, "Clear", tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Close", tint = Color(0xFF858585),
                    modifier = Modifier.size(16.dp))
            }
        }

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionChip("Explain", Icons.Default.HelpOutline) {
                val snippet = currentFileContent.take(500)
                viewModel.explainCode(snippet, currentLanguage)
            }
            QuickActionChip("Generate", Icons.Default.Code) {
                viewModel.generateCode("based on the current file context", currentLanguage)
            }
            QuickActionChip("Fix", Icons.Default.BugReport) {
                viewModel.updateInput("Fix any bugs or issues in this code:\n```$currentLanguage\n${currentFileContent.take(500)}\n```")
            }
        }

        HorizontalDivider(color = Color(0xFF3C3C3C))

        // Messages
        if (uiState.messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Ask me anything about your code!", color = Color(0xFF858585), fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(message = message)
                }
                if (uiState.isLoading) {
                    item {
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp, color = Color(0xFF4FC3F7))
                            Spacer(Modifier.width(8.dp))
                            Text("Thinking...", fontSize = 12.sp, color = Color(0xFF858585))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF3C3C3C))

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D))
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = uiState.currentInput,
                onValueChange = viewModel::updateInput,
                placeholder = { Text("Ask AI...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007ACC),
                    unfocusedBorderColor = Color(0xFF3C3C3C),
                    focusedTextColor = Color(0xFFD4D4D4),
                    unfocusedTextColor = Color(0xFFD4D4D4),
                    cursorColor = Color(0xFFAEAFAD),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage(currentFileContent) })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage(currentFileContent) },
                enabled = uiState.currentInput.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Send, "Send",
                    tint = if (uiState.currentInput.isNotBlank()) Color(0xFF4FC3F7) else Color(0xFF555555))
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiMessage) {
    val isUser = message.role == MessageRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "You" else "AI",
            fontSize = 10.sp,
            color = Color(0xFF858585),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            color = if (isUser) Color(0xFF264F78) else Color(0xFF2D2D2D),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                text = message.content.ifBlank { "..." },
                fontSize = 12.sp,
                fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default,
                color = Color(0xFFD4D4D4),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        icon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFF2D2D2D),
            labelColor = Color(0xFFCCCCCC),
            iconContentColor = Color(0xFF4FC3F7)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color(0xFF3C3C3C)
        )
    )
}
