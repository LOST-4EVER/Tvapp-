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

    /**
     * One step above [surfaceElevated]: icon buttons, pressed chips, the surfaces
     * that sit *on* a card.
     *
     * Added because the old ramp had four steps and the app stacks five: page,
     * card, chip, control, and a control on a chip. The fifth was being drawn in
     * [surfaceElevated] and reading as the same level as the card it sat on.
     */
    val surfaceHigh: Color,

    /** Outlines a card: a surface with content in it. */
    val border: Color,

    /** A rule between rows, where a card border would read as a box per item. */
    val divider: Color,

    /**
     * The scrim laid under a focused element's ring.
     *
     * The ring used to turn, which is what made it findable from a sofa. It does
     * not any more, so it leans on contrast instead: a band of the page colour
     * between the element and the ring, which keeps the ring legible against a
     * bright logo or a light panel without dimming the artwork itself.
     */
    val focusScrim: Color,

    /**
     * A hairline of light along the top edge of a card.
     *
     * Replaces the vertical gradient the cards used to carry. On a near-black
     * surface a flat card reads as a hole with a border around it, and the
     * gradient was the cheapest fix — but a full-height ramp is a per-card draw,
     * and it also made the top of the card look like the "front" of something
     * three-dimensional. One 1dp line does the separating without the shading.
     */
    val edgeHighlight: Color,

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

/**
 * Moves a colour towards white by [amount] without touching its hue much.
 *
 * `lerp` towards white is the obvious implementation and it desaturates: at 6%
 * the result is close enough that a hairline does not go grey, but the same
 * helper at the 40%-plus strengths a *fill* would need is a different colour
 * entirely, which is why container tints go through [tonalPair] instead and this
 * is only for the one-pixel case.
 */
private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha
)

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
    val primary = if (settings.dynamicColor && dynamicScheme != null) dynamicScheme.primary else tone.primary

    val surface = if (amoled) AmoledSurface else DarkSurface
    val variant = if (amoled) AmoledSurfaceVariant else DarkSurfaceVariant
    val (container, onContainer) = if (settings.dynamicColor && dynamicScheme != null) {
        val tinted = lerp(surface, primary, CONTAINER_TINT)
        tinted to onColorFor(tinted)
    } else {
        tonalPair(primary, surface)
    }

    // Named up front because [focusScrim] below *is* this colour, and a constructor
    // call cannot refer to a sibling named argument of itself.
    val background = if (amoled) AmoledBackground else DarkBackground
    val elevated = if (amoled) AmoledSurfaceElevated else DarkSurfaceElevated

    return AppColors(
        background = background,
        surface = surface,
        surfaceVariant = variant,
        surfaceElevated = elevated,
        surfaceHigh = if (amoled) AmoledSurfaceHigh else DarkSurfaceHigh,
        border = if (amoled) AmoledCardBorder else DarkCardBorder,
        divider = if (amoled) AmoledDivider else DarkDivider,
        // Opaque, not a tint of the accent. A translucent ring scrim over a
        // bright logo left the logo showing through the middle of the mark; the
        // job here is a gap of flat background around the element, and a gap has
        // to be flat or it is not a gap.
        focusScrim = background,
        // A shade lighter than the card it sits on, which is what makes a flat
        // card read as a surface rather than as a hole.
        edgeHighlight = elevated.lighten(0.06f),
        borderGlow = primary.copy(alpha = 0.20f),
        glass = if (amoled) Color(0xE6000000) else GlassOverlay,
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
