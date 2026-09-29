package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import com.example.BuildConfig
import com.example.kurdishtv.model.AccentColor
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.DeviceMode
import com.example.kurdishtv.model.ThemeMode
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.components.TopEdgeHighlight
import com.example.kurdishtv.ui.components.UpdateCard
import com.example.kurdishtv.ui.components.edgeFade
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.UpdateState
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoColorFilter
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalIsTv
import com.example.ui.theme.appColorsFor
import com.example.ui.theme.isTvMode
import com.example.ui.theme.onColorFor

/**
 * One line explaining what the device-mode control is currently doing.
 *
 * Pure so it can be unit-tested without a `Configuration`, and kept next to the
 * control it describes rather than inline in the composable.
 */
internal fun deviceModeHint(mode: DeviceMode, detectedIsTv: Boolean): String = when (mode) {
    DeviceMode.AUTO ->
        if (detectedIsTv) {
            "Auto — this device reports itself as a TV, so the ten-foot layout is in use."
        } else {
            "Auto — this device reports itself as a phone or tablet, so the handheld layout is in use."
        }
    DeviceMode.TV ->
        "TV — the room-sized layout is forced, whatever the device reports."
    DeviceMode.MOBILE ->
        "Phone — the compact touch layout is forced, whatever the device reports."
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClearFavorites: () -> Unit,
    onClearRecents: () -> Unit,
    onClearCustomPlaylists: () -> Unit,
    onClearCache: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdate: () -> Unit = {},
    onDownloadUpdate: (AppUpdate) -> Unit = {},
    onInstallUpdate: (AppUpdate, String) -> Unit = { _, _ -> },
    onDismissUpdate: () -> Unit = {},
    needsInstallPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val isTv = LocalIsTv.current
    val snackbarHostState = remember { SnackbarHostState() }
    val supportsDynamicColor = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    // What the *device* says, as opposed to what the preference resolved to. Shown
    // beside the control so "Auto" is not a mystery, and read from the configuration
    // here rather than passed down: it is the one place the raw answer is wanted.
    val detectedIsTv = LocalConfiguration.current.isTvMode()
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // Ten-foot treatment.
            //
            // On a television the window is roughly 1920dp wide, and a `fillMaxWidth`
            // card behind a 20dp phone gutter stretched a settings row across the
            // whole screen: label hard against the left edge, switch hard against
            // the right, a metre of empty card between them. Each card is capped to
            // a column that is comfortable to read from a sofa and
            // `horizontalAlignment` centres it. Phones and tablets are untouched.
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = if (isTv) 40.dp else 20.dp,
                end = if (isTv) 40.dp else 20.dp,
                top = 12.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                SettingsHeader(onBack = onBack)
            }

            // ── Playback ──────────────────────────────────────────────────────
            //
            // First, not third. Of everything on this screen, playback is the only
            // thing someone opens Settings to change *because it annoyed them while
            // watching* — autoplay fighting them, controls vanishing mid-tap. It is
            // the section most likely to be needed and the least likely to be found
            // if it is buried under colour pickers.
            item(key = "playback") {
                SettingsSection(
                    title = "Playback",
                    subtitle = "How streams and controls behave",
                    iconRes = KurdishTvIcons.PlayRes
                ) {
                    SettingsSwitchRow(
                        iconRes = KurdishTvIcons.PlayRes,
                        title = "Autoplay",
                        subtitle = "Start a channel as soon as it opens",
                        checked = settings.autoplay,
                        onCheckedChange = { value -> onUpdate { it.copy(autoplay = value) } }
                    )
                    SettingsSwitchRow(
                        iconRes = KurdishTvIcons.Bedtime,
                        title = "Auto-hide controls",
                        subtitle = "Hide player controls while watching",
                        checked = settings.autoHideControls,
                        onCheckedChange = { value -> onUpdate { it.copy(autoHideControls = value) } }
                    )
                    // Resize mode and picture correction were already persisted and
                    // already applied — the player overlay writes them — but neither
                    // appeared anywhere on this screen, so a viewer who had not found
                    // the player's controls had no idea the choice existed, let alone
                    // how to undo it.
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel("Video fit")
                    SegmentedOptions(
                        options = ResizeMode.entries,
                        selected = settings.resizeMode,
                        labelOf = { it.label },
                        onSelect = { mode -> onUpdate { it.copy(resizeMode = mode) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel("Picture correction")
                    ChipRow(
                        options = VideoColorFilter.entries,
                        selected = settings.videoColorFilter,
                        labelOf = { it.label },
                        onSelect = { filter -> onUpdate { it.copy(videoColorFilter = filter) } }
                    )

                }
            }

            // ── Browsing ──────────────────────────────────────────────────────
            item(key = "browsing") {
                SettingsSection(
                    title = "Browsing",
                    subtitle = "What you see first",
                    iconRes = KurdishTvIcons.LiveTv
                ) {
                    SectionLabel("Default category")
                    ChipRow(
                        options = CategoryFilter.entries,
                        selected = settings.startCategory,
                        labelOf = { it.displayName },
                        onSelect = { category -> onUpdate { it.copy(startCategory = category) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSwitchRow(
                        iconRes = KurdishTvIcons.Tv,
                        title = "Channel logos",
                        subtitle = "Load logos over the network",
                        checked = settings.showLogos,
                        onCheckedChange = { value -> onUpdate { it.copy(showLogos = value) } },
                        showDivider = false
                    )
                }
            }

            // ── Interface ──────────────────────────────────────────────────────
            //
            // Its own section rather than a row at the bottom of Appearance, because
            // it is the one setting on this screen that changes the *shape* of the
            // rest of the app rather than its colours. It also has to sit above
            // Motion, because a viewer who arrives here because the cards are
            // unreadable on their box should meet that reason before they are asked
            // to decide whether the animation is worth the frame rate.
            item(key = "interface") {
                SettingsSection(
                    title = "Interface",
                    subtitle = "Which layout to use",
                    iconRes = KurdishTvIcons.Tv
                ) {
                    SectionLabel("Device mode")
                    SegmentedOptions(
                        options = DeviceMode.entries,
                        selected = settings.deviceMode,
                        labelOf = { it.displayName },
                        onSelect = { mode -> onUpdate { it.copy(deviceMode = mode) } }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Says what "Auto" actually resolved to. Without this the row is
                    // a three-way switch with no way to find out which one the
                    // device is currently on — and on a box that misreports its
                    // `uiMode`, "Auto" producing the wrong layout is exactly the
                    // thing the viewer needs to see explained before they can tell
                    // whether the override has fixed it.
                    Text(
                        text = deviceModeHint(settings.deviceMode, detectedIsTv),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Appearance ────────────────────────────────────────────────────
            item(key = "appearance") {
                SettingsSection(
                    title = "Appearance",
                    subtitle = "Colors and theme",
                    iconRes = KurdishTvIcons.Tune
                ) {
                    SectionLabel("Accent color")
                    AccentSwatchRow(
                        selected = settings.accent,
                        // With Material You on, the swatch that is visibly in use is
                        // the wallpaper's, not the stored one. Marking the stored
                        // accent as selected made the screen claim a colour the app
                        // was not drawing.
                        overridden = settings.dynamicColor && supportsDynamicColor,
                        // Tapping a colour is a request for *that* colour, so it also
                        // switches Material You off. Leaving it on meant the wallpaper
                        // kept winning and the tap changed nothing on screen, which
                        // reads as a dead control rather than as a conflict.
                        onSelect = { accent ->
                            onUpdate { it.copy(accent = accent, dynamicColor = false) }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSwitchRow(
                        iconRes = KurdishTvIcons.Palette,
                        title = "Material You colors",
                        subtitle = if (supportsDynamicColor) {
                            "Match your wallpaper (Android 12+)"
                        } else {
                            "Requires Android 12 or newer"
                        },
                        checked = settings.dynamicColor && supportsDynamicColor,
                        enabled = supportsDynamicColor,
                        onCheckedChange = { value -> onUpdate { it.copy(dynamicColor = value) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel("Theme")
                    SegmentedOptions(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        labelOf = { it.displayName },
                        onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } }
                    )
                }
            }

            // ── Update ────────────────────────────────────────────────────────
            item(key = "update") {
                UpdateCard(
                    modifier = Modifier.readableColumn(isTv),
                    state = updateState,
                    onCheck = onCheckForUpdate,
                    onDownload = onDownloadUpdate,
                    onInstall = onInstallUpdate,
                    onDismiss = onDismissUpdate,
                    needsInstallPermission = needsInstallPermission
                )
            }

            // ── Data ──────────────────────────────────────────────────────────
            //
            // Below Update, above About. Every row in this section destroys
            // something, and one of them resets every other preference on the screen.
            // Sat in the middle of the list it read as just another group of
            // switches, with "Reset all settings" two rows above the app version.
            // Buried at the end, it is where destructive actions belong.
            item(key = "data") {
                SettingsSection(
                    title = "Data & storage",
                    subtitle = "Remove saved information",
                    iconRes = KurdishTvIcons.Delete
                ) {
                    SettingsActionRow(
                        iconRes = KurdishTvIcons.FavoriteFilledRes,
                        title = "Clear favorites",
                        subtitle = "Unmark every saved channel",
                        onClick = { pendingAction = PendingAction.CLEAR_FAVORITES }
                    )
                    SettingsActionRow(
                        iconRes = KurdishTvIcons.History,
                        title = "Clear watch history",
                        subtitle = "Reset the recently watched row",
                        onClick = { pendingAction = PendingAction.CLEAR_RECENTS }
                    )
                    SettingsActionRow(
                        iconRes = KurdishTvIcons.AddLink,
                        title = "Remove custom playlists",
                        subtitle = "Delete every imported IPTV link",
                        onClick = { pendingAction = PendingAction.CLEAR_PLAYLISTS }
                    )
                    SettingsActionRow(
                        iconRes = KurdishTvIcons.Delete,
                        title = "Clear channel cache",
                        subtitle = "Re-download the channel list next launch",
                        onClick = { pendingAction = PendingAction.CLEAR_CACHE }
                    )
                    SettingsActionRow(
                        iconRes = KurdishTvIcons.Refresh,
                        title = "Reset all settings",
                        subtitle = "Restore every preference to default",
                        danger = true,
                        showDivider = false,
                        onClick = { pendingAction = PendingAction.RESET_SETTINGS }
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item(key = "about") {
                SettingsSection(
                    title = "About",
                    subtitle = "Kurdish TV Live",
                    iconRes = KurdishTvIcons.Globe
                ) {
                    AboutRow("Version", BuildConfig.VERSION_NAME)
                    AboutRow("Build", BuildConfig.VERSION_CODE.toString())
                    AboutRow("Start category", settings.startCategory.displayName)
                    AboutRow("Sources", "Curated list, IPTV-org, community")

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Play Protect may warn that this app is from an unknown " +
                            "developer. That is expected for a build that is not on the " +
                            "Play Store: choose “More details” → “Install anyway”, or turn " +
                            "off Play Protect in Play Store → Settings.",
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If an update fails with “package conflicts with an " +
                            "existing package”, an older copy signed with a different " +
                            "key is still installed. Uninstall this app once, then install " +
                            "the new version.",
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    pendingAction?.let { action ->
        ConfirmDialog(
            action = action,
            onConfirm = {
                when (action) {
                    PendingAction.CLEAR_FAVORITES -> onClearFavorites()
                    PendingAction.CLEAR_RECENTS -> onClearRecents()
                    PendingAction.CLEAR_PLAYLISTS -> onClearCustomPlaylists()
                    PendingAction.CLEAR_CACHE -> onClearCache()
                    PendingAction.RESET_SETTINGS -> onResetSettings()
                }
                pendingAction = null
            },
            onDismiss = { pendingAction = null }
        )
    }
}

private enum class PendingAction(val title: String, val body: String) {
    CLEAR_FAVORITES("Clear favorites?", "Every channel will be unmarked as a favorite."),
    CLEAR_RECENTS("Clear watch history?", "The recently watched row will be emptied."),
    CLEAR_PLAYLISTS("Remove custom playlists?", "All imported IPTV playlists will be deleted."),
    CLEAR_CACHE("Clear channel cache?", "The cached channel list will be downloaded again."),
    RESET_SETTINGS("Reset all settings?", "Appearance, playback and browsing preferences return to defaults.")
}

/**
 * Caps a card to a column that is comfortable to read from a sofa.
 *
 * Applied per card rather than by wrapping the whole list, so the cap composes with
 * `LazyColumn`'s own `horizontalAlignment` instead of needing a second layout
 * container around the screen.
 */
private fun Modifier.readableColumn(isTv: Boolean): Modifier =
    if (isTv) this.widthIn(max = 760.dp) else this

/**
 * The outline a focused settings row's plate is drawn in.
 *
 * Small on purpose. A row's plate runs the full width of the screen, so its corners
 * are barely on screen at all; a large radius only makes the ends of the highlight
 * look lopsided.
 */
private val RowPlate = RoundedCornerShape(6.dp)

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    // Shared by the click and the ring, so the two cannot disagree about where the
    // viewer is.
    val backSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .readableColumn(LocalIsTv.current)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceHigh,
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, colors.border, CircleShape)
                .tvClickable(
                    interactionSource = backSource,
                    pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                    pressedShape = CircleShape,
                    onClick = onBack
                )
                // A ring, because this one can carry one: 44dp and round, with room
                // on all four sides. Without it the back button had no focus state
                // at all, which on the first screen a viewer reaches after leaving
                // Settings is the worst place for one.
                .expressiveFocusRing(
                    ringColor = colors.primary,
                    interactionSource = backSource,
                    scrim = colors.focusScrim,
                    restShape = M3ExpressivePolygons.Circle,
                    ringShape = M3ExpressivePolygons.Circle
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                SvgIcon(
                    resId = KurdishTvIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "Settings",
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Make it yours • ڕێکخستن",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** [M3ExpressiveShapes.LargeCard]'s radius, as a number, for [TopEdgeHighlight]. */
private val SectionCorner = 26.dp

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    iconRes: Int,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    Surface(
        shape = M3ExpressiveShapes.LargeCard,
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .readableColumn(LocalIsTv.current)
            .border(1.dp, colors.border, M3ExpressiveShapes.LargeCard)
    ) {
        Box {
            // The same hairline every other card in the app carries. See
            // `TopEdgeHighlight` for why it is a line rather than a gradient.
            TopEdgeHighlight(inset = SectionCorner)
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        // A soft cookie rather than the spiked Burst, which made every
                        // section header look like a cog instead of an icon tile.
                        shape = M3ExpressiveShapes.MediumCard,
                        color = colors.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(38.dp)
                        ) {
                            SvgIcon(
                                resId = iconRes,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = subtitle,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // A rule between the header and what it heads, which is a different
                // job from the hairline at the top of the card. That one separates
                // the card from the page; this one separates two parts of the same
                // surface, so it is a step lighter than a card outline — otherwise
                // a settings list reads as a stack of boxes each holding a title
                // and some text.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider)
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = LocalAppColors.current.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun AccentSwatchRow(
    selected: AccentColor,
    overridden: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val colors = LocalAppColors.current
    Column {
        if (overridden) {
            // Say plainly that these are not in effect, and that tapping one is
            // how you take effect. The stored accent still applies the moment
            // Material You is switched off, so the row stays enabled and keeps the
            // checkmark — it is just labelled as pending.
            Text(
                text = "Material You is on, so the app is using your wallpaper's color. " +
                    "Tap one of these to switch it off and use that color instead.",
                color = colors.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentColor.entries.forEach { accent ->
                val swatch = appColorsFor(AppSettings(accent = accent)).primary
                val isSelected = accent == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 52.dp else 44.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    colors.textPrimary
                                } else {
                                    colors.border
                                },
                                shape = CircleShape
                            )
                            .tvClickable { onSelect(accent) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            SvgIcon(
                                resId = KurdishTvIcons.Check,
                                contentDescription = "Selected",
                                // Chosen by measured contrast, not assumed: a light
                                // accent needs dark ink, a dark one needs light.
                                tint = onColorFor(swatch),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = accent.displayName,
                        color = if (isSelected) colors.textPrimary else colors.textTertiary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SegmentedOptions(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(M3ExpressiveShapes.SectionPill)
            .background(colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = if (isSelected) colors.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .tvClickable { onSelect(option) }
            ) {
                Text(
                    text = labelOf(option),
                    color = if (isSelected) colors.onPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * A horizontally scrolling row of single-choice chips.
 *
 * Generic because two different settings need the same control and the picture
 * presets outnumber what a segmented bar can hold on a phone. `labelOf` rather than
 * `toString` so an enum's own constant name never leaks into the UI.
 */
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    LazyRow(
        modifier = Modifier.edgeFade(listState, width = 20.dp),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(options, key = { labelOf(it) }) { option ->
            val isSelected = option == selected
            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = if (isSelected) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isSelected) colors.primary else colors.border,
                        M3ExpressiveShapes.Pill
                    )
                    .tvClickable { onSelect(option) }
            ) {
                Text(
                    text = labelOf(option),
                    color = if (isSelected) colors.onPrimary else colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val contentAlpha = if (enabled) 1f else 0.45f
    // Focus is a plate, not a ring, and the reason is geometric rather than a
    // preference. This row is `fillMaxWidth()`: an outset focus ring would put 7dp
    // of accent off each end of the screen, so a viewer would see the top and bottom
    // of a rectangle and nothing at either side. A full-width row is the one shape
    // in the app that cannot carry an outline, so it is filled instead — which is
    // also the idiom every other full-width list in the app already uses.
    val rowSource = remember { MutableInteractionSource() }
    val isFocused by rowSource.collectIsFocusedAsState()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RowPlate)
                .background(if (isFocused) colors.surfaceHigh else Color.Transparent)
                .tvClickable(
                    interactionSource = rowSource,
                    enabled = enabled,
                    pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                    pressedShape = RowPlate
                ) { onCheckedChange(!checked) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SvgIcon(
                resId = iconRes,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.textPrimary.copy(alpha = contentAlpha),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary.copy(alpha = contentAlpha),
                    fontSize = 12.sp
                )
            }
            // The switch is the row's *indicator*, not a second control.
            //
            // `Switch` is focusable by design, so with the row already clickable this
            // put a D-pad stop immediately inside a row that is itself a stop: arrowing
            // across the settings list stopped twice on every switch row, and landing
            // on the switch meant the row's own highlight was not where the remote was.
            // `focusProperties` applies to the focus targets after it, and the switch
            // has none of its own, so this removes the duplicate stop and nothing else
            // — the toggle still works by touch, and the row still toggles it.
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.focusProperties { canFocus = false },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surfaceElevated
                )
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    // The same full-width plate as the switch row above, and for the same reason: a
    // ring on a `fillMaxWidth()` row runs off both ends of the screen.
    val rowSource = remember { MutableInteractionSource() }
    val isFocused by rowSource.collectIsFocusedAsState()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RowPlate)
                .background(if (isFocused) colors.surfaceHigh else Color.Transparent)
                .tvClickable(
                    interactionSource = rowSource,
                    pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                    pressedShape = RowPlate,
                    onClick = onClick
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SvgIcon(
                resId = iconRes,
                contentDescription = null,
                tint = if (danger) colors.liveRed else colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (danger) colors.liveRed else colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            SvgIcon(
                resId = KurdishTvIcons.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = LocalAppColors.current.textSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = LocalAppColors.current.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ConfirmDialog(
    action: PendingAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = M3ExpressiveShapes.LargeCard,
        containerColor = colors.surface,
        title = {
            Text(
                text = action.title,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = action.body,
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = M3ExpressiveShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.liveRed,
                    contentColor = Color.White
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.primary)
            }
        }
    )
}
