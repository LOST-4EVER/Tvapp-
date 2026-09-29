package com.example.kurdishtv.ui.motion

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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.kurdishtv.ui.theme.ExpressiveMorph
import com.example.kurdishtv.ui.theme.ExpressivePolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.fittedPath

/**
 * The D-pad focus ring.
 *
 * An Android TV app is driven almost entirely by a D-pad, and focus is its primary
 * state: everything else here is secondary to "where am I right now". The click
 * modifier deliberately passes `indication = null`, so without this a focused element
 * shows nothing at all and a viewer has no idea where they are.
 *
 * ## What it does, and what it used to do
 *
 * It draws a 3dp outline, outset from the element rather than inset into it, the
 * instant focus lands — and that is the whole behaviour.
 *
 * It used to spring into place, lift the element 5% while focused, fade in on one
 * spring and out on another, and rotate continuously underneath a nine-lobed cookie
 * outline. All of that is gone. Two things were lost and one was gained:
 *
 *  - The continuous rotation was the reason a focus ring could be spotted from across
 *    a room, and it is genuinely gone. What replaces it is weight: a 3dp outset ring
 *    in the accent colour is a larger, higher-contrast mark than a hairline stroke
 *    inset into a card, and unlike the rotation it is *still* there. On a
 *    low-contrast panel the ring is also backed by a scrim, which is the change that
 *    actually does the work of surviving a bright room.
 *  - The lift is gone, which is a small loss. It made the focused card read as
 *    nearer. The scrim and the ring carry that now.
 *
 * ## The element must not clip
 *
 * The ring is drawn outside the element's bounds, so a `Modifier.clip` applied
 * *before* it in the chain cuts the ring in half. Two surfaces used to do exactly
 * that — the sidebar row and the navigation rail item — and both were clipping for
 * no reason, because the background and the press fill were already shape-aware and
 * nothing inside them needed a hard edge. Their clips are gone.
 *
 * ## Why instant, not quick
 *
 * A highlight that takes a third of a second to arrive reads as the remote not
 * registering — which is the one failure this element exists to prevent. A
 * `snap()`-to-the-target animation would still create the animation, still register
 * a frame callback and still schedule a recomposition on the frame it landed, to
 * arrive at a picture identical to drawing it immediately. So it is drawn
 * immediately, and there is no animation object anywhere in this file.
 *
 * ## One element, one focus target
 *
 * The ring does **not** create a focus target. It observes the same
 * [MutableInteractionSource] the element's click already uses.
 *
 * It used to work the other way round: the click was told `focusable = false` so the
 * ring could supply a replacement, and the ring added `Modifier.focusable` of its own
 * further down the chain. That is two focus targets stacked on one card, and it
 * depended entirely on how far `focusProperties` reaches — if it applies to every
 * target below it in the chain, the ring's target inherited `canFocus = false` and
 * the card became unreachable by the remote while still working perfectly for a
 * finger. Which one is true is a framework detail, and an app cannot afford to bet
 * its D-pad on it.
 *
 * Letting the click keep its target removes the question. `Modifier.clickable` brings
 * a focus target unconditionally, and that is the only reason a control is reachable
 * by a remote at all — so it is the one the viewer lands on, the one the ring watches,
 * and the one a `FocusRequester] binds to. They cannot disagree, because they are the
 * same object.
 *
 * @param interactionSource the *same* source passed to [tvClickable] on this element.
 *   Pass it and no focus target is created here. Left null the ring owns its own
 *   target, which is the right answer only when the element has no click of its own.
 * @param outset how far outside the element's bounds the ring is drawn. Positive is
 *   outwards; the ring never covers the content it is marking.
 * @param scrim a band of colour laid under the ring, which is what keeps it legible
 *   against a bright or busy background. It fills the gap [outset] leaves between
 *   the element and the ring, and the ring is drawn over the outer part of it.
 *   Pass [Color.Transparent] for no scrim.
 *
 *   It has to be opaque to do that job. A translucent scrim was tried and left the
 *   bright logo it was meant to hide showing through the middle of the mark, so the
 *   call sites pass the page colour; [com.example.ui.theme.AppColors.focusScrim] is
 *   the role for it.
 * @param ringShape the outline. A rectangle by default — see the note above on why
 *   the lobed silhouette went.
 */
