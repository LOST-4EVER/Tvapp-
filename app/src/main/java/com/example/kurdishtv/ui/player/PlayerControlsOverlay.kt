package com.example.kurdishtv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.AppIconButton
import com.example.kurdishtv.ui.components.AppIconButtonStyle
import com.example.kurdishtv.ui.components.ChannelLogo
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.LabelPillButton
import com.example.kurdishtv.ui.components.LiveBadge
import com.example.kurdishtv.ui.motion.tapOnly
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * How the channel's own metadata is drawn in the player header.
 *
 * The header used to render `"Sports • HLS / 720p"` as one grey line under the name.
 * A bullet-separated string is the least legible arrangement available: the two halves
 * are different *kinds* of fact — what the channel is, and what the stream is — and
 * neither is more important than the other, so the separator has to carry that
 * distinction by itself. As separate pills each is named by its own surface, the
 * category can be the one thing in the header with any colour in it, and the quality
 * is legible on its own instead of trailing a separator.
 */
@Composable
private fun PlayerMetaPill(
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    val shape = M3ExpressiveShapes.BadgePill
    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier.then(
            if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier
        )
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            // Only ever reached when the pill has been squeezed by a narrow window
            // (see the `weight` at the call site). A pill that ellipsizes is still a
            // legible badge; one that runs off the edge of the screen is not.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * The transport bar that floats over the video.
 *
 * Three changes from the arrangement it replaces, all of them about the picture rather
 * than about the controls:
 *
 *  1. **The bars are laid out against the video's frame, not the window.** The player
 *     letterboxes inside a full-bleed black container, so on a phone held upright the
 *     window is mostly black bars with a 16:9 band across the middle. Pinning the
 *     header to the top of the *window* and the controls to the bottom of it stranded
 *     both in the letterbox, a long way from the thing they control. See
 *     [playerFrame].
 *
 *  2. **The dimming is two gradients over the picture instead of a flat wash over the
 *     screen.** A 45% black scrim on the whole surface cost the viewer a fifth of the
 *     picture's brightness everywhere — including across the middle, where the
 *     transport sits — and bought nothing at all over the black bars, which it could
 *     not darken further. A gradient that is opaque where the text is and gone before
 *     it reaches the middle leaves the part of the frame the viewer is watching
 *     untouched.
 *
 *  3. **The transport is one cluster rather than three floating discs.** Three
 *     separate circles over a moving image read as three unrelated buttons; a single
 *     rounded surface groups them as the thing they are — one control set — and gives
 *     the D-pad somewhere obvious to start from.
 *
 * The scrim is still a `tapOnly` pointer target and not a `clickable`, and that is not
 * negotiable: as a clickable it is the largest and first focus target on the player,
 * and it sits *above* the controls, so the remote's first press goes to an invisible
 * surface instead of to Back or Play/Pause. See the note on [tapOnly].
 */
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
    /**
     * The decoded picture's aspect ratio, reported by the player. Defaults to 16:9,
     * which is what the overlay assumed before it had ever been told otherwise.
     */
    videoAspect: Float = DEFAULT_VIDEO_ASPECT,
    /** Whether the viewer has channel logos switched on, as everywhere else. */
    showLogos: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .tapOnly(onTap = onTapOverlay)
    ) {
        val density = LocalDensity.current
        val container = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val frame = playerFrame(container, videoAspect, resizeMode)
        val frameLeft = with(density) { frame.left.toDp() }
        val frameTop = with(density) { frame.top.toDp() }
        val frameWidth = with(density) { frame.width.toDp() }
        val frameHeight = with(density) { frame.height.toDp() }

        // ── Adapting the control set to the height of the picture ──────────
        //
        // This is the half of the frame-anchoring that is easy to get wrong. A 16:9
        // stream fitted into a phone held upright is a band about 230dp tall in a
        // window several times that, and the control set laid out at television sizes
        // does not fit inside it: a 96dp header band, a 104dp transport cluster and
        // an 88dp bottom band is 288dp, so the transport sat on top of the header and
        // the bottom bar sat on top of the transport. Overlapping controls on a
        // television are not merely untidy - there is no pointer to disambiguate them
        // and the viewer cannot tell which one the D-pad is on.
        //
        // So the set scales with the picture. `1f` is the roomy case the sizes below
        // were drawn for, and the floor keeps the controls at a size that is still
        // pressable and still readable across a room when the picture is small.
        // `.value`, because `Dp / Float` is itself a `Dp` and this has to be a plain
        // ratio to be coerced against one.
        val roomy = (frameHeight.value / 520f).coerceIn(0.6f, 1f)

        // Bands are a share of the picture, so they track the frame rather than the
        // window, with bounds that scale alongside. The upper bound is what stops a
        // tall frame from spending a third of its height on a gradient.
        val topBand = (frameHeight * 0.36f).coerceIn(72.dp * roomy, 152.dp * roomy)
        val bottomBand = (frameHeight * 0.26f).coerceIn(64.dp * roomy, 130.dp * roomy)

        val playButtonSize = (76f * roomy).dp.coerceAtLeast(56.dp)
        val stepButtonSize = (56f * roomy).dp.coerceAtLeast(44.dp)
        val clusterPadding = (18f * roomy).dp.coerceAtLeast(10.dp)
        val headerTopPadding = (20f * roomy).dp.coerceAtLeast(12.dp)
        // Coerced as a `Float` and only then converted. `Sp` is declared
        // `Comparable<TextUnit>` rather than `Comparable<Sp>`, so it does not satisfy
        // the `T : Comparable<T>` bound that `coerceAtLeast` carries and cannot be
        // coerced directly - which is a compile error, not a subtle behaviour one.
        val nameSize = (19f * roomy).coerceAtLeast(15f).sp

        // On a narrow window the four bottom controls — three labelled pills and the
        // fullscreen button — do not fit beside a LIVE badge without colliding, and a
        // row that overflows does not wrap, it clips. The pills degrade to their
        // icon-only form, which [LabelPillButton] already supports and which keeps the
        // hit targets the same size. The state each pill reports survives the loss of
        // its label: an active colour filter still fills with the accent, and the
        // aspect mode still cycles in the same order.
        val labelled = maxWidth >= 520.dp

        // Shown or not, with nothing in between, and for the same reason as before:
        // the auto-hide timer is what the viewer is actually waiting for, and a
        // half-faded scrim leaves the video at an unreadable brightness.
        if (isVisible) {
            Box(
                modifier = Modifier
                    // Offset rather than `align`: the frame is centred by definition,
                    // so its left/top are the exact distance from the window's own
                    // top-left corner, and stating it that way cannot be thrown off by
                    // how the parent's alignment happens to resolve.
                    .offset(x = frameLeft, y = frameTop)
                    .size(width = frameWidth, height = frameHeight)
            ) {
                // ── Top scrim ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(topBand)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.88f),
                                    Color.Black.copy(alpha = 0.52f),
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // ── Channel header ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = headerTopPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconButton(
                        iconRes = KurdishTvIcons.ChevronLeft,
                        contentDescription = "Back",
                        onClick = onBackClick,
                        style = AppIconButtonStyle.Glass,
                        size = 44.dp,
                        iconSize = 24.dp
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    // The logo is what a viewer recognises a channel by, and this is
                    // the one screen where there is room to say so at a glance. It
                    // falls back to a monogram when the artwork is missing, which is
                    // the same fallback the grid uses, and it honours the viewer's
                    // "show logos" setting rather than ignoring it here.
                    if (showLogos) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(M3ExpressiveShapes.MediumCard)
                        ) {
                            ChannelLogo(
                                channelName = channel.name,
                                logoUrl = channel.logoUrl,
                                showLogos = showLogos,
                                contentPadding = 5.dp,
                                size = 44.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    // Weighted, so the title takes the slack rather than its intrinsic
                    // width. Unweighted it was the row's widest fixed child, and on a
                    // narrow window the controls on the right pushed the whole bar past
                    // the screen edge and took the channel name with it.
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = nameSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // `fill = false` so each pill is drawn at its natural width
                            // when there is room and is only *capped* at the space
                            // left when there is not. A fixed-width pill row overflows a
                            // narrow window silently — a `Row` does not wrap and does
                            // not clip — which on a small phone put the quality badge
                            // past the screen edge.
                            PlayerMetaPill(
                                text = channel.category,
                                contentColor = colors.primary,
                                containerColor = colors.glass,
                                modifier = Modifier.weight(1f, fill = false),
                                borderColor = colors.primary.copy(alpha = 0.45f)
                            )
                            PlayerMetaPill(
                                text = channel.quality,
                                contentColor = Color.White.copy(alpha = 0.78f),
                                containerColor = colors.glass,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    AppIconButton(
                        iconRes = if (isMuted) KurdishTvIcons.VolumeOff else KurdishTvIcons.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = onToggleMute,
                        style = AppIconButtonStyle.Glass,
                        active = isMuted
                    )
                    AppIconButton(
                        iconRes = if (channel.isFavorite) {
                            KurdishTvIcons.FavoriteFilledRes
                        } else {
                            KurdishTvIcons.FavoriteOutline
                        },
                        contentDescription = if (channel.isFavorite) {
                            "Remove from favourites"
                        } else {
                            "Add to favourites"
                        },
                        onClick = onFavoriteToggle,
                        style = AppIconButtonStyle.Glass,
                        active = channel.isFavorite
                    )
                }

                // ── Transport cluster ─────────────────────────────────────────
                // A plain `Box` with a background, deliberately not a `Surface` and not
                // clickable: a surface here would either clip the circles inside it or
                // add a focus target that competes with all three of them.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(colors.glass, M3ExpressiveShapes.LargeCard)
                        // A white hairline rather than `colors.border`, which is tuned
                        // against the app's own dark surfaces and is very nearly
                        // invisible over a video frame.
                        .border(1.dp, Color.White.copy(alpha = 0.14f), M3ExpressiveShapes.LargeCard)
                        .padding(horizontal = clusterPadding, vertical = clusterPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconButton(
                            iconRes = KurdishTvIcons.ChevronLeft,
                            contentDescription = "Previous channel",
                            onClick = onPreviousChannel,
                            style = AppIconButtonStyle.Glass,
                            size = stepButtonSize,
                            iconSize = (stepButtonSize.value * 0.5f).dp
                        )

                        AppIconButton(
                            iconRes = if (isPlaying) KurdishTvIcons.Pause else KurdishTvIcons.PlayRes,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            onClick = onPlayPauseToggle,
                            style = AppIconButtonStyle.Filled,
                            size = playButtonSize,
                            iconSize = (playButtonSize.value * 0.47f).dp
                        )

                        AppIconButton(
                            iconRes = KurdishTvIcons.ChevronRight,
                            contentDescription = "Next channel",
                            onClick = onNextChannel,
                            style = AppIconButtonStyle.Glass,
                            size = stepButtonSize,
                            iconSize = (stepButtonSize.value * 0.5f).dp
                        )
                    }
                }

                // ── Bottom scrim ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(bottomBand)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Black.copy(alpha = 0.62f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // ── Status and playback options ──────────────────────────────
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        LiveBadge()
                        if (!sleepTimerRemainingText.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            PlayerMetaPill(
                                text = "Sleep $sleepTimerRemainingText",
                                contentColor = colors.onPrimaryContainer,
                                containerColor = colors.primaryContainer
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The sleep timer moved down here from the top bar, where it
                        // was a moon in a circle of four unlabelled glyphs. It now sits
                        // beside the two other options that change the picture, and
                        // they are all labelled. The *countdown* deliberately stays on
                        // the left with the LIVE badge: that is a status readout rather
                        // than a control, and it is the one thing in this bar that has
                        // to stay legible on a narrow window, where these pills drop
                        // their labels.
                        LabelPillButton(
                            iconRes = KurdishTvIcons.Bedtime,
                            label = if (labelled) "Sleep" else "",
                            onClick = onOpenSleepTimer,
                            containerColor = colors.glass,
                            active = !sleepTimerRemainingText.isNullOrEmpty()
                        )

                        // The label names the mode that is *on*, not the one the next
                        // tap selects, so the state is readable rather than being a
                        // preview of an action nobody has taken yet.
                        LabelPillButton(
                            iconRes = KurdishTvIcons.AspectRatio,
                            label = if (labelled) resizeMode.label else "",
                            onClick = onResizeModeToggle,
                            containerColor = colors.glass
                        )

                        // Always named, and always naming the *current* state. It used
                        // to be blank until a filter was switched on, which collapsed
                        // the control to a bare accent-coloured disc in a bar where
                        // every other control is a labelled pill.
                        LabelPillButton(
                            iconRes = KurdishTvIcons.Palette,
                            label = if (labelled) {
                                if (colorFilter.isActive) colorFilter.label else "Colour"
                            } else {
                                ""
                            },
                            onClick = onCycleColorFilter,
                            containerColor = if (colorFilter.isActive) {
                                colors.primary
                            } else {
                                colors.glass
                            },
                            contentColor = if (colorFilter.isActive) colors.onPrimary else Color.White,
                            active = colorFilter.isActive
                        )

                        AppIconButton(
                            iconRes = if (isFullscreen) {
                                KurdishTvIcons.FullscreenExit
                            } else {
                                KurdishTvIcons.Fullscreen
                            },
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
