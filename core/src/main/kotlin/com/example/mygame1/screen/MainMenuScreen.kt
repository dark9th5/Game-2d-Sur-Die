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
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.Scaling
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.entities.GunType
import com.example.mygame1.entities.characterTextures
import com.example.mygame1.world.StarField
import ktx.app.KtxScreen
import kotlin.random.Random

class MainMenuScreen(private val game: Main) : KtxScreen {

    private val stage = Stage(ScreenViewport())
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))
    private val starField = StarField(100, sizeScale = 1f)
    private val batch = SpriteBatch()

    private var selectedCharacterIndex: Int = game.selectedCharacterIndex
    private var characterSprite: Sprite? = null
    private var characterImage: Image? = null

    private var selectedWeaponIndex: Int = game.selectedWeaponIndex
    private var weaponSprite: Sprite? = null
    private var weaponImage: Image? = null
    private val weaponTypes = GunType.values()

    // Settings button + texture
    private lateinit var settingsButton: ImageButton
    private var gearTexture: Texture? = null

    // Kích thước icon settings 4x (ví dụ 512)
    private val settingsButtonSize = 128f

    private fun positionSettingsButton() {
        if (::settingsButton.isInitialized) {
            val margin = 50f
            settingsButton.setPosition(
                stage.viewport.worldWidth - settingsButton.width - margin,
                stage.viewport.worldHeight - settingsButton.height - margin
            )
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
        AudioManager.playMusic("sounds/menu_music.mp3")

        stage.clear()
        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        val title = Label("Survival - Die", skin, "default").apply {
            setFontScale(5f)
            color = Color.WHITE
        }

        val playButton = TextButton("Play", skin).apply {
            label.setFontScale(5f)
            label.color = Color.WHITE
        }

        val characterButton = TextButton("Character", skin).apply {
            label.setFontScale(5f)
            label.color = Color.WHITE
        }

        val arrowButtonSize = 100f

        val leftButton = TextButton("<", skin).apply {
            label.setFontScale(4f)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        val rightButton = TextButton(">", skin).apply {
            label.setFontScale(4f)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }

        val panelSize = 100f
        characterImage = Image().apply { isVisible = false }

        // --- Weapon UI ---
        val weaponButton = TextButton("Weapon", skin).apply {
            label.setFontScale(5f)
            label.color = Color.WHITE
        }
        val weaponLeftButton = TextButton("<", skin).apply {
            label.setFontScale(4f)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        val weaponRightButton = TextButton(">", skin).apply {
            label.setFontScale(4f)
            label.color = Color.WHITE
            isVisible = false
            width = arrowButtonSize
            height = arrowButtonSize
        }
        weaponImage = Image().apply { isVisible = false }
        // --- End Weapon UI ---

        characterButton.addListener { _ ->
            leftButton.isVisible = true
            rightButton.isVisible = true
            characterImage?.isVisible = true

            if (selectedCharacterIndex < 0 || selectedCharacterIndex >= characterTextures.size) {
                selectedCharacterIndex = 0
            }
            updateCharacterSprite(panelSize, panelSize)
            true
        }

        leftButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                selectedCharacterIndex = (selectedCharacterIndex - 1 + characterTextures.size) % characterTextures.size
                updateCharacterSprite(panelSize, panelSize)
            }
        })
        rightButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                selectedCharacterIndex = (selectedCharacterIndex + 1) % characterTextures.size
                updateCharacterSprite(panelSize, panelSize)
            }
        })

        weaponButton.addListener { _ ->
            weaponLeftButton.isVisible = true
            weaponRightButton.isVisible = true
            weaponImage?.isVisible = true

            if (selectedWeaponIndex < 0 || selectedWeaponIndex >= weaponTypes.size) {
                selectedWeaponIndex = 0
            }
            updateWeaponSprite(panelSize, panelSize)
            true
        }
        weaponLeftButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                selectedWeaponIndex = (selectedWeaponIndex - 1 + weaponTypes.size) % weaponTypes.size
                updateWeaponSprite(panelSize, panelSize)
            }
        })
        weaponRightButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                selectedWeaponIndex = (selectedWeaponIndex + 1) % weaponTypes.size
                updateWeaponSprite(panelSize, panelSize)
            }
        })

        playButton.addListener { _ ->
            if (selectedCharacterIndex < 0 || selectedCharacterIndex >= characterTextures.size) {
                selectedCharacterIndex = Random.nextInt(characterTextures.size)
            }
            if (selectedWeaponIndex < 0 || selectedWeaponIndex >= weaponTypes.size) {
                selectedWeaponIndex = Random.nextInt(weaponTypes.size)
            }
            game.selectedCharacterIndex = selectedCharacterIndex
            game.selectedWeaponIndex = selectedWeaponIndex

            game.addScreen(GameScreen(game))
            game.setScreen<GameScreen>()
            true
        }

        // Layout
        table.add(title).padBottom(50f).row()
        table.add(playButton).width(200f).height(panelSize).padBottom(20f).row()
        table.add(characterButton).width(250f).height(panelSize).padBottom(20f).row()
        val characterRow = Table()
        characterRow.add(leftButton).width(arrowButtonSize).height(arrowButtonSize).padRight(25f)
        characterRow.add(characterImage).width(panelSize).height(panelSize).padLeft(10f).padRight(10f)
        characterRow.add(rightButton).width(arrowButtonSize).height(arrowButtonSize).padLeft(25f)
        table.add(characterRow).width(250f).height(panelSize).padBottom(10f).row()
        characterImage?.isVisible = false
        leftButton.isVisible = false
        rightButton.isVisible = false

        table.add(weaponButton).width(250f).height(panelSize).padBottom(20f).row()
        val weaponRow = Table()
        weaponRow.add(weaponLeftButton).width(arrowButtonSize).height(arrowButtonSize).padRight(25f)
        weaponRow.add(weaponImage).width(panelSize).height(panelSize).padLeft(10f).padRight(10f)
        weaponRow.add(weaponRightButton).width(arrowButtonSize).height(arrowButtonSize).padLeft(25f)
        table.add(weaponRow).width(250f).height(panelSize).padBottom(10f).row()
        weaponImage?.isVisible = false
        weaponLeftButton.isVisible = false
        weaponRightButton.isVisible = false

        // Settings icon 4x (512x512), vùng chạm = hình, cách viền 50f
        gearTexture = Texture(Gdx.files.internal("ui/gear.png")).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        val gearDrawable = TextureRegionDrawable(TextureRegion(gearTexture)).apply {
            setMinSize(settingsButtonSize, settingsButtonSize)
        }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(settingsButtonSize, settingsButtonSize)           // Actor = 512x512
            image.setScaling(Scaling.stretch)                         // Ảnh fill đủ cell
            imageCell.size(settingsButtonSize, settingsButtonSize)    // Cell ảnh = 512x512
            pad(0f)                                                   // Không thêm padding
        }
        stage.addActor(settingsButton)
        positionSettingsButton()
        settingsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                com.example.mygame1.ui.SettingsDialog(skin).show(stage)
            }
        })
    }

    override fun render(delta: Float) {
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        starField.update(delta, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        starField.render(batch)
        batch.end()

        stage.act(delta)
        stage.draw()
    }

    private fun updateCharacterSprite(width: Float, height: Float) {
        characterSprite?.texture?.dispose()
        characterSprite = Sprite(Texture(characterTextures[selectedCharacterIndex]))
        characterSprite!!.setSize(width, height)
        characterImage?.drawable = TextureRegionDrawable(characterSprite)
    }

    private fun updateWeaponSprite(width: Float, height: Float) {
        weaponSprite?.texture?.dispose()
        val weaponType = weaponTypes[selectedWeaponIndex]
        weaponSprite = Sprite(Texture(weaponType.assetPath))
        weaponSprite!!.setSize(width, height)
        weaponImage?.drawable = TextureRegionDrawable(weaponSprite)
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        positionSettingsButton()
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        batch.dispose()
        starField.dispose()
        characterSprite?.texture?.dispose()
        weaponSprite?.texture?.dispose()
        gearTexture?.dispose()
    }
}
