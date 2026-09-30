package com.quman.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quman.app.MainActivity
import com.quman.app.R

object NotificationHelper {
    const val CHANNEL_ID_MONEY = "quman_money_transactions_channel_v2"
    const val CHANNEL_ID_GENERAL = "quman_general_channel_v2"

    private const val NOTIFICATION_ID_BASE = 8000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. High-importance Money Channel with custom loud sound
            val moneySoundUri = Uri.parse(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.money_alert}"
            )
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val moneyChannel = NotificationChannel(
                CHANNEL_ID_MONEY,
                "Quman Lacagaha (Money Transactions)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ogeysiisyada lacagaha la helo ama la diro"
                enableLights(true)
                lightColor = AndroidColor.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(moneySoundUri, audioAttributes)
            }

            // 2. Default General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Quman Ogeysiisyada Guud (General Notifications)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ogeysiisyada kale ee guud"
                enableLights(true)
                lightColor = AndroidColor.YELLOW
            }

            notificationManager.createNotificationChannel(moneyChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Shows a pop-up (heads-up) notification on message arrival.
     * Does NOT automatically open or read the message (no fullScreenIntent).
     */
    fun showNotification(context: Context, notification: ParsedSmsNotification) {
        createNotificationChannels(context)

        val channelId: String
        val colorInt: Int

        when (notification.type) {
            NotificationType.MONEY_RECEIVED -> {
                channelId = CHANNEL_ID_MONEY
                colorInt = AndroidColor.rgb(22, 163, 74) // Green: #16A34A
                // Also play loud distinctive sound directly
                MoneySoundPlayer.playMoneyAlertSound(context)
            }
            NotificationType.MONEY_SENT -> {
                channelId = CHANNEL_ID_MONEY
                colorInt = AndroidColor.rgb(220, 38, 38) // Red: #DC2626
                // Also play loud distinctive sound directly
                MoneySoundPlayer.playMoneyAlertSound(context)
            }
            NotificationType.OTHER -> {
                channelId = CHANNEL_ID_GENERAL
                colorInt = AndroidColor.rgb(245, 158, 11) // Yellow: #F59E0B
            }
        }

        // Standard content intent when tapped by the user (never triggered automatically)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setColor(colorInt)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(if (channelId == CHANNEL_ID_MONEY) 0 else NotificationCompat.DEFAULT_SOUND)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted yet on Android 13+
        }
    }
}
