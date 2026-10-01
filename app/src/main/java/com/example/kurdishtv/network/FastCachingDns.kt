package com.example.kurdishtv.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance DNS resolver with an in-memory TTL cache.
 *
 * In IPTV streaming, repeated segment and playlist requests continuously query
 * the same domain names. Bypassing repeated OS-level DNS lookups saves 100-300ms
 * per network call, drastically reducing stream startup and channel zapping latency.
 *
 * ## Three things the naive version of this got wrong
 *
 *  1. **The wall clock.** It stamped entries with `System.currentTimeMillis()`, which
 *     NITZ and a user in the system settings both move. Every entry then appears to
 *     have been stored in the future, its age computes as *negative*, and a negative
 *     age passes any `age < ttl` test — so a clock corrected backwards pins every
 *     cached address as fresh until the clock catches back up. On a TV box whose time
 *     arrives by NITZ after boot, that is most of a session. [System.nanoTime] is
 *     monotonic and no user or network action can move it.
 *  2. **No single-flight.** OkHttp calls [lookup] from every dispatcher thread, and a
 *     grid of channel logos from one CDN, or five HLS connections opening at once, all
 *     miss a cold cache together and each performs its own full system lookup. The
 *     stampede is the exact case a DNS cache exists to prevent.
 *  3. **Unbounded.** Hostnames come from playlists, and a viewer can import as many
 *     playlists as they like. Every host any of them names stays resident for the life
 *     of the process.
 *
 * See [DnsEntryTest] for the freshness rule and [normalizeStreamUrl] in the parser for
 * the related de-duplication work.
 */
class FastCachingDns(
    private val delegate: Dns = Dns.SYSTEM,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) : Dns {

    private data class CachedRecord(
        val addresses: List<InetAddress>,
        val timestampNanos: Long
    )

    private val cache = ConcurrentHashMap<String, CachedRecord>()

    /**
     * One lock per hostname, so concurrent misses on the *same* host queue up behind
     * each other while misses on *different* hosts stay parallel — which is what a
     * playlist merge fetching five sources at once actually wants.
     *
     * `computeIfAbsent` rather than `getOrPut`, which is not atomic on a
     * `ConcurrentHashMap` and would hand two threads two different locks, leaving the
     * stampede it exists to stop.
     */
    private val hostLocks = ConcurrentHashMap<String, Any>()

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.nanoTime()
        val cached = cache[hostname]
        if (cached != null && isEntryFresh(now, cached.timestampNanos, ttlMs)) {
            return cached.addresses
        }

        val lock = hostLocks.computeIfAbsent(hostname) { Any() }
        synchronized(lock) {
            try {
                // Re-checked inside the lock rather than trusted from outside: while
                // this thread waited, the thread ahead of it very likely resolved the
                // host and cached the answer. Skipping this is the difference between
                // one system lookup for a stampede and N of them.
                val afterLock = System.nanoTime()
                val nowCached = cache[hostname]
                if (nowCached != null && isEntryFresh(afterLock, nowCached.timestampNanos, ttlMs)) {
                    return nowCached.addresses
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
                    cache[hostname] = CachedRecord(addresses, System.nanoTime())
                    trimToSize()
                }
                return addresses
            } finally {
                // Dropped with the entry rather than kept for the process's life, for
                // the same reason the address cache is bounded: the set of hostnames
                // is not the app's to assume.
                hostLocks.remove(hostname, lock)
            }
        }
    }

    /**
     * Drops the oldest entries until the cache is back within [maxEntries].
     *
     * Expired entries go first and cost nothing to identify. Only if that was not
     * enough is the cache truncated wholesale, which loses live entries too — but a
     * miss is a slow path, never a wrong answer, so over-capacity is the worse state
     * to keep.
     */
    private fun trimToSize() {
        if (cache.size <= maxEntries) return

        val now = System.nanoTime()
        val expired = cache.entries.filter { !isEntryFresh(now, it.value.timestampNanos, ttlMs) }
        expired.forEach { cache.remove(it.key, it.value) }

        if (cache.size > maxEntries) {
            cache.clear()
        }
    }

    fun clear() {
        cache.clear()
    }

    companion object {
        /** Long enough to outlast a zapping session, short enough to notice a moved host. */
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L

        /**
         * Comfortably above the number of hosts a session touches — a handful of
         * playlist origins, a logo CDN or two, and a few dozen stream CDNs — so this
         * is a backstop against an imported playlist full of junk, not a limit anyone
         * reaches in normal use.
         */
        const val DEFAULT_MAX_ENTRIES = 256

        /**
         * Whether a record stamped at [storedAtNanos] is still usable at [nowNanos].
         *
         * The `age >= 0` clause is the whole reason the stamp is taken from
         * `nanoTime` and not from a wall clock, and it is kept as an explicit test
         * rather than as an assumption. A record that claims to have been stored in the
         * future is stale, not fresh: serving it would pin a stale address for as long
         * as the discrepancy lasts, and on a device whose clock arrives late by hours
         * that is not a small window.
         *
         * `ttlNanos` of zero or less disables caching entirely, which is a legitimate
         * configuration and treats every entry as expired.
         */
        internal fun isEntryFresh(
            nowNanos: Long,
            storedAtNanos: Long,
            ttlNanos: Long
        ): Boolean {
            if (ttlNanos <= 0L) return false
            val age = nowNanos - storedAtNanos
            return age >= 0L && age < ttlNanos
        }
    }
}