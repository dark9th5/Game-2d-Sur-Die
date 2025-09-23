package com.example.mygame1.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.example.mygame1.data.ScoreManager

class ScoreboardDialog(private val skin: Skin) : Dialog("HighScore", skin) {

    init {
        contentTable.defaults().pad(24f)

        val title = Label("HighScore", skin).apply {
            setFontScale(5f)
            color = Color.GOLD
        }
        contentTable.add(title).row()

        // Lấy lịch sử điểm và sắp xếp giảm dần
        val scores = ScoreManager.getScoreHistory().sortedDescending()
        if (scores.isEmpty()) {
            val empty = Label("No Data", skin).apply { setFontScale(2f) }
            contentTable.add(empty).padTop(20f).row()
        } else {
            val listTable = Table(skin)
            for ((index, sc) in scores.withIndex()) {
                val rank = index + 1
                val line = Label("Rank.$rank : $sc PTS", skin).apply {
                    setFontScale(3f)
                    color = Color.WHITE
                }
                listTable.add(line).left().pad(6f).row()
            }
            val scroll = ScrollPane(listTable, skin)
            contentTable.add(scroll).width(600f).height(400f).padTop(10f).row()
        }

        val closeBtn = TextButton("Close", skin).apply { label.setFontScale(4f) }
        button(closeBtn, true)
        pack()
    }

    override fun show(stage: Stage?): Dialog {
        val d = super.show(stage)
        stage?.let {
            val w = it.viewport.worldWidth * 0.6f
            val h = it.viewport.worldHeight * 0.6f
            setSize(w, h)
            setPosition(
                (it.viewport.worldWidth - w) / 2f,
                (it.viewport.worldHeight - h) / 2f
            )
        }
        return d
    }
}
