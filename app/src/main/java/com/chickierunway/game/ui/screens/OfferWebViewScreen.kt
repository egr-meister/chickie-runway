package com.chickierunway.game.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.chickierunway.game.integration.HttpOfferProbe
import com.chickierunway.game.integration.OfferWebViewClient

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfferWebViewScreen(url: String) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Live WebView, so back navigation can consult its history.
    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    // Load-once flag keyed on the URL. Comparing with webView.url instead would drag the user
    // back to the landing page on every recomposition after they navigate.
    val loaded = remember(url) { booleanArrayOf(false) }

    BackHandler {
        val webView = webViewRef[0]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            // No game UI behind the offer to fall back to — leave instead of trapping the user.
            activity?.finish()
        }
    }

    AndroidView(
        // The activity is edge-to-edge; keep the page out from under the system bars.
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        factory = { ctx ->
            WebView(ctx).apply {
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    javaScriptCanOpenWindowsAutomatically = true
                    // Offer pages routinely pull http assets into an https shell.
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                // Must happen before the first loadUrl, or that request leaves without it.
                enableRequestedWithHeader(settings, url)
                webChromeClient = WebChromeClient()
                webViewClient = OfferWebViewClient(ctx)
                webViewRef[0] = this
            }
        },
        update = { webView ->
            webViewRef[0] = webView
            if (!loaded[0]) {
                loaded[0] = true
                // Belt and braces for WebView builds that drop the allow-list API: covers the
                // main-frame request only and does not survive redirects.
                webView.loadUrl(
                    url,
                    mapOf(HttpOfferProbe.REQUESTED_WITH_HEADER to webView.context.packageName)
                )
            }
        },
        onRelease = { webView ->
            // Ad-heavy pages: without this JS timers and media outlive the screen.
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
            webViewRef[0] = null
        }
    )
}

/**
 * Since WebView M108 X-Requested-With is no longer sent by default; androidx.webkit's origin
 * allow-list brings it back. "*" covers every origin in the redirect chain plus subresources
 * (XHR, iframes). The API is marked temporary, hence the guards and the loadUrl header above.
 */
private fun enableRequestedWithHeader(settings: WebSettings, url: String) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) return
    try {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, setOf("*"))
    } catch (e: IllegalArgumentException) {
        // This WebView build rejects the wildcard rule - fall back to the start URL's origin.
        Log.w("OfferWebViewScreen", "Wildcard X-Requested-With rule rejected", e)
        originOf(url)?.let { origin ->
            runCatching { WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, setOf(origin)) }
                .onFailure { Log.w("OfferWebViewScreen", "Origin X-Requested-With rule rejected", it) }
        }
    } catch (e: UnsupportedOperationException) {
        Log.w("OfferWebViewScreen", "X-Requested-With allow-list unsupported", e)
    }
}

/** scheme://host[:port], or null when the URL has none. */
private fun originOf(url: String): String? {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd <= 0) return null
    val hostEnd = url.indexOfAny(charArrayOf('/', '?', '#'), schemeEnd + 3).let { if (it < 0) url.length else it }
    return url.substring(0, hostEnd).takeIf { hostEnd > schemeEnd + 3 }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
