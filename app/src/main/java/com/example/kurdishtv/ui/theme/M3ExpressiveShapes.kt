package com.example.kurdishtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shape system.
 *
 * Expressive shapes are not just rounded rectangles: they use deliberately
 * uneven corner radii (cookie / clover / sunny / burst) so containers feel
 * playful. [AppShapes] feeds the standard Material components.
 */
object M3ExpressiveShapes {
    // Legacy tokens (kept so existing call sites keep working)
    val Pill = RoundedCornerShape(percent = 50)
    val ExtraLargeRounded = RoundedCornerShape(32.dp)
    val LargeCard = RoundedCornerShape(26.dp)
    val MediumCard = RoundedCornerShape(20.dp)
    val SmallCard = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(12.dp)
    val AsymmetricHero = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 16.dp,
        bottomEnd = 28.dp,
        bottomStart = 20.dp
    )
    val BadgePill = RoundedCornerShape(10.dp)

    // ── Expressive corner treatments ──────────────────────────────────────────
    /** Small playful cookie: one corner pinched tighter than the others. */
    val Cookie = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomEnd = 4.dp,
        bottomStart = 16.dp
    )

    /** Clover-ish card with two soft corners and two rounder ones. */
    val Clover = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 12.dp,
        bottomEnd = 28.dp,
        bottomStart = 12.dp
    )

    /** Very round "sunny" surface for featured / hero content. */
    val Sunny = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomEnd = 8.dp,
        bottomStart = 32.dp
    )

    /** Burst shape for icon buttons / avatars in expressive layouts. */
    val Burst = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 6.dp,
        bottomEnd = 20.dp,
        bottomStart = 6.dp
    )

    /** Wide pill for section tabs and settings rows. */
    val SectionPill = RoundedCornerShape(18.dp)

    /** Rounded-square tile used by channel logos. */
    val LogoTile = RoundedCornerShape(22.dp)
}

/** MaterialTheme shapes mapping (extraSmall → extraLarge). */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
