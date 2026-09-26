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
import com.example.ui.theme.LocalAppColors

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
    val colors = LocalAppColors.current
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
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // ── Top action bar ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = colors.glass,
                            modifier = Modifier
                                .size(44.dp)
                                .bouncyClickable { onBackClick() }
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
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${channel.category} • ${channel.quality}",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverlayIconButton(
                            iconRes = if (isMuted) KurdishTvIcons.VolumeOff else KurdishTvIcons.VolumeUp,
                            description = if (isMuted) "Unmute" else "Mute",
                            active = isMuted,
                            onClick = onToggleMute
                        )
                        OverlayIconButton(
                            iconRes = KurdishTvIcons.Bedtime,
                            description = "Sleep timer",
                            active = !sleepTimerRemainingText.isNullOrEmpty(),
                            onClick = onOpenSleepTimer
                        )
                        OverlayIconButton(
                            iconRes = if (channel.isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                            description = "Favorite",
                            active = channel.isFavorite,
                            onClick = onFavoriteToggle
                        )
                    }
                }

                // ── Center transport controls ─────────────────────────────────
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = M3ExpressiveShapes.Burst,
                        color = colors.glass,
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, colors.border, M3ExpressiveShapes.Burst)
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
                        color = colors.primary,
                        modifier = Modifier
                            .size(74.dp)
                            .bouncyClickable(scaleDown = 0.88f, onClick = onPlayPauseToggle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SvgIcon(
                                resId = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Surface(
                        shape = M3ExpressiveShapes.Burst,
                        color = colors.glass,
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, colors.border, M3ExpressiveShapes.Burst)
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

                // ── Bottom action bar ─────────────────────────────────────────
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
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = "Timer $sleepTimerRemainingText",
                                    color = colors.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                            color = colors.glass,
                            modifier = Modifier.bouncyClickable(onClick = onResizeModeToggle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SvgIcon(
                                    resId = KurdishTvIcons.AspectRatio,
                                    contentDescription = null,
                                    tint = colors.primary,
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
                            color = colors.glass,
                            modifier = Modifier
                                .size(40.dp)
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

@Composable
private fun OverlayIconButton(
    iconRes: Int,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Surface(
        shape = CircleShape,
        color = if (active) colors.primary.copy(alpha = 0.28f) else colors.glass,
        modifier = Modifier
            .size(44.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(
                resId = iconRes,
                contentDescription = description,
                tint = if (active) colors.primary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
