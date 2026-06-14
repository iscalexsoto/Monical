package com.devsoto.monical.data.parse

import android.util.Log
import com.devsoto.monical.data.model.ReceiptDraft

/**
 * Routes parsing to [GeminiReceiptParser] first and falls back to [RegexReceiptParser] when
 * the online parser fails (no network, quota, malformed response, etc.).
 *
 * This is the entry point the UI layer should depend on.
 */
class ReceiptParserRouter(
    private val online: ReceiptParser = GeminiReceiptParser(),
    private val offline: ReceiptParser = RegexReceiptParser(),
) : ReceiptParser {

    override suspend fun parse(rawText: String): ReceiptDraft {
        if (rawText.isBlank()) return ReceiptDraft.fromRawText(rawText)
        return try {
            online.parse(rawText)
        } catch (e: Exception) {
            Log.w(TAG, "Online parsing failed, falling back to regex", e)
            offline.parse(rawText)
        }
    }

    private companion object {
        const val TAG = "ReceiptParserRouter"
    }
}
