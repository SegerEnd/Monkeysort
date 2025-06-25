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

// --- Core Game Models ---

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

data class Pos(val row: Int, val col: Int)
data class ShuffleTask(val from: Pos, val to: Pos, val fruit: Fruit)

class GridModel(val rows: Int, val cols: Int) {
    private val grid = Array(rows) { Array(cols) { Fruit.random() } }

    fun getGridCopy(): Array<Array<Fruit>> = Array(rows) { r -> grid[r].clone() }

    fun isSorted(): Boolean {
        val flat = grid.flatten()
        return flat.zipWithNext().all { it.first.name <= it.second.name }
    }

    fun detectCombos(): List<Pos> {
        val combos = mutableSetOf<Pos>()
        for (r in 0 until rows) {
            var count = 1
            for (c in 1 until cols) {
                if (grid[r][c] == grid[r][c - 1]) count++ else count = 1
                if (count >= 3) for (cc in c downTo c - 2) combos.add(Pos(r, cc))
            }
        }
        for (c in 0 until cols) {
            var count = 1
            for (r in 1 until rows) {
                if (grid[r][c] == grid[r - 1][c]) count++ else count = 1
                if (count >= 3) for (rr in r downTo r - 2) combos.add(Pos(rr, c))
            }
        }
        return combos.toList()
    }

    fun swap(pos1: Pos, pos2: Pos) {
        val temp = grid[pos1.row][pos1.col]
        grid[pos1.row][pos1.col] = grid[pos2.row][pos2.col]
        grid[pos2.row][pos2.col] = temp
    }

    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]
}

// --- Monkey Logic ---

class Monkey(val id: Int) {
    var currentTask: ShuffleTask? = null
    private var progress = 0.0
    private val speedPerTick = 0.01

    fun assignTask(task: ShuffleTask) {
        currentTask = task
        progress = 0.0
    }

    fun update(grid: GridModel) {
        val task = currentTask ?: return
        progress += speedPerTick
        if (progress >= 1.0) {
            grid.swap(task.from, task.to)
            currentTask = null
        }
    }

    fun isIdle(): Boolean = currentTask == null

    fun getDrawPosition(cellSize: Double): Pair<Double, Double>? {
        val task = currentTask ?: return null
        val x0 = task.from.col * cellSize
        val y0 = task.from.row * cellSize
        val x1 = task.to.col * cellSize
        val y1 = task.to.row * cellSize
        val x = x0 + (x1 - x0) * progress
        val y = y0 + (y1 - y0) * progress
        return x to y
    }

    fun getCarriedFruit(): Fruit? = currentTask?.fruit
}

// --- Game Controller ---

class GameController(val rows: Int = 25, val cols: Int = 25) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(1))
    var coins = 0

    fun tick() {
        // Assign tasks to idle monkeys
        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                val from = Pos(Random.nextInt(rows), Random.nextInt(cols))
                var to: Pos
                do {
                    to = Pos(Random.nextInt(rows), Random.nextInt(cols))
                } while (to == from)

                val fruit = gridModel.get(from)
                monkey.assignTask(ShuffleTask(from, to, fruit))
            }
        }

        monkeys.forEach { it.update(gridModel) }

        // Coins from combos
        val combos = gridModel.detectCombos()
        if (combos.isNotEmpty()) {
            coins += combos.size
        }
    }

    fun buyMonkey(): Boolean {
        val cost = 20 * monkeys.size
        if (coins >= cost) {
            coins -= cost
            monkeys.add(Monkey(monkeys.size + 1))
            return true
        }
        return false
    }
}

// --- Main Application ---

class MonkeySortSimulatorApp : Application() {

    private val rows = 25
    private val cols = 25
    private val cellSize = 24.0

    private val controller = GameController(rows, cols)

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val canvas = Canvas(cols * cellSize, rows * cellSize + 30)
        val gc = canvas.graphicsContext2D

        val buyBtn = Button("Buy Monkey (Cost increases)")
        buyBtn.setOnAction {
            if (!controller.buyMonkey()) println("Not enough coins!")
        }

        root.bottom = HBox(10.0, buyBtn)
        root.center = canvas

        val scene = Scene(root)
        primaryStage.title = "Monkeysort Simulator 🐒"
        primaryStage.scene = scene
        primaryStage.show()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                controller.tick()
                draw(gc)
            }
        }.start()
    }

    private fun draw(gc: GraphicsContext) {
        gc.fill = Color.BEIGE
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)

        val grid = controller.gridModel.getGridCopy()

        gc.font = Font.font(cellSize * 0.75)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.fillText(fruit.emoji, x + 4, y + cellSize * 0.8)
            }
        }

        // Draw moving monkeys with fruit above head
        for (monkey in controller.monkeys) {
            val pos = monkey.getDrawPosition(cellSize) ?: continue
            val (x, y) = pos
            val fruit = monkey.getCarriedFruit()

            if (fruit != null) {
                gc.fillText(fruit.emoji, x + 2, y - 2) // Fruit above
            }
            gc.fillText("🐒", x + 2, y + cellSize * 0.7)
        }

        // Draw UI info
        gc.fill = Color.DARKGREEN
        gc.font = Font.font(16.0)
        gc.fillText("Coins: ${controller.coins}", 10.0, gc.canvas.height - 5)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 5)
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}
