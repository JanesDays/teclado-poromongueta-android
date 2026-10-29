package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextWordPredictorTest {

    private fun predictor() = NextWordPredictor(FrequencyStore(MemoryStorage(), 100))

    @Test
    fun suggestsOnlyAfterEnoughUses() {
        val predictor = predictor()
        predictor.record("bom", "dia")
        assertTrue(predictor.nextWords("bom").isEmpty())
        predictor.record("bom", "dia")
        assertEquals(listOf("dia"), predictor.nextWords("bom"))
    }

    @Test
    fun ordersByFrequencyAndIgnoresCase() {
        val predictor = predictor()
        repeat(2) { predictor.record("Bom", "dia") }
        repeat(5) { predictor.record("bom", "tarde") }
        assertEquals(listOf("tarde", "dia"), predictor.nextWords("BOM"))
    }

    @Test
    fun doesNotMixDifferentPreviousWords() {
        val predictor = predictor()
        repeat(2) { predictor.record("bom", "dia") }
        assertTrue(predictor.nextWords("boa").isEmpty())
        assertTrue(predictor.nextWords("bomba").isEmpty())
    }

    @Test
    fun rejectsInvalidWordsAndLimitsResults() {
        val predictor = predictor()
        assertFalse(predictor.record("123", "dia"))
        assertFalse(predictor.record("bom", "a"))
        listOf("um", "dois", "tres", "quatro").forEach { repeat(2) { _ -> predictor.record("lista", it) } }
        assertEquals(3, predictor.nextWords("lista", 3).size)
    }
}
