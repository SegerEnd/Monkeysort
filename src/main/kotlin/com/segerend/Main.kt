package com.segerend

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import kotlin.random.Random

// --- Data Classes & Enums ---

// Fruit enum with emoji and name (name used for sorting)
enum class Fruit(val emoji: String) {
    APPLE("🍎"),
    BANANA("🍌"),
    GRAPE("🍇"),
    ORANGE("🍊"),
    WATERMELON("🍉");

    companion object {
        fun random() = values()[Random.nextInt(values().size)]
    }
}

// Position helper
data class Pos(val row: Int, val col: Int)

// --- Grid Model: holds fruit grid and game logic ---

class GridModel(val rows: Int, val cols: Int) {
    private val grid = Array(rows) { Array(cols) { Fruit.random() } }

    // Returns a copy of grid for safe read-only use
    fun getGridCopy(): Array<Array<Fruit>> = Array(rows) { r -> grid[r].clone() }

    // Checks if grid is sorted alphabetically in row-major order
    fun isSorted(): Boolean {
        val flat = grid.flatten()
        return flat.zipWithNext().all { it.first.name <= it.second.name }
    }

    // Detect combos: horizontal or vertical 12+ same fruits in a row
    // Returns list of positions part of combos
    fun detectCombos(): List<Pos> {
        val combos = mutableSetOf<Pos>()

        // Horizontal combos
        for (r in 0 until rows) {
            var count = 1
            for (c in 1 until cols) {
                if (grid[r][c] == grid[r][c - 1]) count++ else count = 1
                if (count >= 12) {
                    for (cc in c downTo c - 2) combos.add(Pos(r, cc))
                }
            }
        }

        // Vertical combos
        for (c in 0 until cols) {
            var count = 1
            for (r in 1 until rows) {
                if (grid[r][c] == grid[r - 1][c]) count++ else count = 1
                if (count >= 3) {
                    for (rr in r downTo r - 2) combos.add(Pos(rr, c))
                }
            }
        }
        return combos.toList()
    }

    // Shuffle entire grid randomly
    fun shuffleAll() {
        val flat = grid.flatten().toMutableList()
        flat.shuffle()
        for (i in 0 until rows * cols) {
            grid[i / cols][i % cols] = flat[i]
        }
    }

    // Partial shuffle example: shuffle unsorted rows only
    fun shuffleUnsortedRows() {
        for (r in 0 until rows) {
            if (!isRowSorted(r)) {
                val rowFruits = grid[r].toMutableList()
                rowFruits.shuffle()
                grid[r] = rowFruits.toTypedArray()
            }
        }
    }

    private fun isRowSorted(row: Int): Boolean {
        for (c in 0 until cols - 1) {
            if (grid[row][c].name > grid[row][c + 1].name) return false
        }
        return true
    }

    // Swap fruits at two positions
    fun swap(pos1: Pos, pos2: Pos) {
        val temp = grid[pos1.row][pos1.col]
        grid[pos1.row][pos1.col] = grid[pos2.row][pos2.col]
        grid[pos2.row][pos2.col] = temp
    }
}

// --- Shuffle Strategies ---

sealed class ShuffleStrategy {
    abstract fun shuffle(grid: GridModel)
}

class FullShuffleStrategy : ShuffleStrategy() {
    override fun shuffle(grid: GridModel) = grid.shuffleAll()
}

class PartialShuffleStrategy : ShuffleStrategy() {
    override fun shuffle(grid: GridModel) = grid.shuffleUnsortedRows()
}

// --- Monkey class: responsible for shuffling ---

class Monkey(
    val id: Int,
    var shuffleStrategy: ShuffleStrategy = FullShuffleStrategy()
) {
    // Simulate shuffle animation frames here if you want, for now we just shuffle instantly

    fun shuffle(grid: GridModel) {
        shuffleStrategy.shuffle(grid)
    }
}

// --- Game Controller: Manages game state, coins, upgrades, monkeys, game loop ---

