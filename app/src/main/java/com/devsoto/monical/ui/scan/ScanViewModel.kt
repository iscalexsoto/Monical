package com.devsoto.monical.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.devsoto.monical.AppContainer
import com.devsoto.monical.MonicalApplication
import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.reconcileItems
import com.devsoto.monical.data.ocr.MlKitTextRecognizer
import com.devsoto.monical.data.parse.ReceiptParser
import com.devsoto.monical.data.refine.CorrectionDictionary
import com.devsoto.monical.data.refine.ReceiptPostProcessor
import com.devsoto.monical.data.refine.learnCorrections
import com.devsoto.monical.data.repository.CorrectionRepository
import com.devsoto.monical.data.repository.ReceiptRepository
import com.devsoto.monical.data.repository.SettingsRepository
import com.devsoto.monical.ui.review.ReviewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    private val correctionRepository: CorrectionRepository,
    private val postProcessor: ReceiptPostProcessor,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /** Learned corrections, loaded once and kept in memory so scans don't read Firestore. */
    private var dictionary: CorrectionDictionary = CorrectionDictionary.EMPTY

    init {
        viewModelScope.launch {
            dictionary = runCatching { correctionRepository.load() }.getOrDefault(CorrectionDictionary.EMPTY)
        }
        viewModelScope.launch {
            settingsRepository.observe()
                .catch { /* keep the default share on error */ }
                .collect { cfg -> _uiState.update { it.copy(returnShare = cfg.returnShare) } }
        }
    }

    // ── Flow entry points ────────────────────────────────────
    /** FAB → Manual: open a blank draft in the review form. */
    fun startManual() {
        val draft = ReceiptDraft.blank()
        _uiState.value = ScanUiState(phase = ScanPhase.REVIEW, mode = ReviewMode.MANUAL, draft = draft)
    }

    /** Tap a receipt card on Home: fetch its full document (one read) and open it for editing. */
    fun editReceipt(id: String, archived: Boolean = false) {
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                val receipt = repository.getReceipt(id, archived) ?: return@launch
                val draft = ReceiptDraft.fromReceipt(receipt).reconciled()
                _uiState.value = ScanUiState(phase = ScanPhase.REVIEW, mode = ReviewMode.EDIT, draft = draft)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "No se pudo abrir el recibo") }
            }
        }
    }

    /** Runs OCR + parsing on the picked image, post-processes it, then moves to review. */
    fun processImage(uri: Uri) {
        _uiState.update { it.copy(phase = ScanPhase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val rawText = textRecognizer.recognize(uri)
                val parsed = parser.parse(rawText)
                val refined = postProcessor.process(parsed, dictionary, LocalDate.now())
                _uiState.update {
                    it.copy(
                        phase = ScanPhase.REVIEW,
                        mode = ReviewMode.SCAN,
                        draft = refined.draft.reconciled(),
                        rawParsedDraft = parsed,
                        corrections = refined.changes,
                        error = null,
                    )
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
        val state = _uiState.value
        val draft = state.draft ?: return
        val rawParsed = state.rawParsedDraft
        val isScan = state.mode == ReviewMode.SCAN
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val source = draft.source.takeIf { it == ParseSource.GEMINI } ?: ParseSource.MANUAL
                repository.save(draft.copy(source = source).toReceipt())
                if (isScan && rawParsed != null) learnFrom(rawParsed, draft)
                reset()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "No se pudo guardar el recibo")
                }
            }
        }
    }

    /** Learns the user's edits (raw parser value → confirmed value) for next time. Best-effort. */
    private suspend fun learnFrom(rawParsed: ReceiptDraft, finalDraft: ReceiptDraft) {
        val learned = learnCorrections(rawParsed, finalDraft)
        if (learned.isEmpty()) return
        runCatching {
            correctionRepository.learn(learned)
            dictionary += learned
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
                    correctionRepository = c.correctionRepository,
                    postProcessor = c.postProcessor,
                    settingsRepository = c.settingsRepository,
                ) as T
            }
        }
    }
}
