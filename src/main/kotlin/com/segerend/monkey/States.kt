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
    internal var progress = 0.0
    protected var startX = 0.0
    protected var startY = 0.0
    protected var endX = 0.0
    protected var endY = 0.0

    // Progress is now handled by Monkey, so update does nothing
    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        // Empty, as progress is managed by Monkey
    }

    abstract fun onProgressComplete(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem)

    override fun getDrawPosition(): Pair<Double, Double> = lerp(startX, endX, progress) to lerp(startY, endY, progress)
    protected fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
}

class IdleState(private val x: Double, private val y: Double) : MonkeyState {
    override fun update(monkey: Monkey, grid: GridModel, cellSize: Double, particleSystem: ParticleSystem) {
        when ((1..5).random()) {
            1 -> monkey.state = WanderingState(x, y, cellSize)
            2 -> monkey.state = ChattingState(x, y)
            3 -> monkey.state = DancingState(x, y)
        }
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        monkey.drawWithOverlay(gc, x, y, cellSize, "💤")
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
        monkey.drawBase(gc, x, y, cellSize)
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
        val comboPositions = grid.getComboCellsAt(task.to)
        val comboCount = comboPositions.size
        if (comboCount >= 2) {
            GameStats.coins += comboCount * GameConfig.COMBO_REWARD_MULTIPLIER
            if (comboPositions.isNotEmpty()) particleSystem.add(ComboParticleEffect(comboPositions, cellSize))
        }
    }

    override fun draw(gc: GraphicsContext, monkey: Monkey, cellSize: Double) {
        val (x, y) = getDrawPosition()
        monkey.drawWithCarriedFruit(gc, x, y, cellSize)
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
        monkey.drawWithCarriedFruit(gc, x, y, cellSize)
    }
}

class WanderingState(startX: Double, startY: Double, cellSize: Double) : ProgressState() {
    init {
        this.startX = startX
        this.startY = startY
        pickNewTarget(cellSize)
    }

    private fun pickNewTarget(cellSize: Double = GameConfig.CELL_SIZE) {
        val wanderRadius = GameConfig.MONKEY_WANDER_RADIUS_FACTOR * cellSize
        endX = startX + Random.nextDouble(-wanderRadius, wanderRadius)
        endY = startY + Random.nextDouble(-wanderRadius, wanderRadius)
        // Ensure the target is within bounds
        endX = endX.coerceIn(0.0, GameConfig.COLS * cellSize - cellSize)
        endY = endY.coerceIn(0.0, GameConfig.ROWS * cellSize - cellSize)
        progress = 0.0
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
        monkey.drawWithOverlay(gc, x, y, cellSize, "🗺️", cellSize * 0.6)
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
        val (x, y) = getDrawPosition()
        monkey.drawWithOverlay(gc, x, y, cellSize, "💬")
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
        val (x, y) = getDrawPosition()
        monkey.drawWithOverlay(gc, x, y, cellSize, "🎶")
    }

    override fun getDrawPosition(): Pair<Double, Double> = x to y
}