package com.example

import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.DeviceMode
import com.example.kurdishtv.ui.screens.deviceModeHint
import com.example.ui.theme.resolveIsTv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The override in front of the ten-foot/handheld decision.
 *
 * This exists because `uiMode` is not a reliable answer. Android TV boxes, projectors
 * and HDMI sticks that declare a phone or tablet `uiMode` were being handed the
 * compact touch layout — the one device in the house with the most pixels to spend
 * got the thumbnail grid — and there was nothing a viewer could do about it except
 * uninstall. The fix has to be a real override rather than a second code path,
 * because a second code path is a second thing to keep correct; here the entire app
 * still branches on one boolean, and this decides what it is.
 *
 * The rule is one `when`, which is exactly the kind of thing that gets "tidied" into
 * something that silently lets the device override the viewer again. So the case that
 * matters most is the one where the two disagree.
 */
class DeviceModeTest {

    @Test
    fun `auto follows the device`() {
        assertTrue(DeviceMode.AUTO.resolveIsTv(detected = true))
        assertFalse(DeviceMode.AUTO.resolveIsTv(detected = false))
    }

    @Test
    fun `a forced mode is the same answer whatever the device says`() {
        // The case the setting exists for. If detection were consulted here the
        // control would look live and change nothing on screen for exactly the
        // viewer who went looking for it.
        for (detected in listOf(true, false)) {
            assertTrue(DeviceMode.TV.resolveIsTv(detected))
            assertFalse(DeviceMode.MOBILE.resolveIsTv(detected))
        }
    }

    @Test
    fun `the two layouts are actually different answers`() {
        // Otherwise the override is a no-op and every other test here still passes.
        assertNotEquals(
            DeviceMode.TV.resolveIsTv(detected = false),
            DeviceMode.MOBILE.resolveIsTv(detected = false)
        )
    }

    @Test
    fun `auto is the default so an existing install is unchanged`() {
        // Upgrading must not move anyone's layout. Anything else here would silently
        // re-lay-out every install that has never opened Settings.
        assertEquals(DeviceMode.AUTO, AppSettings().deviceMode)
    }

    @Test
    fun `the hint says what auto resolved to`() {
        // The row is a three-way switch with no readout, so on a misreporting box the
        // viewer cannot tell whether "Auto" picked wrong or whether their override did
        // not take. The hint is the only place that becomes visible.
        assertEquals(
            "Auto — this device reports itself as a TV, so the ten-foot layout is in use.",
            deviceModeHint(DeviceMode.AUTO, detectedIsTv = true)
        )
        assertEquals(
            "Auto — this device reports itself as a phone or tablet, so the handheld layout is in use.",
            deviceModeHint(DeviceMode.AUTO, detectedIsTv = false)
        )
    }

    @Test
    fun `the hint says a forced mode overrides the device`() {
        assertEquals(
            "TV — the room-sized layout is forced, whatever the device reports.",
            deviceModeHint(DeviceMode.TV, detectedIsTv = false)
        )
        assertEquals(
            "Phone — the compact touch layout is forced, whatever the device reports.",
            deviceModeHint(DeviceMode.MOBILE, detectedIsTv = true)
        )
    }

    @Test
    fun `every mode has a hint and a label, so none of them is nameless`() {
        // A new entry added without a display name reaches the UI as the constant
        // name, and nothing else in the build notices.
        for (mode in DeviceMode.entries) {
            assertTrue("${mode.name} has no label", mode.displayName.isNotBlank())
            assertTrue(
                "${mode.name} has no hint",
                deviceModeHint(mode, detectedIsTv = true).isNotBlank()
            )
        }
    }
}
