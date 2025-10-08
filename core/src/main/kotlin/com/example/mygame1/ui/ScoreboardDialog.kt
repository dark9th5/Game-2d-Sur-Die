package com.example.mygame1.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.example.mygame1.data.ScoreManager
import com.example.mygame1.data.Difficulty

class ScoreboardDialog(
    private val skin: Skin,
    initialDifficulty: Difficulty = Difficulty.EASY
) : Dialog("Rank Score", skin) {

    private var currentDifficulty: Difficulty = initialDifficulty
    private var listContainer: Table? = null
    // scrollPane không cần nữa vì chỉ hiển thị tối đa 6 dòng
    private val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())

    init {
        isModal = false
        isMovable = true
        titleLabel.setFontScale(3.5f, 3.5f)
        contentTable.padTop(100f)
        val closeX = TextButton("X", skin).apply {
            label.setFontScale(3f, 3f)
            pad(12f)
            setSize(90f, 90f)
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) { hide() }
            })
        }
        titleTable.add().expandX().fillX()
        titleTable.add(closeX).right().padRight(10f).size(90f, 90f)

        contentTable.defaults().pad(10f)

        val title = Label("Ranking", skin).apply {
            setFontScale(3f, 3f)
            color = Color.CYAN
        }
        contentTable.add(title).padTop(2f).row()

        val tabsTable = Table()
        val difficulties = listOf(
            Difficulty.EASY to "Easy",
            Difficulty.NORMAL to "Normal",
            Difficulty.HARD to "Hard"
        )
        difficulties.forEach { (diff, labelText) ->
            val btn = TextButton(labelText, skin).apply {
                labelCell.pad(2f)
                label.setFontScale(2.2f, 2.2f)
                color = if (diff == currentDifficulty) Color.GOLD else Color.WHITE
                addListener(object: ClickListener(){
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        if (currentDifficulty != diff) {
                            currentDifficulty = diff
                            tabsTable.children.filterIsInstance<TextButton>().forEach { b ->
                                val d = difficulties.firstOrNull { it.second == b.text.toString() }?.first
                                b.color = if (d == currentDifficulty) Color.GOLD else Color.WHITE
                            }
                            rebuildList()
                        }
                    }
                })
            }
            tabsTable.add(btn).width(180f).height(80f).pad(4f)
        }
        contentTable.add(tabsTable).padTop(4f).row()

        // Bảng chứa danh sách top: căn trên, giữa ngang
        listContainer = Table(skin).apply {
            align(Align.top)
            defaults().pad(6f).center()
        }
        val wrapper = Table().apply { add(listContainer).expand().fill().top().center() }
        contentTable.add(wrapper).width(780f).height(520f).expandX().padTop(4f).row()

        rebuildList()
        pack()
    }

    private fun rebuildList() {
        val lc = listContainer ?: return
        lc.clearChildren()
        val topScores = ScoreManager.getTopScoresWithTime(20, currentDifficulty)
        if (topScores.isEmpty()) {
            val empty = Label("No Data", skin).apply { setFontScale(2.5f, 2.5f); color = Color.LIGHT_GRAY; setAlignment(Align.center) }
            lc.add(empty).center().row()
            lc.invalidateHierarchy(); return
        }
        topScores.take(6).forEachIndexed { index, pair ->
            val (score, time) = pair
            val rank = index + 1
            val timeStr = if (time > 0L) sdf.format(java.util.Date(time)) else "--"
            val line = Label("#${rank}: $score   $timeStr", skin).apply {
                setFontScale(3f, 3f)
                color = Color.CYAN
                setAlignment(Align.center)
            }
            lc.add(line).center().row()
        }
        lc.invalidateHierarchy()
    }

    override fun show(stage: Stage?): Dialog {
        val d = super.show(stage)
        stage?.let {
            val w = it.viewport.worldWidth * 0.6f
            val h = it.viewport.worldHeight * 0.7f
            setSize(w, h)
            invalidateHierarchy(); layout()
            setPosition((it.viewport.worldWidth - w)/2f, (it.viewport.worldHeight - h)/2f)
            toFront()
        }
        return d
    }
}
