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

// --- Constants ---
object GameConfig {
    const val ROWS = 25
    const val COLS = 25
    const val CELL_SIZE = 24.0
    const val MONKEY_BASE_COST = 200
    const val MONKEY_UPGRADE_COST = 500
    const val MONKEY_WANDER_RADIUS_FACTOR = 3.0
    const val COMBO_REWARD_MULTIPLIER = 15
    const val MAX_FPS = 60
}

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

    private fun collectMatches(pos: Pos, direction: (Int) -> Pos): List<Pos> {
        val fruit = get(pos)
        val matches = mutableListOf(pos)
        var offset = 1
        while (true) {
            val nextPos = direction(offset)
            if (nextPos.row !in 0 until rows || nextPos.col !in 0 until cols) break
            if (get(nextPos) != fruit) break
            matches.add(nextPos)
            offset++
        }
        return matches
    }

    fun getComboCellsAt(pos: Pos): List<Pos> {
        val horizontal = collectMatches(pos) { Pos(pos.row, pos.col + it) } +
                collectMatches(pos) { Pos(pos.row, pos.col - it) }.drop(1)

        val vertical = collectMatches(pos) { Pos(pos.row + it, pos.col) } +
                collectMatches(pos) { Pos(pos.row - it, pos.col) }.drop(1)

        val result = mutableSetOf<Pos>()
        if (horizontal.size >= 3) result.addAll(horizontal)
        if (vertical.size >= 3) result.addAll(vertical)
        return result.toList()
    }

    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]

    fun set(pos: Pos, fruit: Fruit) {
        grid[pos.row][pos.col] = fruit
    }
}

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

// --- Monkey Logic ---

class Monkey {
    enum class State { IDLE, MOVING_TO_SOURCE, CARRYING, RETURNING, WANDERING, CHATTING, DANCING }

    var state: State = State.IDLE
        private set

    var task: ShuffleTask? = null

    private var progress = 0.0
    private var speedPerTick = 0.03

    private var startX = 0.0
    private var startY = 0.0
    private var endX = 0.0
    private var endY = 0.0

    private var fruitBeingCarried: Fruit? = null

    private var wanderTargetX = 0.0
    private var wanderTargetY = 0.0

    private var behaviorTimer = 0.0
    private val behaviorDuration = 100.0

    var algorithm: SortAlgorithm = SortAlgorithm.BOGO
        set(value) {
            field = value
            speedPerTick = when (value) {
                SortAlgorithm.BOGO -> 0.03
                SortAlgorithm.BUBBLE -> 0.07
            }
        }

    fun assignTask(newTask: ShuffleTask, cellSize: Double): Boolean {
        if (!LockManager.tryLock(newTask.from, newTask.to)) return false

        // Get current position BEFORE modifying state
        val (currentX, currentY) = getDrawPosition()

        task = newTask
        fruitBeingCarried = null

        // Set movement from CURRENT POSITION to source
        startX = currentX
        startY = currentY
        endX = newTask.from.col * cellSize
        endY = newTask.from.row * cellSize
        progress = 0.0

        state = State.MOVING_TO_SOURCE
        return true
    }

    fun update(grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        val deltaTime = 1.0 * GameStats.timeFactor

        if (task != null) {
            progress += speedPerTick * deltaTime
            if (progress >= 1.0) {
                progress = 0.0

                val from = task!!.from
                val to = task!!.to

                when (state) {
                    State.MOVING_TO_SOURCE -> {
                        fruitBeingCarried = grid.get(from)
                        grid.set(from, Fruit.EMPTY)

                        // Continue from current animation position
                        startX = endX
                        startY = endY
                        endX = to.col * cellSize
                        endY = to.row * cellSize
                        state = State.CARRYING
                    }

                    State.CARRYING -> {
                        val oldFruit = grid.get(to)
                        grid.set(to, fruitBeingCarried!!)
                        fruitBeingCarried = oldFruit

                        // Continue from current animation position
                        startX = endX
                        startY = endY
                        endX = from.col * cellSize
                        endY = from.row * cellSize
                        state = State.RETURNING

                        val comboCount = grid.getComboCellsAt(to).size
                        if (comboCount >= 2) {
                            GameStats.coins += comboCount * GameConfig.COMBO_REWARD_MULTIPLIER
                            val comboPositions = grid.getComboCellsAt(to)
                            if (comboPositions.isNotEmpty()) {
                                particleSystem.add(ComboParticleEffect(comboPositions, cellSize))
                            }
                        }
                    }

                    State.RETURNING -> {
                        grid.set(from, fruitBeingCarried!!)
                        fruitBeingCarried = null
                        LockManager.unlock(from, to)
                        task = null

                        // Set idle position to current animation position
                        startX = endX
                        startY = endY
                        state = State.IDLE
                    }

                    else -> {}
                }
            }
        } else {
            when (state) {
                State.IDLE -> {
                    val choice = (1..4).random()
                    when (choice) {
                        1 -> startWandering(cellSize)
                        2 -> startChatting()
                        3 -> startDancing()
                        else -> {}
                    }
                }

                State.WANDERING -> {
                    progress += speedPerTick * deltaTime
                    if (progress >= 1.0) {
                        progress = 0.0
                        if ((1..5).random() == 1) {
                            state = State.IDLE
                        } else {
                            pickNewWanderTarget(cellSize)
                        }
                    }
                }

                State.CHATTING, State.DANCING -> updateBehavioralState(deltaTime)

                else -> {}
            }
        }
    }

