package com.rick.tecladoporomongueta

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    private fun key(
        code: Int,
        width: Dimension = Dimension.Fraction(0.1f),
        gap: Dimension = Dimension.Dp(0f)
    ) = Key(intArrayOf(code), "k$code", 0, false, width, gap)

    private fun row(vararg keys: Key, height: Float = 50f, vGap: Float = 8f) =
        KeyboardRow(keys.toList(), Dimension.Dp(height), Dimension.Dp(vGap))

    @Test
    fun dimensionParsesTheFormsFoundInCompiledXml() {
        val default = Dimension.Dp(0f)
        assertEquals(Dimension.Dp(44f), Dimension.parse("44dp", default))
        assertEquals(Dimension.Dp(44f), Dimension.parse("44.0dip", default))
        assertEquals(Dimension.Fraction(0.12f), Dimension.parse("12%p", default))
        assertEquals(Dimension.Fraction(0.1f), Dimension.parse("10.0%p", default))
        assertEquals(Dimension.Px(10f), Dimension.parse("10px", default))
        assertEquals(default, Dimension.parse("lixo", default))
        assertEquals(default, Dimension.parse(null, default))
    }

    @Test
    fun codesAcceptSingleMultipleAndHex() {
        assertArrayEquals(intArrayOf(49), KeyboardLayout.parseCodes("49"))
        assertArrayEquals(intArrayOf(269, 803), KeyboardLayout.parseCodes("269,803"))
        assertArrayEquals(intArrayOf(0x2022), KeyboardLayout.parseCodes("0x2022"))
        assertArrayEquals(intArrayOf(), KeyboardLayout.parseCodes(null))
        assertArrayEquals(intArrayOf(), KeyboardLayout.parseCodes(" "))
    }

    @Test
    fun multiCodeKeyTypesAllCharacters() {
        val key = Key(intArrayOf(269, 803), "č̣", 0, false, Dimension.Fraction(0.1f), Dimension.Dp(0f))
        assertTrue(key.isMultiCode)
        assertEquals("č̣", key.outputText())
        assertEquals("Č̣", key.outputText(uppercase = true))
    }

    @Test
    fun rowsThatFitKeepTheirSizeAndAreLeftAligned() {
        val layout = KeyboardLayout(listOf(row(key(1), key(2))))
        layout.layout(1000f, 1f)
        assertEquals(0f, layout.keys[0].x, 0.01f)
        assertEquals(100f, layout.keys[0].width, 0.01f)
        assertEquals(100f, layout.keys[1].x, 0.01f)
        assertEquals(200f, layout.contentWidth, 0.01f)
    }

    @Test
    fun rowsThatFitHaveTheGapBeforeEachKey() {
        val layout = KeyboardLayout(
            listOf(row(key(1, gap = Dimension.Dp(10f)), key(2, gap = Dimension.Dp(10f))))
        )
        layout.layout(1000f, 1f)
        assertEquals(10f, layout.keys[0].x, 0.01f)
        assertEquals(120f, layout.keys[1].x, 0.01f)
    }

    @Test
    fun overflowingRowsShrinkToFitTheWidthIncludingGaps() {
        val keys = (1..10).map { key(it, gap = Dimension.Dp(6f)) }.toTypedArray()
        val layout = KeyboardLayout(listOf(row(*keys)))
        layout.layout(1000f, 1f)

        val last = layout.keys.last()
        assertEquals(1000f, last.x + last.width, 0.5f)
        assertTrue(layout.keys.all { it.width in 90f..100f })
        // espaço de 6 entre as teclas
        assertEquals(6f, layout.keys[1].x - (layout.keys[0].x + layout.keys[0].width), 0.01f)
    }

    @Test
    fun rowsStackWithVerticalGapAndNoTrailingGap() {
        val layout = KeyboardLayout(listOf(row(key(1)), row(key(2)), row(key(3))))
        layout.layout(1000f, 1f)
        assertEquals(0f, layout.keys[0].y, 0.01f)
        assertEquals(58f, layout.keys[1].y, 0.01f)
        assertEquals(116f, layout.keys[2].y, 0.01f)
        assertEquals(166f, layout.contentHeight, 0.01f)
    }

    @Test
    fun densityScalesDpButNotFractions() {
        val layout = KeyboardLayout(
            listOf(row(key(1, Dimension.Dp(40f)), key(2, Dimension.Fraction(0.5f)), height = 50f))
        )
        layout.layout(1000f, 2f)
        assertEquals(80f, layout.keys[0].width, 0.01f)
        assertEquals(500f, layout.keys[1].width, 0.01f)
        assertEquals(100f, layout.keys[0].height, 0.01f)
    }

    @Test
    fun nearestKeyFindsKeyUnderOrNearTheTouch() {
        val layout = KeyboardLayout(listOf(row(key(1, gap = Dimension.Dp(10f)), key(2))))
        layout.layout(1000f, 1f)

        assertEquals(1, layout.nearestKey(50f, 25f)?.primaryCode)
        assertEquals(2, layout.nearestKey(150f, 25f)?.primaryCode)
        // no espaço entre as teclas, vale a mais próxima
        assertNotNull(layout.nearestKey(105f, 25f))
        // muito longe de qualquer tecla
        assertNull(layout.nearestKey(900f, 25f))
        assertNull(layout.nearestKey(50f, 400f))
    }
}
