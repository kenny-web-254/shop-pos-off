package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RgDarkColorScheme = darkColorScheme(
    primary = RgAccent,
    onPrimary = Color(0xFF041810),
    primaryContainer = RgSurfaceVariant,
    onPrimaryContainer = RgAccent,
    secondary = RgAccentVariant,
    onSecondary = Color(0xFF041810),
    secondaryContainer = RgSurfaceCard,
    onSecondaryContainer = RgTextPrimary,
    tertiary = RgInfo,
    onTertiary = Color.Black,
    background = RgBackground,
    onBackground = RgTextPrimary,
    surface = RgSurface,
    onSurface = RgTextPrimary,
    surfaceVariant = RgSurfaceVariant,
    onSurfaceVariant = RgTextSecondary,
    surfaceTint = RgAccent,
    outline = RgCardBorder,
    outlineVariant = Color(0xFF1E2838),
    error = RgError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Enforce RG POS signature dark aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RgDarkColorScheme,
        typography = Typography,
        content = content
    )
}
