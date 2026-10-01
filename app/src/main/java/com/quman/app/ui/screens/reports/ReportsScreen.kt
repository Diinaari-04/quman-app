package com.quman.app.ui.screens.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quman.app.QumanApplication
import com.quman.app.R
import com.quman.app.data.local.entities.TransactionEntity
import com.quman.app.ui.theme.HeaderGradient
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
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportFilterPeriod {
    TODAY,       // Maanta
    THIS_MONTH,  // Bishan (Default)
    THIS_YEAR,   // Sanadkan
    CUSTOM       // Dooro Taariikh (Custom Range)
}

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? QumanApplication

    // Default selection: "Bishan" (this month)
    var selectedPeriod by remember { mutableStateOf(ReportFilterPeriod.THIS_MONTH) }

    // Custom date range state (defaults to start of month to today)
    val now = Calendar.getInstance()
    var customStartMillis by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var customEndMillis by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    // Compute effective (startTime, endTime) based on selected period
    val (startTime, endTime) = remember(selectedPeriod, customStartMillis, customEndMillis) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            ReportFilterPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            ReportFilterPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            ReportFilterPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            ReportFilterPeriod.CUSTOM -> {
                Pair(customStartMillis, customEndMillis)
            }
        }
    }

    // Reusing the same Room query pattern with WHERE clause on occurred_at
    val transactions by (app?.database?.transactionDao()?.getTransactionsBetween(startTime, endTime) ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(emptyList())

    // Recalculate summary totals for selected range only
    val totalIn = remember(transactions) {
        transactions.filter { it.direction == "in" }.sumOf { it.amount }
    }
    val totalOut = remember(transactions) {
        transactions.filter { it.direction == "out" }.sumOf { it.amount }
    }
    val netBalance = remember(totalIn, totalOut) {
        totalIn - totalOut
    }

    // DatePicker dialog helpers for Custom Range
    val dateChipFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(chosen.timeInMillis)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
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
            Column {
                Text(
                    text = stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = stringResource(R.string.reports_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFBFDBFE)
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- FILTER CONTROL CHIPS (Bug 1 Fix) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick chip 1: Maanta
                FilterChipItem(
                    label = "Maanta",
                    isSelected = selectedPeriod == ReportFilterPeriod.TODAY,
                    onClick = { selectedPeriod = ReportFilterPeriod.TODAY },
                    testTag = "filter_chip_today"
                )

                // Quick chip 2: Bishan (Default)
                FilterChipItem(
                    label = "Bishan",
                    isSelected = selectedPeriod == ReportFilterPeriod.THIS_MONTH,
                    onClick = { selectedPeriod = ReportFilterPeriod.THIS_MONTH },
                    testTag = "filter_chip_this_month"
                )

                // Quick chip 3: Sanadkan
                FilterChipItem(
                    label = "Sanadkan",
                    isSelected = selectedPeriod == ReportFilterPeriod.THIS_YEAR,
                    onClick = { selectedPeriod = ReportFilterPeriod.THIS_YEAR },
                    testTag = "filter_chip_this_year"
                )

                // Quick chip 4: Dooro Taariikh (Custom Range)
                FilterChipItem(
                    label = "Taariikh Gaar ah",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = selectedPeriod == ReportFilterPeriod.CUSTOM,
                    onClick = { selectedPeriod = ReportFilterPeriod.CUSTOM },
                    testTag = "filter_chip_custom"
                )
            }

            // Custom Range Selectors (visible when CUSTOM is selected)
            if (selectedPeriod == ReportFilterPeriod.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // From Date
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable {
                                    showDatePicker(customStartMillis) { selected ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selected
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        customStartMillis = cal.timeInMillis
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Laga bilaabo", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = QumanDeepBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dateChipFormat.format(Date(customStartMillis)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Text(
                            text = "—",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        // To Date
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable {
                                    showDatePicker(customEndMillis) { selected ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selected
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }
                                        customEndMillis = cal.timeInMillis
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Ilaa", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = QumanDeepBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dateChipFormat.format(Date(customEndMillis)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SUMMARY TOTALS BREAKDOWN CARD (Recalculated for selected range) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .testTag("reports_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haraaga Guud (Net)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${transactions.size} dhaqdhaqaaq",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (netBalance >= 0) "+$" else "-$") + String.format(Locale.US, "%.2f", Math.abs(netBalance)),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = if (netBalance >= 0) MoneyInGreen else MoneyOutRed,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total In (Lacag Soo Gashay)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneyInGreenContainer)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Lacag Soo Gashay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+$${String.format(Locale.US, "%.2f", totalIn)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyInGreen
                            )
                        }

                        // Total Out (Lacag Baxday)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneyOutRedContainer)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Lacag Baxday",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "-$${String.format(Locale.US, "%.2f", totalOut)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyOutRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- FILTERED TRANSACTIONS LIST ---
            if (transactions.isEmpty()) {
                // Placeholder Card when no transactions in this range
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .testTag("reports_empty_card"),
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = QumanViolet,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ma jiro dhaqdhaqaaq taariikhdan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Muddadan la doortay wax dhaqdhaqaaq ah lagama helin xogta SMS-ka.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .testTag("reports_transactions_list_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        transactions.forEachIndexed { idx, tx ->
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(containerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.counterpartyPhone ?: tx.counterpartyName ?: tx.provider,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "${tx.provider} • ${formatReportDate(tx.occurredAt)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                val sign = if (isOut) "-" else "+"
                                Text(
                                    text = "$sign$${String.format(Locale.US, "%.2f", tx.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            if (idx < transactions.size - 1) {
                                HorizontalDivider(
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
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
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val bgModifier = if (isSelected) {
        Modifier.background(PrimaryGradient)
    } else {
        Modifier.background(Color(0xFFF1F5F9))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatReportDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

