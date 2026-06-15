package com.devsoto.monical.data.model

/**
 * A single line item on a receipt. All numeric fields are nullable because OCR/parsing
 * may not always recover them.
 *
 * [isAdjustment] marks the auto-generated reconciliation row (see [reconcileItems]); such a
 * row is locked in the UI and its [lineTotal] is recomputed, never edited by hand.
 */
data class ReceiptItem(
    val name: String,
    val quantity: Double? = null,
    val unitPrice: Double? = null,
    val lineTotal: Double? = null,
    val isAdjustment: Boolean = false,
)
