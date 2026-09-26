package com.example.kurdishtv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.LiveBadge
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.KurdishRed
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    onTapOverlay: () -> Unit,
    channel: Channel,
    isPlaying: Boolean,
    sleepTimerRemainingText: String?,
    onOpenSleepTimer: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onBackClick: () -> Unit,
    resizeMode: ResizeMode,
    onResizeModeToggle: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTapOverlay
            )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GlassOverlay,
                            modifier = Modifier
                                .size(42.dp)
                                .bouncyClickable(onClick = onBackClick)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(
                                    resId = KurdishTvIcons.ChevronLeft,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = channel.name,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${channel.category} • ${channel.quality}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Mute / Unmute Button
                        Surface(
                            shape = CircleShape,
                            color = if (isMuted) KurdishRed.copy(alpha = 0.3f) else GlassOverlay,
                            modifier = Modifier
                                .size(42.dp)
                                .bouncyClickable(onClick = onToggleMute)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(
                                    resId = if (isMuted) KurdishTvIcons.VolumeOff else KurdishTvIcons.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = if (isMuted) KurdishRed else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Sleep timer button
                        Surface(
                            shape = CircleShape,
                            color = if (!sleepTimerRemainingText.isNullOrEmpty()) KurdishSunGold.copy(alpha = 0.25f) else GlassOverlay,
                            modifier = Modifier
                                .size(42.dp)
                                .bouncyClickable(onClick = onOpenSleepTimer)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(
                                    resId = KurdishTvIcons.Bedtime,
                                    contentDescription = "Sleep timer",
                                    tint = if (!sleepTimerRemainingText.isNullOrEmpty()) KurdishSunGold else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Favorite toggle button
                        Surface(
                            shape = CircleShape,
                            color = GlassOverlay,
                            modifier = Modifier
                                .size(42.dp)
                                .bouncyClickable(onClick = onFavoriteToggle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(
                                    resId = if (channel.isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                                    contentDescription = "Favorite",
                                    tint = if (channel.isFavorite) KurdishRed else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause & Channel Skip Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GlassOverlay,
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, DarkCardBorder, CircleShape)
                            .bouncyClickable(scaleDown = 0.86f, onClick = onPreviousChannel)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SvgIcon(
                                resId = KurdishTvIcons.ChevronLeft,
                                contentDescription = "Previous channel",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = KurdishSunGold,
                        modifier = Modifier
                            .size(72.dp)
                            .bouncyClickable(scaleDown = 0.88f, onClick = onPlayPauseToggle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SvgIcon(
                                resId = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = GlassOverlay,
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, DarkCardBorder, CircleShape)
                            .bouncyClickable(scaleDown = 0.86f, onClick = onNextChannel)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SvgIcon(
                                resId = KurdishTvIcons.ChevronRight,
                                contentDescription = "Next channel",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveBadge()
                        if (!sleepTimerRemainingText.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = M3ExpressiveShapes.BadgePill,
                                color = KurdishSunGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Timer: $sleepTimerRemainingText",
                                    color = KurdishSunGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = M3ExpressiveShapes.Pill,
                            color = GlassOverlay,
                            modifier = Modifier.bouncyClickable(onClick = onResizeModeToggle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SvgIcon(
                                    resId = KurdishTvIcons.AspectRatio,
                                    contentDescription = null,
                                    tint = KurdishSunGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = resizeMode.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = GlassOverlay,
                            modifier = Modifier
                                .size(38.dp)
                                .bouncyClickable(onClick = onFullscreenToggle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(
                                    resId = if (isFullscreen) KurdishTvIcons.FullscreenExit else KurdishTvIcons.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
