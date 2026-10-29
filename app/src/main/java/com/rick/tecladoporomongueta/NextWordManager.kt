package com.rick.tecladoporomongueta

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Previsão da próxima palavra: lembra, só neste aparelho, quais palavras o usuário costuma
 * digitar depois de outra. Usa o mesmo tipo de armazenamento criptografado do aprendizado.
 */
class NextWordManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = FrequencyStore(
        EncryptedTextStorage(appContext, FILE_NAME, KEY_ALIAS), MAX_PAIRS
    )
    private val predictor = NextWordPredictor(store)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val saveTask = Runnable { persistNow() }

    fun record(previous: String, next: String) {
        if (predictor.record(previous, next)) {
            mainHandler.removeCallbacks(saveTask)
            mainHandler.postDelayed(saveTask, SAVE_DELAY_MS)
        }
    }

    fun nextWords(previous: String, maxResults: Int = 3): List<String> =
        predictor.nextWords(previous, maxResults)

    fun flush() {
        mainHandler.removeCallbacks(saveTask)
        persistNow()
    }

    fun clearAll() {
        mainHandler.removeCallbacks(saveTask)
        store.clear()
    }

    private fun persistNow() {
        val text = store.serialize()
        writer.execute { EncryptedTextStorage(appContext, FILE_NAME, KEY_ALIAS).write(text) }
    }

    companion object {
        private const val FILE_NAME = "next_words.bin"
        private const val KEY_ALIAS = "poromongueta_learning_key"
        private const val MAX_PAIRS = 3000
        private const val SAVE_DELAY_MS = 2000L

        @Volatile
        private var instance: NextWordManager? = null

        fun getInstance(context: Context): NextWordManager =
            instance ?: synchronized(this) {
                instance ?: NextWordManager(context.applicationContext).also { instance = it }
            }
    }
}
