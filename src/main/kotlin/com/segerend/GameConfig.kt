package com.segerend

object GameConfig {
    var ROWS = 25
    var COLS = 25
    const val CELL_SIZE = 24.0
    const val MONKEY_BASE_COST = 75
    const val MAX_MONKEYS = 150
    const val MONKEY_COST_INCREASE_FACTOR = 1.1 // 10% increase per monkey

    // Number of monkeys upgraded to BubbleSort to unlock "BubbleSort All" button
    const val BUBBLE_SORT_ALL_START_FEE = 750
    // number of monkeys needed to unlock InsertionSort
    const val INSERTION_SORT_ALL_START_FEE = 1500

    const val MONKEY_WANDER_RADIUS_FACTOR = 4.0
    const val COMBO_REWARD_MULTIPLIER = 15

    const val MAX_FPS = 60
    var fps = MAX_FPS
        set(value) {
            field = value.coerceIn(0, MAX_FPS)
        }

    const val STRIP_HEIGHT = 30.0
    const val DEFAULT_MONKEY = "🐒"
}