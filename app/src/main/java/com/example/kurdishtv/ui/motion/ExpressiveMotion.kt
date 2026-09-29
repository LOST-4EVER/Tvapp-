package com.example.kurdishtv.ui.motion

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The two numbers the interaction layer needs, and nothing else.
 *
 * ## Why there is no motion system here
 *
 * This object used to be the app's motion system: a table of Material 3 Expressive
 * spring specs and easing curves, two app-wide frame loops (a shared LIVE-badge
 * pulse and a shared focus-ring rotation), a staggered grid entrance, and a
 * reduced-motion switch that quietly disabled the loops while leaving the code
 * that drove them in place.
 *
 * All of that is gone, and it is gone rather than switched off. The difference
 * matters: a `snap()` spec still creates an animation, still registers a frame
 * callback, still schedules a recomposition for the frame it finishes on, and
 * still leaves a lambda to be reasoned about at every call site. On a
 * six-hundred-channel grid multiplied across a television's whole navigation
 * graph, "animates to the same value instantly" is a real cost paid to arrive
 * at a picture identical to not animating at all.
 *
 * So there is no `AnimationSpec` in this file, no `Easing`, no `withFrameNanos`
 * and no `animateFloatAsState`. What is left is the geometry of *feedback* —
 * how heavy the focus ring is and how strongly a press is acknowledged — which
 * is where a no-motion interface still has real decisions to make.
 *
 * The name is kept because "motion" is still the vocabulary these tokens are
 * referenced through elsewhere, and renaming a symbol in thirty files to say less
 * would not make the code clearer.
 */
object ExpressiveMotion {

    /**
     * The D-pad focus ring.
     *
     * Stroke width, and how far outside the element it is drawn.
     *
     * A television viewer reads this from across a room, so it is heavier than a
     * touch focus ring would be — 3dp against the 1dp hairline the cards carry at
     * rest. That contrast is now doing the work the ring's continuous rotation used
     * to do, and it has one advantage the rotation did not: it is still there.
     *
     * Outset rather than inset, so a focused card's own outline and artwork are
     * left unobstructed by the mark identifying it.
     */
    object Focus {
        val strokeWidth: Dp = 3.dp

        /** The gap between the element's bounds and the inside of the ring. */
        val outset: Dp = 4.dp
    }

    /**
     * Press feedback.
     *
     * Interaction still has to be *visible* without animating. On a touch screen a
     * press has to be acknowledged in under a frame or the screen feels broken; on
     * a television the D-pad press is a *hold* of the OK button, sometimes for a
     * noticeable fraction of a second, and nothing happening for that long reads as
     * an unresponsive app.
     *
     * [heldAlpha] is a flat fill laid over the element while it is down, and it is
     * strong enough to read on a badly-calibrated panel.
     */
    object Press {
        val heldAlpha: Float = 0.16f
    }
}
