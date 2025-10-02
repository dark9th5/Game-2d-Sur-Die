package com.example.mygame1.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.example.mygame1.data.ScoreManager

class ScoreboardDialog(private val skin: Skin) : Dialog("Rank Score", skin) {

    init {
        isModal = false
        isMovable = true
        titleLabel.setFontScale(5f)
        val closeX = TextButton("X", skin).apply {
            label.setFontScale(4f)
            pad(18f)
            setSize(110f, 110f)
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { hide() }
            })
        }
        titleTable.add().expandX().fillX()
        titleTable.add(closeX).right().padRight(16f).size(110f, 110f)

        contentTable.defaults().pad(24f)
        val title = Label("Rank Score", skin).apply {
            setFontScale(5f)
            color = Color.GOLD
        }
        contentTable.add(title).row()

        val topScores = ScoreManager.getTopScoresWithTime(6)
        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())
        if (topScores.isEmpty()) {
            val empty = Label("No Data", skin).apply { setFontScale(2f) }
            contentTable.add(empty).padTop(20f).row()
        } else {
            val listTable = Table(skin)
            for ((index, pair) in topScores.withIndex()) {
                val (score, time) = pair
                val rank = index + 1
                val timeStr = if (time > 0L) sdf.format(java.util.Date(time)) else "--"
                val line = Label("#${rank}: $score   $timeStr", skin).apply {
                    setFontScale(3f)
                    color = Color.WHITE
                }
                listTable.add(line).left().pad(6f).row()
            }
            val scroll = ScrollPane(listTable, skin)
            contentTable.add(scroll).width(700f).height(600f).row()
        }
        pack()
    }

    override fun show(stage: Stage?): Dialog {
        val d = super.show(stage)
        stage?.let {
            val w = it.viewport.worldWidth * 0.62f
            val h = it.viewport.worldHeight * 0.62f
            setSize(w, h)
            setPosition((it.viewport.worldWidth - w)/2f, (it.viewport.worldHeight - h)/2f)
            invalidateHierarchy(); layout(); toFront()
        }
        return d
    }
}
