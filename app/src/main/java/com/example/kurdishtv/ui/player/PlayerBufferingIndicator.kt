package com.example.kurdishtv.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.components.LoadingIndicator
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * The panel shown while a stream is buffering.
 *
 * This is the one loading state a viewer sits and stares at, so it is the one that
 * most needs to be legible rather than interesting. It used to breathe: a
 * `rememberInfiniteTransition` at display rate driving a `graphicsLayer` scale between
 * 0.95 and 1.05, reversing every 800ms, on a surface already waiting on a network
 * round trip — and an entrance that opened the panel's corners from 22dp to 44dp,
 * driven by a spring that was retargeted by `pulseScale > 1f` and therefore never
 * converged.
 *
 * All of that is gone. What is left is a panel, a static mark, and two lines of text
 * saying what is happening. On a buffering screen the words are the useful part; the
 * movement was never information, it was only reassurance, and a static panel with a
 * clear label is more reassuring than one that twitches while nothing changes.
 */
@Composable
fun PlayerBufferingIndicator(
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val panelShape: Shape = M3ExpressiveShapes.Corners.largeCard.toShape()

    Surface(
        shape = panelShape,
        color = colors.surface.copy(alpha = 0.94f),
        modifier = modifier.border(1.dp, colors.border, panelShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LoadingIndicator(size = 22.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Connecting stream…",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live HLS buffer",
                    color = colors.textTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
