package com.segerend

import com.segerend.monkey.*
import com.segerend.particles.*
import com.segerend.sorting.*

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlin.random.Random
import javafx.scene.image.Image

// --- Constants ---
object GameConfig {
    var ROWS = 25
    var COLS = 25
    const val CELL_SIZE = 24.0
    const val MONKEY_BASE_COST = 75
    const val MONKEY_COST_INCREASE_FACTOR = 1.1 // 10% increase per monkey
    const val MONKEY_UPGRADE_COST = 500
    const val MONKEY_WANDER_RADIUS_FACTOR = 4.0
    const val COMBO_REWARD_MULTIPLIER = 15
    const val MAX_FPS = 60
    const val STRIP_HEIGHT = 30.0
    const val DEFAULT_MONKEY = "🐒"
}

// --- Core Game Models ---

enum class Fruit(val emoji: String, val color: Color) {
    APPLE("🍎", Color.RED),
    BANANA("🍌", Color.YELLOW),
    GRAPE("🍇", Color.PURPLE),
    ORANGE("🍊", Color.ORANGE),
    WATERMELON("🍉", Color.GREEN),
    PINEAPPLE("🍍", Color.GOLD),
    STRAWBERRY("🍓", Color.CRIMSON),
    CHERRY("🍒", Color.DARKRED),
    KIWI("🥝", Color.OLIVEDRAB),
    PEACH("🍑", Color.PEACHPUFF),
    MANGO("🥭", Color.DARKORANGE),
    BLUEBERRY("🫐", Color.BLUE),
    LEMON("🍋", Color.LEMONCHIFFON),
    LETTUCE("🥬", Color.FORESTGREEN),
    EMPTY(" ", Color.TRANSPARENT);

    companion object {
        fun random(): Fruit = values().filter { it != EMPTY }.random()
    }
}

data class Pos(val row: Int, val col: Int)

data class ShuffleTask(val from: Pos, val to: Pos, val fruit: Fruit)

enum class SortAlgorithm { BOGO, BUBBLE, INSERTION }

class GridModel(val rows: Int = GameConfig.ROWS, val cols: Int = GameConfig.COLS) {
    private val grid = Array(rows) { Array(cols) { Fruit.random() } }

    fun getGridCopy(): Array<Array<Fruit>> = Array(rows) { r -> grid[r].clone() }
    fun isSorted(): Boolean = grid.flatten().zipWithNext().all { it.first.name <= it.second.name }
    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]
    fun set(pos: Pos, fruit: Fruit) { grid[pos.row][pos.col] = fruit }

//    fun sortedPercentage(): Double {
//        val flat = grid.flatten()
//        val totalPairs = flat.size - 1
//        if (totalPairs <= 0) return 100.0
//        val sortedPairs = flat.zipWithNext().count { it.first.name <= it.second.name }
//        return (sortedPairs.toDouble() / totalPairs) * 100.0
//    }

    fun fill(fruit: Fruit) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                grid[r][c] = fruit
            }
        }
    }

    fun getComboCellsAt(pos: Pos): List<Pos> {
        val fruit = get(pos)
        if (fruit == Fruit.EMPTY) return emptyList() // Empty cell may not have combos

        fun collectMatches(direction: (Int) -> Pos): List<Pos> {
            val matches = mutableListOf(pos)
            var offset = 1
            while (true) {
                val nextPos = direction(offset++)
                if (nextPos.row !in 0 until rows || nextPos.col !in 0 until cols || get(nextPos) != fruit) break
                matches.add(nextPos)
            }
            return matches
        }

        val horizontal = collectMatches { Pos(pos.row, pos.col + it) } + collectMatches { Pos(pos.row, pos.col - it) }.drop(1)
        val vertical = collectMatches { Pos(pos.row + it, pos.col) } + collectMatches { Pos(pos.row - it, pos.col) }.drop(1)
        val result = mutableSetOf<Pos>()
        if (horizontal.size >= 3) result.addAll(horizontal)
        if (vertical.size >= 3) result.addAll(vertical)
        return result.toList()
    }

    fun getSameFruitCount(fruit: Fruit): Int {
        return grid.sumOf { row -> row.count { it == fruit } }
    }

    fun getSameFruitNeighborCount(fruit: Fruit): Int {
        val flatGrid = grid.flatten()
        var maxCount = 0
        var count = 0

        for (cell in flatGrid) {
            if (cell == fruit) {
                count++
            } else {
                maxCount = maxOf(maxCount, count)
                count = 0
            }
        }
        return maxOf(maxCount, count) // Handle streak at the end
    }
}

