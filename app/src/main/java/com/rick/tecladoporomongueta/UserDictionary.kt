package com.rick.tecladoporomongueta

import android.content.Context

/**
 * Palavras que o próprio usuário (ou a comunidade) adicionou ao dicionário.
 * Ficam em um arquivo de texto no formato de [DictionaryEntry], que pode ser exportado
 * e compartilhado, e importado em outro aparelho.
 */
class UserDictionary(private val storage: TextStorage) {

    private class Item(val entry: DictionaryEntry, val lower: String, val folded: String)

    private var items: MutableList<Item>? = null

    /** Devolve true se a palavra foi adicionada (ou atualizada). */
    fun add(word: String, translation: String = ""): Boolean {
        val entry = sanitize(DictionaryEntry(word, translation)) ?: return false
        val list = loaded()
        val lower = entry.word.lowercase()
        val existing = list.indexOfFirst { it.lower == lower }
        if (existing >= 0) list.removeAt(existing)
        else if (list.size >= MAX_WORDS) return false
        list.add(Item(entry, lower, TextNormalizer.fold(entry.word)))
        save()
        return true
    }

    fun remove(word: String) {
        val lower = TextNormalizer.lower(word)
        if (loaded().removeAll { it.lower == lower }) save()
    }

    fun getEntries(): List<DictionaryEntry> =
        loaded().map { it.entry }.sortedBy { TextNormalizer.fold(it.word) }

    fun count(): Int = loaded().size

    /** Palavras que começam com [prefix] (ignora acentos e maiúsculas), exatas primeiro. */
    fun search(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()
        val exact = TextNormalizer.lower(prefix)
        val folded = TextNormalizer.fold(prefix)
        return loaded()
            .filter { it.folded.startsWith(folded) }
            .sortedByDescending { it.lower.startsWith(exact) }
            .take(maxResults)
            .map { it.lower }
    }

    /** Texto para compartilhar: um cabeçalho de comentário e uma palavra por linha. */
    fun export(): String =
        HEADER + "\n" + getEntries().joinToString("\n") { it.toLine() } + "\n"

    /** Importa palavras de um texto exportado. Devolve quantas foram adicionadas. */
    fun import(text: String): Int {
        var added = 0
        text.lineSequence().take(MAX_IMPORT_LINES).forEach { line ->
            val entry = DictionaryEntry.parse(line) ?: return@forEach
            if (add(entry.word, entry.translation)) added++
        }
        return added
    }

    fun clear() {
        items = mutableListOf()
        storage.delete()
    }

    private fun sanitize(entry: DictionaryEntry): DictionaryEntry? {
        val word = TextNormalizer.clean(entry.word)
        if (word.length !in 1..WordPolicy.MAX_LENGTH) return null
        if (!word.all { TextNormalizer.isWordChar(it) } || word.none { it.isLetter() }) return null
        val translation = TextNormalizer.clean(entry.translation.replace('\t', ' ').replace('\n', ' '))
            .take(MAX_TRANSLATION_LENGTH)
        return DictionaryEntry(word, translation)
    }

    private fun loaded(): MutableList<Item> {
        items?.let { return it }
        val list = mutableListOf<Item>()
        storage.read()?.lineSequence()?.forEach { line ->
            DictionaryEntry.parse(line)?.let { sanitize(it) }?.let {
                list.add(Item(it, it.word.lowercase(), TextNormalizer.fold(it.word)))
            }
        }
        items = list
        return list
    }

    private fun save() {
        storage.write(export())
    }

    companion object {
        const val HEADER = "# Teclado Poromonguetá - palavras do usuário (palavra<TAB>tradução)"
        private const val MAX_WORDS = 5000
        private const val MAX_IMPORT_LINES = 20000
        private const val MAX_TRANSLATION_LENGTH = 120

        @Volatile
        private var instance: UserDictionary? = null

        fun getInstance(context: Context): UserDictionary =
            instance ?: synchronized(this) {
                instance ?: UserDictionary(PlainTextStorage(context, "user_dictionary.tsv"))
                    .also { instance = it }
            }
    }
}
