package com.segerend

import javafx.scene.text.Font

class GameStats {
    companion object {
        var coins: Int = 50

        // The speed at which the game runs, affecting all time-based calculations
        var timeFactor: Double = 1.0
    }
}

object Utils {
    fun emojiCompatibleFont(size: Double): Font {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("win")) Font.font("Segoe UI Emoji", size) else Font.font(size)
    }
}