package com.rick.tecladoporomongueta

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/** Desenha as teclas de um [KeyboardLayout]; compartilhado pelo teclado e pelos popups. */
class KeyPainter(
    background: Drawable,
    textColor: Int,
    private val keyTextSize: Float,
    private val labelTextSize: Float
) {

    private val keyBackground: Drawable = background.constantState?.newDrawable()?.mutate() ?: background
    private val padding = Rect().also { keyBackground.getPadding(it) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
    }

    /** [offsetX] e [offsetY] deslocam o desenho (o preenchimento da view). */
    fun draw(
        canvas: Canvas,
        layout: KeyboardLayout,
        pressed: Set<Key>,
        shifted: Boolean,
        offsetX: Float,
        offsetY: Float
    ) {
        for (key in layout.keys) {
            val left = (key.x + offsetX).toInt()
            val top = (key.y + offsetY).toInt()
            val right = (key.x + key.width + offsetX).toInt()
            val bottom = (key.y + key.height + offsetY).toInt()

            keyBackground.setBounds(left, top, right, bottom)
            keyBackground.state = if (key in pressed) PRESSED_STATE else EMPTY_STATE
            keyBackground.draw(canvas)

            val label = displayLabel(key, shifted)
            if (label.isEmpty()) continue

            if (label.length > 1 && key.codes.size < 2) {
                paint.textSize = labelTextSize
                paint.typeface = Typeface.DEFAULT_BOLD
            } else {
                paint.textSize = keyTextSize
                paint.typeface = Typeface.DEFAULT
            }
            val centerX = (left + padding.left + right - padding.right) / 2f
            val centerY = (top + padding.top + bottom - padding.bottom) / 2f
            val metrics = paint.fontMetrics
            canvas.drawText(label, centerX, centerY - (metrics.ascent + metrics.descent) / 2f, paint)
        }
    }

    companion object {
        private val PRESSED_STATE = intArrayOf(android.R.attr.state_pressed)
        private val EMPTY_STATE = intArrayOf()

        /** Com Shift, rótulos curtos em minúsculas aparecem em maiúsculas (nd -> ND). */
        fun displayLabel(key: Key, shifted: Boolean): String {
            val label = key.label
            return if (shifted && label.isNotEmpty() && label.length < 3 && label[0].isLowerCase()) {
                label.uppercase()
            } else {
                label
            }
        }
    }
}
