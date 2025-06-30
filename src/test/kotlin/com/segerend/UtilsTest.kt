package com.segerend

import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
}