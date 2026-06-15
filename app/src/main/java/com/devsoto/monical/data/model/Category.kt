package com.devsoto.monical.data.model

/**
 * A spending category. [id] is the user-facing label (Spanish) and also the stored value;
 * [code] is the 3-letter teller code shown in the receipt-style chips.
 */
data class Category(val id: String, val code: String)

/** Default category id used when none is chosen. */
const val UNCATEGORIZED = "Sin categoría"

val CATEGORIES: List<Category> = listOf(
    Category("Comida", "COM"),
    Category("Servicios", "SRV"),
    Category("Compras", "CMP"),
    Category("Personal", "PER"),
    Category("Transporte", "TRA"),
    Category(UNCATEGORIZED, "—"),
)

/** Teller code for a category id, or "—" when unknown. */
fun categoryCode(id: String): String =
    CATEGORIES.firstOrNull { it.id == id }?.code ?: "—"
