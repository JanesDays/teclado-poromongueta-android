package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionManagerTest {

    private val dictionary = DictionaryManager.fromLines(listOf("ana", "anaûé", "anda", "kûekatu"))

    @Test
    fun learnedWordsComeBeforeDictionaryOrderedByFrequency() {
        val learned = mapOf("anaru" to 1, "ano" to 5)
        val manager = SuggestionManager(dictionary) { learned }
        assertEquals(listOf("ano", "anaru", "ana"), manager.getSuggestions("an", 3))
    }

    @Test
    fun noDuplicatesBetweenLearnedAndDictionary() {
        val manager = SuggestionManager(dictionary) { mapOf("ana" to 2) }
        assertEquals(listOf("ana", "anaûé", "anda"), manager.getSuggestions("an", 3))
    }

    @Test
    fun matchesWithoutAccentsAndIgnoringCase() {
        val manager = SuggestionManager(dictionary) { emptyMap() }
        assertEquals(listOf("kûekatu"), manager.getSuggestions("KUE"))
    }

    @Test
    fun blankPrefixGivesNoSuggestions() {
        val manager = SuggestionManager(dictionary) { emptyMap() }
        assertTrue(manager.getSuggestions("").isEmpty())
    }
}
