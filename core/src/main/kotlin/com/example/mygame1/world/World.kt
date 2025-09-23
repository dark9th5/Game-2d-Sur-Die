package com.example.mygame1.world

import GameState
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
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

    private val collisionManager = CollisionManager(tileLayer, map)

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
        swapWeaponButton.label.setFontScale(2.0f)
        swapWeaponButton.setSize(120f, 60f)
        swapWeaponButton.setPosition(1700f, 120f)
        swapWeaponButton.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent?,
                x: Float, y: Float, pointer: Int, button: Int
            ): Boolean {
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
        reloadButton.label.setFontScale(2.0f)
        reloadButton.setSize(120f, 60f)
        reloadButton.setPosition(1700f, 300f)
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
    }

    fun trySpawnItem(pos: Vector2) {
        val r = Random.nextFloat()
        val type = when {
            r < 0.4f -> ItemType.SPEED
            r < 0.8f -> ItemType.HEAL
            else -> ItemType.VISION
        }
        items.add(Item(type, pos.cpy()))
    }

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
                ItemType.HEAL -> {
                    player.health = (player.health + 5).coerceAtMost(player.maxHealth)
                }
                ItemType.SPEED -> {
                    val percent = 0.1f + Random.nextFloat() * 0.2f
                    if (speedBuffTime <= 0f) speedBuffPercent = percent
                    speedBuffTime += 10f
                }
                ItemType.VISION -> {
                    visionBuffTime += 15f
                }
            }
            item.aliveTime = 999f
        }
        items.removeAll { it.isExpired() }

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
            respawnQueue.add(RespawnEntry(5f))
        }
        enemies.removeAll { it.isDead() }

        // Xử lý respawn sau 5 giây
        if (respawnQueue.isNotEmpty()) {
            val iterator = respawnQueue.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                entry.timeLeft -= delta
                if (entry.timeLeft <= 0f) {
                    val spawnPos = getValidSpawnPosition()
                    val newEnemy = Enemy(spawnPosition = spawnPos)
                    newEnemy.setCollisionManager(collisionManager)
                    enemies.add(newEnemy)
                    iterator.remove()
                }
            }
        }

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
}
