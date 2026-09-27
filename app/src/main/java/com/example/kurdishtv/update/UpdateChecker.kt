package com.example.kurdishtv.update

import android.content.Context
import com.example.kurdishtv.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Checks GitHub for a newer release and downloads the APK.
 *
 * The release feed is read from a small JSON manifest rather than the GitHub API
 * directly. Two reasons:
 *
 * 1. `api.github.com` enforces a low unauthenticated rate limit and returns 404
 *    outright for a private repository, so an in-app check would fail for most
 *    users.
 * 2. The manifest is static, so it can be served from a CDN or raw file host,
 *    cached aggressively, and pointed at a different host without shipping a new
 *    build of the app.
 *
 * When [manifestUrl] is the GitHub API the response is parsed as a release
 * object; when it is a plain manifest the same parser reads the equivalent
 * fields. [UpdateManifestParser] handles both shapes.
 */
class UpdateChecker(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val manifestUrl: String = DEFAULT_MANIFEST_URL
) {

    /**
     * Fetches the newest release, or null when the app is already current.
     *
     * Returns null both for "no newer version" and for "could not check", so
     * callers should surface a generic message rather than claiming up-to-date
     * on a network error.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdate?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(manifestUrl)
                    .header("Accept", "application/vnd.github+json, application/json")
                    .header("User-Agent", NetworkClient.USER_AGENT)
                    // Always revalidate: a stale manifest means missing a release.
                    .header("Cache-Control", "no-cache")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Update check failed (HTTP ${response.code})")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) throw IOException("Update check returned an empty response")
                    UpdateManifestParser.parse(body)
                }?.takeIf { it.isNewerThan(currentVersionCode) }
            }
        }

    /**
     * Downloads the APK to [downloadDir], reporting progress as it goes.
     *
     * The response is streamed rather than buffered: APKs are tens of megabytes
     * and buffering one in memory on a TV would risk an OOM. If a partial file is
     * already present the request asks the server to resume, which matters on the
     * slow connections these streams are often fetched over.
     */
    suspend fun download(
        update: AppUpdate,
        downloadDir: File,
        onProgress: (DownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = File(downloadDir, APK_FILE_NAME)
        val partial = File(downloadDir, "$APK_FILE_NAME.part")
        runCatching {
            downloadDir.mkdirs()
            val existing = if (partial.exists()) partial.length() else 0L

            val request = Request.Builder()
                .url(update.downloadUrl)
                .header("User-Agent", NetworkClient.USER_AGENT)
                .apply {
                    // Resume only when we already have some of the file.
                    if (existing > 0) header("Range", "bytes=$existing-")
                }
                .build()

            onProgress(DownloadState.Running(existing, update.sizeBytes))

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw IOException("Download failed (HTTP ${response.code})")
                }

                // A 200 to a ranged request means the server ignored the Range
                // header and is sending the whole file, so start over.
                val resuming = response.code == 206
                val append = resuming && existing > 0
                if (!append) partial.delete()

                val body = response.body ?: throw IOException("Download returned no body")
                val total = (body.contentLength().takeIf { it > 0 } ?: 0L) +
                    (if (append) existing else 0L)

                body.byteStream().use { input ->
                    java.io.FileOutputStream(partial, append).use { output ->
                        val buffer = ByteArray(DOWNLOAD_CHUNK_BYTES)
                        var written = if (append) existing else 0L
                        var lastReport = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            // Throttle progress callbacks; emitting one per 16 KB
                            // would flood the recomposition for a 40 MB file.
                            if (written - lastReport >= PROGRESS_STEP_BYTES) {
                                lastReport = written
                                onProgress(DownloadState.Running(written, total))
                            }
                        }
                    }
                }

                onProgress(DownloadState.Running(partial.length(), partial.length()))

                if (partial.length() <= 0L) throw IOException("Downloaded file was empty")
                // A truncated download is worse than none: the installer would
                // fail with a parse error that looks like a broken app.
                if (update.sizeBytes > 0 && partial.length() != update.sizeBytes) {
                    partial.delete()
                    throw IOException("Download was incomplete, please try again")
                }

                if (target.exists()) target.delete()
                if (!partial.renameTo(target)) {
                    partial.copyTo(target, overwrite = true)
                    partial.delete()
                }
                onProgress(DownloadState.Done(target.absolutePath, target.length()))
                target
            }
        }.onFailure { error ->
            onProgress(DownloadState.Failed(error.message ?: "Download failed"))
        }
    }

    /** Removes a previously downloaded APK so the next check starts clean. */
    fun clearDownloaded(downloadDir: File) {
        runCatching {
            File(downloadDir, APK_FILE_NAME).delete()
            File(downloadDir, "$APK_FILE_NAME.part").delete()
        }
    }

    companion object {
        const val APK_FILE_NAME = "kurdish-tv-update.apk"

        /**
         * A static manifest describing the newest build. Point this at GitHub
         * Pages, a raw file host or any static JSON endpoint.
         */
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/LOST-4EVER/Tvapp-/main/update.json"

        private const val DOWNLOAD_CHUNK_BYTES = 64 * 1024
        private const val PROGRESS_STEP_BYTES = 256 * 1024L
    }
}

/**
 * Parses an update manifest.
 *
 * Accepts either a minimal custom manifest or a GitHub release object, so the
 * same code path works whether the feed is hand-written or generated.
 */
object UpdateManifestParser {

    fun parse(json: String): AppUpdate? {
        val root = JSONObject(json)

        // A plain manifest describes the release directly.
        if (root.has("download_url")) return fromManifest(root)

        // A GitHub release object: pick the best APK asset.
        if (root.has("assets")) {
            val assets = root.optJSONArray("assets") ?: return null
            var best: JSONObject? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                // Prefer the universal build: it runs on every device.
                if (best == null || name.contains("universal", ignoreCase = true)) {
                    best = asset
                }
            }
            best ?: return null
            return AppUpdate(
                versionName = root.optString("tag_name").removePrefix("v").ifBlank { "—" },
                versionCode = parseVersionCode(root.optString("tag_name")),
                releaseUrl = root.optString("html_url"),
                downloadUrl = best.optString("browser_download_url"),
                sizeBytes = best.optLong("size"),
                publishedAt = root.optString("published_at").takeIf { it.isNotBlank() },
                notes = root.optString("body").takeIf { it.isNotBlank() }
            )
        }
        return null
    }

    private fun fromManifest(root: JSONObject): AppUpdate? {
        val url = root.optString("download_url").takeIf { it.isNotBlank() } ?: return null
        val version = root.optString("version").ifBlank { root.optString("tag_name") }
        return AppUpdate(
            versionName = version.removePrefix("v").ifBlank { version },
            versionCode = root.optInt("version_code").takeIf { it > 0 }
                ?: parseVersionCode(version),
            releaseUrl = root.optString("release_url"),
            downloadUrl = url,
            sizeBytes = root.optLong("size_bytes"),
            publishedAt = root.optString("published_at").takeIf { it.isNotBlank() },
            notes = root.optString("notes").takeIf { it.isNotBlank() }
        )
    }

    /**
     * Extracts a comparable build number from a tag such as `v1.0.21`.
     *
     * Falls back to the patch number, which is monotonic for this project's
     * release scheme, and to 0 when nothing numeric is present.
     */
    fun parseVersionCode(tag: String): Int {
        val digits = tag.trim().removePrefix("v")
            .split('.', '-', '_')
            .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        return digits.lastOrNull() ?: 0
    }
}
