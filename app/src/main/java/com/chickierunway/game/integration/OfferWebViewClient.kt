package com.chickierunway.game.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Turns the WebView opening the campaign link into the single click that decides White/Black.
 *
 * Two phases, split by the first real page paint:
 *  - before the first paint the main-frame response is the tracker's verdict (may pin White);
 *  - after it the branch is settled, and a later main-frame error is only a dead page inside the
 *    offer — the user gets a retry screen, never a re-decided branch.
 *
 * Each outcome is delivered exactly once. The caller wires the callbacks to match the branch: a
 * fresh campaign link may pin White, a cached address may only be rebuilt.
 *
 * @param fromCache true when [url] is a pinned cached address (a returning user), false when it is
 *   a freshly built campaign link whose 404 is the tracker's White verdict.
 * @param onWhiteVerdict main-frame 404 on a campaign link — lock White forever.
 * @param onOfferCommitted the first real page painted — pin the address and drop the splash.
 * @param onDeadOffer the address did not open before any paint — rebuild a cached one, or fall
 *   back to White for a campaign link whose load failed (pinning nothing).
 * @param onRecoverableError a main-frame error AFTER the offer was already showing — show a retry.
 */
class OfferWebViewClient(
    private val context: Context,
    private val fromCache: Boolean,
    private val onWhiteVerdict: () -> Unit,
    private val onOfferCommitted: (String) -> Unit,
    private val onDeadOffer: () -> Unit,
    private val onRecoverableError: () -> Unit,
) : WebViewClient() {

    // The branch verdict is delivered once; a later 404 on a dead link inside the offer must not
    // re-decide it.
    private var resolved = false

    // Set once a real page has painted: after this the offer is on screen and errors are recoverable.
    private var committed = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (!UrlSchemes.isExternal(url)) return false
        return openExternal(url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        // The first actually rendered page, not every onPageStarted: caching on start would pin
        // about:blank and intermediate redirects.
        val scheme = url?.let { UrlSchemes.schemeOf(it) }
        if (scheme == "http" || scheme == "https") {
            resolved = true
            committed = true
            onOfferCommitted(url)
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        // Lands and payment iframes answer 404 for images and scripts all the time; only the main
        // frame is the tracker's answer about this install.
        if (request?.isForMainFrame != true) return
        if (committed) {
            onRecoverableError()
            return
        }
        if (resolved) return
        val status = errorResponse?.statusCode ?: return

        if (fromCache) {
            // A dead cached address is a page problem, not a branch verdict: rebuild, do not lock.
            resolved = true
            onDeadOffer()
            return
        }
        if (RoutingRules.isWhite(status)) {
            resolved = true
            onWhiteVerdict()
        }
        // A non-404 on the campaign link (403, 5xx) means the offer exists but the server glitched —
        // leave it Black and let the WebView show or retry the page.
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return
        val failing = request.url?.toString().orEmpty()
        // Offers redirect to market:// and intent:// constantly; those are handled in
        // shouldOverrideUrlLoading, and an error screen over a working flow is itself the bug.
        if (failing.startsWith("about:") || UrlSchemes.isExternal(failing)) return

        if (committed) {
            onRecoverableError()
            return
        }
        if (resolved) return
        // A network/TLS/DNS error is not the tracker's answer: nothing is pinned here. For a cached
        // address the caller rebuilds the campaign link; for the campaign link itself the caller
        // shows White for this launch only and asks again next time.
        resolved = true
        onDeadOffer()
    }

    private fun openExternal(url: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: ActivityNotFoundException) {
        // Nothing handles this scheme; swallowing it beats an ugly WebView error page.
        Log.w("OfferWebViewClient", "No activity for $url", e)
        true
    }
}
