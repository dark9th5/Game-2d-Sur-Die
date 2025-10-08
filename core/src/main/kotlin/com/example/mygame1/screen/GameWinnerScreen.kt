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
import com.badlogic.gdx.utils.Align

class GameWinnerScreen(private val game: Main) : KtxScreen {
    private var stage: Stage? = Stage(ScreenViewport())
    private lateinit var skin: Skin

    // Settings & Scoreboard buttons
    private lateinit var settingsButton: ImageButton
    private lateinit var listButton: ImageButton
    private var gearTexture: Texture? = null
    private var listTexture: Texture? = null

    private var settingsDialog: com.example.mygame1.ui.SettingsDialog? = null
    private var scoreboardDialog: com.example.mygame1.ui.ScoreboardDialog? = null

    private var winTexture: Texture? = null

    // Thay thế cơ chế width/maxHeight bằng chiều cao cố định + stretch ngang
    private val TITLE_HEIGHT_FRAC = 0.5f
    private val TITLE_STRETCH_X = 2f
    private val TOP_PADDING = 20f
    private val GAP_BELOW_TITLE = 0f
    private val GAP_BELOW_MODE = 10f
    private val GAP_BELOW_SCORE = 8f
    private val GAP_BELOW_TIME = 24f

    private fun uiScale(): Float {
        val baseH = 1080f
        val s = stage ?: return 1f
        return (s.viewport.worldHeight / baseH).coerceIn(0.6f, 1.5f)
    }

    private fun positionIconButtons() {
        val s = stage ?: return
        val scale = uiScale()
        if (::settingsButton.isInitialized) {
            val margin = 50f * scale
            settingsButton.setPosition(
                s.viewport.worldWidth - settingsButton.width - margin,
                s.viewport.worldHeight - settingsButton.height - margin
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
        val s = stage ?: return
        val current = settingsDialog
        if (current != null && current.hasParent()) {
            current.hide(); settingsDialog = null
        } else {
            val dlg = com.example.mygame1.ui.SettingsDialog(skin)
            settingsDialog = dlg; dlg.show(s)
        }
    }

    private fun toggleScoreboardDialog() {
        val s = stage ?: return
        val current = scoreboardDialog
        if (current != null && current.hasParent()) {
            current.hide(); scoreboardDialog = null
        } else {
            val dlg = com.example.mygame1.ui.ScoreboardDialog(skin, game.selectedDifficulty)
            scoreboardDialog = dlg; dlg.show(s)
        }
    }

    override fun show() {
        AudioManager.stopMusic()
        // AudioManager.playMusic("sounds/win.mp3", looping = false)

        val s = stage ?: run { Stage(ScreenViewport()).also { stage = it } }
        settingsDialog = null
        scoreboardDialog = null

        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        Gdx.input.inputProcessor = s
        s.clear()

        val table = Table().apply { setFillParent(true); align(Align.top); padTop(TOP_PADDING) }

        // Title image (YOU WIN) unified size with GameOverScreen
        winTexture = runCatching { Texture(Gdx.files.internal("ui/you_win.png")) }.getOrNull()
        if (winTexture != null) {
            val viewport = (stage ?: return).viewport
            val viewportW = viewport.worldWidth
            val viewportH = viewport.worldHeight
            val originalW = winTexture!!.width.toFloat()
            val originalH = winTexture!!.height.toFloat()
            val aspect = originalH / originalW
            val targetH = viewportH * TITLE_HEIGHT_FRAC
            var baseW = targetH / aspect
            var stretchedW = baseW * TITLE_STRETCH_X
            if (stretchedW > viewportW) stretchedW = viewportW
            val img = Image(winTexture).apply { setScaling(com.badlogic.gdx.utils.Scaling.stretch) }
            table.add(img).size(stretchedW, targetH).padBottom(GAP_BELOW_TITLE).row()
        } else {
            val title = Label("YOU WIN", skin).apply { setFontScale(4f) }
            table.add(title).padBottom(GAP_BELOW_TITLE).row()
        }

        // Difficulty label under title and above score
        val difficultyLabel = Label("Mode: ${game.selectedDifficulty.displayName}", skin).apply {
            setFontScale(3.2f); color = Color.CYAN
        }
        table.add(difficultyLabel).padBottom(GAP_BELOW_MODE).row()

        // Show last score & time
        val score = ScoreManager.getLastScore(game.selectedDifficulty)
        val timeMillis = ScoreManager.getLastScoreTime(game.selectedDifficulty)
        val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        val timeStr = if (timeMillis > 0L) sdf.format(Date(timeMillis)) else "--"
        val scoreLabel = Label("Score: $score", skin).apply { setFontScale(2.8f) }
        val timeLabel = Label("Time: $timeStr", skin).apply { setFontScale(2.2f) }
        table.add(scoreLabel).padBottom(GAP_BELOW_SCORE).row()
        table.add(timeLabel).padBottom(GAP_BELOW_TIME).row()

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

        s.addActor(table)

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
        s.addActor(settingsButton)
        settingsButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleSettingsDialog() } })

        listTexture = Texture(Gdx.files.internal("icons/list.png")).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        val listDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(listTexture))
        listButton = ImageButton(listDrawable).apply {
            setSize(128f * scale, 128f * scale)
            image.setScaling(com.badlogic.gdx.utils.Scaling.stretch)
            imageCell.size(128f * scale, 128f * scale)
            pad(0f)
        }
        s.addActor(listButton)
        listButton.addListener(object : ClickListener() { override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleScoreboardDialog() } })

        positionIconButtons()
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage?.act(delta)
        stage?.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage?.viewport?.update(width, height, true)
        positionIconButtons()
    }

    override fun dispose() {
        stage?.disposeSafely(); stage = null
        if (this::skin.isInitialized) skin.disposeSafely()
        gearTexture?.disposeSafely(); listTexture?.disposeSafely(); winTexture?.disposeSafely()
    }
}
