package com.example.mygame1.world

import GameState
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
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
import ktx.style.get
import ktx.style.skin

class World(
    val stage: Stage,
    val skin: Skin,
    characterIndex: Int = -1,
    weaponIndex: Int = -1
) {

    private val map: TiledMap = TmxMapLoader().load("map/sampleMap.tmx")
    val tileLayer = map.layers.get("Object") as TiledMapTileLayer

    private val renderer = OrthogonalTiledMapRenderer(map)
    val camera = OrthographicCamera()
    val player = Player(characterIndex, weaponIndex)
    val enemies = mutableListOf<Enemy>()
    private val starField = StarField(400, sizeScale = 0.25f)

    private val swapWeaponButton: TextButton
    private val reloadButton: TextButton

    val collisionManager = CollisionManager(tileLayer, map) // was private

    private val bombs = mutableListOf<Bomb>()
    fun addBomb(bomb: Bomb) { bombs.add(bomb) }

    // Splash_bg cuộn ngoài rìa map
    private val splashBgTexture = Texture("background/splash_bg.png")
    private var splashBgScrollX = 0f
    private val splashBgScrollSpeed = 60f

    // Item system
    val items = mutableListOf<Item>()

    // UI icon buff
    private val iconSpeed = Item.textureSpeed
    private val iconVision = Item.textureVision

    // Buff/hiệu ứng (World chỉ quản lý logic, player sẽ render)
    private var speedBuffTime = 0f
    private var visionBuffTime = 0f
    private var speedBuffPercent = 0f
    private val defaultPlayerSpeed = 200f
    private val defaultCameraZoom = 0.5f
    private val visionBuffZoom = 0.75f

    // Score & Respawn
    var score: Int = 0

    private data class RespawnEntry(var timeLeft: Float)
    private val respawnQueue = mutableListOf<RespawnEntry>()

    private data class SlashEffect(
        val pivotX: Float,
        val pivotY: Float,
        val startAngle: Float,
        val endAngle: Float,
        val radius: Float,
        var elapsed: Float = 0f,
        val duration: Float = 0.18f
    )
    private val slashEffects = mutableListOf<SlashEffect>()

    fun addSlashEffect(pivotX: Float, pivotY: Float, startAngle: Float, endAngle: Float, radius: Float) {
        slashEffects.add(SlashEffect(pivotX, pivotY, startAngle, endAngle, radius))
    }

    // Texture 1x1 dùng để vẽ hiệu ứng (chưa khai báo trước đó)
    private val pixelTexture: Texture by lazy {
        val pm = Pixmap(1,1, Pixmap.Format.RGBA8888)
        pm.setColor(1f,1f,1f,1f)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        t
    }

    private fun getValidSpawnPosition(): Vector2 {
        val mapW = getMapWidth()
        val mapH = getMapHeight()
        val maxTry = 50
        val size = 40f
        for (i in 1..maxTry) {
            val x = (mapW * 0.1f) + Math.random().toFloat() * (mapW * 0.8f)
            val y = mapH * 0.2f + Math.random().toFloat() * (mapH * 0.6f)
            val rect = Rectangle(x, y, size, size)
            if (!collisionManager.isBlocked(rect)) {
                return Vector2(x, y)
            }
        }
        return Vector2(mapW / 2f, mapH / 2f)
    }

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

    fun positionActionButtonsLeftOfAttack(attackX: Float, attackY: Float, attackWidth: Float, attackHeight: Float, scale: Float) {
        // Đặt 2 nút đối xứng qua tâm attackPad: Reload ở trên, Swap ở dưới
        val gap = 80f * scale // tăng khoảng cách giữa hai nút
        val swapW = 120f * scale
        val swapH = 60f * scale
        val reloadW = 120f * scale
        val reloadH = 60f * scale
        val centerY = attackY + attackHeight / 2f
        val targetX = (attackX - gap - swapW).coerceAtLeast(10f * scale)
        // Reload trên tâm
        val reloadY = centerY + gap / 2f
        // Swap dưới tâm sao cho khoảng cách tới tâm giống reload
        val swapY = centerY - swapH - gap / 2f
        reloadButton.setSize(reloadW, reloadH)
        swapWeaponButton.setSize(swapW, swapH)
        reloadButton.setPosition(targetX, reloadY)
        swapWeaponButton.setPosition(targetX, swapY)
        reloadButton.label.setFontScale(2f * scale)
        swapWeaponButton.label.setFontScale(2f * scale)
    }

    fun resizeUI() {
        positionActionButtons()
    }

    init {
        camera.setToOrtho(false)
        camera.zoom = defaultCameraZoom
        camera.update()
        player.setSpawnLeftMiddle(getMapHeight())
        val mapW = getMapWidth()
        val mapH = getMapHeight()
        for (i in 0 until 7) {
            val spawnPos = getValidSpawnPosition()
            val enemy = Enemy(spawnPosition = spawnPos)
            enemy.setCollisionManager(collisionManager)
            enemies.add(enemy)
        }

        // Truyền icon buff cho player
        player.iconSpeed = iconSpeed
        player.iconVision = iconVision

        skin.add("buttonUp", Texture("control/buttonLong_blue.png"))
        skin.add("buttonDown", Texture("control/buttonLong_blue_pressed.png"))

        val upTexture = skin.get<Texture>("buttonUp")
        val downTexture = skin.get<Texture>("buttonDown")

        val upDrawable = TextureRegionDrawable(TextureRegion(upTexture))
        val downDrawable = TextureRegionDrawable(TextureRegion(downTexture))
        val swapButtonStyle = TextButtonStyle().apply {
            up = upDrawable
            down = downDrawable
            font = skin.getFont("default")
            fontColor = Color.WHITE
        }

        val reloadButtonStyle = TextButtonStyle().apply {
            up = upDrawable
            down = downDrawable
            font = skin.getFont("default")
            fontColor = Color.WHITE
        }

        swapWeaponButton = TextButton("Swap gun", swapButtonStyle)
        swapWeaponButton.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent?,
                x: Float, y: Float, pointer: Int, button: Int
            ): Boolean {
                // Nếu đang ở special mode (sword/bomb) thì thoát về dùng súng hiện tại
                if (player.specialMode != com.example.mygame1.entities.Player.SpecialMode.NONE) {
                    player.setSpecialMode(com.example.mygame1.entities.Player.SpecialMode.NONE)
                    return true
                }
                val nextIndex = (player.weaponIndex + 1) % player.weapons.size
                player.selectWeapon(
                    nextIndex,
                    enemyPosition = player.position,
                    bulletsOnMap = player.bullets
                )
                return true
            }
        })
        stage.addActor(swapWeaponButton)

        reloadButton = TextButton("Reload", reloadButtonStyle)
        reloadButton.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent?,
                x: Float, y: Float, pointer: Int, button: Int
            ): Boolean {
                player.manualReload(forceFull = false)
                return true
            }
        })
        stage.addActor(reloadButton)

        positionActionButtons()
    }

    fun rootEnemiesAt(center: Vector2, radius: Float, duration: Float) {
        enemies.forEach { e ->
            val dist2 = e.position.dst2(center)
            if (dist2 <= radius * radius) {
                e.rootTimeLeft = maxOf(e.rootTimeLeft, duration)
            }
        }
    }

    fun trySpawnItem(pos: Vector2) {
        val r = Random.nextFloat()
        // New distribution: SPEED 30%, HEAL 30%, VISION 15%, ARMOR 25%
        val type = when {
            r < 0.30f -> ItemType.SPEED
            r < 0.60f -> ItemType.HEAL
            r < 0.75f -> ItemType.VISION
            else -> ItemType.ARMOR // 25%
        }
        items.add(Item(type, pos.cpy()))
    }

    // Trap system
    data class Trap(val position: Vector2, var aliveTime: Float = 30f)
    private val traps = mutableListOf<Trap>()
    fun addTrap(pos: Vector2) { traps.add(Trap(pos)) }

    fun update(delta: Float, touchpad: Touchpad) {
        splashBgScrollX = (splashBgScrollX + splashBgScrollSpeed * delta) % splashBgTexture.width

        val input = InputHandler(touchpad)
        val dx = input.dx
        val dy = input.dy
        if (dx != 0f || dy != 0f) {
            val angle = com.badlogic.gdx.math.MathUtils.atan2(dy, dx)
            val vx = com.badlogic.gdx.math.MathUtils.cos(angle) * player.speed * delta
            val vy = com.badlogic.gdx.math.MathUtils.sin(angle) * player.speed * delta

            val newX = (player.position.x + vx).coerceIn(0f, getMapWidth() - player.sprite.width)
            val newY = (player.position.y + vy).coerceIn(0f, getMapHeight() - player.sprite.height)

            val rectX = Rectangle(newX, player.position.y, player.sprite.width - 2f, player.sprite.height - 2f)
            if (!collisionManager.isBlocked(rectX)) {
                player.position.x = newX
            }
            val rectY = Rectangle(player.position.x, newY, player.sprite.width - 2f, player.sprite.height - 2f)
            if (!collisionManager.isBlocked(rectY)) {
                player.position.y = newY
            }
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
        starField.update(delta, getMapWidth(), getMapHeight())

        enemies.forEach { enemy ->
            val state = GameState(
                enemyPosition = enemy.position,
                playerPosition = player.position.cpy(),
                bullets = player.bullets + enemies.flatMap { it.bullets }
            )
            val visionRange = getGunStats(enemy.weapon.type).bulletRange
            val action = enemy.ai.decideAction(state, visionRange)
            if (action is EnemyAction.Move) {
                val dir = action.direction.nor()
                val vx = dir.x * enemy.speed * delta
                val vy = dir.y * enemy.speed * delta
                val newX = (enemy.position.x + vx).coerceIn(0f, getMapWidth() - enemy.sprite.width)
                val newY = (enemy.position.y + vy).coerceIn(0f, getMapHeight() - enemy.sprite.height)

                val rectX = Rectangle(newX, enemy.position.y, enemy.sprite.width - 2f, enemy.sprite.height - 2f)
                if (!collisionManager.isBlocked(rectX)) {
                    enemy.position.x = newX
                }
                val rectY = Rectangle(enemy.position.x, newY, enemy.sprite.width - 2f, enemy.sprite.height - 2f)
                if (!collisionManager.isBlocked(rectY)) {
                    enemy.position.y = newY
                }
                enemy.sprite.rotation = dir.angleDeg()
                enemy.sprite.setPosition(enemy.position.x, enemy.position.y)
            }
            enemy.update(
                delta,
                state = state,
                mapWidth = getMapWidth(),
                mapHeight = getMapHeight()
            )
        }

        player.bullets.forEach { bullet ->
            if (!bullet.isActive) return@forEach
            if (collisionManager.isBulletBlocked(bullet.bounds())) {
                bullet.isActive = false
            }
        }
        enemies.forEach { enemy ->
            enemy.bullets.forEach { bullet ->
                if (!bullet.isActive) return@forEach
                if (collisionManager.isBulletBlocked(bullet.bounds())) {
                    bullet.isActive = false
                }
            }
        }

        handleCollisions()

        // ITEM: update và remove
        items.forEach { it.update(delta) }
        items.removeAll { it.isExpired() }

        // ITEM: player pick up
        val playerRect = player.sprite.boundingRectangle
        val picked = items.filter { playerRect.overlaps(it.bounds()) }
        for (item in picked) {
            when (item.type) {
                ItemType.HEAL -> { player.health = (player.health + player.maxHealth / 2).coerceAtMost(player.maxHealth) }
                ItemType.SPEED -> {
                    val percent = 0.1f + Random.nextFloat() * 0.2f
                    if (speedBuffTime <= 0f) speedBuffPercent = percent
                    speedBuffTime += 10f
                }
                ItemType.VISION -> { visionBuffTime += 15f }
                ItemType.ARMOR -> { player.armorCount += 1 }
            }
            item.aliveTime = 999f
        }
        items.removeAll { it.isExpired() }

        // BOMBS: update và remove
        bombs.forEach { it.update(delta, this) }
        bombs.removeAll { it.isFinished() }

        // TRAPS: update và remove
        traps.forEach { it.aliveTime -= delta }
        traps.removeAll { it.aliveTime <= 0f }

        // Buff tốc độ
        if (speedBuffTime > 0f) {
            speedBuffTime -= delta
            player.speed = defaultPlayerSpeed * (1f + speedBuffPercent)
            player.speedBuffTime = speedBuffTime
            player.speedBuffPercent = speedBuffPercent
            if (speedBuffTime <= 0f) {
                player.speed = defaultPlayerSpeed
                speedBuffPercent = 0f
                player.speedBuffPercent = 0f
            }
        } else {
            player.speedBuffTime = 0f
            player.speedBuffPercent = 0f
        }

        // Buff camera (zoom mượt, nhìn xa hơn khi có vision)
        if (visionBuffTime > 0f) {
            visionBuffTime -= delta
            player.visionBuffTime = visionBuffTime
            camera.zoom += (visionBuffZoom - camera.zoom) * 0.12f
            if (visionBuffTime <= 0f) {
                player.visionBuffTime = 0f
                camera.zoom += (defaultCameraZoom - camera.zoom) * 0.12f
            }
        } else {
            player.visionBuffTime = 0f
            if (camera.zoom > defaultCameraZoom) {
                camera.zoom += (defaultCameraZoom - camera.zoom) * 0.12f
            }
        }

        // Xử lý cộng điểm, spawn item và lên lịch respawn khi địch chết
        val deadEnemies = enemies.filter { it.isDead() }
        for (enemy in deadEnemies) {
            trySpawnItem(enemy.position)
            score += 10
            // respawnQueue.add(RespawnEntry(5f)) // Remove respawn logic
        }
        if (deadEnemies.isNotEmpty()) {
            enemyKilledThisFrame = true
        }
        enemies.removeAll { it.isDead() }

        // Check if all enemies are dead
        if (enemies.isEmpty()) {
            allEnemiesDead = true // Add a flag to indicate all enemies are dead
        }

        // Remove respawn logic
        // if (respawnQueue.isNotEmpty()) {
        //     val iterator = respawnQueue.iterator()
        //     while (iterator.hasNext()) {
        //         val entry = iterator.next()
        //         entry.timeLeft -= delta
        //         if (entry.timeLeft <= 0f) {
        //             val spawnPos = getValidSpawnPosition()
        //             // ...existing code...
        //         }
        //     }
        // }

        // Cập nhật hiệu ứng chém
        slashEffects.forEach { it.elapsed += delta }
        slashEffects.removeAll { it.elapsed >= it.duration }

        camera.position.set(
            player.position.x + player.sprite.width / 2,
            player.position.y + player.sprite.height / 2,
            0f
        )
        camera.update()
    }

    fun render(batch: SpriteBatch, font: BitmapFont, blankTexture: Texture) {
        batch.projectionMatrix = camera.combined

        // Vẽ splash_bg cuộn ngoài rìa map
        val camLeft = camera.position.x - camera.viewportWidth * 0.5f * camera.zoom
        val camBottom = camera.position.y - camera.viewportHeight * 0.5f * camera.zoom
        val camRight = camera.position.x + camera.viewportWidth * 0.5f * camera.zoom
        val camTop = camera.position.y + camera.viewportHeight * 0.5f * camera.zoom

        val tileWidth = splashBgTexture.width.toFloat()
        val tileHeight = splashBgTexture.height.toFloat()
        val scrollOffsetX = splashBgScrollX % tileWidth
        var x = camLeft - scrollOffsetX

        while (x < camRight) {
            var y = camBottom - (camBottom % tileHeight) - tileHeight
            while (y < camTop) {
                batch.draw(splashBgTexture, x, y, tileWidth, tileHeight)
                y += tileHeight
            }
            x += tileWidth
        }

        batch.color = Color.WHITE
        for (star in starField.stars) {
            val xStar = star.position.x
            val yStar = star.position.y
            val outside = xStar < 0f || xStar > getMapWidth() || yStar < 0f || yStar > getMapHeight()
            if (outside) {
                val color = Color(star.brightness, star.brightness, star.brightness, 1f)
                batch.color = color
                batch.draw(
                    starField.starTexture,
                    xStar - star.size / 2,
                    yStar - star.size / 2,
                    star.size / 2,
                    star.size / 2,
                    star.size,
                    star.size,
                    1f,
                    1f,
                    star.rotation,
                    0,
                    0,
                    starField.starTexture.width,
                    starField.starTexture.height,
                    false,
                    false
                )
            }
        }
        batch.color = Color.WHITE

        renderer.setView(camera)
        renderer.render()

        // Render item trước player/enemy
        items.forEach { it.render(batch) }
        bombs.forEach { it.render(batch) }

        // Vẽ trap
        val trapTex = runCatching { Texture("character/Weapons/weapon_trap.png") }.getOrNull()
        traps.forEach { trap ->
            trapTex?.let {
                batch.draw(it, trap.position.x - 16f, trap.position.y - 16f, 32f, 32f)
            }
        }

        // Render hiệu ứng chém (vẽ các đoạn nhỏ tạo cung)
        if (slashEffects.isNotEmpty()) {
            val step = 6f // độ mỗi đoạn
            slashEffects.forEach { eff ->
                val alpha = 1f - (eff.elapsed / eff.duration)
                val color = Color(1f, 1f, 0.2f, alpha * 0.6f)
                batch.color = color
                var a = eff.startAngle
                while (a <= eff.endAngle) {
                    val rad = a * com.badlogic.gdx.math.MathUtils.degreesToRadians
                    val x2 = eff.pivotX + com.badlogic.gdx.math.MathUtils.cos(rad) * eff.radius
                    val y2 = eff.pivotY + com.badlogic.gdx.math.MathUtils.sin(rad) * eff.radius
                    val w = 12f
                    val h = 4f
                    batch.draw(pixelTexture, x2 - w/2f, y2 - h/2f, w, h)
                    a += step
                }
            }
            batch.color = Color.WHITE
        }

        player.render(batch)
        enemies.forEach { enemy -> enemy.render(batch, font) }

        // Vẽ Score bám theo camera: giữa mép trên
        val scoreText = "Score: $score"
        font.data.setScale(2.5f)
        font.color = Color.ORANGE
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, scoreText)
        val xCenter = camera.position.x - layout.width / 2f
        val yTop = camera.position.y + camera.viewportHeight * 0.5f * camera.zoom - 10f
        font.draw(batch, layout, xCenter, yTop)
        font.data.setScale(1f)

        // --- UI cố định trên màn hình: vẽ toàn bộ UI player tại đây ---
        batch.projectionMatrix = stage.camera.combined
        player.renderUI(batch, blankTexture, font, stage.viewport.worldWidth, stage.viewport.worldHeight)
    }

    fun dispose() {
        map.disposeSafely()
        renderer.disposeSafely()
        player.dispose()
        enemies.forEach { it.dispose() }
        starField.dispose()
        splashBgTexture.disposeSafely()
        bombs.forEach { it.dispose() }
        pixelTexture.disposeSafely()
        // Không dispose iconSpeed/iconVision ở đây vì dùng chung với Item
    }

    private fun getMapWidth(): Float =
        map.properties.get("width", Int::class.java) * map.properties.get("tilewidth", Int::class.java).toFloat()

    private fun getMapHeight(): Float =
        map.properties.get("height", Int::class.java) * map.properties.get("tileheight", Int::class.java).toFloat()

    private fun handleCollisions() {
        for (bullet in player.bullets) {
            if (!bullet.isActive) continue
            for (enemy in enemies) {
                if (enemy.isDead()) continue
                if (bullet.bounds().overlaps(enemy.sprite.boundingRectangle)) {
                    enemy.takeDamage(bullet.damage)
                    bullet.isActive = false
                    break
                }
            }
        }
        player.bullets.removeAll { !it.isActive }

        for (enemy in enemies) {
            for (bullet in enemy.bullets) {
                if (!bullet.isActive) continue
                if (bullet.bounds().overlaps(player.sprite.boundingRectangle)) {
                    // Shield front blocking
                    if (player.specialMode == Player.SpecialMode.SHIELD) {
                        val facingAngleRad = player.sprite.rotation * com.badlogic.gdx.math.MathUtils.degreesToRadians
                        val facing = Vector2(com.badlogic.gdx.math.MathUtils.cos(facingAngleRad), com.badlogic.gdx.math.MathUtils.sin(facingAngleRad))
                        // bullet.direction là hướng bay (từ enemy -> player). Nếu dot < 0 nghĩa là tới từ phía trước mặt player
                        val dot = bullet.direction.dot(facing)
                        if (dot < 0f) { bullet.isActive = false; continue }
                    }
                    player.takeDamage(bullet.damage)
                    bullet.isActive = false
                }
            }
            enemy.bullets.removeAll { !it.isActive }
        }

        // Lưu ý: KHÔNG remove enemy chết tại đây nữa.
        // Việc spawn item, cộng điểm và respawn được xử lý trong update() sau khi va chạm.
    }

    fun isPlayerTouchingBorder(padding: Float = 10f): Boolean {
        val x = player.position.x
        val y = player.position.y
        val width = player.sprite.width
        val height = player.sprite.height
        return (
            x <= padding ||
                y <= padding ||
                x + width >= getMapWidth() - padding ||
                y + height >= getMapHeight() - padding
            )
    }

    var allEnemiesDead: Boolean = false

    // Đặt ở đầu class, cùng với các biến public khác
    var enemyKilledThisFrame: Boolean = false
}
