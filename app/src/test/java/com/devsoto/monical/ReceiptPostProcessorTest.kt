package com.devsoto.monical

import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.refine.CorrectionDictionary
import com.devsoto.monical.data.refine.CorrectionField
import com.devsoto.monical.data.refine.ReceiptPostProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReceiptPostProcessorTest {

    private val processor = ReceiptPostProcessor()
    private val today = LocalDate.of(2026, 6, 16)

    private fun millis(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun applies_learned_merchant_alias() {
        val dict = CorrectionDictionary(merchants = mapOf("oxx0" to "Oxxo"))
        val result = processor.process(ReceiptDraft(merchant = "Oxx0"), dict, today)
        assertEquals("Oxxo", result.draft.merchant)
        assertEquals(1, result.changes.size)
        assertEquals(CorrectionField.Merchant, result.changes.first().field)
    }

    @Test
    fun applies_learned_item_alias_and_skips_adjustment() {
        val dict = CorrectionDictionary(items = mapOf("coca col4" to "Coca Cola"))
        val draft = ReceiptDraft(
            items = listOf(
                ReceiptItem(name = "Coca col4"),
                ReceiptItem(name = "Ajuste", isAdjustment = true),
            )
        )
        val result = processor.process(draft, dict, today)
        assertEquals("Coca Cola", result.draft.items[0].name)
        assertEquals("Ajuste", result.draft.items[1].name)
        assertEquals(CorrectionField.Item(0), result.changes.single().field)
    }

    @Test
    fun corrects_future_year_to_current() {
        val draft = ReceiptDraft(dateMillis = millis(2028, 3, 10))
        val result = processor.process(draft, CorrectionDictionary.EMPTY, today)
        assertEquals(millis(2026, 3, 10), result.draft.dateMillis)
        assertEquals(CorrectionField.Date, result.changes.single().field)
    }

    @Test
    fun leaves_non_future_date_untouched() {
        val draft = ReceiptDraft(dateMillis = millis(2026, 1, 5))
        val result = processor.process(draft, CorrectionDictionary.EMPTY, today)
        assertEquals(millis(2026, 1, 5), result.draft.dateMillis)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun empty_dictionary_makes_no_changes() {
        val draft = ReceiptDraft(merchant = "Oxx0", items = listOf(ReceiptItem(name = "Pan")))
        val result = processor.process(draft, CorrectionDictionary.EMPTY, today)
        assertEquals("Oxx0", result.draft.merchant)
        assertTrue(result.changes.isEmpty())
    }
}
