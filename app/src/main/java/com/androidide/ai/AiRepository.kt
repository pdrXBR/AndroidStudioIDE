package com.androidide.ai

import com.androidide.data.local.dao.ChatHistoryDao
import com.androidide.data.local.entity.ChatHistoryEntity
import com.androidide.data.model.AiProvider
import com.androidide.data.model.WorkspaceSettings
import com.androidide.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val hfAssistant: HuggingFaceAssistant,
    private val openAiAssistant: OpenAiCompatibleAssistant,
    private val ruleAssistant: RuleBasedAssistant,
    private val settingsRepository: SettingsRepository,
    private val chatHistoryDao: ChatHistoryDao
) {
    private var currentSettings: WorkspaceSettings = WorkspaceSettings()

    private val activeAssistant: AiCodeAssistant
        get() = when {
            !currentSettings.aiEnabled -> ruleAssistant
            currentSettings.aiProvider == AiProvider.HUGGINGFACE && hfAssistant.isAvailable -> hfAssistant
            currentSettings.aiProvider == AiProvider.OPENAI_COMPATIBLE -> openAiAssistant
            currentSettings.aiProvider == AiProvider.LOCAL -> ruleAssistant
            else -> ruleAssistant
        }

    suspend fun initialize() {
        settingsRepository.settings.collect { settings ->
            currentSettings = settings
            hfAssistant.setApiKey(settings.huggingFaceApiKey)
            openAiAssistant.apiKey = settings.openAiApiKey.ifBlank { "ollama" }
        }
    }

    suspend fun complete(context: EditorContext): Flow<String> = activeAssistant.complete("", context)
    suspend fun explain(code: String, language: String): Flow<String> = activeAssistant.explain(code, language)
    suspend fun generate(instruction: String, context: EditorContext): Flow<String> = activeAssistant.generate(instruction, context)
    suspend fun chat(messages: List<ChatMessage>): Flow<String> = activeAssistant.chat(messages)

    suspend fun saveMessage(sessionId: String, msg: com.androidide.data.model.AiMessage) {
        chatHistoryDao.insert(
            ChatHistoryEntity(
                id = msg.id,
                role = msg.role.name,
                content = msg.content,
                timestamp = msg.timestamp,
                sessionId = sessionId
            )
        )
    }

    fun getChatHistory(sessionId: String) = chatHistoryDao.getMessagesForSession(sessionId)
}
