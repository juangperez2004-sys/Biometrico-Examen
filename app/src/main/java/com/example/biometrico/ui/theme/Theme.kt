package com.example.biometrico.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SunsetColorScheme = darkColorScheme(
    primary        = SunsetOrange,
    secondary      = SunsetMagenta,
    tertiary       = SunsetPink,
    background     = SunsetBackground,
    surface        = SunsetSurface,
    surfaceVariant = SunsetCard,
    onPrimary      = SunsetWhite,
    onSecondary    = SunsetWhite,
    onBackground   = SunsetWhite,
    onSurface      = SunsetWhite,
    onSurfaceVariant = SunsetWhite70,
)

@Composable
fun BiometricoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SunsetColorScheme,
        typography  = Typography,
        content     = content
    )
}