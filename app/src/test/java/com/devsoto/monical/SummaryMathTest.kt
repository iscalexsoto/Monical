package com.devsoto.monical

import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.ReceiptSummary
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.applyArchive
import com.devsoto.monical.data.model.applyDelete
import com.devsoto.monical.data.model.applySave
import com.devsoto.monical.data.model.buildSummary
import com.devsoto.monical.data.model.monthKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SummaryMathTest {

    private fun millis(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun applySave_adds_pending_card_and_totals() {
        val s = applySave(ReceiptSummary(), Receipt(id = "a", total = 100.0, returnStatus = ReturnStatus.PENDING))
        assertEquals(1, s.active.size)
        assertEquals(100.0, s.pendingTotal, 1e-9)
        // Pending refund is derived at display time (pendingTotal * currentShare), not stored here.
    }

    @Test
    fun applySave_is_idempotent_by_id() {
        var s = applySave(ReceiptSummary(), Receipt(id = "a", total = 100.0))
        s = applySave(s, Receipt(id = "a", total = 250.0)) // same id, edited total
        assertEquals(1, s.active.size)
        assertEquals(250.0, s.pendingTotal, 1e-9)
    }

    @Test
    fun applyArchive_moves_out_of_active_into_monthly() {
        val date = millis(2026, 6, 15)
        var s = applySave(ReceiptSummary(), Receipt(id = "a", total = 200.0, dateMillis = date))
        s = applyArchive(s, Receipt(id = "a", total = 200.0, dateMillis = date, returnStatus = ReturnStatus.RETURNED))
        assertTrue(s.active.isEmpty())
        assertEquals(0.0, s.pendingTotal, 1e-9)
        val roll = s.archivedMonthly.getValue("2026-06")
        assertEquals(1, roll.count)
        assertEquals(200.0, roll.total, 1e-9)
        assertEquals(150.0, roll.refund, 1e-9) // returned share counted
    }

    @Test
    fun applyArchive_freezes_refund_at_the_receipts_own_share() {
        val date = millis(2026, 6, 15)
        // Stamped with an 80% share → refund is frozen at 80, not the 75% default.
        val s = applyArchive(
            ReceiptSummary(),
            Receipt(id = "a", total = 100.0, dateMillis = date, returnStatus = ReturnStatus.RETURNED, returnShare = 0.80),
        )
        assertEquals(80.0, s.archivedMonthly.getValue("2026-06").refund, 1e-9)
    }

    @Test
    fun applyArchive_freezes_partial_item_selection() {
        val date = millis(2026, 6, 15)
        // total 100, items A=60 (kept) + B=40 (deselected). Returnable base = 60 → refund 60 * 0.75 = 45.
        val receipt = Receipt(
            id = "a", total = 100.0, dateMillis = date, returnStatus = ReturnStatus.RETURNED,
            returnShare = 0.75,
            items = listOf(
                ReceiptItem(name = "A", lineTotal = 60.0, returnable = true),
                ReceiptItem(name = "B", lineTotal = 40.0, returnable = false),
            ),
        )
        val s = applyArchive(ReceiptSummary(), receipt)
        val roll = s.archivedMonthly.getValue("2026-06")
        assertEquals(100.0, roll.total, 1e-9)
        assertEquals(45.0, roll.refund, 1e-9)
    }

    @Test
    fun buildSummary_splits_pending_and_archived() {
        val s = buildSummary(
            listOf(
                Receipt(id = "p", total = 100.0, returnStatus = ReturnStatus.PENDING, dateMillis = millis(2026, 6, 1)),
                Receipt(id = "r", total = 80.0, returnStatus = ReturnStatus.RETURNED, dateMillis = millis(2026, 5, 1)),
                Receipt(id = "n", total = 40.0, returnStatus = ReturnStatus.NONE, dateMillis = millis(2026, 5, 20)),
            )
        )
        assertEquals(1, s.active.size)
        assertEquals(100.0, s.pendingTotal, 1e-9)
        val may = s.archivedMonthly.getValue("2026-05")
        assertEquals(2, may.count)
        assertEquals(120.0, may.total, 1e-9)
        assertEquals(60.0, may.refund, 1e-9) // only the RETURNED 80 → 60; NONE adds 0
    }

    @Test
    fun applyDelete_removes_from_active() {
        var s = applySave(ReceiptSummary(), Receipt(id = "a", total = 100.0))
        s = applyDelete(s, Receipt(id = "a", total = 100.0))
        assertTrue(s.active.isEmpty())
        assertEquals(0.0, s.pendingTotal, 1e-9)
    }

    @Test
    fun applyDelete_removes_archived_month_when_empty() {
        val date = millis(2026, 6, 15)
        var s = applyArchive(ReceiptSummary(), Receipt(id = "a", total = 200.0, dateMillis = date, returnStatus = ReturnStatus.RETURNED))
        s = applyDelete(s, Receipt(id = "a", total = 200.0, dateMillis = date, returnStatus = ReturnStatus.RETURNED))
        assertNull(s.archivedMonthly["2026-06"])
    }

    @Test
    fun monthKey_uses_date_then_falls_back_to_createdAt() {
        assertEquals("2026-06", monthKey(millis(2026, 6, 30), createdAt = millis(2020, 1, 1)))
        assertEquals("2020-01", monthKey(null, createdAt = millis(2020, 1, 1)))
    }
}
