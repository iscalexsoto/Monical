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
import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.reconcileItems
import com.devsoto.monical.data.ocr.MlKitTextRecognizer
import com.devsoto.monical.data.parse.ReceiptParser
import com.devsoto.monical.data.repository.ReceiptRepository
import com.devsoto.monical.ui.review.ReviewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the capture → review → save flow and is shared between the capture and review screens so
 * the [ReceiptDraft] survives navigation without serializing it through nav args.
 *
 * "Total manda": every edit that affects the sum is followed by [reconcileItems] so a single locked
 * "Ajuste" line keeps `sum(items) == total`.
 */
class ScanViewModel(
    private val textRecognizer: MlKitTextRecognizer,
    private val parser: ReceiptParser,
    private val repository: ReceiptRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // ── Flow entry points ────────────────────────────────────
    /** FAB → Escanear: go to the capture screen. */
    fun startScan() {
        _uiState.value = ScanUiState(phase = ScanPhase.CAPTURE, mode = ReviewMode.SCAN)
    }

    /** FAB → Manual: open a blank draft in the review form. */
    fun startManual() {
        val draft = ReceiptDraft.blank()
        _uiState.value = ScanUiState(phase = ScanPhase.REVIEW, mode = ReviewMode.MANUAL, draft = draft)
    }

    /** Tap a receipt on Home: open it for editing. */
    fun editReceipt(receipt: Receipt) {
        val draft = ReceiptDraft.fromReceipt(receipt).reconciled()
        _uiState.value = ScanUiState(phase = ScanPhase.REVIEW, mode = ReviewMode.EDIT, draft = draft)
    }

    /** Runs OCR + parsing on the picked image, then moves to review. */
    fun processImage(uri: Uri) {
        _uiState.update { it.copy(phase = ScanPhase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val rawText = textRecognizer.recognize(uri)
                val draft = parser.parse(rawText).reconciled()
                _uiState.update {
                    it.copy(phase = ScanPhase.REVIEW, mode = ReviewMode.SCAN, draft = draft, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(phase = ScanPhase.CAPTURE, error = e.message ?: "No se pudo leer la imagen")
                }
            }
        }
    }

    // ── Draft editors ────────────────────────────────────────
    fun updateMerchant(value: String) = editDraft { it.copy(merchant = value) }
    fun updateCurrency(value: String) = editDraft { it.copy(currency = value) }
    fun updateDate(millis: Long?) = editDraft { it.copy(dateMillis = millis) }
    fun updateTotal(value: Double?) = editDraft { it.copy(total = value) }
    fun updateCategory(value: String) = editDraft { it.copy(category = value) }
    fun updateReturnStatus(value: ReturnStatus) = editDraft { it.copy(returnStatus = value) }

    fun addItem() = editDraft { it.copy(items = it.items + ReceiptItem(name = "")) }

    fun updateItem(index: Int, item: ReceiptItem) = editDraft { draft ->
        draft.copy(items = draft.items.toMutableList().also { it[index] = item })
    }

    fun removeItem(index: Int) = editDraft { draft ->
        draft.copy(items = draft.items.toMutableList().also { it.removeAt(index) })
    }

    /** Persists the current draft and returns Home. Upserts when the draft already has an id. */
    fun save() {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val source = draft.source.takeIf { it == ParseSource.GEMINI } ?: ParseSource.MANUAL
                repository.save(draft.copy(source = source).toReceipt())
                reset()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "No se pudo guardar el recibo")
                }
            }
        }
    }

    /** Deletes the receipt being edited (if persisted) and returns Home. */
    fun deleteCurrent() {
        val id = _uiState.value.draft?.id
        viewModelScope.launch {
            try {
                if (!id.isNullOrBlank()) repository.delete(id)
                reset()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "No se pudo eliminar el recibo") }
            }
        }
    }

    /** Clears state back to Home. */
    fun reset() {
        _uiState.value = ScanUiState()
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun ReceiptDraft.reconciled(): ReceiptDraft =
        copy(items = reconcileItems(total, items))

    private inline fun editDraft(transform: (ReceiptDraft) -> ReceiptDraft) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = transform(draft).reconciled())
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
