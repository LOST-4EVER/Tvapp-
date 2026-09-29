package com.example.kurdishtv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppColors

/**
 * The app's loading mark.
 *
 * ## Why this is three static dots
 *
 * This was `BouncingLoader`, and it was not a spinner. Material's own answer to "what
 * should a loading state look like" is not a spinner either: it is a single shape
 * that walks continuously through a seven-shape sequence, turning as it goes, so the
 * wait is filled with something worth watching. Compose Material3 only ships that from
 * 1.4.0 and this project is on BOM 2024.09.00, so it had been drawn by hand — a
 * `rememberInfiniteTransition` driving a morph fraction and a rotation through the
 * polygon library, running at display rate for the whole time the indicator was up,
 * in three places: the header's refresh spinner, the update card's checking row and
 * the player's buffering panel.
 *
 * All of that is gone. What replaces it has to survive one hard test: **it must not
 * look broken.** A frozen spinner is the classic failure here — a ring of dashes that
 * is not turning reads as a hung app, which is the exact state the viewer is already
 * worried about.
 *
 * An ellipsis passes that test because it was never supposed to move. "•••" means
 * working, means it has always meant working, and a static one still means it. It
 * also costs three circles instead of a polygon path rebuilt every frame, and it is
 * legible down to about 16dp, which the morph was not.
 *
 * Where the mark is not enough on its own, the caller supplies the words. The player's
 * buffering panel says "Connecting stream…"; the update card names what it is checking.
 * The dot is punctuation on that sentence, not the sentence.
 *
 * @param size the box to draw in. The dots are sized as a fraction of it, so the same
 *   call works at 20dp in a card and 26dp in the header.
 * @param color the active dot. The other two are this colour at [dimAlpha], so the
 *   mark still reads as one object rather than three.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = LocalAppColors.current.primary,
    dimAlpha: Float = 0.32f
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension * 0.17f
        val spacing = this.size.minDimension * 0.42f
        val centreY = this.size.minDimension / 2f
        val centreX = this.size.minDimension / 2f

        for (i in -1..1) {
            drawCircle(
                color = if (i == 0) color else color.copy(alpha = dimAlpha),
                radius = radius,
                center = Offset(centreX + i * spacing, centreY)
            )
        }
    }
}
