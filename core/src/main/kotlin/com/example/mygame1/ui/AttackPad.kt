package com.example.mygame1.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.style.get

object AttackPad {
    fun create(
        stage: Stage,
        onAttackDirection: (Vector2) -> Unit,
        onAttackRelease: () -> Unit
    ): Touchpad {
        val skin = Skin().apply {
            add("attack_bg", Texture("control/joystick_circle_pad_d.png"))
            add("attack_knob", Texture("control/icon_crosshair.png"))
        }
        val knobTexture = skin.get<Texture>("attack_knob")
        val knobDrawable = TextureRegionDrawable(knobTexture)
        knobDrawable.setMinWidth(knobTexture.width * 2f)   // Gấp đôi chiều rộng
        knobDrawable.setMinHeight(knobTexture.height * 2f)  // Gấp đôi chiều cao

        val style = TouchpadStyle().apply {
            background = skin.getDrawable("attack_bg")
            knob = knobDrawable
        }
        val attackPad = Touchpad(10f, style).apply {
            setBounds(700f, 300f, 200f, 200f)
        }
        attackPad.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            override fun touchDragged(
                event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                x: Float, y: Float, pointer: Int
            ) {
                val knobX = attackPad.knobPercentX
                val knobY = attackPad.knobPercentY
                if (attackPad.isTouched && (Math.abs(knobX) > 0.1f || Math.abs(knobY) > 0.1f)) {
                    val dir = Vector2(knobX, knobY).nor()
                    onAttackDirection(dir)
                }
            }
            override fun touchUp(
                event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                x: Float, y: Float, pointer: Int, button: Int
            ) {
                onAttackRelease()
            }
        })
        stage.addActor(attackPad)
        return attackPad
    }
}
