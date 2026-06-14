package com.devsoto.monical.data.parse

import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.ReceiptDraft
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig

/**
 * Online parser that asks Gemini to structure the raw OCR text into JSON.
 *
 * Uses the Firebase AI Logic SDK (Gemini Developer API backend) so no API key ships in the
 * app — calls are authenticated via the Firebase project and protected by App Check. The
 * response is constrained to JSON via [generationConfig] and mapped by [ReceiptJsonMapper].
 *
 * A network failure (offline) surfaces as a thrown exception; [ReceiptParserRouter] catches
 * it and falls back to [RegexReceiptParser].
 */
class GeminiReceiptParser(
    private val model: GenerativeModel = defaultModel(),
) : ReceiptParser {

    override suspend fun parse(rawText: String): ReceiptDraft {
        val response = model.generateContent(buildPrompt(rawText))
        val json = response.text?.let(::extractJson)
            ?: throw IllegalStateException("Gemini returned no text")
        return ReceiptJsonMapper.fromJson(json, rawText = rawText, source = ParseSource.GEMINI)
    }

    private fun buildPrompt(rawText: String): String = """
        You are a receipt parsing engine. Extract structured data from the OCR text of a
        purchase receipt below and return ONLY a JSON object, with no markdown fences and no
        commentary.

        Use exactly this shape (use null when a value is not present; date as ISO yyyy-MM-dd):
        {
          "merchant": string | null,
          "date": "yyyy-MM-dd" | null,
          "total": number | null,
          "currency": string | null,   // ISO 4217 code if identifiable, e.g. "MXN", "USD"
          "items": [
            { "name": string, "quantity": number | null, "unitPrice": number | null, "lineTotal": number | null }
          ]
        }

        Rules:
        - "total" is the final grand total actually paid, not the subtotal.
        - Amounts are plain numbers (no currency symbols, dot as decimal separator).
        - Omit non-product lines (taxes, change, payment method) from "items".

        OCR TEXT:
        ${'"'}${'"'}${'"'}
        $rawText
        ${'"'}${'"'}${'"'}
    """.trimIndent()

    /** Strips ```json fences if the model wrapped its output despite instructions. */
    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start in 0 until end) trimmed.substring(start, end + 1) else trimmed
    }

    companion object {
        const val MODEL_NAME = "gemini-2.5-flash"

        fun defaultModel(): GenerativeModel =
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                },
            )
    }
}
