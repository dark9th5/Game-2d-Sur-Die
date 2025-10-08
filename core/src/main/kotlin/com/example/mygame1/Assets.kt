package com.example.mygame1

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Pixmap

object Assets {
    private val textures = mutableMapOf<String, Texture>()
    private var whiteTex: Texture? = null

    private val preloadPaths = listOf(
        "background/splash_bg.png",
        "snowflake/snowflake.png",
        // Weapons
        "character/Weapons/weapon_gun.png",
        "character/Weapons/weapon_machine.png",
        "character/Weapons/weapon_silencer.png",
        "character/Weapons/weapon_bomb.png",
        "character/Weapons/shield_curved.png",
        "character/Weapons/weapon_trap.png",
        // UI / icons reused
        "ui/gear.png",
        "icons/list.png",
        "control/buttonLong_blue.png",
        "control/buttonLong_blue_pressed.png",
        "control/joystick_circle_pad_c.png",
        "control/joystick_circle_pad_d.png",
        "control/joystick_circle_nub_b.png",
        "control/icon_crosshair.png"
    )

    fun init() { preloadPaths.forEach { texture(it) } }

    fun texture(path: String): Texture = textures.getOrPut(path) { Texture(path) }

    fun white(): Texture {
        val existing = whiteTex
        if (existing != null) return existing
        val pm = Pixmap(1,1, Pixmap.Format.RGBA8888)
        pm.setColor(1f,1f,1f,1f); pm.fill()
        val tex = Texture(pm, false)
        pm.dispose()
        whiteTex = tex
        return tex
    }

    fun dispose() {
        textures.values.forEach { runCatching { it.dispose() } }
        textures.clear()
        whiteTex?.dispose(); whiteTex = null
    }
}

