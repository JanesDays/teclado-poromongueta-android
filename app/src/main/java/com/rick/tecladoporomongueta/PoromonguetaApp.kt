package com.rick.tecladoporomongueta

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class PoromonguetaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(KeyboardSettings.get(this).nightMode)
    }
}
