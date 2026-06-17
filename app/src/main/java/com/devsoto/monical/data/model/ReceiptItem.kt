package com.devsoto.monical.data.model

/**
 * A single line item on a receipt. All numeric fields are nullable because OCR/parsing
 * may not always recover them.
 *
 * [isAdjustment] marks the auto-generated reconciliation row (see [reconcileItems]); such a
 * row is locked in the UI and its [lineTotal] is recomputed, never edited by hand.
 *
 * [returnable] marks whether this line is included in the devolución when the receipt is PENDING
 * (see [returnableBase]). Defaults to `true` so legacy items (and the adjustment row, which ignores
 * the flag) count as included.
 */
data class ReceiptItem(
    val name: String,
    val quantity: Double? = null,
    val unitPrice: Double? = null,
    val lineTotal: Double? = null,
    val isAdjustment: Boolean = false,
    val returnable: Boolean = true,
)
