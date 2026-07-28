package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartStockColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BlueContainer,
    onPrimaryContainer = BlueOnContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    background = GrayBackground,
    onBackground = GrayTextPrimary,
    surface = GraySurface,
    onSurface = GrayTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = GrayTextSecondary,
    outline = GrayOutline,
    error = RedDanger,
    errorContainer = RedDangerContainer
)

@Composable
fun SmartStockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartStockColorScheme,
        typography = Typography,
        content = content
    )
}

