package com.chickierunway.game

import com.chickierunway.game.logic.GameLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    @Test
    fun `level table matches the spec`() {
        val expected = listOf(
            Triple(2, 2, 60), Triple(3, 2, 70), Triple(4, 2, 70),
            Triple(6, 3, 80), Triple(6, 3, 80), Triple(8, 4, 100),
            Triple(8, 4, 100), Triple(10, 4, 110), Triple(12, 4, 120)
        )
        assertEquals(GameLogic.LEVEL_COUNT, GameLogic.levels.size)
        GameLogic.levels.forEachIndexed { i, cfg ->
            assertEquals(i + 1, cfg.level)
            assertEquals(expected[i], Triple(cfg.pairs, cfg.columns, cfg.seconds))
        }
        assertNull(GameLogic.levelConfig(0))
        assertNull(GameLogic.levelConfig(10))
    }

    @Test
    fun `deck has exactly two of each chosen item for every level`() {
        GameLogic.levels.forEach { cfg ->
            repeat(20) { seed ->
                val deck = GameLogic.buildDeck(cfg.pairs, Random(seed))
                assertEquals(cfg.pairs * 2, deck.size)
                val counts = deck.groupingBy { it.itemIndex }.eachCount()
                assertEquals(cfg.pairs, counts.size)
                assertTrue(counts.values.all { it == 2 })
                assertTrue(deck.all { it.itemIndex in 0 until GameLogic.ITEM_COUNT })
                assertEquals(deck.size, deck.map { it.id }.toSet().size)
            }
        }
    }

    @Test
    fun `deck clamps invalid pair counts`() {
        assertEquals(0, GameLogic.buildDeck(-3).size)
        assertEquals(GameLogic.ITEM_COUNT * 2, GameLogic.buildDeck(99).size)
    }

    @Test
    fun `match requires same item and different cards`() {
        val a = GameLogic.DeckCard(0, 5)
        val b = GameLogic.DeckCard(1, 5)
        val c = GameLogic.DeckCard(2, 6)
        assertTrue(GameLogic.isMatch(a, b))
        assertFalse(GameLogic.isMatch(a, a))
        assertFalse(GameLogic.isMatch(a, c))
    }

    @Test
    fun `level 1 always unlocked and others need previous cleared`() {
        val empty = emptyMap<String, Int>()
        assertTrue(GameLogic.isLevelUnlocked(empty, 1))
        (2..9).forEach { assertFalse(GameLogic.isLevelUnlocked(empty, it)) }
        assertFalse(GameLogic.isLevelUnlocked(empty, 0))
        assertFalse(GameLogic.isLevelUnlocked(empty, 10))

        val partial = mapOf("1" to 1) // level 1 needs 2 pairs
        assertFalse(GameLogic.isLevelCleared(partial, 1))
        assertFalse(GameLogic.isLevelUnlocked(partial, 2))

        val cleared = mapOf("1" to 2, "2" to 3)
        assertTrue(GameLogic.isLevelUnlocked(cleared, 2))
        assertTrue(GameLogic.isLevelUnlocked(cleared, 3))
        assertFalse(GameLogic.isLevelUnlocked(cleared, 4))
    }

    @Test
    fun `completed levels are counted correctly`() {
        assertEquals(0, GameLogic.completedLevelsCount(emptyMap()))
        assertEquals(2, GameLogic.completedLevelsCount(mapOf("1" to 2, "2" to 3, "3" to 3)))
        val all = GameLogic.levels.associate { it.level.toString() to it.pairs }
        assertEquals(9, GameLogic.completedLevelsCount(all))
        assertEquals(1, GameLogic.completedLevelsCount(mapOf("1" to 2, "junk" to 50, "42" to 12)))
    }

    @Test
    fun `next level and time formatting`() {
        assertEquals(2, GameLogic.nextLevel(1))
        assertNull(GameLogic.nextLevel(9))
        assertNull(GameLogic.nextLevel(0))
        assertEquals("1:05", GameLogic.formatSeconds(65))
        assertEquals("0:00", GameLogic.formatSeconds(-4))
        assertEquals("2:00", GameLogic.formatSeconds(120))
    }
}
