package com.segerend

object GameStats {
    var coins: Int = 50

    // The speed at which the game runs, affecting all time-based calculations
    var timeFactor: Double = 1.0 // set GameConfig.fps to 0 when timeFactor is set to 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Time factor cannot be negative")
            field = value
            if (value == 0.0 && GameConfig.fps > 0) {
                GameConfig.fps = 0 // Stop the game loop if timeFactor is set to 0
            } else if (value > 0.0 && GameConfig.fps == 0) {
                GameConfig.fps = GameConfig.MAX_FPS
            }
        }

    fun reset() {
        coins = 50
        timeFactor = 1.0
    }
}