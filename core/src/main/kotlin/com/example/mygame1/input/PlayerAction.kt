package com.example.mygame1.input

import com.badlogic.gdx.math.Vector2

sealed class PlayerAction {
    data class Move(val direction: Vector2) : PlayerAction()
    object Shoot : PlayerAction()
    data class ChangeWeapon(val weaponIndex: Int) : PlayerAction()
    object Idle : PlayerAction()
}
