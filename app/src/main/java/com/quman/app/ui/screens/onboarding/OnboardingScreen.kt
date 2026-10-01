package com.quman.app.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quman.app.R
import com.quman.app.data.repository.SimCardRepository
import com.quman.app.ui.theme.MoneyInGreen
import com.quman.app.ui.theme.MoneyInGreenContainer
import com.quman.app.ui.theme.MoneyOutRed
import com.quman.app.ui.theme.MoneyOutRedContainer
import com.quman.app.ui.theme.PrimaryGradient
import com.quman.app.ui.theme.QumanDeepBlue
import com.quman.app.ui.theme.QumanViolet
import com.quman.app.ui.theme.TextMuted
import com.quman.app.ui.theme.TextPrimary
import com.quman.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    simCardRepository: SimCardRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission states
    var isSmsGranted by remember { mutableStateOf(false) }
    var isNotificationGranted by remember { mutableStateOf(false) }
    var isBatteryGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }

    // Denied counters for inline warnings
    var smsDeniedCount by rememberSaveable { mutableIntStateOf(0) }
    var overlayDeniedCount by rememberSaveable { mutableIntStateOf(0) }

    // Optional steps skip flags
    var isBatterySkipped by rememberSaveable { mutableStateOf(false) }
    var isOverlaySkipped by rememberSaveable { mutableStateOf(false) }

    // Function to re-check all permissions live
    fun checkPermissions() {
        val sms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Auto-granted below Android 13
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val battery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        val overlay = Settings.canDrawOverlays(context)

        isSmsGranted = sms
        isNotificationGranted = notif
        isBatteryGranted = battery
        isOverlayGranted = overlay

        if (sms) {
            // Silently detect SIMs whenever SMS permission is verified
            scope.launch {
                simCardRepository.detectAndPersistActiveSims()
            }
        }
    }

    // Observe Lifecycle ON_RESUME to auto-check permissions when returning from system dialogs/settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Launchers
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.RECEIVE_SMS] == true) &&
                (permissions[Manifest.permission.READ_SMS] == true) &&
                (permissions[Manifest.permission.READ_PHONE_STATE] == true)
        if (granted) {
            isSmsGranted = true
            scope.launch {
                simCardRepository.detectAndPersistActiveSims()
            }
        } else {
            smsDeniedCount++
        }
        checkPermissions()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        checkPermissions()
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        isOverlayGranted = granted
        if (!granted) {
            overlayDeniedCount++
        }
        checkPermissions()
    }

    fun requestSms() {
        smsLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    fun requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            isNotificationGranted = true
        }
    }

    fun requestBattery() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            batteryLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                batteryLauncher.launch(fallback)
            } catch (_: Exception) {
                // If device lacks battery optimization intent
                isBatterySkipped = true
            }
        }
    }

    fun requestOverlay() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayLauncher.launch(intent)
        } catch (_: Exception) {
            isOverlaySkipped = true
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Determine currently active step (1 to 4)
    val activeStep = when {
        !isSmsGranted -> 1
        !isNotificationGranted -> 2
        !isBatteryGranted && !isBatterySkipped -> 3
        !isOverlayGranted && !isOverlaySkipped -> 4
        else -> 5 // All granted or skipped
    }

    // Calculate remaining ungranted count
    val ungrantedItems = listOf(
        !isSmsGranted,
        !isNotificationGranted,
        !isBatteryGranted && !isBatterySkipped,
        !isOverlayGranted && !isOverlaySkipped
    )
    val ungrantedCount = ungrantedItems.count { it }
    val allCompleted = ungrantedCount == 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Big Gradient Bottom Action Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = QumanDeepBlue,
                            spotColor = QumanViolet
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryGradient)
                        .clickable {
                            if (allCompleted) {
                                onComplete()
                            } else {
                                // Trigger next ungranted item in sequence
                                when (activeStep) {
                                    1 -> requestSms()
                                    2 -> requestNotification()
                                    3 -> requestBattery()
                                    4 -> requestOverlay()
                                    else -> onComplete()
                                }
                            }
                        }
                        .testTag("onboarding_bottom_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allCompleted) {
                            stringResource(R.string.btn_continue)
                        } else {
                            stringResource(R.string.btn_grant_step, ungrantedCount, 4)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                // If mandatory steps are completed, show clear option to skip remaining optional steps
                if (isSmsGranted && isNotificationGranted && !allCompleted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.skip_optional_steps),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = QumanDeepBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                isBatterySkipped = true
                                isOverlaySkipped = true
                            }
                            .testTag("skip_all_optional_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Step Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (step in 1..4) {
                    val isDone = when (step) {
                        1 -> isSmsGranted
                        2 -> isNotificationGranted
                        3 -> isBatteryGranted || isBatterySkipped
                        4 -> isOverlayGranted || isOverlaySkipped
                        else -> false
                    }
                    val isActive = step == activeStep

                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isDone -> MoneyInGreen
                            isActive -> QumanViolet
                            else -> Color(0xFFCBD5E1)
                        },
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(if (isActive) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Header & Subtitle
            Text(
                text = stringResource(R.string.onboarding_header),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = QumanDeepBlue
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: SMS
            PermissionCard(
                icon = Icons.Default.Message,
                title = stringResource(R.string.permission_sms_title),
                subtitle = stringResource(R.string.permission_sms_subtitle),
                isGranted = isSmsGranted,
                isActive = activeStep == 1,
                onGrantClick = { requestSms() },
                bottomContent = {
                    Column {
                        // Samsung suppressed dialogs link
                        Text(
                            text = stringResource(R.string.permission_sms_restricted_link),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = QumanDeepBlue,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { openAppSettings() }
                                .testTag("sms_restricted_settings_link")
                        )

                        // Denied twice inline note with retry button
                        AnimatedVisibility(visible = !isSmsGranted && smsDeniedCount >= 2) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MoneyOutRedContainer)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.permission_sms_denied_warning),
                                    fontSize = 12.sp,
                                    color = MoneyOutRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { requestSms() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MoneyOutRed
                                    ),
                                    border = BorderStroke(1.dp, MoneyOutRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_retry),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("permission_card_sms")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Notifications
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.permission_notifications_title),
                subtitle = stringResource(R.string.permission_notifications_subtitle),
                isGranted = isNotificationGranted,
                isActive = activeStep == 2,
                onGrantClick = { requestNotification() },
                modifier = Modifier.testTag("permission_card_notifications")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Battery
            PermissionCard(
                icon = Icons.Default.BatteryChargingFull,
                title = stringResource(R.string.permission_battery_title),
                subtitle = stringResource(R.string.permission_battery_subtitle),
                isGranted = isBatteryGranted,
                isSkipped = isBatterySkipped,
                isActive = activeStep == 3,
                onGrantClick = { requestBattery() },
                skipContent = {
                    if (!isBatteryGranted) {
                        if (!isBatterySkipped) {
                            Text(
                                text = stringResource(R.string.btn_skip_for_now),
                                fontSize = 12.sp,
                                color = TextMuted,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { isBatterySkipped = true }
                                    .testTag("skip_battery_button")
                            )
                        } else {
                            Text(
                                text = "Waa laga booday (waxaad ka shidi kartaa Settings hadhow)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("permission_card_battery")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 4: Popup / Overlay
            PermissionCard(
                icon = Icons.Default.Layers,
                title = stringResource(R.string.permission_overlay_title),
                subtitle = stringResource(R.string.permission_overlay_subtitle),
                isGranted = isOverlayGranted,
                isSkipped = isOverlaySkipped,
                isActive = activeStep == 4,
                onGrantClick = { requestOverlay() },
                skipContent = {
                    if (!isOverlayGranted) {
                        if (!isOverlaySkipped) {
                            Text(
                                text = stringResource(R.string.btn_skip_for_now),
                                fontSize = 12.sp,
                                color = TextMuted,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { isOverlaySkipped = true }
                                    .testTag("skip_overlay_button")
                            )
                        } else {
                            Text(
                                text = "Waa laga booday (waxaad ka shidi kartaa Settings hadhow)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                },
                bottomContent = {
                    AnimatedVisibility(visible = !isOverlayGranted && overlayDeniedCount >= 2 && !isOverlaySkipped) {
                        Column(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.permission_overlay_denied_warning),
                                fontSize = 12.sp,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { requestOverlay() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF92400E)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF92400E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.btn_retry),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("permission_card_overlay")
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    isActive: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSkipped: Boolean = false,
    skipContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null
) {
    val cardBorder = if (isActive && !isGranted && !isSkipped) {
        BorderStroke(2.dp, QumanViolet)
    } else {
        BorderStroke(1.dp, Color(0xFFF1F5F9))
    }

    val elevation = if (isActive && !isGranted && !isSkipped) 6.dp else 2.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon in round container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isGranted -> MoneyInGreenContainer
                                isSkipped -> Color(0xFFF1F5F9)
                                else -> Color(0xFFEFF6FF)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = when {
                            isGranted -> MoneyInGreen
                            isSkipped -> TextSecondary
                            else -> QumanDeepBlue
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Status Pill / Button
                if (isGranted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MoneyInGreenContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("status_granted"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.status_permission_granted),
                            color = MoneyInGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (isSkipped) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { onGrantClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("status_skipped"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.status_permission_skipped),
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryGradient)
                            .clickable { onGrantClick() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("btn_grant"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.btn_permission_grant),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (skipContent != null) {
                skipContent()
            }

            if (bottomContent != null) {
                bottomContent()
            }
        }
    }
}
