package com.devsoto.monical

import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.parse.RegexReceiptParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RegexReceiptParserTest {

    private val parser = RegexReceiptParser()

    private val sampleReceipt = """
        SUPER MERCADO XYZ
        Av. Siempre Viva 123
        Fecha: 15/09/2024

        Leche 1L        2.50
        Pan integral    1.80
        Huevos x12      3.20
        Subtotal        7.50
        IVA             1.20
        TOTAL           8.70

        Gracias por su compra
    """.trimIndent()

    @Test
    fun `extracts grand total preferring total line over subtotal`() = runBlocking {
        val draft = parser.parse(sampleReceipt)
        assertEquals(8.70, draft.total!!, 0.001)
        assertEquals(ParseSource.REGEX, draft.source)
    }

    @Test
    fun `extracts day-first date as UTC start of day`() = runBlocking {
        val draft = parser.parse(sampleReceipt)
        val expected = LocalDate.of(2024, 9, 15)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, draft.dateMillis)
    }

    @Test
    fun `extracts item lines and skips summary lines`() = runBlocking {
        val draft = parser.parse(sampleReceipt)
        val names = draft.items.map { it.name }
        assertTrue(names.contains("Leche 1L"))
        assertTrue(names.contains("Pan integral"))
        assertTrue(names.contains("Huevos x12"))
        // Summary lines must not be treated as products.
        assertTrue(names.none { it.contains("Subtotal", ignoreCase = true) })
        assertTrue(names.none { it.contains("TOTAL", ignoreCase = true) })
        assertTrue(names.none { it.contains("IVA", ignoreCase = true) })

        val leche = draft.items.first { it.name == "Leche 1L" }
        assertEquals(2.50, leche.lineTotal!!, 0.001)
    }

    @Test
    fun `blank text yields empty draft`() = runBlocking {
        val draft = parser.parse("")
        assertNull(draft.total)
        assertNull(draft.dateMillis)
        assertTrue(draft.items.isEmpty())
    }

    @Test
    fun `parseAmount handles dot and comma decimal conventions`() {
        assertEquals(1234.56, RegexReceiptParser.parseAmount("1,234.56")!!, 0.001)
        assertEquals(1234.56, RegexReceiptParser.parseAmount("1.234,56")!!, 0.001)
        assertEquals(12.50, RegexReceiptParser.parseAmount("12,50")!!, 0.001)
        assertEquals(12.50, RegexReceiptParser.parseAmount("12.50")!!, 0.001)
        assertEquals(12.0, RegexReceiptParser.parseAmount("12")!!, 0.001)
        // 3-digit group with no decimals is treated as a thousands separator.
        assertEquals(1234.0, RegexReceiptParser.parseAmount("1.234")!!, 0.001)
        assertNotNull(RegexReceiptParser.parseAmount("8.70"))
    }
}
