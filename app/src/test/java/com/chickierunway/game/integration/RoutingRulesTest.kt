package com.chickierunway.game.integration

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutingRulesTest {

    private val requested = "https://secret-joker.cfd/android1?sub1=android1"

    @Test
    fun `404 is White`() {
        assertEquals(RouteDecision.White, RoutingRules.decide(ProbeResult(404, requested), requested))
    }

    @Test
    fun `any other code is Black with final url`() {
        listOf(200, 204, 302, 403, 500, 503).forEach { code ->
            assertEquals(
                RouteDecision.Black("https://final.example/"),
                RoutingRules.decide(ProbeResult(code, "https://final.example/"), requested)
            )
        }
    }

    @Test
    fun `missing final url falls back to requested`() {
        assertEquals(RouteDecision.Black(requested), RoutingRules.decide(ProbeResult(200, null), requested))
    }
}
