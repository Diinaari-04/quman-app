package com.quman.app.util

import java.util.Locale

object CurrencyUtils {
    /**
     * Formats currency according to Quman guidelines:
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

    /**
     * Formats running balance accurately:
     * - Positive: $4.21
     * - Negative: -$1.15
     */
    fun formatBalance(balance: Double): String {
        val absVal = kotlin.math.abs(balance)
        val formattedNumber = String.format(Locale.US, "%.2f", absVal)
        return if (balance < 0) "-$$formattedNumber" else "$$formattedNumber"
    }
}
