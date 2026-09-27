package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.kurdishtv.model.AccentColor
import com.example.kurdishtv.model.AppSettings

/**
 * Semantic color set resolved from [AppSettings]. Components read this through
 * [LocalAppColors] so the whole app re-themes (accent + AMOLED) in one place.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderGlow: Color,
    val glass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val tertiary: Color,
    val liveRed: Color,
    val isAmoled: Boolean
)

private data class AccentTone(
    val primary: Color,
    val container: Color,
    val onContainer: Color
)

private fun AccentColor.tone(): AccentTone = when (this) {
    AccentColor.SUN_GOLD -> AccentTone(KurdishSunGold, GoldContainer, OnGoldContainer)
    AccentColor.EMBER -> AccentTone(AccentEmber, EmberContainer, OnEmberContainer)
    AccentColor.ROSE -> AccentTone(AccentRose, RoseContainer, OnRoseContainer)
    AccentColor.JADE -> AccentTone(AccentJade, JadeContainer, OnJadeContainer)
    AccentColor.AZURE -> AccentTone(AccentAzure, AzureContainer, OnAzureContainer)
}

/**
 * Resolves the semantic color set for the current settings.
 *
 * [dynamicScheme] is the wallpaper-derived Material You palette (Android 12+).
 * When present it replaces the accent/tertiary tones only: the dark surfaces stay
 * fixed so the TV UI keeps its contrast and AMOLED behavior regardless of what
 * color the user's wallpaper happens to be.
 */
fun appColorsFor(settings: AppSettings, dynamicScheme: ColorScheme? = null): AppColors {
    val tone = settings.accent.tone()
    val amoled = settings.isAmoled
    val primary = dynamicScheme?.primary ?: tone.primary
    return AppColors(
        background = if (amoled) AmoledBackground else DarkBackground,
        surface = if (amoled) AmoledSurface else DarkSurface,
        surfaceVariant = if (amoled) AmoledSurfaceVariant else DarkSurfaceVariant,
        surfaceElevated = if (amoled) AmoledSurfaceElevated else DarkSurfaceElevated,
        border = if (amoled) AmoledCardBorder else DarkCardBorder,
        borderGlow = primary.copy(alpha = 0.20f),
        glass = if (amoled) Color(0xCC000000) else GlassOverlay,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        primary = primary,
        onPrimary = dynamicScheme?.onPrimary ?: Color.Black,
        primaryContainer = dynamicScheme?.primaryContainer ?: tone.container,
        onPrimaryContainer = dynamicScheme?.onPrimaryContainer ?: tone.onContainer,
        tertiary = dynamicScheme?.tertiary ?: KurdishGreen,
        liveRed = LiveRed,
        isAmoled = amoled
    )
}

/** Default instance used before settings are applied (also the Compose preview default). */
val DefaultAppColors: AppColors = appColorsFor(AppSettings())

/**
 * The app's semantic colours.
 *
 * This is a [compositionLocalOf], not a `staticCompositionLocalOf`, because the
 * value genuinely changes at runtime: switching accent, toggling AMOLED or
 * enabling Material You produces a new instance.
 *
 * `staticCompositionLocalOf` does not track reads, so a change replaced the value
 * in the local but skipped recomposition of everything reading it — the new
 * colours only appeared after some unrelated state change forced the screens to
 * redraw. The cheaper static variant is only safe for values that never change.
 */
val LocalAppColors = compositionLocalOf { DefaultAppColors }
