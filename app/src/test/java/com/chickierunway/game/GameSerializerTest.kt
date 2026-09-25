package com.chickierunway.game

import com.chickierunway.game.data.GameData
import com.chickierunway.game.data.GameSerializer
import com.chickierunway.game.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameSerializerTest {

    private val defaults = GameData()

    @Test
    fun `null empty and blank decode to defaults`() {
        assertEquals(defaults, GameSerializer.decode(null))
        assertEquals(defaults, GameSerializer.decode(""))
        assertEquals(defaults, GameSerializer.decode("   \n"))
    }

    @Test
    fun `corrupted or wrong-shape json decodes to defaults`() {
        listOf("{", "not json", "[]", "null", "42", "{\"quizBest\": [1,2]}", "\u0000\u0001")
            .forEach { assertEquals("input: $it", defaults, GameSerializer.decode(it)) }
    }

    @Test
    fun `unknown keys are ignored`() {
        val raw = """{"quizBest":{"medium":7},"futureField":{"x":1},"settings":{"soundEnabled":false,"vibration":true}}"""
        val data = GameSerializer.decode(raw)
        assertEquals(mapOf("medium" to 7), data.quizBest)
        assertFalse(data.settings.soundEnabled)
    }

    @Test
    fun `missing fields fall back to defaults`() {
        assertEquals(defaults, GameSerializer.decode("{}"))
        val onlyPairs = GameSerializer.decode("""{"pairsBest":{"1":2}}""")
        assertEquals(mapOf("1" to 2), onlyPairs.pairsBest)
        assertEquals(emptyMap<String, Int>(), onlyPairs.quizBest)
        assertEquals(Settings(), onlyPairs.settings)
    }

    @Test
    fun `explicit nulls fall back to defaults`() {
        val data = GameSerializer.decode("""{"quizBest":null,"pairsBest":null,"settings":null}""")
        assertEquals(defaults, data)
    }

    @Test
    fun `impossible values are sanitized`() {
        val data = GameSerializer.decode("""{"quizBest":{"easy":99,"medium":-3},"pairsBest":{"1":-1,"x":4,"2":50}}""")
        assertEquals(mapOf("easy" to 10), data.quizBest)
        assertEquals(mapOf("2" to 12), data.pairsBest)
    }

    @Test
    fun `round trip keeps data`() {
        val data = GameData(
            quizBest = mapOf("easy" to 8, "hard" to 10),
            pairsBest = mapOf("1" to 2, "2" to 3),
            settings = Settings(soundEnabled = false)
        )
        assertEquals(data, GameSerializer.decode(GameSerializer.encode(data)))
    }
}
