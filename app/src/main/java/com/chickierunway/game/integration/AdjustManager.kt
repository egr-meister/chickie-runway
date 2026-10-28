package com.chickierunway.game.integration

import android.app.Application
import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.chickierunway.game.BuildConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Adjust SDK v5. Initialised for every install without exception — there is no attribution
 * holdback. Differences from v4 worth noting: init is `Adjust.initSdk(config)`, not
 * `Adjust.onCreate`, and Activity lifecycle callbacks are gone (sessions count themselves).
 */
object AdjustManager {

    private const val ADID_AWAIT_MS = 3_000L

    private lateinit var appContext: Context

    // The adid arrives asynchronously with the first session response. Without a latch the
    // offer URL of a first install is built before it lands and the single click goes out
    // with no adid.
    private val adidReady = CountDownLatch(1)

    fun init(app: Application) {
        appContext = app.applicationContext

        val token = BuildConfig.ADJUST_APP_TOKEN
        if (token.isBlank()) {
            // Build without a tracker: let White work, and do not stall URL building.
            adidReady.countDown()
            return
        }

        val environment =
            if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX
            else AdjustConfig.ENVIRONMENT_PRODUCTION

        val config = AdjustConfig(app, token, environment)
        config.setLogLevel(if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN)

        // Set BEFORE initSdk, or the first attribution passes the listener by.
        config.setOnAttributionChangedListener { attribution ->
            attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
        }

        Adjust.initSdk(config)

        // adid is not ready synchronously; the callback delivers it. A stored one is the answer
        // at once, so later launches never wait.
        if (!IntegrationStorage.adid.isNullOrBlank()) adidReady.countDown()
        Adjust.getAdid { adid ->
            IntegrationStorage.adid = adid
            adidReady.countDown()
        }
    }

    /**
     * Waits a bounded time for the adid, then returns whatever is stored (empty if it never
     * arrived). Background threads only — it blocks.
     */
    fun awaitAdid(): String {
        if (!::appContext.isInitialized || BuildConfig.ADJUST_APP_TOKEN.isBlank()) {
            return IntegrationStorage.adid.orEmpty()
        }
        runCatching { adidReady.await(ADID_AWAIT_MS, TimeUnit.MILLISECONDS) }
        return IntegrationStorage.adid.orEmpty()
    }

    /**
     * Waits a bounded time for attribution. getAttributionWithTimeout returns null itself if the
     * data does not arrive in time, so no CountDownLatch of our own is needed.
     */
    fun awaitAttribution(timeoutMs: Long, onDone: () -> Unit) {
        if (!::appContext.isInitialized || BuildConfig.ADJUST_APP_TOKEN.isBlank()) {
            onDone()
            return
        }
        Adjust.getAttributionWithTimeout(appContext, timeoutMs) { attribution ->
            attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
            onDone()
        }
    }

    /**
     * AdjustAttribution is a typed object, not a map. Lay it out by field and keep everything
     * non-null; a missing field arrives as null and "null" is a four-char string, not an absence.
     * An empty string, on the other hand, is kept: "sent empty" and "not sent" differ to the offer.
     */
    private fun AdjustAttribution.toMap(): Map<String, String?> = linkedMapOf(
        "tracker_token" to trackerToken,
        "tracker_name" to trackerName,
        "network" to network,
        "campaign" to campaign,
        "adgroup" to adgroup,
        "creative" to creative,
        "click_label" to clickLabel,
    )
}
