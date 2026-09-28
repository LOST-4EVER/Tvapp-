package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The app's type scale: a bold editorial hierarchy built on the system family.
 *
 * `FontFamily.Default` is deliberate rather than lazy. It resolves to Roboto, which
 * carries no Arabic-script glyphs on the oldest supported releases, so a bundled
 * Latin-only family would not have helped the Kurdish text anyway — Android falls
 * back to Noto Naskh Arabic for it either way. Naming the family explicitly is what
 * makes that a decision instead of an accident, and it leaves the door open for a
 * purpose-built Kurdish face later without touching a call site.
 *
 * Two rules below are specific to this app's content and are the reason this is not
 * just the Material defaults with heavier weights:
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
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        lineHeight = 62.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp
    ),
    // Previously undefined, so this slot silently fell back to the Material default
    // (24sp, regular) — the one size in the display/headline run that was neither
    // bold nor on the app's ramp.
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 45.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 33.sp
    ),
    // Used by the featured hero for the channel name. It was undefined here too, so
    // the hero's largest text was the only one on the screen set in the Material
    // default regular weight while everything around it was bold.
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp
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
    // Section labels, captions and the long-form help text in Settings. Also
    // previously undefined.
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
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
