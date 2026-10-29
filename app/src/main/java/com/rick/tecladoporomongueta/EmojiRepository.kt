package com.rick.tecladoporomongueta

import android.content.Context
import android.graphics.Paint
import android.util.Log

class EmojiCategory(val icon: String, val name: String, val emojis: List<String>)

/**
 * Lista de emoji (assets/emoji.txt) e os mais recentes. Só aparecem os emoji que a fonte
 * do aparelho sabe desenhar, para não mostrar quadradinhos em Androids mais antigos.
 */
class EmojiRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val categories: List<EmojiCategory> by lazy { load() }

    fun recents(): List<String> =
        prefs.getString(KEY_RECENTS, "")!!
            .split(SEPARATOR)
            .filter { it.isNotEmpty() }

    fun addRecent(emoji: String) {
        val updated = (listOf(emoji) + recents().filter { it != emoji }).take(MAX_RECENTS)
        prefs.edit().putString(KEY_RECENTS, updated.joinToString(SEPARATOR)).apply()
    }

    fun clearRecents() {
        prefs.edit().clear().apply()
    }

    private fun load(): List<EmojiCategory> {
        val paint = Paint()
        val result = mutableListOf<EmojiCategory>()
        var icon = ""
        var name = ""
        var emojis = mutableListOf<String>()

        fun flush() {
            if (icon.isNotEmpty() && emojis.isNotEmpty()) result.add(EmojiCategory(icon, name, emojis))
            emojis = mutableListOf()
        }

        try {
            appContext.assets.open(FILE_NAME).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()
                    when {
                        line.isEmpty() || line.startsWith("#") -> Unit
                        line.startsWith("@") -> {
                            flush()
                            val header = line.substring(1).split(' ', limit = 2)
                            icon = header[0]
                            name = header.getOrElse(1) { "" }
                        }
                        else -> line.split(' ')
                            .filter { it.isNotEmpty() && paint.hasGlyph(it) }
                            .forEach { if (it !in emojis) emojis.add(it) }
                    }
                }
            }
            flush()
        } catch (e: Exception) {
            Log.e(TAG, "Não foi possível carregar $FILE_NAME", e)
        }
        return result
    }

    companion object {
        private const val TAG = "EmojiRepository"
        private const val FILE_NAME = "emoji.txt"
        private const val PREFS_NAME = "poromongueta_emoji"
        private const val KEY_RECENTS = "recents"
        private const val SEPARATOR = " "
        private const val MAX_RECENTS = 32

        @Volatile
        private var instance: EmojiRepository? = null

        fun getInstance(context: Context): EmojiRepository =
            instance ?: synchronized(this) {
                instance ?: EmojiRepository(context.applicationContext).also { instance = it }
            }
    }
}
