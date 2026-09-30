package com.quman.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun InAppNotificationPopup(
    modifier: Modifier = Modifier
) {
    val currentNotification by InAppNotificationManager.currentNotification.collectAsStateWithLifecycle()

    // Auto-dismiss after 6 seconds
    LaunchedEffect(currentNotification) {
        if (currentNotification != null) {
            delay(6000L)
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
            val (accentColor, containerColor, statusIcon, statusLabel) = when (item.type) {
                NotificationType.MONEY_SENT -> Quadruple(
                    MoneyOutRed,
                    MoneyOutRedContainer,
                    Icons.Default.ArrowUpward,
                    "Lacag La Diray (Sent)"
                )
                NotificationType.MONEY_RECEIVED -> Quadruple(
                    MoneyInGreen,
                    MoneyInGreenContainer,
                    Icons.Default.ArrowDownward,
                    "Lacag La Helay (Received)"
                )
                NotificationType.OTHER -> Quadruple(
                    PromoAmber,
                    PromoAmberContainer,
                    Icons.Default.Notifications,
                    "Ogeysiis (Notification)"
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("in_app_notification_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { InAppNotificationManager.dismiss() },
                        modifier = Modifier
                            .size(36.dp)
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
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
