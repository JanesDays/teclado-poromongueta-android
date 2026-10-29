package com.rick.tecladoporomongueta

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Guarda no aparelho as palavras que o usuário digita e quantas vezes.
 * Os dados ficam em um arquivo criptografado (Android Keystore) e nunca saem do aparelho.
 * Só palavras de verdade são aprendidas (veja [WordPolicy]), e só são sugeridas depois
 * de [WordPolicy.MIN_USES_TO_SUGGEST] usos.
 */
class LearningManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = FrequencyStore(
        EncryptedTextStorage(appContext, FILE_NAME, KEY_ALIAS), MAX_WORDS
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val saveTask = Runnable { persistNow() }
    private var suggestibleCache: Map<String, Int>? = null

    init {
        migrateOldPreferences()
    }

    fun saveWord(word: String) {
        if (!WordPolicy.isLearnable(word)) return
        if (store.increment(TextNormalizer.lower(word))) {
            suggestibleCache = null
            scheduleSave()
        }
    }

    fun getFrequency(word: String): Int = store.get(TextNormalizer.lower(word))

    /** Palavras já usadas o bastante para aparecerem como sugestão. */
    fun getLearnedWords(): Map<String, Int> =
        suggestibleCache ?: store.entries()
            .filterValues { it >= WordPolicy.MIN_USES_TO_SUGGEST }
            .also { suggestibleCache = it }

    fun getTopLearned(limit: Int = 50): List<String> =
        getLearnedWords().entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    fun flush() {
        mainHandler.removeCallbacks(saveTask)
        persistNow()
    }

    fun clearAll() {
        mainHandler.removeCallbacks(saveTask)
        suggestibleCache = null
        store.clear()
    }

    private fun scheduleSave() {
        mainHandler.removeCallbacks(saveTask)
        mainHandler.postDelayed(saveTask, SAVE_DELAY_MS)
    }

    private fun persistNow() {
        val text = store.serialize()
        writer.execute { EncryptedTextStorage(appContext, FILE_NAME, KEY_ALIAS).write(text) }
    }

    /** Versões anteriores guardavam o histórico em texto puro nas SharedPreferences. */
    private fun migrateOldPreferences() {
        val prefs = appContext.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return
        prefs.all.forEach { (word, value) ->
            if (value is Int && value >= WordPolicy.MIN_USES_TO_SUGGEST && WordPolicy.isLearnable(word)) {
                repeat(value.coerceAtMost(MAX_MIGRATED_USES)) { store.increment(TextNormalizer.lower(word)) }
            }
        }
        store.save()
        prefs.edit().clear().commit()
    }

    companion object {
        private const val FILE_NAME = "learned_words.bin"
        private const val KEY_ALIAS = "poromongueta_learning_key"
        private const val OLD_PREFS_NAME = "poromongueta_learning"
        private const val MAX_WORDS = 2000
        private const val MAX_MIGRATED_USES = 50
        private const val SAVE_DELAY_MS = 2000L

        @Volatile
        private var instance: LearningManager? = null

        /** Instância única, para o teclado e as telas do app compartilharem o mesmo cache. */
        fun getInstance(context: Context): LearningManager =
            instance ?: synchronized(this) {
                instance ?: LearningManager(context.applicationContext).also { instance = it }
            }
    }
}