    private fun setMovement(from: Pos, to: Pos, cellSize: Double) {
        startX = from.col * cellSize
        startY = from.row * cellSize
        endX = to.col * cellSize
        endY = to.row * cellSize
        progress = 0.0
    }

    private fun updateBehavioralState(deltaTime: Double) {
        behaviorTimer += deltaTime
        if (behaviorTimer >= behaviorDuration) {
            behaviorTimer = 0.0
            state = State.IDLE
        }
    }

    private fun startWandering(cellSize: Double) {
        state = State.WANDERING
        progress = 0.0
        wanderTargetX = startX
        wanderTargetY = startY
        pickNewWanderTarget(cellSize)
        endX = wanderTargetX
        endY = wanderTargetY
    }

    private fun pickNewWanderTarget(cellSize: Double) {
        val wanderRadius = cellSize * GameConfig.MONKEY_WANDER_RADIUS_FACTOR
        val dx = Random.nextDouble(-wanderRadius, wanderRadius)
        val dy = Random.nextDouble(-wanderRadius, wanderRadius)

        wanderTargetX = (startX + dx).coerceAtLeast(0.0)
        wanderTargetY = (startY + dy).coerceAtLeast(0.0)
        startX = endX
        startY = endY
        progress = 0.0
    }

    private fun startChatting() {
        state = State.CHATTING
        behaviorTimer = 0.0
    }

    private fun startDancing() {
        state = State.DANCING
        behaviorTimer = 0.0
    }

    fun isIdle(): Boolean = (state == State.IDLE && task == null)

    fun getDrawPosition(): Pair<Double, Double> {
        val x = lerp(startX, endX, progress)
        val y = lerp(startY, endY, progress)
        return x to y
    }

    fun getCarriedFruit(): Fruit? = fruitBeingCarried

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    fun draw(gc: GraphicsContext, cellSize: Double) {
        val (x, y) = getDrawPosition()
        val fruit = getCarriedFruit()

        gc.font = emojiCompatibleFont(cellSize * 0.75)

        if (fruit != null) {
            gc.fill = Color.OLIVEDRAB
            gc.fillText(fruit.emoji, x + 2, y - 2)
        }

        gc.fill = Color.CHOCOLATE
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)

        gc.font = emojiCompatibleFont(10.0)
        when (state) {
            State.CHATTING -> gc.fillText("💬", x + (cellSize / 2), y + cellSize * 0.1)
            State.DANCING -> gc.fillText("🎶", x + (cellSize / 2), y + cellSize * 0.1)
            State.WANDERING -> gc.fillText("🗺️", x + (cellSize / 2), y + cellSize * 0.6)
            else -> {}
        }

        gc.fillText(
            when (algorithm) {
                SortAlgorithm.BOGO -> "Bogo"
                SortAlgorithm.BUBBLE -> "Bubble"
            },
            x + 5,
            y + cellSize * 0.55 - 15
        )
    }

    private fun emojiCompatibleFont(size: Double): Font {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("win")) Font.font("Segoe UI Emoji", size) else Font.font(size)
    }
}

// --- Game Controller ---

