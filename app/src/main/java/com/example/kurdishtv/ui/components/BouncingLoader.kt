package com.example.kurdishtv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.theme.ExpressiveMorph
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.fittedPath
import com.example.ui.theme.LocalAppColors

/**
 * Material 3 Expressive loading indicator.
 *
 * Material's own answer to "what should a loading state look like" is not a spinner.
 * It is a single shape that walks continuously through a seven-shape sequence, turning
 * as it goes, so the wait is filled with something worth watching rather than with a
 * rotating line. Google calls it a *looping shape morph sequence composed of seven
 * unique Material 3 shapes*, and the shapes are the ones in
 * [M3ExpressivePolygons.LoadingSequence].
 *
 * Compose Material3 only ships that component from 1.4.0 and this project is on BOM
 * 2024.09.00, so it is drawn directly here. What made it possible to reproduce
 * faithfully rather than approximate is that a morph is just an outline: the indicator
 * is one polygon at any instant, and the animation is the fraction between two of them.
 *
 * This is also the one place in the app where a fully expressive silhouette is
 * unambiguously correct — the shape carries no glyph, no label and no touch target,
 * which is exactly the condition under which the lobed family is allowed.
 *
 * Under reduced motion the walk stops on a single shape and the turn stops with it.
 * Nothing jumps, and the indicator is still unmistakably a loading state.
 */
@Composable
fun BouncingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = LocalAppColors.current.primary
) {
    val reduceMotion = LocalReduceMotion.current
    val sequence = M3ExpressivePolygons.LoadingSequence

    // The morphs are built once, not per frame. Lining two vertex rings up against
    // each other is the expensive half, and the pairs never change.
    val morphs = remember(sequence) {
        sequence.indices.map { i ->
            ExpressiveMorph.between(sequence[i], sequence[(i + 1) % sequence.size])
        }
    }

    val transition = rememberInfiniteTransition(label = "M3LoadingIndicator")
    // The two animated values are held as State and read *inside* the draw lambda
    // rather than being delegated at the top level. Reading them during composition
    // would recompose this composable on every frame of a seven-second loop; reading
    // them during draw only re-runs the draw pass, which is what an indicator needs.
    val walk = transition.animateFloat(
        initialValue = 0f,
        targetValue = sequence.size.toFloat(),
        animationSpec = infiniteRepeatable(
            // Linear, because a morph that also eases looks like it is hesitating
            // between shapes rather than moving continuously through them.
            animation = tween(
                durationMillis = sequence.size * MORPH_MILLIS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoadingWalk"
    )
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = sequence.size * MORPH_MILLIS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoadingRotation"
    )

    val path = remember { Path() }

    Canvas(modifier = modifier.size(size)) {
        val n = sequence.size
        val position = if (reduceMotion) 0f else walk.value
        val index = position.toInt().coerceIn(0, n - 1)
        val fraction = (position - index).coerceIn(0f, 1f)

        val polygon = if (reduceMotion) {
            sequence[0]
        } else {
            morphs[index].polygonAt(fraction, rotation.value)
        }
        polygon.fittedPath(this.size, out = path)
        drawPath(path = path, color = color)
    }
}

/** How long the indicator spends on each of its seven shapes. */
private const val MORPH_MILLIS = 900

