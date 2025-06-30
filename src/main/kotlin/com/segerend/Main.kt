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

    fun fill(fruit: Fruit) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                grid[r][c] = fruit
            }
        }
    }

    fun getComboCellsAt(pos: Pos): List<Pos> {
        fun collectMatches(direction: (Int) -> Pos): List<Pos> {
            val fruit = get(pos)
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
        return return result.toList()
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

class Monkey(algorithm: SortAlgorithm) {
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
        SortAlgorithm.BUBBLE -> 8.0
        SortAlgorithm.INSERTION -> 8.0
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
            var remainingProgress = getSpeedPerTick() * GameStats.timeFactor * delta
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

    fun isIdle(): Boolean {
        return state is IdleState || state is WanderingState || state is ChattingState || state is DancingState
    }
}

// --- Game Controller ---

class GameController(rows: Int = GameConfig.ROWS, cols: Int = GameConfig.COLS) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(SortAlgorithm.BOGO)) // Start with one monkey
    val particleSystem = ParticleSystem()
    private var lastTickTime = System.nanoTime()

    fun tick() {
        val now = System.nanoTime()
        val deltaMs : Double = (now - lastTickTime) / 1_000_000.0 // Time in milliseconds
        val delta = deltaMs / 1000.0 // Convert to seconds for normalized delta
        lastTickTime = now

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

// --- Main Application ---

class MonkeySortSimulatorApp : Application() {
    private val rows = GameConfig.ROWS
    private val cols = GameConfig.COLS
    private val cellSize = GameConfig.CELL_SIZE
    private val controller = GameController(rows, cols)
    private val sortStrip = SortStrip()

    internal val root = BorderPane()

    private val buyButton = Button().apply {
        setOnAction { if (!controller.buyMonkey()) println("Not enough coins!") }
    }

    private val upgradeButton = Button("Upgrade to BubbleSort (${GameConfig.MONKEY_UPGRADE_COST} coins)").apply {
        setOnAction { if (!controller.upgradeMonkey()) println("Not enough coins or no monkeys to upgrade!") }
    }

    private val debugBogoButton = Button("Debug: BogoSort all").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BOGO }
            println("All monkeys set to BogoSort")
        }
    }

    private val debugBubbleButton = Button("Debug: BubbleSort all").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }
            println("All monkeys set to BubbleSort")
        }
    }

    private val debugInsertionButton = Button("Debug: InsertionSort all").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.INSERTION }
            println("All monkeys set to InsertionSort")
        }
    }

    private val debugSpawnButton = Button("Debug: Spawn 5 Monkeys").apply {
        setOnAction {
            repeat(5) { controller.monkeys.add(Monkey(SortAlgorithm.BOGO)) }
            println("Spawned 5 new monkeys")
        }
    }

    private val debugSpeedButton = Button("Debug: Speed x25").apply {
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 25.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    private val debugSuperSpeedButton = Button("Debug: Super Speed x1000000").apply {
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 1000000.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    private val chartButton = Button("Show Sort Chart").apply {
        setOnAction {
            SortChartWindow.show(controller)
        }
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
                val frameTime = (now - lastFrameTime) / 1_000_000_000.0
                if (frameTime >= 1.0 / GameConfig.MAX_FPS) {
                    lastFrameTime = now
                    controller.tick()
                    draw(gc)
                }
            }
        }.start()
    }

    private fun draw(gc: GraphicsContext) {
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

        buyButton.isDisable = GameStats.coins < GameConfig.MONKEY_BASE_COST * controller.monkeys.size
        buyButton.text = "Buy Monkey (${controller.getNewMonkeyPrice()} coins)"
        upgradeButton.isDisable = GameStats.coins < GameConfig.MONKEY_UPGRADE_COST || controller.monkeys.none { it.algorithm == SortAlgorithm.BOGO }

        if (controller.gridModel.isSorted()) {
            gc.fill = Color.DODGERBLUE
            gc.fillRoundRect(gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 50, 300.0, 75.0, 20.0, 20.0)
            gc.fill = Color.ORANGE
            gc.font = Utils.emojiCompatibleFont(24.0)
            gc.fillText("🎉 MONKEYSORT FINISHED! 🎉", gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 20)
            controller.particleSystem.add(ConfettiEffect(gc.canvas.width, gc.canvas.height, 1000L))
        }
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}