class GameController(val rows: Int = GameConfig.ROWS, val cols: Int = GameConfig.COLS) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey())
    val particleSystem = ParticleSystem()
    private var lastTickTime = System.nanoTime()

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
            monkey.update(gridModel, cellSize = GameConfig.CELL_SIZE, particleSystem)
        }

        particleSystem.update(deltaMs)
    }

    private fun assignBubbleTask(monkey: Monkey) {
        val totalCells = rows * cols

        while (true) {
            if (bubbleSortIndex >= totalCells - 1 - bubbleSortPass) {
                bubbleSortIndex = 0
                bubbleSortPass++
                if (bubbleSortPass >= totalCells - 1) {
                    bubbleSortPass = 0
                }
                return
            }

            val indexA = bubbleSortIndex
            val indexB = bubbleSortIndex + 1

            val from = Pos(indexA / cols, indexA % cols)
            val to = Pos(indexB / cols, indexB % cols)

            val fruitA = gridModel.get(from)
            val fruitB = gridModel.get(to)

            bubbleSortIndex++

            if (fruitA.name > fruitB.name) {
                monkey.assignTask(ShuffleTask(from, to, fruitA), cellSize = GameConfig.CELL_SIZE)
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
        monkey.assignTask(ShuffleTask(from, to, fruit), cellSize = GameConfig.CELL_SIZE)
    }

    fun buyMonkey(): Boolean {
        val cost = GameConfig.MONKEY_BASE_COST * monkeys.size
        if (GameStats.coins >= cost) {
            GameStats.coins -= cost
            monkeys.add(Monkey())
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

    private val buyButton = Button("Buy Monkey").apply {
        setOnAction {
            if (!controller.buyMonkey()) println("Not enough coins!")
        }
    }

    private val upgradeButton = Button("Upgrade to BubbleSort (${GameConfig.MONKEY_UPGRADE_COST} coins)").apply {
        setOnAction {
            if (!controller.upgradeMonkey()) println("Not enough coins or no monkeys to upgrade!")
        }
    }

    private val debugBogoButton = Button("Debug: Set All to BogoSort").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BOGO }
            println("All monkeys set to BogoSort")
        }
    }

    private val debugBubbleButton = Button("Debug: Set All to BubbleSort").apply {
        setOnAction {
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }
            println("All monkeys set to BubbleSort")
        }
    }

    private val debugSpawnButton = Button("Debug: Spawn 50 Monkeys").apply {
        setOnAction {
            repeat(50) {
                controller.monkeys.add(Monkey().apply { algorithm = SortAlgorithm.BOGO })
            }
            println("Spawned 50 new monkeys")
        }
    }

    private val debugSpeedButton = Button("Debug: Speed x15").apply {
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 15.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    private fun emojiCompatibleFont(size: Double): Font {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("win")) Font.font("Segoe UI Emoji", size) else Font.font(size)
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
        gc.font = emojiCompatibleFont(cellSize * 0.75)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.fillText(fruit.emoji, x + 4, y + cellSize * 0.8)
            }
        }

        for (monkey in controller.monkeys) {
            monkey.draw(gc, cellSize)
        }

        controller.particleSystem.render(gc, cellSize)

        gc.fill = Color.DARKGREEN
        gc.font = emojiCompatibleFont(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 5)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 5)
        gc.fillText("Bubble Monkeys: ${controller.monkeys.count { it.algorithm == SortAlgorithm.BUBBLE }}", 250.0, gc.canvas.height - 5)

        buyButton.isDisable = GameStats.coins < GameConfig.MONKEY_BASE_COST * controller.monkeys.size
        buyButton.text = "Buy Monkey (${GameConfig.MONKEY_BASE_COST * controller.monkeys.size} coins)"
        upgradeButton.isDisable = GameStats.coins < GameConfig.MONKEY_UPGRADE_COST ||
                controller.monkeys.none { it.algorithm == SortAlgorithm.BOGO }

        if (controller.gridModel.isSorted()) {
            gc.fill = Color.DODGERBLUE
            gc.fillRoundRect(gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 50, 300.0, 75.0, 20.0, 20.0)
            gc.fill = Color.ORANGE
            gc.font = emojiCompatibleFont(24.0)
            gc.fillText("🎉 MONKEYSORT FINISHED! 🎉", gc.canvas.width / 2 - 150, gc.canvas.height / 2 - 20)
            controller.particleSystem.add(ConfettiEffect(gc.canvas.width, gc.canvas.height, 1000L))
        }
    }
}

fun main() {
    Application.launch(MonkeySortSimulatorApp::class.java)
}