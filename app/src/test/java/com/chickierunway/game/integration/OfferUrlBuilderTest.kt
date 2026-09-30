package com.chickierunway.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferUrlBuilderTest {

    private val campaign = "android1_AR_sub3%26_sub4_WBZ_sub6"

    private val params = linkedMapOf(
        "af_status" to "Non-organic",
        "media_source" to "facebook",
        "campaign" to campaign
    )

    @Test
    fun `builds full url with sub1 in path and every param in query`() {
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", params, "test-af-id")

        assertEquals(
            "https://secret-joker.cfd/android1?af_status=Non-organic&media_source=facebook" +
                "&campaign=android1_AR_sub3%26_sub4_WBZ_sub6&sub1=android1&sub2=AR&sub3=sub3%26" +
                "&sub4=sub4&sub5=WBZ&sub6=sub6&appsflyer_id=test-af-id",
            url
        )
        assertTrue(url.startsWith("https://secret-joker.cfd/android1?"))
        assertTrue(url.contains("af_status=Non-organic"))
        assertTrue(url.contains("media_source=facebook"))
        assertTrue(url.contains("campaign=$campaign"))
        assertTrue(url.contains("&sub1=android1"))
        listOf("sub2=AR", "sub3=sub3%26", "sub4=sub4", "sub5=WBZ", "sub6=sub6").forEach {
            assertTrue("missing $it", url.contains(it))
        }
        assertTrue(url.contains("appsflyer_id=test-af-id"))
        assertFalse(url.contains("%2526"))
    }

    @Test
    fun `never drops an unknown appsflyer param`() {
        val extra = LinkedHashMap(params).apply {
            put("adset", "set 1")
            put("is_first_launch", "true")
            put("empty_one", "")
        }
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", extra, null)
        assertTrue(url.contains("adset=set%201"))
        assertTrue(url.contains("is_first_launch=true"))
        assertTrue(url.contains("empty_one="))
        assertTrue(url.contains("campaign=$campaign"))
        assertFalse(url.contains("appsflyer_id"))
    }

    @Test
    fun `parsed subs replace appsflyer subs without duplicating keys`() {
        val withSub = LinkedHashMap(params).apply { put("sub1", "stale") }
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", withSub, "id")
        assertEquals(1, Regex("[?&]sub1=").findAll(url).count())
        assertTrue(url.contains("sub1=android1"))
    }

    @Test
    fun `no campaign means no extra path segment`() {
        val noCampaign = linkedMapOf("af_status" to "Organic", "media_source" to "organic")
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", noCampaign, "id")
        assertEquals("https://secret-joker.cfd/?af_status=Organic&media_source=organic&appsflyer_id=id", url)
    }

    @Test
    fun `empty sub1 means no extra path segment`() {
        val emptySub1 = linkedMapOf("campaign" to "_AR_x", "af_status" to "Non-organic")
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", emptySub1, null)
        assertTrue(url.startsWith("https://secret-joker.cfd/?"))
        assertTrue(url.contains("campaign=_AR_x"))
        assertTrue(url.contains("af_status=Non-organic"))
        assertTrue(url.contains("sub2=AR"))
    }

    @Test
    fun `sub1 goes after an existing base path`() {
        val url = OfferUrlBuilder.build("https://domain.com/offer/", linkedMapOf("campaign" to "test"), null)
        assertEquals("https://domain.com/offer/test?campaign=test&sub1=test", url)
    }

    @Test
    fun `base without trailing slash still gets a separator`() {
        val url = OfferUrlBuilder.build("https://domain.com", linkedMapOf("campaign" to "test"), null)
        assertTrue(url.startsWith("https://domain.com/test?"))
    }

    @Test
    fun `rebuilding an already built url does not repeat sub1 in path`() {
        val first = OfferUrlBuilder.build("https://secret-joker.cfd/", params, "test-af-id")
        val second = OfferUrlBuilder.build(first, params, "test-af-id")
        assertEquals(first, second)
    }

    @Test
    fun `keeps base fragment`() {
        val url = OfferUrlBuilder.build("https://domain.com/#top", linkedMapOf("campaign" to "a_b"), null)
        assertEquals("https://domain.com/a?campaign=a_b&sub1=a&sub2=b#top", url)
    }
}
