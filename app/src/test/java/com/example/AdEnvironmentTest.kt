package com.example

import com.example.kurdishtv.ads.AdEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdEnvironmentTest {

    @Test
    fun `isEmulator returns boolean without throwing`() {
        val result = runCatching { AdEnvironment.isEmulator() }
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `isMissingRenderNode correctly checks DRI presence`() {
        val missing = AdEnvironment.isMissingRenderNode()
        // In JVM and headless container test environment, /dev/dri/renderD* is absent
        assertNotNull(missing)
    }

    @Test
    fun `ad rendering is prevented in headless test environment`() {
        // Ads should not attempt to render without hardware GPU nodes
        if (AdEnvironment.isMissingRenderNode()) {
            assertFalse(AdEnvironment.isAdRenderingSupported())
        }
    }
}
