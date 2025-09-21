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
import com.example.mygame1.audio.AudioManager

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
    characterIndex: Int = 0,
    weaponIndex: Int = -1
) {
    private var currentCharacterIndex: Int
    private var texture: Texture
    val sprite: Sprite
    var position = Vector2(120f, 120f)

    var health: Int = 100
    val maxHealth: Int = 100
    var speed = 200f

    private val detectRange: Float = 600f

    val maxBullets = 20
    var ammoInMagazine = maxBullets
    var isReloading = false
    private var reloadTimer = 0f
    private var reloadTarget = maxBullets
    private val reloadTimePerBullet = 0.2f
    private val reloadTimeFull = 4f

    val weapons: List<Weapon> = listOf(
        Weapon(GunType.Gun),
        Weapon(GunType.Machine),
        Weapon(GunType.Silencer)
    )
    private var currentWeaponIndex: Int =
        if (weaponIndex in weapons.indices) weaponIndex else Random.nextInt(weapons.size)

    val weapon: Weapon
        get() = weapons[currentWeaponIndex]

    val bullets = mutableListOf<Bullet>()
    val actionHistory = mutableListOf<Pair<GameState, PlayerAction>>()

    val characterIndex: Int
        get() = currentCharacterIndex

    val weaponIndex: Int
        get() = currentWeaponIndex

    private var shootCooldown = 0f

    // --- Các biến UI buff ---
    var speedBuffTime = 0f
    var speedBuffPercent = 0f
    var visionBuffTime = 0f

    lateinit var iconSpeed: Texture
    lateinit var iconVision: Texture

    init {
        currentCharacterIndex = characterIndex.coerceIn(0, characterTextures.size - 1)
        texture = Texture(characterTextures[currentCharacterIndex].toInternalFile())
        sprite = Sprite(texture).apply { setOriginCenter() }
    }

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
        position.x = 0f
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
            shootCooldown = 0f
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

    private fun getGunTipPosition(): Vector2 {
        val gunOffsetX = sprite.width / 2f
        val gunOffsetY = 0f
        val angleRad = sprite.rotation * MathUtils.degreesToRadians

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
        val marginLeft = 40f
        var uiY = screenHeight - 40f

        // --- Thanh máu ---
        val barWidth = 360f
        val barHeight = 32f
        uiY -= barHeight
        val barX = marginLeft
        val barY = uiY
        val healthRatio = health / maxHealth.toFloat()
        batch.color = Color.DARK_GRAY
        batch.draw(blankTexture, barX - 3, barY - 3, barWidth + 6, barHeight + 6)
        batch.color = Color.RED
        batch.draw(blankTexture, barX, barY, barWidth, barHeight)
        batch.color = Color.GREEN
        batch.draw(blankTexture, barX, barY, barWidth * healthRatio, barHeight)
        batch.color = Color.WHITE

        // --- Số lượng đạn ---
        val ammoHeight = 32f
        uiY -= ammoHeight
        font.color = Color.SALMON
        font.data.setScale(2.0f)
        val ammoText = if (isReloading)
            "Reloading..."
        else
            "$ammoInMagazine / $maxBullets"
        font.draw(batch, ammoText, marginLeft, uiY + ammoHeight * 0.7f)
        font.data.setScale(1.0f)
        font.color = Color.WHITE

        // --- Icon buff: to gấp đôi, luôn nằm dưới dòng đạn ---
        val iconSize = 64f // gấp đôi (nếu trước là 32)
        var buffY = uiY - iconSize - 24f // cách dòng đạn một khoảng đủ lớn

        if (::iconSpeed.isInitialized && speedBuffTime > 0f) {
            batch.draw(iconSpeed, marginLeft, buffY, iconSize, iconSize)
            font.data.setScale(4.0f) // text buff to gấp đôi
            font.color = Color.ORANGE
            font.draw(
                batch,
                "${speedBuffTime.toInt()}s",
                marginLeft + iconSize + 18f,
                buffY + iconSize * 0.75f
            )
            buffY -= iconSize + 16f
            font.data.setScale(1.0f)
            font.color = Color.WHITE
        }
        if (::iconVision.isInitialized && visionBuffTime > 0f) {
            batch.draw(iconVision, marginLeft, buffY, iconSize, iconSize)
            font.data.setScale(4.0f)
            font.color = Color.ORANGE
            font.draw(
                batch,
                "${visionBuffTime.toInt()}s",
                marginLeft + iconSize + 18f,
                buffY + iconSize * 0.75f
            )
            buffY -= iconSize + 16f
            font.data.setScale(1.0f)
            font.color = Color.WHITE
        }
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

        val centerX = position.x + sprite.width / 2f
        val centerY = position.y + sprite.height / 2f
        val playerCenter = Vector2(centerX, centerY)

        val weaponLength = weapon.sprite.width
        val bulletStart = playerCenter.cpy().add(direction.cpy().scl(weaponLength))

        bullets.add(
            Bullet(
                type = when (weapon.type) {
                    GunType.Gun -> BulletType.Gun
                    GunType.Machine -> BulletType.Machine
                    GunType.Silencer -> BulletType.Silencer
                },
                position = bulletStart,
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

        when (weapon.type) {
            GunType.Gun -> AudioManager.playSound("sounds/submachine-gun-79846.mp3", 0.25f)
            GunType.Machine -> AudioManager.playSound("sounds/machine-gun-129928.mp3", 0.25f)
            GunType.Silencer -> AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.25f)
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
}
