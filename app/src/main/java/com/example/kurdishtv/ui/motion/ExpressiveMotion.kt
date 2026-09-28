package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.delay

/**
 * Material 3 Expressive motion tokens.
 *
 * Expressive replaced fixed durations and easing curves with a physics system, and
 * this object is that system expressed in Compose springs. The values are the ones
 * published in the Material motion token table rather than hand-tuned ones, so the app
 * moves the way Material's own components do.
 *
 * The system has two axes, and picking the wrong one is the most common way to make
 * Expressive motion feel wrong:
 *
 *  - **Spatial** tokens drive anything that *moves*: position, size, scale, rotation,
 *    corner radius, shape. They are under-damped, so the element overshoots its target
 *    and settles back. That overshoot is the entire point — a spatial spring that does
 *    not bounce reads as a linear animation wearing a spring's clothes.
 *  - **Effects** tokens drive anything that *changes without moving*: colour, alpha,
 *    tint. They are critically damped, because an overshooting colour is a visible
 *    artefact, not a delight.
 *
 * Each axis has three speeds. Small things move fast, large things move slow.
 */
object ExpressiveMotion {

    // ── Expressive scheme ────────────────────────────────────────────────────
    // Under-damped spatial springs: they overshoot and bounce into place.

    /** Fast spatial. Small components: buttons, switches, chips. */
    val spatialFast: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    /** Default spatial. Part-screen surfaces: cards, bottom sheets, dialogs. */
    val spatialDefault: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    /** Slow spatial. Full-screen movement. */
    val spatialSlow: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    // ── Effects (no overshoot) ───────────────────────────────────────────────
    // Colour and opacity must never bounce past their target.

    val effectsFast: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 3800f
    )

    val effectsDefault: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1600f
    )

    val effectsSlow: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 800f
    )

    // ── Standard scheme ──────────────────────────────────────────────────────
    // Nearly critically damped: the same springs with the bounce taken out. Useful for
    // continuous or peripheral motion, where an element that keeps wobbling is tiring.

    val standardSpatialFast: SpringSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 1400f
    )

    val standardSpatialDefault: SpringSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 700f
    )

    val standardSpatialSlow: SpringSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 300f
    )

    /**
     * Default spatial, typed for offsets.
     *
     * Lazy-list placement animations animate an [IntOffset], not a [Float], so the
     * float tokens cannot be passed to them. A visibility threshold is required here:
     * without one the spring keeps chasing sub-pixel movements and the list never
     * settles.
     */
    val spatialDefaultOffset: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    /**
     * Press feedback: the default spatial spring with a little more bounce.
     *
     * A press is the one gesture the user is *holding*, so it gets the most generous
     * overshoot in the app — the element visibly rebounds past its resting scale before
     * settling, which is the defining Expressive gesture.
     */
    val pressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Colour and alpha transitions, for the same reason as [effectsDefault]. */
    val snappy: SpringSpec<Float> = effectsDefault

    /**
     * The same tokens, typed for the two other things Compose animates.
     *
     * `animateColorAsState` and `animateDpAsState` need a spec for *their* type, and
     * a `SpringSpec<Float>` is not one — a spring is a pair of numbers, not a generic
     * over the animated value, so the two have to be written out again. Cheap, and it
     * is what stops colour transitions quietly picking up an overshooting spring.
     */
    val effectsColor: SpringSpec<Color> = spring(dampingRatio = 1f, stiffness = 1600f)
    val effectsColorFast: SpringSpec<Color> = spring(dampingRatio = 1f, stiffness = 3800f)
    val spatialDp: SpringSpec<Dp> = spring(dampingRatio = 0.8f, stiffness = 380f)

    // ── Legacy easing ────────────────────────────────────────────────────────
    // Material deprecated duration-and-easing in favour of springs, but a couple of
    // things genuinely are not springs: a plain opacity fade, and anything that has to
    // hold a fixed curve across a whole screen rather than settle. These stay for
    // those, and are not used for anything that moves.

    /** The emphasized curve, for enter/exit fades. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val standard: Easing = FastOutSlowInEasing

    /**
     * Entrance spring for content appearing in a grid or list.
     *
     * Between the two published tokens on purpose. A bouncier spring reads as jitter
     * when thirty items animate at once, and a spatial spring at full strength takes
     * visibly too long when they all start together.
     */
    val entranceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 520f
    )

    /** Durations, for the animations that are genuinely not springs. */
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 600

    /**
     * How long a full turn of a continuously rotating shape takes.
     *
     * Long on purpose. A shape that spins quickly reads as a blur; one that turns once
     * every few seconds is legible as a shape, which is the entire appeal.
     */
    const val ROTATION_PERIOD_MS = 6000

    /**
     * The breath of the LIVE dot: the two ends of it, and one full cycle.
     *
     * Read by the single app-wide loop behind [rememberLivePulse], so the pulse is one
     * number rather than one animation per badge.
     */
    const val LIVE_PULSE_MIN = 0.85f
    const val LIVE_PULSE_MAX = 1.25f
    const val LIVE_PULSE_PERIOD_MS = 1800L

    /**
     * Per-item delay for a staggered grid entrance.
     *
     * Small on purpose: with dozens of cards on screen a large stagger makes the last
     * row feel broken rather than choreographed.
     */
    const val STAGGER_STEP_MS = 26L
    const val STAGGER_MAX_ITEMS = 14

    /** Resting values used when an animation is switched off. */
    const val RESTING_PULSE = 1f
    const val RESTING_BOUNCE = 0f
}

