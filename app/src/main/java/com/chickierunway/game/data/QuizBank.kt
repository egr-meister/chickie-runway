package com.chickierunway.game.data

import androidx.annotation.StringRes
import com.chickierunway.game.R

/**
 * ============================================================================================
 *  QUIZ CONTENT — farm & chickens, 3 difficulty levels × 10 questions × 4 options.
 *  The texts live in string resources (strings.xml of every locale, keys quiz_<level>_qN…), so the
 *  quiz follows the app language. Rules the code relies on:
 *   - each level keeps a stable, unique `id` (it is the key for saved best scores);
 *   - exactly 10 questions per level (QUESTIONS_PER_CATEGORY);
 *   - exactly 4 options per question, one correct (`correctIndex`).
 * ============================================================================================
 */
data class QuizQuestion(
    @StringRes val text: Int,
    val options: List<Int>,
    val correctIndex: Int
)

data class QuizCategory(
    val id: String,
    @StringRes val title: Int,
    @StringRes val tagline: Int,
    /** 1 = easy … 3 = hard. */
    val difficulty: Int,
    val questions: List<QuizQuestion>
)

object QuizBank {

    const val QUESTIONS_PER_CATEGORY = 10
    const val OPTIONS_PER_QUESTION = 4

    /** Builds a question with the correct answer placed at [at] (0..3) among the three wrong ones. */
    private fun q(@StringRes text: Int, @StringRes correct: Int, wrong: List<Int>, at: Int): QuizQuestion {
        val position = at.coerceIn(0, wrong.size)
        val options = wrong.toMutableList().apply { add(position, correct) }
        return QuizQuestion(text = text, options = options, correctIndex = position)
    }

