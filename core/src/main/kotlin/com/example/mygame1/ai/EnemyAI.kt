import com.badlogic.gdx.math.Vector2
import com.example.mygame1.entities.Bullet
import kotlin.random.Random

class EnemyAI(
    private val useML: Boolean = true // Có dùng ML không
) {
    // ML module giả lập: Trả về hướng né đạn (vector), có thể thay bằng model thực tế sau
    fun evadeBullets(state: GameState): Vector2? {
        val nearestBullet = state.bullets
            .filter { it.position.dst(state.enemyPosition) < 100f }
            .minByOrNull { it.position.dst(state.enemyPosition) }
        return nearestBullet?.let {
            Vector2(-it.direction.y, it.direction.x).nor()
        }
    }

    // Tạo hướng ngẫu nhiên (vector chuẩn hóa)
    private fun randomDirection(): Vector2 {
        val angle = Random.nextFloat() * 360f
        return Vector2(
            Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
            Math.sin(Math.toRadians(angle.toDouble())).toFloat()
        ).nor()
    }

    // AI cứng: phát hiện, di chuyển, bắn
    fun decideAction(state: GameState, visionRange: Float): EnemyAction {
        val distToPlayer = state.enemyPosition.dst(state.playerPosition)
        val vision = visionRange
        val shoot = 200f

        // 1. Phát hiện player
        if (distToPlayer < vision) {
            // 2. Né đạn bằng ML nếu có dùng ML và có đạn nguy hiểm
            if (useML) {
                val evade = evadeBullets(state)
                if (evade != null) {
                    return EnemyAction.Move(evade)
                }
            } else {
                // AI cứng né đạn (đơn giản)
                val bulletDanger = state.bullets.any { bullet ->
                    bullet.position.dst(state.enemyPosition) < 60f &&
                        bullet.direction.dot(state.enemyPosition.cpy().sub(bullet.position).nor()) > 0.7f
                }
                if (bulletDanger) {
                    return EnemyAction.Move(Vector2(-1f, 0f))
                }
            }
            // 3. Đuổi/bắn
            if (distToPlayer > shoot) {
                val moveDir = state.playerPosition.cpy().sub(state.enemyPosition).nor()
                return EnemyAction.Move(moveDir)
            } else {
                return EnemyAction.Shoot(state.playerPosition.cpy().sub(state.enemyPosition).nor())
            }
        }
        // 4. Tuần tra: DI CHUYỂN NGẪU NHIÊN khi không phát hiện player
        return EnemyAction.Move(randomDirection())
    }
}

// --- Các kiểu dữ liệu ---
data class GameState(
    val enemyPosition: Vector2,
    val playerPosition: Vector2,
    val bullets: List<Bullet>
)

sealed class EnemyAction {
    object Idle : EnemyAction()
    data class Move(val direction: Vector2) : EnemyAction()
    data class Shoot(val direction: Vector2) : EnemyAction()
}
