package com.devsoto.monical.data.model

/**
 * A single line item on a receipt. All numeric fields are nullable because OCR/parsing
 * may not always recover them.
 */
data class ReceiptItem(
    val name: String,
    val quantity: Double? = null,
    val unitPrice: Double? = null,
    val lineTotal: Double? = null,
)
