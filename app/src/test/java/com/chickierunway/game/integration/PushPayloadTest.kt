package com.chickierunway.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {

    @Test
    fun `notification fields win over data`() {
        val p = PushPayload.from(mapOf("title" to "d", "body" to "d"), "n-title", "n-body")
        assertEquals("n-title", p.title)
        assertEquals("n-body", p.body)
    }

    @Test
    fun `data-only push falls back to data keys`() {
        val p = PushPayload.from(mapOf("title" to "t", "message" to "m", "link" to "https://x.y/a"), null, null)
        assertEquals("t", p.title)
        assertEquals("m", p.body)
        assertEquals("https://x.y/a", p.url)
    }

    @Test
    fun `url key is preferred over link`() {
        val p = PushPayload.from(mapOf("url" to "https://a.b/1", "link" to "https://a.b/2"), null, null)
        assertEquals("https://a.b/1", p.url)
    }

    @Test
    fun `blank url means no url`() {
        assertNull(PushPayload.from(mapOf("url" to "  "), null, null).url)
        assertNull(PushPayload.from(emptyMap(), null, null).url)
    }
}
