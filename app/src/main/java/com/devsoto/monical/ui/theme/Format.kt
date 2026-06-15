package com.devsoto.monical.ui.theme

import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** es-MX / MXN formatting and date helpers, ported from the design prototype. */
private val esMX = Locale("es", "MX")

private val currencyFmt = NumberFormat.getCurrencyInstance(esMX).apply {
    minimumFractionDigits = 2
}
private val plainFmt = NumberFormat.getNumberInstance(esMX).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun fmt(n: Double): String = currencyFmt.format(n)
fun fmtPlain(n: Double): String = plainFmt.format(n)

// ── Dates ────────────────────────────────────────────────────
val TODAY: LocalDate get() = LocalDate.now()
val YESTERDAY: LocalDate get() = LocalDate.now().minusDays(1)

private val DOW = listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")
private val MON_UP = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
private val MON_LO = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
val MON_FULL = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

fun isToday(d: LocalDate) = d == TODAY
fun isYesterday(d: LocalDate) = d == YESTERDAY

// java.time DayOfWeek: Mon=1..Sun=7. value % 7 maps Sun→0..Sat→6.
private fun dowIndex(d: LocalDate) = d.dayOfWeek.value % 7

fun fmtShort(d: LocalDate): String =                                   // 14 JUN
    "%02d %s".format(d.dayOfMonth, MON_UP[d.monthValue - 1])

fun fmtLong(d: LocalDate): String =                                    // vie 14 jun 2026
    "${DOW[dowIndex(d)]} ${d.dayOfMonth} ${MON_LO[d.monthValue - 1]} ${d.year}"

fun fmtNumeric(d: LocalDate): String =                                 // 14/06/2026
    "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)

// ── millis ↔ LocalDate (UTC, consistent with the OCR/Gemini date mapper) ──
fun Long.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
