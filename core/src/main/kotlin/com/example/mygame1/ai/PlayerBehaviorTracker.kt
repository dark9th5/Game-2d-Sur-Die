import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.example.mygame1.entities.Enemy
import com.example.mygame1.entities.Player
import java.util.Locale

/**
 * PlayerBehaviorTracker: Thu thập đặc trưng phong cách người chơi để Enemy AI thích nghi.
 * Lưu lịch sử nhiều trận (match history) để các trận sau khởi động với baseline hành vi.
 */
class PlayerBehaviorTracker {
    private var totalShots = 0
    private var totalHits = 0

    private var lastShotAbsTime = -1f
    private var gameTime = 0f

    private var emaShotFreq = 0f          // phát / giây
    private var emaPreferredDistance = 0f // px

    private val REF_SHOT_FREQ = 3f    // 3 phát/s ~ max aggression
    private val REF_DISTANCE = 300f   // 300px ~ chuẩn hoá =1

    private val prefs = Gdx.app.getPreferences("player_behavior_history")
    private val HISTORY_KEY = "records"
    private val MAX_RECORDS = 50

    private var seeded = false

    init {
        seedFromHistory()
    }

    fun updateFrame(delta: Float, player: Player, enemies: List<Enemy>) {
        gameTime += delta
    }

    fun onPlayerShot(playerPos: Vector2, nearestEnemyPos: Vector2?) {
        totalShots++
        nearestEnemyPos?.let { enemyPos ->
            val dist = playerPos.dst(enemyPos)
            emaPreferredDistance = lerp(if (emaPreferredDistance == 0f) dist else emaPreferredDistance, dist, 0.08f)
        }
        if (lastShotAbsTime >= 0f) {
            val interval = (gameTime - lastShotAbsTime).coerceAtLeast(0.0001f)
            val freq = 1f / interval
            emaShotFreq = lerp(if (emaShotFreq == 0f) freq else emaShotFreq, freq, 0.18f)
        }
        lastShotAbsTime = gameTime
    }

    fun onPlayerHitEnemy() { totalHits++ }

    fun snapshot(): PlayerBehaviorSnapshot {
        val accuracy = if (totalShots == 0) 0f else totalHits.toFloat() / totalShots
        val aggression = clamp(emaShotFreq / REF_SHOT_FREQ, 0f, 1f)
        val prefDistNorm = clamp(emaPreferredDistance / REF_DISTANCE, 0f, 1f)
        return PlayerBehaviorSnapshot(
            aggression = aggression,
            accuracy = clamp(accuracy, 0f, 1f),
            preferredDistance = prefDistNorm
        )
    }

    /** Gọi khi trận kết thúc để tạo record & lưu. */
    fun finalizeAndPersist(score: Int, difficulty: String, win: Boolean) {
        val snap = snapshot()
        val record = PlayerMatchRecord(
            timestamp = System.currentTimeMillis(),
            score = score,
            difficulty = difficulty,
            win = win,
            aggression = snap.aggression,
            accuracy = snap.accuracy,
            preferredDistance = snap.preferredDistance
        )
        val history = loadHistory().toMutableList()
        history.add(record)
        while (history.size > MAX_RECORDS) history.removeFirst()
        saveHistory(history)
    }

    fun loadHistory(): List<PlayerMatchRecord> {
        val raw = prefs.getString(HISTORY_KEY, "")
        if (raw.isBlank()) return emptyList()
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size < 8) return@mapNotNull null
                try {
                    PlayerMatchRecord(
                        timestamp = parts[0].toLong(),
                        score = parts[1].toInt(),
                        difficulty = parts[2],
                        win = parts[3] == "1",
                        aggression = parts[4].toFloat(),
                        accuracy = parts[5].toFloat(),
                        preferredDistance = parts[6].toFloat(),
                        version = parts[7].toInt()
                    )
                } catch (_: Exception) { null }
            }
    }

    private fun saveHistory(list: List<PlayerMatchRecord>) {
        val sb = StringBuilder()
        list.forEach { r ->
            sb.append(
                listOf(
                    r.timestamp,
                    r.score,
                    r.difficulty,
                    if (r.win) "1" else "0",
                    fmt(r.aggression),
                    fmt(r.accuracy),
                    fmt(r.preferredDistance),
                    r.version
                ).joinToString(",")
            ).append('\n')
        }
        prefs.putString(HISTORY_KEY, sb.toString())
        prefs.flush()
    }

    private fun seedFromHistory() {
        if (seeded) return
        val history = loadHistory()
        if (history.isEmpty()) { seeded = true; return }
        val avgAgg = history.map { it.aggression }.average().toFloat()
        val avgPref = history.map { it.preferredDistance }.average().toFloat()
        val avgAcc = history.map { it.accuracy }.average().toFloat()
        // Seed EMA bằng baseline
        emaShotFreq = (avgAgg * REF_SHOT_FREQ).coerceAtLeast(0f)
        emaPreferredDistance = (avgPref * REF_DISTANCE).coerceAtLeast(0f)
        // Seed accuracy bằng cách giả lập số liệu (100 phát)
        if (avgAcc > 0f) {
            totalShots = 100
            totalHits = (avgAcc * 100).toInt().coerceIn(0, 100)
        }
        seeded = true
    }

    private fun fmt(v: Float) = String.format(Locale.US, "%.4f", v)

    private fun lerp(a: Float, b: Float, alpha: Float) = a + (b - a) * alpha
    private fun clamp(v: Float, lo: Float, hi: Float) = if (v < lo) lo else if (v > hi) hi else v
}

data class PlayerBehaviorSnapshot(
    val aggression: Float,       // 0..1
    val accuracy: Float,         // 0..1
    val preferredDistance: Float // 0..1 (chuẩn hoá theo REF_DISTANCE)
)

data class PlayerMatchRecord(
    val timestamp: Long,
    val score: Int,
    val difficulty: String,
    val win: Boolean,
    val aggression: Float,
    val accuracy: Float,
    val preferredDistance: Float,
    val version: Int = 1
)