object LockManager {
    private val lockedPositions = mutableSetOf<Pos>()

    @Synchronized
    fun tryLock(vararg positions: Pos): Boolean {
        if (positions.any { it in lockedPositions }) return false
        lockedPositions.addAll(positions)
        return true
    }

    @Synchronized
    fun unlock(vararg positions: Pos) { lockedPositions.removeAll(positions.toSet()) }

    fun clear() {
        lockedPositions.clear()
    }
}

class Monkey(algorithm: SortAlgorithm = SortAlgorithm.BOGO) {
    var algorithm: SortAlgorithm = algorithm
        set(value) {
            field = value
            strategy = makeStrategy(value, GameConfig.ROWS, GameConfig.COLS)
        }

    var strategy: SortStrategy = makeStrategy(algorithm, GameConfig.ROWS, GameConfig.COLS)
        private set

    var state: MonkeyState = IdleState(Random.nextInt(GameConfig.COLS) * GameConfig.CELL_SIZE, Random.nextInt(GameConfig.ROWS) * GameConfig.CELL_SIZE)

    var fruitBeingCarried: Fruit? = null

    private fun getSpeedPerTick(): Double = when (algorithm) {
        SortAlgorithm.BOGO -> 1.0
        SortAlgorithm.BUBBLE -> 12.0
        SortAlgorithm.INSERTION -> 12.0
    }

    fun assignTask(task: ShuffleTask, cellSize: Double = GameConfig.CELL_SIZE): Boolean {
        if (!LockManager.tryLock(task.from, task.to)) return false
        val (currentX, currentY) = state.getDrawPosition()
        state = MovingToSourceState(task, cellSize, currentX, currentY)
        return true
    }

    fun update(delta: Double, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        if (state is ProgressState) {
            // Calculate total progress available this tick
            var remainingProgress = getSpeedPerTick() * delta
            while (remainingProgress > 0 && state is ProgressState) {
                val currentState = state as ProgressState
                val progressNeeded = 1.0 - currentState.progress
                if (remainingProgress >= progressNeeded) {
                    // Complete the state
                    currentState.progress = 1.0
                    currentState.onProgressComplete(this, grid, cellSize, particleSystem)
                    remainingProgress -= progressNeeded
                } else {
                    // Partially advance progress
                    currentState.progress += remainingProgress
                    remainingProgress = 0.0
                }
            }
        } else {
            // For non-ProgressState, call update once per tick
            state.update(this, grid, cellSize, particleSystem)
        }
    }

    fun draw(gc: GraphicsContext, cellSize: Double) {
        state.draw(gc, this, cellSize)
        val (x, y) = state.getDrawPosition()
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText(
            when (algorithm) {
                SortAlgorithm.BOGO -> "Bogo"
                SortAlgorithm.BUBBLE -> "Bubble"
                SortAlgorithm.INSERTION -> "Insert"
            },
            x + 5, y + cellSize * 0.55 - 15
        )
    }

    fun drawBase(gc: GraphicsContext, x: Double, y: Double, cellSize: Double) {
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText(GameConfig.DEFAULT_MONKEY, x + 2, y + cellSize * 0.55)
    }

