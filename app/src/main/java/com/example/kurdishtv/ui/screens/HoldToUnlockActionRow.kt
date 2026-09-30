package com.example.kurdishtv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalIsTv
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * An action row that triggers a standard click on tap, but when held continuously
 * for [HOLD_DURATION_MS] (10 seconds), triggers [onHoldCompleted] with live visual
 * progress and countdown.
 */
@Composable
fun HoldToUnlockActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onHoldCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    showDivider: Boolean = true
) {
    val colors = LocalAppColors.current
    val isTv = LocalIsTv.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var isHolding by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(10L) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val progress = remember { Animatable(0f) }

    val accent = if (danger) colors.liveRed else colors.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) colors.primary else if (danger) colors.liveRed.copy(alpha = 0.35f) else colors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    isHolding = true
                    remainingSeconds = 10L

                    holdJob?.cancel()
                    holdJob = scope.launch {
                        progress.snapTo(0f)
                        val animJob = launch {
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 10_000, easing = LinearEasing)
                            )
                        }

                        for (sec in 10 downTo 1) {
                            remainingSeconds = sec.toLong()
                            delay(1000L)
                        }
                        remainingSeconds = 0L
                        isHolding = false
                        animJob.cancel()
                        onHoldCompleted()
                    }

                    val upOrCancel = waitForUpOrCancellation()
                    val elapsed = System.currentTimeMillis() - startTime
                    holdJob?.cancel()
                    isHolding = false
                    scope.launch { progress.snapTo(0f) }

                    if (upOrCancel != null && elapsed < 1000L) {
                        onClick()
                    }
                }
            },
        color = when {
            isFocused -> colors.surfaceHigh
            isHolding -> colors.surfaceVariant
            else -> colors.surfaceElevated
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (danger) colors.liveRed.copy(alpha = 0.15f) else colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = if (danger) colors.liveRed else colors.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            color = if (danger) colors.liveRed else colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHolding) {
                                "Hold for $remainingSeconds s to unlock Ads & Terms..."
                            } else {
                                subtitle
                            },
                            color = if (isHolding) accent else colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isHolding) {
                    Text(
                        text = "${remainingSeconds}s",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isHolding,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accent,
                        trackColor = colors.border
                    )
                }
            }
        }
    }

    if (showDivider) {
        Spacer(modifier = Modifier.height(6.dp))
    }
}
