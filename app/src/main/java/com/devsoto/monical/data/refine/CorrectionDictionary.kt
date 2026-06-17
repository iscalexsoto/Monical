package com.devsoto.monical.data.refine

/**
 * Learned corrections for parser output, keyed by a normalized form of the raw OCR/Gemini token
 * (see [normalizeKey]) and mapping to the canonical text the user confirmed. Persisted as the
 * single `users/{uid}/meta/corrections` document and cached in memory so the post-processor never
 * hits Firestore per scan.
 */
data class CorrectionDictionary(
    val merchants: Map<String, String> = emptyMap(),
    val items: Map<String, String> = emptyMap(),
) {
    /** Canonical merchant for a raw value, or null when nothing was learned. */
    fun merchantFor(raw: String?): String? = raw?.let { merchants[normalizeKey(it)] }

    /** Canonical item name for a raw value, or null when nothing was learned. */
    fun itemFor(raw: String?): String? = raw?.let { items[normalizeKey(it)] }

    /** Merge newly learned entries on top of the current dictionary. */
    operator fun plus(learned: LearnedCorrections): CorrectionDictionary = copy(
        merchants = merchants + learned.merchants,
        items = items + learned.items,
    )

    companion object {
        val EMPTY = CorrectionDictionary()
    }
}

/** Lowercase, trim, and collapse internal whitespace — tolerant lookup/learning key. */
fun normalizeKey(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")
