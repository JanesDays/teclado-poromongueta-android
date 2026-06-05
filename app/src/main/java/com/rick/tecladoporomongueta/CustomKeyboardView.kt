package com.rick.tecladoporomongueta

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : KeyboardView(context, attrs) {

    private var popupWindow: PopupWindow? = null
    private var actionListener: OnKeyboardActionListener? = null
    private val keyPreviewPopup: KeyPreviewPopup by lazy { KeyPreviewPopup(context) }
    private var lastKeyCode: Int = Int.MIN_VALUE

    var shiftActive: Boolean = false

    override fun setOnKeyboardActionListener(listener: OnKeyboardActionListener?) {
        super.setOnKeyboardActionListener(listener)
        actionListener = listener
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        when (me.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = getKeyAt(me.x, me.y)
                if (key != null) {
                    lastKeyCode = key.codes[0]
                    keyPreviewPopup.show(this, key, shiftActive, me.rawX, me.rawY)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (keyPreviewPopup.isShowing) {
                    val key = getKeyAt(me.x, me.y)
                    if (key != null && key.codes[0] != lastKeyCode) {
                        lastKeyCode = key.codes[0]
                        keyPreviewPopup.show(this, key, shiftActive, me.rawX, me.rawY)
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                keyPreviewPopup.dismissWithAnimation()
                lastKeyCode = Int.MIN_VALUE
            }
        }
        return super.onTouchEvent(me)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        keyPreviewPopup.dismissImmediately()
    }

    private fun getKeyAt(x: Float, y: Float): Keyboard.Key? {
        val kbd = keyboard ?: return null
        val adjX = x - paddingLeft
        val adjY = y - paddingTop
        for (key in kbd.keys) {
            if (adjX >= key.x && adjX <= key.x + key.width &&
                adjY >= key.y && adjY <= key.y + key.height
            ) {
                return key
            }
        }
        return null
    }

    override fun onLongPress(key: Keyboard.Key?): Boolean {
        keyPreviewPopup.dismissImmediately()

        if (key != null && key.popupResId != 0) {

            popupWindow?.dismiss()

            val popupKeyboard = Keyboard(context, key.popupResId)

            //  Converte para maiúsculo se SHIFT estiver ativo
            if (shiftActive) {
                popupKeyboard.keys.forEach { popupKey ->

                    val originalLabel = popupKey.label?.toString()

                    if (!originalLabel.isNullOrEmpty()) {

                        val upper = originalLabel.uppercase()

                        popupKey.label = upper

                        if (upper.length == 1) {
                            popupKey.codes = intArrayOf(upper[0].code)
                        }
                    }
                }
            }


            val popupView = LayoutInflater.from(context)
                .inflate(R.layout.popup_keyboard_view, null) as KeyboardView

            popupView.keyboard = popupKeyboard
            popupView.isPreviewEnabled = false

            // Listener customizado para fechar ao clicar
            popupView.setOnKeyboardActionListener(object : OnKeyboardActionListener {

                override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                    actionListener?.onKey(primaryCode, keyCodes)
                    popupWindow?.dismiss()
                }

                override fun onPress(primaryCode: Int) {
                    actionListener?.onPress(primaryCode)
                }

                override fun onRelease(primaryCode: Int) {
                    actionListener?.onRelease(primaryCode)
                }

                override fun onText(text: CharSequence?) {
                    actionListener?.onText(text)
                    popupWindow?.dismiss()
                }

                override fun swipeLeft() {}
                override fun swipeRight() {}
                override fun swipeDown() {}
                override fun swipeUp() {}
            })

            popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            popupWindow?.isOutsideTouchable = true
            popupWindow?.elevation = 20f

            // Posiciona sobre a tecla pressionada
            val kbd = keyboard
            if (kbd != null && key.codes.isNotEmpty()) {
                popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val pw = popupView.measuredWidth
                val ph = popupView.measuredHeight
                val px = key.x + (key.width - pw) / 2
                val margin = (8 * resources.displayMetrics.density).toInt()
                val py = key.y - ph - margin
                popupWindow?.showAtLocation(this, Gravity.TOP or Gravity.LEFT, px, py)
            } else {
                popupWindow?.showAtLocation(this, Gravity.TOP or Gravity.LEFT, 0, 0)
            }

            return true
        }

        return super.onLongPress(key)
    }
}
