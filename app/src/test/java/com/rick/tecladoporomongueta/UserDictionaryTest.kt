package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDictionaryTest {

    @Test
    fun addPersistsAndSearchesIgnoringAccents() {
        val storage = MemoryStorage()
        val dictionary = UserDictionary(storage)
        assertTrue(dictionary.add("Kûekatu", "bom"))

        val reloaded = UserDictionary(storage)
        assertEquals(listOf("kûekatu"), reloaded.search("kue"))
        assertEquals("bom", reloaded.getEntries().single().translation)
    }

    @Test
    fun addingTheSameWordTwiceKeepsOne() {
        val dictionary = UserDictionary(MemoryStorage())
        dictionary.add("ana", "primeira")
        dictionary.add("ANA", "segunda")
        assertEquals(1, dictionary.count())
        assertEquals("segunda", dictionary.getEntries().single().translation)
    }

    @Test
    fun invalidWordsAreRejected() {
        val dictionary = UserDictionary(MemoryStorage())
        assertFalse(dictionary.add(""))
        assertFalse(dictionary.add("123"))
        assertFalse(dictionary.add("duas palavras"))
        assertFalse(dictionary.add("a".repeat(WordPolicy.MAX_LENGTH + 1)))
        assertEquals(0, dictionary.count())
    }

    @Test
    fun removeDeletesTheWord() {
        val dictionary = UserDictionary(MemoryStorage())
        dictionary.add("ana")
        dictionary.remove("ANA")
        assertEquals(0, dictionary.count())
    }

    @Test
    fun exportAndImportRoundTrip() {
        val source = UserDictionary(MemoryStorage())
        source.add("kûekatu", "bom")
        source.add("po'õ")

        val target = UserDictionary(MemoryStorage())
        assertEquals(2, target.import(source.export()))
        assertEquals(source.getEntries(), target.getEntries())
    }

    @Test
    fun importSkipsCommentsBlankAndInvalidLines() {
        val dictionary = UserDictionary(MemoryStorage())
        val added = dictionary.import("# comentário\n\n123\nana\tcasa\n" + TextNormalizer.BOM + "kûe\ncom espaço\n")
        assertEquals(2, added)
        assertEquals(listOf("ana", "kûe"), dictionary.getEntries().map { it.word })
    }

    @Test
    fun translationCannotBreakTheFileFormat() {
        val storage = MemoryStorage()
        val dictionary = UserDictionary(storage)
        dictionary.add("ana", "linha1\nlinha2\toutra")
        assertEquals(1, UserDictionary(storage).count())
        assertEquals("linha1 linha2 outra", UserDictionary(storage).getEntries().single().translation)
    }
}
