package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

enum class BulletOwner {
    PLAYER, ENEMY
}
sealed class BulletType(val assetPath: String) {
    data object Gun : BulletType("character/Characters/green_hand.png")
    data object Machine : BulletType("character/Characters/red_hand.png")
    data object Silencer : BulletType("character/Characters/yellow_hand.png")
    companion object {
        fun values(): Array<BulletType> = arrayOf(Gun, Machine, Silencer)
        fun valueOf(value: String): BulletType = when (value) {
            "Gun" -> Gun
            "Machine" -> Machine
            "Silencer" -> Silencer
            else -> throw IllegalArgumentException("No object com.example.mygame1.entities.BulletType.$value")
        }
    }
}

class Bullet(
    val type: BulletType,
    val position: Vector2,
    val direction: Vector2,
    val owner: BulletOwner,
    val speed: Float = 600f,
    val maxDistance: Float = 600f,
    val size: Float = 8f,
    val damage: Int = 10
) {
    private val texture = Texture(type.assetPath)
    private val sprite = Sprite(texture).apply { setOriginCenter() }
    // Tốc độ theo hướng đã chuẩn hóa
    private val velocity = direction.cpy().nor().scl(speed)

    var isActive = true

    private var traveledDistance = 0f
    private var lastPosition = position.cpy()

    init {
        updateSpritePosition()
    }

    // Đặt vị trí sprite đúng tâm viên đạn, xoay theo hướng bay
    private fun updateSpritePosition() {
        sprite.setSize(size * 2f, size) // Chiều ngang gấp đôi chiều dọc
        sprite.setOriginCenter()
        // Vẽ sprite tại vị trí tâm (position)
        sprite.setPosition(
            position.x - sprite.width / 2f,
            position.y - sprite.height / 2f
        )
        // Nếu texture gốc của viên đạn hướng phải thì dùng angle bình thường
        // Nếu texture gốc hướng lên thì cộng thêm 90 độ
        sprite.rotation = direction.angleDeg() // nếu hướng phải
        // sprite.rotation = direction.angleDeg() + 90f // nếu hướng lên (bỏ comment nếu cần)
    }

    fun bounds(): Rectangle {
        val bulletWidth = size * 0.5f // hoặc 1f nếu muốn sát nhất
        val bulletHeight = size * 0.5f
        return Rectangle(
            position.x - bulletWidth / 2f,
            position.y - bulletHeight / 2f,
            bulletWidth,
            bulletHeight
        )
    }

    fun update(delta: Float) {
        if (!isActive) return
        position.add(velocity.x * delta, velocity.y * delta)
        updateSpritePosition()

        traveledDistance += lastPosition.dst(position)
        lastPosition.set(position)
        if (traveledDistance > maxDistance) {
            isActive = false
        }
    }

    fun render(batch: SpriteBatch) {
        if (isActive) {
            updateSpritePosition()
            sprite.draw(batch)
        }
    }

    fun dispose() {
        texture.dispose()
    }
}
