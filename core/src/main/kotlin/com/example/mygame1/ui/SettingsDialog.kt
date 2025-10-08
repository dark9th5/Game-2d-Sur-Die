package com.example.mygame1.ui

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Scaling
import com.example.mygame1.audio.AudioManager
import com.example.mygame1.data.SettingsManager

class SettingsDialog(
    skin: Skin,
    private val onClose: (() -> Unit)? = null,
    private val inGame: Boolean = false,
    private val onBackHome: (() -> Unit)? = null,
    private val onMusicToggle: (() -> Unit)? = null
) : Dialog("Settings", skin) {

    private var musicEnabled = SettingsManager.musicEnabled
    private var soundEnabled = SettingsManager.soundEnabled
    private val musicButton = TextButton("", skin)
    private val soundButton = TextButton("", skin)
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

        // MUSIC BUTTON
        updateMusicButtonText()
        musicButton.label.setFontScale(3f)
        musicButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                musicEnabled = !musicEnabled
                com.example.mygame1.data.SettingsManager.musicEnabled = musicEnabled
                updateMusicButtonText()
                if (!musicEnabled) {
                    com.example.mygame1.audio.AudioManager.stopMusic()
                } else {
                    onMusicToggle?.invoke()
                }
            }
        })

        // SOUND BUTTON
        updateSoundButtonText()
        soundButton.label.setFontScale(3f)
        soundButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                soundEnabled = !soundEnabled
                com.example.mygame1.data.SettingsManager.soundEnabled = soundEnabled
                updateSoundButtonText()
            }
        })

        // Layout
        val table = Table()
        table.defaults().pad(24f).left()
        table.add(musicButton).width(320f).height(110f).row()
        table.add(soundButton).width(320f).height(110f).row()

        content.add(table).grow().row()

        // Action button (Back Home or Exit) at bottom
        val actionButton = TextButton(if (inGame) "Back Home" else "Exit", skin).apply {
            label.setFontScale(3.2f)
        }
        actionButton.addListener(object: ClickListener(){
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                showConfirm()
            }
        })
        content.add(actionButton).padTop(16f).center()

        pack()
    }

    private fun showConfirm() {
        val msg = if (inGame) "ARE YOU SURE YOU BACK TO MENU ?" else "ARE YOU SURE YOU WANT TO EXIT GAME ?"
        val confirm = object: Dialog("Confirm", skin) {}
        val label = com.badlogic.gdx.scenes.scene2d.ui.Label(msg, skin).apply { setFontScale(2.4f); wrap = true }
        val yesBtn = TextButton("YES", skin).apply { label.setFontScale(2.8f) }
        val noBtn = TextButton("NO", skin).apply { label.setFontScale(2.8f) }
        val inner = Table().apply {
            pad(40f)
            add(label).width(800f).padBottom(40f).row()
            val btnRow = Table()
            btnRow.add(yesBtn).width(260f).height(120f).padRight(60f)
            btnRow.add(noBtn).width(260f).height(120f)
            add(btnRow)
        }
        confirm.contentTable.add(inner).grow()
        yesBtn.addListener(object: ClickListener(){
            override fun clicked(e: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                if (inGame) {
                    // Back to main menu via provided callback
                    onBackHome?.invoke()
                } else {
                    com.badlogic.gdx.Gdx.app.exit()
                }
                confirm.hide(); this@SettingsDialog.hide()
            }
        })
        noBtn.addListener(object: ClickListener(){
            override fun clicked(e: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { confirm.hide() }
        })
        confirm.show(stage)
    }

    private fun updateMusicButtonText() {
        musicButton.setText("Music: " + if (musicEnabled) "ON" else "OFF")
    }
    private fun updateSoundButtonText() {
        soundButton.setText("Sound: " + if (soundEnabled) "ON" else "OFF")
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
