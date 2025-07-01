package com.segerend

object GameStats {
    var coins: Int = 50

    // The speed at which the game runs, affecting all time-based calculations
    var timeFactor: Double = 1.0

    fun reset() {
        coins = 50
        timeFactor = 1.0
    }
}