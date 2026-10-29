package com.rick.tecladoporomongueta

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Barra acima do teclado: botão de emoji, 3 sugestões e botão de voz. */
class SuggestionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val slots: List<TextView>
    private val emojiButton: View
    private val micButton: View

    private var onSuggestionClickListener: ((String) -> Unit)? = null

    private val suggestions = arrayOfNulls<String>(SLOT_COUNT)

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.suggestion_bar, this, true)

        slots = listOf(
            findViewById(R.id.suggestion1),
            findViewById(R.id.suggestion2),
            findViewById(R.id.suggestion3)
        )
        emojiButton = findViewById(R.id.btnEmoji)
        micButton = findViewById(R.id.btnMic)

        slots.forEach {
            it.maxLines = 1
            it.ellipsize = TextUtils.TruncateAt.END
        }

        slots.forEachIndexed { index, view ->
            view.setOnClickListener {
                suggestions[index]?.let { word -> onSuggestionClickListener?.invoke(word) }
            }
        }
    }

    fun setSuggestions(suggestions: List<String>) {
        for (i in 0 until SLOT_COUNT) {
            val word = suggestions.getOrNull(i)
            this.suggestions[i] = word
            slots[i].text = word ?: ""
            slots[i].visibility = if (word != null) VISIBLE else INVISIBLE
        }
    }

    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClickListener = listener
    }

    fun setOnEmojiClickListener(listener: () -> Unit) {
        emojiButton.setOnClickListener { listener() }
    }

    fun setOnMicClickListener(listener: () -> Unit) {
        micButton.setOnClickListener { listener() }
    }

    /** O microfone some em campos de senha, e-mail e endereço. */
    fun setMicVisible(visible: Boolean) {
        micButton.visibility = if (visible) VISIBLE else INVISIBLE
    }

    fun clearSuggestions() {
        setSuggestions(emptyList())
    }

    private companion object {
        const val SLOT_COUNT = 3
    }
}
