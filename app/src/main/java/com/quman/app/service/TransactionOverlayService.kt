package com.quman.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.quman.app.QumanApplication
import com.quman.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Locale

private data class OverlayItem(
    val transactionId: String,
    val initialDirection: String,
    val amount: Double,
    val counterpartyName: String,
    val counterpartyPhone: String,
    val balanceAfter: Double,
    val defaultTitle: String,
    val defaultMessage: String,
    val provider: String,
    val startId: Int
)

class TransactionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val overlayQueue = ArrayDeque<OverlayItem>()
    private var isShowing = false
    private var lastStartId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        lastStartId = startId

        if (!Settings.canDrawOverlays(this)) {
            Log.w("OverlayService", "Overlay permission not granted. Stopping service.")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val transactionId = intent?.getStringExtra("transaction_id") ?: ""
        val type = intent?.getStringExtra("type") ?: "OTHER"
        val direction = intent?.getStringExtra("direction") ?: if (type == "MONEY_RECEIVED") "in" else if (type == "MONEY_SENT") "out" else "other"
        val amount = intent?.getDoubleExtra("amount", 0.0) ?: 0.0
        val counterpartyName = intent?.getStringExtra("counterparty_name") ?: ""
        val counterpartyPhone = intent?.getStringExtra("counterparty_phone") ?: ""
        val balanceAfter = intent?.getDoubleExtra("balance_after", -1.0) ?: -1.0
        val title = intent?.getStringExtra("title") ?: "Quman Ogeysiis"
        val message = intent?.getStringExtra("message") ?: ""
        val provider = intent?.getStringExtra("provider") ?: "EVC Plus"

        val item = OverlayItem(
            transactionId = transactionId,
            initialDirection = direction,
            amount = amount,
            counterpartyName = counterpartyName,
            counterpartyPhone = counterpartyPhone,
            balanceAfter = balanceAfter,
            defaultTitle = title,
            defaultMessage = message,
            provider = provider,
            startId = startId
        )

        overlayQueue.addLast(item)

        if (!isShowing) {
            showNextOverlay()
        }

        return START_NOT_STICKY
    }

    private fun showNextOverlay() {
        if (overlayQueue.isEmpty()) {
            isShowing = false
            stopSelf(lastStartId)
            return
        }

        val nextItem = overlayQueue.removeFirst()
        isShowing = true
        showOverlay(nextItem)
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
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(8099, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(8099, notification)
                }
            } catch (e: Exception) {
                Log.w("OverlayService", "startForeground failed: ${e.message}")
            }
        }
    }

    private fun showOverlay(item: OverlayItem) {
        val transactionId = item.transactionId
        val initialDirection = item.initialDirection
        val amount = item.amount
        val counterpartyName = item.counterpartyName
        val counterpartyPhone = item.counterpartyPhone
        val balanceAfter = item.balanceAfter
        val defaultTitle = item.defaultTitle
        val defaultMessage = item.defaultMessage
        val provider = item.provider

        removeOverlay()

        var currentDirection = initialDirection

        val colorGreen = Color.parseColor("#16A34A")
        val colorGreenBg = Color.parseColor("#DCFCE7")
        val colorRed = Color.parseColor("#DC2626")
        val colorRedBg = Color.parseColor("#FEE2E2")
        val colorGrayText = Color.parseColor("#64748B")
        val colorGrayBg = Color.parseColor("#F1F5F9")

        val dp1 = dpToPx(1)
        val dp6 = dpToPx(6)
        val dp8 = dpToPx(8)
        val dp12 = dpToPx(12)
        val dp14 = dpToPx(14)
        val dp16 = dpToPx(16)
        val dp44 = dpToPx(44)
        val dp24 = dpToPx(24)

        // Root container card with swipe-down-to-dismiss gesture that does NOT block button clicks
        val rootLayout = object : LinearLayout(this) {
            private var startY = 0f
            private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = ev.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = ev.rawY - startY
                        if (deltaY > touchSlop) {
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> return true
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = event.rawY - startY
                        if (deltaY > 0) {
                            translationY = deltaY
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val deltaY = event.rawY - startY
                        if (deltaY > dpToPx(45)) {
                            // Swiped down: dismiss and show next in queue or stop
                            removeOverlay()
                            showNextOverlay()
                        } else {
                            animate().translationY(0f).setDuration(150).start()
                        }
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp12, dp14, dp14)

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp16.toFloat()
                setColor(Color.WHITE)
                val initialBorder = if (currentDirection == "out") colorRed else if (currentDirection == "in") colorGreen else Color.parseColor("#D97706")
                setStroke((1.5 * dp1).toInt(), initialBorder)
            }
            background = cardBg
            elevation = (dp12).toFloat()
        }

        // --- TOP ROW: Direction Pills ("Soo gashay" / "Baxday") + Dismiss "X" Button ---
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Direction Pills Container
        val pillsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        // Pill: Soo gashay (IN)
        val inPill = TextView(this).apply {
            text = "↓ Soo gashay"
            textSize = 11f
            isClickable = true
            isFocusable = true
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
        }

        // Pill: Baxday (OUT)
        val outPill = TextView(this).apply {
            text = "↑ Baxday"
            textSize = 11f
            isClickable = true
            isFocusable = true
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp8
            }
        }

        pillsContainer.addView(inPill)
        pillsContainer.addView(outPill)
        topRow.addView(pillsContainer)

        // Dismiss close button (X)
        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(colorGrayText)
            contentDescription = "Xir"
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                // Task 3: Dismisses popup, leaves transaction uncategorized, shows next if queued
                removeOverlay()
                showNextOverlay()
            }
        }
        topRow.addView(closeBtn)
        rootLayout.addView(topRow)

        // Space between top row and content row
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp8)
        }
        rootLayout.addView(spacer)

        // --- MAIN CONTENT ROW: Icon + Details + Amount ---
        val contentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Left circular icon
        val iconContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp44, dp44)
        }
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp24, dp24)
        }
        iconContainer.addView(iconView)
        contentRow.addView(iconContainer)

        // Middle details column
        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(dp12, 0, dp8, 0)
            }
        }

        // Header row inside text column: Status label + Provider badge
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        headerRow.addView(titleView)

        val providerBadge = TextView(this).apply {
            text = provider
            setTextColor(Color.parseColor("#475569"))
            textSize = 10f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp6, dpToPx(2), dp6, dpToPx(2))
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp6.toFloat()
                setColor(colorGrayBg)
            }
            background = badgeBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp6, 0, 0, 0)
            }
        }
        headerRow.addView(providerBadge)
        textColumn.addView(headerRow)

        // Message text
        val messageView = TextView(this).apply {
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
        contentRow.addView(textColumn)

        // Right side: Amount
        val amountView = TextView(this).apply {
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        contentRow.addView(amountView)
        rootLayout.addView(contentRow)

        // Function to refresh UI based on direction
        fun refreshUi(dir: String) {
            val isOut = dir == "out"
            val accentColor = if (isOut) colorRed else colorGreen
            val containerColor = if (isOut) colorRedBg else colorGreenBg

            // Update Root Card Border
            val rootBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp16.toFloat()
                setColor(Color.WHITE)
                setStroke((1.5 * dp1).toInt(), accentColor)
            }
            rootLayout.background = rootBg

            // Update Pills Styling
            val inBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                if (!isOut) {
                    setColor(colorGreen)
                } else {
                    setColor(colorGrayBg)
                }
            }
            inPill.background = inBg
            inPill.setTextColor(if (!isOut) Color.WHITE else colorGrayText)
            inPill.setTypeface(null, if (!isOut) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            val outBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                if (isOut) {
                    setColor(colorRed)
                } else {
                    setColor(colorGrayBg)
                }
            }
            outPill.background = outBg
            outPill.setTextColor(if (isOut) Color.WHITE else colorGrayText)
            outPill.setTypeface(null, if (isOut) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            // Update Icon
            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(containerColor)
            }
            iconContainer.background = circleBg
            iconView.setImageResource(if (isOut) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float)
            iconView.setColorFilter(accentColor)

            // Update Title & Message
            titleView.text = if (isOut) "Lacag La Diray (Sent)" else "Lacag La Helay (Received)"
            titleView.setTextColor(accentColor)

            val formattedAmt = String.format(Locale.US, "%.2f", amount)
            val sign = if (isOut) "-" else "+"
            amountView.text = "$sign$$formattedAmt"
            amountView.setTextColor(accentColor)

            if (isOut) {
                val target = when {
                    counterpartyName.isNotBlank() && counterpartyPhone.isNotBlank() -> "$counterpartyName ($counterpartyPhone)"
                    counterpartyPhone.isNotBlank() -> counterpartyPhone
                    counterpartyName.isNotBlank() -> counterpartyName
                    else -> ""
                }
                messageView.text = if (target.isNotBlank()) "Waxaad lacag dhan $$formattedAmt u dirtay $target" else defaultMessage
            } else {
                messageView.text = if (counterpartyPhone.isNotBlank()) "Waxaad heshay $$formattedAmt ka timid $counterpartyPhone" else defaultMessage
            }
        }

        // Initialize UI
        refreshUi(currentDirection)

        // Pill Click Listeners (Manual Direction Override)
        inPill.setOnClickListener {
            if (currentDirection != "in") {
                currentDirection = "in"
                refreshUi(currentDirection)
                if (transactionId.isNotBlank()) {
                    serviceScope.launch {
                        try {
                            val app = applicationContext as? QumanApplication
                            app?.database?.transactionDao()?.updateDirection(transactionId, "in")
                        } catch (e: Exception) {
                            Log.e("OverlayService", "Failed to update direction to in", e)
                        }
                    }
                }
            }
        }

        outPill.setOnClickListener {
            if (currentDirection != "out") {
                currentDirection = "out"
                refreshUi(currentDirection)
                if (transactionId.isNotBlank()) {
                    serviceScope.launch {
                        try {
                            val app = applicationContext as? QumanApplication
                            app?.database?.transactionDao()?.updateDirection(transactionId, "out")
                        } catch (e: Exception) {
                            Log.e("OverlayService", "Failed to update direction to out", e)
                        }
                    }
                }
            }
        }

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
            y = dpToPx(50) // Just below status bar
            horizontalMargin = 0.04f
        }

        try {
            windowManager?.addView(rootLayout, params)
            overlayView = rootLayout

            // Auto-dismiss after 8 seconds and advance to next in queue
            autoDismissRunnable = Runnable {
                removeOverlay()
                showNextOverlay()
            }
            handler.postDelayed(autoDismissRunnable!!, 8000L)
        } catch (e: Exception) {
            Log.e("OverlayService", "Failed to add overlay view", e)
            showNextOverlay()
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
}
