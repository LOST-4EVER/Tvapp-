package com.example.kurdishtv.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-performance DNS resolver with an in-memory TTL cache.
 *
 * In IPTV streaming, repeated segment and playlist requests continuously query
 * the same domain names. Bypassing repeated OS-level DNS lookups saves 100-300ms
 * per network call, drastically reducing stream startup and channel zapping latency.
 */
class FastCachingDns(
    private val delegate: Dns = Dns.SYSTEM,
    private val ttlMs: Long = TimeUnit.MINUTES.toMillis(5)
) : Dns {

    private data class CachedRecord(
        val addresses: List<InetAddress>,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CachedRecord>()

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        val cached = cache[hostname]
        if (cached != null && (now - cached.timestamp) < ttlMs) {
            return cached.addresses
        }

        val addresses = try {
            delegate.lookup(hostname)
        } catch (se: SecurityException) {
            throw UnknownHostException("Permission denied during DNS lookup: ${se.message}")
        } catch (t: Throwable) {
            if (t is UnknownHostException) throw t
            throw UnknownHostException("Unable to resolve host '$hostname': ${t.message}")
        }

        if (addresses.isNotEmpty()) {
            cache[hostname] = CachedRecord(addresses, now)
        }
        return addresses
    }

    fun clear() {
        cache.clear()
    }
}
