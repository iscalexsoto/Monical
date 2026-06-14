package com.devsoto.monical

import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.parse.ReceiptJsonMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReceiptJsonMapperTest {

    @Test
    fun `maps full gemini json into draft`() {
        val json = """
            {
              "merchant": "Cafe Central",
              "date": "2024-09-15",
              "total": 8.70,
              "currency": "MXN",
              "items": [
                { "name": "Cafe", "quantity": 2, "unitPrice": 2.0, "lineTotal": 4.0 },
                { "name": "Pan", "quantity": null, "unitPrice": null, "lineTotal": 4.7 }
              ]
            }
        """.trimIndent()

        val draft = ReceiptJsonMapper.fromJson(json, rawText = "raw")

        assertEquals("Cafe Central", draft.merchant)
        assertEquals("MXN", draft.currency)
        assertEquals(8.70, draft.total!!, 0.001)
        assertEquals(ParseSource.GEMINI, draft.source)
        assertEquals("raw", draft.rawText)

        val expectedDate = LocalDate.of(2024, 9, 15)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expectedDate, draft.dateMillis)

        assertEquals(2, draft.items.size)
        assertEquals("Cafe", draft.items[0].name)
        assertEquals(2.0, draft.items[0].quantity!!, 0.001)
        assertNull(draft.items[1].quantity)
    }

    @Test
    fun `tolerates nulls and missing fields`() {
        val json = """{ "merchant": null, "total": null }"""
        val draft = ReceiptJsonMapper.fromJson(json, rawText = "")
        assertNull(draft.merchant)
        assertNull(draft.total)
        assertNull(draft.dateMillis)
        assertEquals(0, draft.items.size)
    }

    @Test
    fun `invalid date string maps to null`() {
        val json = """{ "date": "ayer" }"""
        val draft = ReceiptJsonMapper.fromJson(json, rawText = "")
        assertNull(draft.dateMillis)
    }
}
