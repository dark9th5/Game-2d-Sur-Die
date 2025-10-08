package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.example.mygame1.Assets

enum class GunType(val displayName: String, val assetPath: String) {
    Gun("Gun", "character/Weapons/weapon_gun.png"),
    Machine("Machine", "character/Weapons/weapon_machine.png"),
    Silencer("Silencer", "character/Weapons/weapon_silencer.png"),
    Bomb("Bomb", "character/Weapons/weapon_bomb.png"),
    Shield("Shield", "character/Weapons/shield_curved.png"),
    Trap("Trap", "character/Weapons/weapon_trap.png")
}

class Weapon(val type: GunType) {
    // Dùng shared texture từ Assets thay vì tạo mới
    private val texture: Texture = Assets.texture(type.assetPath)
    val sprite = Sprite(texture)

    // ================= Ammo state (riêng từng súng) =================
    val maxBullets: Int = when(type){
        GunType.Gun -> 20
        GunType.Machine -> 40
        GunType.Silencer -> 15
        GunType.Bomb -> 1
        GunType.Shield -> 0
        GunType.Trap -> 1
    }
    var ammoInMagazine: Int = maxBullets
    var isReloading: Boolean = false
    var reloadTimer: Float = 0f
    var reloadTarget: Int = maxBullets

    fun render(batch: SpriteBatch, gunOrigin: Vector2, playerRotation: Float) {
        sprite.setOrigin(sprite.width / 2f, sprite.height / 2f)
        sprite.setPosition(gunOrigin.x - sprite.width / 2f, gunOrigin.y - sprite.height / 2f)
        sprite.rotation = playerRotation
        sprite.draw(batch)
    }

    fun getName(): String = type.displayName

    fun dispose() { /* shared texture managed by Assets */ }
}
