package com.quman.app.util

import java.util.UUID

enum class NotificationType {
    MONEY_SENT,      // Red (Outgoing: Lacag la diray)
    MONEY_RECEIVED,  // Green (Incoming: Lacag la helay)
    OTHER            // Yellow (Other notifications / Ogeysiis)
}

data class ParsedSmsNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val provider: String,
    val amount: Double?,
    val currency: String = "$",
    val counterparty: String?,
    val counterpartyPhone: String?,
    val balanceAfter: Double?,
    val title: String,
    val message: String,
    val rawBody: String,
    val sender: String,
    val isAd: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsTransactionParser {

    private val amountRegex = Regex("""(?:\$|USD\s*)\s*([0-9]+(?:\.[0-9]{1,2})?)|([0-9]+(?:\.[0-9]{1,2})?)\s*(?:\$|USD)""", RegexOption.IGNORE_CASE)
    private val balanceRegex = Regex("""(?:haraaga(?:agu|aga)?|balance)(?:\s+cusub)?\s*(?:waa|is)?\s*\$?([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("""\b(252[0-9]{9}|[0-9]{9})\b""")

    private val adKeywords = listOf(
        "la soo deg", "kala soo deg", "download", "app-ka", "waafi", "ku guuleyso",
        "fursad", "hadiyad", "ku shubo", "dalbo", "offer", "discount", "free",
        "xirmo", "bundle", "ogaysiis", "xayeysiin", "promotional", "campaign",
        "macmiil ku guuleyso", "kordhi fursadaada", "qiimo dhimis", "tartanka",
        "fadlan la xiriir", "adeeg cusub"
    )

    fun parse(sender: String, body: String, timestamp: Long = System.currentTimeMillis()): ParsedSmsNotification {
        val lower = body.lowercase()

        // 1. Detect provider
        val provider = when {
            lower.contains("evcplus") || lower.contains("evc plus") || lower.contains("evc") || sender.contains("EVC", ignoreCase = true) || sender.contains("Hormuud", ignoreCase = true) -> "EVC Plus"
            lower.contains("zaad") || sender.contains("ZAAD", ignoreCase = true) || sender.contains("Telesom", ignoreCase = true) -> "ZAAD"
            lower.contains("sahal") || sender.contains("Sahal", ignoreCase = true) || sender.contains("Golis", ignoreCase = true) -> "Sahal"
            lower.contains("edahab") || lower.contains("e-dahab") || sender.contains("eDahab", ignoreCase = true) || sender.contains("Somtel", ignoreCase = true) -> "eDahab"
            else -> sender.ifBlank { "Quman" }
        }

        // 2. Extract amount
        var amount: Double? = null
        val amountMatch = amountRegex.find(body)
        if (amountMatch != null) {
            val amountStr = amountMatch.groupValues[1].ifEmpty { amountMatch.groupValues[2] }
            amount = amountStr.toDoubleOrNull()
        }

        // 3. Extract balance after
        var balanceAfter: Double? = null
        val balanceMatch = balanceRegex.find(body)
        if (balanceMatch != null) {
            balanceAfter = balanceMatch.groupValues[1].toDoubleOrNull()
        }

        // 4. Extract counterparty phone
        val phoneMatch = phoneRegex.find(body)
        val counterpartyPhone = phoneMatch?.value

        // 5. Determine direction / type
        val isIncoming = lower.contains("ka heshay") ||
                lower.contains("laguu soo wareejiyay") ||
                lower.contains("laguu soo diray") ||
                lower.contains("ku soo dhacday") ||
                lower.contains("ayaad heshay") ||
                lower.contains("received from") ||
                lower.contains("credited with") ||
                lower.contains("ku shubtay")

        val isOutgoing = lower.contains("u wareejisay") ||
                lower.contains("u dirtay") ||
                lower.contains("u bixisay") ||
                lower.contains("ayaad dirtay") ||
                lower.contains("ayaad wareejisay") ||
                lower.contains("laguu jaray") ||
                lower.contains("bixisay") ||
                lower.contains("sent to") ||
                lower.contains("transfer to") ||
                lower.contains("debited")

        // 6. Check if promotional or non-transaction telecom SMS (192, 898, etc.)
        val hasAdKeyword = adKeywords.any { lower.contains(it) }
        val isTelecomSender = sender.contains("192") || sender.contains("898") ||
                sender.equals("hormuud", ignoreCase = true) ||
                sender.equals("telesom", ignoreCase = true) ||
                sender.equals("somtel", ignoreCase = true) ||
                sender.equals("golis", ignoreCase = true)

        val isAd = hasAdKeyword || (isTelecomSender && (!isIncoming && !isOutgoing))

        val type: NotificationType
        val title: String
        val message: String

        if (!isAd && isIncoming && amount != null) {
            type = NotificationType.MONEY_RECEIVED
            val formattedAmount = String.format("%.2f", amount)
            title = "Lacag La Helay (Received)"
            message = if (counterpartyPhone != null) {
                "Waxaad heshay $$formattedAmount ka timid $counterpartyPhone ($provider)"
            } else {
                "Waxaad heshay $$formattedAmount ($provider)"
            }
        } else if (!isAd && isOutgoing && amount != null) {
            type = NotificationType.MONEY_SENT
            val formattedAmount = String.format("%.2f", amount)
            title = "Lacag La Diray (Sent)"
            message = if (counterpartyPhone != null) {
                "Waxaad dirtay $$formattedAmount ku socota $counterpartyPhone ($provider)"
            } else {
                "Waxaad dirtay $$formattedAmount ($provider)"
            }
        } else {
            type = NotificationType.OTHER
            title = if (isAd) "Xayeysiin ($provider)" else "Ogeysiis (Notification)"
            message = if (body.length > 90) body.take(87) + "..." else body
        }

        return ParsedSmsNotification(
            type = type,
            provider = provider,
            amount = if (isAd) null else amount,
            counterparty = counterpartyPhone,
            counterpartyPhone = counterpartyPhone,
            balanceAfter = balanceAfter,
            title = title,
            message = message,
            rawBody = body,
            sender = sender,
            isAd = isAd,
            timestamp = timestamp
        )
    }
}
