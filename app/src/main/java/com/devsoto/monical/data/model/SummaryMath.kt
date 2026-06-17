package com.devsoto.monical.data.model

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure transforms that keep a [ReceiptSummary] in sync with receipt mutations. The Firestore
 * repository reads the current summary inside a transaction, applies one of these, and writes it
 * back — so the dashboard never drifts from the underlying documents. Kept Android-free and
 * JVM-testable, mirroring [reconcileItems] / `ReceiptJsonMapper`.
 *
 * [ReceiptSummary.active] only ever holds PENDING cards; archived (RETURNED/NONE) receipts
 * collapse into the monthly rollups.
 */

private val MONTH_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)

/** "yyyy-MM" bucket for a receipt — by its own date when known, else by creation time. */
fun monthKey(dateMillis: Long?, createdAt: Long): String =
    MONTH_FMT.format(Instant.ofEpochMilli(dateMillis ?: createdAt))

/**
 * Refund booked into the archive for a RETURNED receipt (zero otherwise), frozen at the share that
 * was stamped onto the receipt when it was archived. Legacy receipts without a stamp fall back to
 * [DEFAULT_RETURN_SHARE] so historical totals stay correct.
 */
private fun archivedRefund(receipt: Receipt): Double =
    if (receipt.returnStatus == ReturnStatus.RETURNED)
        round2((receipt.total ?: 0.0) * (receipt.returnShare ?: DEFAULT_RETURN_SHARE))
    else 0.0

/** Rebuilds the whole summary from scratch — used for the one-time backfill/migration. */
fun buildSummary(receipts: List<Receipt>): ReceiptSummary {
    var summary = ReceiptSummary(updatedAt = System.currentTimeMillis())
    receipts.forEach { r ->
        summary = if (r.returnStatus == ReturnStatus.PENDING) applySave(summary, r) else applyArchive(summary, r)
    }
    return summary
}

/** Upsert of a PENDING receipt into the active list (idempotent by id). */
fun applySave(summary: ReceiptSummary, receipt: Receipt): ReceiptSummary =
    summary.withActive(summary.active.filterNot { it.id == receipt.id } + receipt.toCard())

/** Move a receipt out of active and fold its numbers into the month's rollup. */
fun applyArchive(summary: ReceiptSummary, receipt: Receipt): ReceiptSummary {
    val key = monthKey(receipt.dateMillis, receipt.createdAt)
    val prev = summary.archivedMonthly[key] ?: MonthlyRollup()
    val next = prev.copy(
        count = prev.count + 1,
        total = round2(prev.total + (receipt.total ?: 0.0)),
        refund = round2(prev.refund + archivedRefund(receipt)),
    )
    return summary
        .copy(archivedMonthly = summary.archivedMonthly + (key to next))
        .withActive(summary.active.filterNot { it.id == receipt.id })
}

/** Remove a receipt, whether it currently lives in active or in an archived month. */
fun applyDelete(summary: ReceiptSummary, receipt: Receipt): ReceiptSummary {
    if (summary.active.any { it.id == receipt.id }) {
        return summary.withActive(summary.active.filterNot { it.id == receipt.id })
    }
    val key = monthKey(receipt.dateMillis, receipt.createdAt)
    val prev = summary.archivedMonthly[key] ?: return summary
    val next = prev.copy(
        count = (prev.count - 1).coerceAtLeast(0),
        total = round2(prev.total - (receipt.total ?: 0.0)),
        refund = round2(prev.refund - archivedRefund(receipt)),
    )
    val monthly = if (next.count <= 0) summary.archivedMonthly - key
    else summary.archivedMonthly + (key to next)
    return summary.copy(archivedMonthly = monthly, updatedAt = System.currentTimeMillis())
}

/** Replace the active list and recompute the pending running total from it. */
private fun ReceiptSummary.withActive(active: List<ReceiptCard>): ReceiptSummary = copy(
    active = active,
    pendingTotal = round2(active.sumOf { it.total ?: 0.0 }),
    updatedAt = System.currentTimeMillis(),
)
