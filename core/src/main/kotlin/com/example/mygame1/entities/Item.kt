package com.example.mygame1.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile

enum class ItemType {
    HEALTH,    // Hồi máu
    SPEED,     // Tăng tốc
    FOV        // Mở rộng góc nhìn
}

class Item(
    val type: ItemType,
    val position: Vector2,
    val spawnTime: Float = 0f
) {
    private val texture: Texture
    val sprite: Sprite
    var isActive = true
    private var lifeTime = 0f
    private val maxLifeTime = 20f // 20 giây tự hủy
    
    // Hiệu ứng nhấp nháy khi sắp hết hạn
    private var blinkTimer = 0f
    private var shouldBlink = false

    init {
        // Chọn texture dựa trên loại item
        val texturePath = when (type) {
            ItemType.HEALTH -> "items/health.png"
            ItemType.SPEED -> "items/speed.png"  
            ItemType.FOV -> "items/fov.png"
        }
        
        // Sử dụng texture mặc định nếu không tìm thấy
        texture = try {
            Texture(texturePath.toInternalFile())
        } catch (e: Exception) {
            createDefaultTexture(type)
        }
        
        sprite = Sprite(texture).apply {
            setOriginCenter()
            setPosition(position.x, position.y)
            setSize(32f, 32f) // Kích thước item
        }
    }
    
    private fun createDefaultTexture(itemType: ItemType): Texture {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(32, 32, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        
        // Màu sắc theo loại item
        when (itemType) {
            ItemType.HEALTH -> pixmap.setColor(Color.GREEN)
            ItemType.SPEED -> pixmap.setColor(Color.YELLOW)
            ItemType.FOV -> pixmap.setColor(Color.CYAN)
        }
        pixmap.fillCircle(16, 16, 14)
        
        return Texture(pixmap, false).also {
            pixmap.dispose()
        }
    }

    fun update(delta: Float) {
        if (!isActive) return
        
        lifeTime += delta
        
        // Bắt đầu nhấp nháy khi còn 5 giây
        if (lifeTime >= maxLifeTime - 5f) {
            blinkTimer += delta
            shouldBlink = (blinkTimer * 4) % 1f < 0.5f // Nhấp nháy 2 lần/giây
        }
        
        // Tự hủy sau 20 giây
        if (lifeTime >= maxLifeTime) {
            isActive = false
        }
    }

    fun render(batch: SpriteBatch) {
        if (!isActive) return
        
        // Nhấp nháy khi sắp hết hạn
        if (shouldBlink && lifeTime >= maxLifeTime - 5f) {
            val alpha = batch.color.a
            batch.color = Color(batch.color.r, batch.color.g, batch.color.b, 0.5f)
            sprite.draw(batch)
            batch.color = Color(batch.color.r, batch.color.g, batch.color.b, alpha)
        } else {
            sprite.draw(batch)
        }
    }

    fun bounds(): Rectangle {
        return sprite.boundingRectangle
    }

    fun dispose() {
        texture.disposeSafely()
    }
}