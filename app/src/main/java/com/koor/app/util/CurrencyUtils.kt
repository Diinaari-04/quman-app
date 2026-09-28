package com.koor.app.util

import java.util.Locale

object CurrencyUtils {
    /**
     * Formats currency according to Koor guidelines:
     * - Money out: -$5.00
     * - Money in: +$5.00
     * - Neutral: $5.00
     */
    fun formatAmount(amount: Double, direction: String? = null): String {
        val absVal = kotlin.math.abs(amount)
        val formattedNumber = String.format(Locale.US, "%.2f", absVal)
        return when (direction?.lowercase()) {
            "out", "expense" -> "-$$formattedNumber"
            "in", "income" -> "+$$formattedNumber"
            else -> {
                if (amount < 0) "-$$formattedNumber" else "$$formattedNumber"
            }
        }
    }
}