class GameController(
    val rows: Int = 25,
    val cols: Int = 25,
) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(1))  // Start with one monkey

    var coins: Int = 0
    var shuffleIntervalMillis: Long = 2000  // 2 seconds default
    var lastShuffleTime = 0L

    // Upgrades
    var useSmarterShuffle = false

    fun tick(currentTimeMillis: Long) {
        if (currentTimeMillis - lastShuffleTime >= shuffleIntervalMillis) {
            // Shuffle with all monkeys (for speed, call shuffle multiple times per tick)
            monkeys.forEach { it.shuffleStrategy = if (useSmarterShuffle) PartialShuffleStrategy() else FullShuffleStrategy() }
            repeat(monkeys.size) {
                monkeys[it].shuffle(gridModel)
            }

            lastShuffleTime = currentTimeMillis

            // After shuffle: check combos and award coins
            val combos = gridModel.detectCombos()
            if (combos.isNotEmpty()) {
                coins += combos.size // 1 coin per fruit in combos (simple)
            }
        }
    }

    // Buy upgrade: speed up shuffle interval by 10%, costs 10 coins
    fun buySpeedUpgrade(): Boolean {
        val cost = 10
        if (coins >= cost) {
            coins -= cost
            shuffleIntervalMillis = (shuffleIntervalMillis * 0.9).toLong().coerceAtLeast(200)
            return true
        }
        return false
    }

    // Buy smarter shuffle unlock for 500 coins
    fun buySmarterShuffleUnlock(): Boolean {
        val cost = 500
        if (coins >= cost && !useSmarterShuffle) {
            coins -= cost
            useSmarterShuffle = true
            return true
        }
        return false
    }

    // Buy extra monkey to speed up (cost grows)
    fun buyMonkey(): Boolean {
        val cost = 20 * monkeys.size // cost grows with number of monkeys
        if (coins >= cost) {
            coins -= cost
            monkeys.add(Monkey(monkeys.size + 1, if (useSmarterShuffle) PartialShuffleStrategy() else FullShuffleStrategy()))
            return true
        }
        return false
    }
}

// --- UI & Rendering ---

class MonkeySortSimulatorApp : Application() {

    private val gridRows = 25
    private val gridCols = 25
    private val cellSize = 24.0

    private val controller = GameController(gridRows, gridCols)

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val canvas = Canvas(gridCols * cellSize, gridRows * cellSize)
        val gc = canvas.graphicsContext2D

        // Bottom UI buttons for upgrades
        val buySpeedBtn = Button("Buy Speed Upgrade (10 coins)")
        val buySmartShuffleBtn = Button("Unlock Smarter Shuffle (500 coins)")
        val buyMonkeyBtn = Button("Buy Monkey (cost grows)")

        buySpeedBtn.setOnAction {
            if (!controller.buySpeedUpgrade()) {
                println("Not enough coins for speed upgrade")
            }
        }
        buySmartShuffleBtn.setOnAction {
            if (!controller.buySmarterShuffleUnlock()) {
                println("Not enough coins or already unlocked")
            }
        }
        buyMonkeyBtn.setOnAction {
            if (!controller.buyMonkey()) {
                println("Not enough coins for monkey")
            }
        }

        val buttonBox = HBox(10.0, buySpeedBtn, buySmartShuffleBtn, buyMonkeyBtn)
        root.bottom = buttonBox
        root.center = canvas

        primaryStage.scene = Scene(root)
        primaryStage.title = "Monkeysort Simulator"
        primaryStage.show()

        // Start game loop
        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                // now is in nanoseconds
                val currentTimeMillis = now / 1_000_000

                controller.tick(currentTimeMillis)
                draw(gc)
            }
        }
        timer.start()
    }

    private fun draw(gc: GraphicsContext) {
        // Clear canvas
        gc.fill = Color.WHITESMOKE
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)

        val grid = controller.gridModel.getGridCopy()

        gc.font = Font.font(cellSize * 0.8)

        // Draw grid fruits
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize

                gc.fill = Color.BLACK
                gc.fillText(fruit.emoji, x + cellSize * 0.1, y + cellSize * 0.8)
            }
        }

        // Draw coins and info
        gc.fill = Color.DARKGREEN
        gc.font = Font.font(18.0)
        gc.fillText("Coins: ${controller.coins}", 10.0, gc.canvas.height - 10)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 150.0, gc.canvas.height - 10)
        gc.fillText("Shuffle Speed: ${controller.shuffleIntervalMillis} ms", 300.0, gc.canvas.height - 10)
        gc.fillText("Smarter Shuffle: ${if (controller.useSmarterShuffle) "YES" else "NO"}", 500.0, gc.canvas.height - 10)
    }
}

// --- Run the app ---
fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}
