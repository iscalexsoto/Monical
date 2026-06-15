package com.devsoto.monical.data.model

/**
 * How the receipt's structured data was produced. Useful for debugging quality
 * and for telling apart Gemini-parsed receipts from the offline regex fallback.
 */
enum class ParseSource {
    GEMINI,
    REGEX,
    MANUAL,
}

/**
 * A finalized receipt ready to be persisted to Firestore. Built from a confirmed
 * [ReceiptDraft] after the user reviews/edits the extracted data.
 *
 * Field names match the Firestore document keys (see [com.devsoto.monical.data.repository.FirestoreReceiptRepository]).
 */
data class Receipt(
    val id: String = "",
    val merchant: String? = null,
    val dateMillis: Long? = null,
    val total: Double? = null,
    val currency: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val category: String = UNCATEGORIZED,
    val returnStatus: ReturnStatus = ReturnStatus.PENDING,
    val rawText: String = "",
    val source: ParseSource = ParseSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Portion of the total that is owed back (the "devolución"). */
const val RETURN_SHARE = 0.25

/** Amount to be returned for this receipt — non-zero only while [ReturnStatus.PENDING]. */
fun Receipt.returnAmount(): Double =
    if (returnStatus == ReturnStatus.PENDING) round2((total ?: 0.0) * RETURN_SHARE) else 0.0
