package com.quman.app.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quman.app.QumanApplication
import com.quman.app.R
import com.quman.app.data.local.entities.TransactionEntity
import com.quman.app.ui.theme.HeaderGradient
import com.quman.app.ui.theme.MoneyInGreen
import com.quman.app.ui.theme.MoneyInGreenContainer
import com.quman.app.ui.theme.MoneyOutRed
import com.quman.app.ui.theme.MoneyOutRedContainer
import com.quman.app.ui.theme.PromoAmber
import com.quman.app.ui.theme.PromoAmberContainer
import com.quman.app.ui.theme.QumanDeepBlue
import com.quman.app.ui.theme.TextMuted
import com.quman.app.ui.theme.TextPrimary
import com.quman.app.ui.theme.TextSecondary
import com.quman.app.util.CurrencyUtils
import com.quman.app.util.PhoneUtils
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String?,
    userPhone: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? QumanApplication

    // Reactive database transactions and ads
    val transactions by (app?.database?.transactionDao()?.getAllTransactions() ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(emptyList())

    val ads by (app?.database?.adMessageDao()?.getAllAds() ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(emptyList())

    // Runtime SMS Permission check
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermission = (results[Manifest.permission.RECEIVE_SMS] == true) &&
                (results[Manifest.permission.READ_SMS] == true)
    }

    LaunchedEffect(Unit) {
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    // Dynamic Stats Calculations (Money IN and Money OUT)
    val totalIn = remember(transactions) {
        transactions.filter { it.direction == "in" }.sumOf { it.amount }
    }
    val totalOut = remember(transactions) {
        transactions.filter { it.direction == "out" }.sumOf { it.amount }
    }

    // Feature 2 & Feature 3: Authoritative Provider Running Balance (balance_after)
    // 1. Group by provider to get the most recent balance_after for each active provider/SIM
    val providerBalances = remember(transactions) {
        transactions
            .filter { it.balanceAfter != null }
            .groupBy { it.provider }
            .mapValues { (_, txList) -> txList.first().balanceAfter!! }
    }

    // 2. Check if a balance has ever been recorded from real SMS
    val hasKnownBalance = providerBalances.isNotEmpty()

    // 3. Combined total of each SIM's latest authoritative balance_after (or single SIM's balance)
    val haraagaGuud = remember(providerBalances) {
        if (providerBalances.isNotEmpty()) providerBalances.values.sum() else 0.0
    }

    val displayName = if (!userName.isNullOrBlank()) userName else "Saaxiib"
    val displayPhone = if (!userPhone.isNullOrBlank()) PhoneUtils.formatDisplay(userPhone) else ""

    var isAdsExpanded by rememberSaveable { mutableStateOf(false) }

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
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${stringResource(R.string.home_welcome)} $displayName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (displayPhone.isNotEmpty()) {
                            Text(
                                text = displayPhone,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFBFDBFE)
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Balance Card inside Header (Shows authoritative provider balance_after)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_balance_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (hasKnownBalance) {
                            Text(
                                text = CurrencyUtils.formatBalance(haraagaGuud),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = if (haraagaGuud >= 0) QumanDeepBlue else MoneyOutRed,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            if (providerBalances.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    providerBalances.forEach { (prov, bal) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "$prov: ${CurrencyUtils.formatBalance(bal)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Feature 3: First login/signup with zero transaction SMS yet
                            Text(
                                text = "Lama ogeyn wali",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = QumanDeepBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Haraagan wuxuu soo muuqan doonaa marka fariinta xigta ee lacagta timaado.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // In and Out Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Money IN
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MoneyInGreenContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MoneyInGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.home_money_in),
                                            fontSize = 11.sp,
                                            color = Color(0xFF166534),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = CurrencyUtils.formatAmount(totalIn, "in"),
                                            fontSize = 13.sp,
                                            color = MoneyInGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Money OUT
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MoneyOutRedContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MoneyOutRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.home_money_out),
                                            fontSize = 11.sp,
                                            color = Color(0xFF991B1B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = CurrencyUtils.formatAmount(totalOut, "out"),
                                            fontSize = 13.sp,
                                            color = MoneyOutRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Body Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Permission Missing Alert Banner (if SMS permissions are not granted at runtime)
            if (!hasSmsPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .clickable {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        }
                        .testTag("sms_permission_warning_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MoneyOutRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS permission ma ogolayn — Taabo si aad u ogolaato",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Quman ma ogaan karo lacagaha kuu soo dhaca ama baxa haddii aan fariimaha loo oggolaan.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Mobile Money Providers summary badge
            Text(
                text = "Adeegyada Mobilka",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("EVC Plus", "ZAAD", "Sahal", "eDahab").forEach { provider ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = QumanDeepBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = provider,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature 1: Search transactions on HomeScreen
            var homeSearchQuery by rememberSaveable { mutableStateOf("") }
            val filteredHomeTransactions = remember(transactions, homeSearchQuery) {
                if (homeSearchQuery.isBlank()) {
                    transactions
                } else {
                    val q = homeSearchQuery.trim().lowercase()
                    transactions.filter { tx ->
                        (tx.counterpartyName?.lowercase()?.contains(q) == true) ||
                        (tx.counterpartyPhone?.lowercase()?.contains(q) == true)
                    }
                }
            }

            // Recent Transactions Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (transactions.isNotEmpty()) {
                    Text(
                        text = if (homeSearchQuery.isNotBlank()) "${filteredHomeTransactions.size} natiijo" else "${transactions.size} dhaqdhaqaaq",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar for HomeScreen
            if (transactions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Raadi",
                            tint = QumanDeepBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = homeSearchQuery,
                            onValueChange = { homeSearchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_search_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (homeSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Raadi magac ama lambar...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (homeSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { homeSearchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tirtir",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transactions List or Empty Placeholder
            if (transactions.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .testTag("recent_transactions_placeholder"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = QumanDeepBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.home_no_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.home_placeholder_card),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (filteredHomeTransactions.isEmpty()) {
                // Search yielded no results
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .testTag("home_no_search_results"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = QumanDeepBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Lama helin natiijo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Wax dhaqdhaqaaq ah kuma haboona raadintaada: \"$homeSearchQuery\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .testTag("recent_transactions_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredHomeTransactions.take(15).forEachIndexed { index, tx ->
                            TransactionRowItem(tx = tx)
                            if (index < filteredHomeTransactions.take(15).size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible "Xayeysiin" (Promotions & Telecom Announcements) Section
            if (ads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, PromoAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .testTag("ads_collapsible_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdsExpanded = !isAdsExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(PromoAmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = PromoAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Xayeysiin & Fariimo kale",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PromoAmberContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${ads.size}",
                                                color = PromoAmber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Fariimaha aan xisaabta ahayn ee shirkadaha",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isAdsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isAdsExpanded) "Qari" else "Fur",
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = isAdsExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                ads.forEachIndexed { i, ad ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${ad.provider} (${ad.sender})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = PromoAmber
                                                )
                                            )
                                            Text(
                                                text = formatDateTime(ad.occurredAt),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = ad.body,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextPrimary,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                    if (i < ads.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFF1F5F9),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun TransactionRowItem(tx: TransactionEntity) {
    val isOut = tx.direction == "out"
    val accentColor = if (isOut) MoneyOutRed else MoneyInGreen
    val containerColor = if (isOut) MoneyOutRedContainer else MoneyInGreenContainer
    val icon = if (isOut) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored circle icon matching balance tokens
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isOut) "Money Out" else "Money In",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle details
        Column(modifier = Modifier.weight(1f)) {
            val titleText = tx.counterpartyPhone
                ?: tx.counterpartyName
                ?: tx.note
                ?: tx.provider

            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${tx.provider} • ${formatDateTime(tx.occurredAt)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right side: Amount + optional balance
        Column(horizontalAlignment = Alignment.End) {
            val sign = if (isOut) "-" else "+"
            Text(
                text = "$sign$${String.format(Locale.US, "%.2f", tx.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 14.sp
                )
            )
            if (tx.balanceAfter != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Haraa: ${CurrencyUtils.formatBalance(tx.balanceAfter)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

