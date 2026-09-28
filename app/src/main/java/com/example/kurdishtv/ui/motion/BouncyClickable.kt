package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    interactionSource: MutableInteractionSource? = null,
    focusable: Boolean = true,
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
        isFocused && enabled -> 1.04f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = ExpressiveMotion.pressSpring,
        label = "BouncyScaleAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .then(
            if (focusable) Modifier.focusable(enabled = enabled, interactionSource = source)
            else Modifier
        )
}
