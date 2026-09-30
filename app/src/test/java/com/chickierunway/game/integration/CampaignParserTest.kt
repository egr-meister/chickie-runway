package com.chickierunway.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignParserTest {

    @Test
    fun `splits campaign into positional subs without decoding`() {
        val subs = CampaignParser.parse("android1_AR_sub3%26_sub4_WBZ_sub6")
        assertEquals(
            linkedMapOf(
                "sub1" to "android1",
                "sub2" to "AR",
                "sub3" to "sub3%26",
                "sub4" to "sub4",
                "sub5" to "WBZ",
                "sub6" to "sub6"
            ),
            subs
        )
        assertEquals(listOf("sub1", "sub2", "sub3", "sub4", "sub5", "sub6"), subs.keys.toList())
    }

    @Test
    fun `keeps empty segments in place`() {
        val subs = CampaignParser.parse("a__c")
        assertEquals(linkedMapOf("sub1" to "a", "sub2" to "", "sub3" to "c"), subs)
    }

    @Test
    fun `empty campaign gives no subs`() {
        assertTrue(CampaignParser.parse("").isEmpty())
    }

    @Test
    fun `toQuery does not double encode`() {
        val query = CampaignParser.toQuery(linkedMapOf("sub1" to "android1", "sub3" to "sub3%26"))
        assertEquals("sub1=android1&sub3=sub3%26", query)
    }

    @Test
    fun `toQuery encodes raw separators`() {
        assertEquals("k=a%26b%3Dc%20d", CampaignParser.toQuery(linkedMapOf("k" to "a&b=c d")))
    }
}
