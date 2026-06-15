package com.devsoto.monical.ui.review

/** How the review/edit form was reached, used to title it and toggle delete. */
enum class ReviewMode(val title: String, val sub: String) {
    SCAN("Revisar gasto", "Extraído del ticket · corrige lo que falte"),
    MANUAL("Nuevo gasto", "Captura manual"),
    EDIT("Editar gasto", "Modifica y guarda"),
}
