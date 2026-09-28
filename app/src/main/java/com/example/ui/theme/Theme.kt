package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.theme.AppShapes

@Composable
fun KurdishTvTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Material You (Android 12+): pull the accent from the user's wallpaper palette.
    // Falls back to the bundled accent on older releases or if extraction fails.
    val dynamicScheme = remember(settings, context) {
        if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { dynamicDarkColorScheme(context) }.getOrNull()
        } else {
            null
        }
    }

    val colors = remember(settings, dynamicScheme) { appColorsFor(settings, dynamicScheme) }
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
            // Picked by measured contrast rather than assumed to be a light tone.
            onTertiary = onColorFor(colors.tertiary),
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surfaceElevated,
            outline = colors.border,
            error = KurdishRed,
            onError = onColorFor(KurdishRed)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalReduceMotion provides settings.reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
