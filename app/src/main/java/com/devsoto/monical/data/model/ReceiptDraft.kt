package com.devsoto.monical.data.model

import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Editable, pre-persistence representation of a parsed receipt.
 *
 * Fields are kept as nullable/typed values (not Strings) so the review screen can bind
 * them to inputs while preserving what the parser actually recovered. A draft is produced
 * by a [com.devsoto.monical.data.parse.ReceiptParser] and converted to a [Receipt] via
 * [toReceipt] once the user confirms.
 */
data class ReceiptDraft(
    val id: String? = null,
    val merchant: String? = null,
    val dateMillis: Long? = null,
    val total: Double? = null,
    val currency: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val category: String = UNCATEGORIZED,
    val returnStatus: ReturnStatus = ReturnStatus.PENDING,
    val rawText: String = "",
    val source: ParseSource = ParseSource.MANUAL,
) {
    fun toReceipt(): Receipt = Receipt(
        id = id.orEmpty(),
        merchant = merchant?.takeIf { it.isNotBlank() },
        dateMillis = dateMillis,
        total = total,
        currency = currency?.takeIf { it.isNotBlank() },
        items = items,
        category = category,
        returnStatus = returnStatus,
        rawText = rawText,
        source = source,
    )

    companion object {
        /** An empty draft carrying only the OCR text, used when nothing could be parsed. */
        fun fromRawText(rawText: String, source: ParseSource = ParseSource.MANUAL): ReceiptDraft =
            ReceiptDraft(rawText = rawText, source = source)

        /** A blank draft for manual capture, dated today. */
        fun blank(dateMillis: Long? = todayMillis()): ReceiptDraft =
            ReceiptDraft(dateMillis = dateMillis, source = ParseSource.MANUAL)

        /** A draft seeded from an existing receipt, for editing. */
        fun fromReceipt(receipt: Receipt): ReceiptDraft = ReceiptDraft(
            id = receipt.id.takeIf { it.isNotBlank() },
            merchant = receipt.merchant,
            dateMillis = receipt.dateMillis,
            total = receipt.total,
            currency = receipt.currency,
            items = receipt.items,
            category = receipt.category,
            returnStatus = receipt.returnStatus,
            rawText = receipt.rawText,
            source = receipt.source,
        )

        private fun todayMillis(): Long =
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
