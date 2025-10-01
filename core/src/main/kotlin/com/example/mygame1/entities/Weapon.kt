package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2

enum class GunType(val displayName: String, val assetPath: String) {
    Gun("Gun", "character/Weapons/weapon_gun.png"),
    Machine("Machine", "character/Weapons/weapon_machine.png"),
    Silencer("Silencer", "character/Weapons/weapon_silencer.png"),
    Sword("Sword", "character/Weapons/weapon_sword.png"),
    Bomb("Bomb", "character/Weapons/weapon_bomb.png"),
    Shield("Shield", "character/Weapons/shield_curved.png"),
    Trap("Trap", "character/Weapons/weapon_trap.png")
}

class Weapon(val type: GunType) {
    private val texture = Texture(type.assetPath)
    val sprite = Sprite(texture)

    fun render(batch: SpriteBatch, gunOrigin: Vector2, playerRotation: Float) {
        sprite.setOrigin(sprite.width / 2f, sprite.height / 2f)
        sprite.setPosition(gunOrigin.x - sprite.width / 2f, gunOrigin.y - sprite.height / 2f)
        sprite.rotation = playerRotation
        sprite.draw(batch)
    }

    fun getName(): String = type.displayName

    fun dispose() {
        texture.dispose()
    }
}
