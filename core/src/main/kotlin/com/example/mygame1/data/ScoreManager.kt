package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object ScoreManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameSettings")
    private const val KEY_SCORE_LIST = "scoreList" // lưu chuỗi điểm ví dụ "120,80,50"

    fun addScore(score: Int) {
        if (score < 0) return
        val current = prefs.getString(KEY_SCORE_LIST, "")
        val updated = if (current.isBlank()) "$score" else "$current,$score"
        prefs.putString(KEY_SCORE_LIST, updated)

        val high = prefs.getInteger("highScore", 0)
        if (score > high) {
            prefs.putInteger("highScore", score)
        }
        prefs.flush()
    }

    fun getScores(): List<Int> {
        val raw = prefs.getString(KEY_SCORE_LIST, "")
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sortedDescending()
    }

    fun clearScores() {
        prefs.putString(KEY_SCORE_LIST, "")
        prefs.flush()
    }
}
