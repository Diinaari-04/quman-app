package com.quman.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.quman.app.QumanApplication
import com.quman.app.data.local.entities.TransactionEntity
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

            // Parse SMS using Quman rules (Green for received, Red for sent, Yellow for other)
            val parsed = SmsTransactionParser.parse(sender, fullBody, timestamp)

            // 1. Show Pop-up notification (Heads-up notification banner with color coding & distinctive sound)
            NotificationHelper.showNotification(context, parsed)

            // 2. Dispatch to in-app pop-up overlay if app is in foreground
            InAppNotificationManager.show(parsed)

            // 3. Persist money transaction to local database asynchronously
            if (parsed.amount != null && (parsed.type == NotificationType.MONEY_SENT || parsed.type == NotificationType.MONEY_RECEIVED)) {
                scope.launch {
                    try {
                        val app = context.applicationContext as? QumanApplication ?: return@launch
                        val userId = app.authRepository.getCurrentUser()?.id
                            ?: app.userPreferences.cachedPhone.firstOrNull()
                            ?: "local_user"

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
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Error saving transaction to database", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS in SmsReceiver", e)
        }
    }
}
