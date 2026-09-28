package com.example.kurdishtv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import com.example.kurdishtv.ui.components.AppIconButton
import com.example.kurdishtv.ui.components.AppIconButtonStyle
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
    colorFilter: VideoColorFilter = VideoColorFilter.None,
    onCycleColorFilter: () -> Unit = {},
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
                // The scrim is a wrapper with extra bottom padding rather than a
                // background on the row itself. A gradient drawn across the row's
                // own bounds has to reach transparent exactly at the row's edge,
                // which leaves a hard line across the video; giving the wrapper room
                // below the row lets the falloff finish in empty space.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.92f),
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 40.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIconButton(
                                iconRes = KurdishTvIcons.ChevronLeft,
                                contentDescription = "Back",
                                onClick = onBackClick,
                                style = AppIconButtonStyle.Glass,
                                size = 44.dp,
                                iconSize = 24.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = channel.name,
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${channel.category} • ${channel.quality}",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppIconButton(
                                iconRes = if (isMuted) KurdishTvIcons.VolumeOff else KurdishTvIcons.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                onClick = onToggleMute,
                                style = AppIconButtonStyle.Glass,
                                active = isMuted
                            )
                            AppIconButton(
                                iconRes = KurdishTvIcons.Bedtime,
                                contentDescription = "Sleep timer",
                                onClick = onOpenSleepTimer,
                                style = AppIconButtonStyle.Glass,
                                active = !sleepTimerRemainingText.isNullOrEmpty()
                            )
                            AppIconButton(
                                iconRes = if (channel.isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                                contentDescription = "Favorite",
                                onClick = onFavoriteToggle,
                                style = AppIconButtonStyle.Glass,
                                active = channel.isFavorite
                            )
                        }
                    }
                }

                // ── Center transport controls ─────────────────────────────────
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconButton(
                        iconRes = KurdishTvIcons.ChevronLeft,
                        contentDescription = "Previous channel",
                        onClick = onPreviousChannel,
                        style = AppIconButtonStyle.Glass,
                        size = 56.dp,
                        iconSize = 28.dp,
                        scaleDown = 0.86f
                    )

                    AppIconButton(
                        iconRes = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = onPlayPauseToggle,
                        style = AppIconButtonStyle.Filled,
                        size = 74.dp,
                        iconSize = 36.dp,
                        scaleDown = 0.88f
                    )

                    AppIconButton(
                        iconRes = KurdishTvIcons.ChevronRight,
                        contentDescription = "Next channel",
                        onClick = onNextChannel,
                        style = AppIconButtonStyle.Glass,
                        size = 56.dp,
                        iconSize = 28.dp,
                        scaleDown = 0.86f
                    )
                }

                // ── Bottom action bar ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            // A glass pill reads correctly over video; the label is
                            // white rather than the accent, which would vanish
                            // against a bright frame.
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
                                        tint = Color.White,
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

                            // Colour correction. Cycles through the presets and is
                            // highlighted while one is active, so the current
                            // treatment is visible without opening a menu.
                            Surface(
                                shape = M3ExpressiveShapes.Pill,
                                color = if (colorFilter.isActive) {
                                    colors.primary
                                } else {
                                    colors.glass
                                },
                                modifier = Modifier.bouncyClickable(onClick = onCycleColorFilter)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SvgIcon(
                                        resId = KurdishTvIcons.Palette,
                                        contentDescription = "Video colour filter",
                                        tint = if (colorFilter.isActive) colors.onPrimary else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (colorFilter.isActive) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = colorFilter.label,
                                            color = colors.onPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            AppIconButton(
                                iconRes = if (isFullscreen) KurdishTvIcons.FullscreenExit else KurdishTvIcons.Fullscreen,
                                contentDescription = "Fullscreen",
                                onClick = onFullscreenToggle,
                                style = AppIconButtonStyle.Glass,
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
