package com.quman.app.util

object PhoneUtils {
    /**
     * Normalizes Somali phone number:
     * - Strips spaces, "+", dashes, non-digits.
     * - Strips leading zeros.
     * - If it starts with typical Somali operator prefixes (61, 62, 63, 65, 66, 67, 68, 69, 71, 77, 90, etc.)
     *   and has 9 digits, prefixes "252".
     * - Validates exactly 12 digits starting with "252".
     *
     * @return 12-digit normalized string (e.g., "252615123456") or null if invalid.
     */
    fun normalizeSomaliPhone(rawInput: String): String? {
        if (rawInput.isBlank()) return null
        
        var cleaned = rawInput.replace(Regex("[^0-9]"), "")
        while (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }

        if (cleaned.length == 9) {
            cleaned = "252$cleaned"
        }

        return if (cleaned.length == 12 && cleaned.startsWith("252")) {
            cleaned
        } else {
            null
        }
    }

    /**
     * Formats normalized 12-digit phone into human readable format: +252 61 5123456
     */
    fun formatDisplay(normalized: String): String {
        return if (normalized.length == 12 && normalized.startsWith("252")) {
            "+${normalized.substring(0, 3)} ${normalized.substring(3, 5)} ${normalized.substring(5)}"
        } else {
            normalized
        }
    }

    /**
     * Internal Supabase auth email derived from phone: "<normalizedNumber>@quman.app"
     */
    fun toInternalEmail(normalizedPhone: String): String {
        return "$normalizedPhone@quman.app"
    }
}
