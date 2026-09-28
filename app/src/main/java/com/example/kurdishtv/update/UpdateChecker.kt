package com.example.kurdishtv.update

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
    private val okHttpClient: OkHttpClient,
    private val releasesApiUrl: String = RELEASES_API_URL,
    private val manifestUrl: String = MANIFEST_URL
) {

    /**
     * Fetches the newest release, or null when the app is already current.
     *
     * The static manifest is read **first**, deliberately.
     *
     * The releases API only carries the tag (`v1.0.22`), so a build number has to
     * be recovered from it. This project stamps `VERSION_CODE` with the CI run
     * number, so the tag's patch number is on a completely different scale: a tag
     * of `v1.0.22` guesses 22 while the installed build is 36. The comparison
     * then reads 22 > 36 as false and the app reports itself current forever,
     * even with a newer release sitting right there.
     *
     * The manifest is written by the build job, which is the only place that
     * actually knows the run number, so it is authoritative.
     *
     * The API is still consulted when the manifest is unreachable. The release
     * body does record the run number, so that is preferred over the tag guess;
     * only when neither is available does the entry report itself as unknown
     * rather than offering an update it cannot prove is newer.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdate?> =
        withContext(Dispatchers.IO) {
            val fromManifest = runCatching { fetchUpdateFrom(manifestUrl) }
                .onFailure { NetworkClient.logDebug("Update check via manifest failed", it) }
                .getOrNull()

            if (fromManifest != null) {
                return@withContext Result.success(
                    fromManifest.takeIf { it.isNewerThan(currentVersionCode) }
                )
            }

            val fromApi = runCatching { fetchUpdateFrom(releasesApiUrl) }
                .onFailure { NetworkClient.logDebug("Update check via API failed", it) }
                .getOrNull()
                ?: return@withContext Result.failure(
                    IOException("Could not reach the update server")
                )

            // A build number recovered from the release body is on the same
            // scale as VERSION_CODE and can be compared directly. Anything else
            // is unverified, so the app stays quiet rather than prompting for an
            // update that may not exist.
            //
            // The recovered run number replaces [AppUpdate.versionCode] outright
            // rather than being used only as a yes/no gate. Checking that the body
            // *mentions* a run number and then comparing whatever versionCode the
            // parser happened to produce left a hole: when the run number was
            // present but unparseable, the gate passed and the tag's patch number
            // was compared against VERSION_CODE anyway.
            val runNumber = fromApi.notes
                ?.let { runNumberFrom(it) }
                ?: return@withContext Result.success(null)

            Result.success(
                fromApi.copy(versionCode = runNumber)
                    .takeIf { it.isNewerThan(currentVersionCode) }
            )
        }

    private fun fetchUpdateFrom(url: String): AppUpdate? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json, application/json")
            .header("User-Agent", NetworkClient.USER_AGENT)
            // Always revalidate: a stale response means missing a release.
            .header("Cache-Control", "no-cache")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Update check failed (HTTP ${response.code})")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("Update check returned an empty response")
            return UpdateManifestParser.parse(body)
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
        // Keyed to the build being fetched. A single shared ".part" file meant a
        // half-finished download of the *previous* release was resumed byte-for-byte
        // into a different APK, producing a file that was neither version. The size
        // check catches that most of the time, but not when the two builds happen to
        // be the same length — and a corrupt APK reaches the installer either way.
        val partial = File(downloadDir, "$APK_FILE_NAME.${update.versionCode}.part")
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

    /**
     * Removes previously downloaded APKs and any abandoned partial downloads.
     *
     * Partials are matched by prefix rather than by an exact name, because they are
     * named per build number and there is no way to know which builds were started.
     */
    fun clearDownloaded(downloadDir: File) {
        runCatching {
            File(downloadDir, APK_FILE_NAME).delete()
            downloadDir.listFiles { file ->
                file.name.startsWith("$APK_FILE_NAME.") && file.name.endsWith(".part")
            }?.forEach { it.delete() }
        }
    }

    companion object {
        const val APK_FILE_NAME = "kurdish-tv-update.apk"

        /**
         * The primary feed. Written by the release job from the same run number
         * that is compiled into the APK's `VERSION_CODE`, so the two are always
         * directly comparable. Served from raw.githubusercontent with no API
         * rate limit, which is why it is tried before the releases API rather
         * than after it.
         */
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/LOST-4EVER/Tvapp-/main/update.json"

        /**
         * Secondary feed, used only when the manifest cannot be fetched.
         *
         * Kept because the manifest is a single point of failure: if that
         * commit is reverted or the branch is renamed, the app falls back to the
         * releases API. Note that the API exposes only a tag name, so the build
         * number derived from it is approximate — see [checkForUpdate].
         */
        const val RELEASES_API_URL =
            "https://api.github.com/repos/LOST-4EVER/Tvapp-/releases/latest"

        private const val DOWNLOAD_CHUNK_BYTES = 64 * 1024
        private const val PROGRESS_STEP_BYTES = 256 * 1024L

        /**
         * The release body records the CI run number, e.g.
         * `*on run* \`36\`.`. Recovering it is what makes an API-sourced update
         * comparable with the installed `VERSION_CODE`.
         */
        internal val RUN_NUMBER_IN_BODY = Regex("""on run\*?\s*`?(\d+)`?""")
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
            val body = root.optString("body").takeIf { it.isNotBlank() }
            val tag = root.optString("tag_name")
            return AppUpdate(
                versionName = tag.removePrefix("v").ifBlank { "—" },
                // Prefer the run number recorded in the body: it is the same
                // value compiled into the APK as VERSION_CODE. The tag's patch
                // number is not comparable with it.
                versionCode = runNumberFrom(body) ?: parseVersionCode(tag),
                releaseUrl = root.optString("html_url"),
                downloadUrl = best.optString("browser_download_url"),
                sizeBytes = best.optLong("size"),
                publishedAt = root.optString("published_at").takeIf { it.isNotBlank() },
                notes = body
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
     * Recovers the CI run number from a release body, or null when absent.
     *
     * This is the only build-number source on the same scale as the APK's
     * `VERSION_CODE`, so it takes priority over the tag.
     */
    fun runNumberFrom(body: String?): Int? {
        val match = body?.let { UpdateChecker.RUN_NUMBER_IN_BODY.find(it) } ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    /**
     * Extracts a build number from a tag such as `v1.0.21`.
     *
     * NOTE: this returns the tag's patch number, which for this project is *not*
     * the value compiled into the APK as `VERSION_CODE` (that is the CI run
     * number). It is only a last-resort guess; prefer [runNumberFrom].
     */
    fun parseVersionCode(tag: String): Int {
        val digits = tag.trim().removePrefix("v")
            .split('.', '-', '_')
            .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        return digits.lastOrNull() ?: 0
    }
}
