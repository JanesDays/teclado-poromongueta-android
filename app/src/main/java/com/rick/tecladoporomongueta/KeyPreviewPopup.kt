package com.rick.tecladoporomongueta

import android.content.Context
import android.inputmethodservice.Keyboard
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import android.widget.TextView

class KeyPreviewPopup(private val context: Context) {

    private val popupView: View = LayoutInflater.from(context)
        .inflate(R.layout.key_preview, null)

    private val textView: TextView = popupView.findViewById(R.id.preview_text)

    private val popupWindow: PopupWindow = PopupWindow(
        popupView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        isTouchable = false
        isOutsideTouchable = false
        elevation = 16f
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
    }

    private val density = context.resources.displayMetrics.density
    private val screenWidth = context.resources.displayMetrics.widthPixels

    fun show(anchor: View, key: Keyboard.Key, shiftActive: Boolean, rawX: Float, rawY: Float) {
        if (!anchor.isAttachedToWindow) return

        val label = resolveLabel(key, shiftActive) ?: return

        textView.text = label

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val animScale = 0.85f
        val marginPx = (10 * density).toInt()
        val edgePad = (4 * density).toInt()

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val rootLocation = IntArray(2)
        anchor.rootView.getLocationOnScreen(rootLocation)

        val keyScreenTop = anchorLocation[1] + anchor.paddingTop + key.y

        val popupY = keyScreenTop - popupHeight - marginPx - rootLocation[1]

        var popupX = (rawX - popupWidth / 2f).toInt() - rootLocation[0]
        if (popupX < edgePad) popupX = edgePad
        if (popupX + popupWidth > screenWidth - edgePad) {
            popupX = screenWidth - popupWidth - edgePad
        }

        popupWindow.showAtLocation(anchor, Gravity.TOP or Gravity.LEFT, popupX, popupY)

        popupView.apply {
            scaleX = 0.85f
            scaleY = 0.85f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun dismissWithAnimation() {
        if (!popupWindow.isShowing) return
        popupView.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(70)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                popupWindow.dismiss()
                popupView.apply {
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 1f
                }
            }
            .start()
    }

    fun dismissImmediately() {
        popupView.animate().cancel()
        popupWindow.dismiss()
        popupView.apply {
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
        }
    }

    val isShowing: Boolean get() = popupWindow.isShowing

    private fun resolveLabel(key: Keyboard.Key, shiftActive: Boolean): String? {
        if (key.codes.isEmpty()) return null
        val primaryCode = key.codes[0]

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE,
            Keyboard.KEYCODE_DONE,
            Keyboard.KEYCODE_SHIFT,
            32 -> return null
        }

        if (primaryCode == -10 || primaryCode == -11 || primaryCode == -12 ||
            primaryCode == -100 || primaryCode == -101) return null

        val label = key.label?.toString() ?: return null
        if (label.isEmpty()) return null

        return if (shiftActive && label.all { it.isLetter() }) {
            label.uppercase()
        } else {
            label
        }
    }
}
