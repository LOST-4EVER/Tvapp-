package com.example.kurdishtv.network

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkClient {

    fun createOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 25 * 1024 * 1024) // 25MB HTTP Cache for fast playlist/logo loading

        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "KurdishTV-Android/2.0")
                    .header("Accept", "text/plain, application/json, application/x-mpegURL, */*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
