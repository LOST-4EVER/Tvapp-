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
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object NetworkClient {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

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
            64,
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
            maxRequests = 32
            maxRequestsPerHost = 10
        }

        val connectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)

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

        try {
            val cacheDir = File(appContext.cacheDir, "kurdish_tv_http_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            builder.cache(Cache(cacheDir, 25L * 1024L * 1024L))
        } catch (_: Exception) {
            // Graceful fallback if cache dir not accessible
        }

        return builder.build()
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
                                val body = if (res.isSuccessful) res.body?.string() else null
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
