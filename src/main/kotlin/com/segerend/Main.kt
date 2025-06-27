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
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.RadialGradient
import javafx.scene.text.Font
import javafx.stage.Stage
import kotlin.random.Random

// --- Constants ---
object GameConfig {
    const val ROWS = 25
    const val COLS = 25
    const val CELL_SIZE = 24.0
    const val MONKEY_BASE_COST = 75
    const val MONKEY_UPGRADE_COST = 500
    const val MONKEY_WANDER_RADIUS_FACTOR = 4.0
    const val COMBO_REWARD_MULTIPLIER = 15
    const val MAX_FPS = 60
    const val STRIP_HEIGHT = 30.0
}

// --- Core Game Models ---

enum class Fruit(val emoji: String, val color: Color) {
    APPLE("🍎", Color.RED), BANANA("🍌", Color.YELLOW), GRAPE("🍇", Color.PURPLE),
    ORANGE("🍊", Color.ORANGE), WATERMELON("🍉", Color.GREEN),
    PINEAPPLE("🍍", Color.YELLOW), STRAWBERRY("🍓", Color.RED),
    CHERRY("🍒", Color.RED), KIWI("🥝", Color.GREEN), PEACH("🍑", Color.PEACHPUFF),
    MANGO("🥭", Color.ORANGE), BLUEBERRY("🫐", Color.BLUE), LEMON("🍋", Color.YELLOW),
    LETTUCE("🥬", Color.GREEN), EMPTY(" ", Color.TRANSPARENT);

    companion object {
        fun random(): Fruit = values().filter { it != EMPTY }.random()
    }
}

data class Pos(val row: Int, val col: Int)

data class ShuffleTask(val from: Pos, val to: Pos, val fruit: Fruit)

enum class SortAlgorithm { BOGO, BUBBLE }

class GridModel(val rows: Int, val cols: Int) {
    private val grid = Array(rows) { Array(cols) { Fruit.random() } }

    fun getGridCopy(): Array<Array<Fruit>> = Array(rows) { r -> grid[r].clone() }
    fun isSorted(): Boolean = grid.flatten().zipWithNext().all { it.first.name <= it.second.name }
    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]
    fun set(pos: Pos, fruit: Fruit) { grid[pos.row][pos.col] = fruit }

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
}

// --- Monkey States ---

interface MonkeyState {
    fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem)
    fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double)
    fun getDrawPosition(): Pair<Double, Double>
}

abstract class ProgressState : MonkeyState {
    protected var progress = 0.0
    var speedPerTick = 0.03
    protected var startX = 0.0
    protected var startY = 0.0
    protected var endX = 0.0
    protected var endY = 0.0

    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        progress += speedPerTick * GameStats.timeFactor
        if (progress >= 1.0) {
            progress = 0.0
            onProgressComplete(monkey, grid, cellSize, particleSystem)
        }
    }

    abstract fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem)

    override fun getDrawPosition(): Pair<Double, Double> = lerp(startX, endX, progress) to lerp(startY, endY, progress)
    protected fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
}

class IdleState(private val x: Double, private val y: Double) : MonkeyState {
    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        when ((1..4).random()) {
            1 -> monkey.state = WanderingState(x, y, cellSize)
            2 -> monkey.state = ChattingState(x, y)
            3 -> monkey.state = DancingState(x, y)
        }
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText("💤", x + cellSize / 2, y + cellSize * 0.1)
    }

    override fun getDrawPosition(): Pair<Double, Double> = x to y
}

class MovingToSourceState(private val task: ShuffleTask, private val cellSize: Double, startX: Double, startY: Double) : ProgressState() {
    init {
        this.startX = startX
        this.startY = startY
        this.endX = task.from.col * cellSize
        this.endY = task.from.row * cellSize
    }

    override fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        val fruit = grid.get(task.from)
        grid.set(task.from, Fruit.EMPTY)
        monkey.fruitBeingCarried = fruit
        monkey.state = CarryingState(task, cellSize, endX, endY)
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
    }
}

