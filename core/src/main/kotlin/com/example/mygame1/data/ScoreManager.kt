package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object ScoreManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameScores")

    fun addScore(score: Int) {
        // Lưu điểm cuối
        prefs.putInteger("lastScore", score)
        val now = System.currentTimeMillis()
        prefs.putLong("lastScoreTime", now)

        // Cập nhật high score
        val currentHigh = prefs.getInteger("highScore", 0)
        if (score > currentHigh) {
            prefs.putInteger("highScore", score)
            prefs.putLong("highScoreTime", now)
        }

        // Cập nhật lịch sử (giữ tối đa 100 bản ghi), mỗi bản ghi: score|timestamp
        val csv = prefs.getString("scoreHistory", "")
        val history = if (csv.isBlank()) mutableListOf<String>() else csv
            .split(",")
            .filter { it.contains("|") }
            .toMutableList()
        history.add("$score|$now")
        if (history.size > 100) {
            history.removeAt(0)
        }
        prefs.putString("scoreHistory", history.joinToString(","))
        prefs.flush()
    }

    fun getLastScore(): Int = prefs.getInteger("lastScore", 0)
    fun getLastScoreTime(): Long = prefs.getLong("lastScoreTime", 0L)
    fun getHighScore(): Int = prefs.getInteger("highScore", 0)
    fun getHighScoreTime(): Long = prefs.getLong("highScoreTime", 0L)

    /**
     * Trả về top N điểm cao nhất kèm thời gian (score, timestamp)
     */
    fun getTopScoresWithTime(limit: Int = 6): List<Pair<Int, Long>> {
        val csv = prefs.getString("scoreHistory", "")
        if (csv.isBlank()) return emptyList()
        return csv.split(",")
            .mapNotNull {
                val parts = it.split("|")
                if (parts.size == 2) parts[0].toIntOrNull()?.let { score ->
                    val time = parts[1].toLongOrNull() ?: 0L
                    score to time
                } else null
            }
            .sortedByDescending { it.first }
            .take(limit)
    }

    fun getScoreHistory(): List<Int> {
        val csv = prefs.getString("scoreHistory", "")
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { it.toIntOrNull() }
    }
}
