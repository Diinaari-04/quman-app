package com.quman.app.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class NotificationType {
    MONEY_SENT,      // Red (Outgoing: Lacag la diray)
    MONEY_RECEIVED,  // Green (Incoming: Lacag la helay)
    OTHER            // Yellow (Other notifications / Ogeysiis)
}

data class ParsedSmsNotification(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val direction: String = if (type == NotificationType.MONEY_RECEIVED) "in" else if (type == NotificationType.MONEY_SENT) "out" else "other",
    val provider: String,
    val amount: Double?,
    val currency: String = "$",
    val counterpartyName: String? = null,
    val counterpartyPhone: String? = null,
    val counterparty: String? = counterpartyName ?: counterpartyPhone,
    val balanceAfter: Double?,
    val title: String,
    val message: String,
    val rawBody: String,
    val sender: String,
    val isAd: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsTransactionParser {

    private val amountRegex = Regex("""(?:\$|USD\s*)\s*([0-9]+(?:\.[0-9]+)?)|([0-9]+(?:\.[0-9]+)?)\s*(?:\$|USD)""", RegexOption.IGNORE_CASE)
    private val balanceRegex = Regex("""(?:haraag[a-z]*|balance)\s*(?:cusub)?\s*(?:waa|is)?\s*[:\s]*\$?([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("""\b(252[0-9]{9}|0?[0-9]{9})\b""")

    // EVC Plus (192) exact patterns
    // Sent: $0.15 ayaad uwareejisay hassan muqtar mohamed(615999823) or with spaces
    private val evcSentRegex = Regex("""u\s*wareejisay\s+(.+?)\s*\(\s*([0-9]+)\s*\)""", RegexOption.IGNORE_CASE)
    // Received: waxaad $0.5 ka heshay 0613682904
    private val evcReceivedRegex = Regex("""ka\s+heshay\s+([0-9+]+)""", RegexOption.IGNORE_CASE)

    // Date extraction: Tar: 30/09/26 20:51:26 or Tar: 08/07/2026 13:00:51:768
    private val dateRegex = Regex("""Tar:\s*([0-9/]+\s+[0-9:]+)""", RegexOption.IGNORE_CASE)

    fun parse(sender: String, body: String, fallbackTimestamp: Long = System.currentTimeMillis()): ParsedSmsNotification {
        val lower = body.lowercase()
        val cleanSender = sender.trim().replace("+", "")

        val isEvcSender = cleanSender.contains("192") || lower.contains("evc") || cleanSender.contains("hormuud", ignoreCase = true)
        val isJeebSender = cleanSender.contains("898") || lower.contains("jeeb") || cleanSender.contains("somnet", ignoreCase = true)

        // 1. Detect provider
        val provider = when {
            isEvcSender -> "EVC Plus"
            isJeebSender -> "Jeeb"
            lower.contains("zaad") || cleanSender.contains("telesom", ignoreCase = true) -> "ZAAD"
            lower.contains("sahal") || cleanSender.contains("golis", ignoreCase = true) -> "Sahal"
            lower.contains("edahab") || lower.contains("e-dahab") || cleanSender.contains("somtel", ignoreCase = true) -> "eDahab"
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

        // 4. Extract occurrence date
        var occurredAt = fallbackTimestamp
        val dateMatch = dateRegex.find(body)
        if (dateMatch != null) {
            val dateStr = dateMatch.groupValues[1].trim()
            val formats = if (isJeebSender) {
                listOf(
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS", Locale.US),
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
                    SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US)
                )
            } else {
                listOf(
                    SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US),
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
                )
            }
            for (sdf in formats) {
                try {
                    val parsedDate = sdf.parse(dateStr)
                    if (parsedDate != null) {
                        occurredAt = parsedDate.time
                        break
                    }
                } catch (_: Exception) { }
            }
        }

        // 5. Determine direction & counterparty
        var counterpartyName: String? = null
        var counterpartyPhone: String? = null

        val isIncoming = lower.contains("ka heshay") ||
                lower.contains("laguu soo wareejiyay") ||
                lower.contains("laguu soo diray") ||
                lower.contains("ku soo dhacday") ||
                lower.contains("ayaad heshay") ||
                lower.contains("received from") ||
                lower.contains("credited with")

        val isOutgoing = lower.contains("uwareejisay") ||
                lower.contains("u wareejisay") ||
                lower.contains("u dirtay") ||
                lower.contains("udirtay") ||
                lower.contains("u bixisay") ||
                lower.contains("ayaad dirtay") ||
                lower.contains("ayaad wareejisay") ||
                lower.contains("laguu jaray") ||
                lower.contains("sent to") ||
                lower.contains("transfer to") ||
                lower.contains("debited")

        if (isOutgoing) {
            val sentMatch = evcSentRegex.find(body)
            if (sentMatch != null) {
                counterpartyName = sentMatch.groupValues[1].trim()
                counterpartyPhone = sentMatch.groupValues[2].trim()
            } else {
                val pMatch = phoneRegex.find(body)
                counterpartyPhone = pMatch?.value
            }
        } else if (isIncoming) {
            val incMatch = evcReceivedRegex.find(body)
            if (incMatch != null) {
                counterpartyPhone = incMatch.groupValues[1].trim()
            } else {
                val pMatch = phoneRegex.find(body)
                counterpartyPhone = pMatch?.value
            }
            // Incoming money SMS format has no name given, only phone
            counterpartyName = null
        }

        val isRealTransaction = amount != null && (isIncoming || isOutgoing)

        // Ad is strictly messages that do NOT have a real money transfer event
        val isAd = !isRealTransaction && (
                lower.contains("la soo deg") ||
                        lower.contains("waafi") ||
                        lower.contains("offer") ||
                        lower.contains("fursad") ||
                        lower.contains("hadiyad") ||
                        lower.contains("xirmo") ||
                        lower.contains("bundle") ||
                        lower.contains("qiimo dhimis") ||
                        (isEvcSender && !isRealTransaction)
                )

        val type: NotificationType
        val title: String
        val message: String

        if (isRealTransaction && isIncoming) {
            type = NotificationType.MONEY_RECEIVED
            val formattedAmount = String.format(Locale.US, "%.2f", amount)
            title = "Lacag La Helay (Received)"
            message = if (!counterpartyPhone.isNullOrBlank()) {
                "Waxaad heshay $$formattedAmount ka timid $counterpartyPhone ($provider)"
            } else {
                "Waxaad heshay $$formattedAmount ($provider)"
            }
        } else if (isRealTransaction && isOutgoing) {
            type = NotificationType.MONEY_SENT
            val formattedAmount = String.format(Locale.US, "%.2f", amount)
            title = "Lacag La Diray (Sent)"
            val target = when {
                !counterpartyName.isNullOrBlank() && !counterpartyPhone.isNullOrBlank() -> "$counterpartyName ($counterpartyPhone)"
                !counterpartyPhone.isNullOrBlank() -> counterpartyPhone
                !counterpartyName.isNullOrBlank() -> counterpartyName
                else -> ""
            }
            message = if (target.isNotBlank()) {
                "Waxaad lacag dhan $$formattedAmount u dirtay $target"
            } else {
                "Waxaad lacag dhan $$formattedAmount u dirtay ($provider)"
            }
        } else {
            type = NotificationType.OTHER
            title = if (isAd) "Xayeysiin ($provider)" else "Ogeysiis (Notification)"
            message = if (body.length > 90) body.take(87) + "..." else body
        }

        val generatedTxId = UUID.randomUUID().toString()

        return ParsedSmsNotification(
            transactionId = generatedTxId,
            type = type,
            provider = provider,
            amount = if (isAd) null else amount,
            counterpartyName = counterpartyName,
            counterpartyPhone = counterpartyPhone,
            counterparty = counterpartyName ?: counterpartyPhone,
            balanceAfter = balanceAfter,
            title = title,
            message = message,
            rawBody = body,
            sender = sender,
            isAd = isAd,
            timestamp = occurredAt
        )
    }
}
