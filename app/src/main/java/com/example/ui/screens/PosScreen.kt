package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.BusinessEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.HeldSaleEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.UnitConversionEntity
import com.example.data.repository.CartItem
import com.example.data.repository.ConversionEngine
import com.example.data.repository.SaleFullDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RgButton
import com.example.ui.components.RgCard
import com.example.ui.components.formatKes
import com.example.ui.theme.RgAccent
import com.example.ui.theme.RgAccentGlow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    business: BusinessEntity?,
    isOnline: Boolean,
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    conversions: List<UnitConversionEntity>,
    customers: List<CustomerEntity>,
    cartItems: List<CartItem>,
    isWholesale: Boolean,
    selectedCustomer: CustomerEntity?,
    heldSales: List<HeldSaleEntity>,
    cartDiscount: Double,
    onToggleWholesale: (Boolean) -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onAddToCart: (ProductEntity, String, Double, List<UnitConversionEntity>) -> Unit,
    onUpdateCartQty: (Int, Double) -> Unit,
    onUpdateCartPrice: (Int, Double) -> Unit,
    onRemoveCartItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onSetDiscount: (Double) -> Unit,
    onHoldSale: () -> Unit,
    onResumeHeldSale: (HeldSaleEntity) -> Unit,
    onDeleteHeldSale: (Long) -> Unit,
    onCompleteSale: (String, Double, Double, String, Double, Double, Double, (SaleFullDetails) -> Unit, (String) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    // Dialog & Sheet States
    var productForSelection by remember { mutableStateOf<ProductEntity?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showHeldSalesSheet by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var completedSaleDetails by remember { mutableStateOf<SaleFullDetails?>(null) }
    var checkoutErrorMessage by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategoryId == null || p.categoryId == selectedCategoryId
            matchesQuery && matchesCat
        }
    }

    val cartTotalCount by remember(cartItems) {
        derivedStateOf { cartItems.sumOf { it.quantity } }
    }
    val cartSubtotal by remember(cartItems) {
        derivedStateOf { cartItems.sumOf { it.subtotal } }
    }
    val cartGrandTotal by remember(cartSubtotal, cartDiscount) {
        derivedStateOf { (cartSubtotal - cartDiscount).coerceAtLeast(0.0) }
    }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Controls Bar (Part 8)
            Surface(
                color = RgSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Retail / Wholesale Mode Toggle (Part 8 & 18)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isWholesale) Color(0x3364B5F6) else Color(0x2600F5A0),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isWholesale) Color(0xFF64B5F6) else RgAccent
                            ),
                            modifier = Modifier.clickable { onToggleWholesale(!isWholesale) }.testTag("wholesale_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isWholesale) Color(0xFF64B5F6) else RgAccent)
                                )
                                Text(
                                    text = if (isWholesale) "WHOLESALE MODE" else "RETAIL MODE",
                                    color = if (isWholesale) Color(0xFF64B5F6) else RgAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Held Sales & Cart Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (heldSales.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33FFB74D),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RgWarning),
                                    modifier = Modifier.clickable { showHeldSalesSheet = true }.testTag("held_sales_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Pause,
                                            contentDescription = null,
                                            tint = RgWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Held (${heldSales.size})",
                                            color = RgWarning,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (cartItems.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RgSurfaceVariant,
                                    modifier = Modifier.clickable { onClearCart() }
                                ) {
                                    Text(
                                        text = "Clear",
                                        color = RgTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, SKU or barcode...", color = RgTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = RgTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear", tint = RgTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = RgCardBorder,
                            focusedTextColor = RgTextPrimary,
                            unfocusedTextColor = RgTextPrimary,
                            cursorColor = RgAccent,
                            focusedContainerColor = RgSurfaceVariant,
                            unfocusedContainerColor = RgSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }

            // 2. Categories Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        name = "All Items",
                        isSelected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null }
                    )
                }
                items(categories) { cat ->
                    CategoryChip(
                        name = cat.name,
                        isSelected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id }
                    )
                }
            }

            // 3. Products Grid / List (Part 8)
            Box(modifier = Modifier.weight(1f)) {
                if (filteredProducts.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = "No products found",
                        subtitle = if (searchQuery.isNotBlank()) "No matching results for '$searchQuery'" else "Add products in Inventory to start selling."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            val prodConversions = conversions.filter { it.productId == product.id }
                            ProductPosCard(
                                product = product,
                                isWholesale = isWholesale,
                                hasConversions = prodConversions.isNotEmpty(),
                                onClick = {
                                    productForSelection = product
                                }
                            )
                        }
                    }
                }

                // 4. Floating Cart Dock Bar
                if (cartItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xF216202C),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, RgAccent),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 78.dp)
                            .clickable { showCartSheet = true }
                            .testTag("pos_bottom_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RgAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color(0xFF041810),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${cartItems.size} items in cart",
                                        color = RgTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedCustomer != null) "Customer: ${selectedCustomer.name}" else "Walk-in Customer",
                                        color = RgTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatKes(cartGrandTotal),
                                        color = RgAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Tap to Charge",
                                        color = RgTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet 1: Product Unit & Quantity Selector (Part 9)
    if (productForSelection != null) {
        val prod = productForSelection!!
        val prodConversions = conversions.filter { it.productId == prod.id }

        ProductSelectionModal(
            product = prod,
            isWholesale = isWholesale,
            conversions = prodConversions,
            onDismiss = { productForSelection = null },
            onConfirmAdd = { unitName, qty ->
                onAddToCart(prod, unitName, qty, prodConversions)
                productForSelection = null
            }
        )
    }

    // Modal Sheet 2: Cart Sheet / Review (Part 10)
    if (showCartSheet) {
        CartReviewModal(
            cartItems = cartItems,
            isWholesale = isWholesale,
            selectedCustomer = selectedCustomer,
            cartDiscount = cartDiscount,
            onDismiss = { showCartSheet = false },
            onUpdateQty = onUpdateCartQty,
            onUpdatePrice = onUpdateCartPrice,
            onRemoveItem = onRemoveCartItem,
            onSetDiscount = onSetDiscount,
            onSelectCustomerClick = { showCustomerPicker = true },
            onHoldSale = {
                onHoldSale()
                showCartSheet = false
            },
            onProceedToPay = {
                showCartSheet = false
                showPaymentSheet = true
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            currentSelected = selectedCustomer,
            onDismiss = { showCustomerPicker = false },
            onSelect = { cust ->
                onSelectCustomer(cust)
                showCustomerPicker = false
            }
        )
    }

    // Modal Sheet 3: Payment Sheet (Part 11 & 19)
    if (showPaymentSheet) {
        PaymentModal(
            totalDue = cartGrandTotal,
            selectedCustomer = selectedCustomer,
            onDismiss = {
                showPaymentSheet = false
                checkoutErrorMessage = null
            },
            errorMessage = checkoutErrorMessage,
            onConfirmPayment = { method, cashRec, change, mpesaRef, pCash, pMpesa, pCredit ->
                checkoutErrorMessage = null
                onCompleteSale(
                    method, cashRec, change, mpesaRef, pCash, pMpesa, pCredit,
                    { details ->
                        showPaymentSheet = false
                        completedSaleDetails = details
                    },
                    { err ->
                        checkoutErrorMessage = err
                    }
                )
            }
        )
    }

    // Held Sales Modal
    if (showHeldSalesSheet) {
        HeldSalesModal(
            heldSales = heldSales,
            onDismiss = { showHeldSalesSheet = false },
            onResume = { sale ->
                onResumeHeldSale(sale)
                showHeldSalesSheet = false
            },
            onDelete = onDeleteHeldSale
        )
    }

    // Sale Completed / Receipt Modal (Part 12 & 13)
    if (completedSaleDetails != null) {
        SaleCompletedModal(
            saleDetails = completedSaleDetails!!,
            business = business,
            onDismiss = { completedSaleDetails = null }
        )
    }
}

