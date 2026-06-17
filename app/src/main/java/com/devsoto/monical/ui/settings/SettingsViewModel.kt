package com.devsoto.monical.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devsoto.monical.MonicalApplication
import com.devsoto.monical.data.model.UserSettings
import com.devsoto.monical.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Settings screen: observes the user's settings and persists edits. */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        repository.observe()
            .catch { emit(UserSettings.DEFAULT) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings.DEFAULT)

    /** Persists a new return percentage (0–100). Best-effort; errors are swallowed. */
    fun setReturnPercent(percent: Int) {
        viewModelScope.launch {
            runCatching { repository.update(UserSettings.fromPercent(percent)) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonicalApplication
                return SettingsViewModel(app.container.settingsRepository) as T
            }
        }
    }
}
