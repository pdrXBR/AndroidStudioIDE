package com.androidide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.androidide.data.model.AiProvider
import com.androidide.data.model.EditorTheme
import com.androidide.data.model.WorkspaceSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val FONT_SIZE        = intPreferencesKey("font_size")
        val FONT_FAMILY      = stringPreferencesKey("font_family")
        val TAB_SIZE         = intPreferencesKey("tab_size")
        val USE_TABS         = booleanPreferencesKey("use_tabs")
        val WORD_WRAP        = booleanPreferencesKey("word_wrap")
        val SHOW_LINE_NUMS   = booleanPreferencesKey("show_line_numbers")
        val SHOW_MINIMAP     = booleanPreferencesKey("show_minimap")
        val AUTO_SAVE        = booleanPreferencesKey("auto_save")
        val AUTO_SAVE_DELAY  = intPreferencesKey("auto_save_delay")
        val EDITOR_THEME     = stringPreferencesKey("editor_theme")
        val AI_ENABLED       = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER      = stringPreferencesKey("ai_provider")
        val HF_API_KEY       = stringPreferencesKey("hf_api_key")
        val OPENAI_API_KEY   = stringPreferencesKey("openai_api_key")
        val LOCAL_MODEL_PATH = stringPreferencesKey("local_model_path")
    }

    val settings: Flow<WorkspaceSettings> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            WorkspaceSettings(
                fontFamily      = prefs[Keys.FONT_FAMILY] ?: "JetBrains Mono",
                fontSize        = prefs[Keys.FONT_SIZE] ?: 14,
                tabSize         = prefs[Keys.TAB_SIZE] ?: 4,
                useTabs         = prefs[Keys.USE_TABS] ?: false,
                wordWrap        = prefs[Keys.WORD_WRAP] ?: false,
                showLineNumbers = prefs[Keys.SHOW_LINE_NUMS] ?: true,
                showMinimap     = prefs[Keys.SHOW_MINIMAP] ?: true,
                autoSave        = prefs[Keys.AUTO_SAVE] ?: true,
                autoSaveDelay   = prefs[Keys.AUTO_SAVE_DELAY] ?: 1000,
                theme           = EditorTheme.valueOf(prefs[Keys.EDITOR_THEME] ?: EditorTheme.DARK.name),
                aiEnabled       = prefs[Keys.AI_ENABLED] ?: true,
                aiProvider      = AiProvider.valueOf(prefs[Keys.AI_PROVIDER] ?: AiProvider.HUGGINGFACE.name),
                huggingFaceApiKey = prefs[Keys.HF_API_KEY] ?: "",
                openAiApiKey    = prefs[Keys.OPENAI_API_KEY] ?: "",
                localModelPath  = prefs[Keys.LOCAL_MODEL_PATH] ?: ""
            )
        }

    suspend fun updateSettings(settings: WorkspaceSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_FAMILY]      = settings.fontFamily
            prefs[Keys.FONT_SIZE]        = settings.fontSize
            prefs[Keys.TAB_SIZE]         = settings.tabSize
            prefs[Keys.USE_TABS]         = settings.useTabs
            prefs[Keys.WORD_WRAP]        = settings.wordWrap
            prefs[Keys.SHOW_LINE_NUMS]   = settings.showLineNumbers
            prefs[Keys.SHOW_MINIMAP]     = settings.showMinimap
            prefs[Keys.AUTO_SAVE]        = settings.autoSave
            prefs[Keys.AUTO_SAVE_DELAY]  = settings.autoSaveDelay
            prefs[Keys.EDITOR_THEME]     = settings.theme.name
            prefs[Keys.AI_ENABLED]       = settings.aiEnabled
            prefs[Keys.AI_PROVIDER]      = settings.aiProvider.name
            prefs[Keys.HF_API_KEY]       = settings.huggingFaceApiKey
            prefs[Keys.OPENAI_API_KEY]   = settings.openAiApiKey
            prefs[Keys.LOCAL_MODEL_PATH] = settings.localModelPath
        }
    }
}
