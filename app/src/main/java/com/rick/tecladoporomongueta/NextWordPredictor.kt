package com.rick.tecladoporomongueta

/** Lógica pura da previsão da próxima palavra (pares "anterior próxima"). */
class NextWordPredictor(private val store: FrequencyStore) {

    /** Devolve true se o par foi registrado. */
    fun record(previous: String, next: String): Boolean {
        if (!WordPolicy.isLearnable(previous) || !WordPolicy.isLearnable(next)) return false
        return store.increment(key(previous, next))
    }

    /** Palavras que costumam vir depois de [previous], da mais para a menos frequente. */
    fun nextWords(previous: String, maxResults: Int = 3): List<String> {
        if (!WordPolicy.isLearnable(previous)) return emptyList()
        val prefix = TextNormalizer.lower(previous) + SEPARATOR
        return store.entries().entries
            .filter { it.key.startsWith(prefix) && it.value >= WordPolicy.MIN_USES_TO_SUGGEST }
            .sortedByDescending { it.value }
            .take(maxResults)
            .map { it.key.removePrefix(prefix) }
    }

    private fun key(previous: String, next: String) =
        TextNormalizer.lower(previous) + SEPARATOR + TextNormalizer.lower(next)

    private companion object {
        const val SEPARATOR = " "
    }
}
