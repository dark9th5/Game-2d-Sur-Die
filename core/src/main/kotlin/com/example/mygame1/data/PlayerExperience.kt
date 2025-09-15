package com.example.mygame1.data

import GameState
import com.example.mygame1.input.PlayerAction

data class PlayerExperience(
    val state: GameState,
    val action: PlayerAction
)

// Mỗi lần người chơi thực hiện action:
fun recordExperience(state: GameState, action: PlayerAction) {
    // Lưu vào mảng hoặc file để dùng cho việc train ML sau này
    ExperienceLog.experiences.add(PlayerExperience(state, action))
}
