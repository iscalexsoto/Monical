package com.devsoto.monical

import com.devsoto.monical.data.model.ADJUSTMENT_NAME
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.reconcileItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationTest {

    private fun item(name: String, amount: Double?) = ReceiptItem(name = name, lineTotal = amount)

    @Test
    fun total_greater_than_sum_adds_positive_adjustment() {
        val result = reconcileItems(100.0, listOf(item("A", 60.0)))
        assertEquals(2, result.size)
        val adj = result.last()
        assertTrue(adj.isAdjustment)
        assertEquals(ADJUSTMENT_NAME, adj.name)
        assertEquals(40.0, adj.lineTotal!!, 1e-9)
    }

    @Test
    fun total_less_than_sum_adds_negative_adjustment() {
        val result = reconcileItems(100.0, listOf(item("A", 60.0), item("B", 60.0)))
        assertEquals(3, result.size)
        assertEquals(-20.0, result.last().lineTotal!!, 1e-9)
    }

    @Test
    fun total_equals_sum_has_no_adjustment() {
        val result = reconcileItems(100.0, listOf(item("A", 100.0)))
        assertEquals(1, result.size)
        assertTrue(result.none { it.isAdjustment })
    }

    @Test
    fun null_total_drops_existing_adjustment() {
        val withAdj = listOf(item("A", 60.0), ReceiptItem(ADJUSTMENT_NAME, lineTotal = 40.0, isAdjustment = true))
        val result = reconcileItems(null, withAdj)
        assertEquals(1, result.size)
        assertTrue(result.none { it.isAdjustment })
    }

    @Test
    fun does_not_duplicate_adjustment_and_recomputes() {
        // An existing (stale) adjustment is replaced by a freshly computed one.
        val stale = listOf(item("A", 60.0), ReceiptItem(ADJUSTMENT_NAME, lineTotal = 999.0, isAdjustment = true))
        val result = reconcileItems(100.0, stale)
        assertEquals(2, result.size)
        assertEquals(1, result.count { it.isAdjustment })
        assertEquals(40.0, result.last().lineTotal!!, 1e-9)
    }

    @Test
    fun editing_an_item_recomputes_adjustment() {
        val first = reconcileItems(100.0, listOf(item("A", 60.0)))
        assertEquals(40.0, first.last().lineTotal!!, 1e-9)
        // Simulate the item amount changing to 70 (non-adjustment items carried forward).
        val edited = reconcileItems(100.0, listOf(item("A", 70.0)))
        assertEquals(30.0, edited.last().lineTotal!!, 1e-9)
    }
}
