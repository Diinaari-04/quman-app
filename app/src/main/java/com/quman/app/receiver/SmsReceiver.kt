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

        val pendingResult = goAsync()
        scope.launch {
            try {
                Log.d("SmsReceiver", "[Pipeline Step 1] SMS_RECEIVED intent caught by SmsReceiver")
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    Log.w("SmsReceiver", "[Pipeline Step 1] No messages extracted from intent")
                    return@launch
                }

                val sender = messages[0]?.displayOriginatingAddress ?: ""
                val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
                val timestamp = messages[0]?.timestampMillis ?: System.currentTimeMillis()

                if (fullBody.isBlank()) {
                    Log.w("SmsReceiver", "[Pipeline Step 1] Blank message body from sender: $sender")
                    return@launch
                }

                val cleanSender = sender.trim().replace("+", "")
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
                    Log.d("SmsReceiver", "[Pipeline Step 1b] Ignoring SMS from non-money sender: $sender")
                    return@launch
                }

                Log.d("SmsReceiver", "[Pipeline Step 2] Calling SmsTransactionParser for sender $sender")
                val parsed = SmsTransactionParser.parse(sender, fullBody, timestamp)
                Log.d("SmsReceiver", "[Pipeline Step 3] Parser completed: isAd=${parsed.isAd}, type=${parsed.type}, amount=${parsed.amount}, dir=${parsed.direction}")

                // Step 4: Persist data to Room Database synchronously inside goAsync (guaranteed not lost)
                try {
                    val app = context.applicationContext as? QumanApplication
                    if (app != null) {
                        val userId = app.authRepository.getCurrentUser()?.id
                            ?: app.userPreferences.cachedPhone.firstOrNull()
                            ?: "local_user"

                        if (parsed.isAd) {
                            val adEntity = AdMessageEntity(
                                id = UUID.randomUUID().toString(),
                                sender = sender,
                                body = fullBody,
                                provider = parsed.provider,
                                occurredAt = parsed.timestamp
                            )
                            app.database.adMessageDao().insertOrUpdate(adEntity)
                            Log.d("SmsReceiver", "[Pipeline Step 4] Saved promo/ad to Room ad_messages table")
                        } else if (parsed.amount != null && (parsed.type == NotificationType.MONEY_SENT || parsed.type == NotificationType.MONEY_RECEIVED)) {
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
                                categoryId = null,
                                smsHash = fullBody.hashCode().toString(),
                                occurredAt = parsed.timestamp,
                                synced = false
                            )
                            app.database.transactionDao().insertOrUpdate(transaction)
                            Log.d("SmsReceiver", "[Pipeline Step 4] Successfully saved transaction ${parsed.transactionId} to Room transactions table")
                        }
                    } else {
                        Log.w("SmsReceiver", "[Pipeline Step 4 Error] QumanApplication instance was null")
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "[Pipeline Step 4 Error] Failed saving to Room database", e)
                }

                // Step 5: Start overlay service if permission is granted and it's a real transaction
                val canOverlay = Settings.canDrawOverlays(context)
                Log.d("SmsReceiver", "[Pipeline Step 5] Overlay permission canDrawOverlays: $canOverlay")
                if (canOverlay && !parsed.isAd) {
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
                        Log.d("SmsReceiver", "[Pipeline Step 6] TransactionOverlayService started successfully")
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "[Pipeline Step 6 Error] Failed to start overlay service: ${e.message}", e)
                    }
                }

                // Step 6: High-priority notification with sound (fallback or complementary alert)
                NotificationHelper.showNotification(context, parsed)
                InAppNotificationManager.show(parsed)
                Log.d("SmsReceiver", "[Pipeline Step 7] Dispatched notification and in-app manager")

            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS in SmsReceiver", e)
            } finally {
                pendingResult.finish()
                Log.d("SmsReceiver", "[Pipeline Complete] BroadcastReceiver goAsync finished")
            }
        }
    }
}
