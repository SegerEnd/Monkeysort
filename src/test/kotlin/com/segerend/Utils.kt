package com.segerend

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

object AccessibilityUtil {
    val isAccessibilityEnabled: Boolean
        get() {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())

            return when {
                os.contains("mac") -> isMacAccessibilityEnabled()
                os.contains("win") -> isWindowsAccessibilityAvailable()
                else -> true // Assume Linux or other: accessible or unsupported check
            }
        }

    private fun isMacAccessibilityEnabled(): Boolean {
        return try {
            val process = ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to return UI elements enabled"
            ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            result.equals("true", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun isWindowsAccessibilityAvailable(): Boolean {
        // There is no reliable cross-JVM way to check this.
        return true
    }
}