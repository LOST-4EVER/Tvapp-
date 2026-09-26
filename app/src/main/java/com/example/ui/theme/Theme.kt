package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.motion.LocalReduceMotion

@Composable
fun KurdishTvTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val colors = remember(settings) { appColorsFor(settings) }
    val colorScheme = remember(colors) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.primary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.surfaceVariant,
            onSecondaryContainer = colors.textPrimary,
            tertiary = colors.tertiary,
            onTertiary = Color.Black,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surfaceElevated,
            outline = colors.border,
            error = KurdishRed,
            onError = Color.White
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalReduceMotion provides settings.reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
