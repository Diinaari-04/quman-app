package com.quman.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quman.app.R
import com.quman.app.ui.theme.HeaderGradient
import com.quman.app.ui.theme.MoneyOutRed
import com.quman.app.ui.theme.QumanDeepBlue
import com.quman.app.ui.theme.TextMuted
import com.quman.app.ui.theme.TextPrimary
import com.quman.app.ui.theme.TextSecondary
import com.quman.app.util.PhoneUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    userName: String?,
    userPhone: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val displayName = if (!userName.isNullOrBlank()) userName else "Akoon Quman"
    val displayPhone = if (!userPhone.isNullOrBlank()) PhoneUtils.formatDisplay(userPhone) else ""

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.logout_confirmation_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.logout_confirmation_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_confirm_logout),
                        color = MoneyOutRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGradient)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = QumanDeepBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (displayPhone.isNotEmpty()) {
                            Text(
                                text = displayPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Menu Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon = Icons.Default.SimCard,
                        title = stringResource(R.string.settings_sim_cards)
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Category,
                        title = stringResource(R.string.settings_categories)
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_security)
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about_app),
                        subtitle = stringResource(R.string.settings_version)
                    )
                }
            }

            // Notifications & Sound Status Card
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .testTag("notification_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = QumanDeepBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ogeysiisyada & Dhawaqa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Midabada xaaladda & codka dheer ee lacagta",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status color legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Green: Received
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.quman.app.ui.theme.MoneyInGreenContainer)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🟢 Cagaar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.quman.app.ui.theme.MoneyInGreen
                                )
                                Text(
                                    text = "Lacag la helay",
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Red: Sent
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.quman.app.ui.theme.MoneyOutRedContainer)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🔴 Gaduud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyOutRed
                                )
                                Text(
                                    text = "Lacag la diray",
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Yellow: Other
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.quman.app.ui.theme.PromoAmberContainer)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🟡 Jaalle",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.quman.app.ui.theme.PromoAmber
                                )
                                Text(
                                    text = "Ogeysiis kale",
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Fiiro gaar ah: SMS-ka lama akhrinayo si toos ah, mana furayo shaashadda iyada oo aan la taaban. SMS lacag ah marka ay timaado waxaa u dhacaya cod dheer oo gaar ah.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bypass silent mode toggle switch
                    val app = context.applicationContext as? com.quman.app.QumanApplication
                    val isBypassSilent by (app?.userPreferences?.isBypassSilentMode ?: kotlinx.coroutines.flow.flowOf(false))
                        .collectAsStateWithLifecycle(false)
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bypass silent mode for Quman",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "U daaran codka lacagta xitaa haddii taleefanku aamusan yahay. Quman ma dhaafayo Do Not Disturb (DND) si xasiloonidaada loo ilaaliyo.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.material3.Switch(
                            checked = isBypassSilent,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    app?.userPreferences?.setBypassSilentMode(enabled)
                                }
                            },
                            modifier = Modifier.testTag("bypass_silent_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tijaabi Ogeysiisyada:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val item = com.quman.app.util.SmsTransactionParser.parse(
                                    sender = "EVCPlus",
                                    body = "[-EVCPlus-] Waxaad $25.00 ka heshay 252615999888, Haraagaagu waa $145.45. Taariikh: 30/09/2026"
                                )
                                com.quman.app.util.NotificationHelper.showNotification(context, item)
                                com.quman.app.util.InAppNotificationManager.show(item)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_received_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.quman.app.ui.theme.MoneyInGreen),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🟢 Helay",
                                color = com.quman.app.ui.theme.MoneyInGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val item = com.quman.app.util.SmsTransactionParser.parse(
                                    sender = "EVCPlus",
                                    body = "[-EVCPlus-] $10.00 ayaad u wareejisay 252615123456, Haraagaagu waa $135.45. Taariikh: 30/09/2026"
                                )
                                com.quman.app.util.NotificationHelper.showNotification(context, item)
                                com.quman.app.util.InAppNotificationManager.show(item)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_sent_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MoneyOutRed),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🔴 Diray",
                                color = MoneyOutRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val item = com.quman.app.util.SmsTransactionParser.parse(
                                    sender = "Quman",
                                    body = "Quman: Nidaamka akoonkaaga wuxuu u shaqeynayaa si sugan."
                                )
                                com.quman.app.util.NotificationHelper.showNotification(context, item)
                                com.quman.app.util.InAppNotificationManager.show(item)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_other_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.quman.app.ui.theme.PromoAmber),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🟡 Kale",
                                color = com.quman.app.ui.theme.PromoAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Logout Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clickable { showLogoutDialog = true }
                    .testTag("logout_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MoneyOutRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.btn_logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MoneyOutRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QumanDeepBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}
