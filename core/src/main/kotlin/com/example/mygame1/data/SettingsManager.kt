package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object SettingsManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameSettings")

    var musicEnabled: Boolean
        get() = prefs.getBoolean("musicEnabled", true)
        set(value) {
            prefs.putBoolean("musicEnabled", value)
            prefs.flush()
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean("soundEnabled", true)
        set(value) {
            prefs.putBoolean("soundEnabled", value)
            prefs.flush()
        }
}
