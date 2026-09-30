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

            // Parse SMS using Quman rules (Green for received, Red for sent, Yellow for other / ads)
            val parsed = SmsTransactionParser.parse(sender, fullBody, timestamp)

            // 1. Try to display system floating overlay over other apps if overlay permission is granted
            val canOverlay = Settings.canDrawOverlays(context)
            var overlayServiceStarted = false
            if (canOverlay) {
                try {
                    val overlayIntent = Intent(context, TransactionOverlayService::class.java).apply {
                        putExtra("type", parsed.type.name)
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
                            occurredAt = timestamp
                        )
                        app.database.adMessageDao().insertOrUpdate(adEntity)
                    } else if (parsed.amount != null && (parsed.type == NotificationType.MONEY_SENT || parsed.type == NotificationType.MONEY_RECEIVED)) {
                        // Insert real money transaction into transactions table
                        val transaction = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            provider = parsed.provider,
                            sender = sender,
                            direction = if (parsed.type == NotificationType.MONEY_RECEIVED) "in" else "out",
                            amount = parsed.amount,
                            counterpartyName = parsed.counterparty,
                            counterpartyPhone = parsed.counterpartyPhone,
                            balanceAfter = parsed.balanceAfter,
                            note = parsed.title,
                            smsHash = fullBody.hashCode().toString(),
                            occurredAt = timestamp,
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
