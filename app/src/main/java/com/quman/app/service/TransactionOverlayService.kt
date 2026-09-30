package com.quman.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.quman.app.R
import com.quman.app.util.NotificationHelper

class TransactionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle foreground service requirement on Android 8+
        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            Log.w("OverlayService", "Overlay permission not granted. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val type = intent?.getStringExtra("type") ?: "OTHER"
        val title = intent?.getStringExtra("title") ?: "Quman Ogeysiis"
        val message = intent?.getStringExtra("message") ?: ""
        val provider = intent?.getStringExtra("provider") ?: ""

        showOverlay(type, title, message, provider)

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "quman_overlay_channel"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Quman Popup Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Quman")
                .setContentText("Ogeysiis cusub")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            try {
                startForeground(8099, notification)
            } catch (e: Exception) {
                Log.w("OverlayService", "startForeground failed: ${e.message}")
            }
        }
    }

    private fun showOverlay(type: String, title: String, message: String, provider: String) {
        removeOverlay()

        val (accentColor, containerColor, iconRes, statusLabel) = when (type) {
            "MONEY_SENT" -> Quadruple(
                Color.parseColor("#DC2626"), // Red
                Color.parseColor("#FEE2E2"), // Light Red
                android.R.drawable.arrow_up_float,
                "Lacag La Diray (Sent)"
            )
            "MONEY_RECEIVED" -> Quadruple(
                Color.parseColor("#16A34A"), // Green
                Color.parseColor("#DCFCE7"), // Light Green
                android.R.drawable.arrow_down_float,
                "Lacag La Helay (Received)"
            )
            else -> Quadruple(
                Color.parseColor("#D97706"), // Amber/Yellow
                Color.parseColor("#FEF3C7"), // Light Yellow
                android.R.drawable.ic_dialog_info,
                "Ogeysiis (Notification)"
            )
        }

        val dp1 = dpToPx(1)
        val dp8 = dpToPx(8)
        val dp12 = dpToPx(12)
        val dp14 = dpToPx(14)
        val dp16 = dpToPx(16)
        val dp44 = dpToPx(44)
        val dp24 = dpToPx(24)

        // Root container card matching InAppNotificationPopup
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp14, dp14, dp14, dp14)

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp16.toFloat()
                setColor(Color.WHITE)
                setStroke((1.5 * dp1).toInt(), accentColor)
            }
            background = cardBg
            elevation = (dp12).toFloat()
        }

        // Left circular icon
        val iconContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp44, dp44)
            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(containerColor)
            }
            background = circleBg
        }
        val iconView = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(accentColor)
            layoutParams = LinearLayout.LayoutParams(dp24, dp24)
        }
        iconContainer.addView(iconView)
        rootLayout.addView(iconContainer)

        // Middle text column
        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(dp12, 0, dp8, 0)
            }
        }

        // Header row (Title + Provider badge)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = statusLabel
            setTextColor(accentColor)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        headerRow.addView(titleView)

        if (provider.isNotBlank()) {
            val providerBadge = TextView(this).apply {
                text = provider
                setTextColor(Color.parseColor("#475569"))
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
                val badgeBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(6).toFloat()
                    setColor(Color.parseColor("#F1F5F9"))
                }
                background = badgeBg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dpToPx(6), 0, 0, 0)
                }
            }
            headerRow.addView(providerBadge)
        }
        textColumn.addView(headerRow)

        // Message text
        val messageView = TextView(this).apply {
            text = message
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 13f
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(3)
            }
        }
        textColumn.addView(messageView)
        rootLayout.addView(textColumn)

        // Dismiss close button
        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                removeOverlay()
                stopSelf()
            }
        }
        rootLayout.addView(closeBtn)

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(50) // Just below standard status bar
            horizontalMargin = 0.04f
        }

        try {
            windowManager?.addView(rootLayout, params)
            overlayView = rootLayout

            // Auto-dismiss after 6.5 seconds
            autoDismissRunnable = Runnable {
                removeOverlay()
                stopSelf()
            }
            handler.postDelayed(autoDismissRunnable!!, 6500L)
        } catch (e: Exception) {
            Log.e("OverlayService", "Failed to add overlay view", e)
            stopSelf()
        }
    }

    private fun removeOverlay() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        autoDismissRunnable = null
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w("OverlayService", "Error removing overlay view: ${e.message}")
            }
            overlayView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
