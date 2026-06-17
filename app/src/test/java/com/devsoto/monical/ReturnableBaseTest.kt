package com.devsoto.monical

import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.model.reconcileItems
import com.devsoto.monical.data.model.returnableBase
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnableBaseTest {

    private fun item(name: String, amount: Double, returnable: Boolean = true) =
        ReceiptItem(name = name, lineTotal = amount, returnable = returnable)

    @Test
    fun all_items_returnable_equals_total() {
        val items = listOf(item("A", 60.0), item("B", 40.0))
        assertEquals(100.0, returnableBase(100.0, items), 1e-9)
    }

    @Test
    fun partial_selection_is_proportional() {
        // total 90, A=60 B=40 (sum 100, so a -10 discount); keep only A → 90 * 60/100 = 54
        val items = reconcileItems(90.0, listOf(item("A", 60.0), item("B", 40.0, returnable = false)))
        assertEquals(54.0, returnableBase(90.0, items), 1e-9)
    }

    @Test
    fun global_discount_is_spread_across_kept_items() {
        // Items list at 100 but total is 90 (a 10% global discount). Returning only A (list 60)
        // refunds its discounted price: 54, not 60.
        val items = listOf(item("A", 60.0), item("B", 40.0, returnable = false))
        val withDiscount = reconcileItems(90.0, items)
        assertEquals(54.0, returnableBase(90.0, withDiscount), 1e-9)
    }

    @Test
    fun nothing_selected_is_zero() {
        val items = listOf(item("A", 60.0, returnable = false), item("B", 40.0, returnable = false))
        assertEquals(0.0, returnableBase(100.0, items), 1e-9)
    }

    @Test
    fun no_items_falls_back_to_total() {
        assertEquals(100.0, returnableBase(100.0, emptyList()), 1e-9)
    }

    @Test
    fun null_total_uses_selected_sum() {
        val items = listOf(item("A", 60.0), item("B", 40.0, returnable = false))
        assertEquals(60.0, returnableBase(null, items), 1e-9)
    }

    @Test
    fun zero_item_sum_is_zero() {
        val items = listOf(item("A", 0.0), item("B", 0.0))
        assertEquals(0.0, returnableBase(100.0, items), 1e-9)
    }

    @Test
    fun adjustment_row_is_ignored_in_the_base() {
        // The reconciled discount line must not double-count; base is over real items only.
        val items = reconcileItems(90.0, listOf(item("A", 60.0), item("B", 40.0)))
        assertEquals(90.0, returnableBase(90.0, items), 1e-9)
    }
}
