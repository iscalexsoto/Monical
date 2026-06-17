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
    /**
     * The return share frozen onto this receipt when it was archived (RETURNED). `null` while
     * PENDING — pending receipts use the current global share. See [DEFAULT_RETURN_SHARE].
     */
    val returnShare: Double? = null,
)

/**
 * Amount to be returned for this receipt at the given [share] — non-zero only while
 * [ReturnStatus.PENDING]. Used by the UI for live (pending) display; archived amounts are frozen
 * via [Receipt.returnShare] in `SummaryMath`.
 */
fun Receipt.returnAmount(share: Double): Double =
    if (returnStatus == ReturnStatus.PENDING) round2((total ?: 0.0) * share) else 0.0
