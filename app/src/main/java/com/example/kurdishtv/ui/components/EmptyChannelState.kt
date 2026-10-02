package com.example.kurdishtv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun EmptyChannelState(
    searchQuery: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val buttonSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = M3ExpressiveShapes.LargeCard,
            color = colors.surfaceVariant
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No channels found",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isNotBlank())
                        "Nothing matches \"$searchQuery\". Try another name or clear your search."
                    else
                        "This category is empty right now. Try a different one.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val pillShape = M3ExpressiveShapes.Pill
        Surface(
            shape = pillShape,
            color = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .tvClickable(
                    interactionSource = buttonSource,
                    pressedFill = colors.onPrimary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                    pressedShape = pillShape,
                    onClick = onReset
                )
                .expressiveFocusRing(
                    ringColor = colors.primary,
                    interactionSource = buttonSource,
                    scrim = colors.focusScrim,
                    restShape = M3ExpressivePolygons.Square,
                    ringShape = ShapeMorph.focusRing
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.Refresh,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show all channels",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
