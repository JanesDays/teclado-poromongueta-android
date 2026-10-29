package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryManagerTest {

    private val dictionary = DictionaryManager.fromLines(
        listOf(TextNormalizer.BOM + "anaûé","ana", "kûekatu", "kuera", "mba'e", "ANA", "po'õ", "")
    )

    @Test
    fun firstWordWithBomIsFound() {
        assertTrue(dictionary.search("ana").contains("anaûé"))
    }

    @Test
    fun duplicatesAndBlanksAreIgnored() {
        assertEquals(6, dictionary.wordCount())
    }

    @Test
    fun searchIgnoresCase() {
        assertEquals(listOf("mba'e"), dictionary.search("MBA"))
    }

    @Test
    fun searchWithoutAccentFindsSpecialLetters() {
        assertTrue(dictionary.search("kue", 5).contains("kûekatu"))
    }

    @Test
    fun exactAccentMatchComesFirst() {
        assertEquals("kûekatu", dictionary.search("kû").first())
        assertEquals("kuera", dictionary.search("ku").first { it == "kuera" })
    }

    @Test
    fun searchRespectsLimitAndBlankPrefix() {
        assertEquals(1, dictionary.search("a", 1).size)
        assertTrue(dictionary.search("  ").isEmpty())
    }

    @Test
    fun specialLettersAreKeptInResults() {
        assertEquals(listOf("po'õ"), dictionary.search("po"))
    }
}
