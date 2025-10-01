package com.example.mygame1.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import ktx.assets.disposeSafely

class Bomb(
    val position: Vector2,
    private val fuseTime: Float = 5f,
    val explosionRadius: Float = 100f,
    val damage: Int = 50
) {
    private var timer = 0f
    var exploded = false
        private set
    private var finished = false

    private val texture = Texture("character/Weapons/weapon_bomb.png") // placeholder icon
    private val displayScale = 0.5f

    private val explosionDuration = 0.3f
    private var explosionStartTime = -1f

    // Texture hình tròn biểu diễn vùng nổ đúng bằng bán kính sát thương
    private val explosionTexture: Texture by lazy { createCircleTexture((explosionRadius * 2f).toInt()) }

    private fun createCircleTexture(diameter: Int): Texture {
        val d = diameter.coerceAtLeast(2)
        val pm = com.badlogic.gdx.graphics.Pixmap(d, d, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pm.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.SourceOver)
        val center = d / 2f
        val r = d / 2f
        for (y in 0 until d) {
            for (x in 0 until d) {
                val dx = x - center + 0.5f
                val dy = y - center + 0.5f
                val dist = kotlin.math.sqrt(dx*dx + dy*dy)
                if (dist <= r) {
                    val edge = (dist / r)
                    // edge ~1 ở rìa -> alpha thấp; trung tâm alpha cao
                    val alpha = (1f - edge) * 0.6f + 0.1f
                    pm.setColor(1f, 0.5f + 0.5f * (1f - edge), 0.1f, alpha)
                    pm.drawPixel(x, y)
                }
            }
        }
        val tex = Texture(pm)
        pm.dispose()
        return tex
    }

    fun update(delta: Float, world: World) {
        if (finished) return
        timer += delta
        if (!exploded && timer >= fuseTime) {
            explode(world)
        } else if (exploded) {
            if (explosionStartTime < 0f) explosionStartTime = timer
            if (timer - explosionStartTime >= explosionDuration) {
                finished = true
            }
        }
    }

    private fun explode(world: World) {
        exploded = true
        // Damage player & enemies with line-of-sight
        val bombCenter = position
        // Player
        val player = world.player
        val pCenter = Vector2(player.position.x + player.sprite.width/2f, player.position.y + player.sprite.height/2f)
        if (pCenter.dst(bombCenter) <= explosionRadius && !world.collisionManager.isLineBlocked(bombCenter, pCenter)) {
            player.takeDamage(damage)
        }
        // Enemies
        world.enemies.forEach { enemy ->
            val eCenter = Vector2(enemy.position.x + enemy.sprite.width/2f, enemy.position.y + enemy.sprite.height/2f)
            if (eCenter.dst(bombCenter) <= explosionRadius && !world.collisionManager.isLineBlocked(bombCenter, eCenter)) {
                enemy.takeDamage(damage)
            }
        }
    }

    fun render(batch: SpriteBatch) {
        if (!exploded) {
            batch.color = Color.WHITE
            val w = texture.width * displayScale
            val h = texture.height * displayScale
            batch.draw(texture, position.x - w/2f, position.y - h/2f, w, h)
        } else {
            // Vẽ vùng nổ đúng bằng bán kính (đường kính = 2 * explosionRadius)
            val elapsed = if (explosionStartTime >= 0f) (timer - explosionStartTime).coerceAtLeast(0f) else 0f
            val t = (elapsed / explosionDuration).coerceIn(0f, 1f)
            val fade = 1f - t
            batch.color = Color(1f, 1f, 1f, fade)
            val size = explosionRadius * 2f
            batch.draw(
                explosionTexture,
                position.x - size/2f,
                position.y - size/2f,
                size/2f,
                size/2f,
                size,
                size,
                1f,
                1f,
                0f,
                0,
                0,
                explosionTexture.width,
                explosionTexture.height,
                false,
                false
            )
            batch.color = Color.WHITE
        }
    }

    fun isFinished(): Boolean = finished

    fun dispose() { texture.disposeSafely(); if (explosionTexture != texture) explosionTexture.disposeSafely() }
}
