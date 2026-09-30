package com.chickierunway.game.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSchemesTest {

    @Test
    fun `allowed schemes stay inside`() {
        listOf(
            "http://a.b", "https://a.b", "HTTPS://A.B", "about:blank", "blob:https://a.b/x",
            "data:text/html,hi", "javascript:void(0)", "file:///android_asset/x.html", "srcdoc:x"
        ).forEach { assertFalse(it, UrlSchemes.isExternal(it)) }
    }

    @Test
    fun `other schemes go external`() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end", "market://details?id=x", "tg://resolve?domain=x",
            "whatsapp://send", "mailto:a@b.c", "tel:+100"
        ).forEach { assertTrue(it, UrlSchemes.isExternal(it)) }
    }

    @Test
    fun `relative urls stay inside`() {
        listOf("/path", "path/with:colon", "?q=1", "#frag", "2go:x", "").forEach {
            assertFalse(it, UrlSchemes.isExternal(it))
        }
    }
}