class CarryingState(private val task: ShuffleTask, private val cellSize: Double, startX: Double, startY: Double) : ProgressState() {
    init {
        this.startX = startX
        this.startY = startY
        this.endX = task.to.col * cellSize
        this.endY = task.to.row * cellSize
    }

    override fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        val oldFruit = grid.get(task.to)
        grid.set(task.to, monkey.fruitBeingCarried!!)
        monkey.fruitBeingCarried = oldFruit
        monkey.state = ReturningState(task, cellSize, endX, endY)
        val comboCount = grid.getComboCellsAt(task.to).size
        if (comboCount >= 2) {
            GameStats.coins += comboCount * GameConfig.COMBO_REWARD_MULTIPLIER
            val comboPositions = grid.getComboCellsAt(task.to)
            if (comboPositions.isNotEmpty()) particleSystem.add(ComboParticleEffect(comboPositions, cellSize))
        }
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        gc.fill = Color.OLIVEDRAB
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText(monkey.fruitBeingCarried!!.emoji, x + 2, y - 2)
        gc.fill = Color.CHOCOLATE
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
    }
}

class ReturningState(private val task: ShuffleTask, private val cellSize: Double, startX: Double, startY: Double) : ProgressState() {
    init {
        this.startX = startX
        this.startY = startY
        this.endX = task.from.col * cellSize
        this.endY = task.from.row * cellSize
    }

    override fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        grid.set(task.from, monkey.fruitBeingCarried!!)
        monkey.fruitBeingCarried = null
        LockManager.unlock(task.from, task.to)
        monkey.state = IdleState(endX, endY)
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        gc.fill = Color.OLIVEDRAB
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText(monkey.fruitBeingCarried!!.emoji, x + 2, y - 2)
        gc.fill = Color.CHOCOLATE
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
    }
}

class WanderingState(startX: Double, startY: Double, cellSize: Double) : ProgressState() {
    init {
        this.startX = startX
        this.startY = startY
        pickNewTarget(cellSize)
    }

    private fun pickNewTarget(cellSize: Double) {
        val wanderRadius = GameConfig.MONKEY_WANDER_RADIUS_FACTOR * cellSize
        val targetCol = ((startX + wanderRadius * (Random.nextDouble() - 0.5)) / cellSize).toInt()
        val targetRow = ((startY + wanderRadius * (Random.nextDouble() - 0.5)) / cellSize).toInt()
        endX = (targetCol * cellSize).coerceIn(0.0, (GameConfig.COLS - 1) * cellSize)
        endY = (targetRow * cellSize).coerceIn(0.0, (GameConfig.ROWS - 1) * cellSize)
    }

    override fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        if ((1..5).random() == 1) monkey.state = IdleState(endX, endY)
        else {
            startX = endX
            startY = endY
            pickNewTarget(cellSize)
        }
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText("🗺️", x + cellSize / 2, y + cellSize * 0.6)
    }
}

class ChattingState(private val x: Double, private val y: Double) : MonkeyState {
    private var timer = 0.0
    private val duration = 100.0

    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        timer += GameStats.timeFactor
        if (timer >= duration) monkey.state = IdleState(x, y)
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText("💬", x + cellSize / 2, y + cellSize * 0.1)
    }

    override fun getDrawPosition(): Pair<Double, Double> = x to y
}

class DancingState(private val x: Double, private val y: Double) : MonkeyState {
    private var timer = 0.0
    private val duration = 100.0

    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        timer += GameStats.timeFactor
        if (timer >= duration) monkey.state = IdleState(x, y)
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        gc.fill = Color.CHOCOLATE
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText("🎶", x + cellSize / 2, y + cellSize * 0.1)
    }

    override fun getDrawPosition(): Pair<Double, Double> = x to y
}

// --- Sorting Strategies ---

interface SortStrategy {
    fun getNextTask(grid: GridModel): ShuffleTask?
}

