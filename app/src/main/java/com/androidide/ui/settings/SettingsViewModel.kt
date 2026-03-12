package com.androidide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.data.model.WorkspaceSettings
import com.androidide.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<WorkspaceSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkspaceSettings())

    fun updateSettings(settings: WorkspaceSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }
}
