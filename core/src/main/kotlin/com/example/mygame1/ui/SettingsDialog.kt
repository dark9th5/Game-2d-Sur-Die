package com.example.mygame1.ui

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Scaling
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.SettingsManager

class SettingsDialog(
    skin: Skin,
    private val onClose: (() -> Unit)? = null
) : Dialog("Settings", skin) {

    private val musicCheck = CheckBox(" Music", skin)
    private val soundCheck = CheckBox(" Sound", skin)

    // Bạn có thể chỉnh nhanh kích thước ô tick tại đây (ví dụ 128f, 160f, 192f ...)
    private val checkSize = 64f

    init {
        isMovable = true
        isModal = false // cho phép click icon phía sau để toggle đóng/mở
        isResizable = false

        titleLabel.setFontScale(5f)

        // Nút X đóng nhanh ở tiêu đề (tăng vùng chạm cho dễ bấm)
        val closeX = TextButton("X", skin).apply {
            label.setFontScale(4f)
            pad(18f)
            setSize(110f, 110f)
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { hide() }
            })
        }
        // Dọn tiêu đề & bố trí lại để tránh cắt
        titleTable.add().expandX().fillX()
        titleTable.add(closeX).right().padRight(16f).size(110f, 110f)

        val content = contentTable
        content.pad(24f)

        // MUSIC
        musicCheck.isChecked = SettingsManager.musicEnabled
        musicCheck.label.setFontScale(3f)
        // Phóng to ô tick + vùng chạm
        run {
            val style = musicCheck.style as CheckBox.CheckBoxStyle
            val d: Drawable? = style.checkboxOn ?: style.checkboxOff
            // Bắt buộc Image scale theo kích thước cell
            musicCheck.image.setScaling(Scaling.stretch)
            // Set kích thước icon lớn
            musicCheck.imageCell.size(checkSize, checkSize)
            // Tăng khoảng cách và vùng chạm xung quanh
            musicCheck.pad(24f)
        }
        musicCheck.addListener { _ ->
            val enabled = musicCheck.isChecked
            SettingsManager.musicEnabled = enabled
            if (!enabled) AudioManager.stopMusic()
            true
        }

        // SOUND
        soundCheck.isChecked = SettingsManager.soundEnabled
        soundCheck.label.setFontScale(3f)
        run {
            val style = soundCheck.style as CheckBox.CheckBoxStyle
            val d: Drawable? = style.checkboxOn ?: style.checkboxOff
            soundCheck.image.setScaling(Scaling.stretch)
            soundCheck.imageCell.size(checkSize, checkSize)
            soundCheck.pad(24f)
        }
        soundCheck.addListener { _ ->
            SettingsManager.soundEnabled = soundCheck.isChecked
            true
        }

        // Layout
        val table = Table()
        table.defaults().pad(24f).left()
        table.add(musicCheck).left().row()
        table.add(soundCheck).left().row()

        content.add(table).grow()

        pack()
    }

    // Kích thước dialog = 1/4 viewport, căn giữa
    override fun show(stage: Stage?): Dialog {
        val d = super.show(stage)
        stage?.let {
            val w = it.viewport.worldWidth * 0.58f
            val h = it.viewport.worldHeight * 0.58f
            setSize(w, h)
            setPosition((it.viewport.worldWidth - w) / 2f, (it.viewport.worldHeight - h) / 2f)
            invalidateHierarchy(); layout(); toFront()
        }
        return d
    }

    override fun hide(action: Action?) {
        super.hide(action)
        onClose?.invoke()
    }
}