    fun drawWithOverlay(gc: GraphicsContext, x: Double, y: Double, cellSize: Double, overlay: String, overlayOffsetY: Double = cellSize * 0.1) {
        drawBase(gc, x, y, cellSize)
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText(overlay, x + cellSize / 2, y + overlayOffsetY)
    }

    fun drawWithCarriedFruit(gc: GraphicsContext, x: Double, y: Double, cellSize: Double) {
        fruitBeingCarried?.let {
            gc.fill = Color.OLIVEDRAB
            gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
            gc.fillText(it.emoji, x + 2, y - 2)
        }
        drawBase(gc, x, y, cellSize)
    }

    fun isIdle(): Boolean {
        return state is IdleState || state is WanderingState || state is ChattingState || state is DancingState
    }
}

// --- Game Controller ---

class GameController(rows: Int = GameConfig.ROWS, cols: Int = GameConfig.COLS) {
    var gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(SortAlgorithm.BOGO)) // Start with one monkey
    val particleSystem = ParticleSystem()

    fun tick(frameTime : FrameTime) {
        val deltaMs = frameTime.deltaMs * GameStats.timeFactor
//        val deltaMs = frameTime.deltaMs

        // Convert for normalized delta use frameTime.currentTimeSec
        val delta = frameTime.deltaMs / 1000.0 * GameStats.timeFactor

        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                val task = monkey.strategy.getNextTask(gridModel)
                if (task != null) monkey.assignTask(task, GameConfig.CELL_SIZE)
            }
            monkey.update(delta, gridModel, GameConfig.CELL_SIZE, particleSystem)
        }
        particleSystem.update(deltaMs.toLong())
    }

    fun getNewMonkeyPrice(): Int {
        return (GameConfig.MONKEY_BASE_COST * monkeys.size * GameConfig.MONKEY_COST_INCREASE_FACTOR).toInt()
    }

    fun buyMonkey(): Boolean {
        val cost = getNewMonkeyPrice()
        if (GameStats.coins >= cost) {
            GameStats.coins -= cost
            monkeys.add(Monkey(SortAlgorithm.BOGO))
            return true
        }
        return false
    }

    fun upgradeMonkey(): Boolean {
        if (GameStats.coins >= GameConfig.MONKEY_UPGRADE_COST) {
            monkeys.firstOrNull { it.algorithm == SortAlgorithm.BOGO }?.let {
                it.algorithm = SortAlgorithm.BUBBLE
                GameStats.coins -= GameConfig.MONKEY_UPGRADE_COST
                return true
            }
        }
        return false
    }
}

data class FrameTime(val deltaMs: Double, val currentTimeSec: Double)

// --- Main Application ---

class MonkeySortSimulatorApp : Application() {
    private val rows = GameConfig.ROWS
    private val cols = GameConfig.COLS
    private val cellSize = GameConfig.CELL_SIZE
    val controller = GameController(rows, cols)
    private val sortStrip = SortStrip()

    internal val root = BorderPane()

    private val buyButton = Button().apply {
        id = "buyButton"
        setOnAction { if (!controller.buyMonkey()) println("Not enough coins!") }
    }

    private val upgradeButton = Button("Upgrade to BubbleSort (${GameConfig.MONKEY_UPGRADE_COST} coins)").apply {
        id = "upgradeButton"
        setOnAction { if (!controller.upgradeMonkey()) println("Not enough coins or no monkeys to upgrade!") }
    }

