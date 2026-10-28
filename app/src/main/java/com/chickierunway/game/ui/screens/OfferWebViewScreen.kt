package com.chickierunway.game.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.chickierunway.game.integration.OfferWebViewClient
import com.chickierunway.game.integration.UrlSchemes

private const val REQUESTED_WITH_HEADER = "X-Requested-With"

/**
 * The offer WebView. All five callbacks default to no-ops so a push deep link, which must not
 * re-decide or re-pin anything, can be opened by passing only the url.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfferWebViewScreen(
    url: String,
    fromCache: Boolean = true,
    onWhiteVerdict: () -> Unit = {},
    onOfferCommitted: (String) -> Unit = {},
    onDeadOffer: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Live WebView, so back navigation can consult its history.
    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    // Load-once flag keyed on the URL. Comparing with webView.url instead would drag the user
    // back to the landing page on every recomposition after they navigate.
    val loaded = remember(url) { booleanArrayOf(false) }
    var showError by remember(url) { mutableStateOf(false) }

    // File chooser: the callback must be answered exactly once in every outcome, or the page's
    // file input dies for good.
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        callback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
    }

    // WebView flushes cookies lazily; without this the click cookie and session do not survive to
    // the next launch and the user comes back a brand-new visitor.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching { CookieManager.getInstance().flush() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Never leave a file-chooser request hanging when the screen goes away.
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

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
                // Third-party cookies are blocked by default; pixels, payment iframes and partner
                // redirects all depend on them.
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                // Must happen before the first loadUrl, or that request leaves without it.
                enableRequestedWithHeader(settings, url)

                webViewClient = OfferWebViewClient(
                    context = ctx,
                    fromCache = fromCache,
                    onWhiteVerdict = onWhiteVerdict,
                    onOfferCommitted = { committed ->
                        showError = false
                        onOfferCommitted(committed)
                    },
                    onDeadOffer = onDeadOffer,
                    onRecoverableError = { showError = true },
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        callback: ValueCallback<Array<Uri>>?,
                        params: FileChooserParams?,
                    ): Boolean {
                        if (callback == null) return false
                        // Drop any previous unanswered callback before replacing it.
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        val intent = params?.createIntent()
                        if (intent == null) {
                            filePathCallback = null
                            callback.onReceiveValue(null)
                            return false
                        }
                        return try {
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (e: ActivityNotFoundException) {
                            Log.w("OfferWebViewScreen", "No file chooser activity", e)
                            filePathCallback = null
                            callback.onReceiveValue(null)
                            true
                        }
                    }
                }

                // WebView cannot download anything itself; hand http(s) downloads to the system.
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    val scheme = UrlSchemes.schemeOf(downloadUrl)
                    if (scheme == "http" || scheme == "https") {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }

                webViewRef[0] = this
            }
        },
        update = { webView ->
            webViewRef[0] = webView
            if (!loaded[0]) {
                loaded[0] = true
                // Belt and braces for WebView builds that drop the allow-list API: covers the
                // main-frame request only and does not survive redirects.
                webView.loadUrl(url, mapOf(REQUESTED_WITH_HEADER to webView.context.packageName))
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

    if (showError) {
        OfferErrorScreen(
            onRetry = {
                showError = false
                webViewRef[0]?.loadUrl(url, mapOf(REQUESTED_WITH_HEADER to (webViewRef[0]?.context?.packageName ?: "")))
            }
        )
    }
}

@Composable
private fun OfferErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.page_load_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
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
