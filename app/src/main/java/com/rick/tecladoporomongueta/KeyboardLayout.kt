package com.rick.tecladoporomongueta

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** Medida de um layout de teclado: dp, pixels ou fração da largura ("12%p"). */
sealed class Dimension {
    data class Dp(val value: Float) : Dimension()
    data class Px(val value: Float) : Dimension()
    data class Fraction(val value: Float) : Dimension()

    fun resolve(totalWidth: Float, density: Float): Float = when (this) {
        is Dp -> value * density
        is Px -> value
        is Fraction -> value * totalWidth
    }

    companion object {
        private val PATTERN = Regex("^\\s*(-?[0-9]*\\.?[0-9]+)\\s*(dip|dp|px|sp|%p|%)?\\s*$")

        /** Aceita "44dp", "44.0dip", "12%p", "12.0%p" e "10px". */
        fun parse(text: String?, default: Dimension): Dimension {
            val match = PATTERN.matchEntire(text ?: return default) ?: return default
            val number = match.groupValues[1].toFloatOrNull() ?: return default
            return when (match.groupValues[2]) {
                "%p", "%" -> Fraction(number / 100f)
                "px" -> Px(number)
                else -> Dp(number)
            }
        }
    }
}

/** Uma tecla. A posição e o tamanho (em pixels) são calculados por [KeyboardLayout.layout]. */
class Key(
    val codes: IntArray,
    var label: String,
    val popupResId: Int,
    val repeatable: Boolean,
    internal val widthSpec: Dimension,
    internal val gapSpec: Dimension
) {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    val primaryCode: Int get() = codes.firstOrNull() ?: 0

    /** Teclas com vários códigos (como č̣) digitam todos os caracteres em sequência. */
    val isMultiCode: Boolean get() = codes.size > 1

    /** O texto digitado por uma tecla de vários códigos. */
    fun outputText(uppercase: Boolean = false): String {
        val text = buildString { codes.forEach { appendCodePoint(it) } }
        return if (uppercase) text.uppercase() else text
    }
}

class KeyboardRow(
    val keys: List<Key>,
    internal val heightSpec: Dimension,
    internal val verticalGapSpec: Dimension
)

/**
 * Teclado lido de um `res/xml/key_*.xml` ou `popup_*.xml` (o mesmo formato do antigo
 * `android.inputmethodservice.Keyboard`). Os arquivos de layout não mudam.
 */
class KeyboardLayout(private val rows: List<KeyboardRow>) {

    val keys: List<Key> = rows.flatMap { it.keys }

    /** Largura e altura do conteúdo, em pixels, depois de [layout]. */
    var contentWidth = 0f
        private set
    var contentHeight = 0f
        private set

    /**
     * Posiciona as teclas para uma área de [totalWidth] pixels. Como no teclado antigo,
     * o espaço entre as teclas vem depois de cada tecla e, se uma linha passar da largura,
     * as teclas dela encolhem proporcionalmente.
     */
    fun layout(totalWidth: Float, density: Float, heightScale: Float = 1f) {
        var y = 0f
        var maxRight = 0f
        var lastGap = 0f

        for (row in rows) {
            val rowHeight = row.heightSpec.resolve(totalWidth, density) * heightScale
            lastGap = row.verticalGapSpec.resolve(totalWidth, density)

            val widths = row.keys.map { it.widthSpec.resolve(totalWidth, density) }
            val gaps = row.keys.map { it.gapSpec.resolve(totalWidth, density) }
            val sumWidths = widths.sum()
            val sumGaps = gaps.dropLast(1).sum()
            val overflows = sumWidths > 0f && sumWidths + sumGaps > totalWidth
            val scale = if (overflows) ((totalWidth - sumGaps) / sumWidths).coerceAtLeast(0f) else 1f

            // Linha que passa da largura: encolhe e ocupa a tela toda, com o espaço depois de cada tecla.
            // Linha que cabe: mantém o tamanho, com o espaço antes de cada tecla (como o teclado antigo).
            var x = 0f
            row.keys.forEachIndexed { index, key ->
                if (!overflows) x += gaps[index]
                key.x = x
                key.y = y
                key.width = widths[index] * scale
                key.height = rowHeight
                x += key.width
                if (overflows) x += gaps[index]
            }
            maxRight = maxOf(maxRight, row.keys.lastOrNull()?.let { it.x + it.width } ?: 0f)
            y += rowHeight + lastGap
        }

        contentWidth = maxRight
        contentHeight = if (rows.isEmpty()) 0f else y - lastGap
    }

