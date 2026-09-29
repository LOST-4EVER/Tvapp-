package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The app's type scale: a bold editorial hierarchy, with a monospaced lane for
 * the numerals a television is actually navigated by.
 *
 * `FontFamily.Default` is deliberate rather than lazy. It resolves to Roboto, which
 * carries no Arabic-script glyphs on the oldest supported releases, so a bundled
 * Latin-only family would not have helped the Kurdish text anyway — Android falls
 * back to Noto Naskh Arabic for it either way. Naming the family explicitly is what
 * makes that a decision instead of an accident, and it leaves the door open for a
 * purpose-built Kurdish face later without touching a call site.
 *
 * Three rules below are specific to this app's content and are the reason this is
 * not just the Material defaults with heavier weights:
 *
 *  - **Line heights leave room for stacked vowel marks.** Sorani is written with
 *    diacritics above *and* below the baseline (تەلەفزیۆنی کوردی, ڕێکخستن). A display
 *    line height of 1.11 is the right ratio for Latin display type, but it puts the
 *    top and bottom of those marks right on the line boundary, and a multi-line
 *    Kurdish channel description then collides with the line above it. The looser
 *    ratios here cost a few dp of vertical space and remove the collision.
 *
 *  - **No positive letter spacing.** Tracking is inserted as an extra advance between
 *    glyph clusters, and Arabic script is written as *connected* clusters; on several
 *    Android releases that advance lands between joined forms and visibly opens the
 *    joins. These slots carry Kurdish channel names, category labels and UI strings,
 *    so tracking stays at 0 on all of them. The display and headline slots keep their
 *    negative tracking, which is where the editorial look comes from and which only
 *    ever renders Latin channel names at that size.
 *
 *  - **Numerals are monospaced, and that is the point.** This is a television. The
 *    sidebar labels every channel with its position, the remote's number pad is the
 *    fastest way to reach one, and the number-pad overlay exists to show digits
 *    large enough to read from a sofa. In a proportional face a `1` is about half
 *    the width of an `8`, so a column of channel numbers wobbles and two adjacent
 *    numbers are not the same width — which makes a list of them harder to scan
 *    than it has any right to be. [ChannelNumber] and [NumeralLarge] are fixed-
 *    width for that reason, and they are the only two slots in this file that are.
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 47.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Used by the featured hero for the channel name.
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    // Section labels, captions and the long-form help text in Settings.
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    // This is the LIVE badge's "LIVE" and every small all-caps marker in the app.
    // It is bold *and* widely tracked, which is the one place positive tracking is
    // correct: these strings are Latin-only, all-caps, and never carry Kurdish
    // text, so the joins that tracking breaks do not exist here.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp
    )
)

/**
 * A channel's position in the list, in a fixed-width face.
 *
 * Used by the sidebar next to every row, and sized to sit in a fixed slot so a
 * three-digit number and a one-digit number leave the name in the same place.
 */
val ChannelNumber = TextStyle(
    // `FontFamily.Monospace`, not an import of `Monospace`. It is a property on
    // `FontFamily`'s companion object rather than a nested type, so
    // `import FontFamily.Monospace` does not resolve and the import has to go.
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

/**
 * The digits on the remote's number pad, as large as the screen allows.
 *
 * Monospaced for the same reason as [ChannelNumber] and more so: these digits
 * replace themselves on every keystroke, and in a proportional face a `1` landing
 * where a `7` was leaves the number visibly narrower, which reads as a dropped
 * digit rather than as a new one.
 */
val NumeralLarge = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Black,
    fontSize = 56.sp,
    lineHeight = 62.sp,
    letterSpacing = 2.sp
)

