package com.rick.tecladoporomongueta

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class DictionaryManager(context: Context) {

    private val wordList: MutableList<String> = mutableListOf()

    init {
        loadDictionary(context)
    }

    private fun loadDictionary(context: Context) {
        try {
            val inputStream = context.assets.open("dictionary.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val word = line.trim()
                    if (word.isNotEmpty()) {
                        wordList.add(word)
                    }
                }
            }
        } catch (e: Exception) {
            wordList.clear()
        }
    }

    fun search(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lowerPrefix = prefix.lowercase()
        return wordList
            .filter { it.lowercase().startsWith(lowerPrefix) }
            .take(maxResults)
    }

    fun getAllWords(): List<String> = wordList.toList()

    fun wordCount(): Int = wordList.size
}
