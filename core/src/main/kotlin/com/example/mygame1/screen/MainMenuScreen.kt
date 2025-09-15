package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.world.StarField
import ktx.app.KtxScreen

class MainMenuScreen(private val game: Main) : KtxScreen {

    private val stage = Stage(ScreenViewport())
    private val skin = Skin(Gdx.files.internal("ui/uiskin.json")) // cần file uiskin.json (có sẵn trong libGDX setup)
    private val starField = StarField(100, sizeScale = 1f) // GIẢM KÍCH THƯỚC SAO 3 LẦN
    private val batch = SpriteBatch()

    override fun show() {
        Gdx.input.inputProcessor = stage
        AudioManager.playMusic("sounds/menu_music.mp3")

        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        // Tiêu đề game
        val title = Label("Survival - Die", skin, "default").apply {
            setFontScale(3f)
            color = Color.WHITE
        }

        // Nút Play
        val playButton = TextButton("Play", skin).apply {
            label.setFontScale(3f)
            label.color = Color.WHITE // chữ trắng
        }

        // Sự kiện bấm nút
        playButton.addListener { _ ->
            game.setScreen<GameScreen>()
            true
        }

        table.add(title).padBottom(50f).row()
        table.add(playButton).width(200f).height(80f)
    }

    override fun render(delta: Float) {
        // Vẽ nền sao
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        starField.update(delta, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        starField.render(batch)
        batch.end()

        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        batch.dispose()
        starField.dispose()
    }
}
