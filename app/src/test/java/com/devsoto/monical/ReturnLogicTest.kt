package com.devsoto.monical

import com.devsoto.monical.data.model.Receipt
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.returnAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnLogicTest {

    @Test
    fun pending_returns_configured_share() {
        assertEquals(75.0, Receipt(total = 100.0, returnStatus = ReturnStatus.PENDING).returnAmount(0.75), 1e-9)
    }

    @Test
    fun pending_honors_a_different_share() {
        assertEquals(80.0, Receipt(total = 100.0, returnStatus = ReturnStatus.PENDING).returnAmount(0.80), 1e-9)
    }

    @Test
    fun pending_rounds_to_two_decimals() {
        // 432.50 * 0.75 = 324.375 → 324.38
        assertEquals(324.38, Receipt(total = 432.50, returnStatus = ReturnStatus.PENDING).returnAmount(0.75), 1e-9)
    }

    @Test
    fun returned_is_zero() {
        assertEquals(0.0, Receipt(total = 100.0, returnStatus = ReturnStatus.RETURNED).returnAmount(0.75), 1e-9)
    }

    @Test
    fun none_is_zero() {
        assertEquals(0.0, Receipt(total = 100.0, returnStatus = ReturnStatus.NONE).returnAmount(0.75), 1e-9)
    }

    @Test
    fun null_total_is_zero() {
        assertEquals(0.0, Receipt(total = null, returnStatus = ReturnStatus.PENDING).returnAmount(0.75), 1e-9)
    }
}
