package com.devsoto.monical.data.model

/** Aggregated numbers for one archived month ("yyyy-MM" bucket). */
data class MonthlyRollup(
    val count: Int = 0,
    val total: Double = 0.0,
    val refund: Double = 0.0,
)

/**
 * The Home dashboard, stored as a single denormalized document (`users/{uid}/meta/summary`).
 * Reading it costs one Firestore read regardless of how many receipts exist:
 *
 * - [pendingTotal] is the running sum over the pending receipts. The pending *refund* is derived at
 *   display time as `pendingTotal * currentShare`, so changing the configured share is reflected live.
 * - [active] holds a [ReceiptCard] per pending receipt (the list Home renders).
 * - [archivedMonthly] keeps only aggregate numbers for returned/"no devolver" receipts; their
 *   full documents live in the cold `archive` collection, read 1-at-a-time on demand. Their
 *   [MonthlyRollup.refund] is frozen at the share in effect when each receipt was archived.
 *
 * Kept and maintained by the pure helpers in `SummaryMath`; the repository only does the IO.
 */
data class ReceiptSummary(
    val pendingTotal: Double = 0.0,
    val active: List<ReceiptCard> = emptyList(),
    val archivedMonthly: Map<String, MonthlyRollup> = emptyMap(),
    val updatedAt: Long = 0L,
)
