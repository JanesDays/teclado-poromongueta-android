package com.rick.tecladoporomongueta

class SuggestionManager(
    private val dictionaryManager: DictionaryManager,
    private val userWords: (prefix: String, max: Int) -> List<String> = { _, _ -> emptyList() },
    private val learnedWords: () -> Map<String, Int>
) {

    /**
     * Ordem: palavras adicionadas pelo usuário, palavras que ele já usou (as que batem
     * exatamente com os acentos digitados antes, depois as mais frequentes) e, por fim,
     * as do dicionário.
     */
    fun getSuggestions(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = TextNormalizer.lower(prefix)
        val foldedPrefix = TextNormalizer.fold(prefix)

        val combined = linkedSetOf<String>()
        combined.addAll(userWords(lowerPrefix, maxResults))

        if (combined.size < maxResults) {
            learnedWords().entries
                .filter { TextNormalizer.fold(it.key).startsWith(foldedPrefix) }
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.key.startsWith(lowerPrefix) }
                        .thenByDescending { it.value }
                )
                .forEach { combined.add(it.key) }
        }

        if (combined.size < maxResults) {
            combined.addAll(dictionaryManager.search(lowerPrefix, maxResults * 2))
        }

        return combined.take(maxResults)
    }
}
