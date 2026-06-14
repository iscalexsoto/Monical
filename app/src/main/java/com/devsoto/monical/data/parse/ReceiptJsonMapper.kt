package com.devsoto.monical.data.parse

import com.devsoto.monical.data.model.ParseSource
import com.devsoto.monical.data.model.ReceiptDraft
import com.devsoto.monical.data.model.ReceiptItem
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Maps the JSON object returned by Gemini into a [ReceiptDraft].
 *
 * Kept separate from [GeminiReceiptParser] (which depends on Firebase) so the mapping logic
 * can be unit-tested on the JVM with plain JSON strings. Tolerant of missing/null fields and
 * of dates that don't match the expected ISO format.
 */
object ReceiptJsonMapper {

    private val isoDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun fromJson(
        json: String,
        rawText: String,
        source: ParseSource = ParseSource.GEMINI,
    ): ReceiptDraft {
        val obj = JSONObject(json)
        return ReceiptDraft(
            merchant = obj.optStringOrNull("merchant"),
            dateMillis = obj.optStringOrNull("date")?.let { parseIsoDate(it) },
            total = obj.optDoubleOrNull("total"),
            currency = obj.optStringOrNull("currency"),
            items = parseItems(obj),
            rawText = rawText,
            source = source,
        )
    }

    private fun parseItems(obj: JSONObject): List<ReceiptItem> {
        val array = obj.optJSONArray("items") ?: return emptyList()
        val items = mutableListOf<ReceiptItem>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optStringOrNull("name") ?: continue
            items.add(
                ReceiptItem(
                    name = name,
                    quantity = item.optDoubleOrNull("quantity"),
                    unitPrice = item.optDoubleOrNull("unitPrice"),
                    lineTotal = item.optDoubleOrNull("lineTotal"),
                )
            )
        }
        return items
    }

    private fun parseIsoDate(value: String): Long? = try {
        LocalDate.parse(value.trim(), isoDate)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val d = optDouble(key, Double.NaN)
        return if (d.isNaN()) null else d
    }
}
