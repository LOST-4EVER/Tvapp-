package com.example.kurdishtv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun TopHeaderBar(
    channelCount: Int,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = M3ExpressiveShapes.Sunny,
                color = colors.primary,
                modifier = Modifier
                    .size(46.dp)
                    .bouncyClickable { onRefresh() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.Tv,
                        contentDescription = "Kurdish TV Live",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Kurdish TV Live",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$channelCount channels • تەلەفزیۆنی کوردی",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                iconRes = KurdishTvIcons.Settings,
                description = "Settings",
                onClick = onOpenSettings
            )
            HeaderIconButton(
                iconRes = KurdishTvIcons.AddLink,
                description = "Import IPTV playlist",
                onClick = onOpenImport
            )
            HeaderIconButton(
                iconRes = KurdishTvIcons.Refresh,
                description = "Reload channels",
                onClick = onRefresh
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Surface(
        shape = M3ExpressiveShapes.Burst,
        color = colors.surfaceElevated,
        modifier = Modifier
            .size(42.dp)
            .border(1.dp, colors.border, M3ExpressiveShapes.Burst)
            .bouncyClickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(
                resId = iconRes,
                contentDescription = description,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
