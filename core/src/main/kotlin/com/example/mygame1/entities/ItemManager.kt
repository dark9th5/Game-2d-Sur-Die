package com.example.mygame1.entities

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class ItemManager {
    private val items = mutableListOf<Item>()
    private val dropChances = mapOf(
        ItemType.SPEED to 0.4f,    // 40%
        ItemType.HEALTH to 0.4f,   // 40%
        ItemType.FOV to 0.2f       // 20%
    )

    fun spawnItemAt(position: Vector2) {
        val randomValue = Random.nextFloat()
        var cumulativeProbability = 0f
        
        for ((itemType, probability) in dropChances) {
            cumulativeProbability += probability
            if (randomValue <= cumulativeProbability) {
                val item = Item(itemType, position.cpy())
                items.add(item)
                break
            }
        }
    }

    fun update(delta: Float) {
        items.forEach { it.update(delta) }
        items.removeAll { !it.isActive }
    }

    fun render(batch: SpriteBatch) {
        items.forEach { it.render(batch) }
    }

    fun checkCollisions(player: Player): List<Item> {
        val collectedItems = mutableListOf<Item>()
        
        for (item in items) {
            if (item.isActive && item.bounds().overlaps(player.sprite.boundingRectangle)) {
                collectedItems.add(item)
                item.isActive = false
            }
        }
        
        return collectedItems
    }

    fun dispose() {
        items.forEach { it.dispose() }
        items.clear()
    }

    fun getActiveItems(): List<Item> = items.filter { it.isActive }
}