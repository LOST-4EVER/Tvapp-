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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.AccentColor
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.components.TopEdgeHighlight
import com.example.kurdishtv.ui.components.edgeFade
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalIsTv
import com.example.ui.theme.appColorsFor
import com.example.ui.theme.onColorFor

/**
 * The building blocks [SettingsScreen] is assembled from.
 *
 * They live here rather than in the screen because that file had grown past a
 * thousand lines holding two unrelated things: the settings screen's own layout —
 * the order of its sections and what each one says — and the ten generic controls
 * it happens to use. The screen's own structure is the part worth reading top to
 * bottom, and it was the part buried under a switch row, a chip row and a colour
 * swatch.
 *
 * Every declaration here is `private`, as it was in the screen file, so this is a
 * move and not a new API: nothing outside this file can reach any of it. The two
 * that are genuinely shared with the screen — [PendingAction] and
 * [Modifier.readableColumn] — are the only two declared above `private`, and
 * [SettingsScreen] is the sole caller of both.
 *
 * Same package as the screen, so no call site and no import changed.
 */
internal enum class PendingAction(val title: String, val body: String) {
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
internal fun Modifier.readableColumn(isTv: Boolean): Modifier =
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
internal fun SettingsHeader(onBack: () -> Unit) {
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
internal fun SettingsSection(
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
internal fun SectionLabel(text: String) {
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
internal fun AccentSwatchRow(
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
                // The palette is derived, not stored, and building it allocates a
                // whole colour set per swatch. Remembered so that recomposing this
                // screen — which happens on every switch toggle and every focus
                // change anywhere in it — does not rebuild five of them.
                val swatch = remember(accent) { appColorsFor(AppSettings(accent = accent)).primary }
                val isSelected = accent == selected
                val swatchSource = remember(accent) { MutableInteractionSource() }
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
                            .tvClickable(
                                interactionSource = swatchSource,
                                pressedFill = colors.textPrimary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                                pressedShape = CircleShape,
                                onClick = { onSelect(accent) }
                            )
                            // A focus ring, and this row needed one most of all.
                            //
                            // The selected swatch was distinguished only by being
                            // 8dp larger and carrying a 3dp border. That reads as
                            // "chosen" but not as "the remote is here", so arrowing
                            // across the five accents was a blind walk — and these
                            // swatches are the one control on the screen whose
                            // selection is purely visual, so there is nothing else to
                            // fall back on.
                            .expressiveFocusRing(
                                ringColor = colors.primary,
                                interactionSource = swatchSource,
                                scrim = colors.focusScrim,
                                restShape = M3ExpressivePolygons.Circle,
                                ringShape = M3ExpressivePolygons.Circle
                            ),
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
internal fun <T> SegmentedOptions(
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
            val optionSource = remember(option) { MutableInteractionSource() }
            // Focus shown as a fill, not a ring — and this control is the reason.
            //
            // `expressiveFocusRing` draws 7dp outside the element, and the segments
            // here are 4dp apart, so a ring on one segment paints a notch into its
            // neighbour. That is the exact failure the ring's own geometry test
            // guards against for the channel grid's 14dp gutter, and this control is
            // tighter than that.
            //
            // So it uses the other idiom the app already has for a control too small
            // to carry an outline: a background plate, as the switch rows below do.
            // Focus is legible, and it stays inside the segment that owns it.
            val isFocused by optionSource.collectIsFocusedAsState()
            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = when {
                    isSelected -> colors.primary
                    isFocused -> colors.focusScrim
                    else -> Color.Transparent
                },
                modifier = Modifier
                    .weight(1f)
                    .tvClickable(
                        interactionSource = optionSource,
                        pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                        pressedShape = M3ExpressiveShapes.Pill,
                        onClick = { onSelect(option) }
                    )
            ) {
                Text(
                    text = labelOf(option),
                    // A focused-but-unselected segment needs its own contrast, or the
                    // plate is invisible against the label. The selected segment is
                    // already carrying its own fill, so it keeps the stronger ink.
                    color = when {
                        isSelected -> colors.onPrimary
                        isFocused -> colors.textPrimary
                        else -> colors.textSecondary
                    },
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
internal fun <T> ChipRow(
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
            val chipSource = remember(option) { MutableInteractionSource() }
            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = if (isSelected) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isSelected) colors.primary else colors.border,
                        M3ExpressiveShapes.Pill
                    )
                    .tvClickable(
                        interactionSource = chipSource,
                        pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                        pressedShape = M3ExpressiveShapes.Pill,
                        onClick = { onSelect(option) }
                    )
                    // A focus ring, as on the segmented bar above. The default
                    // category row is a dozen chips the D-pad walks across, and
                    // without it a focused chip was indistinguishable from an
                    // unselected one.
                    .expressiveFocusRing(
                        ringColor = colors.primary,
                        interactionSource = chipSource,
                        scrim = colors.focusScrim,
                        restShape = M3ExpressivePolygons.Square,
                        ringShape = M3ExpressivePolygons.Square
                    )
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
internal fun SettingsSwitchRow(
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
internal fun SettingsActionRow(
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
internal fun AboutRow(label: String, value: String) {
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
internal fun ConfirmDialog(
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
