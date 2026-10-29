package com.rick.tecladoporomongueta

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat

/**
 * Teclado desenhado diretamente em Canvas (substitui o antigo `KeyboardView`, obsoleto).
 * Suporta vários dedos, prévia da tecla, apagar contínuo e popups de teclas alternativas
 * (toque após abrir, ou deslize até a tecla e solte).
 */
class PoroKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onKey(code: Int)
        fun onText(text: String)
        fun onPress(code: Int)
        fun onRelease(code: Int)

        /** Segurar a barra de espaço: o teclado oferece trocar de teclado, como o Gboard. */
        fun onSpaceLongPress() {}
    }

    private class Pointer(val id: Int, var key: Key) {
        var consumed = false
        var repeatCount = 0
        var popupView: PopupKeysView? = null
        var longPress: Runnable? = null
        var repeat: Runnable? = null
    }

    var listener: Listener? = null

    var shiftActive: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var keyboard: KeyboardLayout? = null
        set(value) {
            field = value
            cancelAllPointers()
            requestLayout()
            invalidate()
        }

    /** Multiplicador da altura das teclas (configuração do usuário). */
    var heightScale = 1f
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
            invalidate()
        }

    private val painter: KeyPainter
    private val popupPainter: KeyPainter
    private val pointers = SparseArray<Pointer>()
    private val popupLayouts = HashMap<Int, KeyboardLayout>()
    private val previewPopup: KeyPreviewPopup by lazy { KeyPreviewPopup(context) }
    private val density = resources.displayMetrics.density

    private var popupWindow: PopupWindow? = null
    private var previewOwner = -1

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.PoroKeyboardView)
        val background: Drawable = array.getDrawable(R.styleable.PoroKeyboardView_poroKeyBackground)
            ?: ColorDrawable(Color.DKGRAY)
        val textColor = array.getColor(R.styleable.PoroKeyboardView_poroKeyTextColor, Color.WHITE)
        val keyTextSize = array.getDimension(R.styleable.PoroKeyboardView_poroKeyTextSize, 20 * scaledDensity())
        val labelTextSize = array.getDimension(R.styleable.PoroKeyboardView_poroLabelTextSize, 14 * scaledDensity())
        array.recycle()

        painter = KeyPainter(background, textColor, keyTextSize, labelTextSize)
        popupPainter = KeyPainter(
            ContextCompat.getDrawable(context, R.drawable.key_bg) ?: background,
            textColor, 22 * scaledDensity(), labelTextSize
        )
    }

    private fun scaledDensity() = resources.displayMetrics.scaledDensity

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> resources.displayMetrics.widthPixels
            else -> MeasureSpec.getSize(widthMeasureSpec)
        }
        val layout = keyboard
        layout?.layout((width - paddingLeft - paddingRight).toFloat(), density, heightScale)
        val height = (layout?.contentHeight ?: 0f).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val layout = keyboard ?: return
        val pressed = HashSet<Key>()
        for (i in 0 until pointers.size()) pressed.add(pointers.valueAt(i).key)
        painter.draw(canvas, layout, pressed, shiftActive, paddingLeft.toFloat(), paddingTop.toFloat())
    }

    fun invalidateAllKeys() = invalidate()

    // --- Toque ---

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                pointerDown(event.getPointerId(index), event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.pointerCount) {
                    pointerMove(event.getPointerId(index), event.getX(index), event.getY(index))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                pointerUp(event.getPointerId(index), event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_CANCEL -> cancelAllPointers()
        }
        return true
    }

    private fun pointerDown(id: Int, x: Float, y: Float) {
        val key = keyboard?.nearestKey(x - paddingLeft, y - paddingTop) ?: return
        pointers.get(id)?.let { cancelTimers(it) }
        val pointer = Pointer(id, key)
        pointers.put(id, pointer)
        press(pointer, x)
        invalidate()
    }

    private fun pointerMove(id: Int, x: Float, y: Float) {
        val pointer = pointers.get(id) ?: return

        pointer.popupView?.let {
            it.highlightAtScreen(screenX(x), screenY(y))
            return
        }

        val key = keyboard?.nearestKey(x - paddingLeft, y - paddingTop) ?: return
        if (key !== pointer.key) {
            cancelTimers(pointer)
            listener?.onRelease(pointer.key.primaryCode)
            pointer.key = key
            pointer.consumed = false
            pointer.repeatCount = 0
            press(pointer, x)
            invalidate()
        }
    }

    private fun pointerUp(id: Int, x: Float, y: Float) {
        val pointer = pointers.get(id) ?: return
        cancelTimers(pointer)
        pointers.remove(id)

        val popup = pointer.popupView
        if (popup != null) {
            // Soltou sobre uma tecla do popup: usa. Senão o popup continua aberto para um toque.
            popup.highlightAtScreen(screenX(x), screenY(y))?.let {
                selectPopupKey(it)
                dismissPopupWindow()
            }
        } else if (!pointer.consumed) {
            fire(pointer.key)
        }

        listener?.onRelease(pointer.key.primaryCode)
        if (previewOwner == id) {
            previewPopup.dismissWithAnimation()
            previewOwner = -1
        }
        invalidate()
    }

    private fun press(pointer: Pointer, x: Float) {
        val key = pointer.key
        listener?.onPress(key.primaryCode)
        previewOwner = pointer.id
        previewPopup.show(this, key, shiftActive, screenX(x), 0f)
        scheduleTimers(pointer)
    }

    private fun fire(key: Key) {
        if (key.isMultiCode) {
            listener?.onText(key.outputText(shiftActive))
        } else {
            listener?.onKey(key.primaryCode)
        }
    }

    // --- Apagar contínuo e toque longo ---

    private fun scheduleTimers(pointer: Pointer) {
        val key = pointer.key
        if (key.repeatable || key.primaryCode == KeyCodes.DELETE) {
            val repeat = object : Runnable {
                override fun run() {
                    if (pointers.get(pointer.id) !== pointer || pointer.key !== key) return
                    pointer.consumed = true
                    listener?.onKey(key.primaryCode)
                    pointer.repeatCount++
                    postDelayed(
                        this,
                        if (pointer.repeatCount > FAST_REPEAT_AFTER) FAST_REPEAT_MS else REPEAT_MS
                    )
                }
            }
            pointer.repeat = repeat
            postDelayed(repeat, REPEAT_START_MS)
        } else if (key.primaryCode == KeyCodes.SPACE) {
            val longPress = Runnable {
                if (pointers.get(pointer.id) === pointer && pointer.key === key) {
                    pointer.consumed = true
                    dismissPopups()
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    listener?.onSpaceLongPress()
                }
            }
            pointer.longPress = longPress
            postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
        } else if (key.popupResId != 0) {
            val longPress = Runnable {
                if (pointers.get(pointer.id) === pointer && pointer.key === key) openPopup(pointer, key)
            }
            pointer.longPress = longPress
            postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
        }
    }

    private fun cancelTimers(pointer: Pointer) {
        pointer.longPress?.let { removeCallbacks(it) }
        pointer.repeat?.let { removeCallbacks(it) }
        pointer.longPress = null
        pointer.repeat = null
    }

    private fun cancelAllPointers() {
        for (i in 0 until pointers.size()) {
            val pointer = pointers.valueAt(i)
            cancelTimers(pointer)
            listener?.onRelease(pointer.key.primaryCode)
        }
        pointers.clear()
        previewPopup.dismissImmediately()
        previewOwner = -1
        dismissPopupWindow()
        invalidate()
    }

    // --- Popup de teclas alternativas ---

    private fun openPopup(pointer: Pointer, key: Key) {
        val layout = popupLayouts.getOrPut(key.popupResId) { KeyboardLayout.load(context, key.popupResId) }
        if (layout.keys.isEmpty()) return

        dismissPopupWindow()
        previewPopup.dismissImmediately()
        previewOwner = -1
        pointer.consumed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        val view = PopupKeysView(context, layout, popupPainter, shiftActive) { selected ->
            selectPopupKey(selected)
            dismissPopupWindow()
        }
        view.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val window = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        window.isOutsideTouchable = true
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.elevation = 20f
        window.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        window.setOnDismissListener {
            pointer.popupView = null
            if (popupWindow === window) popupWindow = null
        }

        val location = IntArray(2)
        getLocationOnScreen(location)
        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)

        val margin = (8 * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val keyLeft = location[0] + paddingLeft + key.x
        var x = (keyLeft + (key.width - view.measuredWidth) / 2f).toInt()
        x = x.coerceIn(margin, (screenWidth - view.measuredWidth - margin).coerceAtLeast(margin))
        val y = (location[1] + paddingTop + key.y - view.measuredHeight - margin).toInt()

        pointer.popupView = view
        popupWindow = window
        window.showAtLocation(this, Gravity.TOP or Gravity.LEFT, x - rootLocation[0], y - rootLocation[1])
    }

    private fun selectPopupKey(key: Key) {
        if (key.isMultiCode) {
            listener?.onText(key.outputText(shiftActive))
            return
        }
        var code = key.primaryCode
        if (shiftActive && key.label.isNotEmpty()) {
            val upper = key.label.uppercase()
            if (upper.length == 1) code = upper[0].code
        }
        listener?.onKey(code)
    }

    private fun dismissPopupWindow() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    /** Fecha o preview da tecla e o popup de teclas alternativas, se estiverem abertos. */
    fun dismissPopups() {
        previewPopup.dismissImmediately()
        previewOwner = -1
        dismissPopupWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllPointers()
        dismissPopups()
    }

    private fun screenX(x: Float): Float {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[0] + x
    }

    private fun screenY(y: Float): Float {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1] + y
    }

    private companion object {
        const val REPEAT_START_MS = 400L
        const val REPEAT_MS = 100L
        const val FAST_REPEAT_MS = 50L
        const val FAST_REPEAT_AFTER = 8
    }
}
