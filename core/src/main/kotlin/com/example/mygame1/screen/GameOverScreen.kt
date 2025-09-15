package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.example.mygame1.Main
import com.example.mygame1.audio.AudioManager
import ktx.app.KtxScreen
import ktx.app.clearScreen

class GameOverScreen(private val game: Main) : KtxScreen {
    override fun show() {
        AudioManager.playMusic("sounds/gameover.mp3", looping = false)
    }

    override fun render(delta: Float) {
        clearScreen(0.3f, 0f, 0f)
        if (Gdx.input.isTouched) {
            game.setScreen<MainMenuScreen>()
        }
    }
}

