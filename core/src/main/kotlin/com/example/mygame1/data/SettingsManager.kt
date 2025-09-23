package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object SettingsManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameSettings")

    var highScore: Int
        get() = prefs.getInteger("highScore", 0)
        set(value) {
            prefs.putInteger("highScore", value)
            prefs.flush()
        }

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

    // Lịch sử điểm, lưu CSV đơn giản "10,20,35,..."
    fun getScoreHistory(): List<Int> {
        val csv = prefs.getString("scoreHistory", "")
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun addScoreToHistory(score: Int) {
        val list = getScoreHistory().toMutableList()
        list.add(score)
        prefs.putString("scoreHistory", list.joinToString(","))
        // Cập nhật high score nhanh tại đây (phòng hờ)
        if (score > highScore) highScore = score
        prefs.flush()
    }

    fun clearScoreHistory() {
        prefs.remove("scoreHistory")
        prefs.flush()
    }
}
