package com.devsoto.monical.data.refine

import com.devsoto.monical.data.model.ReceiptDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Which field a [FieldCorrection] touched, so the review screen can highlight it. */
sealed interface CorrectionField {
    data object Merchant : CorrectionField
    data object Date : CorrectionField
    data class Item(val index: Int) : CorrectionField
}

/** A single auto-applied correction: what it was, and what it became. */
data class FieldCorrection(
    val field: CorrectionField,
    val original: String,
    val corrected: String,
)

/** Result of post-processing: the corrected draft plus the list of changes made. */
data class RefinedDraft(
    val draft: ReceiptDraft,
    val changes: List<FieldCorrection> = emptyList(),
)

/**
 * Runs after the parser (Gemini/regex) and before the review screen. Applies learned
 * [CorrectionDictionary] entries (merchant + item names) and a conservative future-year date
 * rule. Corrections are applied automatically but reported in [RefinedDraft.changes] so the UI can
 * highlight them and offer a revert. Pure/JVM, no Android deps — mirrors the parser design note.
 */
class ReceiptPostProcessor {

    fun process(
        parsed: ReceiptDraft,
        dict: CorrectionDictionary,
        today: LocalDate = LocalDate.now(),
    ): RefinedDraft {
        val changes = mutableListOf<FieldCorrection>()

        // 1. Merchant ─ learned alias (e.g. "Oxx0" → "Oxxo").
        var merchant = parsed.merchant
        dict.merchantFor(merchant)?.let { fixed ->
            if (fixed != merchant) {
                changes += FieldCorrection(CorrectionField.Merchant, merchant.orEmpty(), fixed)
                merchant = fixed
            }
        }

        // 2. Item names ─ learned aliases, skipping the locked adjustment row.
        val items = parsed.items.mapIndexed { index, item ->
            if (item.isAdjustment) return@mapIndexed item
            val fixed = dict.itemFor(item.name)
            if (fixed != null && fixed != item.name) {
                changes += FieldCorrection(CorrectionField.Item(index), item.name, fixed)
                item.copy(name = fixed)
            } else {
                item
            }
        }

        // 3. Future-year date ─ a year past the current one is almost certainly an OCR slip.
        var dateMillis = parsed.dateMillis
        if (dateMillis != null) {
            val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            if (date.year > today.year) {
                val fixed = date.withYear(today.year) // adjusts Feb-29 → Feb-28 when needed
                changes += FieldCorrection(CorrectionField.Date, date.toString(), fixed.toString())
                dateMillis = fixed.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        }

        return RefinedDraft(
            parsed.copy(merchant = merchant, items = items, dateMillis = dateMillis),
            changes,
        )
    }
}