class BogoSortStrategy : SortStrategy {
    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val from = Pos(Random.nextInt(grid.rows), Random.nextInt(grid.cols))
        var to: Pos
        do { to = Pos(Random.nextInt(grid.rows), Random.nextInt(grid.cols)) } while (from == to)
        return ShuffleTask(from, to, grid.get(from))
    }
}

class BubbleSortStrategy(val rows: Int, val cols: Int) : SortStrategy {
    private var bubbleSortIndex = 0
    private var bubbleSortPass = 0

    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val totalCells = rows * cols
        while (bubbleSortIndex < totalCells - 1 - bubbleSortPass) {
            val indexA = bubbleSortIndex
            val indexB = indexA + 1
            val from = Pos(indexA / cols, indexA % cols)
            val to = Pos(indexB / cols, indexB % cols)
            bubbleSortIndex++
            if (grid.get(from).name > grid.get(to).name) return ShuffleTask(from, to, grid.get(from))
        }
        bubbleSortIndex = 0
        bubbleSortPass++
        if (bubbleSortPass >= totalCells - 1) bubbleSortPass = 0
        return null
    }
}

// --- Monkey Class ---

class Monkey(var algorithm: SortAlgorithm) {
    var state: MonkeyState = IdleState(Random.nextDouble() * GameConfig.COLS * GameConfig.CELL_SIZE, Random.nextDouble() * GameConfig.ROWS * GameConfig.CELL_SIZE)
    var fruitBeingCarried: Fruit? = null

    init {
        updateSpeed()
    }

    private fun updateSpeed() {
        (state as? ProgressState)?.speedPerTick = when (algorithm) {
            SortAlgorithm.BOGO -> 0.03
            SortAlgorithm.BUBBLE -> 0.07
        }
    }

    fun assignTask(task: ShuffleTask, cellSize: Double): Boolean {
        if (!LockManager.tryLock(task.from, task.to)) return false
        val (currentX, currentY) = state.getDrawPosition()
        state = MovingToSourceState(task, cellSize, currentX, currentY)
        return true
    }

    fun update(grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        state.update(this, grid, cellSize, particleSystem)
    }

    fun draw(gc: GraphicsContext, cellSize: Double) {
        state.draw(gc, this, cellSize)
        val (x, y) = state.getDrawPosition()
        gc.font = Utils.emojiCompatibleFont(10.0)
        gc.fillText(when (algorithm) { SortAlgorithm.BOGO -> "Bogo"; SortAlgorithm.BUBBLE -> "Bubble" }, x + 5, y + cellSize * 0.55 - 15)
    }

    fun isIdle(): Boolean = state is IdleState
}

// --- Game Controller ---

