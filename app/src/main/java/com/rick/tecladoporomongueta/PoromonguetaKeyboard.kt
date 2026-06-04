package com.rick.tecladoporomongueta

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class PoromonguetaKeyboard : InputMethodService(),
    KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var suggestionBar: SuggestionBarView
    private lateinit var keyboardPoro: Keyboard
    private lateinit var keyboardAbc: Keyboard
    private lateinit var keyboardSymbols: Keyboard
    private lateinit var keyboardSymbols2: Keyboard

    private var isShifted = false
    private var currentMode = Mode.PORO

    private val deleteHandler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private var deleteRunnable: Runnable? = null

    private var currentWord = ""

    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var learningManager: LearningManager
    private lateinit var suggestionManager: SuggestionManager

    enum class Mode {
        PORO, ABC, SYMBOLS
    }

    override fun onCreate() {
        super.onCreate()
        dictionaryManager = DictionaryManager(this)
        learningManager = LearningManager(this)
        suggestionManager = SuggestionManager(dictionaryManager, learningManager)
    }

    override fun onInitializeInterface() {
        keyboardPoro = Keyboard(this, R.xml.key_poro)
        keyboardAbc = Keyboard(this, R.xml.key_abc)
        keyboardSymbols = Keyboard(this, R.xml.key_symbols)
        keyboardSymbols2 = Keyboard(this, R.xml.key_symbols2)
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null) as View

        suggestionBar = root.findViewById(R.id.suggestionBar)
        keyboardView = root.findViewById(R.id.keyboard)

        keyboardView.keyboard = keyboardPoro
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        suggestionBar.setOnSuggestionClickListener { word ->
            applySuggestion(word)
        }

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentWord = ""
        suggestionBar.clearSuggestions()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        saveCurrentWord()
        currentWord = ""
        suggestionBar.clearSuggestions()
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {

            Keyboard.KEYCODE_DELETE -> {
                handleDelete(ic)
            }

            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                keyboardView.keyboard.isShifted = isShifted
                keyboardView.shiftActive = isShifted
                keyboardView.invalidateAllKeys()
            }

            Keyboard.KEYCODE_DONE -> {
                saveCurrentWord()
                currentWord = ""
                suggestionBar.clearSuggestions()
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

            1000 -> {
                val text = if (isShifted) "MB" else "mb"
                ic.commitText(text, 1)
                currentWord += text
                updateSuggestions()
            }
            1001 -> {
                val text = if (isShifted) "ND" else "nd"
                ic.commitText(text, 1)
                currentWord += text
                updateSuggestions()
            }
            1002 -> {
                val text = if (isShifted) "NG" else "ng"
                ic.commitText(text, 1)
                currentWord += text
                updateSuggestions()
            }

            else -> {
                val char = primaryCode.toChar()

                if (char == ' ') {
                    saveCurrentWord()
                    currentWord = ""
                    suggestionBar.clearSuggestions()
                    ic.commitText(" ", 1)
                    return
                }

                val isWordChar = Character.isLetter(char) || char == '\'' || char == '-'

                if (isWordChar) {
                    val text = if (isShifted) char.uppercaseChar() else char.lowercaseChar()
                    ic.commitText(text.toString(), 1)
                    currentWord += text
                    updateSuggestions()
                } else {
                    saveCurrentWord()
                    currentWord = ""
                    suggestionBar.clearSuggestions()
                    ic.commitText(char.toString(), 1)
                }
            }
        }
    }

    private fun handleDelete(ic: InputConnection) {
        if (currentWord.isNotEmpty()) {
            currentWord = currentWord.dropLast(1)
            ic.deleteSurroundingText(1, 0)
            if (currentWord.isEmpty()) {
                suggestionBar.clearSuggestions()
            } else {
                updateSuggestions()
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun applySuggestion(word: String) {
        val ic = currentInputConnection ?: return
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        ic.commitText("$word ", 1)
        learningManager.saveWord(word)
        currentWord = ""
        suggestionBar.clearSuggestions()
    }

    private fun saveCurrentWord() {
        if (currentWord.length >= 2) {
            learningManager.saveWord(currentWord)
        }
    }

    private fun updateSuggestions() {
        if (currentWord.length < 1) {
            suggestionBar.clearSuggestions()
            return
        }
        val suggestions = suggestionManager.getSuggestions(currentWord, 3)
        suggestionBar.setSuggestions(suggestions)
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
            if (currentWord.isNotEmpty()) {
                currentWord = currentWord.dropLast(1)
            }
            ic.deleteSurroundingText(1, 0)
            if (currentWord.isEmpty()) {
                suggestionBar.clearSuggestions()
            } else {
                updateSuggestions()
            }
        }
    }

    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
