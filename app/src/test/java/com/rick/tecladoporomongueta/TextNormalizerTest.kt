package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun clean_removesBomAndSpaces() {
        assertEquals("anaûé", TextNormalizer.clean(TextNormalizer.BOM + " anaûé \n"))
    }

    @Test
    fun clean_unifiesDecomposedAndComposedForms() {
        assertEquals(TextNormalizer.clean("û"), TextNormalizer.clean("û"))
    }

    @Test
    fun fold_removesDiacriticsButKeepsApostropheAndHyphen() {
        assertEquals("kuekatu", TextNormalizer.fold("kûekatu"))
        assertEquals("mba'e", TextNormalizer.fold("Mba'e"))
        assertEquals("mo-iatik", TextNormalizer.fold("mo-îatïk"))
    }

    @Test
    fun fold_handlesTildeOnY() {
        assertEquals("y", TextNormalizer.fold("ỹ"))
    }
}
