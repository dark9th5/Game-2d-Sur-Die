package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.ScoreManager
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import com.badlogic.gdx.graphics.Color

class GameOverScreen(private val game: Main) : KtxScreen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        AudioManager.playMusic("sounds/gameover.mp3", looping = false)

        stage = Stage(ScreenViewport())
        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
        Gdx.input.inputProcessor = stage

        val table = Table().apply { setFillParent(true) }

        val title = Label("Game Over", skin).apply { setFontScale(5f) }
        val scoreLabel = Label("Score: ${ScoreManager.getLastScore()}", skin).apply { setFontScale(4f) }
        val bestLabel = Label("Best: ${ScoreManager.getHighScore()}", skin).apply {
            setFontScale(3.5f); color = Color.GOLD
        }

        val menuButton = TextButton("Menu", skin).apply { label.setFontScale(4f) }
        menuButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                // Gỡ & dispose GameScreen cũ rồi về MainMenu
                val old = game.removeScreen<GameScreen>()
                old?.dispose()
                game.setScreen<MainMenuScreen>()
            }
        })

        table.add(title).padBottom(30f).row()
        table.add(scoreLabel).padBottom(10f).row()
        table.add(bestLabel).padBottom(40f).row()
        table.add(menuButton).width(400f).height(120f).row()

        stage.addActor(table)
    }

    override fun render(delta: Float) {
        clearScreen(0.3f, 0f, 0f)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.disposeSafely()
        skin.disposeSafely()
    }
}
