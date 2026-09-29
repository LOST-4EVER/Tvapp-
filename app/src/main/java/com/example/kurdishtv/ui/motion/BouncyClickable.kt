package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Material 3 Expressive press feedback: the element springs down on press and bounces
 * back on release. Honours the user's reduced-motion preference.
 *
 * Also focusable, so the element is reachable with a D-pad or TV remote: the click
 * modifier this wraps brings a focus target of its own, and that target is the one
 * the tab order uses. Because `indication = null` removes the default click ripple,
 * focus is signalled by a small springy scale-up — or, on larger surfaces, by
 * [expressiveFocusRing], which can be handed this element's [interactionSource] so
 * that the ring and the click are driven by one piece of state.
 *
 * @param interactionSource pass an existing source when the caller also needs to
 *   observe the press — that is how [com.example.kurdishtv.ui.components.AppIconButton]
 *   morphs its outline while it is held. Left null, one is created here.
 * @param focusable set false when the element should be **removed from the D-pad tab
 *   order entirely**. `Modifier.clickable` brings a focus target of its own and there is
 *   no flag to switch that off, so this marks the click's target `canFocus = false`,
 *   which takes it out of traversal.
 *
 *   Do **not** set it false in order to hand focus to an
 *   [expressiveFocusRing]. That was the old arrangement and it was wrong: it put two
 *   focus targets on one card and relied on `Modifier.focusProperties` not reaching the
 *   ring's, which is a framework detail the D-pad should not be betting on. Pass the
 *   same `interactionSource` to both instead and leave this at its default — the click
 *   keeps its target, the ring watches it, and there is exactly one stop per element.
 *
 *   `focusable = false` remains correct where there is no ring underneath and the
 *   element genuinely should not be reachable — a card's heart, whose action is a long
 *   press on the card instead. See [tapOnly] for a surface that should never be a stop
 *   at all.
 * @param liftOnFocus draw a small springy scale-up while the element holds D-pad
 *   focus. Turn this off when something else on the same node is already animating
 *   focus — [expressiveFocusRing] lifts and draws the ring together, and two lifts
 *   multiply.
 * @param onLongClick when non-null the element gains a long-press action, which is
 *   how a D-pad user reaches an action that is deliberately kept out of the tab
 *   order. See [com.example.kurdishtv.ui.components.ChannelCard].
 */
// `combinedClickable` — the long-press path — is still flagged experimental, where
// `clickable` is not. Opting in here rather than at every call site keeps the
// long-press an implementation detail of this modifier: callers never name the API.
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    interactionSource: MutableInteractionSource? = null,
    focusable: Boolean = true,
    liftOnFocus: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isFocused by source.collectIsFocusedAsState()

    val targetScale = when {
        reduceMotion -> 1f
        isPressed && enabled -> scaleDown
        // D-pad focus gets a gentle lift, mirroring the press response.
        liftOnFocus && isFocused && enabled -> 1.04f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = ExpressiveMotion.pressSpring,
        label = "BouncyScaleAnimation"
    )

    // See the note on `focusable`: a click brings its own focus target, so asking not
    // to be focusable has to say so to the click as well.
    //
    // Unambiguous here because there is nothing focusable below it in the chain — the
    // ring case that used to sit in this slot now passes a shared source instead.
    val focusOverride =
        if (focusable) Modifier else Modifier.focusProperties { canFocus = false }

    this
        // The layer is applied *only while something is actually scaling*.
        //
        // A `graphicsLayer` is not free and it is not free once: each one is a
        // separate render node with its own display list, and on a device with a
        // limited layer budget it can force an off-screen texture the size of the
        // element. This modifier is on every channel card, every chip and every
        // icon button in the app — six hundred-odd cards in the grid — and for
        // all but the one under the viewer's finger the scale is exactly 1f, so
        // the layer was allocating and compositing hundreds of render nodes in
        // order to multiply by one.
        //
        // Toggling the modifier (rather than leaving it in place and writing 1f
        // into it) is safe: the composition is identical either way, only the
        // modifier chain changes, and it is rebuilt twice per interaction rather
        // than once per frame.
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
        )            .then(
                // **Not** `Modifier.focusable(...)` here, which is what used to be on
                // this line and is a D-pad bug on every control in the app.
                //
                // `clickable` and `combinedClickable` each bring a focus target of
                // their own — that is the *only* way they are reachable by a remote at
                // all. Adding a second `focusable` on top of it did not make the
                // control more reachable; it gave it two focus targets stacked on the
                // same 34dp box, and focus traversal picked between them. The one the
                // viewer landed on was not necessarily the one this modifier was
                // watching, so the press lift and the focus ring could disagree with
                // where the highlight actually was.
                //
                // So: when the control is focusable, the click's own target is the
                // target, and this modifier is a no-op. When it is not, the
                // `focusOverride` above has already taken that target out of the tab
                // order and lets focus fall through to a ring's target below.
                Modifier
            )
}

/**
 * A tap target that is deliberately **not** part of the D-pad tab order.
 *
 * This is the one case [bouncyClickable] cannot express. `Modifier.clickable` always
 * brings a focus target, and the only way to take one out of the tab order is
 * `Modifier.focusProperties { canFocus = false }` — which applies to every focus target
 * below it, so on a *container* it takes the contents down with it.
 *
 * For a full-screen "tap anywhere" scrim that leaves no good answer: as a
 * `clickable` it is the largest and first focus target on the player and swallows the
 * first press of every direction key, and marked unfocusable it takes the transport
 * controls inside it out of the tab order as well.
 *
 * So this takes the lower road and handles the tap in a pointer handler, which creates
 * no focus target to begin with. Reach for it when a surface is a convenience target
 * for a finger rather than something a remote should ever land on.
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
