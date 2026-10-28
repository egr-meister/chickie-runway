package com.chickierunway.game.integration

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google Advertising ID, sent as `gps_adid`. It is NOT the same id as Adjust's `adid`: GAID is the
 * main key Adjust S2S postbacks use on Android to match a conversion to its install, so it must be
 * in the single click. Reading it is blocking and goes through Play Services, hence it starts in
 * Application.onCreate on its own thread with its own latch, like PushManager and AccelerometerProbe.
 */
object GaidProbe {

    private const val AWAIT_TIMEOUT_SECONDS = 3L
    private const val ZEROED = "00000000-0000-0000-0000-000000000000"

    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    /** Called from Application.onCreate: the read is blocking, so begin early. */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        // A GAID read on an earlier launch is the answer immediately.
        if (!IntegrationStorage.gpsAdid.isNullOrBlank()) {
            ready.countDown()
            return
        }
        val app = context.applicationContext
        Thread {
            runCatching {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(app)
                val id = info.id
                // Limit Ad Tracking or a zeroed id is not a key: store nothing, and the parameter
                // still goes out empty (so "no GAID" is distinguishable from "did not make it").
                if (!info.isLimitAdTrackingEnabled && !id.isNullOrBlank() && id != ZEROED) {
                    IntegrationStorage.gpsAdid = id
                }
            }
            // Open the latch on failure too: there is no point waiting for a GAID that is not coming.
            ready.countDown()
        }.start()
    }

    /** Background thread only: blocks up to three seconds. */
    fun await(): String {
        if (!started.get()) return IntegrationStorage.gpsAdid.orEmpty()
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        return IntegrationStorage.gpsAdid.orEmpty()
    }
}
