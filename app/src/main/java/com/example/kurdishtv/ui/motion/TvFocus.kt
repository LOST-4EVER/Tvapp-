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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive focus treatment for D-pad and TV remotes.
 *
 * An Android TV app is driven almost entirely by a D-pad, and the app previously
 * had no focus handling at all. `bouncyClickable` deliberately sets
 * `indication = null` for its springy press feedback, so on a TV box a focused
 * element showed nothing at all and the user had no idea where they were.
 *
 * This adds the missing half of Expressive interaction: a visible focus ring
 * that springs the element slightly as it gains focus. Touch input is entirely
 * unaffected, because the ring only draws when the element actually has focus.
 *
 * @param shape the focus ring follows this shape, so it matches the surface it
 *   outlines. Pass the same shape the surface was clipped with.
 * @param onFocusChanged invoked whenever focus is gained or lost.
 */
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    focusScale: Float = 1.04f,
    ringWidth: Dp = 3.dp,
    ringColor: Color,
    shape: Shape,
    onFocusChanged: (isFocused: Boolean) -> Unit = {}
): Modifier = composed {
    if (!enabled) return@composed this

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
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(BorderStroke(ringWidth, ringColor), shape)
}

/** A [FocusRequester] for programmatically focusing an element, e.g. the first card. */
@Composable
fun rememberTvFocusRequester(): FocusRequester = remember { FocusRequester() }
