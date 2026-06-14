package com.devsoto.monical.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devsoto.monical.AppContainer
import com.devsoto.monical.MonicalApplication
import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.ocr.MlKitTextRecognizer
import com.devsoto.monical.data.parse.ReceiptParser
import com.devsoto.monical.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the whole scan flow and is shared between the capture and review screens so the
 * extracted [ReceiptDraft] survives navigation without serializing it through nav args.
 *
 * Capture screen: [processImage]. Review screen: the `update*`/`addItem`/`removeItem`
 * editors and [save].
 */
class ScanViewModel(
    private val textRecognizer: MlKitTextRecognizer,
    private val parser: ReceiptParser,
    private val repository: ReceiptRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /** Runs OCR + parsing on the picked image. Emits [ScanUiState.draft] on success. */
    fun processImage(uri: Uri) {
        _uiState.update { it.copy(phase = ScanPhase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val rawText = textRecognizer.recognize(uri)
                val draft = parser.parse(rawText)
                _uiState.update { it.copy(phase = ScanPhase.REVIEW, draft = draft, error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(phase = ScanPhase.CAPTURE, error = e.message ?: "No se pudo leer la imagen")
                }
            }
        }
    }

    fun updateMerchant(value: String) = editDraft { it.copy(merchant = value) }
    fun updateCurrency(value: String) = editDraft { it.copy(currency = value) }
    fun updateDate(millis: Long?) = editDraft { it.copy(dateMillis = millis) }
    fun updateTotal(value: Double?) = editDraft { it.copy(total = value) }

    fun addItem() = editDraft { it.copy(items = it.items + ReceiptItem(name = "")) }

    fun updateItem(index: Int, item: ReceiptItem) = editDraft { draft ->
        draft.copy(items = draft.items.toMutableList().also { it[index] = item })
    }

    fun removeItem(index: Int) = editDraft { draft ->
        draft.copy(items = draft.items.toMutableList().also { it.removeAt(index) })
    }

    /** Persists the current draft and moves to the [ScanPhase.SAVED] state. */
    fun save() {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                // The user edited the draft, so attribute the saved record to manual review.
                val receipt = draft.copy(source = draft.source.takeIf { it == ParseSource.GEMINI }
                    ?: ParseSource.MANUAL).toReceipt()
                val id = repository.save(receipt)
                _uiState.update { it.copy(isSaving = false, phase = ScanPhase.SAVED, savedId = id) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "No se pudo guardar el recibo")
                }
            }
        }
    }

    /** Clears state to start a fresh scan. */
    fun reset() {
        _uiState.value = ScanUiState()
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private inline fun editDraft(transform: (ReceiptDraft) -> ReceiptDraft) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = transform(draft))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonicalApplication
                val c: AppContainer = app.container
                return ScanViewModel(
                    textRecognizer = c.textRecognizer,
                    parser = c.parser,
                    repository = c.receiptRepository,
                    authManager = c.authManager,
                ) as T
            }
        }
    }
}
