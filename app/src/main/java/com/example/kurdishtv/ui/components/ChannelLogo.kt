package com.example.kurdishtv.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
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
                    // The memory cache holds *decoded bitmaps*, so its cost is
                    // width x height x 4 bytes per logo. At the 512px ceiling that is
                    // 1 MB a logo, which means 20% of a large heap was room for a
                    // hundred of them — far more than the grid ever has on screen, and
                    // memory the OS will not hand to anything else while the app is
                    // alive.
                    //
                    // Sized for what is actually visible plus a screenful of
                    // scroll-back, which is what the cache is for. The *disk* cache
                    // is where the long tail belongs, and it is already generous.
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.12)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.applicationContext.cacheDir, "channel_logo_cache"))
                        .maxSizeBytes(48L * 1024 * 1024)
                        .build()
                }
                // No crossfade.
                //
                // It was on as the loader default, so every one of the several hundred
                // logos in the grid animated its own fade-in on every appearance. A
                // fade is a per-frame alpha animation and a second draw of the image
                // for its duration, so scrolling the grid — which is what a viewer
                // does constantly — meant a rolling wave of them, each holding a
                // render node open. On a television that is the most expensive
                // animation in the app and the one nobody asked for: a logo that is
                // simply *there* when it loads is what reads as fast, because the
                // monogram fallback is already on screen underneath it.
                .crossfade(false)
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
            // Decoded once for the largest tile this can be drawn in, not for the
            // tile it happens to be in right now.
            //
            // The target used to be the caller's own draw size, which quietly made
            // the decode resolution a function of the layout: the same channel's
            // logo was a different cached bitmap on the compact landscape card than
            // on the full-size one, so scrolling between them re-decoded the image
            // instead of hitting the cache — several hundred times a screenful, in
            // exactly the gesture the app asks viewers to make most.
            //
            // Rounding the target up to a power of two also means the bitmaps a
            // grid holds are a small fixed set of sizes rather than one per cell
            // width, which is what lets the memory cache actually do its job.
            val targetPx = decodeSizeFor(size, density)
            ImageRequest.Builder(context)
                .data(url)
                // Logos are square tiles of a few dozen dp, but the source images
                // are often 512-1024px. Without an explicit size, Coil decodes at
                // full resolution and keeps a bitmap roughly 40x larger than the
                // space it occupies — the dominant memory cost in a grid of them.
                .size(targetPx, targetPx)
                .scale(Scale.FIT)
                // INEXACT lets the sampler pick a size at or slightly above the
                // target. EXACT would force an exact-size bitmap, which for every
                // width the adaptive grid can produce means a *new* allocation that
                // nothing else in the cache can ever be reused for.
                .precision(Precision.INEXACT)
                .build()
        }
    }

    // The monogram tile deliberately does NOT take the caller's [modifier].
    //
    // It is a child of the box below, so the box already fills and clips it, and
    // the caller's modifier describes the box rather than this layer. It used to
    // be applied to the monogram itself, and when that composable was invoked
    // from inside `SubcomposeAsyncImage`'s state slots the caller modifier was
    // re-measured against the *subcomposition*'s constraints — measuring a
    // `fillMaxSize` against a box that was itself a fill. That is how a tile
    // ended up escaping the rounded logo well it was meant to be sitting in.
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

    // Whether the bitmap has actually arrived.
    //
    // The monogram used to be drawn underneath the image permanently, on the theory
    // that the image would simply cover it. Most logos do not cover it: a `tvg-logo`
    // is very often a PNG with a transparent background, and a transparent pixel
    // draws nothing — so the monogram's letters and its accent gradient showed
    // straight through the artwork. MBC Iraq's logo rendered with a stray "M" sitting
    // across it. An opaque logo hid the bug, which is why it went unnoticed.
    //
    // Tracking the state is what `SubcomposeAsyncImage` would have done through its
    // `loading` slot, and the reason this file avoids `SubcomposeAsyncImage` in the
    // first place still stands: it builds a whole second composition per cell, and a
    // screen of cards is dozens of them, paid on every scroll of the grid. Reading
    // `onState` off a plain `AsyncImage` costs one boolean and no subcomposition.
    //
    // ## Why there are three states and not one boolean
    //
    // The previous version was a single `loaded` flag, and the monogram was drawn
    // whenever it was false — which is to say during the load *and* after a failure.
    // That makes the monogram stand in for two completely different things, and the
    // first of them is wrong: the monogram is the channel's *fallback identity*, not
    // a placeholder. Every tile in the grid therefore drew letters and an accent
    // gradient that it then tore down the instant the real artwork arrived. On a
    // cold start that is the whole first screen changing its mind at once, and it is
    // slower to read than a neutral block would be, because the eye has already read
    // the monogram and then has to read the logo again.
    //
    // So: a neutral [LogoSkeleton] while the load is in flight, the artwork once it
    // lands, and the monogram only when there is genuinely no artwork to show —
    // either the load failed, or this channel has no `tvg-logo` at all.
    var state by remember(request) { mutableStateOf(LogoLoadState.Idle) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // No request at all means there is nothing to wait for: logos are switched
        // off, or the channel has no `tvg-logo`. The monogram is the answer, not a
        // placeholder for one.
        if (request == null || state == LogoLoadState.Error) {
            fallback()
        }

        if (request != null) {
            // The skeleton is for the load *in flight* only.
            //
            // The test was `state != Success`, which also covers [LogoLoadState.Error]
            // — and because the monogram above is drawn first and this second, a
            // failed logo left an opaque grey block sitting on top of the fallback
            // forever. The fallback exists precisely for the failed case, so the one
            // state in which it was needed was the one state that hid it. Error and
            // not-yet-arrived are both "no artwork", so both show the monogram, and
            // only the two states where artwork may still be coming get a block.
            if (state == LogoLoadState.Idle || state == LogoLoadState.Loading) {
                LogoSkeleton(modifier = Modifier.fillMaxSize())
            }
            AsyncImage(
                model = request,
                contentDescription = channelName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                imageLoader = loader,
                onState = { painterState ->
                    state = when (painterState) {
                        // `Empty` is the state before the request is even handed to
                        // the loader, so it is still "not arrived" — treating it as
                        // anything else would flash the wrong thing on every cell.
                        is AsyncImagePainter.State.Empty -> LogoLoadState.Idle
                        is AsyncImagePainter.State.Loading -> LogoLoadState.Loading
                        is AsyncImagePainter.State.Success -> LogoLoadState.Success
                        is AsyncImagePainter.State.Error -> LogoLoadState.Error
                    }
                }
            )
        }
    }
}