@Composable
private fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) RgAccent else RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RgAccent else RgCardBorder
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            color = if (isSelected) Color(0xFF041810) else RgTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ProductPosCard(
    product: ProductEntity,
    isWholesale: Boolean,
    hasConversions: Boolean,
    onClick: () -> Unit
) {
    val price = if (isWholesale) product.wholesalePrice else product.retailPrice
    val isOutOfStock = product.currentStock <= 0
    val isLowStock = !isOutOfStock && product.currentStock <= product.minStock

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOutOfStock) RgError.copy(alpha = 0.4f) else RgCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.sku}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.categoryName,
                        color = RgTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasConversions) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x3300F5A0)
                        ) {
                            Text(
                                text = "MULTI-UNIT",
                                color = RgAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.name,
                    color = RgTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = formatKes(price),
                    color = RgAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Stock in BASE UNIT (Part 8 & 39)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isOutOfStock -> RgError
                                    isLowStock -> RgWarning
                                    else -> Color(0xFF00F5A0)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ConversionEngine.formatStock(product.currentStock, product.baseUnit),
                        color = when {
                            isOutOfStock -> RgError
                            isLowStock -> RgWarning
                            else -> RgTextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionModal(
    product: ProductEntity,
    isWholesale: Boolean,
    conversions: List<UnitConversionEntity>,
    onDismiss: () -> Unit,
    onConfirmAdd: (String, Double) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(product.baseUnit) }
    var quantityText by remember { mutableStateOf("1") }

    val allUnitOptions = remember(product, conversions) {
        listOf(product.baseUnit) + conversions.map { it.unitName }
    }

    val multiplier = remember(selectedUnit, product, conversions) {
        ConversionEngine.getMultiplierToBase(selectedUnit, product.baseUnit, conversions)
    }

    val unitPrice = remember(selectedUnit, isWholesale, product, conversions) {
        ConversionEngine.calculateUnitPrice(selectedUnit, isWholesale, product, conversions)
    }

    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val totalDeductionInBase = qty * multiplier
    val lineSubtotal = qty * unitPrice

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RgCardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.name, color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Stock: ${ConversionEngine.formatStock(product.currentStock, product.baseUnit)}",
                        color = RgTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (isWholesale) "WHOLESALE" else "RETAIL",
                    color = if (isWholesale) Color(0xFF64B5F6) else RgAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unit Selector Chips
            Text(text = "SELECT UNIT", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allUnitOptions) { unitOption ->
                    val isSel = unitOption.equals(selectedUnit, ignoreCase = true)
                    val priceForUnit = ConversionEngine.calculateUnitPrice(unitOption, isWholesale, product, conversions)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                        modifier = Modifier.clickable { selectedUnit = unitOption }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = unitOption,
                                color = if (isSel) RgAccent else RgTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatKes(priceForUnit),
                                color = RgTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Base Unit Conversion Preview (Part 9 & 39)
            if (selectedUnit != product.baseUnit) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x1A00F5A0),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F5A0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Conversion: 1 $selectedUnit = ${multiplier.toInt()} ${product.baseUnit}s\nSelling $qty $selectedUnit will deduct ${totalDeductionInBase.toInt()} ${product.baseUnit}s from stock.",
                        color = RgAccent,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quantity Stepper
            Text(text = "QUANTITY", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val current = quantityText.toDoubleOrNull() ?: 1.0
                        if (current > 1) quantityText = (current - 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(RgSurfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease", tint = RgTextPrimary)
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                IconButton(
                    onClick = {
                        val current = quantityText.toDoubleOrNull() ?: 1.0
                        quantityText = (current + 1).toString()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(RgSurfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase", tint = RgTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtotal & Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Total Price", color = RgTextSecondary, fontSize = 11.sp)
                    Text(text = formatKes(lineSubtotal), color = RgAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                RgButton(
                    text = "Add to Cart",
                    onClick = {
                        onConfirmAdd(selectedUnit, qty)
                    },
                    modifier = Modifier.width(180.dp).testTag("modal_add_to_cart_confirm")
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartReviewModal(
    cartItems: List<CartItem>,
    isWholesale: Boolean,
    selectedCustomer: CustomerEntity?,
    cartDiscount: Double,
    onDismiss: () -> Unit,
    onUpdateQty: (Int, Double) -> Unit,
    onUpdatePrice: (Int, Double) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSetDiscount: (Double) -> Unit,
    onSelectCustomerClick: () -> Unit,
    onHoldSale: () -> Unit,
    onProceedToPay: () -> Unit
) {
    val subtotal = cartItems.sumOf { it.subtotal }
    val total = (subtotal - cartDiscount).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RgCardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Customer Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RgSurfaceVariant, RoundedCornerShape(10.dp))
                    .clickable { onSelectCustomerClick() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = RgAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = selectedCustomer?.name ?: "Walk-in Customer",
                            color = RgTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCustomer != null) {
                            Text(
                                text = "Credit Limit: ${formatKes(selectedCustomer.creditLimit)} • Owed: ${formatKes(selectedCustomer.outstandingCredit)}",
                                color = if (selectedCustomer.outstandingCredit > 0) RgWarning else RgTextSecondary,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(text = "Tap to assign registered customer", color = RgTextMuted, fontSize = 11.sp)
                        }
                    }
                }
                Text(text = "Change", color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartItems.size) { index ->
                    val item = cartItems[index]
                    CartItemRow(
                        item = item,
                        onIncrease = { onUpdateQty(index, item.quantity + 1) },
                        onDecrease = { onUpdateQty(index, item.quantity - 1) },
                        onRemove = { onRemoveItem(index) },
                        onPriceChange = { newPrice -> onUpdatePrice(index, newPrice) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Totals and Discount
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RgSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Subtotal", color = RgTextSecondary, fontSize = 12.sp)
                        Text(text = formatKes(subtotal), color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (cartDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Discount", color = RgWarning, fontSize = 12.sp)
                            Text(text = "- ${formatKes(cartDiscount)}", color = RgWarning, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RgCardBorder))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Total Payable", color = RgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = formatKes(total), color = RgAccent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RgButton(
                    text = "Hold Sale",
                    onClick = onHoldSale,
                    isPrimary = false,
                    icon = Icons.Filled.Pause,
                    modifier = Modifier.weight(1f).testTag("cart_hold_button")
                )
                RgButton(
                    text = "Pay ${formatKes(total)}",
                    onClick = onProceedToPay,
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(2f).testTag("cart_pay_button")
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onPriceChange: (Double) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RgSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.product.name, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Unit: ${item.unitName} (${formatKes(item.unitPrice)} each)",
                        color = RgTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove", tint = RgError.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(30.dp).background(RgSurfaceCard, RoundedCornerShape(6.dp))
                    ) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease", tint = RgTextPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity}",
                        color = RgTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(30.dp).background(RgSurfaceCard, RoundedCornerShape(6.dp))
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase", tint = RgTextPrimary, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = formatKes(item.subtotal),
                    color = RgAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentModal(
    totalDue: Double,
    selectedCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    errorMessage: String?,
    onConfirmPayment: (String, Double, Double, String, Double, Double, Double) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Cash") } // "Cash", "M-Pesa", "Credit", "Partial"

    // Cash state
    var cashReceivedText by remember { mutableStateOf(totalDue.toInt().toString()) }
    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val change = (cashReceived - totalDue).coerceAtLeast(0.0)

    // Mpesa state
    var mpesaRef by remember { mutableStateOf("") }

    // Partial payment state
    var partialCashText by remember { mutableStateOf("") }
    var partialMpesaText by remember { mutableStateOf("") }
    var partialCreditText by remember { mutableStateOf("") }

    val partialCash = partialCashText.toDoubleOrNull() ?: 0.0
    val partialMpesa = partialMpesaText.toDoubleOrNull() ?: 0.0
    val partialCredit = partialCreditText.toDoubleOrNull() ?: 0.0
    val partialSum = partialCash + partialMpesa + partialCredit
    val partialBalance = (totalDue - partialSum)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RgCardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "TOTAL AMOUNT DUE",
                color = RgTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = formatKes(totalDue),
                color = RgAccent,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Method Selector Tabs (Cash, M-Pesa, Credit, Partial)
            val methods = listOf("Cash", "M-Pesa", "Credit", "Partial")
            TabRow(
                selectedTabIndex = methods.indexOf(selectedMethod),
                containerColor = RgSurfaceVariant,
                contentColor = RgAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[methods.indexOf(selectedMethod)]),
                        color = RgAccent
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                methods.forEach { method ->
                    Tab(
                        selected = selectedMethod == method,
                        onClick = { selectedMethod = method },
                        text = {
                            Text(
                                text = method,
                                fontSize = 12.sp,
                                fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Method Specific UI
            when (selectedMethod) {
                "Cash" -> {
                    Text(text = "Cash Received", color = RgTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = cashReceivedText,
                        onValueChange = { cashReceivedText = it },
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
                        modifier = Modifier.fillMaxWidth().testTag("payment_cash_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick cash presets (+Exact, 500, 1000, 2000)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Exact", "+100", "+500", "+1000").forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RgSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                                modifier = Modifier
                                    .clickable {
                                        when (preset) {
                                            "Exact" -> cashReceivedText = totalDue.toInt().toString()
                                            "+100" -> cashReceivedText = ((cashReceivedText.toDoubleOrNull() ?: totalDue) + 100).toInt().toString()
                                            "+500" -> cashReceivedText = ((cashReceivedText.toDoubleOrNull() ?: totalDue) + 500).toInt().toString()
                                            "+1000" -> cashReceivedText = ((cashReceivedText.toDoubleOrNull() ?: totalDue) + 1000).toInt().toString()
                                        }
                                    }
                            ) {
                                Text(
                                    text = preset,
                                    color = RgTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Change Calculation (Part 11)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RgSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Change Due", color = RgTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = formatKes(change),
                                color = if (cashReceived >= totalDue) RgAccent else RgError,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "M-Pesa" -> {
                    Text(text = "M-Pesa Reference Code", color = RgTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = mpesaRef,
                        onValueChange = { mpesaRef = it.uppercase() },
                        placeholder = { Text("e.g. QDH472910J", color = RgTextMuted, fontSize = 13.sp) },
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
                        modifier = Modifier.fillMaxWidth().testTag("payment_mpesa_ref")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Verify that customer confirmation SMS is received before completing.",
                        color = RgTextMuted,
                        fontSize = 11.sp
                    )
                }
                "Credit" -> {
                    if (selectedCustomer == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x1AFFB74D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFB74D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠ Credit sales require selecting a registered customer. Please assign customer in cart.",
                                color = RgWarning,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Customer: ${selectedCustomer.name}", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Credit Limit: ${formatKes(selectedCustomer.creditLimit)}", color = RgTextSecondary, fontSize = 11.sp)
                                Text(text = "Current Outstanding: ${formatKes(selectedCustomer.outstandingCredit)}", color = RgWarning, fontSize = 11.sp)
                                Text(
                                    text = "Balance after sale: ${formatKes(selectedCustomer.outstandingCredit + totalDue)}",
                                    color = RgAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                "Partial" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Split payment across multiple methods", color = RgTextSecondary, fontSize = 11.sp)
                        OutlinedTextField(
                            value = partialCashText,
                            onValueChange = { partialCashText = it },
                            label = { Text("Cash Portion (KSh)", color = RgTextMuted, fontSize = 11.sp) },
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

                        OutlinedTextField(
                            value = partialMpesaText,
                            onValueChange = { partialMpesaText = it },
                            label = { Text("M-Pesa Portion (KSh)", color = RgTextMuted, fontSize = 11.sp) },
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

                        OutlinedTextField(
                            value = partialCreditText,
                            onValueChange = { partialCreditText = it },
                            label = { Text("Credit Portion (KSh)", color = RgTextMuted, fontSize = 11.sp) },
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Remaining to allocate:", color = RgTextSecondary, fontSize = 12.sp)
                            Text(
                                text = formatKes(partialBalance),
                                color = if (Math.abs(partialBalance) < 0.01) RgAccent else RgError,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = errorMessage, color = RgError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            RgButton(
                text = "Confirm & Complete Sale",
                onClick = {
                    onConfirmPayment(
                        selectedMethod,
                        cashReceived,
                        change,
                        mpesaRef,
                        partialCash,
                        partialMpesa,
                        partialCredit
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("payment_confirm_button")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    currentSelected: CustomerEntity?,
    onDismiss: () -> Unit,
    onSelect: (CustomerEntity?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RgSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
            modifier = Modifier.fillMaxWidth().height(450.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Select Customer", color = RgTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Option for Walk-in Customer
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentSelected == null) Color(0x3300F5A0) else RgSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (currentSelected == null) RgAccent else RgCardBorder),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }
                ) {
                    Text(
                        text = "Walk-in Customer (No Credit)",
                        color = if (currentSelected == null) RgAccent else RgTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers) { cust ->
                        val isSel = currentSelected?.id == cust.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(cust) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = cust.name,
                                    color = if (isSel) RgAccent else RgTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cust.phone} • Limit: ${formatKes(cust.creditLimit)} • Owed: ${formatKes(cust.outstandingCredit)}",
                                    color = RgTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeldSalesModal(
    heldSales: List<HeldSaleEntity>,
    onDismiss: () -> Unit,
    onResume: (HeldSaleEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Held Sales (${heldSales.size})", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Tap a sale to resume it into the cart", color = RgTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(heldSales) { held ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RgSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = held.customerName, color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(held.timestamp))
                                Text(
                                    text = "Held at $timeStr • ${formatKes(held.totalAmount)}",
                                    color = RgAccent,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RgButton(text = "Resume", onClick = { onResume(held) }, modifier = Modifier.height(36.dp))
                                IconButton(onClick = { onDelete(held.id) }, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = RgError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SaleCompletedModal(
    saleDetails: SaleFullDetails,
    business: BusinessEntity?,
    onDismiss: () -> Unit
) {
    val sale = saleDetails.sale
    val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = timeFormat.format(Date(sale.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = RgSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
            modifier = Modifier.fillMaxWidth().height(580.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Success Header
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x2600F5A0))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = RgAccent, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sale Completed",
                        color = RgTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Digital Receipt Slip Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RgBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = business?.name ?: "RG POS Store",
                                color = RgTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = business?.phone ?: "",
                                color = RgTextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${sale.receiptNumber} • $formattedDate",
                                color = RgTextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cashier: ${sale.cashierName} • Cust: ${sale.customerName}",
                                color = RgTextSecondary,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RgCardBorder))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Items
                            saleDetails.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.productName, color = RgTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "${item.quantity} ${item.unitName} @ ${formatKes(item.unitPrice)}",
                                            color = RgTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(text = formatKes(item.subtotal), color = RgTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RgCardBorder))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtotal / Discount / Total
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Subtotal", color = RgTextSecondary, fontSize = 11.sp)
                                Text(text = formatKes(sale.subtotal), color = RgTextPrimary, fontSize = 11.sp)
                            }
                            if (sale.discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Discount", color = RgWarning, fontSize = 11.sp)
                                    Text(text = "- ${formatKes(sale.discount)}", color = RgWarning, fontSize = 11.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "TOTAL", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = formatKes(sale.totalAmount), color = RgAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            // Payment breakdown
                            Spacer(modifier = Modifier.height(6.dp))
                            saleDetails.payments.forEach { p ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "${p.method} ${if (p.reference.isNotBlank()) "(${p.reference})" else ""}", color = RgTextMuted, fontSize = 10.sp)
                                    Text(text = formatKes(p.amount), color = RgTextSecondary, fontSize = 10.sp)
                                }
                            }
                            if (sale.changeGiven > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Change Given", color = RgTextMuted, fontSize = 10.sp)
                                    Text(text = formatKes(sale.changeGiven), color = RgTextSecondary, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = business?.receiptFooter ?: "Thank you for shopping with us.",
                                color = RgTextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Modal Actions
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RgButton(
                        text = "Close",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.weight(1f).testTag("receipt_close_button")
                    )
                    RgButton(
                        text = "New Sale",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("receipt_new_sale_button")
                    )
                }
            }
        }
    }
}
