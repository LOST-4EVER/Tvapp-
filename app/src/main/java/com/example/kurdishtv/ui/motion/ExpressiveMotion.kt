package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
     * The granularity the LIVE pulse is published at.
     *
     * The pulse is one shared float read in the draw phase by *every* badge on
     * screen, and the browse grid has a badge on every card — so one write here is
     * one draw invalidation per visible badge, and the write happens whether or not
     * the badge's drawn result actually changed.
     *
     * A 1.8s breath at 60fps is 108 frames, and over the fast middle of the cosine
     * consecutive frames differ by only about 0.012 in scale. Publishing in hundredths
     * turns that into 77 writes per cycle instead of 108 — 39 distinct values rather
     * than 108 — for a worst-case step of 0.02 in scale, which on the 8dp dot is a
     * sixth of a pixel. The dot breathes exactly as before; it just stops asking the
     * compositor to redraw thirty identical circles twice a second.
     *
     * Finer than this stops helping: at 0.005 and below almost every frame during
     * the fast middle still lands on a new step, so the writes come back without the
     * motion being any smoother. Coarser than this starts to show — 0.02 makes the
     * dot visibly step through the middle of the breath.
     */
    const val LIVE_PULSE_STEP = 0.01f

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
val LocalLivePulse = compositionLocalOf { WatchedFloat(ExpressiveMotion.RESTING_PULSE) }

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
val LocalFocusRotation = compositionLocalOf { WatchedFloat(0f) }

/**
 * Registers the caller as a reader of [LocalFocusRotation] while it is `active`.
 *
 * There is one focus ring drawn at a time, so the honest answer to "is anything
 * watching the turn?" is almost always *no* — and an answer of no has to be able to
 * stop the loop. See [rememberFocusRotation] for why that matters.
 */
@Composable
internal fun WatchFocusRotation(active: Boolean) {
    val rotation = LocalFocusRotation.current
    DisposableEffect(rotation, active) {
        if (active) rotation.watch()
        onDispose { if (active) rotation.unwatch() }
    }
}

