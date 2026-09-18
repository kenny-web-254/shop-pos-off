package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BusinessEntity
import com.example.ui.components.RgButton
import com.example.ui.components.RgCard
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

@Composable
fun SetupScreen(
    onFinishSetup: (BusinessEntity) -> Unit,
    onRestoreBackupClick: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    // Step 1 State: Business Info
    var businessName by remember { mutableStateOf("Nairobi Retail & Wholesale") }
    var businessPhone by remember { mutableStateOf("0712 345 678") }
    var businessLocation by remember { mutableStateOf("Biashara Street, Nairobi") }
    var ownerName by remember { mutableStateOf("Kamau Mwangi") }
    val currency by remember { mutableStateOf("KES — Kenyan Shilling") }

    // Step 2 State: Preferences
    var allowRetail by remember { mutableStateOf(true) }
    var allowWholesale by remember { mutableStateOf(true) }
    var allowCredit by remember { mutableStateOf(true) }
    var lowStockThreshold by remember { mutableStateOf("5") }
    var taxRate by remember { mutableStateOf("0") }
    var receiptHeader by remember { mutableStateOf("RG POS Store") }
    var receiptFooter by remember { mutableStateOf("Thank you for shopping with us.") }

    // Step 4 State: PIN
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var allowBiometric by remember { mutableStateOf(true) }
    var pinError by remember { mutableStateOf<String?>(null) }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Setup Step Progress Indicators
            if (step > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..4) {
                        val isDone = step >= i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDone) RgAccent else RgSurfaceVariant)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setup_step",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    0 -> {
                        // Screen: Welcome
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RgSurfaceVariant)
                                    .border(1.dp, RgAccent, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PointOfSale,
                                    contentDescription = null,
                                    tint = RgAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                            Text(
                                text = "Welcome to RG POS",
                                color = RgTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your business. Your data. Your POS. Even offline.",
                                color = RgTextSecondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                            RgButton(
                                text = "Get Started",
                                onClick = { step = 1 },
                                modifier = Modifier.fillMaxWidth().testTag("setup_get_started")
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            RgButton(
                                text = "Restore Backup",
                                onClick = onRestoreBackupClick,
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth().testTag("setup_restore_backup")
                            )
                        }
                    }
                    1 -> {
                        // Screen: Business Information
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Business Information",
                                color = RgTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter details about your Kenyan business",
                                color = RgTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            RgTextField(label = "Business name", value = businessName, onValueChange = { businessName = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Business phone", value = businessPhone, onValueChange = { businessPhone = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Business location", value = businessLocation, onValueChange = { businessLocation = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Owner name", value = ownerName, onValueChange = { ownerName = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Currency", value = currency, onValueChange = {}, readOnly = true)

                            Spacer(modifier = Modifier.height(28.dp))
                            RgButton(
                                text = "Continue",
                                onClick = {
                                    if (businessName.isNotBlank()) step = 2
                                },
                                modifier = Modifier.fillMaxWidth().testTag("setup_step1_continue")
                            )
                        }
                    }
                    2 -> {
                        // Screen: Business Preferences
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Business Preferences",
                                color = RgTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure retail, wholesale and receipt defaults",
                                color = RgTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            PreferenceToggle("Retail selling", "Sell goods in piece/retail quantities", allowRetail) { allowRetail = it }
                            PreferenceToggle("Wholesale selling", "Support cartons, bulk pricing & discounts", allowWholesale) { allowWholesale = it }
                            PreferenceToggle("Allow credit sales", "Track customer credit ledger & limits", allowCredit) { allowCredit = it }

                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(
                                label = "Low-stock threshold",
                                value = lowStockThreshold,
                                onValueChange = { lowStockThreshold = it.filter { ch -> ch.isDigit() } },
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(
                                label = "Tax VAT % (e.g. 0 or 16)",
                                value = taxRate,
                                onValueChange = { taxRate = it },
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Receipt header", value = receiptHeader, onValueChange = { receiptHeader = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(label = "Receipt footer", value = receiptFooter, onValueChange = { receiptFooter = it })

                            Spacer(modifier = Modifier.height(28.dp))
                            RgButton(
                                text = "Continue",
                                onClick = { step = 3 },
                                modifier = Modifier.fillMaxWidth().testTag("setup_step2_continue")
                            )
                        }
                    }
                    3 -> {
                        // Screen: Payment Methods
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Payment Methods",
                                color = RgTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard Kenyan commercial payment channels",
                                color = RgTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            PaymentMethodCard("Cash", "Standard cash transactions with automatic change calculation")
                            PaymentMethodCard("M-Pesa", "Direct mobile money payments with reference code verification")
                            PaymentMethodCard("Partial Payment", "Split payment across Cash, M-Pesa and Credit in one transaction")

                            Spacer(modifier = Modifier.height(28.dp))
                            RgButton(
                                text = "Continue",
                                onClick = { step = 4 },
                                modifier = Modifier.fillMaxWidth().testTag("setup_step3_continue")
                            )
                        }
                    }
                    4 -> {
                        // Screen: Create Owner PIN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Create Owner PIN",
                                color = RgTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Set up a 4-digit PIN for system access and management authorization",
                                color = RgTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            RgTextField(
                                label = "Owner PIN (4 digits)",
                                value = pin,
                                onValueChange = { if (it.length <= 4) pin = it },
                                keyboardType = KeyboardType.NumberPassword,
                                isPassword = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RgTextField(
                                label = "Confirm PIN",
                                value = confirmPin,
                                onValueChange = { if (it.length <= 4) confirmPin = it },
                                keyboardType = KeyboardType.NumberPassword,
                                isPassword = true
                            )

                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pinError!!,
                                    color = RgError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            PreferenceToggle(
                                title = "Biometric authentication",
                                subtitle = "Allow fingerprint unlock if supported by device",
                                checked = allowBiometric,
                                onCheckedChange = { allowBiometric = it }
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            RgButton(
                                text = "Finish Setup",
                                onClick = {
                                    if (pin.length != 4) {
                                        pinError = "PIN must be 4 digits"
                                        return@RgButton
                                    }
                                    if (pin != confirmPin) {
                                        pinError = "PINs do not match"
                                        return@RgButton
                                    }
                                    pinError = null
                                    val businessEntity = BusinessEntity(
                                        id = 1L,
                                        name = businessName.trim().ifBlank { "RG POS Store" },
                                        phone = businessPhone.trim(),
                                        location = businessLocation.trim(),
                                        ownerName = ownerName.trim().ifBlank { "Owner" },
                                        currency = "KES",
                                        allowRetail = allowRetail,
                                        allowWholesale = allowWholesale,
                                        allowCredit = allowCredit,
                                        lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 5,
                                        taxRatePercent = taxRate.toDoubleOrNull() ?: 0.0,
                                        receiptHeader = receiptHeader,
                                        receiptFooter = receiptFooter,
                                        ownerPin = pin,
                                        biometricEnabled = allowBiometric
                                    )
                                    onFinishSetup(businessEntity)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("setup_finish_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = RgTextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF041810),
                    checkedTrackColor = RgAccent,
                    uncheckedThumbColor = RgTextMuted,
                    uncheckedTrackColor = RgSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A00F5A0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = RgAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = RgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = RgTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RgTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = RgTextSecondary, fontSize = 13.sp) },
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RgAccent,
            unfocusedBorderColor = RgCardBorder,
            focusedTextColor = RgTextPrimary,
            unfocusedTextColor = RgTextPrimary,
            cursorColor = RgAccent,
            focusedContainerColor = RgSurfaceCard,
            unfocusedContainerColor = RgSurfaceCard
        ),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
