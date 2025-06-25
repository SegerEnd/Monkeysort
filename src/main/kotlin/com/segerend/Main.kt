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
    WATERMELON("🍉"),
    PINEAPPLE("🍍"),
    EMPTY(" ");

    companion object {
//        fun random() = values()[Random.nextInt(values().size)]
        fun random(): Fruit {
            val fruits = values().filter { it != EMPTY }
            return fruits[Random.nextInt(fruits.size)]
        }
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
                if (count >= 8) for (cc in c downTo c - 2) combos.add(Pos(r, cc))
            }
        }
        for (c in 0 until cols) {
            var count = 1
            for (r in 1 until rows) {
                if (grid[r][c] == grid[r - 1][c]) count++ else count = 1
                if (count >= 8) for (rr in r downTo r - 2) combos.add(Pos(rr, c))
            }
        }
        return combos.toList()
    }

    fun comboAt(pos: Pos): Int {
        val fruit = get(pos)
        if (fruit == Fruit.EMPTY) return 0

        var totalMatches = 1 // Count this cell

        // Horizontal check
        var left = pos.col - 1
        while (left >= 0 && get(Pos(pos.row, left)) == fruit) {
            totalMatches++
            left--
        }
        var right = pos.col + 1
        while (right < cols && get(Pos(pos.row, right)) == fruit) {
            totalMatches++
            right++
        }
        if (totalMatches >= 3) return totalMatches

        // Vertical check
        totalMatches = 1
        var up = pos.row - 1
        while (up >= 0 && get(Pos(up, pos.col)) == fruit) {
            totalMatches++
            up--
        }
        var down = pos.row + 1
        while (down < rows && get(Pos(down, pos.col)) == fruit) {
            totalMatches++
            down++
        }
        if (totalMatches >= 3) return totalMatches

        return 0
    }

    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]

    fun set(pos: Pos, fruit: Fruit) {
        grid[pos.row][pos.col] = fruit
    }

    fun swap(a: Pos, b: Pos) {
        val tmp = grid[a.row][a.col]
        grid[a.row][a.col] = grid[b.row][b.col]
        grid[b.row][b.col] = tmp
    }
}

// --- Monkey Logic ---

class Monkey(val id: Int) {
    enum class State { IDLE, MOVING_TO_SOURCE, CARRYING }

    private var state = State.IDLE
    private var task: ShuffleTask? = null
    private var progress = 0.0
    private val speedPerTick = 0.01

    private var startX = 0.0
    private var startY = 0.0
    private var endX = 0.0
    private var endY = 0.0

    private var fruitBeingCarried: Fruit? = null
    private var fruitToSwapBack: Fruit? = null

    fun assignTask(newTask: ShuffleTask, cellSize: Double) {
        task = newTask
        fruitBeingCarried = null

        val from = newTask.from
        // Align monkey exactly on grid
        startX = endX
        startY = endY
        endX = from.col * cellSize
        endY = from.row * cellSize
        progress = 0.0
        state = State.MOVING_TO_SOURCE
    }

    fun update(grid: GridModel, cellSize: Double) {
        if (task == null) return

        progress += speedPerTick
        if (progress >= 1.0) {
            progress = 0.0

            when (state) {
                State.MOVING_TO_SOURCE -> {
                    val from = task!!.from
                    val to = task!!.to

                    // Pick up fruit from 'from', and store destination fruit
                    fruitBeingCarried = grid.get(from)
                    fruitToSwapBack = grid.get(to)
                    grid.set(from, Fruit.EMPTY) // Leave empty

                    // Move to 'to'
                    startX = from.col * cellSize
                    startY = from.row * cellSize
                    endX = to.col * cellSize
                    endY = to.row * cellSize
                    state = State.CARRYING
                }

                State.CARRYING -> {
                    val to = task!!.to
                    val from = task!!.from

                    grid.set(to, fruitBeingCarried!!)
                    grid.set(from, fruitToSwapBack!!)

                    // Check combos only on 'to' cell
                    val comboCount = grid.comboAt(to)
                    if (comboCount >= 3) {
                        GameStats.coins += comboCount * 10
                    }

                    fruitBeingCarried = null
                    fruitToSwapBack = null
                    task = null
                    state = State.IDLE
                }

                else -> {}
            }
        }
    }

    fun isIdle(): Boolean = (state == State.IDLE && task == null)

    fun getDrawPosition(): Pair<Double, Double> {
        val x = lerp(startX, endX, progress)
        val y = lerp(startY, endY, progress)
        return x to y
    }

    fun getCarriedFruit(): Fruit? = fruitBeingCarried

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
}

// --- Game Controller ---

class GameController(val rows: Int = 25, val cols: Int = 25) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(1))

    fun tick() {
        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                val from = Pos(Random.nextInt(rows), Random.nextInt(cols))
                var to: Pos
                do {
                    to = Pos(Random.nextInt(rows), Random.nextInt(cols))
                } while (from == to)

                val fruit = gridModel.get(from)
                monkey.assignTask(ShuffleTask(from, to, fruit), cellSize = 24.0)
            }
        }

        monkeys.forEach { it.update(gridModel, cellSize = 24.0) }

        val combos = gridModel.detectCombos()
        if (combos.isNotEmpty()) GameStats.coins += combos.size
    }

    fun buyMonkey(): Boolean {
        val cost = 200 * monkeys.size
        if (GameStats.coins >= cost) {
            GameStats.coins -= cost
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

        val buyBtn = Button("Buy Monkey (200 * Monkeys)")
        buyBtn.setOnAction {
            if (!controller.buyMonkey()) println("Not enough coins!")
        }

        root.bottom = HBox(10.0, buyBtn)
        root.center = canvas

        val scene = Scene(root)
        primaryStage.title = "Monkeysort 🐒"
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
            val pos = monkey.getDrawPosition() ?: continue
            val (x, y) = pos
            val fruit = monkey.getCarriedFruit()
            if (fruit != null) {
                gc.fillText(fruit.emoji, x + 2, y - 2)
            }
            gc.fillText("🐒", x + 2, y + cellSize * 0.7)
        }

        // Draw UI info
        gc.fill = Color.DARKGREEN
        gc.font = Font.font(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 5)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 5)
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}
