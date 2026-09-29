package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.theme.AppShapes

@Composable
fun KurdishTvTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Ten-foot UI. Read from the configuration rather than inferred from the window
    // size, because a TV box and a tablet are the same shape on paper.
    //
    // `uiMode` is in `MainActivity`'s `configChanges`, so a device that is docked
    // or undocked does not recreate the activity; reading the configuration here
    // (rather than caching the answer in a field) is what lets the value change.
    // Read into a local first: `remember`'s calculation lambda is a plain block,
    // so reading `LocalConfiguration.current` *inside* it is a composable call in
    // a non-composable context.
    //
    // The device-mode preference is the second key, and it is what makes the setting
    // work at all: keying on the configuration alone would cache the *detected*
    // answer and never notice the viewer overriding it, so the control in Settings
    // would look live and change nothing. Keyed on both, flipping it recomposes
    // only the handful of `LocalIsTv` readers, which is what this local's tracking
    // variant is for.
    val configuration = LocalConfiguration.current
    val isTv = remember(configuration, settings.deviceMode) {
        settings.deviceMode.resolveIsTv(configuration.isTvMode())
    }

    // Material You (Android 12+): pull the accent from the user's wallpaper palette.
    // Falls back to the bundled accent on older releases or if extraction fails.
    //
    // Keyed on `dynamicColor` alone, not on the whole settings object. Extraction
    // reads the system wallpaper palette, and keying on everything meant that
    // flipping any unrelated switch — autoplay, channel logos, the start category —
    // re-ran it.
    val dynamicScheme = remember(settings.dynamicColor, context) {
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
            surfaceContainerHigh = colors.surfaceHigh,
            outline = colors.border,
            // A rule between rows is not a card outline. Material asks for both,
            // and wiring only `outline` left every divider it draws at card weight.
            outlineVariant = colors.divider,
            error = KurdishRed,
            onError = onColorFor(KurdishRed)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalIsTv provides isTv
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
