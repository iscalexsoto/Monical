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
 * [CorrectionDictionary] entries (merchant + item names) and a date-sanity rule (no future dates,
 * nothing older than last year; otherwise reset to today). Corrections are applied automatically
 * but reported in [RefinedDraft.changes] so the UI can
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

        // 3. Date sanity ─ a ticket records a recent purchase, so the date must be realistic: not in
        //    the future, and no older than last year. Anything else (or a missing date) becomes today.
        val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        var dateMillis = parsed.dateMillis
        if (dateMillis == null) {
            dateMillis = todayMillis // default fill, not a correction → no change recorded
        } else {
            val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            val valid = !date.isAfter(today) && date.year >= today.year - 1
            if (!valid) {
                changes += FieldCorrection(CorrectionField.Date, date.toString(), today.toString())
                dateMillis = todayMillis
            }
        }

        return RefinedDraft(
            parsed.copy(merchant = merchant, items = items, dateMillis = dateMillis),
            changes,
        )
    }
}
