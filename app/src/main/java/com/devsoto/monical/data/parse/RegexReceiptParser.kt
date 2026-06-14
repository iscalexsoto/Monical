package com.devsoto.monical.data.parse

import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Offline, best-effort receipt parser used when Gemini is unreachable.
 *
 * Pure Kotlin/JVM (no Android dependencies) so it can be unit-tested directly. It scans the
 * OCR text line by line with heuristics for the total amount, the purchase date, and item
 * lines that end in a price. Results are intentionally conservative — anything it can't
 * recover stays null for the user to fill in on the review screen.
 */
class RegexReceiptParser : ReceiptParser {

    override suspend fun parse(rawText: String): ReceiptDraft {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ReceiptDraft(
            dateMillis = findDate(lines),
            total = findTotal(lines),
            items = findItems(lines),
            rawText = rawText,
            source = ParseSource.REGEX,
        )
    }

    /**
     * Finds the total. Prefers lines that mention "total" (but not "subtotal"); among those,
     * picks the largest amount, since the grand total is usually the biggest. Falls back to
     * the single largest amount anywhere if no "total" line is found.
     */
    private fun findTotal(lines: List<String>): Double? {
        val totalLineAmounts = lines
            .filter { line ->
                val lower = line.lowercase()
                "total" in lower && "subtotal" !in lower && "sub total" !in lower
            }
            .flatMap { amountsIn(it) }

        if (totalLineAmounts.isNotEmpty()) return totalLineAmounts.max()

        return lines.flatMap { amountsIn(it) }.maxOrNull()
    }

    /** Finds the first date matching any supported format. */
    private fun findDate(lines: List<String>): Long? {
        for (line in lines) {
            for ((regex, formatters) in DATE_PATTERNS) {
                val match = regex.find(line) ?: continue
                for (formatter in formatters) {
                    try {
                        val date = LocalDate.parse(match.value, formatter)
                        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        // try next formatter
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts item lines: those ending in a price token whose label isn't a known summary
     * keyword (total, tax, change, etc.). The trailing amount becomes [ReceiptItem.lineTotal].
     */
    private fun findItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        for (line in lines) {
            val lower = line.lowercase()
            if (SUMMARY_KEYWORDS.any { it in lower }) continue

            val match = TRAILING_AMOUNT.find(line) ?: continue
            val name = line.substring(0, match.range.first).trim().trimEnd('.', '-', ':', '*')
            if (name.length < 2) continue

            val price = parseAmount(match.groupValues[1]) ?: continue
            items.add(ReceiptItem(name = name, lineTotal = price))
        }
        return items
    }

    /** All money-looking tokens in a line, as parsed doubles. */
    private fun amountsIn(line: String): List<Double> =
        AMOUNT.findAll(line).mapNotNull { parseAmount(it.value) }.toList()

    companion object {
        // A money token: optional thousands separators and a 1-2 char decimal part.
        // Examples matched: 12, 12.50, 1,234.56, 1.234,56, 12,50
        private val AMOUNT = Regex("""\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?""")

        // A money token anchored to the end of the line (typical for item rows).
        private val TRAILING_AMOUNT = Regex("""(\d{1,3}(?:[.,]\d{3})*[.,]\d{1,2}|\d+[.,]\d{1,2})\s*$""")

        private val SUMMARY_KEYWORDS = listOf(
            "total", "subtotal", "sub total", "tax", "iva", "vat", "change", "cambio",
            "cash", "efectivo", "card", "tarjeta", "tip", "propina", "balance", "due",
        )

        // Each date regex maps to the formatters to try, in order.
        private val DATE_PATTERNS: List<Pair<Regex, List<DateTimeFormatter>>> = listOf(
            // 2024-09-15
            Regex("""\b\d{4}-\d{2}-\d{2}\b""") to listOf(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            // 15/09/2024 or 15-09-2024 or 15.09.2024 (day-first, common outside the US)
            Regex("""\b\d{1,2}[/.\-]\d{1,2}[/.\-]\d{4}\b""") to listOf(
                DateTimeFormatter.ofPattern("d/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d.MM.yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            ),
            // 15/09/24 (2-digit year)
            Regex("""\b\d{1,2}[/.\-]\d{1,2}[/.\-]\d{2}\b""") to listOf(
                DateTimeFormatter.ofPattern("d/MM/yy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("dd.MM.yy"),
            ),
        )

        /**
         * Parses a single money token into a Double, handling both `1,234.56` (dot decimal)
         * and `1.234,56` (comma decimal) conventions.
         */
        fun parseAmount(raw: String): Double? {
            val token = raw.trim()
            if (token.isEmpty()) return null

            val lastDot = token.lastIndexOf('.')
            val lastComma = token.lastIndexOf(',')

            val normalized = when {
                lastDot >= 0 && lastComma >= 0 -> {
                    // The right-most separator is the decimal point; the other groups thousands.
                    if (lastComma > lastDot) {
                        token.replace(".", "").replace(',', '.')
                    } else {
                        token.replace(",", "")
                    }
                }
                lastComma >= 0 -> {
                    // Only commas: treat as decimal if it looks like one (1-2 trailing digits),
                    // otherwise as a thousands separator.
                    val decimals = token.length - lastComma - 1
                    if (decimals in 1..2) token.replace(',', '.') else token.replace(",", "")
                }
                lastDot >= 0 -> {
                    val decimals = token.length - lastDot - 1
                    if (decimals in 1..2) token else token.replace(".", "")
                }
                else -> token
            }
            return normalized.toDoubleOrNull()
        }
    }
}
