package com.rick.tecladoporomongueta

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : KeyboardView(context, attrs) {

    private var popupWindow: PopupWindow? = null
    private var actionListener: OnKeyboardActionListener? = null

    var shiftActive: Boolean = false

    override fun setOnKeyboardActionListener(listener: OnKeyboardActionListener?) {
        super.setOnKeyboardActionListener(listener)
        actionListener = listener
    }

    override fun onLongPress(key: Keyboard.Key?): Boolean {

        if (key != null && key.popupResId != 0) {

            // Fecha popup anterior
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

            // Abre (estável)
            popupWindow?.showAtLocation(this, 0, 0, 0)

            return true
        }

        return super.onLongPress(key)
    }
}
