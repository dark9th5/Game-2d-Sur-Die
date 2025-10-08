package com.example.mygame1.data

enum class Difficulty(val displayName: String) {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard");

    companion object {
        fun fromString(value: String?): Difficulty = when (value?.uppercase()) {
            "NORMAL" -> NORMAL
            "HARD" -> HARD
            else -> EASY
        }
    }
}

