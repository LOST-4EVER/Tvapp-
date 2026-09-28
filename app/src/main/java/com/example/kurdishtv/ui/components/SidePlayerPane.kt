package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * The preview pane beside the channel grid on wide screens.
 *
 * This is a *preview*: it is never the thing the user is watching, so it starts
 * paused. Autoplaying here meant every visit to the browse screen started a live
 * stream with sound, which is both surprising and the single largest battery
 * drain in the app.
 */
@Composable
fun SidePlayerPane(
    channel: Channel?,
    onFullscreenClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channel == null) return
    val colors = LocalAppColors.current

    var isPlaying by remember(channel.id) { mutableStateOf(false) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }
    // Bumped to force the player to re-prepare the same URL after a failure.
    // Toggling play/pause alone does not make ExoPlayer retry a stream that has
    // already moved to STATE_IDLE on error.
    var retryToken by remember(channel.id) { mutableIntStateOf(0) }

    Surface(
        color = colors.surface,
        shape = M3ExpressiveShapes.LargeCard,
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
            .border(1.dp, colors.border, M3ExpressiveShapes.LargeCard)
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

            // The pane is full height on a tablet but a short strip in a split
            // window. Let the video take whatever space is left rather than
            // fixing its height, so the controls below are never pushed off the
            // bottom of a short pane.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                Card16x9(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    VideoPlayerView(
                        streamUrl = channel.streamUrl,
                        isPlaying = isPlaying,
                        resizeMode = ResizeMode.FIT,
                        onPlaybackError = { errorMessage = it },
                        // No transport overlay is drawn on this pane, so the
                        // buffering notice is the only way to show that something
                        // is happening. Left at its default it was suppressed.
                        areControlsVisible = false,
                        reloadKey = retryToken,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.86f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Stream unavailable",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage!!.take(90),
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                // Clears the error as well as re-preparing:
                                // previously the overlay was drawn from a state
                                // that nothing ever reset, so it stayed on screen
                                // over a stream that had started working.
                                onClick = {
                                    errorMessage = null
                                    isPlaying = true
                                    retryToken++
                                },
                                shape = M3ExpressiveShapes.Pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = channel.name,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${channel.category} • ${channel.quality}",
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        isPlaying = !isPlaying
                    },
                    shape = M3ExpressiveShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceElevated,
                        contentColor = colors.textPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    SvgIcon(
                        resId = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                        contentDescription = null,
                        tint = colors.textPrimary,
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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

@Composable
private fun Card16x9(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    androidx.compose.material3.Surface(
        shape = M3ExpressiveShapes.LogoTile,
        color = Color.Black,
        modifier = modifier
            .clip(M3ExpressiveShapes.LogoTile)
            .border(1.dp, colors.border, M3ExpressiveShapes.LogoTile),
        content = content
    )
}
