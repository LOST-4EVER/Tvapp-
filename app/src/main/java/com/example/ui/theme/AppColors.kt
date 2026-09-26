package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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

fun appColorsFor(settings: AppSettings): AppColors {
    val tone = settings.accent.tone()
    val amoled = settings.isAmoled
    return AppColors(
        background = if (amoled) AmoledBackground else DarkBackground,
        surface = if (amoled) AmoledSurface else DarkSurface,
        surfaceVariant = if (amoled) AmoledSurfaceVariant else DarkSurfaceVariant,
        surfaceElevated = if (amoled) AmoledSurfaceElevated else DarkSurfaceElevated,
        border = if (amoled) AmoledCardBorder else DarkCardBorder,
        borderGlow = tone.primary.copy(alpha = 0.20f),
        glass = if (amoled) Color(0xCC000000) else GlassOverlay,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        primary = tone.primary,
        onPrimary = Color.Black,
        primaryContainer = tone.container,
        onPrimaryContainer = tone.onContainer,
        tertiary = KurdishGreen,
        liveRed = LiveRed,
        isAmoled = amoled
    )
}

/** Default instance used before settings are applied (also the Compose preview default). */
val DefaultAppColors: AppColors = appColorsFor(AppSettings())

val LocalAppColors = staticCompositionLocalOf { DefaultAppColors }
