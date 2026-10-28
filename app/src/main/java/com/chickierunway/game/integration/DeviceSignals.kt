package com.chickierunway.game.integration

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log

/** Platform access for sub12 and sub14. Initialised once from Application.onCreate. */
object DeviceSignals {

    private const val TAG = "DeviceSignals"

    private val INSTRUMENTATION_CLASSES = listOf(
        "androidx.test.platform.app.InstrumentationRegistry",
        "androidx.test.espresso.Espresso",
        "org.robolectric.Robolectric",
    )

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** sub12. Read on each access - the charge moves and the read is cheap. */
    val battery: String
        get() = runCatching {
            // A null receiver just returns the sticky broadcast: registers nothing, needs no
            // permission, and avoids Android 14's RECEIVER_EXPORTED requirement.
            val intent = appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ) ?: return@runCatching unknownBattery()

            BatteryState.describe(
                plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryState.STATUS_UNKNOWN),
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
            )
        }.getOrElse {
            Log.w(TAG, "Could not read the battery", it)
            // Still a value: "no reading" must stay distinguishable from "no signal".
            unknownBattery()
        }

    /**
     * sub14, computed once per process. A debugger can attach mid-session, but sub14
     * flipping between two URL builds in one run would be worse than a stale answer.
     */
    val isTestEnvironment: Boolean by lazy {
        runCatching { TestEnvironment.isTestEnvironment(snapshot()) }.getOrElse {
            // Runs on the URL-building thread; an escaping exception would kill routing.
            Log.w(TAG, "Could not build the test-environment snapshot", it)
            false
        }
    }

    private fun unknownBattery(): String =
        BatteryState.describe(plugged = 0, status = BatteryState.STATUS_UNKNOWN, level = -1, scale = -1)

    private fun snapshot(): TestEnvironment.Snapshot {
        val resolver = appContext.contentResolver
        return TestEnvironment.Snapshot(
            // Settings.Global reads need no permission - only writes do.
            firebaseTestLab = safe("firebase.test.lab") {
                Settings.Global.getString(resolver, "firebase.test.lab")
            },
            instrumentationPresent = INSTRUMENTATION_CLASSES.any(::isOnClasspath),
            debuggerConnected = safe("debugger") { Debug.isDebuggerConnected() } ?: false,
            debuggable = safe("debuggable") {
                (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } ?: false,
            hardware = Build.HARDWARE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            fingerprint = Build.FINGERPRINT.orEmpty(),
        )
    }

    /** A ROM that throws on one lookup must not take the whole signal down. */
    private fun <T> safe(what: String, block: () -> T): T? =
        runCatching(block)
            .onFailure { Log.w(TAG, "Could not read $what", it) }
            .getOrNull()

    /**
     * initialize = false: running a test class's static initializer could throw and report
     * it absent while it is present.
     */
    private fun isOnClasspath(className: String): Boolean =
        runCatching { Class.forName(className, false, DeviceSignals::class.java.classLoader) }.isSuccess
}
