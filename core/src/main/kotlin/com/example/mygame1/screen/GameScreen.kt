package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.world.World
import com.example.mygame1.ui.Joystick
import com.example.mygame1.ui.AttackPad
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely

class GameScreen(private val game: Main) : KtxScreen {
    private val batch = SpriteBatch()
    private val stage = Stage()
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))
    val world = World(stage, skin)
    private val touchpad = Joystick.create(stage)

    private val screenWidth = Gdx.graphics.width.toFloat()
    private val screenHeight = Gdx.graphics.height.toFloat()

    private var paused = false

    // --- Biến đếm thời gian bắt đầu ---
    private var startDelay = 5f
    private var isStartDelayActive = false
    private var firstFrameRan = false

    private val attackPad = AttackPad.create(
        stage,
        onAttackDirection = {},
        onAttackRelease = {}
    ).apply {
        setBounds(
            screenWidth - 350f,
            150f,
            200f, 200f
        )
    }

    private val blankTexture: Texture by lazy {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        Texture(pixmap, false)
    }
    private val font: BitmapFont = BitmapFont()

    override fun show() {
        Gdx.input.inputProcessor = stage
        AudioManager.playMusic("sounds/game_music.mp3")

        if (!attackPad.hasParent()) {
            stage.addActor(attackPad)
        }

        // Reset thời gian mỗi lần show screen
        startDelay = 5f
        isStartDelayActive = false // chưa đếm ngược ngay
        firstFrameRan = false
    }

    override fun render(delta: Float) {
        clearScreen(0.06f, 0.06f, 0.1f)

        // Xử lý phím ESC để chuyển sang màn hình tạm dừng
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen<PauseMenuScreen>()
            return
        }

        // Luôn cập nhật camera bám theo player
        world.camera.position.set(
            world.player.position.x + world.player.sprite.width / 2f,
            world.player.position.y + world.player.sprite.height / 2f,
            0f
        )
        world.camera.update()

        // --- Logic frame đầu tiên: luôn update để khởi tạo enemy/logic ---
        if (!firstFrameRan) {
            world.update(delta, touchpad)
            firstFrameRan = true
            isStartDelayActive = true // Bắt đầu đếm ngược sau khi đã init xong
        } else if (isStartDelayActive) {
            // Nếu đang đếm ngược thì không update logic, chỉ trừ thời gian
            startDelay -= delta
            if (startDelay <= 0f) {
                isStartDelayActive = false
                startDelay = 0f
            }
        } else if (!paused) {
            // Sau khi hết đếm ngược thì update logic như bình thường
            world.update(delta, touchpad)

            val knobX = attackPad.knobPercentX
            val knobY = attackPad.knobPercentY
            val isPadActive = attackPad.isTouched && (Math.abs(knobX) > 0.1f || Math.abs(knobY) > 0.1f)
            if (isPadActive) {
                val direction = Vector2(knobX, knobY).nor()
                world.player.sprite.rotation = direction.angleDeg()
                world.player.attack(
                    bulletsOnMap = world.player.bullets + world.enemies.flatMap { it.bullets },
                    enemyPosition = world.enemies.firstOrNull()?.position ?: Vector2.Zero
                )
            }

            if (world.player.isDead()) {
                game.setScreen<GameOverScreen>()
                return
            }
        }

        // --- Vẽ thế giới ---
        batch.projectionMatrix = world.camera.combined
        batch.begin()
        world.render(batch, font, blankTexture)
        batch.end()

        // --- Vẽ UI theo màn hình ---
        batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, screenWidth, screenHeight)
        batch.begin()
        world.player.renderUI(batch, blankTexture, font, screenWidth, screenHeight)

        // --- Vẽ thông báo đếm ngược căn giữa camera ---
        if (isStartDelayActive) {
            val text = "Start in: ${startDelay.toInt()}s"
            font.data.setScale(3f)
            font.color = Color.RED

            // Lấy tâm camera trên thế giới
            val cameraCenterWorld = Vector3(world.camera.position.x, world.camera.position.y, 0f)
            // Chuyển sang tọa độ màn hình vật lý
            val screenPos = world.camera.project(cameraCenterWorld)

            // Căn giữa text bằng GlyphLayout
            val layout = GlyphLayout(font, text)
            val drawX = screenPos.x - layout.width / 2f
            val drawY = screenPos.y + layout.height / 2f + 100f // cao hơn tâm 20px

            font.draw(batch, text, drawX, drawY)
            font.data.setScale(1f)
            font.color = Color.WHITE
        }

        batch.end()

        // --- Stage cho các nút, joystick, swapWeaponButton... ---
        stage.act(delta)
        stage.draw()
    }

    override fun pause() {
        paused = true
        AudioManager.stopMusic()
    }

    override fun resume() {
        paused = false
        AudioManager.playMusic("sounds/game_music.mp3")
    }

    override fun dispose() {
        batch.disposeSafely()
        stage.dispose()
        world.dispose()
        blankTexture.disposeSafely()
        font.disposeSafely()
    }
}
