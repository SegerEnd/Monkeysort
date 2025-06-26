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
    STRAWBERRY("🍓"),
    CHERRY("🍒"),
    KIWI("🥝"),
    PEACH("🍑"),
    MANGO("🥭"),
    BLUEBERRY("🫐"),
    LEMON("🍋"),
    LETTUCE("🥬"),
    EMPTY(" ");

    companion object {
        fun random(): Fruit {
            val fruits = values().filter { it != EMPTY }
            return fruits[Random.nextInt(fruits.size)]
        }
    }
}

data class Pos(val row: Int, val col: Int)
data class ShuffleTask(val from: Pos, val to: Pos, val fruit: Fruit)

enum class SortAlgorithm {
    BOGO,
    BUBBLE
}

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
        if (totalMatches >= 2) return totalMatches

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
        if (totalMatches >= 2) return totalMatches

        return 0
    }

    fun getComboCellsAt(pos: Pos): List<Pos> {
        val fruit = get(pos)
        if (fruit == Fruit.EMPTY) return emptyList()

        val matched = mutableSetOf<Pos>()

        // Horizontal combo detection
        val row = pos.row
        val col = pos.col

        var left = col
        while (left > 0 && get(Pos(row, left - 1)) == fruit) left--
        var right = col
        while (right < cols - 1 && get(Pos(row, right + 1)) == fruit) right++

        if (right - left + 1 >= 2) {
            for (c in left..right) matched.add(Pos(row, c))
        }

        // Vertical combo detection
        var up = row
        while (up > 0 && get(Pos(up - 1, col)) == fruit) up--
        var down = row
        while (down < rows - 1 && get(Pos(down + 1, col)) == fruit) down++

        if (down - up + 1 >= 2) {
            for (r in up..down) matched.add(Pos(r, col))
        }

        return matched.toList()
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

// Shared Lock Manager
object LockManager {
    private val lockedPositions = mutableSetOf<Pos>()

    @Synchronized
    fun tryLock(vararg positions: Pos): Boolean {
        if (positions.any { it in lockedPositions }) return false
        lockedPositions.addAll(positions)
        return true
    }

    @Synchronized
    fun unlock(vararg positions: Pos) {
        lockedPositions.removeAll(positions.toSet())
    }
}

// Updated Monkey class with proper locking and task state
class Monkey(val id: Int) {
    enum class State { IDLE, MOVING_TO_SOURCE, CARRYING, RETURNING }

    private var state = State.IDLE
    var task: ShuffleTask? = null
    private var progress = 0.0
    private var speedPerTick = 0.03

    private var startX = 0.0
    private var startY = 0.0
    private var endX = 0.0
    private var endY = 0.0

    private var fruitBeingCarried: Fruit? = null

    // Sorting algorithm
    var algorithm: SortAlgorithm = SortAlgorithm.BOGO
        set(value) {
            field = value
            speedPerTick = when (value) {
                SortAlgorithm.BOGO -> 0.03
                SortAlgorithm.BUBBLE -> 0.07 // Faster monkey speed for BubbleSort
            }
        }

    fun assignTask(newTask: ShuffleTask, cellSize: Double): Boolean {
        if (!LockManager.tryLock(newTask.from, newTask.to)) return false

        val (currentX, currentY) = getDrawPosition()

        task = newTask
        fruitBeingCarried = null

        startX = currentX
        startY = currentY
        endX = newTask.from.col * cellSize
        endY = newTask.from.row * cellSize

        progress = 0.0
        state = State.MOVING_TO_SOURCE
        return true
    }

    fun update(grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        if (task == null) return

        progress += speedPerTick * GameStats.timeFactor
        if (progress >= 1.0) {
            progress = 0.0

            val from = task!!.from
            val to = task!!.to

            when (state) {
                State.MOVING_TO_SOURCE -> {
                    fruitBeingCarried = grid.get(from)
                    grid.set(from, Fruit.EMPTY)

                    startX = from.col * cellSize
                    startY = from.row * cellSize
                    endX = to.col * cellSize
                    endY = to.row * cellSize
                    state = State.CARRYING
                }

                State.CARRYING -> {
                    val oldFruit = grid.get(to)
                    grid.set(to, fruitBeingCarried!!)
                    fruitBeingCarried = oldFruit

                    startX = to.col * cellSize
                    startY = to.row * cellSize
                    endX = from.col * cellSize
                    endY = from.row * cellSize
                    state = State.RETURNING

                    val comboCount = grid.comboAt(to)
                    if (comboCount >= 2) {
                        GameStats.coins += comboCount * 15
                        val comboPositions = grid.getComboCellsAt(to)
                        if (comboPositions.isNotEmpty()) {
                            particleSystem.add(ComboParticleEffect(comboPositions, 24.0))
                        }
                    }
                }

                State.RETURNING -> {
                    grid.set(from, fruitBeingCarried!!)
                    fruitBeingCarried = null
                    LockManager.unlock(from, to)
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
    val particleSystem = ParticleSystem()
    private var lastTickTime = System.nanoTime()

    // Track grid state for bubble sort
    private var bubbleSortIndex = 0
    private var bubbleSortPass = 0

    fun tick() {
        val now = System.nanoTime()
        val deltaMs = (now - lastTickTime) / 1_000_000
        lastTickTime = now

        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                when (monkey.algorithm) {
                    SortAlgorithm.BOGO -> assignRandomTask(monkey)
                    SortAlgorithm.BUBBLE -> assignBubbleTask(monkey)
                }
            }
            monkey.update(gridModel, cellSize = 24.0, particleSystem)
        }

        particleSystem.update(deltaMs)
    }

    private fun assignBubbleTask(monkey: Monkey) {
        val totalCells = rows * cols

        while (true) {
            if (bubbleSortIndex >= totalCells - 1 - bubbleSortPass) {
                // End of pass, start next pass
                bubbleSortIndex = 0
                bubbleSortPass++
                if (bubbleSortPass >= totalCells - 1) {
                    // Fully sorted!
                    bubbleSortPass = 0
                }
                return
            }

            val indexA = bubbleSortIndex
            val indexB = bubbleSortIndex + 1

            // Convert to (row, col)
            val from = Pos(indexA / cols, indexA % cols)
            val to = Pos(indexB / cols, indexB % cols)

            val fruitA = gridModel.get(from)
            val fruitB = gridModel.get(to)

            bubbleSortIndex++

            if (fruitA.name > fruitB.name) {
                monkey.assignTask(ShuffleTask(from, to, fruitA), cellSize = 24.0)
                return
            }
        }
    }

    private fun assignRandomTask(monkey: Monkey) {
        val from = Pos(Random.nextInt(rows), Random.nextInt(cols))
        var to: Pos
        do {
            to = Pos(Random.nextInt(rows), Random.nextInt(cols))
        } while (from == to)

        val fruit = gridModel.get(from)
        monkey.assignTask(ShuffleTask(from, to, fruit), cellSize = 24.0)
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

    fun upgradeMonkey(): Boolean {
        if (GameStats.coins >= 500) {
            // Upgrade first monkey with BogoSort
            monkeys.firstOrNull { it.algorithm == SortAlgorithm.BOGO }?.let {
                it.algorithm = SortAlgorithm.BUBBLE
                GameStats.coins -= 500
                return true
            }
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

    private val buyButton = Button("Buy Monkey").apply {
        setOnAction {
            if (!controller.buyMonkey()) println("Not enough coins!")
        }
    }

    private val upgradeButton = Button("Upgrade to BubbleSort (500 coins)").apply {
        setOnAction {
            if (!controller.upgradeMonkey()) println("Not enough coins or no monkeys to upgrade!")
        }
    }

    // add a debug button to make from all monkeys BogoSort
    private val debugBogoButton = Button("Debug: Set All to BogoSort").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BOGO }
            println("All monkeys set to BogoSort")
        }
    }
    // debug button to make all monkeys BubbleSort
    private val debugBubbleButton = Button("Debug: Set All to BubbleSort").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }
            println("All monkeys set to BubbleSort")
        }
    }
    // debug button to spawn 50 new monkeys
    private val debugSpawnButton = Button("Debug: Spawn 50 Monkeys").apply {
        setOnAction {
            repeat(50) {
                controller.monkeys.add(Monkey(controller.monkeys.size + 1).apply {
                    algorithm = SortAlgorithm.BOGO
                })
            }
            println("Spawned 50 new monkeys")
        }
    }

    // Debug button to set the speed of the game to 10x
    private val debugSpeedButton = Button("Debug: Speed x15").apply {
        setOnAction {
            // toggle the speed factor
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 15.0 else 1.0
            println("Game speed set to x15")
        }
    }

    fun emojiCompatibleFont(size: Double): Font {
        val os = System.getProperty("os.name").lowercase()

        if (os.contains("win")) {
            return Font.font("Segoe UI Emoji", size)
        } else {
            return Font.font(size)
        }
    }

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val canvas = Canvas(cols * cellSize, rows * cellSize + 30)
        val gc = canvas.graphicsContext2D

        root.bottom = HBox(10.0, buyButton, upgradeButton, debugBogoButton, debugBubbleButton, debugSpawnButton, debugSpeedButton)
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

        // Set fill color for visibility of emojis and text
        gc.fill = Color.OLIVEDRAB

        val grid = controller.gridModel.getGridCopy()

        gc.font = emojiCompatibleFont(cellSize * 0.75)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.fillText(fruit.emoji, x + 4, y + cellSize * 0.8)
            }
        }

        gc.font = emojiCompatibleFont(cellSize * 0.95)

        // Draw monkeys with algorithm indicator
        for (monkey in controller.monkeys) {
            val pos = monkey.getDrawPosition()
            val (x, y) = pos
            val fruit = monkey.getCarriedFruit()
            if (fruit != null) {
                gc.fill = Color.OLIVEDRAB
                gc.fillText(fruit.emoji, x + 2, y - 2)
            }
            gc.fill = Color.CHOCOLATE

            gc.fillText("🐒", x + 2, y + cellSize * 0.55)

            // Draw algorithm indicator
            gc.font = emojiCompatibleFont(10.0)
            gc.fillText(
                when (monkey.algorithm) {
                    SortAlgorithm.BOGO -> "Bogo"
                    SortAlgorithm.BUBBLE -> "Bubble"
                },
                x + 5,
                y + cellSize * 0.55 - 15
            )
            gc.font = emojiCompatibleFont(cellSize * 0.75)
        }

        // Render particle effects
        controller.particleSystem.render(gc, cellSize)

        // Draw UI info
        gc.fill = Color.DARKGREEN
        gc.font = emojiCompatibleFont(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 5)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 5)
        gc.fillText("Bubble Monkeys: ${controller.monkeys.count { it.algorithm == SortAlgorithm.BUBBLE }}",
            250.0, gc.canvas.height - 5)

        buyButton.isDisable = GameStats.coins < 200 * controller.monkeys.size
        buyButton.text = "Buy Monkey (${200 * controller.monkeys.size} coins)"
        upgradeButton.isDisable = GameStats.coins < 500 ||
                controller.monkeys.none { it.algorithm == SortAlgorithm.BOGO }

        // Show completion message
        if (controller.gridModel.isSorted()) {
            // 🎉 MONKEYSORT FINISHED! 🎉
            // draw a beautiful message with finished message with backdrop behind text
            gc.fill = Color.DODGERBLUE
            gc.fillRoundRect(
                gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 50, 300.0, 75.0, 20.0, 20.0
            )
            gc.fill = Color.ORANGE
            gc.font = emojiCompatibleFont(24.0)
            gc.fillText("🎉 MONKEYSORT FINISHED! 🎉", gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 20)

            // add confetti effect
            controller.particleSystem.add(ConfettiEffect( gc.canvas.width, gc.canvas.height,  1000L))
        }
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}