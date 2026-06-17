package com.devsoto.monical.data.model

/**
 * Lightweight projection of a [Receipt] for the Home dashboard. Carries only what the list
 * needs to render — no `items` or `rawText` — so the whole active list fits inside the single
 * `meta/summary` document and opening Home costs a single Firestore read.
 */
data class ReceiptCard(
    val id: String = "",
    val merchant: String? = null,
    val dateMillis: Long? = null,
    val total: Double? = null,
    val currency: String? = null,
    val category: String = UNCATEGORIZED,
    val returnStatus: ReturnStatus = ReturnStatus.PENDING,
)

/** Project a full [Receipt] down to the home-list card. */
fun Receipt.toCard(): ReceiptCard = ReceiptCard(
    id = id,
    merchant = merchant,
    dateMillis = dateMillis,
    total = total,
    currency = currency,
    category = category,
    returnStatus = returnStatus,
)

/** Amount to be returned for this card at the given [share] — non-zero only while PENDING. */
fun ReceiptCard.returnAmount(share: Double): Double =
    if (returnStatus == ReturnStatus.PENDING) round2((total ?: 0.0) * share) else 0.0
