package com.devsoto.monical.data.model

/**
 * Devolución status of a receipt. The amount to be returned is computed by
 * [returnAmount]; only [PENDING] receipts count toward the pending total.
 *
 * [label]/[mark] are user-facing (Spanish, receipt-style teller marks).
 */
enum class ReturnStatus(val label: String, val mark: String) {
    PENDING("Por Devolver", "※"),
    RETURNED("Devuelto", "✓"),
    NONE("No Devolver", "—"),
}
