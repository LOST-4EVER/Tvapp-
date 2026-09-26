package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KurdishDarkColorScheme = darkColorScheme(
    primary = KurdishSunGold,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = KurdishRed,
    onSecondary = Color.White,
    tertiary = KurdishGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun KurdishTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KurdishDarkColorScheme,
        typography = Typography,
        content = content
    )
}
