package com.chickierunway.game.integration

/**
 * sub14: is this install running somewhere that is not a real user's phone. It describes the
 * ENVIRONMENT, never a setting the user once switched on. Deliberately absent:
 *  - Settings.Global.ADB_ENABLED — true for anyone who ever opened developer options;
 *  - fingerprint "test-keys" — every AOSP-based ROM and much grey-market retail ships it.
 * Both would mark real paying users as test traffic.
 */
object TestEnvironment {

    data class Snapshot(
        val firebaseTestLab: String? = null,
        val instrumentationPresent: Boolean = false,
        val debuggerConnected: Boolean = false,
        val debuggable: Boolean = false,
        val hardware: String = "",
        val product: String = "",
        val model: String = "",
        val manufacturer: String = "",
        val brand: String = "",
        val device: String = "",
        val fingerprint: String = "",
    )

    private val HARDWARE_MARKERS = listOf(
        "goldfish", "ranchu", "vbox", "android_x86", "gce_x86", "cutf", "cuttlefish",
    )

    // Without the trailing underscore - "<marker>_" is applied at the comparison. "sdk" is
    // what catches the current AVD's "sdk_gphone64_arm64"; do not drop it as "too broad".
    private val PRODUCT_MARKERS = listOf(
        "sdk", "google_sdk", "sdk_gphone", "sdk_x86", "vbox86p", "emulator", "simulator",
    )

    private val MODEL_MARKERS = listOf(
        "google_sdk", "emulator", "android sdk built for", "sdk_gphone",
    )

    fun isTestEnvironment(s: Snapshot): Boolean =
        s.firebaseTestLab == "true" ||
            s.instrumentationPresent ||
            s.debuggerConnected ||
            s.debuggable ||
            isEmulator(s)

    fun isEmulator(s: Snapshot): Boolean {
        val hardware = s.hardware.lowercase()
        val product = s.product.lowercase()
        val model = s.model.lowercase()
        val fingerprint = s.fingerprint.lowercase()

        if (HARDWARE_MARKERS.any { hardware.contains(it) }) return true

        // Equality or "<marker>_" prefix, never a bare prefix: a real "sdkfusion" product
        // would otherwise be written off as an emulator.
        if (PRODUCT_MARKERS.any { product == it || product.startsWith("${it}_") }) return true

        if (MODEL_MARKERS.any { model.contains(it) }) return true
        if (s.manufacturer.lowercase().contains("genymotion")) return true

        if (fingerprint.startsWith("generic") || fingerprint.contains("/sdk_")) return true

        // Both, not either - a single "generic" is common on real hardware.
        if (s.brand.lowercase().startsWith("generic") && s.device.lowercase().startsWith("generic")) {
            return true
        }

        return false
    }
}
