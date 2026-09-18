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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BusinessEntity
import com.example.ui.components.RgButton
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

@Composable
fun LoginScreen(
    business: BusinessEntity?,
    isOnline: Boolean,
    onLoginSuccess: () -> Unit,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun addDigit(digit: String) {
        if (enteredPin.length < 4) {
            errorMessage = null
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                onVerifyPin(newPin) { success ->
                    if (success) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Incorrect PIN"
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun removeDigit() {
        if (enteredPin.isNotEmpty()) {
            errorMessage = null
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Online/Offline Badge & Business Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOnline) Color(0x1A00F5A0) else Color(0x1AFFB74D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOnline) Color(0x3300F5A0) else Color(0x33FFB74D)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) RgAccent else RgWarning)
                            )
                            Text(
                                text = if (isOnline) "● Online" else "● Offline",
                                color = if (isOnline) RgAccent else RgWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RgSurfaceVariant)
                        .border(1.dp, RgAccent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PointOfSale,
                        contentDescription = null,
                        tint = RgAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RG POS",
                    color = RgTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = business?.name ?: "Business Terminal",
                    color = RgTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) RgAccent else RgSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isFilled) RgAccent else RgCardBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = RgError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )

                for (row in keypad) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (key) {
                                            "bio", "del" -> RgSurfaceVariant.copy(alpha = 0.6f)
                                            else -> RgSurfaceCard
                                        }
                                    )
                                    .border(1.dp, RgCardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (key) {
                                            "del" -> removeDigit()
                                            "bio" -> {
                                                // Quick biometric bypass / unlock
                                                onLoginSuccess()
                                            }
                                            else -> addDigit(key)
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "del" -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = RgTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    "bio" -> Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = RgAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    else -> Text(
                                        text = key,
                                        color = RgTextPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                RgButton(
                    text = "Unlock",
                    onClick = {
                        if (enteredPin.length == 4) {
                            onVerifyPin(enteredPin) { success ->
                                if (success) onLoginSuccess() else errorMessage = "Incorrect PIN"
                            }
                        } else {
                            errorMessage = "Enter 4-digit PIN"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("login_unlock_button")
                )
            }
        }
    }
}
