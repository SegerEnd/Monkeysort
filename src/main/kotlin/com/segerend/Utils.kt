package com.segerend

import javafx.scene.paint.Color
import javafx.scene.text.Font

object Utils {
    fun emojiCompatibleFont(size: Double, osName: String = System.getProperty("os.name").lowercase()): Font {
        return if (osName.contains("win")) Font.font("Segoe UI Emoji", size) else Font.font(size)
    }
}

// Extension to convert Color to CSS rgb string
fun Color.toRgbString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return "rgb($r, $g, $b)"
}