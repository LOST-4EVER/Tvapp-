package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
 * Also focusable, so the element is reachable with a D-pad or TV remote. Because
 * `indication = null` removes the default click ripple, focus is signalled by a small
 * springy scale-up — see [tvFocusable] and [expressiveFocusRing] for the ringed
 * variants used on larger surfaces.
 *
 * @param interactionSource pass an existing source when the caller also needs to
 *   observe the press — that is how [com.example.kurdishtv.ui.components.AppIconButton]
 *   morphs its outline while it is held. Left null, one is created here.
 * @param focusable set false when the element already gets its focus handling from
 *   somewhere else — pair it with [expressiveFocusRing], which supplies the focus
 *   behaviour and draws it.
 *
 *   Setting it false does more than skip the `focusable` modifier below, and it has to.
 *   `Modifier.clickable` brings a focus target of its own, so a plain `focusable = false`
 *   left every such element a D-pad stop *anyway* — and, worse, a stop that was not the
 *   one the caller's focus ring was watching, so the ring never lit. The click is
 *   therefore marked `canFocus = false` as well, which takes its focus target out of the
 *   tab order and lets focus fall through to the ring's own target underneath it: one
 *   stop on the element, owned by the ring. This is what makes the flag honest for
 *   cards, hero tiles, recent chips and rail items alike.
 *
 *   Use [tapOnly] instead when the element should never be a stop at all.
 * @param liftOnFocus draw a small springy scale-up while the element holds D-pad
 *   focus. Turn this off when something else on the same node is already animating
 *   focus — [expressiveFocusRing] lifts and draws the ring together, and two lifts
 *   multiply.
 * @param onLongClick when non-null the element gains a long-press action, which is
 *   how a D-pad user reaches an action that is deliberately kept out of the tab
 *   order. See [com.example.kurdishtv.ui.components.ChannelCard].
 */
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

    // See the note on `focusable`: a click brings its own focus target, so asking not to
    // be focusable has to say so to the click as well.
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
        )
        .then(
            if (focusable) Modifier.focusable(enabled = enabled, interactionSource = source)
            else Modifier
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
