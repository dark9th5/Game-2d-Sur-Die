package com.example.mygame1.entities

import GameState
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.input.InputHandler
import com.example.mygame1.input.PlayerAction
import com.example.mygame1.world.World
import ktx.assets.disposeSafely
import kotlin.random.Random
import com.example.mygame1.Assets

// Danh sách texture nhân vật
val characterTextures: List<String> = listOf(
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
    companion object { private const val DEBUG_AMMO = false }
    // ---------------- Character & texture ----------------
    private var currentCharacterIndex: Int = characterIndex.coerceIn(0, characterTextures.size - 1)
    private var texture: Texture = Assets.texture(characterTextures[currentCharacterIndex])
    val sprite: Sprite = Sprite(texture).apply { setOriginCenter() }
    var position = Vector2(120f, 120f)

    // ---------------- Stats ----------------
    var health: Int = 500
    val maxHealth: Int = 500
    var speed = 200f

    // ---------------- Ammo (moved into Weapon) ----------------
    // Mỗi vũ khí có ammo riêng trong class Weapon. Các getter dưới đây để giữ tương thích.
    private val reloadTimePerBullet = 0.2f
    private val reloadTimeFull = 4f
    val ammoInMagazine: Int get() = weapon.ammoInMagazine
    val maxBullets: Int get() = weapon.maxBullets
    val isReloading: Boolean get() = weapon.isReloading

    // ---------------- Weapons (tầm xa) ----------------
    val weapons: List<Weapon> = listOf(
        Weapon(GunType.Gun),
        Weapon(GunType.Machine),
        Weapon(GunType.Silencer)
    )
    private var currentWeaponIndex: Int = if (weaponIndex in weapons.indices) weaponIndex else Random.nextInt(weapons.size)
    val weapon: Weapon get() = weapons[currentWeaponIndex]

    // ---------------- Bullets & action log ----------------
    val bullets = mutableListOf<Bullet>()
    val actionHistory = mutableListOf<Pair<GameState, PlayerAction>>()

    // ---------------- UI Buffs ----------------
    var speedBuffTime = 0f
    var speedBuffPercent = 0f
    var visionBuffTime = 0f
    lateinit var iconSpeed: Texture
    lateinit var iconVision: Texture
    // Armor icon (UI) và Shield weapon icon
    var armorCount: Int = 0
    val iconArmor: Texture by lazy { Texture("items/armor.png") }
    val iconShield: Texture by lazy { Texture("character/Weapons/shield_curved.png") }

    // ---------------- Special Modes ----------------
    enum class SpecialMode { NONE, BOMB, SHIELD, TRAP }
    var specialMode: SpecialMode = SpecialMode.NONE
        private set

    // ---------------- Cooldowns ----------------
    private var shootCooldown = 0f
    var bombCooldown = 0f
        private set
    var trapCooldown = 0f
        private set

    // ---------------- Bomb hiển thị ----------------
    private var bombTex: Texture? = null
    private var bombSprite: Sprite? = null
    private var trapTex: Texture? = null

    // ---------------- Movement root (trap) ----------------
    var rootTimeLeft: Float = 0f

    // ---------------- Getters ----------------
    val weaponIndex: Int get() = currentWeaponIndex

    // =====================================================
    // Character selection (removed selectCharacter as unused to clean warnings)
    // =====================================================

    fun setSpawnLeftMiddle(mapHeight: Float) {
        position.x = 0f
        position.y = (mapHeight - sprite.height) / 2f
        sprite.setPosition(position.x, position.y)
    }

    fun selectWeapon(index: Int, enemyPosition: Vector2, bulletsOnMap: List<Bullet>) {
        if (index !in weapons.indices) return
        currentWeaponIndex = index
        shootCooldown = 0f
        val gs = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(gs to PlayerAction.ChangeWeapon(index))
    }

    fun setSpecialMode(mode: SpecialMode) {
        if (mode == specialMode) return
        specialMode = mode
    }

    // =====================================================
    // Cooldowns & helpers
    // =====================================================
    private fun updateCooldowns(delta: Float) {
        bombCooldown = (bombCooldown - delta).coerceAtLeast(0f)
        trapCooldown = (trapCooldown - delta).coerceAtLeast(0f)
    }

    // =====================================================
    // Bomb
    // =====================================================
    fun canPlaceBomb(): Boolean = specialMode == SpecialMode.BOMB && bombCooldown <= 0f

    fun placeBomb(world: World) {
        if (!canPlaceBomb()) return
        val cx = position.x + sprite.width / 2f
        val cy = position.y + sprite.height / 2f
        val stats = getGunStats(GunType.Bomb)
        world.addBomb(
            com.example.mygame1.world.Bomb(
                position = Vector2(cx, cy),
                explosionRadius = stats.bulletRange,
                damage = stats.damage * 2 // tăng gấp đôi sát thương bomb cho nhân vật
            )
        )
        bombCooldown = 15f
        // Đã xóa AudioManager.playSound("sounds/bomb.mp3", 0.4f) - âm thanh sẽ phát khi bomb nổ
    }

    fun canPlaceTrap(): Boolean = specialMode == SpecialMode.TRAP && trapCooldown <= 0f
    fun placeTrap(world: World) {
        if (!canPlaceTrap()) return
        val cx = position.x + sprite.width/2f
        val cy = position.y + sprite.height/2f
        // Đặt bẫy: không áp dụng hiệu ứng ngay lập tức; chỉ khi kẻ địch bước vào mới bị trói
        world.addTrap(Vector2(cx, cy)) // Thêm trap vào World để vẽ hình trap
        trapCooldown = 5f
        AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.2f)
    }

    // =====================================================
    // Update
    // =====================================================
    fun update(
        delta: Float,
        input: InputHandler,
        enemyPosition: Vector2,
        bulletsOnMap: List<Bullet>,
        mapWidth: Float = 800f,
        mapHeight: Float = 600f
    ) {
        // underscore unused size params to silence warnings
        @Suppress("UNUSED_PARAMETER") val _mw = mapWidth
        @Suppress("UNUSED_PARAMETER") val _mh = mapHeight

        updateCooldowns(delta)

        // Buff timers
        if (weapon.ammoInMagazine == 0 && !weapon.isReloading) manualReload(forceFull = true)
        if (weapon.isReloading) {
            weapon.reloadTimer -= delta
            if (weapon.reloadTimer <= 0f) {
                weapon.ammoInMagazine = weapon.reloadTarget
                weapon.isReloading = false
            }
        }
        shootCooldown = (shootCooldown - delta).coerceAtLeast(0f)

        val moveAction = if (input.dx != 0f || input.dy != 0f)
            PlayerAction.Move(Vector2(input.dx, input.dy)) else PlayerAction.Idle
        val gs = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(gs to moveAction)
        if (actionHistory.size > 500) actionHistory.removeAt(0)

        sprite.setPosition(position.x, position.y)

        bullets.forEach { it.update(delta) }
        bullets.removeAll { !it.isActive }
    }

    // Anchor gốc dùng cho súng / bomb (giữ nguyên logic cũ nửa chiều rộng nhân vật)
    private fun getHandAnchor(): Vector2 {
        val pivotX = sprite.x + sprite.originX
        val pivotY = sprite.y + sprite.originY
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val dist = sprite.width * 0.5f
        return Vector2(
            pivotX + MathUtils.cos(angleRad) * dist,
            pivotY + MathUtils.sin(angleRad) * dist
        )
    }

    // =====================================================
    // Render
    // =====================================================
    fun render(batch: SpriteBatch) {
        sprite.draw(batch)
        if (rootTimeLeft > 0f) {
            RootFontHolder.font.color = Color.RED
            RootFontHolder.font.data.setScale(1.2f)
            RootFontHolder.font.draw(batch, "unable to move", sprite.x - 10f, sprite.y + sprite.height + 30f)
            RootFontHolder.font.data.setScale(1f)
        }
        when (specialMode) {
            SpecialMode.NONE -> {
                val anchor = getHandAnchor(); weapon.render(batch, anchor, sprite.rotation)
            }
            SpecialMode.BOMB -> {
                if (bombSprite == null) {
                    bombTex = bombTex ?: runCatching { Texture(GunType.Bomb.assetPath) }.getOrNull()
                    bombTex?.let { bombSprite = Sprite(it).apply { setOriginCenter(); setScale(0.5f) } }
                }
                bombSprite?.let { bs ->
                    val anchor = getHandAnchor()
                    bs.rotation = sprite.rotation
                    bs.setPosition(anchor.x - bs.width/2f, anchor.y - bs.height/2f)
                    bs.draw(batch)
                }
            }
            SpecialMode.SHIELD -> {
                // Vẽ khiên phía trước (dùng icon của vũ khí khiên)
                val shieldTex = iconShield
                val angleRad = sprite.rotation * MathUtils.degreesToRadians
                val dirX = MathUtils.cos(angleRad)
                val dirY = MathUtils.sin(angleRad)
                val size = 48f
                val drawX = position.x + sprite.width/2f + dirX * 20f - size/2f
                val drawY = position.y + sprite.height/2f + dirY * 20f - size/2f
                batch.draw(shieldTex, drawX, drawY, size/2f, size/2f, size, size,1f,1f,sprite.rotation,0,0,shieldTex.width,shieldTex.height,false,false)
            }
            SpecialMode.TRAP -> {
                // Hiển thị icon trap trên tay (cache texture để tránh tạo lại mỗi frame)
                if (trapTex == null) {
                    trapTex = runCatching { Texture(GunType.Trap.assetPath) }.getOrNull()
                }
                trapTex?.let { t ->
                    val anchor = getHandAnchor()
                    batch.draw(t, anchor.x - 16f, anchor.y - 16f, 16f,16f,32f,32f,1f,1f,0f,0,0,t.width,t.height,false,false)
                }
            }
        }
        bullets.forEach { it.render(batch) }
    }

    fun renderUI(
        batch: SpriteBatch,
        blankTexture: Texture,
        font: BitmapFont,
        _screenWidth: Float,
        screenHeight: Float
    ) {
        val marginLeft = 40f
        var uiY = screenHeight - 40f

        // Thanh máu
        val barWidth = 360f
        val barHeight = 32f
        uiY -= barHeight
        val healthRatio = health / maxHealth.toFloat()
        batch.color = Color.DARK_GRAY
        batch.draw(blankTexture, marginLeft - 3, uiY - 3, barWidth + 6, barHeight + 6)
        batch.color = Color.RED
        batch.draw(blankTexture, marginLeft, uiY, barWidth, barHeight)
        batch.color = Color.GREEN
        batch.draw(blankTexture, marginLeft, uiY, barWidth * healthRatio, barHeight)
        batch.color = Color.WHITE

        // Đạn
        val ammoHeight = 32f
        uiY -= ammoHeight
        font.color = Color.SALMON
        font.data.setScale(2f)
        val ammoText = if (weapon.isReloading) "Reloading..." else "${weapon.ammoInMagazine} / ${weapon.maxBullets}"
        font.draw(batch, ammoText, marginLeft, uiY + ammoHeight * 0.7f)
        font.data.setScale(1f)
        font.color = Color.WHITE

        // Buff icons
        val iconSize = 64f
        var buffY = uiY - iconSize - 24f
        if (::iconSpeed.isInitialized && speedBuffTime > 0f) {
            batch.draw(iconSpeed, marginLeft, buffY, iconSize, iconSize)
            font.data.setScale(4f)
            font.color = Color.RED
            font.draw(batch, "${speedBuffTime.toInt()}s", marginLeft + iconSize + 18f, buffY + iconSize * 0.75f)
            buffY -= iconSize + 16f
            font.data.setScale(1f)
            font.color = Color.WHITE
        }
        if (::iconVision.isInitialized && visionBuffTime > 0f) {
            batch.draw(iconVision, marginLeft, buffY, iconSize, iconSize)
            font.data.setScale(4f)
            font.color = Color.RED
            font.draw(batch, "${visionBuffTime.toInt()}s", marginLeft + iconSize + 18f, buffY + iconSize * 0.75f)
            buffY -= iconSize + 16f
            font.data.setScale(1f)
            font.color = Color.WHITE
        }
        // After buff icons, add armor count
        if (armorCount > 0) {
            val iconSize = 64f
            val armorY = uiY - 140f // below ammo area
            batch.draw(iconArmor, marginLeft, armorY - iconSize, iconSize, iconSize)
            font.data.setScale(4f)
            font.color = Color.CYAN
            font.draw(batch, "x$armorCount", marginLeft + iconSize + 18f, armorY - iconSize/2f)
            font.data.setScale(1f)
            font.color = Color.WHITE
        }
    }

    // =====================================================
    // Ranged attack (guns)
    // =====================================================
    fun attack(enemyPosition: Vector2, bulletsOnMap: List<Bullet>) {
        if ( weapon.type == GunType.Bomb) return
        val stats = getGunStats(weapon.type)
        if (weapon.isReloading || weapon.ammoInMagazine == 0) return
        if (shootCooldown > 0f) return
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        val direction = Vector2(MathUtils.cos(angleRad), MathUtils.sin(angleRad))
        val anchor = getHandAnchor()
        val bulletStart = anchor.cpy().add(direction.cpy().scl(weapon.sprite.width * 0.5f))

        bullets.add(
            Bullet(
                type = when (weapon.type) {
                    GunType.Gun -> BulletType.Gun
                    GunType.Machine -> BulletType.Machine
                    GunType.Silencer -> BulletType.Silencer
                    GunType.Bomb, GunType.Shield, GunType.Trap -> BulletType.Gun
                },
                position = bulletStart,
                direction = direction,
                maxDistance = stats.bulletRange,
                size = stats.bulletSize,
                damage = stats.damage * 2 // tăng gấp đôi sát thương súng cho nhân vật
            )
        )
        weapon.ammoInMagazine--
        if (DEBUG_AMMO) debugPrintAllWeaponAmmo("After shot ${weapon.type}")
        shootCooldown = 1f / stats.fireRate

        val gs = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(gs to PlayerAction.Shoot)
        if (actionHistory.size > 500) actionHistory.removeAt(0)

        when (weapon.type) {
            GunType.Gun -> AudioManager.playSound("sounds/submachine-gun-79846.mp3", 0.25f)
            GunType.Machine -> AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.25f)
            GunType.Silencer -> { /* no sound */ }
            GunType.Bomb, GunType.Shield, GunType.Trap -> { /* no sound */ }
        }
    }

    private fun debugPrintAllWeaponAmmo(context: String) {
        val sb = StringBuilder("[AmmoDebug] $context | ")
        weapons.forEach { w -> sb.append("${w.type}:${w.ammoInMagazine}/${w.maxBullets} ") }
        Gdx.app.log("Player", sb.toString())
    }

    // =====================================================
    // Reload & damage & dispose
    // =====================================================
    fun manualReload(forceFull: Boolean = false) {
        if (weapon.isReloading) return
        if (weapon.ammoInMagazine == weapon.maxBullets) return
        weapon.isReloading = true
        weapon.reloadTarget = weapon.maxBullets
        weapon.reloadTimer = if (forceFull) reloadTimeFull else (weapon.maxBullets - weapon.ammoInMagazine) * reloadTimePerBullet
    }

    fun takeDamage(amount: Int) {
        if (armorCount > 0) {
            armorCount -= 1
            AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.15f)
            return
        }
        health = (health - amount).coerceAtLeast(0)
    }
    fun isDead(): Boolean = health <= 0

    fun dispose() {
        // texture shared via Assets
        weapons.forEach { it.dispose() }
        bullets.forEach { it.dispose() }
        bombTex?.disposeSafely()
        trapTex?.disposeSafely()
    }
}

private object RootFontHolder { val font = BitmapFont() }
