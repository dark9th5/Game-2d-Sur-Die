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
import com.example.mygame1.entities.Bullet
import ktx.style.get
import ktx.style.skin

class World(
    val stage: Stage,
    val skin: Skin
) {

    private val map: TiledMap = TmxMapLoader().load("map/sampleMap.tmx")
    val tileLayer = map.layers.get("Object") as TiledMapTileLayer

    private val renderer = OrthogonalTiledMapRenderer(map)
    val camera = OrthographicCamera()
    val player = Player()
    val enemies = mutableListOf<Enemy>()
    private val starField = StarField(400, sizeScale = 0.25f)

    private val swapWeaponButton: TextButton
    private val reloadButton: TextButton
    private val collisionManager = CollisionManager(tileLayer, map)

    // Spawn enemy tại vị trí hợp lệ (không bị vật cản)
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
        camera.zoom = 0.5f
        camera.update()
        player.setSpawnLeftMiddle(getMapHeight())
        val mapW = getMapWidth()
        val mapH = getMapHeight()
        for (i in 0 until 7) {
            val spawnPos = getValidSpawnPosition()
            enemies.add(
                Enemy(
                    characterIndex = player.characterIndex,
                    weaponIndex = player.weaponIndex,
                    spawnPosition = spawnPos
                )
            )
        }

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

    fun update(delta: Float, touchpad: Touchpad) {
        val input = InputHandler(touchpad)
        // --- PLAYER COLLISION --- (Di chuyển mượt khi sát tường)
        val dx = input.dx
        val dy = input.dy
        if (dx != 0f || dy != 0f) {
            val angle = com.badlogic.gdx.math.MathUtils.atan2(dy, dx)
            val vx = com.badlogic.gdx.math.MathUtils.cos(angle) * player.speed * delta
            val vy = com.badlogic.gdx.math.MathUtils.sin(angle) * player.speed * delta

            // TÁCH KIỂM TRA VA CHẠM THEO TỪNG TRỤC X/Y
            val newX = (player.position.x + vx).coerceIn(0f, getMapWidth() - player.sprite.width)
            val newY = (player.position.y + vy).coerceIn(0f, getMapHeight() - player.sprite.height)

            // Di chuyển X trước
            val rectX = Rectangle(newX, player.position.y, player.sprite.width - 2f, player.sprite.height - 2f)
            if (!collisionManager.isBlocked(rectX)) {
                player.position.x = newX
            }
            // Di chuyển Y sau
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

        // --- ENEMY COLLISION ---
        enemies.forEach { enemy ->
            val state = GameState(
                enemyPosition = enemy.position,
                playerPosition = player.position.cpy(),
                bullets = player.bullets + enemies.flatMap { it.bullets }
            )
            val action = enemy.ai.decideAction(state)
            if (action is EnemyAction.Move) {
                val dir = action.direction.nor()
                val vx = dir.x * enemy.speed * delta
                val vy = dir.y * enemy.speed * delta
                val newX = (enemy.position.x + vx).coerceIn(0f, getMapWidth() - enemy.sprite.width)
                val newY = (enemy.position.y + vy).coerceIn(0f, getMapHeight() - enemy.sprite.height)

                // TÁCH KIỂM TRA VA CHẠM X/Y CHO ENEMY
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

        // --- BULLET COLLISION ---
        player.bullets.forEach { bullet ->
            if (!bullet.isActive) return@forEach
            // SỬA: dùng bounding box của viên đạn để kiểm tra tường/vật cản
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

        camera.position.set(
            player.position.x + player.sprite.width / 2,
            player.position.y + player.sprite.height / 2,
            0f
        )
        camera.update()
    }

    fun render(batch: SpriteBatch, font: BitmapFont, blankTexture: Texture) {
        batch.projectionMatrix = camera.combined
        val mapW = getMapWidth()
        val mapH = getMapHeight()
        batch.color = Color.WHITE
        for (star in starField.stars) {
            val x = star.position.x
            val y = star.position.y
            val outside = x < 0f || x > mapW || y < 0f || y > mapH
            if (outside) {
                val color = Color(star.brightness, star.brightness, star.brightness, 1f)
                batch.color = color
                batch.draw(
                    starField.starTexture,
                    x - star.size / 2,
                    y - star.size / 2,
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

        player.render(batch)
        enemies.forEach { enemy -> enemy.render(batch, font) }
    }

    fun dispose() {
        map.disposeSafely()
        renderer.disposeSafely()
        player.dispose()
        enemies.forEach { it.dispose() }
        starField.dispose()
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
                // SỬA: kiểm tra bounding box viên đạn overlaps với bounding box enemy
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

        enemies.removeAll { it.isDead() }
    }
}
