package com.chickierunway.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferUrlBuilderTest {

    private val campaign = "android1_AR_sub3%26_sub4_WBZ_sub6"

    private val attribution = linkedMapOf(
        "network" to "unityads",
        "campaign" to campaign,
    )

    // --- «АСО»: path is left alone, sub1 only in query -----------------------

    @Test
    fun `aso builds query with campaign whole and exploded, adid last`() {
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", attribution, "test-adid")

        assertEquals(
            "https://secret-joker.cfd/?network=unityads" +
                "&campaign=android1_AR_sub3%26_sub4_WBZ_sub6&sub1=android1&sub2=AR&sub3=sub3%26" +
                "&sub4=sub4&sub5=WBZ&sub6=sub6&adid=test-adid",
            url
        )
        assertFalse(url.contains("%2526"))
    }

    @Test
    fun `aso keeps the path even when a campaign is present`() {
        val url = OfferUrlBuilder.build("https://domain.com/offer/", attribution, null)
        assertTrue(url.startsWith("https://domain.com/offer/?"))
        assertTrue(url.contains("sub1=android1"))
    }

    @Test
    fun `never drops an unknown attribution param, keeps empty, omits blank adid`() {
        val extra = LinkedHashMap(attribution).apply {
            put("adgroup", "set 1")
            put("creative", "cr1")
            put("empty_one", "")
        }
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", extra, null)
        assertTrue(url.contains("adgroup=set%201"))
        assertTrue(url.contains("creative=cr1"))
        assertTrue(url.contains("empty_one="))
        assertTrue(url.contains("campaign=$campaign"))
        assertFalse(url.contains("adid"))
    }

    @Test
    fun `parsed subs replace tracker-sent subs without duplicating keys`() {
        val withSub = LinkedHashMap(attribution).apply { put("sub1", "stale") }
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", withSub, "id")
        assertEquals(1, Regex("[?&]sub1=").findAll(url).count())
        assertTrue(url.contains("sub1=android1"))
    }

    @Test
    fun `extras override a same-named campaign sub without duplicating it`() {
        val longCampaign = (1..12).joinToString("_") { "s$it" }   // produces sub1..sub12
        val url = OfferUrlBuilder.build(
            "https://domain.com/",
            linkedMapOf("campaign" to longCampaign),
            null,
            extraParams = listOf("sub12" to "85_true"),
        )
        assertEquals(1, Regex("[?&]sub12=").findAll(url).count())
        assertTrue(url.contains("sub12=85_true"))
    }

    @Test
    fun `gps_adid from extras is always in the url, empty included, and wins over attribution`() {
        val withStale = LinkedHashMap(attribution).apply { put("gps_adid", "stale") }

        val empty = OfferUrlBuilder.build("https://domain.com/", withStale, null, listOf("gps_adid" to ""))
        assertEquals(1, Regex("[?&]gps_adid=").findAll(empty).count())
        assertTrue(Regex("[?&]gps_adid=(&|$)").containsMatchIn(empty))

        val real = OfferUrlBuilder.build("https://domain.com/", withStale, null, listOf("gps_adid" to "gaid-1"))
        assertTrue(real.contains("gps_adid=gaid-1"))
        assertFalse(real.contains("stale"))
    }

    @Test
    fun `no campaign means no subs`() {
        val url = OfferUrlBuilder.build(
            "https://secret-joker.cfd/",
            linkedMapOf("network" to "organic"),
            "id",
        )
        assertEquals("https://secret-joker.cfd/?network=organic&adid=id", url)
    }

    @Test
    fun `keeps base fragment`() {
        val url = OfferUrlBuilder.build("https://domain.com/#top", linkedMapOf("campaign" to "a_b"), null)
        assertEquals("https://domain.com/?campaign=a_b&sub1=a&sub2=b#top", url)
    }

    // --- «Прилы»: sub1 also in path after the alias --------------------------

    @Test
    fun `prily puts sub1 in path and keeps it in query, campaign whole`() {
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", attribution, "test-adid", sub1InPath = true)

        assertTrue(url.startsWith("https://secret-joker.cfd/android1?"))
        assertTrue(url.contains("campaign=$campaign"))
        assertTrue(url.contains("&sub1=android1"))
        listOf("sub2=AR", "sub3=sub3%26", "sub4=sub4", "sub5=WBZ", "sub6=sub6").forEach {
            assertTrue("missing $it", url.contains(it))
        }
        assertTrue(url.contains("adid=test-adid"))
    }

    @Test
    fun `prily with empty sub1 adds no path segment`() {
        val url = OfferUrlBuilder.build(
            "https://secret-joker.cfd/",
            linkedMapOf("campaign" to "_AR_x"),
            null,
            sub1InPath = true,
        )
        assertTrue(url.startsWith("https://secret-joker.cfd/?"))
        assertTrue(url.contains("campaign=_AR_x"))
        assertTrue(url.contains("sub2=AR"))
    }

    @Test
    fun `prily base without trailing slash still gets a separator`() {
        val url = OfferUrlBuilder.build(
            "https://domain.com",
            linkedMapOf("campaign" to "test"),
            null,
            sub1InPath = true,
        )
        assertTrue(url.startsWith("https://domain.com/test?"))
    }

    @Test
    fun `prily rebuilding an already built url does not repeat sub1 in path`() {
        val first = OfferUrlBuilder.build("https://secret-joker.cfd/", attribution, "test-adid", sub1InPath = true)
        val second = OfferUrlBuilder.build(first, attribution, "test-adid", sub1InPath = true)
        assertEquals(first, second)
    }
}