    private val debugBogoButton = Button("Debug: BogoSort all").apply {
        id = "debugBogoButton"
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BOGO }
            println("All monkeys set to BogoSort")
        }
    }

    private val debugBubbleButton = Button("Debug: BubbleSort all").apply {
        id = "debugBubbleButton"
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }
            println("All monkeys set to BubbleSort")
        }
    }

    private val debugInsertionButton = Button("Debug: InsertionSort all").apply {
        id = "debugInsertionButton"
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.INSERTION }
            println("All monkeys set to InsertionSort")
        }
    }

    private val debugSpawnButton = Button("Debug: Spawn 5 Monkeys").apply {
        id = "debugSpawn5MonkeysButton"
        setOnAction {
            repeat(5) { controller.monkeys.add(Monkey(SortAlgorithm.BOGO)) }
            println("Spawned 5 new monkeys")
        }
    }

    private val debugSpeedButton = Button("Debug: Speed x5").apply {
        id = "debugSpeedx5Button"
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 5.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    private val debugSuperSpeedButton = Button("Debug: Super Speed x1000000").apply {
        id = "debugSuperSpeedButton"
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 1000000.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    private val chartButton = Button("Show Sort Chart").apply {
        id = "chartButton"
        setOnAction {
            SortChartWindow.show(controller)
        }
    }

    private val completedImage: Image by lazy {
        val stream = javaClass.getResourceAsStream("/completed.png")
            ?: error("Image resource not found: completed.png")
        Image(stream)
    }

    override fun start(primaryStage: Stage) {
        val canvas = Canvas(cols * cellSize, rows * cellSize + 30)
        val gc = canvas.graphicsContext2D

        root.bottom = HBox(10.0, buyButton, upgradeButton, debugBogoButton, debugBubbleButton, debugInsertionButton, debugSpawnButton, debugSpeedButton, debugSuperSpeedButton, chartButton)
        root.center = canvas

        val scene = Scene(root)
        primaryStage.title = "Monkeysort 🐒"
        primaryStage.scene = scene

        // Exit the application when the main window is closed
        primaryStage.setOnCloseRequest {
            javafx.application.Platform.exit()
        }

        primaryStage.show()

        var lastFrameTime = System.nanoTime()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                val deltaSeconds = (now - lastFrameTime) / 1_000_000_000.0
                if (deltaSeconds >= 1.0 / GameConfig.MAX_FPS) {
                    val deltaMs = deltaSeconds * 1000.0
                    lastFrameTime = now

                    val frameTime = FrameTime(deltaMs, now / 1_000_000_000.0)
                    controller.tick(frameTime)
                    draw(gc, frameTime)
                }
            }
        }.start()
    }

    private fun draw(gc: GraphicsContext, frameTime: FrameTime) {
        gc.fill = Color.BEIGE
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)

        val grid = controller.gridModel.getGridCopy()
        gc.fill = Color.OLIVEDRAB
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.fillText(fruit.emoji, x + 4, y + cellSize * 0.8)
            }
        }

        for (monkey in controller.monkeys) monkey.draw(gc, cellSize)
        controller.particleSystem.render(gc, cellSize)

        gc.fill = Color.DARKGREEN
        gc.font = Utils.emojiCompatibleFont(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 5)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 5)
        gc.fillText("Bubble Monkeys: ${controller.monkeys.count { it.algorithm == SortAlgorithm.BUBBLE }}", 250.0, gc.canvas.height - 5)

        sortStrip.draw(gc, controller.gridModel)

        buyButton.isDisable = GameStats.coins < controller.getNewMonkeyPrice()
        buyButton.text = "Buy Monkey (${controller.getNewMonkeyPrice()} coins)"
        upgradeButton.isDisable = GameStats.coins < GameConfig.MONKEY_UPGRADE_COST || controller.monkeys.none { it.algorithm == SortAlgorithm.BOGO }

        if (controller.gridModel.isSorted()) {
            GameStats.timeFactor = 1.0 // Set time speed back to normal when sorted

            val img = completedImage
            val wave = kotlin.math.sin(frameTime.currentTimeSec * 2 * Math.PI * 0.5)
            val scale = 1.0 + 0.01 * wave
            val offsetY = 3.0 * wave

            val width = img.width * scale
            val height = img.height * scale
            val x = (gc.canvas.width - width) / 2
            val y = (gc.canvas.height - height) / 2 - 20 + offsetY

            gc.drawImage(img, x, y, width, height)
        }
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}