package com.example.kurdishtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shapes system.
 */
object M3ExpressiveShapes {
    val Pill = RoundedCornerShape(percent = 50)
    val ExtraLargeRounded = RoundedCornerShape(32.dp)
    val LargeCard = RoundedCornerShape(26.dp)
    val MediumCard = RoundedCornerShape(20.dp)
    val SmallCard = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(12.dp)

    // Expressive asymmetric shape for spotlight cards
    val AsymmetricHero = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 16.dp,
        bottomEnd = 28.dp,
        bottomStart = 20.dp
    )

    // Expressive channel badge shape
    val BadgePill = RoundedCornerShape(10.dp)
}
