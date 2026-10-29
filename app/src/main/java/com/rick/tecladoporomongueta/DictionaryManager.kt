package com.rick.tecladoporomongueta

import android.content.Context
import android.util.Log

/**
 * Dicionário em memória, ordenado pela forma sem acentos para permitir busca
 * binária por prefixo. Os caracteres especiais das palavras são preservados.
 */
class DictionaryManager(entries: List<DictionaryEntry>) {

    private class Item(val entry: DictionaryEntry, val lower: String, val folded: String)

    private val items: List<Item> = entries
        .map { it.copy(word = TextNormalizer.clean(it.word)) }
        .filter { it.word.isNotEmpty() }
        .distinctBy { it.word.lowercase() }
        .map { Item(it, it.word.lowercase(), TextNormalizer.fold(it.word)) }
        .sortedBy { it.folded }

    /**
     * Palavras que começam com [prefix], ignorando maiúsculas e acentos.
     * Quem bate exatamente com os acentos digitados vem primeiro.
     */
    fun search(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()
        val exactPrefix = TextNormalizer.lower(prefix)
        val foldedPrefix = TextNormalizer.fold(prefix)

        val matches = ArrayList<Item>()
        var i = lowerBound(foldedPrefix)
        while (i < items.size && items[i].folded.startsWith(foldedPrefix)) {
            matches.add(items[i])
            i++
        }
        return matches
            .sortedByDescending { it.lower.startsWith(exactPrefix) }
            .take(maxResults)
            .map { it.lower }
    }

    /** Dados completos de uma palavra (tradução, classe, exemplo), se existirem. */
    fun lookup(word: String): DictionaryEntry? {
        val folded = TextNormalizer.fold(word)
        val lower = TextNormalizer.lower(word)
        var i = lowerBound(folded)
        var fallback: DictionaryEntry? = null
        while (i < items.size && items[i].folded == folded) {
            if (items[i].lower == lower) return items[i].entry
            if (fallback == null) fallback = items[i].entry
            i++
        }
        return fallback
    }

    fun getEntries(): List<DictionaryEntry> = items.map { it.entry }

    fun getAllWords(): List<String> = items.map { it.lower }

    fun wordCount(): Int = items.size

    private fun lowerBound(foldedPrefix: String): Int {
        var low = 0
        var high = items.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (items[mid].folded < foldedPrefix) low = mid + 1 else high = mid
        }
        return low
    }

    companion object {
        private const val TAG = "DictionaryManager"

        /** Cria o dicionário a partir de linhas no formato descrito em [DictionaryEntry]. */
        fun fromLines(lines: List<String>): DictionaryManager =
            DictionaryManager(lines.mapNotNull { DictionaryEntry.parse(it) })

        fun fromAssets(context: Context, fileName: String = "dictionary.txt"): DictionaryManager {
            val lines = try {
                context.assets.open(fileName).bufferedReader(Charsets.UTF_8).useLines { it.toList() }
            } catch (e: Exception) {
                Log.e(TAG, "Não foi possível carregar $fileName", e)
                emptyList()
            }
            return fromLines(lines)
        }
    }
}
