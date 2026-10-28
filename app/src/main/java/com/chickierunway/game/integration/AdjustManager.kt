package com.chickierunway.game.integration

import android.app.Application
import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.chickierunway.game.BuildConfig

/**
 * Adjust SDK v5. Initialised for every install without exception — there is no attribution
 * holdback. Differences from v4 worth noting: init is `Adjust.initSdk(config)`, not
 * `Adjust.onCreate`, and Activity lifecycle callbacks are gone (sessions count themselves).
 */
object AdjustManager {

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        val token = BuildConfig.ADJUST_APP_TOKEN
        if (token.isBlank()) return   // build without a tracker: let White work

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

        // adid is not ready synchronously; the callback delivers it.
        Adjust.getAdid { adid -> IntegrationStorage.adid = adid }
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
