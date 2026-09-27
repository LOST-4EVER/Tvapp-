package com.example.kurdishtv.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.ui.theme.LocalAppColors
import java.io.File

/**
 * One [ImageLoader] shared by every logo in the app.
 *
 * Channel logos are small, numerous and scrolled constantly, so decoding them
 * through one loader with a generous memory cache and a dedicated disk cache is
 * what keeps scrolling smooth on a TV. Per-request loaders would each hold their
 * own cache and defeat this.
 */
private object LogoLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.applicationContext.cacheDir, "channel_logo_cache"))
                        .maxSizeBytes(40L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
                .also { instance = it }
        }
}

/**
 * Channel logo with a graceful fallback.
 *
 * Plenty of channels in the merged remote playlists point at a `tvg-logo` that
 * 404s. Previously that rendered an empty tile; it now degrades to the channel's
 * initials, which stays readable and looks deliberate.
 */
@Composable
fun ChannelLogo(
    channelName: String,
    logoUrl: String?,
    showLogos: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val loader = remember(context) { LogoLoader.get(context) }

    val request = remember(logoUrl, showLogos, loader) {
        if (!showLogos) null
        else logoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .imageLoader(loader)
                .crossfade(true)
                .build()
        }
    }

    val fallback: @Composable () -> Unit = {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(channelName),
                color = colors.primary.copy(alpha = 0.75f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }

    if (request == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = channelName,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize().padding(contentPadding),
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

/** Up to two initials, e.g. "Kurdistan 24" -> "K2". */
private fun initialsOf(name: String): String {
    val words = name.trim().split(' ', '-', '_').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}
