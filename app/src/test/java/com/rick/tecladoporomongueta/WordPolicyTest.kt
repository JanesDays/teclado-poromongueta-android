package com.rick.tecladoporomongueta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordPolicyTest {

    @Test
    fun acceptsWordsWithSpecialLetters() {
        assertTrue(WordPolicy.isLearnable("kûekatu"))
        assertTrue(WordPolicy.isLearnable("po'õ"))
        assertTrue(WordPolicy.isLearnable("mo-îatïk"))
        assertTrue(WordPolicy.isLearnable("č̣a"))
        assertTrue(WordPolicy.isLearnable("Ana"))
    }

    @Test
    fun rejectsDigitsSoCodesAndDocumentsAreNotLearned() {
        assertFalse(WordPolicy.isLearnable("123456"))
        assertFalse(WordPolicy.isLearnable("abc123"))
        assertFalse(WordPolicy.isLearnable("a1"))
    }

    @Test
    fun rejectsTooShortTooLongAndSymbolsOnly() {
        assertFalse(WordPolicy.isLearnable("a"))
        assertFalse(WordPolicy.isLearnable(""))
        assertFalse(WordPolicy.isLearnable("a".repeat(WordPolicy.MAX_LENGTH + 1)))
        assertFalse(WordPolicy.isLearnable("--"))
        assertFalse(WordPolicy.isLearnable("''"))
        assertFalse(WordPolicy.isLearnable("a b"))
        assertFalse(WordPolicy.isLearnable("a@b"))
    }
}
