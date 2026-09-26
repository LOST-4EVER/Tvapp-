package com.example.kurdishtv.ui.components

import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = M3ExpressiveShapes.LargeCard,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SvgIcon(
                    resId = KurdishTvIcons.Bedtime,
                    contentDescription = null,
                    tint = KurdishSunGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sleep Timer",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Playback will automatically stop after the selected duration:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(3).forEach { mins ->
                        val isSelected = currentMinutes == mins
                        TimerChip(
                            minutes = mins,
                            isSelected = isSelected,
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
                        val isSelected = currentMinutes == mins
                        TimerChip(
                            minutes = mins,
                            isSelected = isSelected,
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
                            color = Color.Red.copy(alpha = 0.2f),
                            modifier = Modifier
                                .weight(1f)
                                .bouncyClickable {
                                    onSelectMinutes(0)
                                    onDismiss()
                                }
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = KurdishSunGold)
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
    Surface(
        shape = M3ExpressiveShapes.Pill,
        color = if (isSelected) KurdishSunGold else DarkSurfaceVariant,
        modifier = modifier
            .border(
                1.dp,
                if (isSelected) KurdishSunGold else DarkCardBorder,
                M3ExpressiveShapes.Pill
            )
            .bouncyClickable(scaleDown = 0.90f, onClick = onClick)
    ) {
        Text(
            text = "${minutes}m",
            color = if (isSelected) Color.Black else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
