package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.ui.AttackPad
import com.example.mygame1.ui.Joystick
import com.example.mygame1.world.World
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely

class GameScreen(private val game: Main) : KtxScreen {
    private val batch = SpriteBatch()
    // Dùng ScreenViewport để tận dụng full màn hình thiết bị
    private val viewport = ScreenViewport()
    private val stage = Stage(viewport)
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))

    val world = World(stage, skin, game.selectedCharacterIndex, game.selectedWeaponIndex)

    private val touchpad = Joystick.create(stage)
    private var paused = false

    private var startDelay = 5f
    private var isStartDelayActive = false
    private var firstFrameRan = false

    private var musicMuted = false
    private var isBorderSoundPlaying = false
    private val attackPad = AttackPad.create(
        stage,
        onAttackDirection = {},
        onAttackRelease = {}
    )

    private val blankTexture: Texture by lazy {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        Texture(pixmap, false)
    }
    private val font: BitmapFont = BitmapFont()

    private lateinit var settingsButton: ImageButton
    private var gearTexture: Texture? = null
    private val baseSettingsButtonSize = 128f

    private lateinit var swordButton: ImageButton
    private lateinit var bombButton: ImageButton
    private lateinit var shieldButton: ImageButton
    private lateinit var trapButton: ImageButton
    private var swordTexture: Texture? = null
    private var bombTexture: Texture? = null
    private var shieldTexture: Texture? = null
    private var trapTexture: Texture? = null
    private var bombCooldownLabel: com.badlogic.gdx.scenes.scene2d.ui.Label? = null
    private var trapCooldownLabel: com.badlogic.gdx.scenes.scene2d.ui.Label? = null

    private lateinit var centerActionButton: ImageButton
    private var centerActionTexture: Texture? = null

    private fun uiScale(): Float {
        val baseH = 1080f
        return (stage.viewport.worldHeight / baseH).coerceIn(0.6f, 1.5f)
    }

    private fun positionSettingsButton() {
        if (::settingsButton.isInitialized) {
            val margin = 50f * uiScale()
            settingsButton.setPosition(
                stage.viewport.worldWidth - settingsButton.width - margin,
                stage.viewport.worldHeight - settingsButton.height - margin
            )
        }
    }

    private fun positionSpecialButtons() {
        if (!::settingsButton.isInitialized) return
        val gap = 20f * uiScale()
        val buttons = mutableListOf<ImageButton>()
        if (::swordButton.isInitialized) buttons.add(swordButton)
        if (::bombButton.isInitialized) buttons.add(bombButton)
        if (::shieldButton.isInitialized) buttons.add(shieldButton)
        if (::trapButton.isInitialized) buttons.add(trapButton)
        if (buttons.isEmpty()) return
        var currentY = settingsButton.y - gap
        buttons.forEach { btn ->
            btn.setPosition(settingsButton.x, currentY - btn.height)
            currentY = btn.y - gap
        }
        val minY = buttons.minOf { it.y }
        if (minY < 10f) {
            val delta = 10f - minY
            (buttons + settingsButton).forEach { it.moveBy(0f, delta) }
        }
    }

    private fun layoutUI() {
        val scale = uiScale()
        if (attackPad.hasParent()) {
            attackPad.setBounds(
                viewport.worldWidth - (350f * scale),
                150f * scale,
                200f * scale,
                200f * scale
            )
            world.positionActionButtonsLeftOfAttack(
                attackPad.x,
                attackPad.y,
                attackPad.width,
                attackPad.height,
                scale
            )
            if (::centerActionButton.isInitialized) {
                centerActionButton.setSize(110f * scale, 110f * scale)
                centerActionButton.setPosition(
                    attackPad.x + attackPad.width/2f - centerActionButton.width/2f,
                    attackPad.y + attackPad.height/2f - centerActionButton.height/2f
                )
            }
        }
        positionSettingsButton()
        positionSpecialButtons()
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
        AudioManager.playMusic("sounds/game_music.mp3")

        if (!attackPad.hasParent()) stage.addActor(attackPad)

        val scale = uiScale()

        // SETTINGS FIRST
        gearTexture = Texture(Gdx.files.internal("ui/gear.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val gearDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(gearTexture)).apply {
            setMinSize(baseSettingsButtonSize * scale, baseSettingsButtonSize * scale)
        }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(baseSettingsButtonSize * scale, baseSettingsButtonSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(baseSettingsButtonSize * scale, baseSettingsButtonSize * scale)
            pad(0f)
        }
        stage.addActor(settingsButton)
        settingsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                paused = true
                com.example.mygame1.ui.SettingsDialog(skin) { paused = false }.show(stage)
            }
        })

        // CENTER ACTION BUTTON
        if (!::centerActionButton.isInitialized) {
            centerActionTexture = Texture(Gdx.files.internal("ui/gear.png")) // placeholder
            val centerDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(centerActionTexture))
            centerActionButton = ImageButton(centerDrawable).apply { isVisible = false; pad(0f) }
            stage.addActor(centerActionButton)
            centerActionButton.addListener(object: ClickListener(){
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val p = world.player
                    when (p.specialMode) {
                        com.example.mygame1.entities.Player.SpecialMode.SWORD -> p.swordAttack(world)
                        com.example.mygame1.entities.Player.SpecialMode.BOMB -> p.placeBomb(world)
                        com.example.mygame1.entities.Player.SpecialMode.TRAP -> p.placeTrap(world)
                        else -> {}
                    }
                }
            })
        }

        // SPECIAL BUTTONS (Sword, Bomb, Shield, Trap)
        swordTexture = loadWeaponTextureSafe("character/Weapons/weapon_sword.png")
        val swordDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(swordTexture))
        swordButton = ImageButton(swordDrawable).apply {
            val sz = baseSettingsButtonSize * 0.8f * scale; setSize(sz, sz); image.setScaling(Scaling.stretch); pad(0f)
        }
        stage.addActor(swordButton)
        swordButton.addListener(object: ClickListener(){
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val p = world.player
                p.setSpecialMode(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SWORD) com.example.mygame1.entities.Player.SpecialMode.NONE else com.example.mygame1.entities.Player.SpecialMode.SWORD)
                updateWeaponButtonsColor(); onSpecialModeChanged()
            }
        })

        bombTexture = loadWeaponTextureSafe("character/Weapons/weapon_bomb.png")
        val bombDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(bombTexture))
        bombButton = ImageButton(bombDrawable).apply {
            val sz = baseSettingsButtonSize * 0.8f * scale; setSize(sz, sz); image.setScaling(Scaling.stretch); pad(0f)
        }
        stage.addActor(bombButton)
        bombButton.addListener(object: ClickListener(){
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val p = world.player
                p.setSpecialMode(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.BOMB) com.example.mygame1.entities.Player.SpecialMode.NONE else com.example.mygame1.entities.Player.SpecialMode.BOMB)
                updateWeaponButtonsColor(); onSpecialModeChanged()
            }
        })

        shieldTexture = loadWeaponTextureSafe("character/Weapons/shield_curved.png")
        val shieldDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(shieldTexture))
        shieldButton = ImageButton(shieldDrawable).apply {
            val sz = baseSettingsButtonSize * 0.8f * scale; setSize(sz, sz); image.setScaling(Scaling.stretch); pad(0f)
        }
        stage.addActor(shieldButton)
        shieldButton.addListener(object: ClickListener(){
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val p = world.player
                p.setSpecialMode(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SHIELD) com.example.mygame1.entities.Player.SpecialMode.NONE else com.example.mygame1.entities.Player.SpecialMode.SHIELD)
                updateWeaponButtonsColor(); onSpecialModeChanged()
            }
        })

        trapTexture = loadWeaponTextureSafe("character/Weapons/weapon_trap.png")
        val trapDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(trapTexture))
        trapButton = ImageButton(trapDrawable).apply {
            val sz = baseSettingsButtonSize * 0.8f * scale; setSize(sz, sz); image.setScaling(Scaling.stretch); pad(0f)
        }
        stage.addActor(trapButton)
        trapButton.addListener(object: ClickListener(){
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val p = world.player
                p.setSpecialMode(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.TRAP) com.example.mygame1.entities.Player.SpecialMode.NONE else com.example.mygame1.entities.Player.SpecialMode.TRAP)
                updateWeaponButtonsColor(); onSpecialModeChanged()
            }
        })

        bombCooldownLabel = com.badlogic.gdx.scenes.scene2d.ui.Label("", skin).apply { setFontScale(2f * scale); color = Color.WHITE }
        trapCooldownLabel = com.badlogic.gdx.scenes.scene2d.ui.Label("", skin).apply { setFontScale(2f * scale); color = Color.WHITE }
        stage.addActor(bombCooldownLabel); stage.addActor(trapCooldownLabel)

        // Initial layout
        layoutUI()
        updateWeaponButtonsColor(); onSpecialModeChanged()

        startDelay = 5f; isStartDelayActive = false; firstFrameRan = false; musicMuted = false
    }

    private fun loadWeaponTextureSafe(path: String): Texture {
        return if (Gdx.files.internal(path).exists()) Texture(Gdx.files.internal(path)).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        else Texture(Gdx.files.internal("ui/gear.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
    }

    private fun onSpecialModeChanged() {
        val p = world.player
        val actionTexPath = when (p.specialMode) {
            com.example.mygame1.entities.Player.SpecialMode.SWORD -> "character/Weapons/weapon_sword.png"
            com.example.mygame1.entities.Player.SpecialMode.BOMB -> "character/Weapons/weapon_bomb.png"
            com.example.mygame1.entities.Player.SpecialMode.TRAP -> "control/icon_crosshair.png"
            else -> null
        }
        if (actionTexPath == null) {
            centerActionButton.isVisible = false
            attackPad.touchable = Touchable.enabled
        } else {
            centerActionTexture?.disposeSafely()
            centerActionTexture = loadWeaponTextureSafe(actionTexPath)
            val style = centerActionButton.style as ImageButton.ImageButtonStyle
            style.imageUp = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(centerActionTexture))
            centerActionButton.isVisible = true
            // For sword we disable joystick; for bomb/trap also disable to avoid accidental shooting
            attackPad.touchable = Touchable.disabled
            layoutUI()
        }
        val selectedScale = 1.2f
        swordButton.setTransform(true)
        bombButton.setTransform(true)
        shieldButton.setTransform(true)
        trapButton.setTransform(true)
        swordButton.setScale(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SWORD) selectedScale else 1f)
        bombButton.setScale(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.BOMB) selectedScale else 1f)
        shieldButton.setScale(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SHIELD) selectedScale else 1f)
        trapButton.setScale(if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.TRAP) selectedScale else 1f)
    }

    private fun updateWeaponButtonsColor() {
        if (!::swordButton.isInitialized) return
        val p = world.player
        swordButton.color = if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SWORD) Color.YELLOW else Color.WHITE
        bombButton.color = if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.BOMB) Color.YELLOW else Color.WHITE
        shieldButton.color = if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SHIELD) Color.YELLOW else Color.WHITE
        trapButton.color = if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.TRAP) Color.YELLOW else Color.WHITE
    }

    private var lastSpecialMode: com.example.mygame1.entities.Player.SpecialMode? = null

    override fun render(delta: Float) {
        clearScreen(0.06f, 0.06f, 0.1f)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen<PauseMenuScreen>()
            return
        }

        world.camera.position.set(
            world.player.position.x + world.player.sprite.width / 2f,
            world.player.position.y + world.player.sprite.height / 2f,
            0f
        )
        world.camera.update()

        if (!firstFrameRan) {
            world.update(delta, touchpad)
            firstFrameRan = true
            isStartDelayActive = true
        } else if (isStartDelayActive) {
            world.update(0f, touchpad)
            startDelay -= delta
            if (startDelay <= 0f) {
                isStartDelayActive = false
                startDelay = 0f
            }
        } else if (!paused) {
            world.update(delta, touchpad)
            // Đồng bộ nếu specialMode đổi ở nơi khác (vd swap gun)
            val currentMode = world.player.specialMode
            if (currentMode != lastSpecialMode) {
                onSpecialModeChanged()
                updateWeaponButtonsColor()
            }
            lastSpecialMode = currentMode
            val knobX = attackPad.knobPercentX
            val knobY = attackPad.knobPercentY
            val p = world.player
            val isPadActive = attackPad.isTouched && (kotlin.math.abs(knobX) > 0.1f || kotlin.math.abs(knobY) > 0.1f)
            if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.NONE) {
                if (isPadActive) {
                    val direction = Vector2(knobX, knobY).nor()
                    p.sprite.rotation = direction.angleDeg()
                    p.attack(
                        bulletsOnMap = p.bullets + world.enemies.flatMap { it.bullets },
                        enemyPosition = world.enemies.firstOrNull()?.position ?: Vector2.Zero
                    )
                }
            } else if (p.specialMode == com.example.mygame1.entities.Player.SpecialMode.SWORD) {
                if (isPadActive) {
                    val direction = Vector2(knobX, knobY).nor()
                    p.sprite.rotation = direction.angleDeg()
                    p.swordAttack(world)
                }
            }
            if (world.player.isDead()) {
                com.example.mygame1.data.ScoreManager.addScore(world.score)
                game.setScreen<GameOverScreen>()
                return
            }
        }

        if (!musicMuted && !isStartDelayActive && startDelay <= 0f) {
            AudioManager.stopMusic(); musicMuted = true
        }

        batch.projectionMatrix = world.camera.combined
        batch.begin(); world.render(batch, font, blankTexture); batch.end()

        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        world.player.renderUI(batch, blankTexture, font, viewport.worldWidth, viewport.worldHeight)
        if (isStartDelayActive) {
            val text = "Start in: ${startDelay.toInt()}s"
            font.data.setScale(3f * uiScale())
            font.color = Color.RED
            val cameraCenterWorld = Vector3(world.camera.position.x, world.camera.position.y, 0f)
            val screenPos = world.camera.project(cameraCenterWorld)
            val layout = GlyphLayout(font, text)
            val drawX = screenPos.x - layout.width / 2f
            val drawY = screenPos.y + layout.height / 2f + (100f * uiScale())
            font.draw(batch, text, drawX, drawY)
            font.data.setScale(1f)
            font.color = Color.WHITE
        }
        if (!isStartDelayActive && startDelay <= 0f) {
            val touchingBorder = world.isPlayerTouchingBorder(10f)
            if (touchingBorder && !isBorderSoundPlaying) { AudioManager.playMusic("sounds/game_music.mp3"); isBorderSoundPlaying = true }
            else if (!touchingBorder && isBorderSoundPlaying) { AudioManager.stopMusic(); isBorderSoundPlaying = false }
        }
        batch.end()

        // Cập nhật nhãn cooldown bom trước khi stage.act()/draw để hiển thị đúng frame
        bombCooldownLabel?.let { lbl ->
            val cd = world.player.bombCooldown.coerceAtLeast(0f)
            val scale = uiScale()
            val text = if (cd > 0f) {
                if (cd >= 1f) "${cd.toInt()}s" else String.format("%.1fs", cd)
            } else "Ready"
            lbl.setText(text)
            // Phóng to gấp đôi so với trước (trước 1.6f * scale)
            lbl.setFontScale(3.2f * scale)
            lbl.color = if (cd > 0f) Color(1f,0.55f,0.55f,1f) else Color(0.4f,1f,0.4f,1f)
            if (::bombButton.isInitialized) {
                lbl.invalidateHierarchy()
                val lblW = lbl.prefWidth
                val lblH = lbl.prefHeight
                val padding = 18f * scale
                // Đặt sang bên trái, căn giữa theo chiều dọc nút bom
                lbl.setPosition(
                    bombButton.x - lblW - padding,
                    bombButton.y + bombButton.height/2f - lblH/2f
                )
            }
            lbl.isVisible = true
        }
        // After bomb label update block, add trap label update
        trapCooldownLabel?.let { lbl ->
            val cd = world.player.trapCooldown.coerceAtLeast(0f)
            val scale = uiScale()
            val text = if (cd > 0f) {
                if (cd >= 1f) "${cd.toInt()}s" else String.format(java.util.Locale.US, "%.1fs", cd)
            } else "Ready"
            lbl.setText(text)
            lbl.setFontScale(3.2f * scale)
            lbl.color = if (cd > 0f) Color(1f,0.55f,0.55f,1f) else Color(0.4f,1f,0.4f,1f)
            if (::trapButton.isInitialized) {
                lbl.invalidateHierarchy()
                val lblW = lbl.prefWidth
                val lblH = lbl.prefHeight
                val padding = 18f * scale
                lbl.setPosition(
                    trapButton.x - lblW - padding,
                    trapButton.y + trapButton.height/2f - lblH/2f
                )
            }
            lbl.isVisible = true
        }

        stage.act(delta)
        stage.draw()
    }

    override fun pause() { paused = true; AudioManager.stopMusic() }
    override fun resume() { paused = false; AudioManager.playMusic("sounds/game_music.mp3") }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        layoutUI()
    }

    override fun dispose() {
        batch.disposeSafely(); stage.dispose(); world.dispose(); blankTexture.disposeSafely(); font.disposeSafely(); gearTexture?.dispose(); swordTexture?.dispose(); bombTexture?.dispose(); shieldTexture?.dispose(); trapTexture?.dispose()
    }
}
