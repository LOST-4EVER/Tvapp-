package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun SidePlayerPane(
    channel: Channel?,
    onFullscreenClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channel == null) return
    val colors = LocalAppColors.current

    var isPlaying by remember(channel.id) { mutableStateOf(true) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }

    Surface(
        color = colors.surface,
        shape = M3ExpressiveShapes.Clover,
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
            .border(1.dp, colors.border, M3ExpressiveShapes.Clover)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE PREVIEW",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.glass,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = { onFavoriteToggle(channel.id) }) {
                        SvgIcon(
                            resId = if (channel.isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) colors.liveRed else colors.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = M3ExpressiveShapes.LogoTile,
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(M3ExpressiveShapes.LogoTile)
                    .border(1.dp, colors.border, M3ExpressiveShapes.LogoTile)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoPlayerView(
                        streamUrl = channel.streamUrl,
                        isPlaying = isPlaying,
                        resizeMode = ResizeMode.FIT,
                        onPlaybackError = { errorMessage = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Stream unavailable — pick another channel",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = channel.name,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${channel.category} • ${channel.quality}",
                color = colors.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isPlaying = !isPlaying },
                    shape = M3ExpressiveShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceElevated,
                        contentColor = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    SvgIcon(
                        resId = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlaying) "Pause" else "Play", fontSize = 12.sp)
                }

                Button(
                    onClick = { onFullscreenClick(channel) },
                    shape = M3ExpressiveShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    SvgIcon(
                        resId = KurdishTvIcons.Fullscreen,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fullscreen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
