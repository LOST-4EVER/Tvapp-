package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.theme.ExpressiveMorph
import com.example.kurdishtv.ui.theme.ExpressivePolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.fittedPath

/**
 * Material 3 Expressive focus treatment for D-pad and TV remotes.
 *
 * An Android TV app is driven almost entirely by a D-pad, and focus is its primary
 * state: everything else in this app is secondary to the question "where am I right
 * now". `bouncyClickable` deliberately sets `indication = null` for its springy press
 * feedback, so without a focus treatment a focused element shows nothing at all and a
 * user has no idea where they are.
 *
 * That treatment is [tvFocusable] — a ring in the surface's own shape, drawn only
 * while focused, with a springy lift. [expressiveFocusRing] is the same idea taken
 * further, and it is the single most useful thing Expressive gives a TV app: a ring
 * that is a *shape* rather than a rectangle, morphing as it appears and turning slowly
 * while it is there.
 *
 * The reason this works on a television, where a static ring is often missed, is that
 * the ring is the only continuously moving thing on screen. A user looking up from the
 * sofa can see that something is turning, and turn their attention to the right place
 * without reading anything.
 *
 * Touch input is entirely unaffected: the ring only draws when the element actually
 * has focus, and it is removed again the moment focus leaves.
 */
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    focusScale: Float = 1.04f,
    ringWidth: Dp = 3.dp,
    ringColor: Color,
    shape: Shape,
    onFocusChanged: (isFocused: Boolean) -> Unit = {}
): Modifier = composed {
    // No `if (!enabled) return@composed this` here or in expressiveFocusRing below.
    // An early return would make the number of remembered values depend on a
    // parameter, and a runtime change to it is exactly the slot-table shape change
    // Compose refuses to reconcile. `enabled` is instead honoured where it matters:
    // the focus target and the ring.
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current

    // Report focus changes from the same state the visuals are driven by, so the
    // callback cannot disagree with what is drawn.
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    LaunchedEffect(isFocused) { currentOnFocusChanged(isFocused) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !reduceMotion) focusScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TvFocusScale"
    )

    this
        .focusable(enabled = enabled, interactionSource = interactionSource)
        // As in `expressiveFocusRing`: the layer is only worth having while the
        // element is actually lifted.
        .then(
            if (scale == 1f) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            }
        )
        // Only while focused. The ring used to be applied unconditionally, which put a
        // 3dp accent outline on *every* card in the grid — the grid read as though
        // everything were selected at once, and the accent colour competed with the
        // LIVE badge and the artwork.
        .then(
            if (isFocused) Modifier.border(BorderStroke(ringWidth, ringColor), shape) else Modifier
        )
}

/**
 * A focus ring that is itself an animated Material 3 Expressive shape.
 *
 * Three things happen, all on one shared transition so only the focused element ever
 * animates:
 *
 *  1. the ring's outline springs from the element's own resting shape into a rounded
 *     expressive one, so gaining focus reads as the surface changing rather than as a
 *     line being switched on;
 *  2. it turns, driven by one app-wide clock ([LocalFocusRotation]) — slowly, so it
 *     reads as a shape rather than a blur;
 *  3. the element lifts, as in [tvFocusable].
 *
 * @param ringShape the shape the ring settles into. [ShapeMorph.focusRing] by default.
 * @param restShape the shape it grows out of, usually the surface's own outline. Pass
 *   the same shape the surface is clipped with and the ring looks like the surface
 *   blooming outward.
 * @param focusScale how far the element lifts while focused. Set this to 1f when
 *   something else on the same node already lifts it, or the two multiply and a
 *   "gentle 5%" turns into a 10% jump.
 */
fun Modifier.expressiveFocusRing(
    enabled: Boolean = true,
    ringColor: Color,
    focusScale: Float = 1.05f,
    ringWidth: Dp = 2.5.dp,
    restShape: ExpressivePolygon = M3ExpressivePolygons.Square,
    ringShape: ExpressivePolygon = ShapeMorph.focusRing,
    onFocusChanged: (isFocused: Boolean) -> Unit = {}
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current

    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    LaunchedEffect(isFocused) { currentOnFocusChanged(isFocused) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !reduceMotion) focusScale else 1f,
        animationSpec = ExpressiveMotion.pressSpring,
        label = "ExpressiveFocusScale"
    )

    // Lining the two outlines up is the expensive half, so it happens once per pair
    // rather than once per frame.
    val morph = remember(restShape, ringShape) { ExpressiveMorph.between(restShape, ringShape) }
    val ringPath = remember { Path() }
    val strokeWidth = with(LocalDensity.current) { ringWidth.toPx() }

    // One app-wide turn, not one per ring.
    //
    // This used to be `key(isFocused) { rememberInfiniteTransition() ... }`, which was
    // already better than an unconditional loop but still wrong for a D-pad: every
    // arrow press tore down a `withFrameNanos` loop on the card losing focus and
    // started a fresh one on the card gaining it, so holding a direction key down
    // churned a frame callback per card per step. There is only ever one ring drawn,
    // so there is only ever one loop needed. See [LocalFocusRotation].
    val sharedRotation = LocalFocusRotation.current
    val reveal by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = ExpressiveMotion.spatialDefault,
        label = "FocusRingReveal"
    )

    this
        .focusable(enabled = enabled, interactionSource = interactionSource)
        // The layer exists only while the element is lifted. Six hundred cards each
        // holding a permanently scaled render node is the same problem as the other
        // two layers this screen used to carry; see `bouncyClickable`.
        .then(
            if (scale == 1f) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            }
        )
        .drawWithContent {
            drawContent()
            if (isFocused) {
                // Inset by half the stroke so the ring sits inside the element's own
                // bounds rather than straddling its neighbour's.
                val inset = strokeWidth / 2f
                val ringSize = Size(
                    width = (size.width - inset * 2f).coerceAtLeast(0f),
                    height = (size.height - inset * 2f).coerceAtLeast(0f)
                )
                if (ringSize.minDimension > 0f) {
                    morph.fittedPath(
                        progress = reveal,
                        size = ringSize,
                        rotationDegrees = if (reduceMotion) 0f else sharedRotation,
                        out = ringPath
                    )
                    drawPath(
                        path = ringPath,
                        color = ringColor,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
}

/** A [FocusRequester] for programmatically focusing an element, e.g. the first card. */
@Composable
fun rememberTvFocusRequester(): FocusRequester = remember { FocusRequester() }
