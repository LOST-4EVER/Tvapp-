package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material 3 Expressive motion tokens.
 *
 * The Expressive update replaced fixed-duration easing with a physics-first system:
 * springs for anything the user initiates, and spatial springs for elements that
 * move to a new position on screen. The tokens below follow that split so press
 * feedback feels physical and layout changes feel continuous.
 */
object ExpressiveMotion {

    /**
     * Press feedback. Medium-bouncy so the element visibly rebounds past its
     * resting scale before settling, which is the defining Expressive gesture.
     */
    val pressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Fast spatial spring: for elements that barely move, e.g. an icon nudge. */
    val spatialFast: SpringSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = 1200f
    )

    /** Default spatial spring for container movement. */
    val spatialMedium: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 600f
    )

    /** Slow spatial spring for large surfaces settling into place. */
    val spatialSlow: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 300f
    )

    /** No overshoot. Use for colour and alpha, where bouncing reads as a glitch. */
    val snappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Standard emphasized easing, for enter/exit fades. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val standard: Easing = FastOutSlowInEasing

    /** Durations for non-spring animations. */
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 600
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

/**
 * A 0→1 driver for the bouncing loading indicator.
 *
 * The keyframe spec holds at each extreme, which reproduces the M3 loading
 * indicator's stretch-and-release cadence instead of looping mechanically. One
 * transition drives the whole indicator, so its cost does not grow with the
 * number of segments drawn.
 */
@Composable
fun rememberBounceProgress(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "BounceProgress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                1f at 700
                1f at 850
                0f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BounceProgressValue"
    )
    return progress
}
