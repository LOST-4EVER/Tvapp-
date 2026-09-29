package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.kurdishtv.viewmodel.ChannelJump
import com.example.ui.theme.LocalAppColors

/**
 * What the remote's number pad is doing, shown while the viewer types.
 *
 * The digits are drawn very large, and that is the point of the surface. On a
 * television read from across a room a 13dp readout is not a readout, and the only
 * two questions a viewer has after pressing a number — *did it hear me, and where is
 * it going?* — have to be answerable without focusing anything.
 *
 * The channel name underneath answers the second half. Resolving on every digit
 * rather than only when the number is complete is what makes this worth showing: `1`
 * and `12` are both valid prefixes of different channels, and an overlay that only
 * reported a failure at the end left no way to tell "still typing" from "no such
 * channel". A number that resolves to nothing therefore says so in words, rather
 * than leaving the viewer staring at digits that do nothing.
 */
@Composable
fun ChannelNumberOverlay(
    jump: ChannelJump,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val target = jump.target
    val shape = M3ExpressiveShapes.LargeCard

    // The panel used to pop from 0.88 to 1 on a bouncy spring, re-armed on every
    // digit so `1` then `2` read as two confirmations. It is drawn at full size for
    // every digit now.
    //
    // The confirmation it gave is still here, just carried by the digits themselves:
    // they are redrawn at a very large size and the resolved channel name underneath
    // changes on every keystroke, so the panel visibly answers each press. What is
    // gone is a 240dp-wide surface crossing the screen on a spring every time.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(240.dp)
                .clip(shape)
                .background(colors.glass)
                .border(1.dp, colors.primary, shape)
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Text(
                text = jump.digits,
                color = colors.textPrimary,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = target?.name ?: "No channel ${jump.digits}",
                color = if (target != null) colors.primary else colors.textTertiary,
                fontSize = 14.sp,
                fontWeight = if (target != null) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}
