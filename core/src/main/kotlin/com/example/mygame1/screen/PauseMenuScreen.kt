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

class PauseMenuScreen(private val game: Main) : KtxScreen {
    private val stage = Stage()
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json"))

    override fun show() {
        Gdx.input.inputProcessor = stage

        val table = Table().apply { setFillParent(true) }

        val resumeButton = TextButton("Resume", skin).apply {
            addListener { _ ->
                game.setScreen<GameScreen>() // quay lại GameScreen
                true
            }
        }

        val quitButton = TextButton("Quit to Menu", skin).apply {
            addListener { _ ->
                game.setScreen<MainMenuScreen>()
                true
            }
        }

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
