package com.example.mygame1.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle


object Joystick {
    fun create(stage: Stage): Touchpad {
        val skin = Skin().apply {
            add("touchBackground", Texture("control/joystick_circle_pad_c.png"))
            add("touchKnob", Texture("control/joystick_circle_nub_b.png"))
        }

        val style = TouchpadStyle().apply {
            background = skin.getDrawable("touchBackground")
            knob = skin.getDrawable("touchKnob")
        }

        return Touchpad(10f, style).apply {
            setBounds(100f, 100f, 300f, 300f) // vị trí + kích thước joystick
            stage.addActor(this)
        }
    }
}
