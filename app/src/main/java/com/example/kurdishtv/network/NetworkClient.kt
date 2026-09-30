package com.example.kurdishtv.network

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.BuildConfig
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object NetworkClient {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Debug logging that is compiled out of release builds by R8, so diagnostics
     * can be left in place without shipping verbose logs.
     */
    fun logDebug(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.d("KurdishTv", message, error)
        }
    }

    /**
     * Hard ceiling on a single playlist/JSON response. Playlists are small; a hostile or
     * misconfigured source must not be able to allocate an unbounded string and take the
     * app down with an OOM.
     */
    private const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024

    @Volatile
    private var clientInstance: OkHttpClient? = null

    @Volatile
    private var mediaClientInstance: OkHttpClient? = null

    /**
     * Safe, high-performance cached DNS lookup shared by both playlist and media clients.
     */
    private val safeDns: Dns = FastCachingDns()

    fun getOkHttpClient(context: Context): OkHttpClient {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: buildClient(context.applicationContext).also { clientInstance = it }
        }
    }

    /**
     * A second client dedicated to video, deliberately **not** sharing the
     * playlist client's cache or dispatcher.
     *
     * Sharing them caused two real problems:
     *
     *  - HLS segments were written into the 64 MB HTTP cache. Segment responses
     *    usually carry no `Cache-Control`, so OkHttp stored megabytes of MPEG-TS
     *    and evicted the playlists and logo images that actually benefit from
     *    being cached.
     *  - The playlist client allows 6 concurrent requests per host, and it is
     *    shared with Coil. A grid scrolling past a dozen logos from one CDN could
     *    hold those slots open while a stream was trying to fetch its segments
     *    from that same host, so playback stalled behind logo loading.
     *
     * Video also needs a longer read timeout: these streams are frequently served
     * from slow origins, and a 15 s cutoff aborts segments that were about to land.
     */
    private fun getMediaClient(context: Context): OkHttpClient {
        return mediaClientInstance ?: synchronized(this) {
            mediaClientInstance ?: OkHttpClient.Builder()
                // The same DNS wrapper the playlist client uses. It was missing
                // here, so the protection only covered half the app: a
                // restricted-profile device that denied the lookup raised a raw
                // SecurityException out of the media client, which surfaces as an
                // unplayable stream rather than as the UnknownHostException the
                // other half of the app turns into a retryable failure.
                .dns(safeDns)
                .dispatcher(
                    Dispatcher().apply {
                        maxRequests = 16
                        maxRequestsPerHost = 8
                    }
                )
                .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                // No cache and no gzip interceptor: a media body must stream
                // through untouched, and re-encoding video to save bandwidth costs
                // far more than it saves on a live stream.
                .build()
                .also { mediaClientInstance = it }
        }
    }

    @UnstableApi
    fun createMediaDataSourceFactory(context: Context): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(getMediaClient(context))
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )
    }

    private fun buildClient(appContext: Context): OkHttpClient {
        val executorService = ThreadPoolExecutor(
            0,
            32,
            60L,
            TimeUnit.SECONDS,
            SynchronousQueue()
        ) { runnable ->
            Thread(runnable, "KurdishTv-OkHttp-Worker").apply {
                isDaemon = false
                setUncaughtExceptionHandler { _, _ ->
                    // Prevent process crash from thread-level uncaught exceptions
                }
            }
        }

        val dispatcher = Dispatcher(executorService).apply {
            maxRequests = 24
            maxRequestsPerHost = 6
        }

        // Keeping connections alive longer avoids a fresh TLS handshake per
        // request. Channel logos alone can mean dozens of requests per screen.
        val connectionPool = ConnectionPool(16, 10, TimeUnit.MINUTES)

        val builder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .dns(safeDns)
            .connectionPool(connectionPool)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            // No Accept-Encoding interceptor here on purpose.
            //
            // OkHttp already negotiates gzip transparently: when the caller sets no
            // Accept-Encoding, BridgeInterceptor adds `Accept-Encoding: gzip` itself
            // and strips the matching Content-Encoding off the response before the
            // body is handed over. That behaviour is conditional on the header being
            // *absent*. An interceptor that adds the header by hand — which is what
            // this class used to do — therefore switches transparent mode off while
            // still getting the compressed bytes from the server. The playlist and
            // update manifest then arrived as gzip data that was decoded as UTF-8,
            // producing mojibake that every parser rejected, and every remote source
            // silently contributed zero channels.
            //
            // Letting OkHttp do it gets the same bandwidth saving and a body that is
            // already plain text.

        try {
            val cacheDir = File(appContext.cacheDir, "kurdish_tv_http_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            // Raised from 25 MB: the HTTP cache now also holds the update manifest
            // and the image requests Coil makes through a shared client.
            builder.cache(Cache(cacheDir, 64L * 1024L * 1024L))
        } catch (_: Exception) {
            // Graceful fallback if cache dir not accessible
        }

        return builder.build()
    }

    /**
     * Reads at most [MAX_RESPONSE_BYTES] from the stream. Anything past the cap is
     * ignored, which is plenty for a playlist and bounds memory use.
     */
    private fun InputStream.readCapped(): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (total < MAX_RESPONSE_BYTES) {
            val read = read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_BYTES - total))
            // `read <= 0`, not `read < 0`.
            //
            // `InputStream.read` is permitted to return 0 for a non-zero length,
            // and a `BufferedInputStream` over some socket-backed sources does.
            // The old `if (read < 0) break` treated that as progress and kept
            // looping: `total` never advanced, the loop condition never changed,
            // and the call spun on a stream that was never going to produce
            // another byte. That is a busy-wait holding a dispatcher thread and
            // burning battery, on the IO dispatcher every playlist fetch runs on,
            // against a server that had already stalled. Zero is not progress
            // either way, so both cases end the read.
            if (read <= 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /**
     * Asynchronously and cancellably fetches a URL, reporting *why* it failed.
     *
     * This used to return `String?`, and the difference matters. A null cannot
     * distinguish "the source is gone" from "the network hiccuped", so a caller
     * has exactly two options: give up on the first failure, or retry blindly.
     * Both are wrong. A 404 will still be a 404 after four attempts and four
     * wasted round trips; a dropped connection is usually fine a second later.
     *
     * Splitting the two lets the caller retry exactly the failures that can
     * change their mind.
     */
    suspend fun OkHttpClient.fetchBody(request: Request): FetchOutcome =
        suspendCancellableCoroutine { continuation ->
            try {
                val call = newCall(request)
                continuation.invokeOnCancellation {
                    try {
                        call.cancel()
                    } catch (_: Exception) {}
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            // No response was ever received, so this is always a
                            // transport-level hiccup and always worth another try.
                            continuation.resume(FetchOutcome.Transient(e.javaClass.simpleName))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val outcome = try {
                            response.use { res ->
                                val body = if (res.isSuccessful) {
                                    res.body?.byteStream()?.use { it.readCapped() }
                                } else {
                                    null
                                }
                                if (body == null) res.code.asOutcome() else FetchOutcome.Success(body)
                            }
                        } catch (t: Throwable) {
                            FetchOutcome.Transient(t.javaClass.simpleName)
                        }
                        if (continuation.isActive) {
                            continuation.resume(outcome)
                        }
                    }
                })
            } catch (t: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(FetchOutcome.Transient(t.javaClass.simpleName))
                }
            }
        }

    /**
     * Whether an HTTP status is worth trying again.
     *
     * 404 and the rest of the 4xx range are the source's answer, not a glitch,
     * and repeating the request only delays the refresh. 408 (request timeout)
     * and 429 (rate limited) are the exceptions: the server is explicitly saying
     * "not now", which is the one case where waiting genuinely helps. Every 5xx
     * is a server-side fault that frequently clears on its own.
     */
    private fun Int.asOutcome(): FetchOutcome = when {
        this == 408 || this == 429 -> FetchOutcome.Transient("HTTP $this")
        this >= 500 -> FetchOutcome.Transient("HTTP $this")
        else -> FetchOutcome.Permanent(this)
    }
}

