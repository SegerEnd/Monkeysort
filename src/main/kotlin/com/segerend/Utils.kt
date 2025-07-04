package com.segerend

import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.text.NumberFormat
import java.util.Locale

object Utils {
    fun emojiCompatibleFont(size: Double, osName: String = System.getProperty("os.name").lowercase()): Font {
        return if (osName.contains("win")) Font.font("Segoe UI Emoji", size) else Font.font(size)
    }
}

data class FrameTime(val deltaMs: Double, val currentTimeSec: Double)

// Extension to convert Color to CSS rgb string
fun Color.toRgbString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return "rgb($r, $g, $b)"
}

val systemLocale: Locale = Locale.getDefault()

fun Int.formatWithDots(locale: Locale = systemLocale): String {
    return if (this >= 10_000) {
        NumberFormat.getInstance(locale).format(this)
    } else {
        this.toString()
    }
}