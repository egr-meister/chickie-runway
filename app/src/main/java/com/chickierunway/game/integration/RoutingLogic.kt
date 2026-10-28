package com.chickierunway.game.integration

import java.util.concurrent.TimeUnit

sealed interface RouteDecision {
    data object White : RouteDecision

    /**
     * Show the offer. [fromCache] = true means a still-fresh pinned address (a returning user);
     * false means a freshly built campaign link whose main-frame response is still the tracker's
     * verdict and may pin White.
     */
    data class Black(val url: String, val fromCache: Boolean) : RouteDecision
}

object RoutingRules {

    const val NOT_FOUND = 404

    /**
     * 404 is the agreed "this install gets the app" answer. Anything else means the offer exists:
     * 200, a redirect chain that ended somewhere, even a 5xx server glitch the WebView may retry.
     *
     * 403 is deliberately NOT White: it is Cloudflare or a WAF, not a decision by the tracker.
     * Reading it as White would write off an install over someone else's error — and irreversibly,
     * because the White verdict can never be undone.
     */
    fun isWhite(mainFrameStatus: Int): Boolean = mainFrameStatus == NOT_FOUND
}

object OfferCache {

    val OFFER_TTL_MS: Long = TimeUnit.HOURS.toMillis(24)

    fun isFresh(savedAt: Long, now: Long, ttlMs: Long = OFFER_TTL_MS): Boolean {
        // 0 is also a record written before timestamps existed: treat as expired.
        if (savedAt <= 0L) return false
        val age = now - savedAt
        // Negative age means the device clock moved back. Rebuilding the link is cheaper than
        // leaving the install on an eternal cache.
        if (age < 0L) return false
        return age < ttlMs
    }
}
