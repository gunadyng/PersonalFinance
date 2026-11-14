package com.personalfinance.tracker.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moneyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun money(amount: Double): String = "$" + moneyFormat.format(amount)

fun monthLabel(m: YearMonth): String = m.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

fun dayLabel(iso: String): String {
    val d = LocalDate.parse(iso)
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }
}

/** Parses "12.5", "$12.50" or "1,234.5" into a positive amount rounded to cents, or null. */
fun parseAmount(raw: String): Double? {
    val cleaned = raw.replace(Regex("[$,\\s]"), "")
    if (cleaned.isEmpty() || cleaned == "." || !cleaned.matches(Regex("\\d*\\.?\\d*"))) return null
    val n = Math.round(cleaned.toDouble() * 100) / 100.0
    return if (n > 0 && n < 1e10) n else null
}

fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
