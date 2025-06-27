package com.segerend

import javafx.scene.text.Font
import javafx.scene.paint.Color

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

// Extension to convert Color to CSS rgb string
fun Color.toRgbString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return "rgb($r, $g, $b)"
}