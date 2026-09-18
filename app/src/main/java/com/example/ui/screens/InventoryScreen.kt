package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.StockMovementEntity
import com.example.data.local.entities.UnitConversionEntity
import com.example.data.repository.ConversionEngine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricCard
import com.example.ui.components.RgButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<ProductEntity>,
    conversions: List<UnitConversionEntity>,
    movements: List<StockMovementEntity>,
    isOnline: Boolean,
    onOpenAddProduct: () -> Unit,
    onOpenEditProduct: (ProductEntity) -> Unit,
    onAdjustStock: (Long, Double, String, String, () -> Unit) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Products, 1: Stock Movements, 2: Stock Take
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Low", "Out"

    var productForAdjustment by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery, filterStatus) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterStatus) {
                "Low" -> p.currentStock in 1.0..p.minStock.toDouble()
                "Out" -> p.currentStock <= 0
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val totalStockValuation = remember(products) {
        products.sumOf { it.currentStock * it.buyingCost }
    }
    val lowStockCount = remember(products) {
        products.count { it.currentStock in 1.0..it.minStock.toDouble() }
    }
    val outOfStockCount = remember(products) {
        products.count { it.currentStock <= 0 }
    }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RgTopBar(
                title = "Inventory",
                subtitle = "${products.size} Catalog Items",
                isOnline = isOnline,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RgAccent,
                        modifier = Modifier.clickable { onOpenAddProduct() }.testTag("inventory_add_product")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color(0xFF041810), modifier = Modifier.size(16.dp))
                            Text(text = "Add Item", color = Color(0xFF041810), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )

            // Subtab Navigation
            val tabs = listOf("Products", "Movement Log", "Stock Take")
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = RgSurfaceCard,
                contentColor = RgAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = RgAccent
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            when (selectedSubTab) {
                0 -> {
                    // TAB 0: Products Catalog View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // Metric summary banner
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard(
                                    title = "Stock Valuation",
                                    value = formatKes(totalStockValuation),
                                    subValue = "At buying cost",
                                    icon = Icons.Filled.Inventory2,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Low / Out",
                                    value = "$lowStockCount / $outOfStockCount",
                                    subValue = "Threshold alerts",
                                    accentColor = if (lowStockCount > 0 || outOfStockCount > 0) RgWarning else RgAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Search and Filter row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Filter products...", color = RgTextMuted, fontSize = 12.sp) },
                                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = RgTextSecondary, modifier = Modifier.size(16.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RgAccent,
                                        unfocusedBorderColor = RgCardBorder,
                                        focusedTextColor = RgTextPrimary,
                                        unfocusedTextColor = RgTextPrimary,
                                        cursorColor = RgAccent,
                                        focusedContainerColor = RgSurfaceVariant,
                                        unfocusedContainerColor = RgSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(46.dp)
                                )

                                listOf("All", "Low", "Out").forEach { status ->
                                    val isSel = filterStatus == status
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) RgAccent else RgSurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                                        modifier = Modifier.clickable { filterStatus = status }
                                    ) {
                                        Text(
                                            text = status,
                                            color = if (isSel) Color(0xFF041810) else RgTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Filled.Inventory2,
                                    title = "No products found",
                                    subtitle = "Add items to your inventory to monitor stock and enable sales.",
                                    actionText = "Add Product",
                                    onAction = onOpenAddProduct
                                )
                            }
                        } else {
                            items(filteredProducts) { prod ->
                                val prodConversions = conversions.filter { it.productId == prod.id }
                                InventoryProductRow(
                                    product = prod,
                                    conversions = prodConversions,
                                    onEdit = { onOpenEditProduct(prod) },
                                    onAdjust = { productForAdjustment = prod }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(84.dp)) }
                    }
                }

                1 -> {
                    // TAB 1: Stock Movement Log (Part 20)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        item {
                            Text(
                                text = "AUDIT LOG OF ALL INVENTORY MOVEMENTS (BASE UNIT)",
                                color = RgTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (movements.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Filled.History,
                                    title = "No movements recorded",
                                    subtitle = "Stock movements are created automatically on Sales, Purchases, Returns and Adjustments."
                                )
                            }
                        } else {
                            items(movements) { mov ->
                                StockMovementItem(mov = mov)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(84.dp)) }
                    }
                }

                2 -> {
                    // TAB 2: Stock Take / Physical Count Audit
                    StockTakeView(
                        products = products,
                        onAdjustStock = onAdjustStock
                    )
                }
            }
        }
    }

    // Modal: Quick Stock Adjustment
    if (productForAdjustment != null) {
        val prod = productForAdjustment!!
        StockAdjustmentModal(
            product = prod,
            onDismiss = { productForAdjustment = null },
            onConfirm = { actualCount, reason, notes ->
                onAdjustStock(prod.id, actualCount, reason, notes) {
                    productForAdjustment = null
                }
            }
        )
    }
}

