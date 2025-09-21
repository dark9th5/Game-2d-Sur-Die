package com.example.mygame1

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.screen.GameOverScreen
import com.example.mygame1.screen.MainMenuScreen
import com.example.mygame1.screen.PauseMenuScreen
import com.example.mygame1.screen.SplashScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import com.example.mygame1.screen.GameScreen

class Main : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    var selectedCharacterIndex: Int = -1 // Chỉ số nhân vật
    var selectedWeaponIndex: Int = -1    // Chỉ số vũ khí

    override fun create() {
        addScreen(SplashScreen(this))
        addScreen(MainMenuScreen(this))
        addScreen(GameOverScreen(this))
        setScreen<SplashScreen>()
        addScreen(PauseMenuScreen(this))
    }

    override fun dispose() {
        super.dispose()
        AudioManager.dispose()
    }
}
