package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictionaryEntryTest {

    @Test
    fun parsesPlainWordsAndRichLines() {
        assertEquals(DictionaryEntry("anaûé"), DictionaryEntry.parse("anaûé"))
        assertEquals(
            DictionaryEntry("kûekatu", "bom", "adj", "exemplo"),
            DictionaryEntry.parse("kûekatu\tbom\tadj\texemplo")
        )
    }

    @Test
    fun ignoresBlankLinesAndComments() {
        assertNull(DictionaryEntry.parse(""))
        assertNull(DictionaryEntry.parse("   "))
        assertNull(DictionaryEntry.parse("# comentário"))
        assertNull(DictionaryEntry.parse("\t sem palavra"))
    }

    @Test
    fun stripsBomAndNormalizes() {
        assertEquals("anaûé", DictionaryEntry.parse(TextNormalizer.BOM + "anaûé")?.word)
        assertEquals(DictionaryEntry.parse("û")?.word, DictionaryEntry.parse("û")?.word)
    }

    @Test
    fun toLineOmitsEmptyTrailingColumns() {
        assertEquals("ana", DictionaryEntry("ana").toLine())
        assertEquals("ana\tcasa", DictionaryEntry("ana", "casa").toLine())
        assertEquals("ana\t\tsubst", DictionaryEntry("ana", "", "subst").toLine())
    }

    @Test
    fun dictionaryExposesTranslationLookup() {
        val dictionary = DictionaryManager.fromLines(listOf("ana\tcasa", "kûe"))
        assertEquals("casa", dictionary.lookup("ANA")?.translation)
        assertEquals("kûe", dictionary.lookup("kue")?.word)
        assertNull(dictionary.lookup("nada"))
    }
}
