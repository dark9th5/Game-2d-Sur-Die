package com.example.mygame1.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.Color
import com.example.mygame1.Main
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class SplashScreen(private val game: Main) : KtxScreen {

    private var elapsed = 0f// Thời gian đã trôi qua
    private val logo = Texture("logo/Logo1.png")
    private val background = Texture(Gdx.files.internal("background/splash_bg.png"))

    // Tăng kích thước font chữ tiêu đề
    private val font = BitmapFont().apply {
        data.setScale(3f) // tăng scale từ 2f -> 3f
        color = Color.WHITE
    }
    // Sử dụng GlyphLayout để đo kích thước chữ
    private val layout = GlyphLayout()

    private val screenRatio = Gdx.graphics.width.toFloat() / Gdx.graphics.height
    private val imageRatio = background.width.toFloat() / background.height

    private var scale: Float = 0.0f
    private var drawWidth: Float = 0.0f
    private var drawHeight: Float = 0.0f

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f)
        elapsed += delta

        // Tính toán scale background để giữ tỉ lệ
        if (screenRatio < imageRatio) {
            scale = Gdx.graphics.height.toFloat() / background.height
            drawWidth = background.width * scale
            drawHeight = Gdx.graphics.height.toFloat()
        } else {
            scale = Gdx.graphics.width.toFloat() / background.width
            drawWidth = Gdx.graphics.width.toFloat()
            drawHeight = background.height * scale
        }

        game.batch.use { batch ->
            // Vẽ background
            batch.draw(
                background,
                (Gdx.graphics.width - drawWidth) / 2f,
                (Gdx.graphics.height - drawHeight) / 2f,
                drawWidth,
                drawHeight
            )

            // Tính toán kích thước logo
            val targetWidth = Gdx.graphics.width * 0.3f
            val logoScale = targetWidth / logo.width
            val targetHeight = logo.height * logoScale
            val logoX = (Gdx.graphics.width - targetWidth) / 2f
            val logoY = (Gdx.graphics.height - targetHeight) / 2f

            // Vẽ logo
            batch.draw(
                logo,
                logoX,
                logoY,
                targetWidth,
                targetHeight
            )

            // Vẽ chữ "Survival - Die" gần logo hơn
            val text = "Survival - Die"
            layout.setText(font, text)
            val textX = (Gdx.graphics.width - layout.width) / 2f
            val textY = logoY - 10f // gần hơn: 10px thay vì 20px

            font.draw(batch, layout, textX, textY)
        }

        // Sau 5 giây chuyển sang MainMenuScreen
        if (elapsed > 5f) {
            game.setScreen<MainMenuScreen>()
        }
    }

    override fun dispose() {
        background.dispose()
        logo.dispose()
        font.dispose()
    }
}
