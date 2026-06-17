package com.devsoto.monical.ui.scan

import com.devsoto.monical.data.model.DEFAULT_RETURN_SHARE
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.refine.FieldCorrection
import com.devsoto.monical.ui.review.ReviewMode

/** High-level phase of the flow, used to drive navigation. */
enum class ScanPhase { IDLE, CAPTURE, PROCESSING, REVIEW }

/**
 * UI state shared by the capture and review screens.
 *
 * @property phase current step of the flow ([ScanPhase.IDLE] = Home).
 * @property mode how the review form was reached.
 * @property draft the extracted/edited receipt, available once parsing succeeds or editing starts.
 * @property rawParsedDraft the parser output before post-processing; the diff key-set for learning.
 * @property corrections auto-applied post-processing changes, surfaced for highlighting in review.
 * @property isSaving true while a Firestore write is in flight.
 * @property returnShare current configured return share, for the live "Devolución" preview.
 * @property error user-facing error message, if any.
 */
data class ScanUiState(
    val phase: ScanPhase = ScanPhase.IDLE,
    val mode: ReviewMode = ReviewMode.SCAN,
    val draft: ReceiptDraft? = null,
    val rawParsedDraft: ReceiptDraft? = null,
    val corrections: List<FieldCorrection> = emptyList(),
    val isSaving: Boolean = false,
    val returnShare: Double = DEFAULT_RETURN_SHARE,
    val error: String? = null,
)
