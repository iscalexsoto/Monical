package com.devsoto.monical

import com.devsoto.monical.ui.components.evalExpr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorEvalTest {

    @Test
    fun honors_multiplication_over_addition() {
        assertEquals(14.0, evalExpr("2+3×4")!!, 1e-9)
    }

    @Test
    fun honors_division_over_subtraction() {
        assertEquals(8.0, evalExpr("10−4÷2")!!, 1e-9)
    }

    @Test
    fun rounds_result_to_two_decimals() {
        assertEquals(3.33, evalExpr("10÷3")!!, 1e-9)
    }

    @Test
    fun trailing_operator_is_ignored() {
        assertEquals(5.0, evalExpr("5+")!!, 1e-9)
    }

    @Test
    fun blank_is_null() {
        assertNull(evalExpr("   "))
    }
}
