package com.rick.tecladoporomongueta

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/** Preferências do teclado, guardadas só neste aparelho. */
class KeyboardSettings private constructor(private val prefs: SharedPreferences) {

    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    /** Altura das teclas em porcentagem do tamanho normal. */
    var heightPercent: Int
        get() = prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT).coerceIn(MIN_HEIGHT, MAX_HEIGHT)
        set(value) = prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(MIN_HEIGHT, MAX_HEIGHT)).apply()

    /** [THEME_SYSTEM], [THEME_LIGHT] ou [THEME_DARK]. */
    var theme: Int
        get() = prefs.getInt(KEY_THEME, THEME_DARK).takeIf { it in THEME_SYSTEM..THEME_DARK } ?: THEME_DARK
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

    val nightMode: Int
        get() = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    val heightScale: Float get() = heightPercent / 100f

    companion object {
        const val MIN_HEIGHT = 80
        const val MAX_HEIGHT = 130
        const val DEFAULT_HEIGHT = 100

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        private const val FILE = "keyboard_settings"
        private const val KEY_SUGGESTIONS = "suggestions"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_HEIGHT = "height_percent"
        private const val KEY_THEME = "theme"

        fun get(context: Context) = KeyboardSettings(
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        )
    }
}
