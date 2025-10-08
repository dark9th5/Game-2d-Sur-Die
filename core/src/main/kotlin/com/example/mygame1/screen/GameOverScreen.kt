package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.ScoreManager
import java.text.SimpleDateFormat
import java.util.*
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import com.example.mygame1.Assets

class GameOverScreen(private val game: Main) : KtxScreen {
    // Changed to nullable & recreated in show() to avoid NPE after dispose.
    private var stage: Stage? = Stage(ScreenViewport())
    private lateinit var skin: Skin

    private lateinit var settingsButton: ImageButton
    private lateinit var listButton: ImageButton
    private var gearTexture: Texture? = null
    private var listTexture: Texture? = null
    private var gameOverTexture: Texture? = null
    private val iconSize = 128f

    private var settingsDialog: com.example.mygame1.ui.SettingsDialog? = null
    private var scoreboardDialog: com.example.mygame1.ui.ScoreboardDialog? = null

    // Thay thế cơ chế cũ (WIDTH_FRAC & MAX_HEIGHT_FRAC) bằng chiều cao cố định và hệ số kéo ngang
    private val TITLE_HEIGHT_FRAC = 0.5f //
    private val TITLE_STRETCH_X = 2f      // Kéo ngang gấp đôi
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
        AudioManager.playMusic("sounds/gameover.mp3", looping = false)

        // Recreate stage if disposed previously.
        val s = stage ?: run { Stage(ScreenViewport()).also { stage = it } }
        settingsDialog = null
        scoreboardDialog = null

        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        Gdx.input.inputProcessor = s
        s.clear()

        val table = Table().apply { setFillParent(true); align(Align.top); padTop(TOP_PADDING) }

        gameOverTexture = runCatching { Assets.texture("ui/game_over.png") }.getOrNull()
        if (gameOverTexture != null) {
            val viewport = (stage ?: return).viewport
            val viewportW = viewport.worldWidth
            val viewportH = viewport.worldHeight
            val originalW = gameOverTexture!!.width.toFloat()
            val originalH = gameOverTexture!!.height.toFloat()
            val aspect = originalH / originalW // H/W
            var targetH = viewportH * TITLE_HEIGHT_FRAC
            var baseW = targetH / aspect // giữ tỉ lệ bình thường
            var stretchedW = baseW * TITLE_STRETCH_X // kéo ngang gấp đôi
            if (stretchedW > viewportW) {
                // Nếu vượt màn hình, giữ nguyên chiều cao (yêu cầu) và giới hạn chiều rộng vừa khít
                stretchedW = viewportW
            }
            val img = Image(gameOverTexture).apply { setScaling(Scaling.stretch) }
            table.add(img).size(stretchedW, targetH).padBottom(GAP_BELOW_TITLE).row()
        } else {
            val title = Label("Game Over", skin).apply { setFontScale(4f) }
            table.add(title).padBottom(GAP_BELOW_TITLE).row()
        }

        val diffLabel = Label("Mode: ${game.selectedDifficulty.displayName}", skin).apply {
            setFontScale(3.2f); color = Color.CYAN
        }
        table.add(diffLabel).padBottom(GAP_BELOW_MODE).row()

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

        val buttonRow = Table().apply {
            add(playButton).width(350f).height(120f).padRight(40f)
            add(menuButton).width(350f).height(120f)
        }
        table.add(buttonRow).padBottom(20f).row()

        s.addActor(table)

        val scale = uiScale()
        gearTexture = Assets.texture("ui/gear.png")
        val gearDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(gearTexture)).apply { setMinSize(iconSize * scale, iconSize * scale) }
        settingsButton = ImageButton(gearDrawable).apply {
            setSize(iconSize * scale, iconSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(iconSize * scale, iconSize * scale)
            pad(0f)
        }
        s.addActor(settingsButton)
        settingsButton.addListener(object: ClickListener(){ override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleSettingsDialog() } })

        listTexture = Assets.texture("icons/list.png")
        val listDrawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(listTexture))
        listButton = ImageButton(listDrawable).apply {
            setSize(iconSize * scale, iconSize * scale)
            image.setScaling(Scaling.stretch)
            imageCell.size(iconSize * scale, iconSize * scale)
            pad(0f)
        }
        s.addActor(listButton)
        listButton.addListener(object: ClickListener(){ override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { toggleScoreboardDialog() } })

        positionIconButtons()
    }

    override fun render(delta: Float) {
        clearScreen(0.3f, 0f, 0f)
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
        // Shared textures managed by Assets
        gameOverTexture = null; gearTexture = null; listTexture = null
    }
}
