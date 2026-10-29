package com.rick.tecladoporomongueta

import android.os.SystemClock

/**
 * Leva o texto reconhecido por voz da [VoiceInputActivity] de volta ao teclado.
 * O texto fica só na memória e vale por pouco tempo; o teclado o recolhe quando o campo
 * de texto volta a ser exibido.
 */
object VoiceBridge {

    private const val VALID_FOR_MS = 60_000L

    private var pending: String? = null
    private var deliveredAt = 0L

    @Synchronized
    fun deliver(text: String) {
        pending = text
        deliveredAt = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun take(): String? {
        val text = pending
        pending = null
        return if (text != null && SystemClock.elapsedRealtime() - deliveredAt < VALID_FOR_MS) text else null
    }
}
