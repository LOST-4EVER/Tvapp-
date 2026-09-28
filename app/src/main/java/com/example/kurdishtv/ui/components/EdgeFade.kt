package com.example.kurdishtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppColors

/**
 * Fades the leading and trailing edges of a horizontally scrolling row into the
 * page background.
 *
 * A row that runs past the screen edge is *meant* to read as "there is more", but
 * a hard clip through the middle of a word reads as a layout bug — the recently
 * watched row cut "KurdSat News" in half with nothing to suggest it was
 * scrollable. Fading the last few dp turns the same clip into an affordance.
 *
 * The fade only appears while content is actually under the edge, so a short row
 * that fits is left completely untouched.
 */
fun Modifier.horizontalEdgeFade(
    background: Color,
    width: Dp = 28.dp
): Modifier = drawWithContent {
    drawContent()

    val fadePx = width.toPx()
    val bounds = size
    // A row narrower than the fade has nothing to fade into it; painting it would
    // just dim the right-hand end of the only item.
    if (bounds.width <= fadePx * 2f) return@drawWithContent

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(background, Color.Transparent),
            startX = 0f,
            endX = fadePx
        ),
        topLeft = Offset.Zero,
        size = Size(fadePx, bounds.height)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, background),
            startX = bounds.width - fadePx,
            endX = bounds.width
        ),
        topLeft = Offset(bounds.width - fadePx, 0f),
        size = Size(fadePx, bounds.height)
    )
}

/** [horizontalEdgeFade] against the app's own background colour. */
@Composable
fun Modifier.edgeFade(width: Dp = 28.dp): Modifier =
    this.horizontalEdgeFade(background = LocalAppColors.current.background, width = width)