/**
 * True when the user asked for reduced motion; press/scale feedback becomes instant.
 *
 * `compositionLocalOf` rather than `staticCompositionLocalOf`, because the user can
 * toggle this in Settings mid-session. A static local does not track its reads, so
 * toggling the setting left already-composed animations running at full motion until
 * something else happened to force a redraw.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * A staggered entrance for grid and list items.
 *
 * Items fade and rise into place one after another, which is the M3 Expressive pattern
 * for content that arrives as a set. The stagger is capped at
 * [ExpressiveMotion.STAGGER_MAX_ITEMS] so a long list does not leave its last rows
 * waiting, and under reduced motion it collapses to a plain fade.
 *
 * Pass the item's index **within the currently visible set**, not its absolute index in
 * the data, otherwise scrolling would replay the whole animation as rows recycle.
 *
 * @param animate whether the set is still settling. A lazy layout recycles item
 *   compositions, so a grid that staggers unconditionally replays the entrance on
 *   every card that scrolls into view: each one waits out its own delay before it
 *   appears, which reads as the whole grid lagging behind the D-pad. Callers arm this
 *   when the set changes and disarm it once the entrance has had time to play.
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    animate: Boolean = true,
    maxStaggeredItems: Int = ExpressiveMotion.STAGGER_MAX_ITEMS
): Modifier {
    val reduceMotion = LocalReduceMotion.current
    // Reduced motion, or a grid that has already settled, collapses the entrance to
    // "appear where it belongs": no delay, no layer, nothing to wait for.
    val staggered = animate && !reduceMotion
    val step = if (staggered) {
        index.coerceIn(0, maxStaggeredItems) * ExpressiveMotion.STAGGER_STEP_MS
    } else {
        0L
    }

    // The delayed start is a one-shot effect, not an animation input, so the value is
    // flipped once and then left to the spring.
    val started = remember { mutableFloatStateOf(if (staggered) 0f else 1f) }
    LaunchedEffect(step, staggered) {
        if (staggered) delay(step)
        started.floatValue = 1f
    }

    val progress by animateFloatAsState(
        targetValue = started.floatValue,
        animationSpec = ExpressiveMotion.entranceSpring,
        label = "StaggerEntrance"
    )

    // The layer is dropped the moment the entrance finishes.
    //
    // This modifier is on every item of the channel grid, and the grid is the
    // largest list in the app — six hundred-odd channels. A `graphicsLayer` is a
    // render node with its own display list, and a transform on it asks the
    // compositor to treat that node as a separately composited surface. Keeping
    // one alive per card, for the entire time the browse screen is on show, in
    // order to hold alpha at 1 and a scale at 1, is the single most expensive
    // thing the grid was doing when nothing was animating.
    //
    // `>= 1f` also drops the layer at the spring's small overshoot, which is a
    // percent of a percent here and not worth a permanent render node.
    if (progress >= 1f) return this

    return this.graphicsLayer {
        alpha = progress
        // A short rise plus a slight scale reads as "settling into place" without the
        // wobble a bouncier spring would produce at this volume.
        translationY = (1f - progress) * 18f * this.density
        scaleX = 0.97f + 0.03f * progress
        scaleY = 0.97f + 0.03f * progress
    }
}

/**
 * The shared pulse of every LIVE badge in the app, as a **value** rather than as a
 * composition input, and that distinction is the whole reason for its type.
 *
 * The number changes on every frame. Published as a `Float` it would have to be read
 * during composition, which invalidates every reader on every frame — and every card
 * in the grid carries a badge, so that is several hundred recompositions per frame to
 * breathe an 8dp dot. Published as a [MutableFloatState] it can be read from the
 * *draw* phase instead, through `Modifier.drawWithContent` or
 * `Modifier.graphicsLayer`, where a change invalidates the drawing of the one node
 * that read it and nothing recomposes at all.
 *
 * `compositionLocalOf` rather than `staticCompositionLocalOf` because the state
 * *object* changes when the setting is toggled, and readers have to see the new one.
 */