fun Modifier.expressiveFocusRing(
    enabled: Boolean = true,
    ringColor: Color,
    interactionSource: MutableInteractionSource? = null,
    ringWidth: Dp = ExpressiveMotion.Focus.strokeWidth,
    outset: Dp = ExpressiveMotion.Focus.outset,
    scrim: Color = Color.Transparent,
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

    // Reported from the same state the visuals are driven by, so the callback cannot
    // disagree with what is drawn.
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    LaunchedEffect(isFocused) { currentOnFocusChanged(isFocused) }

    // Lining the two outlines up is the expensive half, so it happens once per pair
    // rather than once per frame. The stroke and the scratch path are reused for the
    // same reason: a fresh `Stroke` per frame was an allocation per frame per ring.
    val morph = remember(restShape, ringShape) { ExpressiveMorph.between(restShape, ringShape) }
    val ringPath = remember { Path() }
    val density = LocalDensity.current
    val strokePx = with(density) { ringWidth.toPx() }
    val outsetPx = with(density) { outset.toPx() }
    val ringStroke = remember(strokePx) { Stroke(width = strokePx) }
    // The width that makes the scrim span from the element's own edge outward to
    // `outset + stroke`. See the note where it is drawn.
    val scrimStroke = remember(outsetPx, strokePx) { Stroke(width = outsetPx * 2f + strokePx) }

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
        .drawWithContent {
            drawContent()
            if (!isFocused) return@drawWithContent

            // Grown by the ring's own stroke so the outline sits entirely outside the
            // element: half the stroke inside, half out, plus the outset.
            val grown = (outsetPx + strokePx / 2f).coerceAtLeast(0f)
            val ringSize = Size(
                width = size.width + grown * 2f,
                height = size.height + grown * 2f
            )
            if (ringSize.minDimension <= 0f) return@drawWithContent

            // progress and rotation are both constants. A shape that does not change
            // outline and does not turn is a rounded rectangle, and building it
            // through the morph machinery is a few dozen points of arithmetic per
            // focus change, which is a few dozen more than this needs.
            morph.fittedPath(
                progress = 1f,
                size = ringSize,
                rotationDegrees = 0f,
                out = ringPath
            )

            // `fittedPath` puts the shape's **top-left** at the origin: it translates
            // by `-bounds.left`/`-bounds.top` and then scales, and a Compose matrix
            // scales about no pivot, so the outline it returns spans 0..ringSize
            // rather than being centred in it.
            //
            // Drawn as it comes, the ring therefore sits [outset] too far right and
            // down. Its top and left edges land on the element's own top-left corner,
            // where the stroke eats 1.5dp *into* the content it is supposed to be
            // marking and the 4dp outset is simply not there, while only the bottom
            // and right carry it. On a clipped row that asymmetry is what made the
            // ring look like an L: the two sides drawn outside the clip were the two
            // that got cut off. Shifting the whole thing back by `grown` centres it
            // and puts an even `outset` on all four sides.
            translate(left = -grown, top = -grown) {
                // The scrim is what keeps the ring legible against a bright logo or a
                // light panel, and it works by being a band of page colour in the gap
                // that the outset left between the element and the ring.
                //
                // At the ring's own width it is painted at exactly the same place as
                // the ring drawn immediately after it, so it contributed nothing at
                // all — six call sites passed a colour and got no band. At
                // `2 * outset + stroke` it spans from the element's edge out past the
                // ring, so the ring lands on the outer part of it and the inner part
                // is the visible gap.
                if (scrim != Color.Transparent) {
                    drawPath(path = ringPath, color = scrim, style = scrimStroke)
                }
                drawPath(path = ringPath, color = ringColor, style = ringStroke)
            }
        }
}

/** A [FocusRequester] for programmatically focusing an element, e.g. the first card. */
@Composable
fun rememberTvFocusRequester(): FocusRequester = remember { FocusRequester() }