/** Drives [LocalFocusRotation]. One frame loop for the whole app; see that property. */
@Composable
fun rememberFocusRotation(enabled: Boolean): WatchedFloat {
    val rotation = remember { WatchedFloat(0f) }

    // Driven by a plain `withFrameNanos` loop writing one state object, rather
    // than by a `rememberInfiniteTransition`.
    //
    // A transition could not be switched off. It ran its frame callback for the life of
    // the app and the `if (enabled)` at the end threw the result away, so a viewer who
    // asked for reduced motion still paid for a continuously running animation. This
    // loop is keyed on `enabled` and simply is not there when the answer is no.
    //
    // The second key is the important one. `enabled` only says the viewer has not
    // switched motion *off*; it says nothing about whether anything is on screen
    // actually reading the number. It was true on every screen, so the loop ran at
    // display rate for the whole life of the process — including the player, where no
    // focus ring is ever drawn, and Settings, which has none at all. A
    // `withFrameNanos` loop is not free: it wakes the CPU sixty times a second, which
    // on a television is the difference between a set-top box that is warm and one
    // that is noticeably hot to the touch, and on a phone it is measurable battery.
    //
    // Keyed on the watcher count, the loop exists exactly while something is drawing
    // from it, and not one frame longer.
    //
    // The phase comes from [WatchedFloat.epoch] rather than from a local `start`
    // captured inside the loop. That detail is what keeps arrowing smooth: moving
    // focus from one card to the next disposes one watcher and creates another, and
    // for an instant the count passes through zero and the effect restarts. With a
    // per-loop origin, the ring would snap back to 0° on every single arrow press —
    // the exact stutter the slow continuous turn exists to avoid, reintroduced by the
    // optimisation. With a shared origin it picks up at the angle it left off at.
    LaunchedEffect(enabled, rotation.watchers.intValue) {
        if (!enabled || !rotation.isWatched) {
            // Deliberately *not* zeroed. Nothing is drawing the ring while it is
            // unfocused, and zeroing it would make the next focus gain start the
            // turn from the top rather than from where it actually was.
            return@LaunchedEffect
        }
        val period = ExpressiveMotion.ROTATION_PERIOD_MS * 1_000_000L
        while (true) {
            withFrameNanos { now ->
                val start = rotation.epoch(now)
                // A constant angular speed. An easing curve applied to a repeating
                // 0→360 is not a slow turn, it is a turn that stutters once per
                // revolution — and a ring that is the only moving thing on screen is
                // exactly where that reads as a fault.
                rotation.state.floatValue = ((now - start) % period) / period.toFloat() * 360f
            }
        }
    }
    return rotation
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
fun rememberLivePulse(enabled: Boolean): WatchedFloat {
    val pulse = remember { WatchedFloat(ExpressiveMotion.RESTING_PULSE) }

    // Keyed on the watcher count for the same reason as [rememberFocusRotation]: the
    // setting says whether the viewer *wants* a pulse, not whether any badge is on
    // screen to receive one. The player draws exactly one badge and only while its
    // transport controls are up, so most of the time spent watching a channel there
    // is nothing reading this number — and it was waking the CPU anyway.
    LaunchedEffect(enabled, pulse.watchers.intValue) {
        if (!enabled || !pulse.isWatched) {
            // Resting, not merely unwritten: unlike the focus ring, a badge draws this
            // value on every frame it is composed, so leaving the last breathing scale
            // in place would freeze a dot mid-pulse at whatever size it happened to
            // be when the last badge went away.
            pulse.state.floatValue = ExpressiveMotion.RESTING_PULSE
            return@LaunchedEffect
        }
        val period = ExpressiveMotion.LIVE_PULSE_PERIOD_MS * 1_000_000L
        val low = ExpressiveMotion.LIVE_PULSE_MIN
        val span = ExpressiveMotion.LIVE_PULSE_MAX - low
        val step = ExpressiveMotion.LIVE_PULSE_STEP
        // The last value actually published, so a frame that snaps to the same step
        // as the one before it is not written at all. A write would invalidate the
        // draw of every badge on screen for no visible difference; see
        // [ExpressiveMotion.LIVE_PULSE_STEP] for where the step size comes from.
        var published = ExpressiveMotion.RESTING_PULSE
        while (true) {
            withFrameNanos { now ->
                // Shared origin — see the note on the rotation loop above.
                val phase = ((now - pulse.epoch(now)) % period) / period.toFloat()
                // A cosine breath rather than a tween's easing curve: the same smooth
                // in-and-out, with no cubic solve between the two ends.
                val raw = low + span * (0.5f - 0.5f * cos(2f * PI.toFloat() * phase))
                // Truncating toward the step below, so the value is always one the dot
                // has been drawn at before and never overshoots [ExpressiveMotion.LIVE_PULSE_MAX].
                val snapped = (raw / step).toInt() * step
                if (snapped != published) {
                    published = snapped
                    pulse.state.floatValue = snapped
                }
            }
        }
    }
    return pulse
}

/** Registers the caller as a reader of [LocalLivePulse] for as long as it is composed. */
@Composable
internal fun WatchLivePulse() {
    val pulse = LocalLivePulse.current
    DisposableEffect(pulse) {
        pulse.watch()
        onDispose { pulse.unwatch() }
    }
}

/**
 * A shared app-wide animation value, and the count of things currently reading it.
 *
 * The distinction between "the viewer has not switched this motion off" and "something
 * on screen is actually animating from it" is the whole reason this type exists. Almost
 * every value the app animates on a loop is read by a surface that is usually not
 * there: focus rings exist on exactly one element, the player's LIVE badge only while
 * its controls are showing. A loop that cannot tell the difference between those two
 * states runs at display rate forever, which is the most expensive kind of bug there
 * is — it costs power constantly, shows nothing, and is invisible in a screenshot.
 *
 * [state] is the value, published as a [MutableFloatState] so it can be read from the
 * draw phase; see [LocalLivePulse] for why that matters. [watchers] is a plain counter
 * and is written only on composition and disposal, so it never invalidates a draw.
 */
@Stable
class WatchedFloat internal constructor(initial: Float) {

    internal val state = mutableFloatStateOf(initial)

    internal val watchers = mutableIntStateOf(0)

    /**
     * The origin every loop driving this value measures its phase from.
     *
     * Set once, on the first frame that asks, and never reset — see [epoch].
     */
    private var origin: Long = 0L

    /**
     * The shared time origin, set on first use and returned unchanged thereafter.
     *
     * A loop that captured its own `start` would restart from zero every time the
     * watcher count moved, which for a focus ring means a visible snap on every arrow
     * press — the watcher count passes through zero each time focus moves between
     * cards, so the snap would be constant. Measuring from one origin that outlives
     * any individual loop makes a restart invisible: the value simply continues.
     */
    internal fun epoch(now: Long): Long {
        if (origin == 0L) origin = now
        return origin
    }

    internal val isWatched: Boolean get() = watchers.intValue > 0

    /** The current value. Read this from the draw phase, never from composition. */
    val floatValue: Float get() = state.floatValue

    internal fun watch() {
        watchers.intValue++
    }

    internal fun unwatch() {
        // Guarded rather than decremented blindly: a double disposal would otherwise
        // drive the count negative and wedge the loop off permanently.
        if (watchers.intValue > 0) watchers.intValue--
    }
}
