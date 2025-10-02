package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.ScoreManager
import java.text.SimpleDateFormat
import java.util.*
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely

class GameWinnerScreen(private val game: Main) : KtxScreen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    // Settings & Scoreboard buttons
    private lateinit var settingsButton: ImageButton
    private lateinit var listButton: ImageButton
    private var gearTexture: Texture? = null
    private var listTexture: Texture? = null

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
        AudioManager.stopMusic()
        // AudioManager.playMusic("sounds/win.mp3", looping = false) // Nếu có nhạc win

        stage = Stage(ScreenViewport())
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        Gdx.input.inputProcessor = stage
        stage.clear()

        val table = Table().apply { setFillParent(true) }

        val title = Label("You Win!", skin).apply { setFontScale(5f) }
        table.add(title).padBottom(30f).row()

        // Hiển thị điểm và thời gian ván vừa xong
        val score = ScoreManager.getLastScore()
        val timeMillis = ScoreManager.getLastScoreTime()
        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())
        val timeStr = if (timeMillis > 0L) sdf.format(java.util.Date(timeMillis)) else "--"
        val scoreLabel = Label("Score: $score", skin).apply { setFontScale(3.5f) }
        val timeLabel = Label("Time: $timeStr", skin).apply { setFontScale(2.5f) }
        table.add(scoreLabel).padBottom(10f).row()
        table.add(timeLabel).padBottom(40f).row()

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
        table.add(buttonRow).padBottom(20f).row()

        stage.addActor(table)

        // --- Icon buttons (Settings & Scoreboard) ---
        val scale = uiScale()
        gearTexture = Texture(Gdx.files.internal("ui/gear.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val gearDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(gearTexture)).apply {
            setMinSize(128f * scale, 128f * scale)
        }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(128f * scale, 128f * scale)
            image.setScaling(com.badlogic.gdx.utils.Scaling.stretch)
            imageCell.size(128f * scale, 128f * scale)
            pad(0f)
        }
        stage.addActor(settingsButton)
        settingsButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleSettingsDialog() } })

        listTexture = Texture(Gdx.files.internal("icons/list.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val listDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(listTexture))
        listButton = ImageButton(listDrawable).apply {
            setSize(128f * scale, 128f * scale)
            image.setScaling(com.badlogic.gdx.utils.Scaling.stretch)
            imageCell.size(128f * scale, 128f * scale)
            pad(0f)
        }
        stage.addActor(listButton)
        listButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleScoreboardDialog() } })

        positionIconButtons()
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        positionIconButtons()
    }

    override fun dispose() {
        stage.disposeSafely()
        gearTexture?.disposeSafely()
        listTexture?.disposeSafely()
    }
}
