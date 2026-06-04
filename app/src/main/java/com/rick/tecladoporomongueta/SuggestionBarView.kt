package com.rick.tecladoporomongueta

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val suggestion1: TextView
    private val suggestion2: TextView
    private val suggestion3: TextView

    private var onSuggestionClickListener: ((String) -> Unit)? = null

    private val suggestions = arrayOfNulls<String>(3)

    init {
        orientation = HORIZONTAL
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.suggestion_bar, this, true)

        suggestion1 = findViewById(R.id.suggestion1)
        suggestion2 = findViewById(R.id.suggestion2)
        suggestion3 = findViewById(R.id.suggestion3)

        suggestion1.setOnClickListener {
            suggestions[0]?.let { word ->
                onSuggestionClickListener?.invoke(word)
            }
        }
        suggestion2.setOnClickListener {
            suggestions[1]?.let { word ->
                onSuggestionClickListener?.invoke(word)
            }
        }
        suggestion3.setOnClickListener {
            suggestions[2]?.let { word ->
                onSuggestionClickListener?.invoke(word)
            }
        }
    }

    fun setSuggestions(suggestions: List<String>) {
        val views = listOf(suggestion1, suggestion2, suggestion3)
        for (i in 0 until 3) {
            this.suggestions[i] = null
            if (i < suggestions.size) {
                this.suggestions[i] = suggestions[i]
                views[i].text = suggestions[i]
                views[i].visibility = VISIBLE
            } else {
                views[i].text = ""
                views[i].visibility = INVISIBLE
            }
        }
    }

    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClickListener = listener
    }

    fun clearSuggestions() {
        setSuggestions(emptyList())
    }
}
