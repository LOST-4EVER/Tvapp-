package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * M3 Expressive spring tokens: a soft, physics-based bounce for press feedback
 * and spatial transitions.
 */
object ExpressiveMotion {
    val pressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val spatialFast: SpringSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = 1200f
    )
    val spatialMedium: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 600f
    )
    val spatialSlow: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 300f
    )
    val snappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/** True when the user asked for reduced motion; press/scale feedback becomes instant. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * A single shared pulse value for every LIVE badge in the app. Running one
 * infinite transition instead of one per card meaningfully cuts animation work
 * inside the channel grid.
 */
val LocalLivePulse = staticCompositionLocalOf { 1f }

@Composable
fun rememberLivePulse(enabled: Boolean): Float {
    if (!enabled) return 1f
    val transition = rememberInfiniteTransition(label = "LivePulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LivePulseScale"
    )
    return scale
}
