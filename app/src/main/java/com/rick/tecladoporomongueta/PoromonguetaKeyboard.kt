package com.rick.tecladoporomongueta

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection

class PoromonguetaKeyboard : InputMethodService(),
    KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var keyboardPoro: Keyboard
    private lateinit var keyboardAbc: Keyboard
    private lateinit var keyboardSymbols: Keyboard
    private lateinit var keyboardSymbols2: Keyboard

    private var isShifted = false
    private var currentMode = Mode.PORO

    private val deleteHandler = Handler()
    private var isDeleting = false
    private var deleteRunnable: Runnable? = null

    enum class Mode {
        PORO, ABC, SYMBOLS
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(
            R.layout.keyboard_view,
            null
        ) as CustomKeyboardView

        keyboardPoro = Keyboard(this, R.xml.key_poro)
        keyboardAbc = Keyboard(this, R.xml.key_abc)
        keyboardSymbols = Keyboard(this, R.xml.key_symbols)
        keyboardSymbols2 = Keyboard(this, R.xml.key_symbols2)

        keyboardView.keyboard = keyboardPoro
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        return keyboardView
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {

            Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }

            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                keyboardView.keyboard.isShifted = isShifted
                keyboardView.shiftActive = isShifted //Converte para maiusculo se SHIFT estiver ativo
                keyboardView.invalidateAllKeys()
            }

            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            }

            -101 -> keyboardView.keyboard = keyboardSymbols2
            -100 -> keyboardView.keyboard = keyboardSymbols
            -10 -> {
                currentMode = Mode.ABC
                keyboardView.keyboard = keyboardAbc
            }
            -11 -> {
                currentMode = Mode.PORO
                keyboardView.keyboard = keyboardPoro
            }
            -12 -> {
                currentMode = Mode.SYMBOLS
                keyboardView.keyboard = keyboardSymbols
            }

            1000 -> ic.commitText(if (isShifted) "MB" else "mb", 1)
            1001 -> ic.commitText(if (isShifted) "ND" else "nd", 1)
            1002 -> ic.commitText(if (isShifted) "NG" else "ng", 1)

            else -> {
                val char = primaryCode.toChar()
                val text = if (isShifted) char.uppercaseChar() else char.lowercaseChar()
                ic.commitText(text.toString(), 1)
            }
        }
    }

    override fun onPress(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeleting = true
            deleteRunnable = Runnable {
                if (isDeleting) {
                    deleteRepeated()
                    deleteHandler.postDelayed(deleteRunnable!!, 100)
                }
            }
            deleteHandler.postDelayed(deleteRunnable!!, 400)
        }
    }

    override fun onRelease(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeleting = false
            deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
        }
    }

    private fun deleteRepeated() {
        val ic = currentInputConnection ?: return
        val text = ic.getTextBeforeCursor(1, 0)
        if (!text.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        }
    }

    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}