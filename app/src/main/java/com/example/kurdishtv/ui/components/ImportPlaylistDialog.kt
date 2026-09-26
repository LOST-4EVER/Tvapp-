package com.example.kurdishtv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImportPlaylistDialog(
    customUrls: Set<String>,
    onAddUrl: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = M3ExpressiveShapes.LargeCard,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SvgIcon(
                    resId = KurdishTvIcons.AddLink,
                    contentDescription = null,
                    tint = KurdishSunGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add Custom IPTV Playlist",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter any direct .m3u, .m3u8, or JSON Kurdish channel stream link:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        inputError = null
                    },
                    placeholder = {
                        Text(
                            text = "https://example.com/playlist.m3u8",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = M3ExpressiveShapes.MediumCard,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = KurdishSunGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (inputError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = inputError ?: "",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val clean = urlInput.trim()
                        if (clean.startsWith("http://") || clean.startsWith("https://")) {
                            onAddUrl(clean)
                            urlInput = ""
                        } else {
                            inputError = "Must start with http:// or https://"
                        }
                    },
                    shape = M3ExpressiveShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KurdishSunGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Playlist", fontWeight = FontWeight.Bold)
                }

                if (customUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Saved Playlists (${customUrls.size}):",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(120.dp)) {
                        items(customUrls.toList()) { url ->
                            Surface(
                                shape = M3ExpressiveShapes.SmallCard,
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = url,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onRemoveUrl(url) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        SvgIcon(
                                            resId = KurdishTvIcons.Delete,
                                            contentDescription = "Remove",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = KurdishSunGold)
            }
        }
    )
}
