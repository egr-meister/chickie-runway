package com.chickierunway.game.logic

import kotlin.random.Random

/** Pure, context-free game rules. Fully unit-testable on the JVM. */
object GameLogic {

    const val LEVEL_COUNT = 9
    /** Size of the icon pool the deck is drawn from (Assets.items). */
    const val ITEM_COUNT = 18

    data class LevelConfig(val level: Int, val pairs: Int, val columns: Int, val seconds: Int)

    /** level : pairs / columns / seconds */
    val levels: List<LevelConfig> = listOf(
        LevelConfig(1, 2, 2, 60),
        LevelConfig(2, 3, 2, 70),
        LevelConfig(3, 4, 2, 70),
        LevelConfig(4, 6, 3, 80),
        LevelConfig(5, 6, 3, 80),
        LevelConfig(6, 8, 4, 100),
        LevelConfig(7, 8, 4, 100),
        LevelConfig(8, 10, 4, 110),
        LevelConfig(9, 12, 4, 120)
    )

    fun levelConfig(level: Int): LevelConfig? = levels.firstOrNull { it.level == level }

    /** One card on the table. [id] is unique per card; [itemIndex] (0..17) identifies the picture. */
    data class DeckCard(val id: Int, val itemIndex: Int)

    /**
     * Picks [pairs] distinct items out of [ITEM_COUNT], duplicates each and shuffles.
     * Result always has exactly 2 * pairs cards (pairs is clamped to 0..ITEM_COUNT).
     */
    fun buildDeck(pairs: Int, random: Random = Random.Default): List<DeckCard> {
        val safePairs = pairs.coerceIn(0, ITEM_COUNT)
        val chosen = (0 until ITEM_COUNT).shuffled(random).take(safePairs)
        return (chosen + chosen)
            .shuffled(random)
            .mapIndexed { index, item -> DeckCard(id = index, itemIndex = item) }
    }

    fun isMatch(a: DeckCard, b: DeckCard): Boolean = a.id != b.id && a.itemIndex == b.itemIndex

    fun bestPairs(pairsBest: Map<String, Int>, level: Int): Int = pairsBest[level.toString()] ?: 0

    fun isLevelCleared(pairsBest: Map<String, Int>, level: Int): Boolean {
        val config = levelConfig(level) ?: return false
        return bestPairs(pairsBest, level) >= config.pairs
    }

    /** Level 1 is always unlocked; level N unlocks once level N-1 is cleared. */
    fun isLevelUnlocked(pairsBest: Map<String, Int>, level: Int): Boolean = when {
        level < 1 || level > LEVEL_COUNT -> false
        level == 1 -> true
        else -> isLevelCleared(pairsBest, level - 1)
    }

    fun completedLevelsCount(pairsBest: Map<String, Int>): Int =
        (1..LEVEL_COUNT).count { isLevelCleared(pairsBest, it) }

    fun nextLevel(level: Int): Int? = if (level in 1 until LEVEL_COUNT) level + 1 else null

    fun formatSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
