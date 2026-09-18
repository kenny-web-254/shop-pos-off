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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BusinessEntity
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.SaleEntity
import com.example.data.local.entities.UserEntity
import com.example.data.repository.ReportsData
import com.example.ui.components.MetricCard
import com.example.ui.components.RgCard
import com.example.ui.components.RgTopBar
import com.example.ui.components.formatKes
import com.example.ui.navigation.MainTab
import com.example.ui.theme.RgAccent
import com.example.ui.theme.RgAccentSubtle
import com.example.ui.theme.RgBackground
import com.example.ui.theme.RgCardBorder
import com.example.ui.theme.RgError
import com.example.ui.theme.RgSurfaceCard
import com.example.ui.theme.RgSurfaceVariant
import com.example.ui.theme.RgTextMuted
import com.example.ui.theme.RgTextPrimary
import com.example.ui.theme.RgTextSecondary
import com.example.ui.theme.RgWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    business: BusinessEntity?,
    currentUser: UserEntity?,
    isOnline: Boolean,
    reportsData: ReportsData?,
    recentSales: List<SaleEntity>,
    lowStockProducts: List<ProductEntity>,
    customersWithCredit: List<CustomerEntity>,
    onNavigateTab: (MainTab) -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenPurchases: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSaleDetails: (SaleEntity) -> Unit
) {
    val totalRevenue = reportsData?.revenue ?: 0.0
    val grossProfit = reportsData?.grossProfit ?: 0.0
    val transactionsCount = reportsData?.transactionsCount ?: 0
    val cashTotal = reportsData?.cashTotal ?: 0.0
    val mpesaTotal = reportsData?.mpesaTotal ?: 0.0
    val creditTotal = reportsData?.creditTotal ?: 0.0

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RgTopBar(
                title = business?.name ?: "RG POS",
                subtitle = "Dashboard",
                isOnline = isOnline,
                userRole = "${currentUser?.fullName ?: "Staff"} (${currentUser?.role ?: "User"})"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // 1. Metric Cards Grid (2x2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Today's Sales",
                            value = formatKes(totalRevenue),
                            subValue = "$transactionsCount completed",
                            icon = Icons.Filled.PointOfSale,
                            accentColor = RgAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Est. Profit",
                            value = formatKes(grossProfit),
                            subValue = "Margin ~${if (totalRevenue > 0) (grossProfit / totalRevenue * 100).toInt() else 0}%",
                            icon = Icons.Filled.TrendingUp,
                            accentColor = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Payment Summary Breakdown
                item {
                    RgCard {
                        Column {
                            Text(
                                text = "PAYMENT SUMMARY (TODAY)",
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
                                PaymentBreakdownItem("Cash", formatKes(cashTotal), Color(0xFF00F5A0))
                                PaymentBreakdownItem("M-Pesa", formatKes(mpesaTotal), Color(0xFF4CAF50))
                                PaymentBreakdownItem("Credit", formatKes(creditTotal), Color(0xFFFFB74D))
                            }
                        }
                    }
                }

                // 2. Actionable Alerts (Low stock / Customer credit)
                if (lowStockProducts.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x1AFFB74D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFB74D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateTab(MainTab.INVENTORY) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FFB74D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = RgWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${lowStockProducts.size} Products Low on Stock",
                                        color = RgTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to review re-order thresholds",
                                        color = RgTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = RgWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Quick Action Buttons
                item {
                    Text(
                        text = "QUICK ACTIONS",
                        color = RgTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "New Sale",
                            icon = Icons.Filled.ShoppingCart,
                            isAccent = true,
                            onClick = { onNavigateTab(MainTab.POS) },
                            modifier = Modifier.weight(1f).testTag("quick_new_sale")
                        )
                        QuickActionButton(
                            title = "Receive Stock",
                            icon = Icons.Filled.Inventory,
                            onClick = onOpenPurchases,
                            modifier = Modifier.weight(1f).testTag("quick_receive_stock")
                        )
                        QuickActionButton(
                            title = "Add Product",
                            icon = Icons.Filled.Add,
                            onClick = onOpenAddProduct,
                            modifier = Modifier.weight(1f).testTag("quick_add_product")
                        )
                        QuickActionButton(
                            title = "Credit Ledger",
                            icon = Icons.Filled.People,
                            onClick = onOpenCustomers,
                            modifier = Modifier.weight(1f).testTag("quick_credit_ledger")
                        )
                    }
                }

                // 4. Recent Sales Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT TRANSACTIONS",
                            color = RgTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "View All",
                            color = RgAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateTab(MainTab.REPORTS) }
                        )
                    }
                }

                if (recentSales.isEmpty()) {
                    item {
                        RgCard {
                            Text(
                                text = "No sales recorded yet today. Use 'New Sale' to begin.",
                                color = RgTextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    items(recentSales.take(6)) { sale ->
                        RecentSaleItem(sale = sale, onClick = { onOpenSaleDetails(sale) })
                    }
                }

                // Bottom spacer for floating bar
                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun PaymentBreakdownItem(label: String, amount: String, color: Color) {
    Column {
        Text(text = label, color = RgTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = amount, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAccent) RgAccent else RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAccent) RgAccent else RgCardBorder
        ),
        modifier = modifier
            .height(82.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAccent) Color(0xFF041810) else RgAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isAccent) Color(0xFF041810) else RgTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentSaleItem(sale: SaleEntity, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(sale.timestamp))

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RgSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sale.receiptNumber,
                            color = RgTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (sale.isWholesale) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x3364B5F6)
                            ) {
                                Text(
                                    text = "WHOLESALE",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${sale.customerName} • $formattedTime • ${sale.paymentMethod}",
                        color = RgTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatKes(sale.totalAmount),
                    color = RgAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sale.status,
                    color = if (sale.status == "Completed") RgTextSecondary else RgError,
                    fontSize = 10.sp
                )
            }
        }
    }
}
