package com.segerend.monkey

import com.segerend.Fruit
import com.segerend.GameConfig
import com.segerend.GridModel
import com.segerend.LockManager
import com.segerend.Utils
import com.segerend.particles.ParticleSystem
import com.segerend.sorting.SortAlgorithm
import com.segerend.sorting.SortStrategy
import com.segerend.sorting.makeStrategy
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.random.Random

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
        SortAlgorithm.BUBBLE -> 25.0
        SortAlgorithm.INSERTION -> 25.0
        else -> 1.0
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