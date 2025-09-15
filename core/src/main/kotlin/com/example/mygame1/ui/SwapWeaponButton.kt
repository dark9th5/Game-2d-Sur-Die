package com.example.mygame1.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.example.mygame1.entities.Player

class SwapWeaponButton(
    stage: Stage,
    player: Player,
    skin: Skin = Skin(Gdx.files.internal("uiskin.json")) // Skin mặc định hoặc truyền vào
) {
    val button = TextButton("Đổi súng", skin)

    init {
        button.setSize(120f, 60f)
        button.setPosition(100f, 80f) // Điều chỉnh vị trí cho phù hợp UI

        button.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent?,
                x: Float, y: Float, pointer: Int, button: Int
            ): Boolean {
                val nextIndex = (player.weaponIndex + 1) % player.weapons.size
                player.selectWeapon(
                    nextIndex,
                    enemyPosition = player.position,
                    bulletsOnMap = player.bullets
                )
                return true
            }
        })

        stage.addActor(button)
    }
}
