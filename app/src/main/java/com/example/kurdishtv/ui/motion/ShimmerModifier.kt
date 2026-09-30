package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import com.example.ui.theme.LocalAppColors

/**
 * High-performance, battery-friendly skeleton shimmer modifier.
 *
 * ## CPU, GPU & Battery Optimization:
 * 1. Defers animated gradient calculations and state reads exclusively to the **Draw Phase**
 *    via [drawWithCache], completely eliminating layout measurements and UI recompositions per frame.
 * 2. Employs a single 1300ms smooth linear cycle.
 * 3. Adapts colors to the active AMOLED / dark theme so OLED pixels remain predominantly off,
 *    substantially lowering display power draw and GPU fill rate on TVs and mobile devices.
 */
fun Modifier.shimmerEffect(
    shape: Shape? = null,
    baseColor: Color? = null,
    highlightColor: Color? = null
): Modifier = composed {
    val colors = LocalAppColors.current
    val base = baseColor ?: colors.surfaceVariant
    val highlight = highlightColor ?: colors.surfaceHigh

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    this.drawWithCache {
        val width = size.width
        val height = size.height
        val outline = shape?.createOutline(size, layoutDirection, this)

        onDrawBehind {
            val startX = translateAnim * width
            val brush = Brush.linearGradient(
                colors = listOf(
                    base,
                    highlight,
                    base
                ),
                start = Offset(startX - width, 0f),
                end = Offset(startX, height)
            )

            if (outline != null) {
                drawOutline(outline = outline, brush = brush)
            } else {
                drawRect(brush = brush)
            }
        }
    }
}
