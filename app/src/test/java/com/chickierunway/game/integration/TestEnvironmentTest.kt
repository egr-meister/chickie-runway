package com.chickierunway.game.integration

import com.chickierunway.game.integration.TestEnvironment.Snapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestEnvironmentTest {

    private val pixel = Snapshot(
        hardware = "tensor",
        product = "husky",
        model = "Pixel 8 Pro",
        manufacturer = "Google",
        brand = "google",
        device = "husky",
        fingerprint = "google/husky/husky:15/AP4A/123:user/release-keys",
    )

    @Test
    fun `a retail phone is not a test environment`() {
        assertFalse(TestEnvironment.isTestEnvironment(pixel))
    }

    @Test
    fun `test-keys alone is not a test environment`() {
        val customRom = pixel.copy(fingerprint = "lineage/husky/husky:15/AP4A/1:userdebug/test-keys")
        assertFalse(TestEnvironment.isTestEnvironment(customRom))
    }

    @Test
    fun `current AVD is an emulator`() {
        val avd = pixel.copy(hardware = "ranchu", product = "sdk_gphone64_arm64", model = "sdk_gphone64_arm64")
        assertTrue(TestEnvironment.isEmulator(avd))
    }

    @Test
    fun `sdk-like real product name is not an emulator`() {
        assertFalse(TestEnvironment.isEmulator(pixel.copy(product = "sdkfusion")))
    }

    @Test
    fun `a single generic marker is not enough`() {
        assertFalse(TestEnvironment.isEmulator(pixel.copy(brand = "generic")))
        assertTrue(TestEnvironment.isEmulator(pixel.copy(brand = "generic", device = "generic_x86")))
    }

    @Test
    fun `test lab, instrumentation, debugger and debuggable all count`() {
        assertTrue(TestEnvironment.isTestEnvironment(pixel.copy(firebaseTestLab = "true")))
        assertTrue(TestEnvironment.isTestEnvironment(pixel.copy(instrumentationPresent = true)))
        assertTrue(TestEnvironment.isTestEnvironment(pixel.copy(debuggerConnected = true)))
        assertTrue(TestEnvironment.isTestEnvironment(pixel.copy(debuggable = true)))
    }
}
