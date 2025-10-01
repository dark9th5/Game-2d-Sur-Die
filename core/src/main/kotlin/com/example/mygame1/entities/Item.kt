package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.Gdx

enum class ItemType { HEAL, SPEED, VISION, ARMOR }

class Item(
    val type: ItemType,
    val position: Vector2,
    val duration: Float = 6000f
) {
    companion object {
        val textureHeal = Texture(Gdx.files.internal("items/item_heal.png"))
        val textureSpeed = Texture(Gdx.files.internal("items/item_speed.png"))
        val textureVision = Texture(Gdx.files.internal("items/item_vision.png"))
        val textureArmor = Texture(Gdx.files.internal("items/armor.png"))
    }
    val texture: Texture = when(type) {
        ItemType.HEAL -> textureHeal
        ItemType.SPEED -> textureSpeed
        ItemType.VISION -> textureVision
        ItemType.ARMOR -> textureArmor
    }
    val sprite = Sprite(texture).apply { setSize(32f,32f); setPosition(position.x, position.y); setOriginCenter() }
    var aliveTime = 0f
    fun update(delta: Float) { aliveTime += delta }
    fun render(batch: SpriteBatch) { sprite.draw(batch) }
    fun bounds(): Rectangle = sprite.boundingRectangle
    fun isExpired(): Boolean = aliveTime > duration
    fun dispose() { }
}
