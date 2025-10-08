package com.example.mygame1.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.example.mygame1.Assets

class StarField(
    private val starCount: Int = 400,
    private val sizeScale: Float = 1f // mặc định không giảm kích thước
) {
    val stars = mutableListOf<Star>()
    // Use shared asset
    val starTexture: Texture = Assets.texture("snowflake/snowflake.png")

    data class Star(
        var position: Vector2,
        var velocity: Vector2,
        var size: Float,
        var brightness: Float,
        var rotation: Float,
        var rotationSpeed: Float
    )

    fun update(delta: Float, mapWidth: Float, mapHeight: Float) {
        val margin = 300f

        // Nếu danh sách rỗng thì tạo mới
        if (stars.isEmpty()) {
            repeat(starCount) {
                stars.add(createRandomStar(mapWidth, mapHeight))
            }
        }

        // Cập nhật vị trí, reset sao nếu bay quá xa
        stars.forEach { star ->
            star.position.add(star.velocity.cpy().scl(delta * 80f))
            star.rotation = (star.rotation + star.rotationSpeed * delta * 30f) % 360

            if (star.position.x < -margin || star.position.x > mapWidth + margin ||
                star.position.y < -margin || star.position.y > mapHeight + margin) {
                resetStar(star, mapWidth, mapHeight)
            }
        }
    }

    private fun createRandomStar(mapWidth: Float, mapHeight: Float): Star {
        val posX = MathUtils.random(0f, mapWidth)
        val posY = MathUtils.random(0f, mapHeight)
        val angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians
        val speed = MathUtils.random(1f, 3f)
        // size gốc: 32f~64f, nhân sizeScale
        val baseMinSize = 32f
        val baseMaxSize = 64f
        return Star(
            position = Vector2(posX, posY),
            velocity = Vector2(MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed),
            size = MathUtils.random(baseMinSize, baseMaxSize) * sizeScale,
            brightness = MathUtils.random(0.5f, 1f),
            rotation = MathUtils.random(0f, 360f),
            rotationSpeed = MathUtils.random(-2f, 2f)
        )
    }

    private fun resetStar(star: Star, mapWidth: Float, mapHeight: Float) {
        val posX = MathUtils.random(0f, mapWidth)
        val posY = MathUtils.random(0f, mapHeight)
        val angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians
        val speed = MathUtils.random(1f, 3f)
        val baseMinSize = 32f
        val baseMaxSize = 64f
        star.position.set(posX, posY)
        star.velocity.set(MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed)
        star.size = MathUtils.random(baseMinSize, baseMaxSize) * sizeScale
        star.brightness = MathUtils.random(0.5f, 1f)
        star.rotation = MathUtils.random(0f, 360f)
        star.rotationSpeed = MathUtils.random(-2f, 2f)
    }

    fun render(batch: SpriteBatch) {
        for (star in stars) {
            val color = Color(star.brightness, star.brightness, star.brightness, 1f)
            batch.color = color
            batch.draw(
                starTexture,
                star.position.x - star.size / 2,
                star.position.y - star.size / 2,
                star.size / 2,
                star.size / 2,
                star.size,
                star.size,
                1f,
                1f,
                star.rotation,
                0,
                0,
                starTexture.width,
                starTexture.height,
                false,
                false
            )
        }
        batch.color = Color.WHITE
    }

    fun dispose() { /* shared texture managed by Assets */ }
}
