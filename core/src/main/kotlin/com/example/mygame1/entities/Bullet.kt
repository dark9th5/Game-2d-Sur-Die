package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

// Removed BulletOwner (owner distinction handled by separate lists player.bullets / enemy.bullets)
sealed class BulletType(val assetPath: String) {
    data object Gun : BulletType("character/Characters/green_hand.png")
    data object Machine : BulletType("character/Characters/red_hand.png")
    data object Silencer : BulletType("character/Characters/yellow_hand.png")
}

object BulletAssets {
    private val textures = mutableMapOf<BulletType, Texture>()
    fun getTexture(type: BulletType): Texture = textures.getOrPut(type) { Texture(type.assetPath) }
    fun dispose() { textures.values.forEach { it.dispose() }; textures.clear() }
}

class Bullet(
    val type: BulletType,
    val position: Vector2,
    val direction: Vector2,
    val maxDistance: Float = 600f,
    val size: Float = 8f,
    val damage: Int = 10,
    bulletSpeed: Float = 600f
) {
    private val sprite = Sprite(BulletAssets.getTexture(type)).apply { setOriginCenter() }
    private val velocity = direction.cpy().nor().scl(bulletSpeed)
    var isActive = true
    private var traveledDistance = 0f
    private var lastPosition = position.cpy()

    init { updateSpritePosition() }

    private fun updateSpritePosition() {
        sprite.setSize(size * 2f, size)
        sprite.setOriginCenter()
        sprite.setPosition(position.x - sprite.width / 2f, position.y - sprite.height / 2f)
        sprite.rotation = direction.angleDeg()
    }

    fun bounds(): Rectangle {
        val w = size * 0.5f
        val h = size * 0.5f
        return Rectangle(position.x - w / 2f, position.y - h / 2f, w, h)
    }

    fun update(delta: Float) {
        if (!isActive) return
        position.add(velocity.x * delta, velocity.y * delta)
        updateSpritePosition()
        traveledDistance += lastPosition.dst(position)
        lastPosition.set(position)
        if (traveledDistance > maxDistance) isActive = false
    }

    fun render(batch: SpriteBatch) { if (isActive) { updateSpritePosition(); sprite.draw(batch) } }
    fun dispose() { /* Shared textures disposed in BulletAssets */ }
}
