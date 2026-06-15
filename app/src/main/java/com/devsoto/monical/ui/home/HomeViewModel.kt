package com.devsoto.monical.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devsoto.monical.AppContainer
import com.devsoto.monical.MonicalApplication
import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State for the Home (receipt list) screen. */
data class HomeUiState(
    val receipts: List<Receipt> = emptyList(),
    val sectionOpen: Map<ReturnStatus, Boolean> = DEFAULT_SECTIONS,
    val error: String? = null,
) {
    companion object {
        val DEFAULT_SECTIONS = mapOf(
            ReturnStatus.PENDING to true,
            ReturnStatus.RETURNED to true,
            ReturnStatus.NONE to false,
        )
    }
}

/** Observes the user's receipts and exposes Home actions. */
class HomeViewModel(private val repository: ReceiptRepository) : ViewModel() {

    private val sectionOpen = MutableStateFlow(HomeUiState.DEFAULT_SECTIONS)
    private val error = MutableStateFlow<String?>(null)

    private val receipts = repository.observeReceipts()
        .catch { e -> error.value = e.message ?: "No se pudieron cargar los recibos"; emit(emptyList()) }

    val uiState: StateFlow<HomeUiState> =
        combine(receipts, sectionOpen, error) { r, open, err -> HomeUiState(r, open, err) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleSection(status: ReturnStatus) {
        sectionOpen.update { it + (status to !(it[status] ?: false)) }
    }

    fun markAllReturned() {
        val pendingIds = uiState.value.receipts
            .filter { it.returnStatus == ReturnStatus.PENDING }
            .map { it.id }
        if (pendingIds.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.markReturned(pendingIds)
            } catch (e: Exception) {
                error.value = e.message ?: "No se pudo actualizar"
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: Exception) {
                error.value = e.message ?: "No se pudo eliminar"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonicalApplication
                return HomeViewModel(app.container.receiptRepository) as T
            }
        }
    }
}
