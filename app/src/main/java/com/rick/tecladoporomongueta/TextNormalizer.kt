package com.rick.tecladoporomongueta

import java.text.Normalizer

/**
 * Normalização de texto para o Poromonguetá.
 *
 * As palavras guardam sempre os caracteres especiais (û, î, ï, ã, ỹ...) como
 * foram escritas; [fold] só é usado para comparar na busca, permitindo que
 * quem digita "kuekatu" encontre "kûekatu".
 */
object TextNormalizer {

    private val COMBINING_MARKS = Regex("\\p{M}+")

    /** Marca de ordem de bytes (U+FEFF) que alguns editores colocam no início de arquivos UTF-8. */
    val BOM: String = 0xFEFF.toChar().toString()

    /** Remove BOM e espaços e unifica a forma Unicode (NFC). */
    fun clean(text: String): String =
        Normalizer.normalize(text.removePrefix(BOM).trim(), Normalizer.Form.NFC)

    fun lower(text: String): String = clean(text).lowercase()

    /** Letras, apóstrofo, hífen, ponto médio (·) e marcas de acento fazem parte de uma palavra. */
    fun isWordChar(char: Char): Boolean =
        Character.isLetter(char) || char == '\'' || char == '-' || char == '·' ||
            Character.getType(char) == Character.NON_SPACING_MARK.toInt()

    /** Minúsculas e sem acentos/diacríticos, apenas para comparação. */
    fun fold(text: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
}
