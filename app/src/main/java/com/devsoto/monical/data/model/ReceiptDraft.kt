package com.devsoto.monical.data.model

/**
 * Editable, pre-persistence representation of a parsed receipt.
 *
 * Fields are kept as nullable/typed values (not Strings) so the review screen can bind
 * them to inputs while preserving what the parser actually recovered. A draft is produced
 * by a [com.devsoto.monical.data.parse.ReceiptParser] and converted to a [Receipt] via
 * [toReceipt] once the user confirms.
 */
data class ReceiptDraft(
    val merchant: String? = null,
    val dateMillis: Long? = null,
    val total: Double? = null,
    val currency: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val rawText: String = "",
    val source: ParseSource = ParseSource.MANUAL,
) {
    fun toReceipt(): Receipt = Receipt(
        merchant = merchant?.takeIf { it.isNotBlank() },
        dateMillis = dateMillis,
        total = total,
        currency = currency?.takeIf { it.isNotBlank() },
        items = items,
        rawText = rawText,
        source = source,
    )

    companion object {
        /** An empty draft carrying only the OCR text, used when nothing could be parsed. */
        fun fromRawText(rawText: String, source: ParseSource = ParseSource.MANUAL): ReceiptDraft =
            ReceiptDraft(rawText = rawText, source = source)
    }
}
