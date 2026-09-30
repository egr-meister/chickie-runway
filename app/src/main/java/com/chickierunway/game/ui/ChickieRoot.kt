package com.chickierunway.game.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chickierunway.game.ChickieApp
import com.chickierunway.game.integration.AppsFlyerManager
import com.chickierunway.game.integration.DeepLinkRouter
import com.chickierunway.game.integration.IntegrationStorage
import com.chickierunway.game.integration.OneSignalManager
import com.chickierunway.game.integration.RouteDecision
import com.chickierunway.game.integration.TrafficRouter
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.navigation.AppNavHost
import com.chickierunway.game.ui.screens.OfferWebViewScreen
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/** Decides between the game (White) and the offer (Black); a push deep link outranks both. */
@Composable
fun ChickieRoot(app: ChickieApp) {
    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var routeGate by remember { mutableStateOf<RouteDecision?>(null) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    LaunchedEffect(Unit) {
        // On a first launch the offer URL is worthless until conversion data lands — wait,
        // but bounded: AppsFlyer may never answer, and an endless splash is worse.
        if (!IntegrationStorage.hasRouteDecision) {
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    AppsFlyerManager.setOnAttributionResolved {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    continuation.invokeOnCancellation {
                        AppsFlyerManager.setOnAttributionResolved(null)
                    }
                }
            }
            AppsFlyerManager.setOnAttributionResolved(null)
        }
        // TrafficRouter reads the full stored AppsFlyer map itself when building the URL.
        TrafficRouter.route { decision -> routeGate = decision }
    }

    val isBlack = deepLinkUrl != null || routeGate is RouteDecision.Black
    LaunchedEffect(isBlack) {
        // Push permission is asked on the offer side only; the game itself stays prompt-free.
        if (isBlack) OneSignalManager.requestPermission()
    }

    val currentDeepLink = deepLinkUrl
    val gate = routeGate
    when {
        currentDeepLink != null -> OfferWebViewScreen(currentDeepLink)
        gate is RouteDecision.Black -> OfferWebViewScreen(gate.url)
        gate is RouteDecision.White -> AppNavHost(app = app)   // starts at MainRoute
        else -> LoadingContent()
    }
}

@Composable
private fun LoadingContent() {
    GameBackground {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center).size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
