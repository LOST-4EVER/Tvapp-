package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

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
 *   somewhere else. Two `focusable` modifiers on one node share a single focus target,
 *   so they do not create two D-pad stops, but they do mean two independent
 *   focus callbacks that can disagree. Pair this with [expressiveFocusRing], which
 *   supplies the focus behaviour, and there is only one owner of the state.
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
