package com.example

import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.UpdateManifestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reading the update feed.
 *
 * The whole updater turns on one comparison — `versionCode > VERSION_CODE` — so
 * getting the *scale* of the number wrong does not produce a partial failure, it
 * produces silence. A tag's patch number (22) is on a different scale from the
 * CI run number the APK is stamped with (36); comparing them reads 22 > 36 as
 * false, and the app reports itself current forever with a newer release
 * sitting right there. These tests pin which number comes from where, because
 * that is the part that is easy to "simplify" back into a bug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateManifestParserTest {

    /** The static manifest, in the exact shape the release job writes. */
    private val manifest = """
        {
          "version": "1.0.41",
          "version_code": 142,
          "release_url": "https://github.com/o/r/releases/tag/v1.0.41",
          "download_url": "https://github.com/o/r/releases/download/v1.0.41/kurdish-tv-universal.apk",
          "size_bytes": 2653944,
          "published_at": "",
          "notes": "Build 142 from 3a25685b."
        }
    """.trimIndent()

    @Test
    fun `a static manifest is read field for field`() {
        val update = UpdateManifestParser.parse(manifest)
        assertNotNull(update)

        assertEquals("1.0.41", update!!.versionName)
        assertEquals(142, update.versionCode)
        assertEquals(
            "https://github.com/o/r/releases/download/v1.0.41/kurdish-tv-universal.apk",
            update.downloadUrl
        )
        assertEquals(2653944L, update.sizeBytes)
        assertEquals("Build 142 from 3a25685b.", update.notes)
    }

    @Test
    fun `an empty published_at is null rather than an empty string`() {
        // The manifest field is always present but often blank, and the update
        // card renders it directly, so "" would show as a stray gap.
        assertNull(UpdateManifestParser.parse(manifest)!!.publishedAt)
    }

    @Test
    fun `a github release object is read`() {
        val release = """
            {
              "tag_name": "v1.0.42",
              "html_url": "https://github.com/o/r/releases/tag/v1.0.42",
              "published_at": "2026-01-01T00:00:00Z",
              "body": "Built from commit abc123456 *on run* 143.",
              "assets": [
                {"name": "kurdish-tv-arm64-v8a.apk", "browser_download_url": "https://x/arm64.apk", "size": 100},
                {"name": "kurdish-tv-universal.apk", "browser_download_url": "https://x/universal.apk", "size": 200}
              ]
            }
        """.trimIndent()

        val update = UpdateManifestParser.parse(release)
        assertNotNull(update)

        assertEquals("1.0.42", update!!.versionName)
        assertEquals("https://x/universal.apk", update.downloadUrl)
        assertEquals(200L, update.sizeBytes)
        assertEquals(143, update.versionCode)
    }

    @Test
    fun `the universal asset wins over an architecture specific one`() {
        // The release publishes both, and the universal one is the only one that
        // runs on every device, so it is the one that must be offered.
        val release = """
            {
              "tag_name": "v1.0.42",
              "body": "on run 143.",
              "assets": [
                {"name": "kurdish-tv-arm64-v8a.apk", "browser_download_url": "https://x/arm64.apk", "size": 100},
                {"name": "kurdish-tv-universal.apk", "browser_download_url": "https://x/universal.apk", "size": 200}
              ]
            }
        """.trimIndent()

        assertEquals("https://x/universal.apk", UpdateManifestParser.parse(release)!!.downloadUrl)
    }

    @Test
    fun `the run number in the release body is preferred over the tag`() {
        // The tag says 1.0.42, the body says build 143. Only the second is on the
        // same scale as the APK's VERSION_CODE, so it is the one that decides.
        val release = """
            {
              "tag_name": "v1.0.42",
              "body": "Built from commit abc123456 on run 143.",
              "assets": [{"name": "a.apk", "browser_download_url": "https://x/a.apk", "size": 1}]
            }
        """.trimIndent()

        val update = UpdateManifestParser.parse(release)!!
        assertEquals(143, update.versionCode)
        // The tag's own patch number is 42 — a number that would compare as
        // "older" than almost any real build and hide the update entirely.
        assertEquals(42, UpdateManifestParser.parseVersionCode("v1.0.42"))
    }

    @Test
    fun `a release with no run number in its body falls back to the tag`() {
        val release = """
            {
              "tag_name": "v1.0.42",
              "body": "No build number here.",
              "assets": [{"name": "a.apk", "browser_download_url": "https://x/a.apk", "size": 1}]
            }
        """.trimIndent()

        assertEquals(42, UpdateManifestParser.parse(release)!!.versionCode)
    }

    @Test
    fun `the run number is recovered from every shape the release body uses`() {
        // The exact form the release job generates, with the backticks it wraps
        // the number in...
        assertEquals(36, UpdateManifestParser.runNumberFrom("... *on run* " + TICK + "36" + TICK + "."))
        // ...and the rewordings a human editing the body would reach for, each of
        // which used to fail to match and so hid the update without any error.
        assertEquals(36, UpdateManifestParser.runNumberFrom("... on run 36."))
        assertEquals(36, UpdateManifestParser.runNumberFrom("... on run *36*."))
        assertEquals(36, UpdateManifestParser.runNumberFrom("... on run " + TICK + "36"))
        assertEquals(7, UpdateManifestParser.runNumberFrom("on run 7"))
    }

    @Test
    fun `a body without a run number reports none`() {
        // Not zero and not a guess: the caller treats a null as "unverified" and
        // stays quiet rather than offering an update it cannot prove is newer.
        assertNull(UpdateManifestParser.runNumberFrom("no number in this body"))
        assertNull(UpdateManifestParser.runNumberFrom(""))
        assertNull(UpdateManifestParser.runNumberFrom(null))
    }

    @Test
    fun `a json document that is neither shape yields nothing`() {
        // Better to offer no update than to offer one whose fields we invented.
        assertNull(UpdateManifestParser.parse("""{"unrelated": true}"""))
    }

    @Test
    fun `a manifest with no version_code falls back to the version string`() {
        val noCode = """
            {
              "version": "1.0.9",
              "download_url": "https://x/a.apk"
            }
        """.trimIndent()

        assertEquals(9, UpdateManifestParser.parse(noCode)!!.versionCode)
    }

    @Test
    fun `a manifest with no download url yields nothing`() {
        assertNull(UpdateManifestParser.parse("""{"version": "1.0.41", "version_code": 142}"""))
    }

    @Test
    fun `a release with no apk asset yields nothing`() {
        val release = """
            {
              "tag_name": "v1.0.42",
              "body": "on run 143.",
              "assets": [{"name": "checksums.txt", "browser_download_url": "https://x/sums", "size": 1}]
            }
        """.trimIndent()

        assertNull(UpdateManifestParser.parse(release))
    }

    @Test
    fun `newness is decided on the number, not on the string`() {
        fun update(code: Int) = AppUpdate(
            versionName = "1.0.$code",
            versionCode = code,
            releaseUrl = "",
            downloadUrl = "",
            sizeBytes = 0,
            publishedAt = null,
            notes = null
        )

        // The whole reason the comparison is numeric: "1.0.9" sorts *after*
        // "1.0.10" as text, which would hide a real update.
        assertTrue(update(10).isNewerThan(9))
        assertTrue(update(143).isNewerThan(142))

        assertFalse(update(142).isNewerThan(142))
        assertFalse(update(141).isNewerThan(142))
    }

    private companion object {
        /** The release body wraps the run number in backticks. */
        const val TICK = "`"
    }
}
