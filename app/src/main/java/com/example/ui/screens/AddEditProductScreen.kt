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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.UnitConversionEntity
import com.example.ui.components.RgButton
import com.example.ui.components.RgCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    existingProduct: ProductEntity?,
    existingConversions: List<UnitConversionEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSave: (ProductEntity, List<UnitConversionEntity>) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var sku by remember { mutableStateOf(existingProduct?.sku ?: "SKU-${(System.currentTimeMillis() % 9000) + 1000}") }
    var barcode by remember { mutableStateOf(existingProduct?.barcode ?: "") }
    var baseUnit by remember { mutableStateOf(existingProduct?.baseUnit ?: "Piece") }
    var categoryId by remember { mutableStateOf(existingProduct?.categoryId ?: (categories.firstOrNull()?.id ?: 1L)) }
    var categoryName by remember { mutableStateOf(existingProduct?.categoryName ?: (categories.firstOrNull()?.name ?: "General")) }

    var buyingCostText by remember { mutableStateOf(existingProduct?.buyingCost?.toString() ?: "0.0") }
    var retailPriceText by remember { mutableStateOf(existingProduct?.retailPrice?.toString() ?: "0.0") }
    var wholesalePriceText by remember { mutableStateOf(existingProduct?.wholesalePrice?.toString() ?: "0.0") }
    var allowWholesale by remember { mutableStateOf(existingProduct?.allowWholesale ?: true) }
    var minStockText by remember { mutableStateOf((existingProduct?.minStock ?: 10).toString()) }
    var initialStockText by remember { mutableStateOf((existingProduct?.currentStock ?: 0.0).toString()) }

    var conversionsList by remember { mutableStateOf(existingConversions.toMutableList()) }
    var showAddConversionModal by remember { mutableStateOf(false) }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RgTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingProduct == null) "New Product" else "Edit Product",
                    color = RgTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Info Card
            RgCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "PRODUCT DETAILS", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedInputField("Product Name", name) { name = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedInputField("SKU", sku, modifier = Modifier.weight(1f)) { sku = it }
                        OutlinedInputField("Base Unit", baseUnit, modifier = Modifier.weight(1f)) { baseUnit = it }
                    }
                    OutlinedInputField("Barcode / Scan Code", barcode) { barcode = it }

                    // Category Selector
                    Text(text = "Category", color = RgTextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = categoryId == cat.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                                modifier = Modifier.clickable {
                                    categoryId = cat.id
                                    categoryName = cat.name
                                }
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (isSel) RgAccent else RgTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pricing & Costing
            RgCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "PRICING (PER BASE UNIT: $baseUnit)", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedInputField("Buying Cost (KSh)", buyingCostText, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { buyingCostText = it }
                        OutlinedInputField("Retail Price (KSh)", retailPriceText, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { retailPriceText = it }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Allow Wholesale Selling", color = RgTextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = allowWholesale,
                            onCheckedChange = { allowWholesale = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041810), checkedTrackColor = RgAccent)
                        )
                    }

                    if (allowWholesale) {
                        OutlinedInputField("Wholesale Price (KSh)", wholesalePriceText, keyboardType = KeyboardType.Decimal) { wholesalePriceText = it }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inventory & Thresholds
            RgCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "STOCK THRESHOLDS", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedInputField("Initial Stock ($baseUnit)", initialStockText, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { initialStockText = it }
                        OutlinedInputField("Low-Stock Alert", minStockText, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)) { minStockText = it }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unit Conversions Table (Part 18)
            RgCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "UNIT CONVERSIONS", color = RgTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "e.g. Carton = 72 $baseUnit, Dozen = 12 $baseUnit", color = RgTextMuted, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RgAccent),
                            modifier = Modifier.clickable { showAddConversionModal = true }.testTag("add_conversion_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = RgAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Add Unit", color = RgAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (conversionsList.isEmpty()) {
                        Text(
                            text = "No conversion units added. Item sells only in base unit ($baseUnit).",
                            color = RgTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        conversionsList.forEachIndexed { idx, conv ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RgSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "1 ${conv.unitName} = ${conv.multiplierToBase.toInt()} ${baseUnit}s",
                                            color = RgTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Retail: ${formatKes(if (conv.retailPriceOverride > 0) conv.retailPriceOverride else (retailPriceText.toDoubleOrNull() ?: 0.0) * conv.multiplierToBase)} • Wholesale: ${formatKes(if (conv.wholesalePriceOverride > 0) conv.wholesalePriceOverride else (wholesalePriceText.toDoubleOrNull() ?: 0.0) * conv.multiplierToBase)}",
                                            color = RgTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = conversionsList.toMutableList()
                                            updated.removeAt(idx)
                                            conversionsList = updated
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = RgError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            RgButton(
                text = "Save Product",
                onClick = {
                    val buyingCost = buyingCostText.toDoubleOrNull() ?: 0.0
                    val retailPrice = retailPriceText.toDoubleOrNull() ?: 0.0
                    val wholesalePrice = wholesalePriceText.toDoubleOrNull() ?: 0.0
                    val minStock = minStockText.toIntOrNull() ?: 10
                    val initialStock = initialStockText.toDoubleOrNull() ?: 0.0

                    val product = (existingProduct ?: ProductEntity(
                        name = name.ifBlank { "Untitled Item" },
                        baseUnit = baseUnit
                    )).copy(
                        name = name.ifBlank { "Untitled Item" },
                        sku = sku,
                        barcode = barcode,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        baseUnit = baseUnit,
                        buyingCost = buyingCost,
                        retailPrice = retailPrice,
                        wholesalePrice = wholesalePrice,
                        allowWholesale = allowWholesale,
                        minStock = minStock,
                        currentStock = if (existingProduct == null) initialStock else existingProduct.currentStock
                    )

                    onSave(product, conversionsList)
                },
                modifier = Modifier.fillMaxWidth().testTag("save_product_button")
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal: Add Unit Conversion
    if (showAddConversionModal) {
        AddConversionModal(
            baseUnit = baseUnit,
            baseRetailPrice = retailPriceText.toDoubleOrNull() ?: 0.0,
            baseWholesalePrice = wholesalePriceText.toDoubleOrNull() ?: 0.0,
            onDismiss = { showAddConversionModal = false },
            onAdd = { conv ->
                conversionsList = (conversionsList + conv).toMutableList()
                showAddConversionModal = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddConversionModal(
    baseUnit: String,
    baseRetailPrice: Double,
    baseWholesalePrice: Double,
    onDismiss: () -> Unit,
    onAdd: (UnitConversionEntity) -> Unit
) {
    var unitName by remember { mutableStateOf("Carton") }
    var multiplierText by remember { mutableStateOf("12") }
    var retailOverrideText by remember { mutableStateOf("") }
    var wholesaleOverrideText by remember { mutableStateOf("") }

    val multiplier = multiplierText.toDoubleOrNull() ?: 12.0
    val calcRetail = multiplier * baseRetailPrice
    val calcWholesale = multiplier * baseWholesalePrice

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Add Unit Conversion", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Define higher unit of sale mapped to $baseUnit", color = RgTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedInputField("Unit Name (e.g. Carton, Dozen, Bale, Crate)", unitName) { unitName = it }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedInputField("Multiplier to Base Unit (How many ${baseUnit}s in 1 $unitName?)", multiplierText, keyboardType = KeyboardType.Number) { multiplierText = it }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedInputField(
                label = "Retail Price Override (Optional, Auto: ${formatKes(calcRetail)})",
                value = retailOverrideText,
                keyboardType = KeyboardType.Decimal
            ) { retailOverrideText = it }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedInputField(
                label = "Wholesale Price Override (Optional, Auto: ${formatKes(calcWholesale)})",
                value = wholesaleOverrideText,
                keyboardType = KeyboardType.Decimal
            ) { wholesaleOverrideText = it }

            Spacer(modifier = Modifier.height(18.dp))
            RgButton(
                text = "Add Conversion",
                onClick = {
                    val entity = UnitConversionEntity(
                        productId = 0L,
                        unitName = unitName.trim().ifBlank { "Carton" },
                        multiplierToBase = multiplier,
                        retailPriceOverride = retailOverrideText.toDoubleOrNull() ?: 0.0,
                        wholesalePriceOverride = wholesaleOverrideText.toDoubleOrNull() ?: 0.0
                    )
                    onAdd(entity)
                },
                modifier = Modifier.fillMaxWidth().testTag("modal_confirm_add_conversion")
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OutlinedInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = RgTextMuted, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
        modifier = modifier.fillMaxWidth()
    )
}
