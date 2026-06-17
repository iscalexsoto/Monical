package com.devsoto.monical

import com.devsoto.monical.ui.components.applyPercentKey
import org.junit.Assert.assertEquals
import org.junit.Test

class PercentageInputTest {

    @Test
    fun appends_digits() {
        assertEquals("7", applyPercentKey("0", "7"))
        assertEquals("75", applyPercentKey("7", "5"))
    }

    @Test
    fun replaces_leading_zero() {
        assertEquals("5", applyPercentKey("0", "5"))
    }

    @Test
    fun clamps_to_100() {
        // "10" + "5" would be 105 → ignored, buffer stays.
        assertEquals("10", applyPercentKey("10", "5"))
        // Exactly 100 is allowed.
        assertEquals("100", applyPercentKey("10", "0"))
    }

    @Test
    fun clear_resets_to_zero() {
        assertEquals("0", applyPercentKey("75", "C"))
    }

    @Test
    fun backspace_drops_last_digit_then_floors_at_zero() {
        assertEquals("7", applyPercentKey("75", "⌫"))
        assertEquals("0", applyPercentKey("7", "⌫"))
        assertEquals("0", applyPercentKey("0", "⌫"))
    }
}
