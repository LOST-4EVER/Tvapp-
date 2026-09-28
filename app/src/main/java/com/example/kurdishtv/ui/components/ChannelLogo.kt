package com.example.kurdishtv.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import coil.size.Precision
import coil.size.Scale
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
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.applicationContext.cacheDir, "channel_logo_cache"))
                        .maxSizeBytes(48L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
                .also { instance = it }
        }
}

/**
 * Channel logo with a graceful fallback.
 *
 * Plenty of channels in the merged remote playlists point at a `tvg-logo` that
 * 404s, and a handful in the curated catalog ship without one at all. Previously
 * that rendered an empty tile; it now degrades to the channel's initials on a
 * per-channel accent, so a missing logo reads as a designed monogram rather than
 * a hole in the grid.
 */
@Composable
fun ChannelLogo(
    channelName: String,
    logoUrl: String?,
    showLogos: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    size: Dp = 96.dp
) {
    val context = LocalContext.current
    val loader = remember(context) { LogoLoader.get(context) }
    // Coil sizes are in pixels, so the dp the tile is drawn at has to be converted
    // against the current density.
    val density = LocalDensity.current

    val request = remember(logoUrl, showLogos, size, density) {
        if (!showLogos) null
        else logoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val targetPx = with(density) { size.roundToPx() }.coerceAtLeast(1)
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                // Logos are square tiles of a few dozen dp, but the source images
                // are often 512-1024px. Without an explicit size, Coil decodes at
                // full resolution and keeps a bitmap roughly 40x larger than the
                // space it occupies — the dominant memory cost in a grid of them.
                .size(targetPx, targetPx)
                .scale(Scale.FIT)
                .precision(Precision.INEXACT)
                .build()
        }
    }

    // The monogram tile deliberately does NOT take the caller's [modifier].
    //
    // It used to, and that was a layout bug rather than a cosmetic one: the same
    // lambda is invoked from three different places — as this composable's own
    // output when there is no image, and from inside `SubcomposeAsyncImage`'s
    // `loading` and `error` slots — and the slots are composed in a *subcomposition*
    // whose constraints are the ones Coil hands the `SubcomposeAsyncImage` node,
    // not the ones the caller handed this function. Re-applying a caller modifier
    // that says `fillMaxSize` inside that subcomposition means measuring a fill
    // against a box that is itself the fill, which is how a tile ended up escaping
    // the rounded logo well it was supposed to be sitting inside.
    //
    // Filling the space it is actually given is the only thing a fallback can
    // honestly do, and the well that owns the clip already clips it.
    val fallback: @Composable () -> Unit = {
        // Derived from the channel name, so the same channel keeps the same colour
        // across refreshes, reorderings and devices.
        val accent = remember(channelName) { monogramAccent(channelName) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.07f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(channelName),
                color = accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }

    if (request == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            fallback()
        }
    } else {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = channelName,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            imageLoader = loader,
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

/**
 * A small, fixed set of accents used for logo-less channels.
 *
 * Hues are spread around the wheel and all sit at similar perceived lightness on
 * the dark surfaces, so two adjacent cards never end up with one vivid tile and
 * one that disappears. Fixed rather than fully generated so the grid reads as a
 * palette instead of a colour wheel.
 */
private val MonogramAccents = listOf(
    Color(0xFFF2B33D), // gold
    Color(0xFF4FC3A1), // jade
    Color(0xFF5AA9F0), // azure
    Color(0xFFF07A9A), // rose
    Color(0xFFEE8A4C), // ember
    Color(0xFF9C8CF0), // violet
    Color(0xFF4FC0D4), // turquoise
    Color(0xFFD8C05A) // sand
)

/** Stable per-channel accent, chosen from a hash of the name. */
internal fun monogramAccent(seed: String): Color {
    // String.hashCode is specified by the JDK, so this is stable across processes
    // and app restarts — the same channel always gets the same colour.
    val hash = seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else it }
    val index = ((hash % MonogramAccents.size) + MonogramAccents.size) % MonogramAccents.size
    return MonogramAccents[index]
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