/**
 * Where a logo tile is in its load.
 *
 * [Loading] and [Idle] draw the same thing; they are separate because
 * `AsyncImagePainter.State` distinguishes them and collapsing them at the call site
 * is where the old off-by-one crept in.
 */
private enum class LogoLoadState { Idle, Loading, Success, Error }

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

/**
 * The pixel size a logo is decoded at for a tile of [size] dp.
 *
 * Rounded **up** to the next power of two, with a floor, so that:
 *
 *  - a logo is never decoded below the size it is drawn at, which is what makes it
 *    look soft on a television;
 *  - every tile that shares a bucket shares a bitmap, so the memory cache holds a
 *    handful of sizes rather than one per distinct cell width the adaptive grid can
 *    produce. Without the bucketing, a grid that lays out at 137dp and one that
 *    lays out at 141dp keep entirely separate copies of every logo on screen.
 *
 * Capped at [MAX_DECODE_PX]: a 1024px source decoded at full size is 4 MB of
 * bitmap for a tile that is a couple of hundred pixels across, and the grid can hold
 * hundreds of them.
 */
private fun decodeSizeFor(size: Dp, density: Density): Int {
    val raw = with(density) { size.roundToPx() }.coerceAtLeast(1)
    if (raw >= MAX_DECODE_PX) return MAX_DECODE_PX
    // Next power of two, from MIN_DECODE_PX up.
    var bucket = MIN_DECODE_PX
    while (bucket < raw) bucket = bucket shl 1
    return bucket.coerceAtMost(MAX_DECODE_PX)
}

/** The smallest bucket. Below this a logo is small enough that artefacts show. */
private const val MIN_DECODE_PX = 128

/** The ceiling. A grid of several hundred cards cannot afford 1024px bitmaps. */
private const val MAX_DECODE_PX = 512

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
