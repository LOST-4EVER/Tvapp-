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
 *  - [rememberMorphingPolygon] morphs between two [ExpressivePolygon]s. It is
 *    expressive — cookies, clovers, bursts — and it belongs on surfaces that hold no
 *    glyph, no label and no touch target, because a lobed silhouette on a surface you
 *    have to read pushes text out of the safe area and makes neighbouring controls
 *    look merged.
 *  - [rememberMorphingCorners] morphs between two corner-radius scales. It is
 *    restrained, and it is what every labelled surface uses. Material lists rounded
 *    corners as a spatial-spring property, so animating them is not a compromise; it
 *    is the spec's own suggestion.
 */
object ShapeMorph {

    /**
     * Resting and active shapes for a filter chip.
     *
     * A pill that flattens slightly when selected. A full morph to a lobed cookie was
     * tried and rejected: chips are wide, and a seven-lobed shape stretched across a
     * 140dp-wide chip stops reading as a chip at all.
     */
    val chipRest: ExpressivePolygon = M3ExpressivePolygons.Pill
    val chipActive: ExpressivePolygon = M3ExpressivePolygons.Pill.scaled(1f, 0.82f)

    /** A plain circle and a rounded square: the clearest possible shape-as-state change. */
    val buttonRest: ExpressivePolygon = M3ExpressivePolygons.Circle
    val buttonPressed: ExpressivePolygon = M3ExpressivePolygons.Square

    /** The rail's selection indicator, and a slowly turning version of it. */
    val indicatorRest: ExpressivePolygon = M3ExpressivePolygons.Pill
    val indicatorActive: ExpressivePolygon = M3ExpressivePolygons.VerySunny

    /** The outline drawn around whatever currently holds D-pad focus. */
    val focusRing: ExpressivePolygon = M3ExpressivePolygons.Cookie9Sided

    /** The tile a channel's logo sits in: a plain square at rest, organic when focused. */
    val logoRest: ExpressivePolygon = M3ExpressivePolygons.Square
    val logoActive: ExpressivePolygon = M3ExpressivePolygons.Puffy

    /** The live dot: a circle that opens into a very slightly lobed sun as it pulses. */
    val liveRest: ExpressivePolygon = M3ExpressivePolygons.Circle
    val liveActive: ExpressivePolygon = M3ExpressivePolygons.VerySunny
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
}

/**
 * A pill whose silhouette changes when it becomes selected.
 *
 * Kept as a named helper because the category bar is the one place in the app where a
 * shape morph carries real meaning: the selected tab has to be identifiable from
 * across a room, and on a television that is often the only cue available.
 */
@Composable
fun rememberMorphingPillShape(
    selected: Boolean,
    reduceMotion: Boolean = LocalReduceMotion.current
): Shape = rememberMorphingPolygon(
    rest = ShapeMorph.chipRest,
    active = ShapeMorph.chipActive,
    isActive = selected,
    spec = if (reduceMotion) ExpressiveMotion.effectsDefault else ExpressiveMotion.spatialDefault,
    // A small turn, not a quarter. The chip is wide and holds a label, so a large
    // rotation visibly distorts the text; a slight lean is enough to read as motion.
    rotationWhileActive = 12f
)

/**
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

    companion object {
        /** Every corner the same. */
        fun uniform(radius: Dp) = CornerScale(radius, radius, radius, radius)
    }
}

private fun lerpDp(from: Dp, to: Dp, t: Float): Dp = from + ((to - from) * t)