/** The result of a single request, distinguishing the failures worth repeating. */
sealed interface FetchOutcome {
    data class Success(val body: String) : FetchOutcome

    /** The server answered, and the answer was no. Retrying cannot change it. */
    data class Permanent(val code: Int) : FetchOutcome

    /** A hiccup — 5xx, rate limit, dropped connection, timeout. */
    data class Transient(val reason: String) : FetchOutcome
}

/**
 * How long to wait before attempt number [attempt] + 1.
 *
 * Exponential, with "equal jitter" — half the delay fixed, half random. Pure
 * jitter can collapse to nearly zero and defeat the backoff; no jitter at all
 * is worse here, because every source in a refresh fails at roughly the same
 * moment and would otherwise retry in lockstep, hammering the same origin in
 * the same instant.
 *
 * Split out as a pure function so the shape of the curve can be tested without
 * a network, a clock, or a real backoff in a test's runtime.
 *
 * @param jitter a value in `[0, 1)`, injected so the curve is deterministic
 *   under test. Production passes a real random draw.
 */
internal fun retryDelayMs(attempt: Int, jitter: Float): Long {
    val exponential = BASE_RETRY_DELAY_MS shl (attempt - 1).coerceIn(0, 6)
    val capped = exponential.coerceAtMost(MAX_RETRY_DELAY_MS)
    val fixed = capped / 2
    return (fixed + (capped - fixed) * jitter.coerceIn(0f, 1f)).toLong()
}

private const val BASE_RETRY_DELAY_MS = 400L
private const val MAX_RETRY_DELAY_MS = 4_000L
