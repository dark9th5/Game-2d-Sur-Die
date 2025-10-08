import com.badlogic.gdx.math.Vector2
import com.example.mygame1.entities.Bullet
import kotlin.random.Random

class EnemyAI(
    private val useML: Boolean = true // Có dùng ML không
) {
    // Trạng thái tuần tra giữ lại hướng trong một khoảng thời gian để tránh quay vòng tại chỗ
    private var patrolDir: Vector2 = Vector2(1f, 0f)
    private var patrolTimeLeft: Float = 0f

    private fun ensurePatrolDirection() {
        if (patrolTimeLeft <= 0f) {
            patrolDir = randomDirection()
            patrolTimeLeft = 2f + Random.nextFloat() * 2.5f // 2-4.5s
        }
    }

    private fun updatePatrol(delta: Float) { patrolTimeLeft -= delta }

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

    // AI cứng: phát hiện, di chuyển, bắn (đã thêm delta cho tuần tra mượt)
    fun decideAction(state: GameState, visionRange: Float, delta: Float): EnemyAction {
        updatePatrol(delta)
        val distToPlayer = state.enemyPosition.dst(state.playerPosition)
        val vision = visionRange
        val shoot = 200f

        if (distToPlayer < vision) {
            // Reset bộ đếm tuần tra để khi mất dấu sẽ chọn hướng mới mượt hơn
            if (patrolTimeLeft < 1f) patrolTimeLeft = 1f
            if (useML) {
                val evade = evadeBullets(state)
                if (evade != null) {
                    return EnemyAction.Move(evade)
                }
            } else {
                val bulletDanger = state.bullets.any { bullet ->
                    bullet.position.dst(state.enemyPosition) < 60f &&
                        bullet.direction.dot(state.enemyPosition.cpy().sub(bullet.position).nor()) > 0.7f
                }
                if (bulletDanger) {
                    return EnemyAction.Move(Vector2(-1f, 0f))
                }
            }
            if (distToPlayer > shoot) {
                val moveDir = state.playerPosition.cpy().sub(state.enemyPosition).nor()
                return EnemyAction.Move(moveDir)
            } else {
                return EnemyAction.Shoot(state.playerPosition.cpy().sub(state.enemyPosition).nor())
            }
        }
        // Ngoài tầm nhìn => tuần tra ổn định
        ensurePatrolDirection()
        return EnemyAction.Move(patrolDir.cpy())
    }

    // Phiên bản thích nghi dùng snapshot hành vi người chơi (có delta)
    fun decideActionAdaptive(state: GameState, baseVision: Float, behavior: PlayerBehaviorSnapshot?, delta: Float): EnemyAction {
        updatePatrol(delta)
        behavior ?: return decideAction(state, baseVision, delta)
        val distToPlayer = state.enemyPosition.dst(state.playerPosition)
        val vision = baseVision * (1f + behavior.aggression * 0.3f)
        val shootNear = 160f
        val shootFar = 260f
        val shootDist = lerp(shootNear, shootFar, behavior.accuracy)

        if (distToPlayer < vision) {
            // Khi thấy player tạm hoãn đổi hướng tuần tra, nhưng giữ lại hướng hiện tại để tiếp tục sau
            if (useML && (behavior.accuracy > 0.5f || behavior.aggression > 0.6f)) {
                val evade = evadeBullets(state)
                if (evade != null && Random.nextFloat() < 0.85f) {
                    return EnemyAction.Move(evade)
                }
            }
            if (distToPlayer > shootDist) {
                val dirToPlayer = state.playerPosition.cpy().sub(state.enemyPosition).nor()
                val rushBoost = 1f + behavior.preferredDistance * 0.4f
                return EnemyAction.Move(dirToPlayer.scl(rushBoost))
            } else {
                val baseShootDir = state.playerPosition.cpy().sub(state.enemyPosition).nor()
                if (behavior.accuracy > 0.5f) {
                    val perp = Vector2(-baseShootDir.y, baseShootDir.x)
                    val strafeAmount = (behavior.accuracy - 0.5f) * 0.6f
                    val mixed = baseShootDir.cpy().add(perp.scl(if (Random.nextBoolean()) strafeAmount else -strafeAmount)).nor()
                    return EnemyAction.Shoot(mixed)
                }
                return EnemyAction.Shoot(baseShootDir)
            }
        }
        // Không thấy player => tuần tra giữ hướng trong 2-4.5s rồi đổi
        ensurePatrolDirection()
        return EnemyAction.Move(patrolDir.cpy())
    }

    private fun lerp(a: Float, b: Float, alpha: Float) = a + (b - a) * alpha
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
