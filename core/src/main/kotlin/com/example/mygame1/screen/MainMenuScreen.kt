package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
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
import com.example.mygame1.data.Difficulty

class MainMenuScreen(private val game: Main) : KtxScreen {

    // thành phần UI
        private val viewport = ScreenViewport()

        private val stage = Stage(viewport)
        private val skin: Skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        private val starField = StarField(100, sizeScale = 1f)
        private val batch = SpriteBatch()

    private var selectedCharacterIndex: Int = game.selectedCharacterIndex
    private var characterSprite: Sprite? = null
    private var characterImage: Image? = null

    private var selectedWeaponIndex: Int = game.selectedWeaponIndex
    private var weaponSprite: Sprite? = null
    private var weaponImage: Image? = null
    private val weaponTypes = arrayOf(GunType.Gun, GunType.Machine, GunType.Silencer)

    private lateinit var settingsButton: ImageButton
    private var gearTexture: Texture? = null

    private lateinit var listButton: ImageButton
    private var listTexture: Texture? = null

    private val settingsButtonSize = 128f

    private fun dynamicScale(): Float {
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
            val dlg = com.example.mygame1.ui.SettingsDialog(skin,
                onClose = null,
                inGame = false,
                onBackHome = null,
                onMusicToggle = {
                    if (com.example.mygame1.data.SettingsManager.musicEnabled) {
                        com.example.mygame1.audio.AudioManager.playMusic("sounds/menu_music.mp3")
                    }
                }
            )
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
            val dlg = com.example.mygame1.ui.ScoreboardDialog(skin, game.selectedDifficulty)
            scoreboardDialog = dlg
            dlg.show(stage)
        }
    }

    // Difficulty buttons container & state
    private var difficultyTable: Table? = null
    private var playButton: TextButton? = null
    private var playContainer: Table? = null

    override fun show() {
        // Reset state so Play always rebuilds difficulty buttons after returning from other screens
        difficultyTable = null
        // Removed conditional lazy init; skin is always ready
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

        playButton = TextButton("Play", skin).apply {
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

        fun startGameWithDifficulty(diff: Difficulty) {
            if (selectedCharacterIndex !in characterTextures.indices) selectedCharacterIndex = 0
            if (selectedWeaponIndex !in weaponTypes.indices) selectedWeaponIndex = 0
            game.selectedCharacterIndex = selectedCharacterIndex
            game.selectedWeaponIndex = selectedWeaponIndex
            game.selectedDifficulty = diff
            AudioManager.stopMusic()
            val old = game.removeScreen<GameScreen>()
            old?.dispose()
            game.addScreen(GameScreen(game))
            game.setScreen<GameScreen>()
        }

        playButton?.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (difficultyTable == null) {
                    // Build difficulty buttons using explicit style object to avoid style-name lookup NPE
                    val baseStyle = runCatching { skin.get(TextButtonStyle::class.java) }.getOrNull()
                        ?: TextButtonStyle().apply { font = skin.getFont("default"); fontColor = Color.WHITE }
                    fun makeBtn(text: String, diff: Difficulty): TextButton {
                        val btn = TextButton(text, baseStyle)
                        btn.label.setFontScale(3.2f * scale, 3.2f * scale)
                        btn.addListener(object: ClickListener(){
                            override fun clicked(e: InputEvent?, x2: Float, y2: Float) { startGameWithDifficulty(diff) }
                        })
                        return btn
                    }
                    difficultyTable = Table().apply {
                        defaults().pad(6f * scale)
                        add(makeBtn("Easy", Difficulty.EASY)).width(200f * scale).height(110f * scale)
                        add(makeBtn("Normal", Difficulty.NORMAL)).width(200f * scale).height(110f * scale)
                        // Hard button: starts game directly with Hard difficulty
                        val hardBtn = makeBtn("Hard", Difficulty.HARD)
                        hardBtn.clearListeners()
                        hardBtn.addListener(object: ClickListener(){
                            override fun clicked(e: InputEvent?, x2: Float, y2: Float) { startGameWithDifficulty(Difficulty.HARD) }
                        })
                        add(hardBtn).width(200f * scale).height(110f * scale)
                    }
                    playContainer?.clearChildren()
                    playContainer?.add(difficultyTable)
                    playContainer?.invalidateHierarchy(); playContainer?.layout()
                    table.invalidateHierarchy(); table.layout()
                }
            }
        })

        playContainer = Table().apply { add(playButton).width(200f * scale).height(panelSize) }
        table.add(title).padBottom(50f * scale).row()
        table.add(playContainer).padBottom(20f * scale).row()

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
        characterSprite?.texture?.dispose(); characterSprite = null
        weaponSprite?.texture?.dispose(); weaponSprite = null
        gearTexture?.dispose(); gearTexture = null
        listTexture?.dispose(); listTexture = null
        show()
    }

    override fun dispose() {
        settingsDialog?.remove(); scoreboardDialog?.remove()
        stage.dispose(); skin.dispose(); batch.dispose(); starField.dispose()
        characterSprite?.texture?.dispose(); weaponSprite?.texture?.dispose(); gearTexture?.dispose(); listTexture?.dispose()
    }
}
