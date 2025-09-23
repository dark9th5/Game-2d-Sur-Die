package com.example.mygame1.screen
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.example.mygame1.Main
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent

class PauseMenuScreen(private val game: Main) : KtxScreen {
    private val stage = Stage()
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))

    override fun show() {
        Gdx.input.inputProcessor = stage

        val table = Table().apply { setFillParent(true) }

        val resumeButton = TextButton("Resume", skin)
        resumeButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.setScreen<GameScreen>() // quay lại GameScreen
            }
        })

        val quitButton = TextButton("Quit to Menu", skin)
        quitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                // Gỡ & dispose GameScreen cũ rồi về MainMenu
                val old = game.removeScreen<GameScreen>()
                old?.dispose()
                game.setScreen<MainMenuScreen>()
            }
        })

        table.add(Label("Paused", skin)).padBottom(40f).row()
        table.add(resumeButton).pad(10f).row()
        table.add(quitButton).pad(10f).row()

        stage.addActor(table)
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 0.7f)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.disposeSafely()
    }
}
