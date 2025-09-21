package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Rectangle
import ktx.assets.toInternalFile

enum class ItemType {
    HEAL, SPEED, VISION
}

class Item(
    val type: ItemType,
    val position: Vector2,
    val duration: Float = 60f // thời gian tồn tại
) {
    companion object {
        val textureHeal = Texture("items/item_heal.png".toInternalFile())
        val textureSpeed = Texture("items/item_speed.png".toInternalFile())
        val textureVision = Texture("items/item_vision.png".toInternalFile())
    }

    val texture: Texture = when(type) {
        ItemType.HEAL -> textureHeal
        ItemType.SPEED -> textureSpeed
        ItemType.VISION -> textureVision
    }
    val sprite = Sprite(texture).apply {
        setSize(32f, 32f)
        setPosition(position.x, position.y)
        setOriginCenter()
    }
    var aliveTime = 0f

    fun update(delta: Float) {
        aliveTime += delta
    }

    fun render(batch: SpriteBatch) {
        sprite.draw(batch)
    }

    fun bounds(): Rectangle = sprite.boundingRectangle

    fun isExpired(): Boolean = aliveTime > duration

    fun dispose() {
        // Texture được dùng chung, chỉ dispose khi chắc chắn không còn item nào
        // Không dispose ở đây để tránh dispose nhiều lần gây lỗi
    }
}
