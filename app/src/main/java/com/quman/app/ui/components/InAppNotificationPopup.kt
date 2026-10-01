package com.quman.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quman.app.QumanApplication
import com.quman.app.ui.theme.MoneyInGreen
import com.quman.app.ui.theme.MoneyInGreenContainer
import com.quman.app.ui.theme.MoneyOutRed
import com.quman.app.ui.theme.MoneyOutRedContainer
import com.quman.app.ui.theme.PromoAmber
import com.quman.app.ui.theme.PromoAmberContainer
import com.quman.app.ui.theme.TextPrimary
import com.quman.app.ui.theme.TextSecondary
import com.quman.app.util.InAppNotificationManager
import com.quman.app.util.NotificationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun InAppNotificationPopup(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? QumanApplication
    val scope = rememberCoroutineScope()

    val currentNotification by InAppNotificationManager.currentNotification.collectAsStateWithLifecycle()

    // Auto-dismiss after 8 seconds
    LaunchedEffect(currentNotification) {
        if (currentNotification != null) {
            delay(8000L)
            InAppNotificationManager.dismiss()
        }
    }

    AnimatedVisibility(
        visible = currentNotification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        currentNotification?.let { item ->
            // Local state for direction manual toggle
            var direction by remember(item.id) {
                mutableStateOf(item.direction.ifBlank { if (item.type == NotificationType.MONEY_RECEIVED) "in" else "out" })
            }

            val isOut = direction == "out"
            val isMoney = item.type == NotificationType.MONEY_SENT || item.type == NotificationType.MONEY_RECEIVED

            val accentColor = if (isMoney) {
                if (isOut) MoneyOutRed else MoneyInGreen
            } else {
                PromoAmber
            }

            val containerColor = if (isMoney) {
                if (isOut) MoneyOutRedContainer else MoneyInGreenContainer
            } else {
                PromoAmberContainer
            }

            val statusIcon = if (isMoney) {
                if (isOut) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
            } else {
                Icons.Default.Notifications
            }

            val statusLabel = if (isMoney) {
                if (isOut) "Lacag La Diray (Sent)" else "Lacag La Helay (Received)"
            } else {
                "Ogeysiis (Notification)"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .pointerInput(item.id) {
                        // Task 3: Support swipe-down to dismiss gesture
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 25) {
                                InAppNotificationManager.dismiss()
                            }
                        }
                    }
                    .testTag("in_app_notification_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // --- TOP ROW: Direction Pills ("Soo gashay" / "Baxday") + Close Button ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMoney) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pill: Soo gashay (IN)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (!isOut) MoneyInGreen else Color(0xFFF1F5F9))
                                        .clickable {
                                            if (direction != "in") {
                                                direction = "in"
                                                scope.launch {
                                                    app?.database?.transactionDao()?.updateDirection(item.transactionId, "in")
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("direction_pill_in")
                                ) {
                                    Text(
                                        text = "↓ Soo gashay",
                                        color = if (!isOut) Color.White else TextSecondary,
                                        fontWeight = if (!isOut) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }

                                // Pill: Baxday (OUT)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isOut) MoneyOutRed else Color(0xFFF1F5F9))
                                        .clickable {
                                            if (direction != "out") {
                                                direction = "out"
                                                scope.launch {
                                                    app?.database?.transactionDao()?.updateDirection(item.transactionId, "out")
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("direction_pill_out")
                                ) {
                                    Text(
                                        text = "↑ Baxday",
                                        color = if (isOut) Color.White else TextSecondary,
                                        fontWeight = if (isOut) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Ogeysiis Guud",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = PromoAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Task 3: Dismiss close button (leaves transaction uncategorized)
                        IconButton(
                            onClick = { InAppNotificationManager.dismiss() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xir",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- CONTENT ROW: Icon + Details + Amount ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusLabel,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        fontSize = 14.sp
                                    )
                                )
                                if (item.provider.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.provider,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                maxLines = 2
                            )
                        }

                        if (item.amount != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val sign = if (isOut) "-" else "+"
                            Text(
                                text = "$sign$${String.format(Locale.US, "%.2f", item.amount)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
