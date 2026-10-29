package com.rick.tecladoporomongueta

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnFechar).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnAtivar).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<View>(R.id.btnEscolher).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        findViewById<View>(R.id.btnDicionario).setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }

        findViewById<View>(R.id.btnConfiguracoes).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnLimpar).setOnClickListener {
            LearningManager.getInstance(this).clearAll()
            NextWordManager.getInstance(this).clearAll()
            EmojiRepository.getInstance(this).clearRecents()
            Toast.makeText(this, R.string.main_cleared, Toast.LENGTH_SHORT).show()
        }
    }
}
