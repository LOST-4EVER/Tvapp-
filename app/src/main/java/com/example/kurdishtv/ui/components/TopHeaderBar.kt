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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopHeaderBar(
    channelCount: Int,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = KurdishSunGold,
                modifier = Modifier
                    .size(44.dp)
                    .bouncyClickable(onClick = onRefresh)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.Tv,
                        contentDescription = "Kurdish TV Live",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Kurdish TV Live",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$channelCount Channels • تەلەفزیۆنی کوردی",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, DarkCardBorder, CircleShape)
                    .bouncyClickable(onClick = onRefresh)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.Refresh,
                        contentDescription = "Reload channels",
                        tint = KurdishSunGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, DarkCardBorder, CircleShape)
                    .bouncyClickable(onClick = onOpenImport)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.AddLink,
                        contentDescription = "Import IPTV Playlist",
                        tint = KurdishSunGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
