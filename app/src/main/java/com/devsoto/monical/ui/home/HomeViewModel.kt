package com.devsoto.monical.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devsoto.monical.MonicalApplication
import com.devsoto.monical.data.model.DEFAULT_RETURN_SHARE
import com.devsoto.monical.data.model.ReceiptSummary
import com.devsoto.monical.data.model.UserSettings
import com.devsoto.monical.data.repository.ReceiptRepository
import com.devsoto.monical.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Collapsible sections on Home. */
enum class HomeSection { PENDING, ARCHIVE }

/** State for the Home (dashboard) screen, backed by the single summary document. */
data class HomeUiState(
    val summary: ReceiptSummary = ReceiptSummary(),
    val returnShare: Double = DEFAULT_RETURN_SHARE,
    val sectionOpen: Map<HomeSection, Boolean> = DEFAULT_SECTIONS,
    val error: String? = null,
) {
    companion object {
        val DEFAULT_SECTIONS = mapOf(
            HomeSection.PENDING to true,
            HomeSection.ARCHIVE to false,
        )
    }
}

/** Observes the user's dashboard summary (one document read) and exposes Home actions. */
class HomeViewModel(
    private val repository: ReceiptRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val sectionOpen = MutableStateFlow(HomeUiState.DEFAULT_SECTIONS)
    private val error = MutableStateFlow<String?>(null)

    private val summary = repository.observeSummary()
        .catch { e -> error.value = e.message ?: "No se pudo cargar el resumen"; emit(ReceiptSummary()) }

    private val settings = settingsRepository.observe()
        .catch { emit(UserSettings.DEFAULT) }

    val uiState: StateFlow<HomeUiState> =
        combine(summary, settings, sectionOpen, error) { s, cfg, open, err ->
            HomeUiState(s, cfg.returnShare, open, err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleSection(section: HomeSection) {
        sectionOpen.update { it + (section to !(it[section] ?: false)) }
    }

    fun markAllReturned() {
        val pendingIds = uiState.value.summary.active.map { it.id }
        if (pendingIds.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.markReturned(pendingIds)
            } catch (e: Exception) {
                error.value = e.message ?: "No se pudo actualizar"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonicalApplication
                return HomeViewModel(app.container.receiptRepository, app.container.settingsRepository) as T
            }
        }
    }
}
