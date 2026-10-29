package com.rick.tecladoporomongueta

/** Regras do que o teclado aceita aprender, para não guardar dados que não são palavras. */
object WordPolicy {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 40

    /** Uma palavra aprendida só é sugerida depois de usada este número de vezes. */
    const val MIN_USES_TO_SUGGEST = 2

    fun isLearnable(word: String): Boolean {
        val text = TextNormalizer.lower(word)
        if (text.length !in MIN_LENGTH..MAX_LENGTH) return false
        if (!text.all { TextNormalizer.isWordChar(it) }) return false
        return text.any { it.isLetter() }
    }
}