@Composable
private fun InventoryProductRow(
    product: ProductEntity,
    conversions: List<UnitConversionEntity>,
    onEdit: () -> Unit,
    onAdjust: () -> Unit
) {
    val isOutOfStock = product.currentStock <= 0
    val isLowStock = !isOutOfStock && product.currentStock <= product.minStock

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = RgTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.categoryName} • SKU: ${product.sku}",
                        color = RgTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RgSurfaceVariant,
                        modifier = Modifier.clickable { onAdjust() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = RgAccent, modifier = Modifier.size(13.dp))
                            Text(text = "Count", color = RgAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", tint = RgTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Stock metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Retail: ${formatKes(product.retailPrice)} / ${product.baseUnit}", color = RgTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    if (product.allowWholesale) {
                        Text(text = "Wholesale: ${formatKes(product.wholesalePrice)} / ${product.baseUnit}", color = Color(0xFF64B5F6), fontSize = 11.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ConversionEngine.formatStock(product.currentStock, product.baseUnit),
                        color = when {
                            isOutOfStock -> RgError
                            isLowStock -> RgWarning
                            else -> RgAccent
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Threshold: ${product.minStock} ${product.baseUnit}",
                        color = RgTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Conversion units pill list
            if (conversions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    conversions.forEach { conv ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder)
                        ) {
                            Text(
                                text = "1 ${conv.unitName} = ${conv.multiplierToBase.toInt()} ${product.baseUnit}s",
                                color = RgTextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockMovementItem(mov: StockMovementEntity) {
    val isPositive = mov.quantityDelta > 0
    val timeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(mov.timestamp))

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) Color(0x2600F5A0) else Color(0x26FF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) RgAccent else RgError,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = mov.productName, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${mov.reason} (${mov.reference}) • ${mov.userName} • $formattedTime",
                        color = RgTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}${mov.quantityDelta} ${mov.unitName}",
                    color = if (isPositive) RgAccent else RgError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stock now: ${mov.resultingStock.toInt()}",
                    color = RgTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StockTakeView(
    products: List<ProductEntity>,
    onAdjustStock: (Long, Double, String, String, () -> Unit) -> Unit
) {
    var selectedProd by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var physicalCountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Stock Take / Count Correction") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "PHYSICAL STOCK AUDIT",
                color = RgTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Audit actual shelf count vs recorded base inventory. Discrepancies are logged in the audit trail.",
                color = RgTextMuted,
                fontSize = 12.sp
            )
        }

        item {
            Text(text = "1. Select Product", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.height(140.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(products) { prod ->
                    val isSel = selectedProd?.id == prod.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0x3300F5A0) else RgSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedProd = prod
                            physicalCountText = prod.currentStock.toInt().toString()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prod.name, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "System: ${ConversionEngine.formatStock(prod.currentStock, prod.baseUnit)}", color = RgTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (selectedProd != null) {
            val prod = selectedProd!!
            val physicalCount = physicalCountText.toDoubleOrNull() ?: prod.currentStock
            val diff = physicalCount - prod.currentStock

            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RgSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "System Count:", color = RgTextSecondary, fontSize = 13.sp)
                            Text(text = "${prod.currentStock.toInt()} ${prod.baseUnit}s", color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Variance / Difference:", color = RgTextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${if (diff > 0) "+" else ""}${diff.toInt()} ${prod.baseUnit}s",
                                color = if (diff == 0.0) RgAccent else if (diff < 0) RgError else RgWarning,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            item {
                Text(text = "2. Enter Actual Physical Count (${prod.baseUnit}s)", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = physicalCountText,
                    onValueChange = { physicalCountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceCard,
                        unfocusedContainerColor = RgSurfaceCard
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stock_take_physical_input")
                )
            }

            item {
                Text(text = "3. Reason for Adjustment", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                val reasons = listOf("Stock Take / Count Correction", "Damaged Goods", "Expired Stock", "Loss / Theft")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { r ->
                        val isSel = reason == r
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.fillMaxWidth().clickable { reason = r }
                        ) {
                            Text(
                                text = r,
                                color = if (isSel) RgAccent else RgTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                RgButton(
                    text = "Commit Stock Adjustment",
                    onClick = {
                        val count = physicalCountText.toDoubleOrNull() ?: prod.currentStock
                        onAdjustStock(prod.id, count, reason, notes) {}
                    },
                    modifier = Modifier.fillMaxWidth().testTag("stock_take_commit")
                )
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustmentModal(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var actualCountText by remember { mutableStateOf(product.currentStock.toInt().toString()) }
    var reason by remember { mutableStateOf("Count Correction") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = "Adjust Stock: ${product.name}", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Current Recorded: ${product.currentStock.toInt()} ${product.baseUnit}s",
                color = RgAccent,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Actual Physical Count (${product.baseUnit})", color = RgTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = actualCountText,
                onValueChange = { actualCountText = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = RgCardBorder,
                    focusedTextColor = RgTextPrimary,
                    unfocusedTextColor = RgTextPrimary,
                    cursorColor = RgAccent,
                    focusedContainerColor = RgSurfaceVariant,
                    unfocusedContainerColor = RgSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "Reason", color = RgTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = RgCardBorder,
                    focusedTextColor = RgTextPrimary,
                    unfocusedTextColor = RgTextPrimary,
                    cursorColor = RgAccent,
                    focusedContainerColor = RgSurfaceVariant,
                    unfocusedContainerColor = RgSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            RgButton(
                text = "Save Adjustment",
                onClick = {
                    val count = actualCountText.toDoubleOrNull() ?: product.currentStock
                    onConfirm(count, reason, notes)
                },
                modifier = Modifier.fillMaxWidth().testTag("modal_save_adjustment")
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
