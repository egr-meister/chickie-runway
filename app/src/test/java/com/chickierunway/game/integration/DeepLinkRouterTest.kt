package com.chickierunway.game.integration

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkRouterTest {

    @After
    fun tearDown() {
        DeepLinkRouter.clearHandler()
        // Drain anything a test left queued.
        DeepLinkRouter.setHandler { }
        DeepLinkRouter.clearHandler()
    }

    @Test
    fun `delivers directly when handler is set`() {
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        DeepLinkRouter.handle("https://a.b/1")
        assertEquals(listOf("https://a.b/1"), received)
    }

    @Test
    fun `queues links until a handler registers, then delivers once`() {
        DeepLinkRouter.handle("https://a.b/1")
        DeepLinkRouter.handle("https://a.b/2")
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        assertEquals(listOf("https://a.b/1", "https://a.b/2"), received)

        val second = mutableListOf<String>()
        DeepLinkRouter.setHandler { second += it }
        assertEquals(emptyList<String>(), second)
    }

    @Test
    fun `ignores blank links`() {
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        DeepLinkRouter.handle("")
        DeepLinkRouter.handle(null)
        assertEquals(emptyList<String>(), received)
    }

    @Test
    fun `cleared handler queues again`() {
        DeepLinkRouter.setHandler { }
        DeepLinkRouter.clearHandler()
        DeepLinkRouter.handle("https://a.b/late")
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        assertEquals(listOf("https://a.b/late"), received)
    }
}
