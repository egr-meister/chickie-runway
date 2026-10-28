package com.chickierunway.game.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingRulesTest {

    @Test
    fun `404 is White`() {
        assertTrue(RoutingRules.isWhite(404))
    }

    @Test
    fun `everything else is not White`() {
        // 403 (WAF) and 5xx (server glitch) are deliberately Black: they are not the tracker's answer.
        listOf(200, 204, 301, 302, 403, 410, 500, 503).forEach { code ->
            assertFalse("code $code should not be White", RoutingRules.isWhite(code))
        }
    }
}

class OfferCacheTest {

    private val ttl = OfferCache.OFFER_TTL_MS

    @Test
    fun `a record with no timestamp has expired`() {
        assertFalse(OfferCache.isFresh(savedAt = 0L, now = 1_000_000L, ttlMs = ttl))
    }

    @Test
    fun `a clock that moved back does not grant an eternal cache`() {
        assertFalse(OfferCache.isFresh(savedAt = 2_000L, now = 1_000L, ttlMs = ttl))
    }

    @Test
    fun `fresh inside the window, stale on and after the boundary`() {
        val savedAt = 10_000L
        assertTrue(OfferCache.isFresh(savedAt, savedAt + ttl - 1, ttl))
        assertFalse(OfferCache.isFresh(savedAt, savedAt + ttl, ttl))
        assertFalse(OfferCache.isFresh(savedAt, savedAt + ttl + 1, ttl))
    }
}
