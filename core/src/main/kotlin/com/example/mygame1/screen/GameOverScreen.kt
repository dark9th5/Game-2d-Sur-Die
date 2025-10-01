package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.ScoreManager
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely

class GameOverScreen(private val game: Main) : KtxScreen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    // Settings & Scoreboard buttons
    private lateinit var settingsButton: ImageButton
    private lateinit var listButton: ImageButton
    private var gearTexture: Texture? = null
    private var listTexture: Texture? = null
    private val iconSize = 128f

    private var settingsDialog: com.example.mygame1.ui.SettingsDialog? = null
    private var scoreboardDialog: com.example.mygame1.ui.ScoreboardDialog? = null

    private fun uiScale(): Float {
        val baseH = 1080f
        return (stage.viewport.worldHeight / baseH).coerceIn(0.6f, 1.5f)
    }

    private fun positionIconButtons() {
        val scale = uiScale()
        if (::settingsButton.isInitialized) {
            val margin = 50f * scale
            settingsButton.setPosition(
                stage.viewport.worldWidth - settingsButton.width - margin,
                stage.viewport.worldHeight - settingsButton.height - margin
            )
        }
        if (::listButton.isInitialized && ::settingsButton.isInitialized) {
            val marginBetween = 24f * scale
            listButton.setPosition(
                settingsButton.x,
                settingsButton.y - listButton.height - marginBetween
            )
        }
    }

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
        AudioManager.playMusic("sounds/gameover.mp3", looping = false)

        stage = Stage(ScreenViewport())
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        Gdx.input.inputProcessor = stage
        stage.clear()

        val table = Table().apply { setFillParent(true) }

        val title = Label("Game Over", skin).apply { setFontScale(5f) }
        val scoreLabel = Label("Score: ${ScoreManager.getLastScore()}", skin).apply { setFontScale(4f) }
        val bestLabel = Label("Best: ${ScoreManager.getHighScore()}", skin).apply {
            setFontScale(3.5f); color = Color.GOLD
        }

        val playButton = TextButton("Play", skin).apply { label.setFontScale(4f) }
        val menuButton = TextButton("Home", skin).apply { label.setFontScale(4f) }

        playButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                AudioManager.stopMusic()
                val old = game.removeScreen<GameScreen>()
                old?.dispose()
                game.addScreen(GameScreen(game))
                game.setScreen<GameScreen>()
            }
        })
        menuButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                val old = game.removeScreen<GameScreen>()
                old?.dispose()
                game.setScreen<MainMenuScreen>()
            }
        })

        // Button row (Play + Home)
        val buttonRow = Table().apply {
            add(playButton).width(350f).height(120f).padRight(40f)
            add(menuButton).width(350f).height(120f)
        }

        table.add(title).padBottom(30f).row()
        table.add(scoreLabel).padBottom(10f).row()
        table.add(bestLabel).padBottom(40f).row()
        table.add(buttonRow).padBottom(20f).row()

        stage.addActor(table)

        // --- Icon buttons (Settings & Scoreboard) ---
        val scale = uiScale()
        gearTexture = Texture(Gdx.files.internal("ui/gear.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val gearDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(gearTexture)).apply {
            setMinSize(iconSize * scale, iconSize * scale)
        }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(iconSize * scale, iconSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(iconSize * scale, iconSize * scale)
            pad(0f)
        }
        stage.addActor(settingsButton)
        settingsButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleSettingsDialog() } })

        listTexture = Texture(Gdx.files.internal("icons/list.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val listDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(listTexture))
        listButton = ImageButton(listDrawable).apply {
            setSize(iconSize * scale, iconSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(iconSize * scale, iconSize * scale)
            pad(0f)
        }
        stage.addActor(listButton)
        listButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleScoreboardDialog() } })

        positionIconButtons()
    }

    override fun render(delta: Float) {
        clearScreen(0.3f, 0f, 0f)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        positionIconButtons()
    }

    override fun dispose() {
        stage.disposeSafely()
        skin.disposeSafely()
        gearTexture.disposeSafely()
        listTexture.disposeSafely()
    }
}
