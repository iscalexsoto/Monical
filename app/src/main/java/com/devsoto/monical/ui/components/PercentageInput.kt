package com.devsoto.monical.ui.components

/**
 * Pure keypad reducer for the integer percentage editor. Kept Android-free so it's JVM-testable.
 * [current] is the digit buffer ("0"–"100"); [key] is "0".."9", "C" (clear) or "⌫" (backspace).
 * Always returns a string parseable as an Int in 0..100.
 */
fun applyPercentKey(current: String, key: String): String = when {
    key == "C" -> "0"
    key == "⌫" -> if (current.length <= 1) "0" else current.dropLast(1)
    key.length == 1 && key[0].isDigit() -> {
        val appended = if (current == "0") key else current + key
        // Clamp to 100: extra digits are ignored once the value would exceed the max.
        if ((appended.toIntOrNull() ?: Int.MAX_VALUE) > 100) current else appended
    }
    else -> current
}
