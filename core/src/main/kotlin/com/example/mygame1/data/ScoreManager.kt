package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object ScoreManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameScores")

    fun addScore(score: Int) {
        // Lưu điểm cuối
        prefs.putInteger("lastScore", score)

        // Cập nhật high score
        val currentHigh = prefs.getInteger("highScore", 0)
        if (score > currentHigh) {
            prefs.putInteger("highScore", score)
        }

        // Cập nhật lịch sử (giữ tối đa 100 bản ghi)
        val csv = prefs.getString("scoreHistory", "")
        val history = if (csv.isBlank()) mutableListOf<Int>() else csv
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toMutableList()
        history.add(score)
        if (history.size > 100) {
            history.removeAt(0)
        }
        prefs.putString("scoreHistory", history.joinToString(","))

        prefs.flush()
    }

    fun getLastScore(): Int = prefs.getInteger("lastScore", 0)
    fun getHighScore(): Int = prefs.getInteger("highScore", 0)

    fun getScoreHistory(): List<Int> {
        val csv = prefs.getString("scoreHistory", "")
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { it.toIntOrNull() }
    }
}
