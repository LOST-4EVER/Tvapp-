package com.example.kurdishtv.network

import android.content.Context
import androidx.media3.common.util.UnstableApi
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
     * Hard ceiling on a single playlist/JSON response. Playlists are small; a hostile or
     * misconfigured source must not be able to allocate an unbounded string and take the
     * app down with an OOM.
     */
    private const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024

    @Volatile
    private var clientInstance: OkHttpClient? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: buildClient(context.applicationContext).also { clientInstance = it }
        }
    }

    @UnstableApi
    fun createMediaDataSourceFactory(context: Context): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(getOkHttpClient(context))
            .setUserAgent(USER_AGENT)
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

        // Safe DNS lookup preventing SecurityException / EPERM crashes
        val safeDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (se: SecurityException) {
                    throw UnknownHostException("Permission denied during DNS lookup: ${se.message}")
                } catch (t: Throwable) {
                    if (t is UnknownHostException) throw t
                    throw UnknownHostException("Unable to resolve host '$hostname': ${t.message}")
                }
            }
        }

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
            // Transparently gzip the playlist and manifest responses. These are
            // text payloads that compress roughly 10:1, which matters a lot on the
            // slow links these streams are often fetched over.
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.header("Accept-Encoding") == null) {
                    val compressed = request.newBuilder()
                        .header("Accept-Encoding", "gzip")
                        .build()
                    chain.proceed(compressed)
                } else {
                    chain.proceed(request)
                }
            }

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
            if (read < 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /**
     * Asynchronously and cancellably fetches a URL string using OkHttp without blocking threads.
     */
    suspend fun OkHttpClient.fetchString(request: Request): String? =
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
                            continuation.resume(null)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use { res ->
                                val body = if (res.isSuccessful) res.body?.byteStream()?.use { it.readCapped() } else null
                                if (continuation.isActive) {
                                    continuation.resume(body)
                                }
                            }
                        } catch (_: Throwable) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                })
            } catch (_: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
}
