package com.rick.tecladoporomongueta

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View

/** Teclas alternativas que aparecem ao segurar uma tecla (ex.: a -> â, ã, á...). */
@SuppressLint("ViewConstructor")
class PopupKeysView(
    context: Context,
    private val layout: KeyboardLayout,
    private val painter: KeyPainter,
    private val shifted: Boolean,
    private val onKeySelected: (Key) -> Unit
) : View(context) {

    private val density = resources.displayMetrics.density
    private val inset = (6 * density).toInt()
    private val slack = 3 * density
    private var highlighted: Key? = null

    init {
        layout.layout(resources.displayMetrics.widthPixels.toFloat(), density)
        setPadding(inset, inset, inset, inset)
        setBackgroundResource(R.drawable.key_bg)
        setPadding(inset, inset, inset, inset)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            layout.contentWidth.toInt() + paddingLeft + paddingRight,
            layout.contentHeight.toInt() + paddingTop + paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        painter.draw(
            canvas, layout, setOfNotNull(highlighted), shifted,
            paddingLeft.toFloat(), paddingTop.toFloat()
        )
    }

    /** Destaca a tecla sob o ponto (em coordenadas da tela) e a devolve, se houver. */
    fun highlightAtScreen(screenX: Float, screenY: Float): Key? {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val key = layout.nearestKeyInside(
            screenX - location[0] - paddingLeft,
            screenY - location[1] - paddingTop,
            slack
        )
        if (key !== highlighted) {
            highlighted = key
            invalidate()
        }
        return key
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val key = layout.nearestKeyInside(event.x - paddingLeft, event.y - paddingTop, slack)
                if (key !== highlighted) {
                    highlighted = key
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val key = layout.nearestKeyInside(event.x - paddingLeft, event.y - paddingTop, slack)
                highlighted = null
                invalidate()
                key?.let(onKeySelected)
            }
            MotionEvent.ACTION_CANCEL -> {
                highlighted = null
                invalidate()
            }
        }
        return true
    }
}

/** A tecla que contém o ponto, com uma pequena folga ([slack], em pixels) nas bordas. */
internal fun KeyboardLayout.nearestKeyInside(x: Float, y: Float, slack: Float = 0f): Key? =
    keys.firstOrNull {
        x >= it.x - slack && x <= it.x + it.width + slack &&
            y >= it.y - slack && y <= it.y + it.height + slack
    }
