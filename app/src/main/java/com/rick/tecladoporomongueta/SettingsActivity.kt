package com.rick.tecladoporomongueta

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val settings = KeyboardSettings.get(this)

        findViewById<SwitchCompat>(R.id.switchSuggestions).apply {
            isChecked = settings.suggestionsEnabled
            setOnCheckedChangeListener { _, checked -> settings.suggestionsEnabled = checked }
        }
        findViewById<SwitchCompat>(R.id.switchVibration).apply {
            isChecked = settings.vibrationEnabled
            setOnCheckedChangeListener { _, checked -> settings.vibrationEnabled = checked }
        }

        findViewById<RadioGroup>(R.id.radioTheme).apply {
            check(
                when (settings.theme) {
                    KeyboardSettings.THEME_SYSTEM -> R.id.themeSystem
                    KeyboardSettings.THEME_LIGHT -> R.id.themeLight
                    else -> R.id.themeDark
                }
            )
            setOnCheckedChangeListener { _, id ->
                settings.theme = when (id) {
                    R.id.themeSystem -> KeyboardSettings.THEME_SYSTEM
                    R.id.themeLight -> KeyboardSettings.THEME_LIGHT
                    else -> KeyboardSettings.THEME_DARK
                }
                AppCompatDelegate.setDefaultNightMode(settings.nightMode)
            }
        }

        val label = findViewById<TextView>(R.id.heightLabel)
        findViewById<SeekBar>(R.id.seekHeight).apply {
            max = KeyboardSettings.MAX_HEIGHT - KeyboardSettings.MIN_HEIGHT
            progress = settings.heightPercent - KeyboardSettings.MIN_HEIGHT
            label.text = getString(R.string.settings_height, settings.heightPercent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    val percent = KeyboardSettings.MIN_HEIGHT + value
                    label.text = getString(R.string.settings_height, percent)
                    if (fromUser) settings.heightPercent = percent
                }

                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }
    }
}
