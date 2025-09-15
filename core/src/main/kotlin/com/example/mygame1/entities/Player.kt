package com.example.mygame1.entities

import GameState
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.example.mygame1.input.InputHandler
import com.example.mygame1.input.PlayerAction
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import kotlin.random.Random

val characterTextures = listOf(
    "character/Characters/hitman1_hold.png",
    "character/Characters/manBlue_hold.png",
    "character/Characters/manBrown_hold.png",
    "character/Characters/manOld_hold.png",
    "character/Characters/robot1_hold.png",
    "character/Characters/soldier1_hold.png",
    "character/Characters/survivor1_hold.png",
    "character/Characters/womanGreen_hold.png",
    "character/Characters/zombie1_hold.png"
)

class Player(
    characterIndex: Int = -1
) {
    private var currentCharacterIndex =
        if (characterIndex in characterTextures.indices) characterIndex else randomizeCharacter()
    private var texture = Texture(characterTextures[currentCharacterIndex].toInternalFile())
    val sprite = Sprite(texture).apply { setOriginCenter() }
    var position = Vector2(120f, 120f)

    var health: Int = 100
    val maxHealth: Int = 100
    val speed = 200f

    private val detectRange: Float = 600f

    val maxBullets = 20
    var ammoInMagazine = maxBullets
    var isReloading = false
    private var reloadTimer = 0f
    private var reloadTarget = maxBullets // Số viên sẽ nạp lại (toàn bộ hoặc số đã mất)
    private val reloadTimePerBullet = 0.2f
    private val reloadTimeFull = 4f // Tự động reload khi hết đạn

    val weapons: List<Weapon> = listOf(
        Weapon(GunType.Gun),
        Weapon(GunType.Machine),
        Weapon(GunType.Silencer)
    )
    private var currentWeaponIndex: Int = randomizeWeapon()

    val weapon: Weapon
        get() = weapons[currentWeaponIndex]

    val bullets = mutableListOf<Bullet>()
    val actionHistory = mutableListOf<Pair<GameState, PlayerAction>>()

    val characterIndex: Int
        get() = currentCharacterIndex

    val weaponIndex: Int
        get() = currentWeaponIndex

    private var shootCooldown = 0f

    private fun randomizeWeapon(): Int = Random.nextInt(weapons.size)
    private fun randomizeCharacter(): Int = Random.nextInt(characterTextures.size)

    fun selectCharacter(index: Int) {
        if (index in characterTextures.indices) {
            currentCharacterIndex = index
            texture.disposeSafely()
            texture = Texture(characterTextures[currentCharacterIndex].toInternalFile())
            sprite.setTexture(texture)
            sprite.setOriginCenter()
        }
    }
    fun setSpawnLeftMiddle(mapHeight: Float) {
        position.x = 0f // Sát cạnh trái, có thể là 0f nếu muốn
        position.y = (mapHeight - sprite.height) / 2f
        sprite.setPosition(position.x, position.y)
    }
    fun selectWeapon(
        index: Int,
        enemyPosition: Vector2,
        bulletsOnMap: List<Bullet>
    ) {
        if (index in weapons.indices) {
            currentWeaponIndex = index
            shootCooldown = 0f // Sửa: reset cooldown khi đổi súng
            val currentState = GameState(
                enemyPosition = enemyPosition,
                playerPosition = position.cpy(),
                bullets = bulletsOnMap
            )
            actionHistory.add(currentState to PlayerAction.ChangeWeapon(index))
        }
    }

    fun update(
        delta: Float,
        input: InputHandler,
        enemyPosition: Vector2,
        bulletsOnMap: List<Bullet>,
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

        val dx = input.dx
        val dy = input.dy

        val moveAction = if (dx != 0f || dy != 0f) {
            PlayerAction.Move(Vector2(dx, dy))
        } else {
            PlayerAction.Idle
        }

        val currentState = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(currentState to moveAction)

        sprite.setPosition(position.x, position.y)

        bullets.forEach { it.update(delta) }
        bullets.removeAll { !it.isActive }
    }

    // Chỉnh vị trí đầu súng theo offset trên sprite (giả sử là cạnh phải giữa sprite)
    private fun getGunTipPosition(): Vector2 {
        // Offset từ tâm sprite đến đầu súng (giả sử cạnh phải giữa)
        val gunOffsetX = sprite.width / 2f
        val gunOffsetY = 0f
        val angleRad = sprite.rotation * MathUtils.degreesToRadians

        // Xoay offset theo góc
        val rotatedOffsetX = gunOffsetX * MathUtils.cos(angleRad) - gunOffsetY * MathUtils.sin(angleRad)
        val rotatedOffsetY = gunOffsetX * MathUtils.sin(angleRad) + gunOffsetY * MathUtils.cos(angleRad)

        val centerX = position.x + sprite.width / 2f
        val centerY = position.y + sprite.height / 2f
        return Vector2(
            centerX + rotatedOffsetX,
            centerY + rotatedOffsetY
        )
    }

    fun render(batch: SpriteBatch) {
        sprite.draw(batch)
        val gunPos = getGunTipPosition()
        weapon.render(batch, gunPos, sprite.rotation)
        bullets.forEach { it.render(batch) }
    }

    fun renderUI(
        batch: SpriteBatch,
        blankTexture: Texture,
        font: BitmapFont,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val barWidth = 360f
        val barHeight = 32f
        val barX = 40f
        val barY = screenHeight - barHeight - 40f

        val healthRatio = health / maxHealth.toFloat()
        batch.color = Color.DARK_GRAY
        batch.draw(blankTexture, barX - 3, barY - 3, barWidth + 6, barHeight + 6)
        batch.color = Color.RED
        batch.draw(blankTexture, barX, barY, barWidth, barHeight)
        batch.color = Color.GREEN
        batch.draw(blankTexture, barX, barY, barWidth * healthRatio, barHeight)
        batch.color = Color.WHITE

        font.color = Color.SALMON
        font.data.setScale(2.0f)
        val ammoText = if (isReloading)
            "Reloading..."
        else
            "$ammoInMagazine / $maxBullets"
        font.draw(batch, ammoText, barX, barY - 20f)
        font.data.setScale(1.0f)
        font.color = Color.WHITE
    }

    fun attack(
        enemyPosition: Vector2,
        bulletsOnMap: List<Bullet>
    ) {
        val stats = getGunStats(weapon.type)
        if (isReloading || ammoInMagazine == 0) return
        if (shootCooldown > 0f) return

        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val direction = Vector2(MathUtils.cos(angleRad), MathUtils.sin(angleRad))

        // Lấy tâm người chơi
        val centerX = position.x + sprite.width / 2f
        val centerY = position.y + sprite.height / 2f
        val playerCenter = Vector2(centerX, centerY)

        // Đẩy viên đạn ra xa tâm 1 khoảng bằng chiều dài súng
        val weaponLength = weapon.sprite.width // hoặc weapon.sprite.height nếu sprite xoay đứng
        val bulletStart = playerCenter.cpy().add(direction.cpy().scl(weaponLength))

        bullets.add(
            Bullet(
                type = when (weapon.type) {
                    GunType.Gun -> BulletType.Gun
                    GunType.Machine -> BulletType.Machine
                    GunType.Silencer -> BulletType.Silencer
                },
                position = bulletStart,   // ✅ spawn cách tâm 1 khoảng weaponLength
                direction = direction,
                owner = BulletOwner.PLAYER,
                maxDistance = stats.bulletRange,
                size = stats.bulletSize,
                damage = stats.damage
            )
        )
        ammoInMagazine--
        shootCooldown = 1f / stats.fireRate

        val currentState = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(currentState to PlayerAction.Shoot)
    }

    // Gọi hàm này khi bấm nút reload
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
}
