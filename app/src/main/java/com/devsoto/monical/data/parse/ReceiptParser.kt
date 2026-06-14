package com.devsoto.monical.data.parse

import com.devsoto.monical.data.model.ReceiptDraft

/**
 * Turns the raw OCR text of a receipt into a structured [ReceiptDraft].
 *
 * Implementations: [GeminiReceiptParser] (online, LLM-based) and [RegexReceiptParser]
 * (offline fallback). [ReceiptParserRouter] picks between them.
 */
interface ReceiptParser {
    /**
     * @throws Exception if parsing fails (e.g. no network for the Gemini parser). Callers
     * such as [ReceiptParserRouter] are expected to handle failures and fall back.
     */
    suspend fun parse(rawText: String): ReceiptDraft
}
