package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.DownloadState
import com.example.kurdishtv.update.UpdateState
import com.example.ui.theme.LocalAppColors

/**
 * In-app update surface.
 *
 * Renders whichever state the updater is in: idle, checking, up to date, an
 * available release, an in-flight download, or a finished download waiting to be
 * handed to the package installer.
 */
@Composable
fun UpdateCard(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (AppUpdate) -> Unit,
    onInstall: (AppUpdate, String) -> Unit,
    onDismiss: () -> Unit,
    /**
     * Whether Android is still refusing to let this app install packages.
     *
     * When it is, the card says so *before* the viewer taps Install and then has to
     * discover it, instead of after — and, more importantly, it stops offering a
     * "Retry" that re-runs the update check while the APK that is already on disk
     * sits there unusable. See the note in `TvViewModel.requestInstallUpdate`.
     */
    needsInstallPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Surface(
        // A lobed shape clips its content, which cut this card's title and
        // status line off mid-word. Text-bearing surfaces need a shape whose
        // silhouette stays clear of the padding box.
        shape = M3ExpressiveShapes.LargeCard,
        color = colors.surfaceElevated,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.Refresh,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App update",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusLine(state),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Shown or not shown, with nothing in between. This was an
            // `AnimatedVisibility` that faded and grew, so the card below it was
            // measured twice on every state change of a check the viewer did not
            // start.
            if (state !is UpdateState.Idle) {
                Column {
                    when (state) {
                        is UpdateState.Checking -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LoadingIndicator(size = 20.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking for updates…",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        is UpdateState.Available -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            releaseNotes(state.update)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onDownload(state.update) },
                                shape = M3ExpressiveShapes.Pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Download ${state.update.versionName}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is UpdateState.Downloading -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            DownloadProgress(state.progress)
                        }

                        is UpdateState.ReadyToInstall -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (needsInstallPermission) {
                                    "Downloaded and ready. Android needs permission to " +
                                        "install this update — tap Install and allow it " +
                                        "when the system asks."
                                } else {
                                    "Downloaded. Install to update — Android will ask you " +
                                        "to confirm."
                                },
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onInstall(state.update, state.filePath)
                                },
                                shape = M3ExpressiveShapes.Pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Install update", fontWeight = FontWeight.Bold)
                            }
                        }

                        is UpdateState.Failed -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.message,
                                color = colors.liveRed,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onCheck,
                                    shape = M3ExpressiveShapes.Pill,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    )
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
                            }
                        }

                        is UpdateState.UpToDate -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "You're on the latest version.",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        UpdateState.Idle -> Unit
                    }
                }
            }

            if (state is UpdateState.Idle || state is UpdateState.UpToDate) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCheck,
                    shape = M3ExpressiveShapes.Pill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check for updates", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: DownloadState) {
    val colors = LocalAppColors.current
    when (progress) {
        is DownloadState.Idle -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoadingIndicator(size = 20.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Preparing download…", color = colors.textSecondary, fontSize = 12.sp)
            }
        }

        is DownloadState.Running -> {
            val fraction = progress.fraction
            // The bar is written straight to. It was an `animateFloatAsState` easing
            // towards each new value, so a download throttled to one update per
            // 256 KB dragged a five-second spring behind every one of them and the
            // bar read as several hundred bytes behind the number beside it.
            val animated = fraction ?: 0f
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Downloading…", color = colors.textSecondary, fontSize = 12.sp)
                    Text(
                        text = if (fraction != null) {
                            "${(fraction * 100).toInt()}%"
                        } else {
                            formatBytes(progress.bytesRead)
                        },
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { animated },
                        color = colors.primary,
                        trackColor = colors.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(M3ExpressiveShapes.Pill)
                    )
                } else {
                    LinearProgressIndicator(
                        color = colors.primary,
                        trackColor = colors.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(M3ExpressiveShapes.Pill)
                    )
                }
            }
        }

        is DownloadState.Failed -> {
            Text(progress.message, color = colors.liveRed, fontSize = 12.sp)
        }

        is DownloadState.Done -> {
            Text("Download complete", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun releaseNotes(update: AppUpdate) {
    val colors = LocalAppColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    colors.primaryContainer.copy(alpha = 0.35f),
                    M3ExpressiveShapes.Pill
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version ${update.versionName} is available",
                color = colors.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (update.sizeBytes > 0) {
                Text(
                    text = formatBytes(update.sizeBytes),
                    color = colors.onPrimaryContainer,
                    fontSize = 11.sp
                )
            }
        }
        if (!update.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = update.notes.orEmpty().take(280),
                color = colors.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

private fun statusLine(state: UpdateState): String = when (state) {
    UpdateState.Idle -> "Stay on the latest build"
    UpdateState.Checking -> "Checking…"
    is UpdateState.UpToDate -> "Up to date (${state.currentVersion})"
    is UpdateState.Available -> "Version ${state.update.versionName} available"
    is UpdateState.Downloading -> "Downloading update…"
    is UpdateState.ReadyToInstall -> "Ready to install"
    is UpdateState.Failed -> "Update problem"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
