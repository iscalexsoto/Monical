package com.devsoto.monical.data.refine

import com.devsoto.monical.data.model.ReceiptDraft

/** New dictionary entries learned from one review, ready to merge/persist. */
data class LearnedCorrections(
    val merchants: Map<String, String> = emptyMap(),
    val items: Map<String, String> = emptyMap(),
) {
    fun isEmpty(): Boolean = merchants.isEmpty() && items.isEmpty()
}

/**
 * Diffs the raw parser output ([raw], whose values become dictionary *keys*) against the user's
 * final [edited] draft. Any field the user changed to a non-blank value yields a learned mapping
 * `normalizeKey(raw) -> edited`. Keying off the raw token (not what was shown after a prior
 * correction) keeps learning idempotent and lets a later edit update an existing entry.
 *
 * Items are matched positionally, skipping the locked adjustment row on both sides.
 */
fun learnCorrections(raw: ReceiptDraft, edited: ReceiptDraft): LearnedCorrections {
    val merchants = mutableMapOf<String, String>()
    val items = mutableMapOf<String, String>()

    val rawMerchant = raw.merchant?.trim().orEmpty()
    val finalMerchant = edited.merchant?.trim().orEmpty()
    if (rawMerchant.isNotEmpty() && finalMerchant.isNotEmpty() && rawMerchant != finalMerchant) {
        merchants[normalizeKey(rawMerchant)] = finalMerchant
    }

    val rawItems = raw.items.filterNot { it.isAdjustment }
    val finalItems = edited.items.filterNot { it.isAdjustment }
    rawItems.forEachIndexed { index, rawItem ->
        val finalItem = finalItems.getOrNull(index) ?: return@forEachIndexed
        val rawName = rawItem.name.trim()
        val finalName = finalItem.name.trim()
        if (rawName.isNotEmpty() && finalName.isNotEmpty() && rawName != finalName) {
            items[normalizeKey(rawName)] = finalName
        }
    }

    return LearnedCorrections(merchants, items)
}
