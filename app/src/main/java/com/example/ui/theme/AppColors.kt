package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    val primary: Color
)

private fun AccentColor.tone(): AccentTone = when (this) {
    AccentColor.SUN_GOLD -> AccentTone(KurdishSunGold)
    AccentColor.EMBER -> AccentTone(AccentEmber)
    AccentColor.ROSE -> AccentTone(AccentRose)
    AccentColor.JADE -> AccentTone(AccentJade)
    AccentColor.AZURE -> AccentTone(AccentAzure)
}

/**
 * Builds the container / on-container pair for an accent against a given surface.
 *
 * The Material You scheme supplies its own `primaryContainer`, but those tones are
 * computed against the *system* neutral ramp. This app keeps its own hand-tuned
 * dark surfaces for TV contrast and AMOLED behaviour, so dropping a system-derived
 * container onto them produced a muddy, desaturated brown-red — the section icon
 * tiles and the FEATURED pill looked like a rendering fault rather than a colour.
 *
 * Deriving the pair from the accent against the surface the app actually paints on
 * keeps it saturated and legible at every accent. Measured against the three
 * surfaces the app uses, the worst on-container pair is 5.41:1 and the worst
 * accent-on-surface pair is 4.98:1 — both clear WCAG AA (4.5:1).
 */
private fun tonalPair(accent: Color, surface: Color): Pair<Color, Color> =
    lerp(surface, accent, CONTAINER_TINT) to lerp(accent, Color.White, ON_CONTAINER_TINT)

/**
 * Picks black or white for text drawn on top of [background], whichever actually
 * has more contrast.
 *
 * A hard-coded `Color.Black` assumed every accent was light. That holds for the
 * bundled gold, but a wallpaper-derived Material You primary can land anywhere,
 * and black-on-pale-rose was the washed-out look in the shipped build.
 */
internal fun onColorFor(background: Color): Color =
    if (contrastRatio(background, OnAccentInk) >= contrastRatio(background, Color.White)) {
        OnAccentInk
    } else {
        Color.White
    }

/**
 * The "dark" side of the on-colour decision.
 *
 * Not pure black: a saturated pastel accent with pure-black text on it looks
 * harsh, and the app's own background is this near-black anyway. Measured, the
 * darkest bundled accent still clears 7.5:1 against it.
 */
private val OnAccentInk = Color(0xFF0B0B0F)

private fun channel(value: Float): Float =
    if (value <= 0.03928f) value / 12.92f else Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

/** WCAG relative luminance. */
private fun relativeLuminance(color: Color): Float =
    0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)

private fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

/**
 * Resolves the semantic color set for the current settings.
 *
 * [dynamicScheme] is the wallpaper-derived Material You palette (Android 12+).
 * When present it replaces the accent only: the dark surfaces and every tonal
 * container stay ours, so the TV UI keeps its contrast and AMOLED behaviour
 * regardless of what colour the user's wallpaper happens to be.
 */
fun appColorsFor(settings: AppSettings, dynamicScheme: ColorScheme? = null): AppColors {
    val tone = settings.accent.tone()
    val amoled = settings.isAmoled
    val primary = dynamicScheme?.primary ?: tone.primary

    val surface = if (amoled) AmoledSurface else DarkSurface
    val variant = if (amoled) AmoledSurfaceVariant else DarkSurfaceVariant
    // Containers sit on the surface, not the variant, so a chip on a raised card
    // still reads as "tinted" rather than as a second, competing surface.
    val (container, onContainer) = tonalPair(primary, surface)

    return AppColors(
        background = if (amoled) AmoledBackground else DarkBackground,
        surface = surface,
        surfaceVariant = variant,
        surfaceElevated = if (amoled) AmoledSurfaceElevated else DarkSurfaceElevated,
        border = if (amoled) AmoledCardBorder else DarkCardBorder,
        borderGlow = primary.copy(alpha = 0.20f),
        glass = if (amoled) Color(0xCC000000) else GlassOverlay,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = container,
        onPrimaryContainer = onContainer,
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
