package com.example.mygame1.input

import com.badlogic.gdx.scenes.scene2d.ui.Touchpad


class InputHandler(private val touchpad: Touchpad) {
    val dx: Float
        get() = touchpad.knobPercentX

    val dy: Float
        get() = touchpad.knobPercentY

    fun isMoving(): Boolean = dx != 0f || dy != 0f
}
