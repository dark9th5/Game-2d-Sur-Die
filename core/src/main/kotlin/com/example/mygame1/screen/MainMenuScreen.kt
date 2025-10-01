package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.entities.GunType
import com.example.mygame1.entities.characterTextures
import com.example.mygame1.world.StarField
import ktx.app.KtxScreen
import kotlin.random.Random

class MainMenuScreen(private val game: Main) : KtxScreen {

    // Dùng ScreenViewport để tận dụng tối đa màn hình thiết bị, không ép 1920x1080
    private val viewport = ScreenViewport()
    private val stage = Stage(viewport)
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))
    private val starField = StarField(100, sizeScale = 1f)
    private val batch = SpriteBatch()

    private var selectedCharacterIndex: Int = game.selectedCharacterIndex
    private var characterSprite: Sprite? = null
    private var characterImage: Image? = null

    private var selectedWeaponIndex: Int = game.selectedWeaponIndex
    private var weaponSprite: Sprite? = null
    private var weaponImage: Image? = null
    private val weaponTypes = arrayOf(GunType.Gun, GunType.Machine, GunType.Silencer) // chỉ 3 súng cơ bản

    // Settings button + texture
    private lateinit var settingsButton: ImageButton
    private var gearTexture: Texture? = null

    // List button + texture (bảng điểm)
    private lateinit var listButton: ImageButton
    private var listTexture: Texture? = null

    // Kích thước icon settings/list 4x (ví dụ 512)
    private val settingsButtonSize = 128f

    private fun dynamicScale(): Float {
        // Tính scale tương đối dựa trên chiều cao để UI không quá to trên màn hình nhỏ
        val baseH = 1080f
        return (stage.viewport.worldHeight / baseH).coerceIn(0.6f, 1.5f)
    }

    private fun positionSettingsButton() {
        if (::settingsButton.isInitialized) {
            val margin = 50f * dynamicScale()
            settingsButton.setPosition(
                stage.viewport.worldWidth - settingsButton.width - margin,
                stage.viewport.worldHeight - settingsButton.height - margin
            )
        }
        if (::listButton.isInitialized && ::settingsButton.isInitialized) {
            val marginBetween = 24f * dynamicScale()
            listButton.setPosition(
                settingsButton.x,
                settingsButton.y - listButton.height - marginBetween
            )
        }
    }

    private var settingsDialog: com.example.mygame1.ui.SettingsDialog? = null
    private var scoreboardDialog: com.example.mygame1.ui.ScoreboardDialog? = null

    private fun toggleSettingsDialog() {
        val current = settingsDialog
        if (current != null && current.hasParent()) {
            current.hide()
            settingsDialog = null
        } else {
            val dlg = com.example.mygame1.ui.SettingsDialog(skin)
            settingsDialog = dlg
            dlg.show(stage)
        }
    }
    private fun toggleScoreboardDialog() {
        val current = scoreboardDialog
        if (current != null && current.hasParent()) {
            current.hide()
            scoreboardDialog = null
        } else {
            val dlg = com.example.mygame1.ui.ScoreboardDialog(skin)
            scoreboardDialog = dlg
            dlg.show(stage)
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
        AudioManager.playMusic("sounds/menu_music.mp3")

        stage.clear()
        val table = Table().apply { setFillParent(true) }
        stage.addActor(table)

        val scale = dynamicScale()

        val title = Label("Survival - Die", skin, "default").apply {
            setFontScale(5f * scale)
            color = Color.WHITE
        }

        val playButton = TextButton("Play", skin).apply {
            label.setFontScale(5f * scale)
            label.color = Color.WHITE
        }
        val characterButton = TextButton("Character", skin).apply {
            label.setFontScale(5f * scale)
            label.color = Color.WHITE
        }

        val arrowButtonSize = 100f * scale

        val leftButton = TextButton("<", skin).apply {
            label.setFontScale(4f * scale)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        val rightButton = TextButton(">", skin).apply {
            label.setFontScale(4f * scale)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }

        val panelSize = 100f * scale
        characterImage = Image().apply { isVisible = false }

        // --- Weapon UI ---
        val weaponButton = TextButton("Weapon", skin).apply {
            label.setFontScale(5f * scale)
            label.color = Color.WHITE
        }
        val weaponLeftButton = TextButton("<", skin).apply {
            label.setFontScale(4f * scale)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        val weaponRightButton = TextButton(">", skin).apply {
            label.setFontScale(4f * scale)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        weaponImage = Image().apply { isVisible = false }
        // --- End Weapon UI ---

        characterButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                leftButton.isVisible = true
                rightButton.isVisible = true
                characterImage?.isVisible = true
                if (selectedCharacterIndex !in characterTextures.indices) selectedCharacterIndex = 0
                updateCharacterSprite(panelSize, panelSize)
            }
        })
        leftButton.addListener(object : ClickListener() { override fun clicked(e: InputEvent?, x: Float, y: Float) { selectedCharacterIndex = (selectedCharacterIndex - 1 + characterTextures.size) % characterTextures.size; updateCharacterSprite(panelSize, panelSize) } })
        rightButton.addListener(object : ClickListener() { override fun clicked(e: InputEvent?, x: Float, y: Float) { selectedCharacterIndex = (selectedCharacterIndex + 1) % characterTextures.size; updateCharacterSprite(panelSize, panelSize) } })

        weaponButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                weaponLeftButton.isVisible = true
                weaponRightButton.isVisible = true
                weaponImage?.isVisible = true
                if (selectedWeaponIndex !in weaponTypes.indices) selectedWeaponIndex = 0
                updateWeaponSprite(panelSize, panelSize)
            }
        })
        weaponLeftButton.addListener(object : ClickListener() { override fun clicked(e: InputEvent?, x: Float, y: Float) { selectedWeaponIndex = (selectedWeaponIndex - 1 + weaponTypes.size) % weaponTypes.size; updateWeaponSprite(panelSize, panelSize) } })
        weaponRightButton.addListener(object : ClickListener() { override fun clicked(e: InputEvent?, x: Float, y: Float) { selectedWeaponIndex = (selectedWeaponIndex + 1) % weaponTypes.size; updateWeaponSprite(panelSize, panelSize) } })

        playButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                // Bảo đảm có lựa chọn hợp lệ
                if (selectedCharacterIndex !in characterTextures.indices) selectedCharacterIndex = 0
                if (selectedWeaponIndex !in weaponTypes.indices) selectedWeaponIndex = 0
                // Lưu vào game
                game.selectedCharacterIndex = selectedCharacterIndex
                game.selectedWeaponIndex = selectedWeaponIndex

                // Dừng nhạc menu
                AudioManager.stopMusic()

                // Loại bỏ & giải phóng GameScreen cũ (nếu có) trước khi tạo mới
                val old = game.removeScreen<GameScreen>()
                old?.dispose()

                // Đăng ký và chuyển sang GameScreen mới
                game.addScreen(GameScreen(game))
                game.setScreen<GameScreen>()
            }
        })

        // Layout
        table.add(title).padBottom(50f * scale).row()
        table.add(playButton).width(200f * scale).height(panelSize).padBottom(20f * scale).row()
        table.add(characterButton).width(250f * scale).height(panelSize).padBottom(20f * scale).row()
        val characterRow = Table().apply {
            add(leftButton).width(arrowButtonSize).height(arrowButtonSize).padRight(25f * scale)
            add(characterImage).width(panelSize).height(panelSize).padLeft(10f * scale).padRight(10f * scale)
            add(rightButton).width(arrowButtonSize).height(arrowButtonSize).padLeft(25f * scale)
        }
        table.add(characterRow).width(250f * scale).height(panelSize).padBottom(10f * scale).row()
        characterImage?.isVisible = false; leftButton.isVisible = false; rightButton.isVisible = false

        table.add(weaponButton).width(250f * scale).height(panelSize).padBottom(20f * scale).row()
        val weaponRow = Table().apply {
            add(weaponLeftButton).width(arrowButtonSize).height(arrowButtonSize).padRight(25f * scale)
            add(weaponImage).width(panelSize).height(panelSize).padLeft(10f * scale).padRight(10f * scale)
            add(weaponRightButton).width(arrowButtonSize).height(arrowButtonSize).padLeft(25f * scale)
        }
        table.add(weaponRow).width(250f * scale).height(panelSize).padBottom(10f * scale).row()
        weaponImage?.isVisible = false; weaponLeftButton.isVisible = false; weaponRightButton.isVisible = false

        // Settings icon 4x (512x512), vùng chạm = hình, cách viền 50f
        gearTexture = Texture(Gdx.files.internal("ui/gear.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val gearDrawable = TextureRegionDrawable(TextureRegion(gearTexture)).apply { setMinSize(settingsButtonSize * scale, settingsButtonSize * scale) }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(settingsButtonSize * scale, settingsButtonSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(settingsButtonSize * scale, settingsButtonSize * scale)
            pad(0f)
        }
        stage.addActor(settingsButton)
        settingsButton.addListener(object : ClickListener() { override fun clicked(event: InputEvent?, x: Float, y: Float) { toggleSettingsDialog() } })

        // List (scoreboard) icon đặt dưới settings
        listTexture = Texture(Gdx.files.internal("icons/list.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val listDrawable = TextureRegionDrawable(TextureRegion(listTexture))
        listButton = ImageButton(listDrawable).apply {
            setSize(settingsButtonSize * scale, settingsButtonSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(settingsButtonSize * scale, settingsButtonSize * scale)
            pad(0f)
        }
        stage.addActor(listButton)
        listButton.addListener(object : ClickListener() { override fun clicked(e: InputEvent?, x: Float, y: Float) { toggleScoreboardDialog() } })

        positionSettingsButton()
    }

    override fun render(delta: Float) {
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        // Use virtual world size for star field so density differences do not explode layout
        starField.update(delta, stage.viewport.worldWidth, stage.viewport.worldHeight)
        starField.render(batch)
        batch.end()

        stage.act(delta)
        stage.draw()
    }

    private fun updateCharacterSprite(width: Float, height: Float) {
        characterSprite?.texture?.dispose()
        characterSprite = Sprite(Texture(characterTextures[selectedCharacterIndex])).apply { setSize(width, height) }
        characterImage?.drawable = TextureRegionDrawable(characterSprite)
    }

    private fun updateWeaponSprite(width: Float, height: Float) {
        weaponSprite?.texture?.dispose()
        val weaponType = weaponTypes[selectedWeaponIndex]
        weaponSprite = Sprite(Texture(weaponType.assetPath)).apply { setSize(width, height) }
        weaponImage?.drawable = TextureRegionDrawable(weaponSprite)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        // Rebuild UI với scale mới để font/kích thước nút điều chỉnh đúng
        // Giải phóng texture cũ tránh leak
        characterSprite?.texture?.dispose(); characterSprite = null
        weaponSprite?.texture?.dispose(); weaponSprite = null
        gearTexture?.dispose(); gearTexture = null
        listTexture?.dispose(); listTexture = null
        show() // tạo lại layout theo kích thước mới
    }

    override fun dispose() {
        settingsDialog?.remove(); scoreboardDialog?.remove()
        stage.dispose(); skin.dispose(); batch.dispose(); starField.dispose()
        characterSprite?.texture?.dispose(); weaponSprite?.texture?.dispose(); gearTexture?.dispose(); listTexture?.dispose()
    }
}
