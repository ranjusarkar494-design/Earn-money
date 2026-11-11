package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LudoDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF2A1500),
    primaryContainer = GoldDark,
    onPrimaryContainer = Color.White,
    secondary = RoyalPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = RoyalPurpleGlow,
    onSecondaryContainer = Color.White,
    tertiary = GoldLight,
    onTertiary = Color.Black,
    background = RoyalPurpleDark,
    onBackground = TextPrimary,
    surface = SurfaceCardDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondary
)

@Composable
fun LudoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LudoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
