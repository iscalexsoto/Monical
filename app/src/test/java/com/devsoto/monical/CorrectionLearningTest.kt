package com.devsoto.monical

import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.refine.learnCorrections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionLearningTest {

    @Test
    fun learns_merchant_keyed_by_normalized_raw() {
        val learned = learnCorrections(
            raw = ReceiptDraft(merchant = "Oxx0"),
            edited = ReceiptDraft(merchant = "Oxxo"),
        )
        assertEquals(mapOf("oxx0" to "Oxxo"), learned.merchants)
        assertTrue(learned.items.isEmpty())
    }

    @Test
    fun learns_item_by_position_skipping_adjustment() {
        val raw = ReceiptDraft(items = listOf(ReceiptItem(name = "Coca col4")))
        val edited = ReceiptDraft(
            items = listOf(
                ReceiptItem(name = "Coca Cola"),
                ReceiptItem(name = "Ajuste", isAdjustment = true),
            )
        )
        val learned = learnCorrections(raw, edited)
        assertEquals(mapOf("coca col4" to "Coca Cola"), learned.items)
    }

    @Test
    fun does_not_learn_when_unchanged() {
        val learned = learnCorrections(
            raw = ReceiptDraft(merchant = "Oxxo"),
            edited = ReceiptDraft(merchant = "Oxxo"),
        )
        assertTrue(learned.isEmpty())
    }

    @Test
    fun does_not_learn_when_raw_or_final_blank() {
        assertTrue(
            learnCorrections(ReceiptDraft(merchant = ""), ReceiptDraft(merchant = "Oxxo")).isEmpty()
        )
        assertTrue(
            learnCorrections(ReceiptDraft(merchant = "Oxx0"), ReceiptDraft(merchant = "  ")).isEmpty()
        )
    }
}
