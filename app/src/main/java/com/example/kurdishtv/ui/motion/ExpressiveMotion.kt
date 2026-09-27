package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

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

    /**
     * Entrance spring for content that appears in a list or grid.
     *
     * Softer than [pressSpring]: an entrance should settle rather than wobble,
     * and it runs on many items at once so overshoot reads as jitter.
     */
    val entranceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 420f
    )

    /** Durations for non-spring animations. */
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 600

    /**
     * Per-item delay for a staggered grid entrance.
     *
     * Small on purpose: with dozens of cards on screen a large stagger makes the
     * last row feel broken rather than choreographed.
     */
    const val STAGGER_STEP_MS = 26L
    const val STAGGER_MAX_ITEMS = 14

    /**
     * Resting values used when an animation is switched off.
     *
     * 1f is the neutral scale, so a disabled pulse leaves the badge at its
     * normal size rather than freezing it mid-swing at 0.85 or 1.25.
     */
    const val RESTING_PULSE = 1f
    const val RESTING_BOUNCE = 0f
}

/**
 * True when the user asked for reduced motion; press/scale feedback becomes instant.
 *
 * `compositionLocalOf` rather than `staticCompositionLocalOf`, because the user can
 * toggle this in Settings mid-session. A static local does not track its reads, so
 * toggling the setting left already-composed animations running at full motion
 * until something else happened to force a redraw.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * A staggered entrance for grid and list items.
 *
 * Items fade and rise into place one after another, which is the M3 Expressive
 * pattern for content that arrives as a set. The stagger is capped at
 * [ExpressiveMotion.STAGGER_MAX_ITEMS] so a long list does not leave its last
 * rows waiting, and under reduced motion it collapses to a plain fade.
 *
 * Pass the item's index **within the currently visible set**, not its absolute
 * index in the data, otherwise scrolling would replay the whole animation as
 * rows recycle.
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    maxStaggeredItems: Int = ExpressiveMotion.STAGGER_MAX_ITEMS
): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val step = index.coerceIn(0, maxStaggeredItems) * ExpressiveMotion.STAGGER_STEP_MS

    // The delayed start is a one-shot effect, not an animation input, so the value
    // is flipped once and then left to the spring.
    val started = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(step) {
        if (reduceMotion) {
            started.floatValue = 1f
        } else {
            delay(step)
            started.floatValue = 1f
        }
    }

    val progress by animateFloatAsState(
        targetValue = started.floatValue,
        animationSpec = ExpressiveMotion.entranceSpring,
        label = "StaggerEntrance"
    )

    return this.graphicsLayer {
        alpha = progress
        // A short rise plus a slight scale reads as "settling into place"
        // without the wobble a bouncier spring would produce at this volume.
        translationY = (1f - progress) * 18f * this.density
        scaleX = 0.97f + 0.03f * progress
        scaleY = 0.97f + 0.03f * progress
    }
}

/**
 * A single shared pulse value for every LIVE badge in the app. Running one
 * infinite transition instead of one per card meaningfully cuts animation work
 * inside the channel grid.
 *
 * `compositionLocalOf`, not `staticCompositionLocalOf`: this value changes on
 * every frame of the pulse animation, and a static local does not track reads.
 * Every badge would then keep rendering the value it captured at composition
 * time, so the shared pulse never actually animated — one transition was running
 * and its result was being thrown away.
 */
val LocalLivePulse = compositionLocalOf { 1f }

/**
 * The shared LIVE-badge pulse, or a constant when disabled.
 *
 * The `remember` calls run unconditionally and the animation is simply paused
 * when disabled. An early `if (!enabled) return` before them would change how
 * many slots this composable occupies, and toggling the setting in Settings
 * mid-session would then hit Compose's "slot table changed structure" check.
 */
@Composable
fun rememberLivePulse(enabled: Boolean): Float {
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
    return if (enabled) scale else RESTING_PULSE
}

/**
 * A 0→1 driver for the bouncing loading indicator.
 *
 * The keyframe spec holds at each extreme, which reproduces the M3 loading
 * indicator's stretch-and-release cadence instead of looping mechanically. One
 * transition drives the whole indicator, so its cost does not grow with the
 * number of segments drawn.
 */
/** See [rememberLivePulse]: the transition is always created, then gated. */
@Composable
fun rememberBounceProgress(enabled: Boolean): Float {
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
    return if (enabled) progress else RESTING_BOUNCE
}