val LocalLivePulse = compositionLocalOf { mutableFloatStateOf(ExpressiveMotion.RESTING_PULSE) }

/**
 * The shared turn of every focus ring, in degrees.
 *
 * The ring is the only thing on a TV screen that is allowed to move continuously,
 * because it is the only thing that says "you are here" without being read. That
 * makes it the most expensive kind of element to have many of.
 *
 * Arrowing through a grid used to create a `rememberInfiniteTransition` per card as
 * focus arrived and tear it down as focus left, so a single press of the right arrow
 * started and stopped a frame-callback loop, and holding the D-pad down churned one
 * per card per step. There is one ring drawn at a time, so there is no reason for
 * there to be more than one loop driving them.
 *
 * `compositionLocalOf` rather than `staticCompositionLocalOf` for the same reason as
 * [LocalLivePulse]: the state object has to be replaceable when the setting changes.
 * A plain `Float` would have been worse still — see [LocalLivePulse] for why the angle
 * is published as state rather than as a number.
 */
val LocalFocusRotation = compositionLocalOf { mutableFloatStateOf(0f) }

/** Drives [LocalFocusRotation]. One frame loop for the whole app; see that property. */
@Composable
fun rememberFocusRotation(enabled: Boolean): MutableFloatState {
    val degrees = remember { mutableFloatStateOf(0f) }

    // Driven by a plain `withFrameNanos` loop writing one [MutableFloatState], rather
    // than by a `rememberInfiniteTransition`.
    //
    // A transition could not be switched off. It ran its frame callback for the life of
    // the app and the `if (enabled)` at the end threw the result away, so a viewer who
    // asked for reduced motion still paid for a continuously running animation. This
    // loop is keyed on `enabled` and simply is not there when the answer is no.
    LaunchedEffect(enabled) {
        if (!enabled) {
            degrees.floatValue = 0f
            return@LaunchedEffect
        }
        val period = ExpressiveMotion.ROTATION_PERIOD_MS * 1_000_000L
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                // A constant angular speed. An easing curve applied to a repeating
                // 0→360 is not a slow turn, it is a turn that stutters once per
                // revolution — and a ring that is the only moving thing on screen is
                // exactly where that reads as a fault.
                degrees.floatValue = ((now - start) % period) / period.toFloat() * 360f
            }
        }
    }
    return degrees
}

/**
 * The shared LIVE-badge pulse, as state. Constant while the pulse is switched off.
 *
 * One frame loop for the whole app, and no loop at all when the setting is off — which
 * is what an `if (enabled)` *outside* an infinite transition could never achieve. The
 * caller reads the returned state in the draw phase: see [LocalLivePulse]; reading it
 * during composition would put a recomposition on every badge, on every frame.
 */
@Composable
fun rememberLivePulse(enabled: Boolean): MutableFloatState {
    val scale = remember { mutableFloatStateOf(ExpressiveMotion.RESTING_PULSE) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            scale.floatValue = ExpressiveMotion.RESTING_PULSE
            return@LaunchedEffect
        }
        val period = ExpressiveMotion.LIVE_PULSE_PERIOD_MS * 1_000_000L
        val low = ExpressiveMotion.LIVE_PULSE_MIN
        val span = ExpressiveMotion.LIVE_PULSE_MAX - low
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                val phase = ((now - start) % period) / period.toFloat()
                // A cosine breath rather than a tween's easing curve: the same smooth
                // in-and-out, with no cubic solve between the two ends.
                scale.floatValue = low + span * (0.5f - 0.5f * cos(2f * PI.toFloat() * phase))
            }
        }
    }
    return scale
}

/**
 * A shared 0→1 driver for continuous shape animation.
 *
 * Everything that loops forever in this app — the loading indicator's shape walk, the
 * live badge's rotation, the hero card's slow turn — reads from this one transition, so
 * the number of running animations does not grow with the number of things on screen.
 *
 * [keyframes] holds at each extreme, which reproduces the loading indicator's
 * stretch-and-settle cadence instead of looping mechanically.
 */
@Composable
fun rememberBounceProgress(enabled: Boolean): Float {
    val transition = rememberInfiniteTransition(label = "ShapeCycle")
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
        label = "ShapeCycleValue"
    )
    return if (enabled) progress else ExpressiveMotion.RESTING_BOUNCE
}
