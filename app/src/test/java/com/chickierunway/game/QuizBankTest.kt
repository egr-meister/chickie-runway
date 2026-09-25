package com.chickierunway.game

import com.chickierunway.game.data.QuizBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizBankTest {

    @Test
    fun `three difficulty levels with ten questions of four options each`() {
        assertEquals(3, QuizBank.categories.size)
        assertEquals(listOf(1, 2, 3), QuizBank.categories.map { it.difficulty })
        assertEquals(QuizBank.categories.size, QuizBank.categories.map { it.id }.toSet().size)
        QuizBank.categories.forEach { category ->
            assertEquals(QuizBank.QUESTIONS_PER_CATEGORY, category.questions.size)
            category.questions.forEach { q ->
                assertEquals("options for: ${q.text}", QuizBank.OPTIONS_PER_QUESTION, q.options.size)
                assertTrue(q.correctIndex in q.options.indices)
                assertEquals("duplicate options in: ${q.text}", q.options.size, q.options.toSet().size)
            }
        }
    }

    @Test
    fun `lookup handles unknown ids`() {
        assertEquals("easy", QuizBank.category("easy")?.id)
        assertNull(QuizBank.category(null))
        assertNull(QuizBank.category(""))
        assertNull(QuizBank.category("nope"))
    }
}
