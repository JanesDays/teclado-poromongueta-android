package com.rick.tecladoporomongueta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream

/**
 * Consulta o dicionário e permite à pessoa adicionar as próprias palavras, compartilhá-las
 * (para a comunidade ou o mantenedor do projeto) e importar palavras de outro aparelho.
 */
class DictionaryActivity : AppCompatActivity() {

    private class Row(val entry: DictionaryEntry, val isUser: Boolean)

    private lateinit var dictionary: DictionaryManager
    private lateinit var userDictionary: UserDictionary
    private lateinit var summary: TextView
    private lateinit var emptyView: TextView
    private lateinit var search: EditText

    private val adapter = RowAdapter { row -> if (row.isUser) confirmRemove(row.entry.word) }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFrom(it) } }

    private val saveLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { saveTo(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dictionary)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dictRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + dp(16), bars.top + dp(16), bars.right + dp(16), bars.bottom + dp(16)
            )
            insets
        }

        dictionary = DictionaryManager.fromAssets(this)
        userDictionary = UserDictionary.getInstance(this)

        summary = findViewById(R.id.dictSummary)
        emptyView = findViewById(R.id.dictEmpty)
        search = findViewById(R.id.dictSearch)

        findViewById<RecyclerView>(R.id.dictList).apply {
            layoutManager = LinearLayoutManager(this@DictionaryActivity)
            adapter = this@DictionaryActivity.adapter
        }

        findViewById<View>(R.id.btnDictFechar).setOnClickListener { finish() }
        findViewById<View>(R.id.btnDictAdd).setOnClickListener { showAddDialog() }
        findViewById<View>(R.id.btnDictImport).setOnClickListener {
            importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
        }
        findViewById<View>(R.id.btnDictShare).setOnClickListener { share() }
        findViewById<View>(R.id.btnDictSave).setOnClickListener {
            if (userDictionary.count() == 0) toast(R.string.dict_nothing_to_share)
            else saveLauncher.launch("palavras-poromonguete.txt")
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = refresh()
        })

        refresh()
    }

    private fun refresh() {
        val query = TextNormalizer.fold(search.text.toString().trim())
        fun matches(entry: DictionaryEntry) = query.isEmpty() ||
            TextNormalizer.fold(entry.word).contains(query) ||
            TextNormalizer.fold(entry.translation).contains(query)

        val userEntries = userDictionary.getEntries()
        val rows = userEntries.filter { matches(it) }.map { Row(it, true) } +
            dictionary.getEntries().filter { matches(it) }.map { Row(it, false) }

        adapter.submit(rows)
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        summary.text = getString(R.string.dict_summary, dictionary.wordCount(), userEntries.size)
    }

    private fun showAddDialog() {
        val padding = dp(20)
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, dp(8), padding, 0)
        }
        val wordField = EditText(this).apply {
            hint = getString(R.string.dict_word_hint)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val translationField = EditText(this).apply {
            hint = getString(R.string.dict_translation_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        form.addView(wordField)
        form.addView(translationField)

        AlertDialog.Builder(this)
            .setTitle(R.string.dict_add_title)
            .setView(form)
            .setPositiveButton(R.string.dict_save_button) { _, _ ->
                if (userDictionary.add(wordField.text.toString(), translationField.text.toString())) {
                    toast(R.string.dict_added)
                    refresh()
                } else {
                    toast(R.string.dict_invalid_word)
                }
            }
            .setNegativeButton(R.string.dict_cancel, null)
            .show()
    }

    private fun confirmRemove(word: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dict_remove_title, word))
            .setPositiveButton(R.string.dict_remove) { _, _ ->
                userDictionary.remove(word)
                refresh()
            }
            .setNegativeButton(R.string.dict_cancel, null)
            .show()
    }

    private fun share() {
        if (userDictionary.count() == 0) {
            toast(R.string.dict_nothing_to_share)
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.dict_share_subject))
            putExtra(Intent.EXTRA_TEXT, userDictionary.export())
        }
        startActivity(Intent.createChooser(send, getString(R.string.dict_share)))
    }

    private fun saveTo(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use {
                it.write(userDictionary.export().toByteArray(Charsets.UTF_8))
            }
            toast(R.string.dict_saved)
        } catch (e: Exception) {
            toast(R.string.dict_import_error)
        }
    }

    private fun importFrom(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { readLimited(it, MAX_IMPORT_BYTES) }
            if (text == null) {
                toast(R.string.dict_import_error)
                return
            }
            val added = userDictionary.import(text)
            Toast.makeText(this, getString(R.string.dict_imported, added), Toast.LENGTH_SHORT).show()
            refresh()
        } catch (e: Exception) {
            toast(R.string.dict_import_error)
        }
    }

    /** Lê o arquivo inteiro, recusando os grandes demais. */
    private fun readLimited(input: InputStream, maxBytes: Int): String? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            buffer.write(chunk, 0, read)
            if (buffer.size() > maxBytes) return null
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class RowAdapter(
        private val onLongClick: (Row) -> Unit
    ) : RecyclerView.Adapter<RowAdapter.Holder>() {

        private var rows: List<Row> = emptyList()

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val word: TextView = view.findViewById(R.id.itemWord)
            val translation: TextView = view.findViewById(R.id.itemTranslation)
        }

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<Row>) {
            rows = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_dictionary, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = rows[position]
            holder.word.text =
                if (row.isUser) getString(R.string.dict_user_mark, row.entry.word) else row.entry.word
            holder.translation.text = row.entry.translation
            holder.translation.visibility = if (row.entry.translation.isEmpty()) View.GONE else View.VISIBLE
            holder.itemView.setOnLongClickListener {
                onLongClick(row)
                row.isUser
            }
        }

        override fun getItemCount() = rows.size
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 2 * 1024 * 1024
    }
}
