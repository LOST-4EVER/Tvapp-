package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppColors

/**
 * A 1dp line of light along the inside of a card's top edge.
 *
 * Call it as the first child of the card's own `Box`, with [inset] set to the card's
 * corner radius:
 *
 * ```
 * Box(modifier = Modifier.background(colors.surface, cardShape)) {
 *     TopEdgeHighlight(inset = 20.dp)
 *     // …content
 * }
 * ```
 *
 * This is what replaced the vertical gradient the cards used to carry. On a
 * near-black page a flat card reads as a hole with a border around it, and a
 * full-height ramp from `surfaceVariant` to `surface` was the cheap fix — but it is
 * a per-card draw in a grid of several hundred, and it made the top of every card
 * look like the front face of something solid. One hairline separates the card from
 * the page for the price of one line.
 *
 * The line is *inset* by the corner radius rather than run edge to edge, because at
 * the two top corners a straight line and a rounded outline do not meet: a
 * full-width line pokes out past the curve and is the one place on the card where
 * the two are visibly fighting.
 *
 * It is a `BoxScope` extension, not a plain composable, so that `align` resolves
 * against the card's box. Returning it from a helper with its own `Box` would draw
 * the line at the top of *that* box instead, which is the top of nothing.
 */
@Composable
fun BoxScope.TopEdgeHighlight(inset: Dp) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.edgeHighlight)
    )
}
