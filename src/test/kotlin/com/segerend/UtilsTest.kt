package com.segerend

import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

class UtilsTest {
    @Test
    fun emojiCompatibleFont() {
        var font = Utils.emojiCompatibleFont(12.0)
        assertNotNull(font, "Font should not be null")
        assertTrue(font.size == 12.0, "Font size should match the requested size")

        // Check if the font is emoji compatible
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            assertEquals("Segoe UI Emoji", font.family, "On Windows, the font should be 'Segoe UI Emoji'")
        } else {
            assertNotEquals("Segoe UI Emoji", font.family, "On non-Windows systems, the font should not be 'Segoe UI Emoji'")
        }
    }

    @Test
    fun `test color toRgbString`(){
        val color = Color.rgb(255, 0, 0) // Red color
        val rgbString = color.toRgbString()
        assertEquals("rgb(255, 0, 0)", rgbString, "Color.toRgbString should return the correct RGB string")
    }

    @Test
    fun `test reset GameStats`() {
        GameStats.coins = 700
        GameStats.reset()
        assertEquals(50, GameStats.coins, "GameStats should reset coins to 50 starter money")
        assertEquals(1.0, GameStats.timeFactor, "GameStats speed timeFactor should reset to 1.0")
    }

    @Test
    fun `test formatWithDots`() {
        val number = 12345
        val formatted = number.formatWithDots()
        // NL locale is expected
        if (Locale.getDefault() == Locale.of("nl", "NL")) {
            assertEquals("12.345", formatted, "Number should be formatted with dots for thousands")
        } else {
            assertEquals("12,345", formatted, "Number should be formatted with commas for thousands")
        }

        val smallNumber = 9999
        val smallFormatted = smallNumber.formatWithDots()
        assertEquals("9999", smallFormatted, "Small numbers should not be formatted with dots")
    }
}