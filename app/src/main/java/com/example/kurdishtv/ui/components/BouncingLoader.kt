package com.example.kurdishtv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Resolves a [Shape] into a drawable [Path] at a given size. */
private fun Shape.toPath(size: Size, layoutDirection: LayoutDirection, density: Density): Path =
    when (val outline = createOutline(size, layoutDirection, density)) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }

/**
 * Material 3 Expressive loading indicator.
 *
 * Compose Material3 only ships the Expressive `LoadingIndicator` from 1.4.0, and
 * this project is on BOM 2024.09.00 (1.3.0), so the indicator is drawn directly:
 * dots travel a closed loop while morphing between expressive shapes, which is
 * what gives M3 its "stretch and settle" character instead of a mechanical spin.
 *
 * One infinite transition drives every dot, so animation cost does not grow with
 * the dot count. Under reduced motion the dots hold still and keep their opacity
 * ramp, so nothing jumps.
 */
@Composable
fun BouncingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    dotCount: Int = 7,
    color: Color = LocalAppColors.current.primary
) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "BouncingLoader")
    val shapes = remember { expressiveShapeSequence() }

    // 0→1 sweep around the loop. The keyframes hold at the end so each lap eases
    // out rather than restarting abruptly.
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                1f at 1150
                1f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BounceSweep"
    )

    Canvas(modifier = modifier.size(size)) {
        val dotDiameter = min(this.size.width, this.size.height) / 7f
        val orbit = min(this.size.width, this.size.height) / 2f - dotDiameter * 0.9f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        for (i in 0 until dotCount) {
            val phase = ((sweep - i / dotCount.toFloat()) % 1f + 1f) % 1f
            val angle = phase * 2f * PI.toFloat() - (PI / 2f).toFloat()
            val x = center.x + orbit * cos(angle)
            val y = center.y + orbit * sin(angle)

            // The lead dot is fully opaque; the rest trail off behind it.
            val prominence = 1f - i.toFloat() / dotCount
            val alpha = if (reduceMotion) {
                0.45f + 0.4f * prominence
            } else {
                0.22f + 0.78f * prominence
            }
            // Dots swell as they pass the front of the orbit.
            val scale = if (reduceMotion) {
                0.95f
            } else {
                0.7f + 0.45f * abs(cos(phase * PI.toFloat()))
            }

            val dotSize = dotDiameter * scale
            val shape = shapes[(i + (phase * shapes.size).toInt()) % shapes.size]

            drawPath(
                path = shape.toPath(Size(dotSize, dotSize), layoutDirection, this),
                color = color.copy(alpha = alpha)
            )
        }
    }
}

/**
 * A calm placeholder shaped like a channel card, shown while the list hydrates
 * from disk so the grid does not visibly pop from empty to full on cold start.
 */
@Composable
fun ChannelCardSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "SkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = if (reduceMotion) 0.18f else 0.10f,
        targetValue = if (reduceMotion) 0.18f else 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(ExpressiveMotion.DURATION_LONG, easing = ExpressiveMotion.standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )
    val block = colors.surfaceVariant.copy(alpha = alpha)

    Column(modifier = modifier.fillMaxWidth().padding(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(M3ExpressiveShapes.LogoTile)
                .background(block)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(13.dp)
                .clip(M3ExpressiveShapes.Pill)
                .background(block)
        )
        Spacer(modifier = Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .height(11.dp)
                .clip(M3ExpressiveShapes.Pill)
                .background(block)
        )
    }
}

/** Full-screen loading state shown while the very first channel list resolves. */
@Composable
fun BouncingLoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BouncingLoader(size = 52.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = message,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Inline variant for use inside a toolbar or on a small surface. */
@Composable
fun BouncingLoaderRow(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        BouncingLoader(size = 22.dp, dotCount = 5)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The shapes the dot cycles through; the sequence alternates soft and faceted. */
private fun expressiveShapeSequence(): List<Shape> = listOf(
    M3ExpressiveShapes.Circle,
    M3ExpressiveShapes.FourLeafClover,
    M3ExpressiveShapes.SixSidedCookie,
    M3ExpressiveShapes.SevenSidedCookie,
    M3ExpressiveShapes.Flower,
    M3ExpressiveShapes.NineSidedCookie,
    M3ExpressiveShapes.PuffyDiamond
)
