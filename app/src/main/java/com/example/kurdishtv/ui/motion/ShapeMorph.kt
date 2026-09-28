package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.theme.ExpressiveMorph
import com.example.kurdishtv.ui.theme.ExpressivePolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.MorphingPolygonShape

/**
 * Shape as a state channel.
 *
 * Material 3 Expressive's central idea about shape is that an element can signal what
 * it is doing by changing its *outline*, not only its colour. A filter chip that
 * squeezes when selected, a button that squares off under a finger, a focus ring that
 * turns — all of them say more than a tint swap, and all of them cost one spring.
 *
 * Two families of shape are animated here, and the split is not arbitrary:
 *
 *  - [rememberMorphingCorners] morphs between two corner-radius scales. This is what
 *    **every interactive surface in the app uses** — chips, buttons, icon buttons, the
 *    rail, the logo well, the live dot. Material lists rounded corners as a
 *    spatial-spring property, so animating them is not a compromise; it is the spec's
 *    own suggestion.
 *  - [rememberMorphingPolygon] morphs between two [ExpressivePolygon]s. It is
 *    expressive — cookies, clovers, bursts — and it is now confined to surfaces that
 *    hold no glyph, no label and no touch target.
 *
 * ## Why the polygon library came off the controls
 *
 * The Expressive vocabulary used to sit under essentially every control: chips were a
 * `Pill` polygon, icon buttons a `Circle` morphing to a `Pentagon`, favourites a
 * `Heart`. It failed, visibly and repeatedly, because the hand-rolled rounded-polygon
 * path builder is only approximately correct away from right angles, and a control is
 * exactly where that error is most visible:
 *
 *  - the arc's second control point swung across the shape instead of around the
 *    corner, turning chips and buttons into pointed lenses;
 *  - a *lobed* silhouette on a surface carrying a 13sp label pushes the text out of
 *    its own safe area and makes neighbouring controls read as merged;
 *  - a malformed outline is also a malformed **clip**, so a `Surface` clipped its own
 *    glyph away and the control rendered as an empty disc — which is what the header
 *    and the card hearts were doing.
 *
 * The corner scale has none of those failure modes, and it is the shape system
 * Material itself ships. The expressive library is still here and still animates, on
 * the focus ring and the loading sequence, where nothing is clipped and nothing has to
 * be read.
 */
object ShapeMorph {

    /**
     * Resting and active corner radii for a filter chip.
     *
     * Chips use a percent radius rather than these — see [ShapeMorph.pill].
     * These are here for the fixed-height surfaces that want to round further when
     * they are selected.
     */
    val chipRest: CornerScale = CornerScale.uniform(14.dp)
    val chipActive: CornerScale = CornerScale.uniform(20.dp)

    /** A circle at rest, squaring off as it is held. */
    val buttonRest: CornerScale = CornerScale.uniform(20.dp)
    val buttonPressed: CornerScale = CornerScale.uniform(11.dp)

    /** The rail's selection indicator. */
    val indicatorRest: CornerScale = CornerScale.uniform(14.dp)
    val indicatorActive: CornerScale = CornerScale.uniform(20.dp)

    /** The tile a channel's logo sits in: a rounded square that opens when focused. */
    val logoRest: CornerScale = CornerScale.uniform(20.dp)
    val logoActive: CornerScale = CornerScale.uniform(28.dp)

    /** The live dot: 4dp on an 8dp dot is a circle, flattening to 2dp as it pulses. */
    val liveRest: CornerScale = CornerScale.uniform(4.dp)
    val liveActive: CornerScale = CornerScale.uniform(2.dp)

    /**
     * A corner radius for a control of [size], as a fraction of its shorter side.
     *
     * The one rule a fixed `dp` radius has to obey and cannot check for itself:
     * **a corner radius must be less than half the shortest side.** Past that the four
     * arcs overlap each other and the shape renders as a lopsided blob — which is
     * exactly the failure the polygon library used to produce everywhere, now
     * reintroduced through the back door by hard-coding radii that were sized for a
     * different control.
     *
     * This came up for real when the vocabulary moved off polygons: a polygon is
     * refitted to whatever box it is drawn in and so can never exceed it, whereas a
     * `dp` radius carries whatever number it was given. A 20dp radius is a circle on a
     * 40dp button and a broken shape on the 36dp one, and on the 34dp-tall selection
     * indicator in the navigation rail.
     *
     * Sizing the radius as a fraction of the control, and clamping it here, makes the
     * whole class of bug unrepresentable: no call site can ask for a radius its
     * element is too small to carry.
     */
    fun cornerRadius(size: Dp, fraction: Float): Dp =
        (size.value * fraction).coerceAtMost(size.value / 2f).dp

