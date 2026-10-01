package com.quman.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.quman.app.QumanApplication
import com.quman.app.data.local.entities.AdMessageEntity
import com.quman.app.data.local.entities.TransactionEntity
import com.quman.app.service.TransactionOverlayService
import com.quman.app.util.InAppNotificationManager
import com.quman.app.util.NotificationHelper
import com.quman.app.util.NotificationType
import com.quman.app.util.SmsTransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0]?.displayOriginatingAddress ?: ""
            val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
            val timestamp = messages[0]?.timestampMillis ?: System.currentTimeMillis()

            if (fullBody.isBlank()) return

            val cleanSender = sender.trim().replace("+", "")
            // Only parse and trigger for known money providers (192 EVC, 898 Jeeb, etc.) to prevent misfires
            val isTargetMoneySender = cleanSender.contains("192") ||
                    cleanSender.contains("898") ||
                    cleanSender.contains("EVC", ignoreCase = true) ||
                    cleanSender.contains("Jeeb", ignoreCase = true) ||
                    cleanSender.contains("Hormuud", ignoreCase = true) ||
                    cleanSender.contains("Somnet", ignoreCase = true) ||
                    cleanSender.contains("ZAAD", ignoreCase = true) ||
                    cleanSender.contains("Telesom", ignoreCase = true) ||
                    cleanSender.contains("Sahal", ignoreCase = true) ||
                    cleanSender.contains("Golis", ignoreCase = true) ||
                    cleanSender.contains("eDahab", ignoreCase = true) ||
                    cleanSender.contains("Somtel", ignoreCase = true)

            if (!isTargetMoneySender) {
                Log.d("SmsReceiver", "Ignoring SMS from non-money sender: $sender")
                return
            }

            // Parse SMS using Quman rules (Green for received, Red for sent, Yellow for other / ads)
            val parsed = SmsTransactionParser.parse(sender, fullBody, timestamp)

            // 1. Try to display system floating overlay over other apps if overlay permission is granted
            val canOverlay = Settings.canDrawOverlays(context)
            var overlayServiceStarted = false
            if (canOverlay) {
                try {
                    val overlayIntent = Intent(context, TransactionOverlayService::class.java).apply {
                        putExtra("transaction_id", parsed.transactionId)
                        putExtra("type", parsed.type.name)
                        putExtra("direction", parsed.direction)
                        putExtra("amount", parsed.amount ?: 0.0)
                        putExtra("counterparty_name", parsed.counterpartyName ?: "")
                        putExtra("counterparty_phone", parsed.counterpartyPhone ?: "")
                        putExtra("balance_after", parsed.balanceAfter ?: -1.0)
                        putExtra("title", parsed.title)
                        putExtra("message", parsed.message)
                        putExtra("provider", parsed.provider)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, overlayIntent)
                    } else {
                        context.startService(overlayIntent)
                    }
                    overlayServiceStarted = true
                } catch (e: Exception) {
                    Log.w("SmsReceiver", "Could not start overlay service: ${e.message}")
                    overlayServiceStarted = false
                }
            }

            // 2. High-priority pop-up notification with sound (serves as primary alert or fallback if overlay restricted)
            NotificationHelper.showNotification(context, parsed)

            // 3. Dispatch to in-app overlay if app is in foreground
            InAppNotificationManager.show(parsed)

            // 4. Persist data to Room Database asynchronously
            scope.launch {
                try {
                    val app = context.applicationContext as? QumanApplication ?: return@launch
                    val userId = app.authRepository.getCurrentUser()?.id
                        ?: app.userPreferences.cachedPhone.firstOrNull()
                        ?: "local_user"

                    if (parsed.isAd) {
                        // Insert promotional/ad SMS into dedicated ad_messages table
                        val adEntity = AdMessageEntity(
                            id = UUID.randomUUID().toString(),
                            sender = sender,
                            body = fullBody,
                            provider = parsed.provider,
                            occurredAt = parsed.timestamp
                        )
                        app.database.adMessageDao().insertOrUpdate(adEntity)
                    } else if (parsed.amount != null && (parsed.type == NotificationType.MONEY_SENT || parsed.type == NotificationType.MONEY_RECEIVED)) {
                        // Insert real money transaction into transactions table with categoryId = null (uncategorized)
                        val transaction = TransactionEntity(
                            id = parsed.transactionId,
                            userId = userId,
                            provider = parsed.provider,
                            sender = sender,
                            direction = parsed.direction,
                            amount = parsed.amount,
                            counterpartyName = parsed.counterpartyName,
                            counterpartyPhone = parsed.counterpartyPhone,
                            balanceAfter = parsed.balanceAfter,
                            note = parsed.title,
                            categoryId = null, // Saved uncategorized so user can categorize later
                            smsHash = fullBody.hashCode().toString(),
                            occurredAt = parsed.timestamp,
                            synced = false
                        )
                        app.database.transactionDao().insertOrUpdate(transaction)
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving transaction/ad to database", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS in SmsReceiver", e)
        }
    }
}
