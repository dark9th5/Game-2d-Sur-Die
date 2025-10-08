package com.example.mygame1.world

import GameState
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.graphics.Color
import ktx.assets.disposeSafely
import com.example.mygame1.entities.Player
import com.example.mygame1.entities.Enemy
import com.example.mygame1.input.InputHandler
import com.example.mygame1.entities.getGunStats
import com.example.mygame1.entities.Item
import com.example.mygame1.entities.ItemType
import kotlin.random.Random
import com.example.mygame1.data.Difficulty
import PlayerBehaviorTracker
import ktx.style.get
import com.example.mygame1.Assets
import com.badlogic.gdx.graphics.g2d.GlyphLayout

class World(
    val stage: Stage,
    val skin: Skin,
    characterIndex: Int = -1,
    weaponIndex: Int = -1,
    val difficulty: Difficulty = Difficulty.EASY
) {
    // Map & rendering
    private val map: TiledMap = TmxMapLoader().load("map/sampleMap.tmx")
    private val renderer = OrthogonalTiledMapRenderer(map)
    val camera = OrthographicCamera()

    // Entities
    val player = Player(characterIndex, weaponIndex)
    val enemies = mutableListOf<Enemy>()
    private val starField = StarField(400, sizeScale = 0.25f)
    private val bombs = mutableListOf<Bomb>()
    fun addBomb(bomb: Bomb) { bombs.add(bomb) }

    // Items
    val items = mutableListOf<Item>()

    // UI buttons (swap / reload)
    private val swapWeaponButton: TextButton
    private val reloadButton: TextButton

    // Collision
    val collisionManager = CollisionManager(map)

    // Background scrolling
    private val splashBgTexture = Assets.texture("background/splash_bg.png")
    private var splashBgScrollX = 0f
    private val splashBgScrollSpeed = 60f
    private val trapTexture = runCatching { Assets.texture("character/Weapons/weapon_trap.png") }.getOrNull()

    // Buff resources shared với Player UI
    private val iconSpeed = Item.textureSpeed
    private val iconVision = Item.textureVision

    // Buff logic state (World quản lý giá trị, Player render UI)
    private var speedBuffTime = 0f
    private var visionBuffTime = 0f
    private var speedBuffPercent = 0f
    private val defaultPlayerSpeed = 200f
    private val defaultCameraZoom = 0.6f
    private val visionBuffZoom = 0.7f

    // Score
    var score: Int = 0
    private val scoreLayout = GlyphLayout() // Added for rendering centered score text

    // Trap system
    private val MIN_TRAP_DISTANCE = 60f
    data class Trap(
        val position: Vector2,
        var aliveTime: Float = 30f,
        var elapsed: Float = 0f,
        val activationDelay: Float = 1f
    ) {
        val isActive: Boolean get() = elapsed >= activationDelay
    }
    private val traps = mutableListOf<Trap>()
    private val trapRadius = 16f
    fun addTrap(pos: Vector2) { traps.add(Trap(pos)) }
    fun canPlaceTrapAt(pos: Vector2, minDistance: Float = MIN_TRAP_DISTANCE): Boolean = traps.all { it.position.dst(pos) >= minDistance }

    // Hard difficulty enemy special ability cooldown maps
    private val enemyBombCooldown = mutableMapOf<Enemy, Float>()
    private val enemyTrapCooldown = mutableMapOf<Enemy, Float>()

    // Tracker hành vi người chơi
    val behaviorTracker: PlayerBehaviorTracker = PlayerBehaviorTracker()

    private fun uiScale(): Float {
        val baseH = 1080f
        return (stage.viewport.worldHeight / baseH).coerceIn(0.6f, 1.5f)
    }

    private fun positionActionButtons() {
        val scale = uiScale()
        val margin = 40f * scale
        val swapW = 120f * scale
        val swapH = 60f * scale
        val reloadW = 120f * scale
        val reloadH = 60f * scale
        swapWeaponButton.setSize(swapW, swapH)
        reloadButton.setSize(reloadW, reloadH)
        val rightX = stage.viewport.worldWidth - swapW - margin
        swapWeaponButton.setPosition(rightX, 120f * scale)
        reloadButton.setPosition(rightX, swapWeaponButton.y + swapWeaponButton.height + (120f * scale))
        swapWeaponButton.label.setFontScale(2f * scale)
        reloadButton.label.setFontScale(2f * scale)
    }

    fun positionActionButtonsLeftOfAttack(attackX: Float, attackY: Float, _attackWidth: Float, attackHeight: Float, scale: Float) {
        val gap = 80f * scale
        val swapW = 120f * scale
        val swapH = 60f * scale
        val reloadW = 120f * scale
        val reloadH = 60f * scale
        val centerY = attackY + attackHeight / 2f
        val targetX = (attackX - gap - swapW).coerceAtLeast(10f * scale)
        val reloadY = centerY + gap / 2f
        val swapY = centerY - swapH - gap / 2f
        reloadButton.setSize(reloadW, reloadH)
        swapWeaponButton.setSize(swapW, swapH)
        reloadButton.setPosition(targetX, reloadY)
        swapWeaponButton.setPosition(targetX, swapY)
        reloadButton.label.setFontScale(2f * scale)
        swapWeaponButton.label.setFontScale(2f * scale)
    }

    init {
        camera.setToOrtho(false)
        camera.zoom = defaultCameraZoom
        camera.update()
        player.setSpawnLeftMiddle(getMapHeight())
        // Difficulty scaling for enemies
        val (hpMul, dmgMul) = when(difficulty) {
            Difficulty.EASY -> 1f to 1f
            Difficulty.NORMAL -> 1.5f to 1.5f
            Difficulty.HARD -> 1.5f to 1.5f // Hard: same stats as Normal but smarter abilities
        }
        repeat(7) {
            val spawnPos = getValidSpawnPosition()
            val ability = if (difficulty == Difficulty.HARD) {
                val abilities = com.example.mygame1.entities.EnemyAbility.entries.toTypedArray()
                abilities[kotlin.random.Random.nextInt(abilities.size)]
            } else com.example.mygame1.entities.EnemyAbility.GUN
            val isHard = difficulty == Difficulty.HARD
            val enemy = Enemy(spawnPosition = spawnPos, healthMultiplier = hpMul, damageMultiplier = dmgMul, selectedAbility = ability, isHardMode = isHard)
            enemy.setCollisionManager(collisionManager)
            enemies.add(enemy)
            if (difficulty == Difficulty.HARD) {
                enemyBombCooldown[enemy] = Random.nextFloat() * 3f
                enemyTrapCooldown[enemy] = Random.nextFloat() * 3f
            }
        }
        player.iconSpeed = iconSpeed
        player.iconVision = iconVision

        skin.add("buttonUp", Texture("control/buttonLong_blue.png"))
        skin.add("buttonDown", Texture("control/buttonLong_blue_pressed.png"))
        val upTexture = skin.get<Texture>("buttonUp")
        val downTexture = skin.get<Texture>("buttonDown")
        val upDrawable = TextureRegionDrawable(TextureRegion(upTexture))
        val downDrawable = TextureRegionDrawable(TextureRegion(downTexture))
        val swapButtonStyle = TextButtonStyle().apply { up = upDrawable; down = downDrawable; font = skin.getFont("default"); fontColor = Color.WHITE }
        val reloadButtonStyle = TextButtonStyle().apply { up = upDrawable; down = downDrawable; font = skin.getFont("default"); fontColor = Color.WHITE }

        swapWeaponButton = TextButton("Swap gun", swapButtonStyle).also { btn ->
            btn.addListener(object: InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    if (player.specialMode != Player.SpecialMode.NONE) { player.setSpecialMode(Player.SpecialMode.NONE); return true }
                    val nextIndex = (player.weaponIndex + 1) % player.weapons.size
                    player.selectWeapon(nextIndex, enemyPosition = player.position, bulletsOnMap = player.bullets)
                    return true
                }
            })
            stage.addActor(btn)
        }
        reloadButton = TextButton("Reload", reloadButtonStyle).also { btn ->
            btn.addListener(object: InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    player.manualReload(forceFull = false); return true
                }
            })
            stage.addActor(btn)
        }
        positionActionButtons()
    }

    private fun getValidSpawnPosition(): Vector2 {
        val mapW = getMapWidth(); val mapH = getMapHeight(); val maxTry = 50; val size = 40f
        repeat(maxTry) {
            val x = (mapW * 0.1f) + Math.random().toFloat() * (mapW * 0.8f)
            val y = mapH * 0.2f + Math.random().toFloat() * (mapH * 0.6f)
            val rect = Rectangle(x, y, size, size)
            if (!collisionManager.isBlocked(rect)) return Vector2(x, y)
        }
        return Vector2(mapW / 2f, mapH / 2f)
    }

    private fun handleCollisions() {
        // Player bullets -> enemies
        for (bullet in player.bullets) {
            if (!bullet.isActive) continue
            for (enemy in enemies) {
                if (enemy.isDead()) continue
                if (bullet.bounds().overlaps(enemy.sprite.boundingRectangle)) { enemy.takeDamage(bullet.damage); bullet.isActive = false; behaviorTracker.onPlayerHitEnemy(); break }
            }
        }
        player.bullets.removeAll { !it.isActive }
        // Enemy bullets -> player
        for (enemy in enemies) {
            for (bullet in enemy.bullets) {
                if (!bullet.isActive) continue
                if (bullet.bounds().overlaps(player.sprite.boundingRectangle)) {
                    if (player.specialMode == Player.SpecialMode.SHIELD) {
                        val facingAngleRad = player.sprite.rotation * com.badlogic.gdx.math.MathUtils.degreesToRadians
                        val facing = Vector2(com.badlogic.gdx.math.MathUtils.cos(facingAngleRad), com.badlogic.gdx.math.MathUtils.sin(facingAngleRad))
                        if (bullet.direction.dot(facing) < 0f) { bullet.isActive = false; continue }
                    }
                    player.takeDamage(bullet.damage); bullet.isActive = false
                }
            }
            enemy.bullets.removeAll { !it.isActive }
        }
    }

    private fun handleItemPickups() {
        if (items.isEmpty()) return
        val playerRect = player.sprite.boundingRectangle
        val removeList = mutableListOf<Item>()
        items.forEach { item ->
            if (playerRect.overlaps(item.bounds())) {
                when (item.type) {
                    ItemType.HEAL -> player.health = (player.health + 200).coerceAtMost(player.maxHealth)
                    ItemType.SPEED -> { speedBuffPercent = 0.5f; speedBuffTime = 15f; player.speedBuffPercent = speedBuffPercent; player.speedBuffTime = speedBuffTime }
                    ItemType.VISION -> { visionBuffTime = 15f; player.visionBuffTime = visionBuffTime }
                    ItemType.ARMOR -> player.armorCount += 1
                }
                removeList.add(item)
            }
        }
        if (removeList.isNotEmpty()) items.removeAll(removeList)
    }

    private fun updateHardEnemyAbilities(delta: Float) {
        if (difficulty != Difficulty.HARD) return

        val gunStats = getGunStats(com.example.mygame1.entities.GunType.Gun)
        val machineStats = getGunStats(com.example.mygame1.entities.GunType.Machine)
        val silencerStats = getGunStats(com.example.mygame1.entities.GunType.Silencer)
        enemies.forEach { enemy ->
            if (enemy.isWaitingRevive || enemy.isDead()) return@forEach
            val bombCd = (enemyBombCooldown[enemy] ?: 0f) - delta
            val trapCd = (enemyTrapCooldown[enemy] ?: 0f) - delta
            enemyBombCooldown[enemy] = bombCd
            enemyTrapCooldown[enemy] = trapCd

            when (enemy.selectedAbility) {
                com.example.mygame1.entities.EnemyAbility.GUN -> {
                    enemy.selectWeapon(0)
                    val dist = enemy.position.dst(player.position)
                    if (dist < gunStats.bulletRange) {
                        val dir = player.position.cpy().sub(enemy.position).nor()
                        enemy.sprite.rotation = dir.angleDeg()
                        enemy.attack()
                    }
                }
                com.example.mygame1.entities.EnemyAbility.MACHINE -> {
                    enemy.selectWeapon(1)
                    val dist = enemy.position.dst(player.position)
                    if (dist < machineStats.bulletRange) {
                        val dir = player.position.cpy().sub(enemy.position).nor()
                        enemy.sprite.rotation = dir.angleDeg()
                        enemy.attack()
                    }
                }
                com.example.mygame1.entities.EnemyAbility.SILENCER -> {
                    enemy.selectWeapon(2)
                    val dist = enemy.position.dst(player.position)
                    if (dist < silencerStats.bulletRange) {
                        val dir = player.position.cpy().sub(enemy.position).nor()
                        enemy.sprite.rotation = dir.angleDeg()
                        enemy.attack()
                    }
                }
                com.example.mygame1.entities.EnemyAbility.TRAP -> {
                    if (trapCd <= 0f) {
                        if (canPlaceTrapAt(enemy.position)) {
                            addTrap(enemy.position.cpy())
                            enemyTrapCooldown[enemy] = 6f
                        }
                    }
                }
            }
        }
    }

    fun update(delta: Float, touchpad: Touchpad) {
        splashBgScrollX = (splashBgScrollX + splashBgScrollSpeed * delta) % splashBgTexture.width

        // Movement input
        val input = InputHandler(touchpad)
        if (player.rootTimeLeft > 0f) {
            player.rootTimeLeft = (player.rootTimeLeft - delta).coerceAtLeast(0f)
        } else if (input.dx != 0f || input.dy != 0f) {
            val angle = com.badlogic.gdx.math.MathUtils.atan2(input.dy, input.dx)
            val vx = com.badlogic.gdx.math.MathUtils.cos(angle) * player.speed * delta
            val vy = com.badlogic.gdx.math.MathUtils.sin(angle) * player.speed * delta
            val newX = (player.position.x + vx).coerceIn(0f, getMapWidth() - player.sprite.width)
            val newY = (player.position.y + vy).coerceIn(0f, getMapHeight() - player.sprite.height)
            val rectX = Rectangle(newX, player.position.y, player.sprite.width - 2f, player.sprite.height - 2f)
            if (!collisionManager.isBlocked(rectX)) player.position.x = newX
            val rectY = Rectangle(player.position.x, newY, player.sprite.width - 2f, player.sprite.height - 2f)
            if (!collisionManager.isBlocked(rectY)) player.position.y = newY
            player.sprite.rotation = angle * com.badlogic.gdx.math.MathUtils.radiansToDegrees
            player.sprite.setPosition(player.position.x, player.position.y)
        }

        player.update(
            delta,
            input,
            enemyPosition = enemies.firstOrNull()?.position ?: player.position,
            bulletsOnMap = player.bullets + enemies.flatMap { it.bullets },
            mapWidth = getMapWidth(),
            mapHeight = getMapHeight()
        )
        behaviorTracker.updateFrame(delta, player, enemies)
        val snapshot = behaviorTracker.snapshot()
        starField.update(delta, getMapWidth(), getMapHeight())

        // Enemy AI + movement
        enemies.forEach { enemy ->
            val state = GameState(enemyPosition = enemy.position, playerPosition = player.position.cpy(), bullets = player.bullets + enemies.flatMap { it.bullets })
            enemy.update(delta, state, getMapWidth(), getMapHeight(), behaviorSnapshot = snapshot)
        }

        updateHardEnemyAbilities(delta)

        // Bullets vs walls
        player.bullets.forEach { if (it.isActive && collisionManager.isBulletBlocked(it.bounds())) it.isActive = false }
        enemies.forEach { e -> e.bullets.forEach { b -> if (b.isActive && collisionManager.isBulletBlocked(b.bounds())) b.isActive = false } }

        handleCollisions()
        items.forEach { it.update(delta) }
        handleItemPickups()
        bombs.forEach { it.update(delta, this) }
        bombs.removeAll { it.isFinished() }

        traps.forEach { trap ->
            trap.elapsed += delta
            trap.aliveTime -= delta
        }

        val trapsToRemove = mutableListOf<Trap>()
        enemies.forEach { enemy ->
            traps.forEach { trap ->
                if (!trap.isActive) return@forEach
                val enemyRect = enemy.sprite.boundingRectangle
                val enemyCx = enemyRect.x + enemyRect.width / 2f
                val enemyCy = enemyRect.y + enemyRect.height / 2f
                val dx = enemyCx - trap.position.x
                val dy = enemyCy - trap.position.y
                if (dx*dx + dy*dy <= trapRadius*trapRadius) {
                    if (enemy.rootTimeLeft <= 0f) enemy.rootTimeLeft = 1f
                    trapsToRemove.add(trap)
                }
            }
        }
        traps.forEach { trap ->
            if (!trap.isActive) return@forEach
            val playerRect = player.sprite.boundingRectangle
            val playerCx = playerRect.x + playerRect.width/2f
            val playerCy = playerRect.y + playerRect.height/2f
            val dx = playerCx - trap.position.x
            val dy = playerCy - trap.position.y
            if (dx*dx + dy*dy <= trapRadius*trapRadius) {
                if (player.rootTimeLeft <= 0f) player.rootTimeLeft = 1f
                trapsToRemove.add(trap)
            }
        }
        traps.removeAll { it.aliveTime <= 0f }
        traps.removeAll(trapsToRemove)

        // Speed buff
        if (speedBuffTime > 0f) {
            speedBuffTime -= delta
            player.speed = defaultPlayerSpeed * (1f + speedBuffPercent)
            player.speedBuffTime = speedBuffTime; player.speedBuffPercent = speedBuffPercent
            if (speedBuffTime <= 0f) { player.speed = defaultPlayerSpeed; speedBuffPercent = 0f; player.speedBuffPercent = 0f }
        } else { player.speedBuffTime = 0f; player.speedBuffPercent = 0f }

        // Vision buff (camera zoom smooth)
        if (visionBuffTime > 0f) {
            visionBuffTime -= delta; player.visionBuffTime = visionBuffTime
            camera.zoom += (visionBuffZoom - camera.zoom) * 0.12f
            if (visionBuffTime <= 0f) player.visionBuffTime = 0f
        } else if (camera.zoom > defaultCameraZoom) {
            camera.zoom += (defaultCameraZoom - camera.zoom) * 0.12f
        }

        // Enemy deaths -> score & item spawn
        enemies.forEach { enemy ->
            if (enemy.isDead() && enemy.isWaitingRevive && !enemy.hasDroppedLoot) {
                trySpawnItem(enemy.position)
                score += 10
                enemy.hasDroppedLoot = true
            }
        }

        camera.position.set(player.position.x + player.sprite.width/2f, player.position.y + player.sprite.height/2f, 0f)
        camera.update()
    }

    // Attempt to spawn an item at a given position when an enemy dies.
    // Drop chance & distribution can be tuned here.
    private fun trySpawnItem(enemyPos: Vector2) {
        // 50% chance to drop an item
        if (Random.nextFloat() > 0.5f) return
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.40f -> ItemType.HEAL
            roll < 0.60f -> ItemType.SPEED
            roll < 0.80f -> ItemType.VISION
            else -> ItemType.ARMOR
        }
        // Try small random offset so multiple drops don't perfectly overlap
        val attempts = 6
        val size = 32f
        for (i in 0 until attempts) {
            val ox = Random.nextFloat() * 48f - 24f
            val oy = Random.nextFloat() * 48f - 24f
            val px = (enemyPos.x + ox).coerceIn(0f, getMapWidth() - size)
            val py = (enemyPos.y + oy).coerceIn(0f, getMapHeight() - size)
            val rect = Rectangle(px, py, size, size)
            if (!collisionManager.isBlocked(rect)) {
                items.add(Item(type, Vector2(px, py)))
                return
            }
        }
        // Fallback: place exactly at enemy position if no free spot found
        val px = enemyPos.x.coerceIn(0f, getMapWidth() - size)
        val py = enemyPos.y.coerceIn(0f, getMapHeight() - size)
        items.add(Item(type, Vector2(px, py)))
    }

    fun render(batch: SpriteBatch, font: BitmapFont) {
        batch.projectionMatrix = camera.combined

        // Parallax-ish background tiling
        val camLeft = camera.position.x - camera.viewportWidth * 0.5f * camera.zoom
        val camBottom = camera.position.y - camera.viewportHeight * 0.5f * camera.zoom
        val camRight = camera.position.x + camera.viewportWidth * 0.5f * camera.zoom
        val camTop = camera.position.y + camera.viewportHeight * 0.5f * camera.zoom
        val tileW = splashBgTexture.width.toFloat(); val tileH = splashBgTexture.height.toFloat()
        val scrollOffsetX = splashBgScrollX % tileW
        var x = camLeft - scrollOffsetX
        while (x < camRight) { var y = camBottom - (camBottom % tileH) - tileH; while (y < camTop) { batch.draw(splashBgTexture, x, y, tileW, tileH); y += tileH }; x += tileW }

        batch.color = Color.WHITE
        starField.stars.forEach { star ->
            val outside = star.position.x < 0f || star.position.x > getMapWidth() || star.position.y < 0f || star.position.y > getMapHeight()
            if (outside) {
                val c = Color(star.brightness, star.brightness, star.brightness, 1f)
                batch.color = c
                batch.draw(starField.starTexture, star.position.x - star.size/2f, star.position.y - star.size/2f, star.size/2f, star.size/2f, star.size, star.size,1f,1f, star.rotation,0,0,starField.starTexture.width, starField.starTexture.height,false,false)
            }
        }
        batch.color = Color.WHITE

        renderer.setView(camera); renderer.render()
        items.forEach { it.render(batch) }
        bombs.forEach { it.render(batch) }

        // Vẽ trap từ cache
        traps.forEach { trap -> trapTexture?.let { batch.draw(it, trap.position.x - 16f, trap.position.y - 16f, 32f, 32f) } }

        player.render(batch)
        enemies.forEach { it.render(batch, font) }

        // Score centered top (camera space)
        val scoreText = "Score: $score"
        font.data.setScale(2.5f); font.color = Color.ORANGE
        scoreLayout.setText(font, scoreText)
        val xCenter = camera.position.x - scoreLayout.width/2f
        val yTop = camera.position.y + camera.viewportHeight * 0.5f * camera.zoom - 10f
        font.draw(batch, scoreLayout, xCenter, yTop)
        font.data.setScale(1f); font.color = Color.WHITE
    }

    fun dispose() {
        map.disposeSafely(); renderer.disposeSafely(); player.dispose(); enemies.forEach { it.dispose() }; starField.dispose(); bombs.forEach { it.dispose() }
    }

    private fun getMapWidth(): Float = map.properties.get("width", Int::class.java) * map.properties.get("tilewidth", Int::class.java).toFloat()
    private fun getMapHeight(): Float = map.properties.get("height", Int::class.java) * map.properties.get("tileheight", Int::class.java).toFloat()

    fun isPlayerTouchingBorder(padding: Float = 10f): Boolean {
        val x = player.position.x; val y = player.position.y; val w = player.sprite.width; val h = player.sprite.height
        return (x <= padding || y <= padding || x + w >= getMapWidth() - padding || y + h >= getMapHeight() - padding)
    }
}
