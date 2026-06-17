package com.devsoto.monical.data.model

/** Name of the auto-generated reconciliation line item when the difference is positive. */
const val ADJUSTMENT_NAME = "Ajuste"

/** Name of the auto-generated reconciliation line item when the difference is negative (a global
 *  discount the items don't itemize). */
const val DISCOUNT_NAME = "Descuento"

/**
 * Reconciles line items against the authoritative [total] ("total manda").
 *
 * Keeps the non-adjustment items as-is and appends (or updates) a single, locked reconciliation
 * item carrying the difference `total − sum(non-adjustment line totals)`:
 *  - total > sum → positive adjustment, named [ADJUSTMENT_NAME]
 *  - total < sum → negative adjustment (global discount), named [DISCOUNT_NAME]
 *  - total == sum, or [total] is null, or there are no items → no adjustment row
 *
 * The reconciliation row is always flagged [ReceiptItem.isAdjustment] (detection never relies on the
 * name). The result always satisfies the invariant: `sum(all lineTotals, incl. adjustment) == total`.
 */
fun reconcileItems(total: Double?, items: List<ReceiptItem>): List<ReceiptItem> {
    val base = items.filter { !it.isAdjustment }
    if (total == null) return base

    val sum = base.sumOf { it.lineTotal ?: 0.0 }
    val diff = round2(total - sum)
    if (diff == 0.0) return base

    val name = if (diff < 0) DISCOUNT_NAME else ADJUSTMENT_NAME
    return base + ReceiptItem(name = name, lineTotal = diff, isAdjustment = true)
}

/**
 * Portion of the ticket eligible for devolución, spreading the adjustment/discount proportionally
 * across the items marked [ReceiptItem.returnable]. With every item marked this equals [total], so
 * receipts without a partial selection behave exactly as before.
 */
fun returnableBase(total: Double?, items: List<ReceiptItem>): Double {
    val base = items.filter { !it.isAdjustment }
    val allSum = base.sumOf { it.lineTotal ?: 0.0 }
    val selSum = base.filter { it.returnable }.sumOf { it.lineTotal ?: 0.0 }
    return when {
        base.isEmpty() -> total ?: 0.0     // no line items → whole total is returnable
        allSum == 0.0 -> 0.0
        else -> round2((total ?: allSum) * (selSum / allSum))
    }
}
