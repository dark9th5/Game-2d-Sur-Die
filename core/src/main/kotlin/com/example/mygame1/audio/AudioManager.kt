package com.example.mygame1.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import ktx.assets.disposeSafely

object AudioManager {
    private var music: Music? = null
    private val sounds = mutableMapOf<String, Sound>()

    fun playMusic(file: String, looping: Boolean = true, volume: Float = 0.5f) {
        music?.disposeSafely()
        music = Gdx.audio.newMusic(Gdx.files.internal(file)).apply {
            isLooping = looping
            this.volume = volume
            play()
        }
    }

    fun stopMusic() {
        music?.stop()
    }

    fun playSound(file: String, volume: Float = 1f) {
        val sound = sounds.getOrPut(file) {
            Gdx.audio.newSound(Gdx.files.internal(file))
        }
        sound.play(volume)
    }

    fun dispose() {
        music?.disposeSafely()
        sounds.values.forEach { it.disposeSafely() }
        sounds.clear()
    }

}
