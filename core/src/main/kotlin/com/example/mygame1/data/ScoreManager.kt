package com.example.mygame1.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object ScoreManager {
    private val prefs: Preferences = Gdx.app.getPreferences("MyGameScores")

    // Lưu lại chế độ cuối cùng đã ghi điểm
    private const val LAST_DIFFICULTY_KEY = "lastDifficulty"

    // API mới: lưu điểm kèm chế độ chơi
    fun addScore(score: Int, difficulty: Difficulty) {
        val diffKey = difficulty.name
        prefs.putString(LAST_DIFFICULTY_KEY, diffKey)

        // Lưu điểm cuối của chế độ
        prefs.putInteger("lastScore_$diffKey", score)
        val now = System.currentTimeMillis()
        prefs.putLong("lastScoreTime_$diffKey", now)

        // High score riêng từng chế độ
        val currentHigh = prefs.getInteger("highScore_$diffKey", 0)
        if (score > currentHigh) {
            prefs.putInteger("highScore_$diffKey", score)
            prefs.putLong("highScoreTime_$diffKey", now)
        }

        // Lịch sử riêng: giữ tối đa 100 bản ghi. Mỗi bản ghi score|timestamp
        val historyKey = "scoreHistory_$diffKey"
        val csv = prefs.getString(historyKey, "")
        val history = if (csv.isBlank()) mutableListOf<String>() else csv
            .split(",")
            .filter { it.contains("|") }
            .toMutableList()
        history.add("$score|$now")
        if (history.size > 100) history.removeAt(0)
        prefs.putString(historyKey, history.joinToString(","))
        prefs.flush()
    }

    fun getLastScore(difficulty: Difficulty? = null): Int {
        val diff = difficulty ?: getLastDifficulty()
        return prefs.getInteger("lastScore_${diff.name}", 0)
    }

    fun getLastScoreTime(difficulty: Difficulty? = null): Long {
        val diff = difficulty ?: getLastDifficulty()
        return prefs.getLong("lastScoreTime_${diff.name}", 0L)
    }

    fun getHighScore(difficulty: Difficulty? = null): Int {
        val diff = difficulty ?: getLastDifficulty()
        return prefs.getInteger("highScore_${diff.name}", 0)
    }

    fun getHighScoreTime(difficulty: Difficulty? = null): Long {
        val diff = difficulty ?: getLastDifficulty()
        return prefs.getLong("highScoreTime_${diff.name}", 0L)
    }

    fun getTopScoresWithTime(limit: Int = 6, difficulty: Difficulty? = null): List<Pair<Int, Long>> {
        val diff = difficulty ?: getLastDifficulty()
        val csv = prefs.getString("scoreHistory_${diff.name}", "")
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

    fun getLastDifficulty(): Difficulty {
        val stored = prefs.getString(LAST_DIFFICULTY_KEY, Difficulty.EASY.name)
        return Difficulty.fromString(stored)
    }
}
