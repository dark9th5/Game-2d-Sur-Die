package com.example.mygame1.entities

import EnemyAI
import GameState
import EnemyAction
import PlayerBehaviorSnapshot
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Rectangle
import ktx.assets.disposeSafely
import kotlin.random.Random
import com.badlogic.gdx.Gdx
import com.example.mygame1.audio.AudioManager
enum class EnemyAbility { GUN, MACHINE, SILENCER, TRAP }
class Enemy(
    characterIndex: Int = -1,
    weaponIndex: Int = -1,
    spawnPosition: Vector2 = Vector2(600f, 400f),
    val ai: EnemyAI = EnemyAI(useML = true),
    private val healthMultiplier: Float = 1f,
    private val damageMultiplier: Float = 1f,
    var selectedAbility: EnemyAbility = EnemyAbility.GUN, // mặc định là GUN
    var isHardMode: Boolean = false // thêm thuộc tính này
) {
    private var currentCharacterIndex =
        if (characterIndex in characterTextures.indices) characterIndex else Random.nextInt(characterTextures.size)
    private var texture = Texture(Gdx.files.internal(characterTextures[currentCharacterIndex]))
    val sprite = Sprite(texture).apply { setOriginCenter() }
    var position = spawnPosition.cpy()
    var maxHealth: Int = (100 * healthMultiplier).toInt().coerceAtLeast(1)
    var health: Int = maxHealth
    val speed = 50f

    // ---------------- Ammo per weapon (delegated) ----------------
    private val reloadTimePerBullet = 0.1f
    private val reloadTimeFull = 2f
    val ammoInMagazine: Int get() = weapon.ammoInMagazine
    val maxBullets: Int get() = weapon.maxBullets
    val isReloading: Boolean get() = weapon.isReloading

    // Remove old fields (ammoInMagazine, isReloading, timers) now handled in Weapon

    val bullets = mutableListOf<Bullet>()

    val weapons: List<Weapon> = listOf(
        Weapon(GunType.Gun),
        Weapon(GunType.Machine),
        Weapon(GunType.Silencer)
    )
    // Texture riêng cho ability TRAP để hiển thị cầm bẫy thay vì súng
    private var trapTexture: Texture? = if (selectedAbility == EnemyAbility.TRAP) runCatching { Texture("character/Weapons/weapon_trap.png") }.getOrNull() else null
    private var trapSprite: Sprite? = trapTexture?.let { Sprite(it) }
    private var currentWeaponIndex: Int =
        if (weaponIndex in weapons.indices) weaponIndex else Random.nextInt(weapons.size)

    val weapon: Weapon
        get() = weapons[currentWeaponIndex]

    private var shootCooldown = 0f
    var allowShooting: Boolean = true

    // CollisionManager lưu cho enemy
    private var collisionManager: com.example.mygame1.world.CollisionManager? = null
    fun setCollisionManager(collisionManager: com.example.mygame1.world.CollisionManager) {
        this.collisionManager = collisionManager
    }

    // Thêm vào class Enemy:
    var reviveTimer: Float = 0f
    var isWaitingRevive: Boolean = false
    val spawnPoint = spawnPosition.cpy()

    // Thêm thuộc tính mới rootTimeLeft
    var rootTimeLeft: Float = 0f // thời gian còn lại bị trói (trap)

    // Đánh dấu đã trao loot/score cho lần chết này chưa
    var hasDroppedLoot: Boolean = false

    fun selectCharacter(index: Int) {
        if (index in characterTextures.indices) {
            currentCharacterIndex = index
            texture.disposeSafely()
            texture = Texture(Gdx.files.internal(characterTextures[currentCharacterIndex]))
            sprite.setTexture(texture)
            sprite.setOriginCenter()
        }
    }

    fun selectWeapon(index: Int) {
        if (index == currentWeaponIndex) return // tránh reset cooldown nếu cùng súng
        if (index in weapons.indices) {
            currentWeaponIndex = index
            shootCooldown = 0f // reset cooldown khi đổi súng thực sự
        }
    }

    /**
     * Hàm update logic đầy đủ, dùng AI ngoài (state, decideAction)
     */
    fun update(
        delta: Float,
        state: GameState,
        mapWidth: Float = 800f,
        mapHeight: Float = 600f,
        behaviorSnapshot: PlayerBehaviorSnapshot? = null
    ) {
        if (isWaitingRevive) {
            reviveTimer += delta
            if (reviveTimer >= 5f) {
                // Hồi sinh
                health = maxHealth
                position.set(spawnPoint)
                reviveTimer = 0f
                isWaitingRevive = false
                hasDroppedLoot = false // cho phép rơi loot lại ở lần chết sau
            }
            return
        }

        // Tự động reload khi hết đạn
        if (weapon.ammoInMagazine == 0 && !weapon.isReloading) {
            manualReload(forceFull = true)
        }
        if (weapon.isReloading) {
            weapon.reloadTimer -= delta
            if (weapon.reloadTimer <= 0f) {
                weapon.ammoInMagazine = weapon.reloadTarget
                weapon.isReloading = false
            }
        }
        shootCooldown = (shootCooldown - delta).coerceAtLeast(0f)

        // Nếu đang bị trói thì chỉ giảm thời gian & update đạn, bỏ qua AI
        if (rootTimeLeft > 0f) {
            rootTimeLeft -= delta
            bullets.forEach { it.update(delta) }
            bullets.removeAll { !it.isActive }
            sprite.setPosition(position.x, position.y)
            return
        }

        // LẤY VISION RANGE TỪ VŨ KHÍ HIỆN TẠI (giống world)
        val visionRange = getGunStats(weapon.type).bulletRange

        // Thay bằng decideActionAdaptive
        when (val action = ai.decideActionAdaptive(state, visionRange, behaviorSnapshot, delta)) {
            is EnemyAction.Move -> {
                val dir = action.direction.nor()
                // Di chuyển có kiểm tra va chạm
                val vx = dir.x * speed * delta
                val vy = dir.y * speed * delta
                val rectX = Rectangle(position.x + vx, position.y, sprite.width - 2f, sprite.height - 2f)
                if (collisionManager == null || !collisionManager!!.isBlocked(rectX)) {
                    position.x = (position.x + vx).coerceIn(0f, mapWidth - sprite.width)
                }
                val rectY = Rectangle(position.x, position.y + vy, sprite.width - 2f, sprite.height - 2f)
                if (collisionManager == null || !collisionManager!!.isBlocked(rectY)) {
                    position.y = (position.y + vy).coerceIn(0f, mapHeight - sprite.height)
                }
                sprite.rotation = dir.angleDeg()
            }
            is EnemyAction.Shoot -> {
                // Nếu ở chế độ Hard thì không cho phép AI tự động bắn (chỉ cho phép World kiểm soát)
                if (isHardMode) {
                    // Bỏ qua, không gọi attack()
                } else {
                    val dir = action.direction.nor()
                    sprite.rotation = dir.angleDeg()
                    attack()
                }
            }
            is EnemyAction.Idle -> {
                // Đứng yên
            }
        }
        sprite.setPosition(position.x, position.y)
        bullets.forEach { it.update(delta) }
        bullets.removeAll { !it.isActive }
    }

    fun render(batch: SpriteBatch, font: com.badlogic.gdx.graphics.g2d.BitmapFont? = null) {
        // Không vẽ khi đang chờ hồi sinh hoặc đã chết
        if (isWaitingRevive || health <= 0) return

        sprite.draw(batch)
        if (selectedAbility == EnemyAbility.TRAP) {
            // Vẽ bẫy trên tay thay cho súng
            trapSprite?.let { ts ->
                val gunPos = getGunTipPosition()
                ts.setOrigin(ts.width / 2f, ts.height / 2f)
                ts.setPosition(gunPos.x - ts.width / 2f, gunPos.y - ts.height / 2f)
                ts.rotation = sprite.rotation
                ts.draw(batch)
            }
        } else {
            val gunPos = getGunTipPosition()
            weapon.render(batch, gunPos, sprite.rotation)
        }
        bullets.forEach { it.render(batch) }
        // Hiển thị chữ "unable to move" nếu bị trói
        if (rootTimeLeft > 0f && font != null) {
            font.color = Color.RED
            font.draw(batch, "unable to move", sprite.x + sprite.width / 2 - 50f, sprite.y + sprite.height + 30f)
            font.color = Color.WHITE
        }

        val barWidth = sprite.width
        val barHeight = 8f
        val healthRatio = health / maxHealth.toFloat()
        val barX = sprite.x
        val barY = sprite.y + sprite.height + 5f

        batch.color = Color.RED
        batch.draw(blankTexture, barX, barY, barWidth, barHeight)
        batch.color = Color.GREEN
        batch.draw(blankTexture, barX, barY, barWidth * healthRatio, barHeight)
        batch.color = Color.WHITE
        font?.color = Color.SALMON
        if (selectedAbility != EnemyAbility.TRAP) {
            font?.draw(
                batch,
                if (weapon.isReloading) "Reloading..." else "$ammoInMagazine / $maxBullets",
                barX,
                barY - 2f
            )
        }
    }

    private fun getGunTipPosition(): Vector2 {
        val gunOffsetX = sprite.width / 2f
        val gunOffsetY = 0f
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val rotatedOffsetX = gunOffsetX * MathUtils.cos(angleRad) - gunOffsetY * MathUtils.sin(angleRad)
        val rotatedOffsetY = gunOffsetX * MathUtils.sin(angleRad) + gunOffsetY * MathUtils.cos(angleRad)
        val centerX = position.x + sprite.width / 2f
        val centerY = position.y + sprite.height / 2f
        return Vector2(centerX + rotatedOffsetX, centerY + rotatedOffsetY)
    }

    fun attack() {
        if (!allowShooting) return
        val stats = getGunStats(weapon.type)
        if (weapon.isReloading || weapon.ammoInMagazine == 0) return
        if (shootCooldown > 0f) return

        val bulletType = when (weapon.type) {
            GunType.Gun -> BulletType.Gun
            GunType.Machine -> BulletType.Machine
            GunType.Silencer -> BulletType.Silencer
            GunType.Bomb, GunType.Shield, GunType.Trap -> BulletType.Gun // placeholder
        }
        val bulletStart = getGunTipPosition()
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val direction = Vector2(MathUtils.cos(angleRad), MathUtils.sin(angleRad))

        bullets.add(
            Bullet(
                type = bulletType,
                position = bulletStart,
                direction = direction,
                maxDistance = stats.bulletRange,
                size = stats.bulletSize,
                damage = (stats.damage * damageMultiplier).toInt().coerceAtLeast(1)
            )
        )
        weapon.ammoInMagazine--
        shootCooldown = 1f / stats.fireRate
        // Play sound if not silencer
        when (weapon.type) {
            GunType.Gun -> AudioManager.playSound("sounds/submachine-gun-79846.mp3",0.25f)
            GunType.Machine -> AudioManager.playSound("sounds/machine-gun-129928.mp3",0.25f)
            GunType.Silencer -> {AudioManager.playSound("sounds/gun-shot-359196.mp3",0.25f)}
            GunType.Bomb, GunType.Shield, GunType.Trap -> { /* no shooting sound */ }
        }
    }

    fun manualReload(forceFull: Boolean = false) {
        if (weapon.isReloading) return
        if (weapon.ammoInMagazine == weapon.maxBullets) return
        weapon.isReloading = true
        weapon.reloadTarget = weapon.maxBullets
        weapon.reloadTimer = if (forceFull) reloadTimeFull else (weapon.maxBullets - weapon.ammoInMagazine) * reloadTimePerBullet
    }

    fun takeDamage(amount: Int) {
        if (isWaitingRevive) return
        health = (health - amount).coerceAtLeast(0)
        if (health <= 0) {
            isWaitingRevive = true
            reviveTimer = 0f
            // đánh dấu chưa rơi loot cho lần chết này (World sẽ kiểm tra cờ này)
            hasDroppedLoot = false
        }
    }

    fun isDead(): Boolean = health <= 0

    fun dispose() {
        texture.disposeSafely()
        weapons.forEach { it.dispose() }
        bullets.forEach { it.dispose() }
        trapTexture?.disposeSafely()
    }

    companion object {
        val blankTexture: Texture by lazy {
            val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap, false)
        }
    }
}
