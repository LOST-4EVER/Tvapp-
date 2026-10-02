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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.kurdishtv.ui.components.edgeFade
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.appColorsFor
import com.example.ui.theme.onColorFor

private val RowPlate = RoundedCornerShape(6.dp)

@Composable
internal fun AccentSwatchRow(
    selected: AccentColor,
    overridden: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val colors = LocalAppColors.current
    Column {
        if (overridden) {
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
                                color = if (isSelected) colors.textPrimary else colors.border,
                                shape = CircleShape
                            )
                            .tvClickable(
                                interactionSource = swatchSource,
                                pressedFill = colors.textPrimary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                                pressedShape = CircleShape,
                                onClick = { onSelect(accent) }
                            )
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
