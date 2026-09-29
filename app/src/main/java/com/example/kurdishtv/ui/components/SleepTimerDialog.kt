package com.example.kurdishtv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val options = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = M3ExpressiveShapes.ExtraLargeRounded,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = M3ExpressiveShapes.MediumCard,
                    color = colors.primaryContainer
                ) {
                    SvgIcon(
                        resId = KurdishTvIcons.Bedtime,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sleep timer",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Playback stops automatically after the selected duration.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(3).forEach { mins ->
                        TimerChip(
                            minutes = mins,
                            isSelected = currentMinutes == mins,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.drop(3).forEach { mins ->
                        TimerChip(
                            minutes = mins,
                            isSelected = currentMinutes == mins,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                        )
                    }
                    if (currentMinutes > 0) {
                        Surface(
                            shape = M3ExpressiveShapes.Pill,
                            color = colors.liveRed.copy(alpha = 0.18f),
                            modifier = Modifier
                                .weight(1f)
                                .tvClickable {
                                    onSelectMinutes(0)
                                    onDismiss()
                                }
                        ) {
                            Text(
                                text = "Cancel",
                                color = colors.liveRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.primary)
            }
        }
    )
}

@Composable
private fun TimerChip(
    minutes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    // Shared by the click and the ring, so the two cannot disagree about where the
    // viewer is.
    val chipSource = remember { MutableInteractionSource() }
    Surface(
        shape = M3ExpressiveShapes.Pill,
        color = if (isSelected) colors.primary else colors.surfaceHigh,
        modifier = modifier
            .border(
                1.dp,
                if (isSelected) colors.primary else colors.border,
                M3ExpressiveShapes.Pill
            )
            .tvClickable(
                interactionSource = chipSource,
                pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                pressedShape = M3ExpressiveShapes.Pill,
                onClick = onClick
            )
            // These chips are picked with the remote, and selection is a *choice*
            // while focus is where the viewer *is* — a chip can be focused without
            // being the current duration, so the selected/unselected fill says
            // nothing about where the highlight is. Without a ring these were
            // indistinguishable under the D-pad.
            .expressiveFocusRing(
                ringColor = colors.primary,
                interactionSource = chipSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Square,
                ringShape = ShapeMorph.focusRing
            )
    ) {
        Text(
            text = "${minutes}m",
            color = if (isSelected) colors.onPrimary else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}
