package com.chickierunway.game.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MotionSampleTest {

    @Test
    fun `decimal separator is a dot on a comma locale`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"))
            val s = MotionSample.format(floatArrayOf(0.123f, -9.81f, 1f))
            assertTrue(s, s.contains("0.123"))
            assertFalse(s, s.contains("0,123"))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
