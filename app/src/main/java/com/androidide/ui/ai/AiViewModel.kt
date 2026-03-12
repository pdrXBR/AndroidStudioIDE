package com.androidide.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.ai.AiRepository
import com.androidide.ai.ChatMessage
import com.androidide.data.model.AiMessage
import com.androidide.data.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val sessionId: String = UUID.randomUUID().toString(),
    val errorMessage: String? = null
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun updateInput(text: String) { _uiState.update { it.copy(currentInput = text) } }

    fun sendMessage(contextCode: String = "") {
        val input = _uiState.value.currentInput.trim()
        if (input.isBlank()) return

        val userMessage = AiMessage(role = MessageRole.USER, content = input)
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                currentInput = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            val assistantMsg = AiMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                isStreaming = true
            )
            _uiState.update { it.copy(messages = it.messages + assistantMsg) }

            val historyMessages = _uiState.value.messages
                .filter { !it.isStreaming }
                .takeLast(10)
                .map { ChatMessage(it.role.name.lowercase(), it.content) }

            val prefix = if (contextCode.isNotBlank()) "Context:\n```\n$contextCode\n```\n\n" else ""
            val messagesWithContext = historyMessages.dropLast(1) +
                ChatMessage("user", prefix + input)

            var fullResponse = ""
            aiRepository.chat(messagesWithContext)
                .catch { e ->
                    _uiState.update { state ->
                        val updated = state.messages.dropLast(1) +
                            assistantMsg.copy(content = "Error: ${e.message}", isStreaming = false)
                        state.copy(messages = updated, isLoading = false)
                    }
                }
                .collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { state ->
                        val updated = state.messages.dropLast(1) +
                            assistantMsg.copy(content = fullResponse, isStreaming = false)
                        state.copy(messages = updated)
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(messages = emptyList(), sessionId = UUID.randomUUID().toString()) }
        }
    }

    fun explainCode(code: String, language: String) {
        _uiState.update { it.copy(currentInput = "Explain this $language code:\n```$language\n$code\n```") }
        sendMessage()
    }

    fun generateCode(instruction: String, language: String) {
        _uiState.update { it.copy(currentInput = "Generate $language code: $instruction") }
        sendMessage()
    }
}
