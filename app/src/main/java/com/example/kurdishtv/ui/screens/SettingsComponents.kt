package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.components.TopEdgeHighlight
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalIsTv

/**
 * Destructive actions that ask for confirmation before committing.
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
 */
internal fun Modifier.readableColumn(isTv: Boolean): Modifier =
    if (isTv) this.widthIn(max = 760.dp) else this

@Composable
internal fun SettingsHeader(onBack: () -> Unit) {
    val colors = LocalAppColors.current
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
            TopEdgeHighlight(inset = SectionCorner)
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
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
