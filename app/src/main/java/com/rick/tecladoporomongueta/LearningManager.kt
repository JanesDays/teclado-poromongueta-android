package com.rick.tecladoporomongueta

import android.content.Context
import android.content.SharedPreferences

class LearningManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveWord(word: String) {
        if (word.isBlank()) return
        val lower = word.lowercase()
        val currentFreq = getFrequency(lower)
        prefs.edit().putInt(lower, currentFreq + 1).apply()
    }

    fun getFrequency(word: String): Int {
        return prefs.getInt(word.lowercase(), 0)
    }

    fun getLearnedWords(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) ->
            if (value is Int && value > 0) {
                result[key] = value
            }
        }
        return result
    }

    fun getTopLearned(limit: Int = 50): List<String> {
        return getLearnedWords()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "poromongueta_learning"
    }
}
