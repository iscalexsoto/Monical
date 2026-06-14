package com.devsoto.monical.ui.scan

import com.devsoto.monical.data.model.ReceiptDraft

/** High-level phase of the scan flow, used to drive navigation. */
enum class ScanPhase { CAPTURE, PROCESSING, REVIEW, SAVED }

/**
 * UI state shared by the capture and review screens.
 *
 * @property phase current step of the flow.
 * @property draft the extracted/edited receipt, available once parsing succeeds.
 * @property isSaving true while a Firestore write is in flight.
 * @property savedId id of the saved document, set after a successful save.
 * @property error user-facing error message, if any.
 */
data class ScanUiState(
    val phase: ScanPhase = ScanPhase.CAPTURE,
    val draft: ReceiptDraft? = null,
    val isSaving: Boolean = false,
    val savedId: String? = null,
    val error: String? = null,
)
