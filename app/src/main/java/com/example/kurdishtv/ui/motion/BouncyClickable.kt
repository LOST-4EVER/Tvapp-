package com.example.kurdishtv.ui.motion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The app's click modifier.
 *
 * ## What it used to do
 *
 * This was `bouncyClickable`, and it drove a `Modifier.graphicsLayer` scale through
 * `animateFloatAsState` on a spring: 0.93 while held, 1.04 while D-pad focused, back
 * to 1 on release. Every channel card, every chip and every icon button in the app
 * carried that layer, and every one of them was multiplying by exactly 1.0 for all
 * but the single element the viewer was touching.
 *
 * The scale is gone. What is left is a press *state* that changes instantly, drawn
 * as a flat fill.
 *
 * ## Why a fill rather than nothing
 *
 * Removing the motion does not mean removing the feedback. On a touch screen a press
 * has to be acknowledged in under a frame or the screen feels broken; on a television
 * the D-pad press is a *hold* of the OK button, sometimes for a noticeable fraction
 * of a second, and nothing at all happening for that long reads as an unresponsive
 * app. An instant fill gives the same acknowledgement without a spring, without a
 * render node per card, and without the compositor being asked to re-layer several
 * hundred of them.
 *
 * Focus is not drawn here. It is [expressiveFocusRing]'s job, and it shares the
 * [interactionSource] this modifier is driven by so the two cannot disagree about
 * where the viewer is.
 *
 * @param interactionSource pass an existing source when the caller also needs to
 *   observe the press, or when a focus ring underneath shares it. Left null, one is
 *   created here.
 * @param focusable set false when the element should be **removed from the D-pad tab
 *   order entirely**. `Modifier.clickable` brings a focus target of its own and there
 *   is no flag to switch that off, so this marks the click's target `canFocus = false`,
 *   which takes it out of traversal.
 *
 *   Do **not** set it false in order to hand focus to an [expressiveFocusRing]. Pass
 *   the same `interactionSource` to both instead: the click keeps its target, the ring
 *   watches it, and there is exactly one stop per element. Two stacked focus targets
 *   on one card is a D-pad bug, because traversal picks between them and the viewer
 *   lands on one of them rather than the one the visuals are watching.
 *
 *   `focusable = false` remains correct where there is no ring underneath and the
 *   element genuinely should not be reachable — a card's heart, whose action is a
 *   long press on the card instead.
 * @param pressedFill a flat colour laid over the element while it is held, and
 *   [pressedShape] the outline to lay it in. Both are opt-in: pass a fill and the
 *   element acknowledges a press instantly. The state is read during composition, so
 *   this costs two recompositions per press — down, up — and nothing in between. A
 *   `graphicsLayer` scaled by a spring cost a render node *permanently*, on every
 *   card, for the whole time the grid was on show.
 * @param onLongClick when non-null the element gains a long-press action, which is how
 *   a D-pad user reaches an action deliberately kept out of the tab order. See
 *   [com.example.kurdishtv.ui.components.ChannelCard].
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    focusable: Boolean = true,
    pressedFill: Color? = null,
    pressedShape: Shape = RectangleShape,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    // See the note on `focusable`: a click brings its own focus target, so asking
    // not to be focusable has to say so to the click as well.
    val focusOverride =
        if (focusable) Modifier else Modifier.focusProperties { canFocus = false }

    this
        .then(
            if (pressedFill != null && isPressed) {
                Modifier.background(pressedFill, pressedShape)
            } else {
                Modifier
            }
        )
        .then(focusOverride)
        .then(
            if (onLongClick == null) {
                Modifier.clickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
            }
        )
        // **Not** `Modifier.focusable(...)` here. `clickable` and
        // `combinedClickable` each bring a focus target of their own — that is the
        // only reason a control is reachable by a remote at all. Adding a second one
        // on top gave every control two stops on the same box, and focus traversal
        // picked between them, so the press fill and the focus ring could disagree
        // with where the highlight actually was.
        //
        // When the control is focusable, the click's own target is the target and
        // this modifier is a no-op. When it is not, the `focusOverride` above has
        // already taken that target out of the tab order.
}

/**
 * A tap target that is deliberately **not** part of the D-pad tab order.
 *
 * This is the one case [tvClickable] cannot express. `Modifier.clickable` always
 * brings a focus target, and the only way to take one out of the tab order is
 * `Modifier.focusProperties { canFocus = false }` — which applies to every focus
 * target below it, so on a *container* it takes the contents down with it.
 *
 * For a full-screen "tap anywhere" scrim that leaves no good answer: as a
 * `clickable` it is the largest and first focus target on the player and swallows
 * the first press of every direction key, and marked unfocusable it takes the
 * transport controls inside it out of the tab order as well.
 *
 * So this takes the lower road and handles the tap in a pointer handler, which
 * creates no focus target to begin with. Reach for it when a surface is a
 * convenience target for a finger rather than something a remote should land on.
 */
fun Modifier.tapOnly(
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier = composed {
    val currentOnTap by rememberUpdatedState(onTap)
    this.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures { currentOnTap() }
    }
}