    val categories: List<QuizCategory> = listOf(
        QuizCategory(
            id = "easy",
            title = R.string.quiz_level_easy,
            tagline = R.string.quiz_tagline_easy,
            difficulty = 1,
            questions = listOf(
                q(R.string.quiz_easy_q1, R.string.quiz_easy_q1_a, listOf(R.string.quiz_easy_q1_w1, R.string.quiz_easy_q1_w2, R.string.quiz_easy_q1_w3), 1),
                q(R.string.quiz_easy_q2, R.string.quiz_easy_q2_a, listOf(R.string.quiz_easy_q2_w1, R.string.quiz_easy_q2_w2, R.string.quiz_easy_q2_w3), 0),
                q(R.string.quiz_easy_q3, R.string.quiz_easy_q3_a, listOf(R.string.quiz_easy_q3_w1, R.string.quiz_easy_q3_w2, R.string.quiz_easy_q3_w3), 2),
                q(R.string.quiz_easy_q4, R.string.quiz_easy_q4_a, listOf(R.string.quiz_easy_q4_w1, R.string.quiz_easy_q4_w2, R.string.quiz_easy_q4_w3), 3),
                q(R.string.quiz_easy_q5, R.string.quiz_easy_q5_a, listOf(R.string.quiz_easy_q5_w1, R.string.quiz_easy_q5_w2, R.string.quiz_easy_q5_w3), 1),
                q(R.string.quiz_easy_q6, R.string.quiz_easy_q6_a, listOf(R.string.quiz_easy_q6_w1, R.string.quiz_easy_q6_w2, R.string.quiz_easy_q6_w3), 0),
                q(R.string.quiz_easy_q7, R.string.quiz_easy_q7_a, listOf(R.string.quiz_easy_q7_w1, R.string.quiz_easy_q7_w2, R.string.quiz_easy_q7_w3), 2),
                q(R.string.quiz_easy_q8, R.string.quiz_easy_q8_a, listOf(R.string.quiz_easy_q8_w1, R.string.quiz_easy_q8_w2, R.string.quiz_easy_q8_w3), 3),
                q(R.string.quiz_easy_q9, R.string.quiz_easy_q9_a, listOf(R.string.quiz_easy_q9_w1, R.string.quiz_easy_q9_w2, R.string.quiz_easy_q9_w3), 1),
                q(R.string.quiz_easy_q10, R.string.quiz_easy_q10_a, listOf(R.string.quiz_easy_q10_w1, R.string.quiz_easy_q10_w2, R.string.quiz_easy_q10_w3), 2)
            )
        ),
        QuizCategory(
            id = "medium",
            title = R.string.quiz_level_medium,
            tagline = R.string.quiz_tagline_medium,
            difficulty = 2,
            questions = listOf(
                q(R.string.quiz_medium_q1, R.string.quiz_medium_q1_a, listOf(R.string.quiz_medium_q1_w1, R.string.quiz_medium_q1_w2, R.string.quiz_medium_q1_w3), 2),
                q(R.string.quiz_medium_q2, R.string.quiz_medium_q2_a, listOf(R.string.quiz_medium_q2_w1, R.string.quiz_medium_q2_w2, R.string.quiz_medium_q2_w3), 0),
                q(R.string.quiz_medium_q3, R.string.quiz_medium_q3_a, listOf(R.string.quiz_medium_q3_w1, R.string.quiz_medium_q3_w2, R.string.quiz_medium_q3_w3), 3),
                q(R.string.quiz_medium_q4, R.string.quiz_medium_q4_a, listOf(R.string.quiz_medium_q4_w1, R.string.quiz_medium_q4_w2, R.string.quiz_medium_q4_w3), 1),
                q(R.string.quiz_medium_q5, R.string.quiz_medium_q5_a, listOf(R.string.quiz_medium_q5_w1, R.string.quiz_medium_q5_w2, R.string.quiz_medium_q5_w3), 0),
                q(R.string.quiz_medium_q6, R.string.quiz_medium_q6_a, listOf(R.string.quiz_medium_q6_w1, R.string.quiz_medium_q6_w2, R.string.quiz_medium_q6_w3), 3),
                q(R.string.quiz_medium_q7, R.string.quiz_medium_q7_a, listOf(R.string.quiz_medium_q7_w1, R.string.quiz_medium_q7_w2, R.string.quiz_medium_q7_w3), 2),
                q(R.string.quiz_medium_q8, R.string.quiz_medium_q8_a, listOf(R.string.quiz_medium_q8_w1, R.string.quiz_medium_q8_w2, R.string.quiz_medium_q8_w3), 1),
                q(R.string.quiz_medium_q9, R.string.quiz_medium_q9_a, listOf(R.string.quiz_medium_q9_w1, R.string.quiz_medium_q9_w2, R.string.quiz_medium_q9_w3), 0),
                q(R.string.quiz_medium_q10, R.string.quiz_medium_q10_a, listOf(R.string.quiz_medium_q10_w1, R.string.quiz_medium_q10_w2, R.string.quiz_medium_q10_w3), 3)
            )
        ),
        QuizCategory(
            id = "hard",
            title = R.string.quiz_level_hard,
            tagline = R.string.quiz_tagline_hard,
            difficulty = 3,
            questions = listOf(
                q(R.string.quiz_hard_q1, R.string.quiz_hard_q1_a, listOf(R.string.quiz_hard_q1_w1, R.string.quiz_hard_q1_w2, R.string.quiz_hard_q1_w3), 2),
                q(R.string.quiz_hard_q2, R.string.quiz_hard_q2_a, listOf(R.string.quiz_hard_q2_w1, R.string.quiz_hard_q2_w2, R.string.quiz_hard_q2_w3), 0),
                q(R.string.quiz_hard_q3, R.string.quiz_hard_q3_a, listOf(R.string.quiz_hard_q3_w1, R.string.quiz_hard_q3_w2, R.string.quiz_hard_q3_w3), 3),
                q(R.string.quiz_hard_q4, R.string.quiz_hard_q4_a, listOf(R.string.quiz_hard_q4_w1, R.string.quiz_hard_q4_w2, R.string.quiz_hard_q4_w3), 1),
                q(R.string.quiz_hard_q5, R.string.quiz_hard_q5_a, listOf(R.string.quiz_hard_q5_w1, R.string.quiz_hard_q5_w2, R.string.quiz_hard_q5_w3), 2),
                q(R.string.quiz_hard_q6, R.string.quiz_hard_q6_a, listOf(R.string.quiz_hard_q6_w1, R.string.quiz_hard_q6_w2, R.string.quiz_hard_q6_w3), 0),
                q(R.string.quiz_hard_q7, R.string.quiz_hard_q7_a, listOf(R.string.quiz_hard_q7_w1, R.string.quiz_hard_q7_w2, R.string.quiz_hard_q7_w3), 3),
                q(R.string.quiz_hard_q8, R.string.quiz_hard_q8_a, listOf(R.string.quiz_hard_q8_w1, R.string.quiz_hard_q8_w2, R.string.quiz_hard_q8_w3), 1),
                q(R.string.quiz_hard_q9, R.string.quiz_hard_q9_a, listOf(R.string.quiz_hard_q9_w1, R.string.quiz_hard_q9_w2, R.string.quiz_hard_q9_w3), 2),
                q(R.string.quiz_hard_q10, R.string.quiz_hard_q10_a, listOf(R.string.quiz_hard_q10_w1, R.string.quiz_hard_q10_w2, R.string.quiz_hard_q10_w3), 0)
            )
        )
    )

    fun category(id: String?): QuizCategory? =
        if (id.isNullOrBlank()) null else categories.firstOrNull { it.id == id }
}
