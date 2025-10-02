package com.example.mygame1.entities

import GameState
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
    // ---------------- Character & texture ----------------
    private var currentCharacterIndex: Int = characterIndex.coerceIn(0, characterTextures.size - 1)
    private var texture: Texture = Texture(characterTextures[currentCharacterIndex])
    val sprite: Sprite = Sprite(texture).apply { setOriginCenter() }
    var position = Vector2(120f, 120f)

    // ---------------- Stats ----------------
    var health: Int = 1000
    val maxHealth: Int = 1000
    var speed = 200f

    // ---------------- Ammo ----------------
    val maxBullets = 20
    var ammoInMagazine = maxBullets
    var isReloading = false
    private var reloadTimer = 0f
    private var reloadTarget = maxBullets
    private val reloadTimePerBullet = 0.2f
    private val reloadTimeFull = 4f

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
    // Armor icon (dùng texture shield)
    var armorCount: Int = 0
    val iconArmor: Texture by lazy { Texture("character/Weapons/shield_curved.png") }

    // ---------------- Special Modes ----------------
    enum class SpecialMode { NONE, SWORD, BOMB, SHIELD, TRAP }
    var specialMode: SpecialMode = SpecialMode.NONE
        private set

    // ---------------- Cooldowns ----------------
    private var shootCooldown = 0f
    var bombCooldown = 0f
        private set
    var trapCooldown = 0f
        private set

    // ---------------- Sword (đâm thẳng) ----------------
    private var swordTex: Texture? = null
    private var swordSprite: Sprite? = null
    private var swordCooldown = 0f
    private var swordThrusting = false
    private var swordThrustElapsed = 0f
    private val swordThrustDuration = 0.20f // tổng thời gian animation đâm
    private var swordFacingAngle = 0f
    private var swordMaxReach = 0f
    private val swordDamageWidth = 40f // “độ rộng” hitbox đường đâm
    private val swordEnemiesHit = mutableSetOf<Enemy>()
    private var swordEnemySnapshot: List<Enemy> = emptyList()
    private var swordLastDamage = 0
    private var swordHandleX = 0f
    private var swordHandleY = 0f
    // Thêm biến trạng thái cho khoảng tiến hiện tại và cấu hình giữ kiếm sát người
    private var swordCurrentReach = 0f
    private val swordTravelFactor = 0.55f // % chiều dài sprite dùng để lao ra
    private val swordHoldDistance = 14f   // khoảng cách chuôi kiếm tính từ tâm nhân vật (sát người)

    // ---------------- Bomb hiển thị ----------------
    private var bombTex: Texture? = null
    private var bombSprite: Sprite? = null

    // ---------------- Getters ----------------
    val characterIndex: Int get() = currentCharacterIndex
    val weaponIndex: Int get() = currentWeaponIndex

    // =====================================================
    // Character selection
    // =====================================================
    fun selectCharacter(index: Int) {
        if (index !in characterTextures.indices) return
        currentCharacterIndex = index
        texture.disposeSafely()
        texture = Texture(characterTextures[currentCharacterIndex])
        sprite.setTexture(texture)
        sprite.setOriginCenter()
    }

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
        if (specialMode == SpecialMode.SWORD) shootCooldown = 0f
    }

    // =====================================================
    // Cooldowns & helpers
    // =====================================================
    private fun updateCooldowns(delta: Float) {
        swordCooldown = (swordCooldown - delta).coerceAtLeast(0f)
        bombCooldown = (bombCooldown - delta).coerceAtLeast(0f)
        trapCooldown = (trapCooldown - delta).coerceAtLeast(0f)
    }

    private fun ensureSwordSprite() {
        if (swordSprite != null) return
        swordTex = swordTex ?: runCatching { Texture(GunType.Sword.assetPath) }.getOrNull()
        swordTex?.let { tex ->
            swordSprite = Sprite(tex).apply {
                // Origin tại chuôi (giữ chuôi ở giữa đáy để quay/đâm đúng tay cầm)
                setOrigin(width / 2f, 0f)
                setScale(0.3f)
            }
        }
    }

    // =====================================================
    // Sword attack (đâm thẳng thay vì vung 90°)
    // =====================================================
    fun swordAttack(world: World) {
        if (specialMode != SpecialMode.SWORD) return
        if (swordCooldown > 0f || swordThrusting) return
        ensureSwordSprite()
        val stats = getGunStats(GunType.Sword)
        swordFacingAngle = sprite.rotation
        swordThrustElapsed = 0f
        swordThrusting = true
        swordEnemiesHit.clear()
        swordEnemySnapshot = world.enemies // snapshot để nhất quán trong animation
        val length = swordSprite?.let { it.height * it.scaleY } ?: stats.bulletRange
        swordMaxReach = length * swordTravelFactor
        swordCurrentReach = 0f
        swordLastDamage = stats.damage
        swordCooldown = 1f / stats.fireRate
        val anchor = getSwordAnchor()
        swordHandleX = anchor.x
        swordHandleY = anchor.y
        AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.15f)
    }

    private fun updateSwordThrust(delta: Float) {
        if (!swordThrusting) return
        swordThrustElapsed += delta
        val progress = (swordThrustElapsed / swordThrustDuration).coerceIn(0f,1f)
        // Pha đi ra (0 -> 0.5) và thu về (0.5 -> 1)
        val outwardFactor = if (progress <= 0.5f) (progress / 0.5f) else (1f - (progress - 0.5f) / 0.5f)
        val currentReach = swordMaxReach * outwardFactor
        swordCurrentReach = currentReach

        // Cập nhật anchor (nhân vật có thể di chuyển / quay trong lúc animation? Giữ hướng cũ để tạo cảm giác đâm cố định)
        val anchor = getSwordAnchor()
        swordHandleX = anchor.x
        swordHandleY = anchor.y

        val dirRad = swordFacingAngle * MathUtils.degreesToRadians
        val dirX = MathUtils.cos(dirRad)
        val dirY = MathUtils.sin(dirRad)
        val halfWidth = swordDamageWidth * 0.5f

        swordEnemySnapshot.forEach { enemy ->
            if (enemy.isDead()) return@forEach
            if (enemy in swordEnemiesHit) return@forEach
            val ex = enemy.position.x + enemy.sprite.width * 0.5f
            val ey = enemy.position.y + enemy.sprite.height * 0.5f
            val dx = ex - swordHandleX
            val dy = ey - swordHandleY
            val proj = dx * dirX + dy * dirY
            if (proj < 0f || proj > currentReach) return@forEach
            val perp = kotlin.math.abs(dx * (-dirY) + dy * dirX)
            if (perp <= halfWidth) {
                enemy.takeDamage(swordLastDamage)
                swordEnemiesHit.add(enemy)
            }
        }

        if (progress >= 1f) {
            swordThrusting = false
        }
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
                damage = stats.damage
            )
        )
        bombCooldown = 5f
        AudioManager.playSound("sounds/bomb.mp3", 0.4f)
    }

    fun canPlaceTrap(): Boolean = specialMode == SpecialMode.TRAP && trapCooldown <= 0f
    fun placeTrap(world: World) {
        if (!canPlaceTrap()) return
        val cx = position.x + sprite.width/2f
        val cy = position.y + sprite.height/2f
        world.rootEnemiesAt(Vector2(cx, cy), 140f, 3f)
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
        updateCooldowns(delta)
        if (specialMode == SpecialMode.SWORD) updateSwordThrust(delta)

        if (ammoInMagazine == 0 && !isReloading) manualReload(forceFull = true)
        if (isReloading) {
            reloadTimer -= delta
            if (reloadTimer <= 0f) {
                ammoInMagazine = reloadTarget
                isReloading = false
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

    // Anchor gần thân dành riêng cho kiếm để chuôi sát người
    private fun getSwordAnchor(): Vector2 {
        val pivotX = sprite.x + sprite.originX
        val pivotY = sprite.y + sprite.originY
        val angleRad = sprite.rotation * MathUtils.degreesToRadians
        return Vector2(
            pivotX + MathUtils.cos(angleRad) * swordHoldDistance,
            pivotY + MathUtils.sin(angleRad) * swordHoldDistance
        )
    }

    // =====================================================
    // Render
    // =====================================================
    fun render(batch: SpriteBatch) {
        sprite.draw(batch)
        when (specialMode) {
            SpecialMode.NONE -> {
                val anchor = getHandAnchor(); weapon.render(batch, anchor, sprite.rotation)
            }
            SpecialMode.SWORD -> {
                ensureSwordSprite()
                swordSprite?.let { sw ->
                    val displayAngle = swordFacingAngle
                    val dirRad = displayAngle * MathUtils.degreesToRadians
                    val dirX = MathUtils.cos(dirRad)
                    val dirY = MathUtils.sin(dirRad)
                    val baseAnchor = getSwordAnchor()
                    val handleOffset = 4f
                    swordHandleX = baseAnchor.x + dirX * handleOffset
                    swordHandleY = baseAnchor.y + dirY * handleOffset
                    val reachX = dirX * swordCurrentReach
                    val reachY = dirY * swordCurrentReach
                    sw.rotation = displayAngle
                    sw.setPosition(
                        swordHandleX + reachX - sw.originX * sw.scaleX,
                        swordHandleY + reachY - sw.originY * sw.scaleY
                    )
                    sw.draw(batch)
                }
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
                // Vẽ khiên phía trước (cùng icon) ở khoảng cách nhỏ
                val shieldTex = iconArmor
                val angleRad = sprite.rotation * MathUtils.degreesToRadians
                val dirX = MathUtils.cos(angleRad)
                val dirY = MathUtils.sin(angleRad)
                val size = 48f
                val drawX = position.x + sprite.width/2f + dirX * 20f - size/2f
                val drawY = position.y + sprite.height/2f + dirY * 20f - size/2f
                batch.draw(shieldTex, drawX, drawY, size/2f, size/2f, size, size,1f,1f,sprite.rotation,0,0,shieldTex.width,shieldTex.height,false,false)
            }
            SpecialMode.TRAP -> {
                // Hiển thị icon trap trên tay (dùng icon crosshair)
                val trapTex = runCatching { Texture(GunType.Trap.assetPath) }.getOrNull()
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
        screenWidth: Float,
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
        val ammoText = if (isReloading) "Reloading..." else "$ammoInMagazine / $maxBullets"
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
        if (weapon.type == GunType.Sword || weapon.type == GunType.Bomb) return
        val stats = getGunStats(weapon.type)
        if (isReloading || ammoInMagazine == 0) return
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
                    GunType.Sword, GunType.Bomb, GunType.Shield, GunType.Trap -> BulletType.Gun // không dùng / placeholder
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

        val gs = GameState(
            enemyPosition = enemyPosition,
            playerPosition = position.cpy(),
            bullets = bulletsOnMap
        )
        actionHistory.add(gs to PlayerAction.Shoot)

        when (weapon.type) {
            GunType.Gun -> AudioManager.playSound("sounds/submachine-gun-79846.mp3", 0.25f)
            GunType.Machine -> AudioManager.playSound("sounds/machine-gun-129928.mp3", 0.25f)
            GunType.Silencer -> AudioManager.playSound("sounds/gun-shot-359196.mp3", 0.25f)
            GunType.Sword, GunType.Bomb, GunType.Shield, GunType.Trap -> { /* no sound */ }
        }
    }

    // =====================================================
    // Reload & damage & dispose
    // =====================================================
    fun manualReload(forceFull: Boolean = false) {
        if (isReloading) return
        if (ammoInMagazine == maxBullets) return
        isReloading = true
        reloadTarget = maxBullets
        reloadTimer = if (forceFull) reloadTimeFull else (maxBullets - ammoInMagazine) * reloadTimePerBullet
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
        texture.disposeSafely()
        weapons.forEach { it.dispose() }
        bullets.forEach { it.dispose() }
        swordTex?.disposeSafely(); bombTex?.disposeSafely()
    }
}
