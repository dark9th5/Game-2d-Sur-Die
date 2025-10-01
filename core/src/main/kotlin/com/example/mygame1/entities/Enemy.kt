package com.example.mygame1.entities

import EnemyAI
import GameState
import EnemyAction
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
class Enemy(
    characterIndex: Int = -1,
    weaponIndex: Int = -1,
    spawnPosition: Vector2 = Vector2(600f, 400f),
    val ai: EnemyAI = EnemyAI(useML = true)
) {
    private var currentCharacterIndex =
        if (characterIndex in characterTextures.indices) characterIndex else Random.nextInt(characterTextures.size)
    private var texture = Texture(Gdx.files.internal(characterTextures[currentCharacterIndex]))
    val sprite = Sprite(texture).apply { setOriginCenter() }
    var position = spawnPosition.cpy()
    var health: Int = 100
    val maxHealth: Int = 100
    val speed = 50f

    val maxBullets = 20
    var ammoInMagazine = maxBullets
    var isReloading = false
    private var reloadTimer = 0f
    private var reloadTarget = maxBullets
    private val reloadTimePerBullet = 0.1f
    private val reloadTimeFull = 2f

    val bullets = mutableListOf<Bullet>()

    val weapons: List<Weapon> = listOf(
        Weapon(GunType.Gun),
        Weapon(GunType.Machine),
        Weapon(GunType.Silencer)
    )
    private var currentWeaponIndex: Int =
        if (weaponIndex in weapons.indices) weaponIndex else Random.nextInt(weapons.size)

    val weapon: Weapon
        get() = weapons[currentWeaponIndex]

    private var shootCooldown = 0f

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
        if (index in weapons.indices) {
            currentWeaponIndex = index
            shootCooldown = 0f // reset cooldown khi đổi súng
        }
    }

    /**
     * Hàm update logic đầy đủ, dùng AI ngoài (state, decideAction)
     */
    fun update(
        delta: Float,
        state: GameState,
        mapWidth: Float = 800f,
        mapHeight: Float = 600f
    ) {
        // Tự động reload khi hết đạn
        if (ammoInMagazine == 0 && !isReloading) {
            manualReload(forceFull = true)
        }
        if (isReloading) {
            reloadTimer -= delta
            if (reloadTimer <= 0f) {
                ammoInMagazine = reloadTarget
                isReloading = false
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

        // Quyết định hành động AI (phải truyền visionRange)
        when (val action = ai.decideAction(state, visionRange)) {
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
                val dir = action.direction.nor()
                sprite.rotation = dir.angleDeg()
                attack()
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
        sprite.draw(batch)
        val gunPos = getGunTipPosition()
        weapon.render(batch, gunPos, sprite.rotation)
        bullets.forEach { it.render(batch) }

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
        font?.draw(
            batch,
            if (isReloading) "Reloading..." else "$ammoInMagazine / $maxBullets",
            barX,
            barY - 2f
        )
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
        val stats = getGunStats(weapon.type)
        if (isReloading || ammoInMagazine == 0) return
        if (shootCooldown > 0f) return

        val bulletType = when (weapon.type) {
            GunType.Gun -> BulletType.Gun
            GunType.Machine -> BulletType.Machine
            GunType.Silencer -> BulletType.Silencer
            GunType.Sword, GunType.Bomb, GunType.Shield, GunType.Trap -> BulletType.Gun // placeholder for non-ranged
        }
        val bulletStart = getGunTipPosition()
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val direction = Vector2(MathUtils.cos(angleRad), MathUtils.sin(angleRad))

        bullets.add(
            Bullet(
                type = bulletType,
                position = bulletStart,
                direction = direction,
                owner = BulletOwner.ENEMY,
                maxDistance = stats.bulletRange,
                size = stats.bulletSize,
                damage = stats.damage
            )
        )
        ammoInMagazine--
        shootCooldown = 1f / stats.fireRate
        // Play sound if not silencer
        when (weapon.type) {
            GunType.Gun -> AudioManager.playSound("sounds/submachine-gun-79846.mp3",0.25f)
            GunType.Machine -> AudioManager.playSound("sounds/machine-gun-129928.mp3",0.25f)
            GunType.Silencer -> {AudioManager.playSound("sounds/gun-shot-359196.mp3",0.25f)}
            GunType.Sword, GunType.Bomb, GunType.Shield, GunType.Trap -> { /* no shooting sound */ }
        }
    }

    fun manualReload(forceFull: Boolean = false) {
        if (isReloading) return
        if (ammoInMagazine == maxBullets) return
        isReloading = true
        reloadTarget = maxBullets
        reloadTimer = if (forceFull) reloadTimeFull else (maxBullets - ammoInMagazine) * reloadTimePerBullet
    }

    fun takeDamage(amount: Int) {
        health = (health - amount).coerceAtLeast(0)
    }

    fun isDead(): Boolean = health <= 0

    fun dispose() {
        texture.disposeSafely()
        weapons.forEach { it.dispose() }
        bullets.forEach { it.dispose() }
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