    /** A tecla mais próxima do ponto (x, y), se estiver perto o bastante de alguma. */
    fun nearestKey(x: Float, y: Float): Key? {
        var best: Key? = null
        var bestDistance = Float.MAX_VALUE
        for (key in keys) {
            val dx = maxOf(key.x - x, 0f, x - (key.x + key.width))
            val dy = maxOf(key.y - y, 0f, y - (key.y + key.height))
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = key
            }
        }
        val limit = (best?.height ?: 0f) * MAX_TOUCH_DISTANCE_FACTOR
        return if (best != null && bestDistance <= limit * limit) best else null
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val MAX_TOUCH_DISTANCE_FACTOR = 0.9f

        private val DEFAULT_WIDTH: Dimension = Dimension.Fraction(0.1f)
        private val DEFAULT_HEIGHT: Dimension = Dimension.Dp(50f)
        private val NO_GAP: Dimension = Dimension.Dp(0f)

        fun load(context: Context, resId: Int): KeyboardLayout {
            val parser = context.resources.getXml(resId)
            try {
                return parse(parser)
            } finally {
                parser.close()
            }
        }

        internal fun parse(parser: android.content.res.XmlResourceParser): KeyboardLayout {
            val attrs = Xml.asAttributeSet(parser)

            var keyWidth = DEFAULT_WIDTH
            var keyHeight = DEFAULT_HEIGHT
            var horizontalGap = NO_GAP
            var verticalGap = NO_GAP

            val rows = mutableListOf<KeyboardRow>()
            var rowKeys: MutableList<Key>? = null

            // A própria <Row> pode trocar os valores padrão das teclas dela.
            var rowKeyWidth = keyWidth
            var rowKeyHeight = keyHeight
            var rowHorizontalGap = horizontalGap
            var rowVerticalGap = verticalGap

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "Keyboard" -> {
                            keyWidth = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "keyWidth"), keyWidth)
                            keyHeight = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "keyHeight"), keyHeight)
                            horizontalGap = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "horizontalGap"), horizontalGap)
                            verticalGap = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "verticalGap"), verticalGap)
                        }
                        "Row" -> {
                            rowKeys = mutableListOf()
                            rowKeyWidth = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "keyWidth"), keyWidth)
                            rowKeyHeight = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "keyHeight"), keyHeight)
                            rowHorizontalGap = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "horizontalGap"), horizontalGap)
                            rowVerticalGap = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "verticalGap"), verticalGap)
                        }
                        "Key" -> rowKeys?.add(
                            Key(
                                codes = parseCodes(parser.getAttributeValue(ANDROID_NS, "codes")),
                                label = parser.getAttributeValue(ANDROID_NS, "keyLabel") ?: "",
                                popupResId = attrs.getAttributeResourceValue(ANDROID_NS, "popupKeyboard", 0),
                                repeatable = parser.getAttributeValue(ANDROID_NS, "isRepeatable") == "true",
                                widthSpec = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "keyWidth"), rowKeyWidth),
                                gapSpec = Dimension.parse(parser.getAttributeValue(ANDROID_NS, "horizontalGap"), rowHorizontalGap)
                            )
                        )
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "Row") {
                    rowKeys?.let { rows.add(KeyboardRow(it, rowKeyHeight, rowVerticalGap)) }
                    rowKeys = null
                }
                event = parser.next()
            }
            return KeyboardLayout(rows)
        }

        /** "49" -> [49]; "269,803" -> [269, 803]; aceita hexadecimal ("0x2022"). */
        internal fun parseCodes(text: String?): IntArray {
            if (text.isNullOrBlank()) return intArrayOf()
            return text.split(',').mapNotNull {
                val part = it.trim()
                if (part.startsWith("0x", ignoreCase = true)) part.substring(2).toIntOrNull(16)
                else part.toIntOrNull()
            }.toIntArray()
        }
    }
}
