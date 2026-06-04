package com.rick.tecladoporomongueta

class SuggestionManager(
    private val dictionaryManager: DictionaryManager,
    private val learningManager: LearningManager
) {

    fun getSuggestions(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = prefix.lowercase()

        val learnedWords = learningManager.getLearnedWords()
            .filterKeys { it.startsWith(lowerPrefix) }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val dictWords = dictionaryManager.search(lowerPrefix, maxResults * 2)
            .map { it.lowercase() }

        val combined = linkedSetOf<String>()

        for (word in learnedWords) {
            combined.add(word)
            if (combined.size >= maxResults) return combined.toList().take(maxResults)
        }

        for (word in dictWords) {
            combined.add(word)
            if (combined.size >= maxResults) return combined.toList().take(maxResults)
        }

        return combined.toList().take(maxResults)
    }
}
