package com.example.kurdishtv.ui.motion

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.theme.ExpressivePolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons

/**
 * Shape selection, without the animation.
 *
 * ## What changed
 *
 * This file used to hold the app's whole shape-animation layer. `rememberMorphingCorners`
 * and `rememberMorphingPolygon` both drove `animateFloatAsState` between two shapes,
 * so a filter chip squeezed when it was selected, a card's logo well opened when it
 * was focused, and the navigation rail's indicator rounded as the D-pad moved along
 * it. All of that moved; all of it has been deleted.
 *
 * The two functions are kept — renamed to [staticCornerShape] and
 * [staticPolygonShape] — because they are now doing something genuinely useful
 * that is not about animation: they collapse a *pair* of shapes down to the one
 * that applies right now. Every call site had to choose between two shapes on the
 * basis of a single boolean, and doing that in one place means the choice, and
 * the geometry that goes with it, is written once.
 *
 * The parameters the animation needed — a spec, a rotation, a progress fraction —
 * are gone rather than left at defaults that would be ignored, so no call site
 * can quietly ask for motion that is not there.
 */
object ShapeMorph {

    /** The tile a channel's logo sits in. */
    val logoRest: CornerScale = CornerScale.uniform(16.dp)

    /**
     * A corner radius for a control of [size], as a fraction of its shorter side.
     *
     * The one rule a hard-coded `dp` radius has to obey and cannot check for
     * itself: **a corner radius must be less than half the shortest side.** Past
     * that the four arcs overlap each other and the shape renders as a lopsided
     * blob — which is what a fixed radius does to a control that turns out to be
     * smaller than the one it was sized for.
     *
     * Sizing the radius as a fraction of the control, and clamping it here, makes
     * that whole class of bug unrepresentable: no call site can ask for a radius
     * its element is too small to carry.
     */
    fun cornerRadius(size: Dp, fraction: Float): Dp =
        (size.value * fraction).coerceAtMost(size.value / 2f).dp

    /**
     * A true pill, at any size.
     *
     * A percent radius is exact everywhere: 50% of the shorter side is a circle on
     * a square element and a stadium on a wide one, with no arithmetic to get
     * wrong.
     */
    val pill: Shape = RoundedCornerShape(percent = 50)

    /**
     * The outline drawn around whatever currently holds D-pad focus.
     *
     * A rounded rectangle. It used to be a nine-lobed cookie that slowly turned,
     * and the turning was the argument for it: on a television the ring is the
     * only thing that moves, so a viewer looking up from the sofa could see that
     * something was turning and aim their attention at it without reading
     * anything. With the motion gone there is nothing left to catch the eye but
     * weight and contrast, so this is a plain, heavy, square-cornered outline
     * drawn outside the element.
     */
    val focusRing: ExpressivePolygon = M3ExpressivePolygons.Square
}

/**
 * The shape for a surface that has two, and a boolean saying which one.
 *
 * Not a morph any more — the second shape is ignored and the first is returned
 * unchanged. Only the navigation rail's selection indicator still calls it, and
 * it will be simplified away rather than kept alive for symmetry with a system
 * that no longer exists.
 */
@Composable
fun staticCornerShape(
    rest: CornerScale,
    @Suppress("UNUSED_PARAMETER") active: CornerScale,
    @Suppress("UNUSED_PARAMETER") isActive: Boolean
): Shape = remember(rest) { rest.toShape() }

/** The four corner radii of a rounded rectangle, in dp. */
data class CornerScale(
    val topStart: Dp = 0.dp,
    val topEnd: Dp = 0.dp,
    val bottomEnd: Dp = 0.dp,
    val bottomStart: Dp = 0.dp
) {
    fun toShape(): Shape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    /** Every corner the same. */
    companion object {
        fun uniform(radius: Dp) = CornerScale(radius, radius, radius, radius)
    }
}
