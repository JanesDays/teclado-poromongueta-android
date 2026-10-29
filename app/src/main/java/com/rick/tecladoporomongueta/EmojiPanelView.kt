package com.rick.tecladoporomongueta

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Painel de emoji que ocupa o lugar do teclado: abas de categoria, grade, ABC e apagar. */
class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onEmojiSelected: ((String) -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var onDelete: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val tabsRow = LinearLayout(context)
    private val grid = RecyclerView(context)
    private val adapter = EmojiAdapter { emoji ->
        repository?.addRecent(emoji)
        onEmojiSelected?.invoke(emoji)
    }

    private var repository: EmojiRepository? = null
    private var tabs: List<Pair<String, List<String>>> = emptyList()
    private var selectedTab = 0

    private val deleteHandler = Handler(Looper.getMainLooper())
    private val deleteRepeat = object : Runnable {
        override fun run() {
            onDelete?.invoke()
            deleteHandler.postDelayed(this, DELETE_REPEAT_MS)
        }
    }

    /** Espaço inferior, para a barra de navegação do sistema não cobrir a última linha. */
    fun setBottomInset(pixels: Int) = setPadding(0, dp(4), 0, pixels)

    init {
        orientation = VERTICAL
        setPadding(0, dp(4), 0, dp(40))

        tabsRow.orientation = HORIZONTAL
        addView(tabsRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        grid.layoutManager = GridLayoutManager(context, SPAN_COUNT)
        grid.adapter = adapter
        grid.overScrollMode = OVER_SCROLL_NEVER
        grid.clipToPadding = false
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(buildBottomRow(), LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
    }

    fun setup(repository: EmojiRepository) {
        this.repository = repository
    }

    /** Monta as abas (com os recentes primeiro) e mostra a primeira categoria. */
    fun show() {
        val repo = repository ?: return
        val recents = repo.recents()
        tabs = buildList {
            add(RECENT_ICON to recents)
            repo.categories.forEach { add(it.icon to it.emojis) }
        }
        selectedTab = if (recents.isEmpty()) 1.coerceAtMost(tabs.lastIndex) else 0
        buildTabs()
        selectTab(selectedTab)
    }

    private fun buildTabs() {
        tabsRow.removeAllViews()
        tabs.forEachIndexed { index, (icon, _) ->
            val tab = TextView(context).apply {
                text = icon
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTextColor(ContextCompat.getColor(context, R.color.poro_text))
                setOnClickListener { selectTab(index) }
            }
            tabsRow.addView(tab, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun selectTab(index: Int) {
        selectedTab = index
        for (i in 0 until tabsRow.childCount) {
            tabsRow.getChildAt(i).apply {
                alpha = if (i == index) 1f else 0.45f
                setBackgroundColor(if (i == index) ContextCompat.getColor(context, R.color.poro_key) else Color.TRANSPARENT)
            }
        }
        adapter.submit(tabs.getOrNull(index)?.second ?: emptyList())
        grid.scrollToPosition(0)
    }

    private fun buildBottomRow(): View {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }

        val back = bottomButton("ABC").apply { setOnClickListener { onBack?.invoke() } }
        row.addView(back, LayoutParams(dp(80), LayoutParams.MATCH_PARENT))

        row.addView(View(context), LayoutParams(0, 1, 1f))

        val delete = bottomButton("⌫")
        delete.setOnTouchListener(deleteTouchListener())
        row.addView(delete, LayoutParams(dp(80), LayoutParams.MATCH_PARENT))
        return row
    }

    private fun bottomButton(label: String) = TextView(context).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.poro_text))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setBackgroundResource(R.drawable.key_bg)
        (layoutParams as? MarginLayoutParams)?.setMargins(dp(4), dp(2), dp(4), dp(2))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun deleteTouchListener() = OnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view.isPressed = true
                onDelete?.invoke()
                deleteHandler.postDelayed(deleteRepeat, DELETE_START_MS)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                deleteHandler.removeCallbacks(deleteRepeat)
            }
        }
        true
    }

    override fun onDetachedFromWindow() {
        deleteHandler.removeCallbacks(deleteRepeat)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int) = (value * density).toInt()

    private class EmojiAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.Holder>() {

        private var items: List<String> = emptyList()

        class Holder(val view: TextView) : RecyclerView.ViewHolder(view)

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val density = parent.resources.displayMetrics.density
            val view = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (46 * density).toInt()
                )
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                setTextColor(ContextCompat.getColor(context, R.color.poro_text))
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val emoji = items[position]
            holder.view.text = emoji
            holder.view.setOnClickListener { onClick(emoji) }
        }

        override fun getItemCount() = items.size
    }

    private companion object {
        const val SPAN_COUNT = 8
        const val RECENT_ICON = "🕘"
        const val DELETE_START_MS = 400L
        const val DELETE_REPEAT_MS = 70L
    }
}
