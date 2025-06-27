package com.segerend.monkey

import com.segerend.Fruit
import com.segerend.GameConfig
import com.segerend.GameStats
import com.segerend.GridModel
import com.segerend.LockManager
import com.segerend.Monkey
import com.segerend.ShuffleTask
import com.segerend.Utils
import com.segerend.particles.*
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.random.Random

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
        gc.fillText("🐒", x + 2, y + cellSize * 0.55)
        gc.font = Utils.emojiCompatibleFont(10.0)
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