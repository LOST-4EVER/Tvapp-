package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.animateFloatAsState
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
 * Material 3 Expressive focus treatment for D-pads and TV remotes.
 *
 * An Android TV app is driven almost entirely by a D-pad, and focus is its primary
 * state: everything else in this app is secondary to the question "where am I right
 * now". `bouncyClickable` deliberately sets `indication = null` for its springy press
 * feedback, so without a focus treatment a focused element shows nothing at all and a
 * user has no idea where they are.
 *
 * That treatment is [expressiveFocusRing]: a ring in an Expressive shape that springs
 * into place and turns slowly while it is there. It is the single most useful thing
 * Expressive gives a TV app — a ring that is a *shape* rather than a rectangle — and it
 * works on a television because the ring is the only continuously moving thing on
 * screen. A viewer looking up from the sofa can see that something is turning and
 * turn their attention to the right place without reading anything.
 *
 * ## One element, one focus target
 *
 * The ring does **not** create a focus target. It observes the same
 * [MutableInteractionSource] the element's click already uses, which is the arrangement
 * this file was rewritten to arrive at.
 *
 * It used to work the other way round. `bouncyClickable(focusable = false)` removed the
 * click's own focus target with `Modifier.focusProperties { canFocus = false }` — on the
 * reasoning that the ring would supply a replacement — and the ring then added
 * `Modifier.focusable` of its own further down the chain. That is two focus targets
 * stacked on one card, and it depended entirely on how far `focusProperties` reaches:
 * if it applies to every target below it in the chain, the ring's target inherited
 * `canFocus = false` and the card became unreachable by the remote while still working
 * perfectly for a finger; if it is scoped to the next target only, the arrangement
 * worked. Which one is true is a framework detail, and an app cannot afford to bet its
 * D-pad on it.
 *
 * Letting the click keep its target removes the question. `Modifier.clickable` brings a
 * focus target unconditionally and unambiguously — it is the only reason a control is
 * reachable by a remote at all — so that is the one the viewer lands on, the one the
 * ring watches, and the one a `FocusRequester` binds to. They cannot disagree, because
 * they are the same object.
 *
 * @param interactionSource the *same* source passed to
 *   [bouncyClickable] on this element. Pass it and no focus target is created here.
 *   Left null the ring owns its own target, which is the right answer only when the
 *   element has no click of its own.
 * @param focusScale how far the element lifts while focused. Set this to 1f when
 *   something else on the same node already lifts it, or the two multiply and a
 *   "gentle 5%" turns into a 10% jump.
 */
fun Modifier.expressiveFocusRing(
    enabled: Boolean = true,
    ringColor: Color,
    interactionSource: MutableInteractionSource? = null,
    focusScale: Float = 1.05f,
    ringWidth: Dp = 2.5.dp,
    restShape: ExpressivePolygon = M3ExpressivePolygons.Square,
    ringShape: ExpressivePolygon = ShapeMorph.focusRing,
    onFocusChanged: (isFocused: Boolean) -> Unit = {}
): Modifier = composed {
    // The caller's source when there is one, so the ring and the click cannot report
    // different answers. Owning a private one is only correct when this modifier is
    // the element's sole focus target.
    val ownsTarget = interactionSource == null
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current

    // Only a *focused* element draws a ring, so only a focused one is a reader of the
    // shared turn. Registering that here is what lets the loop behind
    // [rememberFocusRotation] stop: there is one ring drawn at a time, and for most of
    // a session — in Settings, on the player, before the viewer has touched the remote
    // at all — there is no reader, so there is no reason to be waking the CPU sixty
    // times a second to compute an angle nobody is drawing.
    WatchFocusRotation(isFocused)

    // Reported from the same state the visuals are driven by, so the callback cannot
    // disagree with what is drawn.
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    LaunchedEffect(isFocused) { currentOnFocusChanged(isFocused) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !reduceMotion) focusScale else 1f,
        animationSpec = ExpressiveMotion.pressSpring,
        label = "ExpressiveFocusScale"
    )

    // The ring appears immediately and leaves slowly.
    //
    // It shared `spatialDefault` with the lift above, which is a ~350ms spring. That is
    // the wrong shape for the *outgoing* direction of a D-pad press: the viewer has
    // already decided where they are going, and a highlight that takes a third of a
    // second to arrive reads as the remote having not registered. An effects token is
    // critically damped and effectively instant, so the ring is simply there on the
    // frame focus lands. The exit keeps the spring, because a ring that vanishes
    // instantly while the element settles back reads as a flicker.
    val reveal by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = if (isFocused) ExpressiveMotion.effectsFast
        else ExpressiveMotion.spatialDefault,
        label = "FocusRingReveal"
    )

    // Lining the two outlines up is the expensive half, so it happens once per pair
    // rather than once per frame. The stroke and the scratch path are reused for the
    // same reason: a fresh `Stroke` per frame was an allocation per frame per ring.
    val morph = remember(restShape, ringShape) { ExpressiveMorph.between(restShape, ringShape) }
    val ringPath = remember { Path() }
    val strokeWidth = with(LocalDensity.current) { ringWidth.toPx() }
    val ringStroke = remember(strokeWidth) { Stroke(width = strokeWidth) }

    // Neither of the two *continuously* animated inputs is read here, and that is the
    // point.
    //
    // Both change on every frame while a ring is on screen. Read during composition,
    // they invalidate *this* modifier on every frame — which rebuilds the entire
    // modifier chain of every element that carries a ring, and in the channel grid that
    // is several hundred of them, sixty times a second. They are State objects read
    // inside the draw lambda below, where a change invalidates the drawing of the one
    // node that actually read it and nothing else.
    //
    // `scale` above is the exception, and it is not an oversight: it moves only on a
    // focus change, and the read is what lets the layer be dropped entirely at rest.
    // One node recomposing for the length of one focus transition is a great deal
    // cheaper than several hundred render nodes held to multiply by one.
    //
    // The turn itself is app-wide — see [LocalFocusRotation]. It used to be
    // `key(isFocused) { rememberInfiniteTransition() ... }`, which was already better
    // than one loop per card but still wrong for a D-pad: every arrow press tore down a
    // `withFrameNanos` loop on the card losing focus and started a fresh one on the
    // card gaining it, so holding a direction key down churned a frame callback per
    // card per step. There is only ever one ring drawn, so there is only ever one loop
    // needed.
    val rotation = LocalFocusRotation.current

    this
        // A focus target only when this element has no click of its own. When a shared
        // source arrives, `clickable`'s target is the target and adding one here would
        // put the card back to two stacked stops.
        .then(
            if (ownsTarget) {
                Modifier.focusable(enabled = enabled, interactionSource = source)
            } else {
                Modifier
            }
        )
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
                        rotationDegrees = if (reduceMotion) 0f else rotation.floatValue,
                        out = ringPath
                    )
                    drawPath(
                        path = ringPath,
                        color = ringColor,
                        style = ringStroke
                    )
                }
            }
        }
}

/** A [FocusRequester] for programmatically focusing an element, e.g. the first card. */
@Composable
fun rememberTvFocusRequester(): FocusRequester = remember { FocusRequester() }