    /**
     * A true pill, at any size.
     *
     * A percent radius is Material's own `Pill` and is exact everywhere: 50% of the
     * shorter side is a circle on a square element and a stadium on a wide one, with
     * no arithmetic to get wrong. Preferred over [cornerRadius] wherever the control
     * has no reason to be anything but a pill.
     */
    val pill: Shape = RoundedCornerShape(percent = 50)

    /**
     * The outline drawn around whatever currently holds D-pad focus.
     *
     * The one place the Expressive library still earns its keep, and the reason the
     * rest of it stayed. A focus ring is *stroked*, not filled: it clips nothing, it
     * contains no label, and it is the only element in the app whose entire job is to
     * be visually unlike everything else. "Where am I" should look different from
     * every other state in the app, and a nine-lobed cookie that slowly turns is a
     * far clearer answer to that than a rounded rectangle that got slightly rounder.
     */
    val focusRing: ExpressivePolygon = M3ExpressivePolygons.Cookie9Sided
}

/**
 * A shape that morphs between two polygons as [isActive] flips.
 *
 * The morph itself is built once and remembered, because lining the two vertex rings up
 * against each other is the expensive part; only the fraction between them changes as
 * the animation runs.
 *
 * @param rotation degrees of turn applied to the whole shape while the morph plays.
 *   A shape that only changes outline reads as a flat cross-fade; adding a quarter
 *   turn is what makes it read as one object turning into another.
 */
@Composable
fun rememberMorphingPolygon(
    rest: ExpressivePolygon,
    active: ExpressivePolygon,
    isActive: Boolean,
    spec: AnimationSpec<Float> = ExpressiveMotion.spatialFast,
    rotationWhileActive: Float = 0f
): Shape {
    val reduceMotion = LocalReduceMotion.current
    val morph = remember(rest, active) { ExpressiveMorph.between(rest, active) }

    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (reduceMotion) ExpressiveMotion.effectsDefault else spec,
        label = "ShapeMorphProgress"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isActive && !reduceMotion) rotationWhileActive else 0f,
        animationSpec = if (reduceMotion) ExpressiveMotion.effectsDefault else spec,
        label = "ShapeMorphRotation"
    )

    return MorphingPolygonShape(morph, progress, rotation)
}/**
 * A rounded-rectangle shape whose four corner radii animate between two states.
 *
 * This is the shape animation for every surface that carries a label, and it is why
 * the app can still be expressive without the lobed silhouettes ever getting near a
 * line of text. Rest and active are given in dp so the radius stays a real distance
 * regardless of how wide the element is — a corner radius that scaled with the
 * element would be a different shape on every screen.
 */
@Composable
fun rememberMorphingCorners(
    rest: CornerScale,
    active: CornerScale,
    isActive: Boolean,
    spec: AnimationSpec<Float> = ExpressiveMotion.spatialDefault
): Shape {
    val reduceMotion = LocalReduceMotion.current
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (reduceMotion) ExpressiveMotion.effectsDefault else spec,
        label = "CornerMorph"
    )
    return remember(rest, active, progress) { rest.lerp(active, progress).toShape() }
}

/** The four corner radii of a rounded rectangle, in dp. */
data class CornerScale(
    val topStart: Dp = 0.dp,
    val topEnd: Dp = 0.dp,
    val bottomEnd: Dp = 0.dp,
    val bottomStart: Dp = 0.dp
) {
    fun lerp(other: CornerScale, t: Float): CornerScale = CornerScale(
        topStart = lerpDp(topStart, other.topStart, t),
        topEnd = lerpDp(topEnd, other.topEnd, t),
        bottomEnd = lerpDp(bottomEnd, other.bottomEnd, t),
        bottomStart = lerpDp(bottomStart, other.bottomStart, t)
    )

    fun toShape(): Shape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    /**
     * A radius expressed as a fraction of the shorter side.
     *
     * 50 is a pill, 25 is a rounded square. [CornerScale] stores `Dp`, because a
     * fixed radius is what the animatable surfaces want; [ShapeMorph.pill] is the
     * percent-radius escape hatch for a control whose size is not known at the call
     * site.
     */

    companion object {
        /** Every corner the same. */
        fun uniform(radius: Dp) = CornerScale(radius, radius, radius, radius)
    }
}

private fun lerpDp(from: Dp, to: Dp, t: Float): Dp = from + ((to - from) * t)
