package com.example.kurdishtv.ui.components

import androidx.compose.foundation.lazy.LazyListState
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
 * Only the edge that actually has content beyond it is painted. Fading both ends
 * unconditionally is wrong in a way that is easy to miss: the fade overlaps the
 * row's own content padding, so it dimmed the first and last chip even when the
 * row fitted on screen and had nothing to scroll to.
 *
 * The scroll state is read *inside* the draw lambda rather than captured into
 * remembered derived state. `LazyListState.canScrollForward` and
 * `canScrollBackward` are themselves derived state, so reading them from the draw
 * phase registers this node as an observer and re-runs the draw when they flip —
 * the same mechanism every other draw-time read uses, and it keeps this a plain
 * Modifier extension rather than a composable one.
 */
fun Modifier.horizontalEdgeFade(
    listState: LazyListState,
    background: Color,
    width: Dp = 28.dp
): Modifier = this.drawWithContent {
    drawContent()

    val fadePx = width.toPx()
    val bounds = size
    // A row narrower than the fade has nothing to fade into it; painting it would
    // just dim the ends of the only item.
    if (bounds.width <= fadePx * 2f) return@drawWithContent

    if (listState.canScrollBackward) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(background, Color.Transparent),
                startX = 0f,
                endX = fadePx
            ),
            topLeft = Offset.Zero,
            size = Size(fadePx, bounds.height)
        )
    }
    if (listState.canScrollForward) {
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
}

/**
 * [horizontalEdgeFade] against the app's own background colour.
 *
 * The row's [listState] is required rather than optional on purpose: without it
 * there is no way to know which end is off screen, and guessing paints a fade
 * over content the user can already see in full.
 */
@Composable
fun Modifier.edgeFade(
    listState: LazyListState,
    width: Dp = 28.dp
): Modifier = horizontalEdgeFade(
    listState = listState,
    background = LocalAppColors.current.background,
    width = width
)