class GameController(val rows: Int = GameConfig.ROWS, val cols: Int = GameConfig.COLS) {
    val gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(SortAlgorithm.BOGO))
    val particleSystem = ParticleSystem()
    private val strategies = mapOf(
        SortAlgorithm.BOGO to BogoSortStrategy(),
        SortAlgorithm.BUBBLE to BubbleSortStrategy(rows, cols)
    )
    private var lastTickTime = System.nanoTime()

    fun tick() {
        val now = System.nanoTime()
        val deltaMs = (now - lastTickTime) / 1_000_000
        lastTickTime = now

        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                val strategy = strategies[monkey.algorithm]
                val task = strategy?.getNextTask(gridModel)
                if (task != null) monkey.assignTask(task, GameConfig.CELL_SIZE)
            }
            monkey.update(gridModel, GameConfig.CELL_SIZE, particleSystem)
        }
        particleSystem.update(deltaMs)
    }

    fun buyMonkey(): Boolean {
        val cost = (GameConfig.MONKEY_BASE_COST * monkeys.size * 1.1).toInt()
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

    private val debugSpawnButton = Button("Debug: Spawn 50 Monkeys").apply {
        setOnAction {
            repeat(50) { controller.monkeys.add(Monkey(SortAlgorithm.BOGO)) }
            println("Spawned 50 new monkeys")
        }
    }

    private val debugSpeedButton = Button("Debug: Speed x15").apply {
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 15.0 else 1.0
            println("Game speed toggled to x${GameStats.timeFactor}")
        }
    }

    // debug button to complete the sorting based on fruit name alphabetically immediately
    private val debugCompleteButton = Button("Debug: Complete Sorting").apply {
        setOnAction {
            val sortedFruits = controller.gridModel.getGridCopy().flatten().sortedBy { it.name }
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    controller.gridModel.set(Pos(r, c), sortedFruits[r * cols + c])
                }
            }
            // set all monkeys to BubbleSort for visual consistency
            controller.monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }

            println("Grid sorted alphabetically")
        }
    }

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val canvas = Canvas(cols * cellSize, rows * cellSize + 30)
        val gc = canvas.graphicsContext2D

        root.bottom = HBox(10.0, buyButton, upgradeButton, debugBogoButton, debugBubbleButton, debugSpawnButton, debugSpeedButton, debugCompleteButton)
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

    fun drawSortStrip(gc: GraphicsContext, grid: GridModel) {
        val stripWidth = gc.canvas.width
        val stripHeight = GameConfig.STRIP_HEIGHT

        val fruits = Fruit.values().filter { it != Fruit.EMPTY }
        val count = fruits.size
        val spacing = 2.0
        val totalSpacing = spacing * (count - 1)
        val squareWidth = (stripWidth - totalSpacing) / count
        val squareHeight = stripHeight

        val fontSize = 18.0
        gc.font = Font.font(fontSize)

        for ((index, fruit) in fruits.withIndex()) {
            val maxCount = grid.getSameFruitCount(fruit)
            val neighborCount = grid.getSameFruitNeighborCount(fruit)
            val grayScaleFactor = if (maxCount > 0) neighborCount.toDouble() / maxCount else 0.0

            val x = index * (squareWidth + spacing)
            val y = gc.canvas.height - stripHeight

            fun blendColors(c1: Color, c2: Color, t: Double): Color {
                val r = c1.red * (1 - t) + c2.red * t
                val g = c1.green * (1 - t) + c2.green * t
                val b = c1.blue * (1 - t) + c2.blue * t
                return Color.color(r, g, b)
            }

            val centerColor = blendColors(Color.WHITE, fruit.color, grayScaleFactor)
            // edge color is a little bit more saturated than the center color
            val edgeColor = blendColors(Color.gray(0.8), fruit.color, grayScaleFactor * 0.8)

            val gradient = RadialGradient(
                0.0, 0.0,
                x + squareWidth / 2, y + squareHeight / 2,
                squareWidth / 1.5,
                false, CycleMethod.NO_CYCLE,
                javafx.scene.paint.Stop(0.0, centerColor),
                javafx.scene.paint.Stop(1.0, edgeColor)
            )

            gc.fill = gradient
            gc.fillRect(x, y, squareWidth, squareHeight)

            // Use Text node to measure emoji width
            val emojiText = javafx.scene.text.Text(fruit.emoji)
            emojiText.font = gc.font
            val textWidth = emojiText.layoutBounds.width
            val textHeight = emojiText.layoutBounds.height

            val textX = x + (squareWidth - textWidth) / 2
            val textY = y + (squareHeight + textHeight) / 2 - 4 // fine-tuned vertical centering

            gc.fill = centerColor
            gc.fillText(fruit.emoji, textX, textY)

            if (maxCount == neighborCount) {
                gc.stroke = Color.BLACK
                gc.lineWidth = 2.0
                gc.strokeRect(x, y, squareWidth, squareHeight)
                gc.fill = Color.LIMEGREEN
                gc.fillText("✅", x + squareWidth - 20, y + squareHeight - 5)
            }
        }
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

        drawSortStrip(gc, controller.gridModel)

        buyButton.isDisable = GameStats.coins < GameConfig.MONKEY_BASE_COST * controller.monkeys.size
        buyButton.text = "Buy Monkey (${GameConfig.MONKEY_BASE_COST * controller.monkeys.size} coins)"
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