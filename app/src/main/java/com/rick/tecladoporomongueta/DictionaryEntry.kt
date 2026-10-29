package com.rick.tecladoporomongueta

/**
 * Uma palavra do dicionário. Só [word] é obrigatória; o restante pode ser preenchido
 * aos poucos. No arquivo, as colunas são separadas por tabulação:
 *
 *     palavra <TAB> tradução <TAB> classe gramatical <TAB> exemplo
 */
data class DictionaryEntry(
    val word: String,
    val translation: String = "",
    val partOfSpeech: String = "",
    val example: String = ""
) {
    fun toLine(): String = listOf(word, translation, partOfSpeech, example)
        .dropLastWhile { it.isEmpty() }
        .joinToString("\t")

    companion object {
        /** Lê uma linha do arquivo. Devolve null para linha vazia ou comentário (`#`). */
        fun parse(line: String): DictionaryEntry? {
            val clean = line.removePrefix(TextNormalizer.BOM)
            if (clean.isBlank() || clean.trimStart().startsWith("#")) return null
            val parts = clean.split('\t').map { TextNormalizer.clean(it) }
            val word = parts[0]
            if (word.isEmpty()) return null
            return DictionaryEntry(
                word = word,
                translation = parts.getOrElse(1) { "" },
                partOfSpeech = parts.getOrElse(2) { "" },
                example = parts.getOrElse(3) { "" }
            )
        }
    }
}
