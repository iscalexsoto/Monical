package com.devsoto.monical.data.model

/** Name of the auto-generated reconciliation line item. */
const val ADJUSTMENT_NAME = "Ajuste"

/**
 * Reconciles line items against the authoritative [total] ("total manda").
 *
 * Keeps the non-adjustment items as-is and appends (or updates) a single, locked
 * [ADJUSTMENT_NAME] item carrying the difference `total − sum(non-adjustment line totals)`:
 *  - total > sum → positive adjustment
 *  - total < sum → negative adjustment
 *  - total == sum, or [total] is null, or there are no items → no adjustment row
 *
 * The result always satisfies the invariant: `sum(all lineTotals, incl. adjustment) == total`.
 */
fun reconcileItems(total: Double?, items: List<ReceiptItem>): List<ReceiptItem> {
    val base = items.filter { !it.isAdjustment }
    if (total == null) return base

    val sum = base.sumOf { it.lineTotal ?: 0.0 }
    val diff = round2(total - sum)
    if (diff == 0.0) return base

    return base + ReceiptItem(name = ADJUSTMENT_NAME, lineTotal = diff, isAdjustment = true)
}
