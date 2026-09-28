package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import com.example.BuildConfig
import com.example.kurdishtv.model.AccentColor
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.MotionLevel
import com.example.kurdishtv.model.ThemeMode
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.components.UpdateCard
import com.example.kurdishtv.ui.components.edgeFade
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.UpdateState
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.appColorsFor
import com.example.ui.theme.onColorFor

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
    val snackbarHostState = remember { SnackbarHostState() }
    val supportsDynamicColor = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
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
                    SettingsSwitchRow(
                        iconRes = KurdishTvIcons.LiveTv,
                        title = "Animated LIVE badge",
                        subtitle = "Turn off to save battery on large lists",
                        checked = settings.livePulse,
                        onCheckedChange = { value -> onUpdate { it.copy(livePulse = value) } },
                        showDivider = false
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
                    CategoryChipRow(
                        selected = settings.startCategory,
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

            // ── Appearance ────────────────────────────────────────────────────
            item(key = "appearance") {
                SettingsSection(
                    title = "Appearance",
                    subtitle = "Colors, theme and motion",
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
                        onSelect = { accent -> onUpdate { it.copy(accent = accent) } }
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

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel("Motion")
                    SegmentedOptions(
                        options = MotionLevel.entries,
                        selected = settings.motion,
                        labelOf = { it.displayName },
                        onSelect = { level -> onUpdate { it.copy(motion = level) } }
                    )                }
            }

            // ── Update ────────────────────────────────────────────────────────
            item(key = "update") {
                UpdateCard(
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

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceElevated,
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, colors.border, CircleShape)
                .bouncyClickable(onClick = onBack)
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
            .border(1.dp, colors.border, M3ExpressiveShapes.LargeCard)
    ) {
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
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
            // Say plainly that these are not in effect. The stored accent still
            // applies the moment Material You is switched off, so the row stays
            // enabled and keeps the checkmark — it is just labelled as pending.
            Text(
                text = "Material You is on, so the app is using your wallpaper's color. " +
                    "Turn it off below to use one of these.",
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
                            .bouncyClickable(scaleDown = 0.88f) { onSelect(accent) },
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
                    .bouncyClickable(scaleDown = 0.95f) { onSelect(option) }
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

@Composable
private fun CategoryChipRow(
    selected: CategoryFilter,
    onSelect: (CategoryFilter) -> Unit
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    LazyRow(
        modifier = Modifier.edgeFade(listState, width = 20.dp),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(CategoryFilter.entries, key = { it.name }) { category ->
            val isSelected = category == selected
            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = if (isSelected) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isSelected) colors.primary else colors.border,
                        M3ExpressiveShapes.Pill
                    )
                    .bouncyClickable(scaleDown = 0.92f) { onSelect(category) }
            ) {
                Text(
                    text = category.displayName,
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(scaleDown = 0.98f, enabled = enabled) { onCheckedChange(!checked) }
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(scaleDown = 0.98f, onClick = onClick)
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
