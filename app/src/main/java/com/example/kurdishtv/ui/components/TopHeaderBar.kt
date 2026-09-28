package com.example.kurdishtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun TopHeaderBar(
    channelCount: Int,
    isLoading: Boolean = false,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    // While a refresh is in flight the logo gently breathes, so the header reads as
    // alive without needing a blocking spinner over the grid.
    val logoScale by animateFloatAsState(
        targetValue = if (isLoading) 1.06f else 1f,
        animationSpec = ExpressiveMotion.spatialMedium,
        label = "HeaderLogoScale"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // On narrow screens the title, the action row and the channel count cannot all
        // fit on one line, so the title is capped and the secondary line is dropped
        // rather than letting the text slide under the buttons.
        val isNarrow = maxWidth < 420.dp
        val titleMaxWidth = if (isNarrow) 132.dp else 220.dp
        val gutter = if (isNarrow) 12.dp else 20.dp
        val logoSize = if (isNarrow) 40.dp else 46.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // The brand tile is the one place a bold shape still reads correctly:
                // a rounded square, matching the logo tiles in the grid below it, so
                // the header and the content share a geometry.
                Surface(
                    shape = M3ExpressiveShapes.MediumCard,
                    color = colors.primary,
                    modifier = Modifier
                        .size(logoSize)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                        .bouncyClickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(
                            resId = KurdishTvIcons.Tv,
                            contentDescription = "Kurdish TV Live",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(if (isNarrow) 22.dp else 25.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (isNarrow) 8.dp else 12.dp))

                Column {
                    Text(
                        text = "Kurdish TV Live",
                        color = colors.textPrimary,
                        fontSize = if (isNarrow) 17.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = titleMaxWidth)
                    )
                    Text(
                        text = if (isNarrow) {
                            "$channelCount channels"
                        } else {
                            "$channelCount channels • تەلەفزیۆنی کوردی"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = titleMaxWidth)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconButton(
                    iconRes = KurdishTvIcons.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    style = AppIconButtonStyle.Tonal
                )
                AppIconButton(
                    iconRes = KurdishTvIcons.AddLink,
                    contentDescription = "Import IPTV playlist",
                    onClick = onOpenImport,
                    style = AppIconButtonStyle.Tonal
                )
                AppIconButton(
                    iconRes = KurdishTvIcons.Refresh,
                    contentDescription = "Reload channels",
                    onClick = onRefresh,
                    style = AppIconButtonStyle.Tonal
                )
                if (isLoading) {
                    BouncingLoader(size = 26.dp, dotCount = 5)
                }
            }
        }
    }
}
