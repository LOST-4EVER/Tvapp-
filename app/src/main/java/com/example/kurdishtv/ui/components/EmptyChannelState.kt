package com.example.kurdishtv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun EmptyChannelState(
    searchQuery: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val reduceMotion = LocalReduceMotion.current

    // Springs in rather than appearing flat, which makes an empty result read as a
    // deliberate state instead of a failed render.
    val entrance by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "EmptyStateEntrance"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                scaleX = entrance
                scaleY = entrance
                alpha = entrance.coerceIn(0f, 1f)
            },
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

        Button(
            onClick = onReset,
            shape = M3ExpressiveShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            SvgIcon(
                resId = KurdishTvIcons.Refresh,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show all channels", fontWeight = FontWeight.Bold)
        }
    }
}
