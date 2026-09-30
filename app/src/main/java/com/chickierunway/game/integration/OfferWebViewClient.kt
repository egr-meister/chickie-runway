package com.chickierunway.game.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class OfferWebViewClient(private val context: Context) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Remember where the user is, so a relaunch resumes the offer flow. Only real pages:
        // WebView reports about:blank routinely, and caching it would pin a blank screen.
        val scheme = url?.let { UrlSchemes.schemeOf(it) }
        if (scheme == "http" || scheme == "https") {
            IntegrationStorage.cachedOfferUrl = url
        }
    }

    private fun handleUrl(url: String): Boolean {
        if (!UrlSchemes.isExternal(url)) return false
        return try {
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
}
