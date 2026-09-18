package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ReportsData
import com.example.ui.components.MetricCard
import com.example.ui.components.RgCard
import com.example.ui.components.RgTopBar
import com.example.ui.components.formatKes
import com.example.ui.theme.RgAccent
import com.example.ui.theme.RgBackground
import com.example.ui.theme.RgCardBorder
import com.example.ui.theme.RgError
import com.example.ui.theme.RgSurfaceCard
import com.example.ui.theme.RgSurfaceVariant
import com.example.ui.theme.RgTextMuted
import com.example.ui.theme.RgTextPrimary
import com.example.ui.theme.RgTextSecondary
import com.example.ui.theme.RgWarning

@Composable
fun ReportsScreen(
    reportsData: ReportsData?,
    selectedRange: String,
    isOnline: Boolean,
    onSelectRange: (String) -> Unit
) {
    val data = reportsData ?: ReportsData(
        revenue = 0.0,
        transactionsCount = 0,
        averageSale = 0.0,
        retailRevenue = 0.0,
        wholesaleRevenue = 0.0,
        cashTotal = 0.0,
        mpesaTotal = 0.0,
        creditTotal = 0.0,
        costOfGoods = 0.0,
        grossProfit = 0.0,
        expensesTotal = 0.0,
        netProfit = 0.0,
        stockValue = 0.0,
        lowStockCount = 0,
        outOfStockCount = 0,
        outstandingCreditTotal = 0.0,
        purchasesTotal = 0.0
    )

    val totalSales = data.revenue
    val retailPct = if (totalSales > 0) (data.retailRevenue / totalSales).toFloat() else 0.5f
    val wholesalePct = if (totalSales > 0) (data.wholesaleRevenue / totalSales).toFloat() else 0.5f

    val totalPayments = (data.cashTotal + data.mpesaTotal + data.creditTotal).coerceAtLeast(1.0)
    val cashPct = (data.cashTotal / totalPayments).toFloat()
    val mpesaPct = (data.mpesaTotal / totalPayments).toFloat()
    val creditPct = (data.creditTotal / totalPayments).toFloat()

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RgTopBar(
                title = "Reports & Analytics",
                subtitle = "Business Intelligence & Profit",
                isOnline = isOnline
            )

            // Date Range Filter Tabs
            Surface(
                color = RgSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Today", "Week", "Month", "All Time").forEach { range ->
                        val isSel = selectedRange == range
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RgAccent else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectRange(range) }
                                .testTag("report_range_$range")
                        ) {
                            Text(
                                text = range,
                                color = if (isSel) Color(0xFF041810) else RgTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 1. Revenue & Profit Matrix
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Total Revenue",
                            value = formatKes(data.revenue),
                            subValue = "${data.transactionsCount} sales (${formatKes(data.averageSale)} avg)",
                            icon = Icons.Filled.PointOfSale,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Gross Profit",
                            value = formatKes(data.grossProfit),
                            subValue = "After KSh ${data.costOfGoods.toInt()} COGS",
                            icon = Icons.Filled.TrendingUp,
                            accentColor = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Expenses",
                            value = formatKes(data.expensesTotal),
                            subValue = "Operating expenses",
                            icon = Icons.Filled.MoneyOff,
                            accentColor = RgError,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Net Profit",
                            value = formatKes(data.netProfit),
                            subValue = "Gross profit - Expenses",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = if (data.netProfit >= 0) RgAccent else RgError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Retail vs Wholesale Channel Split (Part 18 & 29)
                item {
                    RgCard {
                        Column {
                            Text(
                                text = "SALES CHANNEL DISTRIBUTION",
                                color = RgTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Retail Sales", color = RgTextSecondary, fontSize = 11.sp)
                                    Text(text = formatKes(data.retailRevenue), color = RgAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Wholesale Sales", color = RgTextSecondary, fontSize = 11.sp)
                                    Text(text = formatKes(data.wholesaleRevenue), color = Color(0xFF64B5F6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                Box(modifier = Modifier.weight(retailPct.coerceAtLeast(0.01f)).fillMaxSize().background(RgAccent))
                                Box(modifier = Modifier.weight(wholesalePct.coerceAtLeast(0.01f)).fillMaxSize().background(Color(0xFF64B5F6)))
                            }
                        }
                    }
                }

                // 3. Payment Methods Breakdown
                item {
                    RgCard {
                        Column {
                            Text(
                                text = "PAYMENT METHOD MIX",
                                color = RgTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaymentRowItem("Cash", formatKes(data.cashTotal), Color(0xFF00F5A0))
                                PaymentRowItem("M-Pesa", formatKes(data.mpesaTotal), Color(0xFF4CAF50))
                                PaymentRowItem("Credit", formatKes(data.creditTotal), Color(0xFFFFB74D))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                Box(modifier = Modifier.weight(cashPct.coerceAtLeast(0.01f)).fillMaxSize().background(Color(0xFF00F5A0)))
                                Box(modifier = Modifier.weight(mpesaPct.coerceAtLeast(0.01f)).fillMaxSize().background(Color(0xFF4CAF50)))
                                Box(modifier = Modifier.weight(creditPct.coerceAtLeast(0.01f)).fillMaxSize().background(Color(0xFFFFB74D)))
                            }
                        }
                    }
                }

                // 4. Inventory Valuation & Working Capital
                item {
                    RgCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "WORKING CAPITAL & ASSETS",
                                color = RgTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Total Stock Valuation (Cost):", color = RgTextSecondary, fontSize = 12.sp)
                                Text(text = formatKes(data.stockValue), color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Low Stock Alerts:", color = RgTextSecondary, fontSize = 12.sp)
                                Text(text = "${data.lowStockCount} items", color = if (data.lowStockCount > 0) RgWarning else RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Out of Stock Items:", color = RgTextSecondary, fontSize = 12.sp)
                                Text(text = "${data.outOfStockCount} items", color = if (data.outOfStockCount > 0) RgError else RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun PaymentRowItem(name: String, amount: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = name, color = RgTextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = amount, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
