package com.rick.tecladoporomongueta

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class PoromonguetaKeyboard : InputMethodService(), PoroKeyboardView.Listener {

    private lateinit var keyboardView: PoroKeyboardView
    private lateinit var suggestionBar: SuggestionBarView
    private lateinit var emojiPanel: EmojiPanelView
    private lateinit var keyboardPoro: KeyboardLayout
    private lateinit var keyboardAbc: KeyboardLayout
    private lateinit var keyboardSymbols: KeyboardLayout
    private lateinit var keyboardSymbols2: KeyboardLayout

    private var shiftState = ShiftState.OFF
    private var lastShiftTap = 0L
    private var currentMode = Mode.PORO

    private var currentWord = ""

    /** A palavra que vem antes de [currentWord] (separada só por espaços); vazia se não houver. */
    private var contextWord = ""

    // Campos de senha, e-mail e URL não são acompanhados, nem geram sugestões ou aprendizado.
    private var trackingEnabled = true
    private var suggestionsEnabled = true
    private var learningEnabled = true
    private var voiceEnabled = true

    private lateinit var settings: KeyboardSettings

    /** Se o teclado atual foi montado no tema escuro; null antes de montar. */
    private var viewNight: Boolean? = null

    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var learningManager: LearningManager
    private lateinit var nextWordManager: NextWordManager
    private lateinit var userDictionary: UserDictionary
    private lateinit var suggestionManager: SuggestionManager

    enum class Mode {
        PORO, ABC, SYMBOLS
    }

    private enum class ShiftState {
        OFF, ONCE, LOCK
    }

    override fun onCreate() {
        super.onCreate()
        settings = KeyboardSettings.get(this)
        dictionaryManager = DictionaryManager.fromAssets(this)
        learningManager = LearningManager.getInstance(this)
        nextWordManager = NextWordManager.getInstance(this)
        userDictionary = UserDictionary.getInstance(this)
        suggestionManager = SuggestionManager(
            dictionaryManager,
            userWords = { prefix, max -> userDictionary.search(prefix, max) },
            learnedWords = { learningManager.getLearnedWords() }
        )
    }

    override fun onInitializeInterface() {
        keyboardPoro = KeyboardLayout.load(this, R.xml.key_poro)
        keyboardAbc = KeyboardLayout.load(this, R.xml.key_abc)
        keyboardSymbols = KeyboardLayout.load(this, R.xml.key_symbols)
        keyboardSymbols2 = KeyboardLayout.load(this, R.xml.key_symbols2)
    }

    override fun onCreateInputView(): View {
        val night = isNight()
        viewNight = night
        val root = LayoutInflater.from(themedContext(night)).inflate(R.layout.keyboard_view, null) as View

        suggestionBar = root.findViewById(R.id.suggestionBar)
        keyboardView = root.findViewById(R.id.keyboard)
        emojiPanel = root.findViewById(R.id.emojiPanel)

        keyboardView.keyboard = keyboardPoro
        keyboardView.listener = this

        suggestionBar.setOnSuggestionClickListener { word -> applySuggestion(word) }
        suggestionBar.setOnEmojiClickListener { toggleEmojiPanel() }
        suggestionBar.setOnMicClickListener { startVoiceInput() }

        emojiPanel.setup(EmojiRepository.getInstance(this))
        emojiPanel.onEmojiSelected = { emoji -> insertEmoji(emoji) }
        emojiPanel.onBack = { hideEmojiPanel() }
        emojiPanel.onDelete = { currentInputConnection?.let { handleDelete(it) } }

        // A barra de navegação do sistema pode cobrir a parte de baixo do teclado.
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottom = maxOf(navigation + dp(12), dp(MIN_BOTTOM_DP))
            if (keyboardView.paddingBottom != bottom) {
                keyboardView.setPadding(
                    keyboardView.paddingLeft, keyboardView.paddingTop, keyboardView.paddingRight, bottom
                )
                emojiPanel.setBottomInset(bottom)
            }
            insets
        }

        applyShiftState()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (viewNight != null && viewNight != isNight()) {
            currentMode = Mode.PORO
            setInputView(onCreateInputView())
        }
        // Ícones escuros na barra do sistema quando o teclado está no tema claro.
        window.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightNavigationBars = !isNight()
        }
        configureForField(info)
        keyboardView.heightScale = settings.heightScale
        hideEmojiPanel()
        currentWord = ""
        contextWord = ""
        suggestionBar.setMicVisible(voiceEnabled)
        clearSuggestions()

        shiftState = ShiftState.OFF
        currentInputConnection?.let { ic ->
            VoiceBridge.take()?.let { if (voiceEnabled) commitVoiceText(ic, it) }
            updateAutoShift(ic)
        }
        applyShiftState()
        syncCurrentWord()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (::keyboardView.isInitialized) keyboardView.dismissPopups()
        if (::emojiPanel.isInitialized) hideEmojiPanel()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        saveCurrentWord()
        currentWord = ""
        contextWord = ""
        clearSuggestions()
        learningManager.flush()
        nextWordManager.flush()
    }

    /** Mantém [currentWord] fiel ao texto quando o cursor muda por fora do teclado. */
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        if (newSelStart != newSelEnd) {
            currentWord = ""
            contextWord = ""
            clearSuggestions()
            return
        }
        syncCurrentWord()
    }

    private fun syncCurrentWord() {
        if (!trackingEnabled) return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(TEXT_WINDOW, 0)?.toString() ?: return

        val word = before.takeLastWhile { TextNormalizer.isWordChar(it) }
        val beforeWord = before.dropLast(word.length)
        val context = if (beforeWord.endsWith(' ')) {
            beforeWord.trimEnd(' ').takeLastWhile { TextNormalizer.isWordChar(it) }
        } else {
            ""
        }

        if (word != currentWord || context != contextWord) {
            currentWord = word
            contextWord = context
            updateSuggestions()
        }
    }

    private fun configureForField(info: EditorInfo?) {
        val type = info?.inputType ?: 0
        val typeClass = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION

        val isText = typeClass == InputType.TYPE_CLASS_TEXT
        val isPassword = when (typeClass) {
            InputType.TYPE_CLASS_TEXT ->
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER ->
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
        val isPrivateField = isPassword || (isText && (
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_URI
            ))
        val noSuggestions = type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
        val incognito = (info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

        trackingEnabled = !isPrivateField
        suggestionsEnabled = settings.suggestionsEnabled && isText && !isPrivateField && !noSuggestions
        learningEnabled = isText && !isPrivateField && !incognito
        voiceEnabled = !isPrivateField && typeClass != 0
    }

    override fun onKey(code: Int) {
        val ic = currentInputConnection ?: return

        when (code) {

            KeyCodes.DELETE -> handleDelete(ic)

            KeyCodes.SHIFT -> toggleShift()

            KeyCodes.DONE -> {
                finishWord()
                handleEnter(ic)
                updateAutoShift(ic)
                applyShiftState()
            }

            KeyCodes.SYMBOLS_PAGE_2 -> setKeyboard(keyboardSymbols2)
            KeyCodes.SYMBOLS_PAGE_1 -> setKeyboard(keyboardSymbols)
            KeyCodes.MODE_ABC -> {
                currentMode = Mode.ABC
                setKeyboard(keyboardAbc)
            }
            KeyCodes.MODE_PORO -> {
                currentMode = Mode.PORO
                setKeyboard(keyboardPoro)
            }
            KeyCodes.MODE_SYMBOLS -> {
                currentMode = Mode.SYMBOLS
                setKeyboard(keyboardSymbols)
            }

            KeyCodes.DIGRAPH_MB -> typeWordText(ic, if (isShifted()) "MB" else "mb")
            KeyCodes.DIGRAPH_ND -> typeWordText(ic, if (isShifted()) "ND" else "nd")
            KeyCodes.DIGRAPH_NG -> typeWordText(ic, if (isShifted()) "NG" else "ng")

            else -> {
                val char = code.toChar()

                if (char == ' ') {
                    finishWord()
                    ic.commitText(" ", 1)
                    afterTextTyped(ic)
                    return
                }

                if (TextNormalizer.isWordChar(char)) {
                    val text = if (isShifted()) char.uppercaseChar() else char.lowercaseChar()
                    typeWordText(ic, text.toString())
                } else {
                    finishWord()
                    contextWord = ""
                    ic.commitText(char.toString(), 1)
                    afterTextTyped(ic)
                }
            }
        }
    }

    /** Teclas que digitam vários caracteres de uma vez (como č̣). */
    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        if (text.all { TextNormalizer.isWordChar(it) }) {
            typeWordText(ic, text)
        } else {
            finishWord()
            ic.commitText(text, 1)
            afterTextTyped(ic)
        }
    }

    private fun typeWordText(ic: InputConnection, text: String) {
        ic.commitText(text, 1)
        if (trackingEnabled) {
            currentWord += text
            updateSuggestions()
        }
        afterTextTyped(ic)
    }

    private fun handleEnter(ic: InputConnection) {
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val hasAction = options and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED

        if (hasAction) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        contextWord = ""
    }

    private fun handleDelete(ic: InputConnection) {
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            ic.commitText("", 1)
            currentWord = ""
            clearSuggestions()
        } else {
            val count = lettersToDelete(ic)
            ic.deleteSurroundingTextInCodePoints(count, 0)
            currentWord = currentWord.dropLast(count)
            updateSuggestions()
        }
        updateAutoShift(ic)
        applyShiftState()
    }

    /** Uma letra com acentos soltos (como č̣ = c + ̌ + ̣) é apagada de uma vez. */
    private fun lettersToDelete(ic: InputConnection): Int {
        val before = ic.getTextBeforeCursor(MAX_MARKS, 0) ?: return 1
        var count = 1
        var index = before.length
        while (index > 0 && isCombiningMark(before[index - 1])) {
            count++
            index--
        }
        if (index == 0 && count > 1) count--
        return count
    }

    private fun isCombiningMark(char: Char) =
        Character.getType(char) == Character.NON_SPACING_MARK.toInt()

    private fun applySuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val typed = currentWord
        if (typed.isNotEmpty()) {
            ic.deleteSurroundingText(typed.length, 0)
        }
        ic.commitText("${matchCapitalization(typed, word)} ", 1)
        if (learningEnabled) {
            learningManager.saveWord(word)
            if (contextWord.isNotEmpty()) nextWordManager.record(contextWord, word)
        }
        contextWord = word
        currentWord = ""
        updateSuggestions()
        afterTextTyped(ic)
    }

    /** Mantém a caixa do que foi digitado: "Ana" -> "Anaûé", "ANA" -> "ANAÛÉ". */
    private fun matchCapitalization(typed: String, word: String): String = when {
        typed.length > 1 && typed.none { it.isLowerCase() } -> word.uppercase()
        typed.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
        else -> word
    }

    private fun finishWord() {
        saveCurrentWord()
        if (currentWord.length >= WordPolicy.MIN_LENGTH) contextWord = currentWord
        currentWord = ""
        clearSuggestions()
    }

    private fun saveCurrentWord() {
        if (!learningEnabled || currentWord.length < WordPolicy.MIN_LENGTH) return
        learningManager.saveWord(currentWord)
        if (contextWord.isNotEmpty()) nextWordManager.record(contextWord, currentWord)
    }

    private fun updateSuggestions() {
        if (!suggestionsEnabled) {
            clearSuggestions()
            return
        }
        val suggestions = if (currentWord.isEmpty()) {
            if (contextWord.isEmpty()) emptyList() else nextWordManager.nextWords(contextWord, 3)
        } else {
            suggestionManager.getSuggestions(currentWord, 3)
        }
        suggestionBar.setSuggestions(suggestions)
    }

    private fun isNight(): Boolean = when (settings.theme) {
        KeyboardSettings.THEME_DARK -> true
        KeyboardSettings.THEME_LIGHT -> false
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** Contexto com o tema (claro ou escuro) escolhido, independente do tema do sistema. */
    private fun themedContext(night: Boolean): Context {
        val config = Configuration(resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return ContextThemeWrapper(createConfigurationContext(config), R.style.Theme_TecladoPoromongueta)
    }

    private fun clearSuggestions() {
        if (::suggestionBar.isInitialized) suggestionBar.clearSuggestions()
    }

    // --- Shift: toque simples (uma letra), toque duplo (Caps Lock) e maiúscula automática ---

    private fun isShifted() = shiftState != ShiftState.OFF

    private fun toggleShift() {
        val now = SystemClock.uptimeMillis()
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE ->
                if (now - lastShiftTap < DOUBLE_TAP_MS) ShiftState.LOCK else ShiftState.OFF
            ShiftState.LOCK -> ShiftState.OFF
        }
        lastShiftTap = now
        applyShiftState()
    }

    private fun afterTextTyped(ic: InputConnection) {
        if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
        updateAutoShift(ic)
        applyShiftState()
    }

    private fun updateAutoShift(ic: InputConnection) {
        if (shiftState != ShiftState.OFF) return
        val inputType = currentInputEditorInfo?.inputType ?: return
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return
        if (ic.getCursorCapsMode(inputType) != 0) shiftState = ShiftState.ONCE
    }

    private fun applyShiftState() {
        if (!::keyboardView.isInitialized) return
        val shifted = isShifted()
        val shiftLabel = if (shiftState == ShiftState.LOCK) "⇪" else "⇧"

        for (layout in listOf(keyboardPoro, keyboardAbc)) {
            layout.keys
                .filter { it.primaryCode == KeyCodes.SHIFT }
                .forEach { it.label = shiftLabel }
        }
        keyboardView.shiftActive = shifted
        keyboardView.invalidateAllKeys()
    }

    private fun setKeyboard(layout: KeyboardLayout) {
        keyboardView.keyboard = layout
        applyShiftState()
    }

    // --- Emoji ---

    private fun toggleEmojiPanel() {
        if (emojiPanel.visibility == View.VISIBLE) hideEmojiPanel() else showEmojiPanel()
    }

    private fun showEmojiPanel() {
        val height = keyboardView.height
        emojiPanel.layoutParams = emojiPanel.layoutParams.apply {
            this.height = if (height > 0) height else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        }
        keyboardView.dismissPopups()
        keyboardView.visibility = View.GONE
        emojiPanel.visibility = View.VISIBLE
        emojiPanel.show()
    }

    private fun hideEmojiPanel() {
        if (!::emojiPanel.isInitialized || emojiPanel.visibility != View.VISIBLE) return
        emojiPanel.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
    }

    private fun insertEmoji(emoji: String) {
        val ic = currentInputConnection ?: return
        finishWord()
        ic.commitText(emoji, 1)
        afterTextTyped(ic)
    }

    // --- Voz ---

    /** Usa o reconhecimento de voz do sistema; o app não pede permissão de microfone. */
    private fun startVoiceInput() {
        val recognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        if (recognizer.resolveActivity(packageManager) == null) {
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        startActivity(
            Intent(this, VoiceInputActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun commitVoiceText(ic: InputConnection, text: String) {
        val before = ic.getTextBeforeCursor(1, 0)
        val needsSpace = !before.isNullOrEmpty() && !before.last().isWhitespace()
        ic.commitText(if (needsSpace) " $text" else text, 1)
    }

    // --- Callbacks do teclado ---

    override fun onPress(code: Int) {
        if (::keyboardView.isInitialized && settings.vibrationEnabled) {
            keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    override fun onRelease(code: Int) {}

    /** Segurar o espaço mostra os teclados instalados, como o Gboard. */
    override fun onSpaceLongPress() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MIN_BOTTOM_DP = 28
        private const val DOUBLE_TAP_MS = 400L
        private const val TEXT_WINDOW = 96
        private const val MAX_MARKS = 8
    }
}
