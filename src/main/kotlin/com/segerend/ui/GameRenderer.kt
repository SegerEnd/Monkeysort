package com.segerend.ui

import com.segerend.*
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.roundToInt
import com.segerend.sorting.*
import javafx.geometry.VPos
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.text.TextAlignment

class GameRenderer(
    private val gc: GraphicsContext,
    private val controller: GameController
) {
    private val cellSize = GameConfig.CELL_SIZE
    private val rows = GameConfig.ROWS
    private val cols = GameConfig.COLS
    private val fruitImages: Map<Fruit, Image> by lazy {
        Fruit.values().associateWith { fruit ->
            val emojiSize = 32.0
            val canvas = Canvas(emojiSize, emojiSize)
            val gc = canvas.graphicsContext2D

            gc.clearRect(0.0, 0.0, emojiSize, emojiSize)

            if (fruit != Fruit.EMPTY) {
                gc.fill = Color.OLIVEDRAB
                gc.font = Utils.emojiCompatibleFont(emojiSize * 0.9)

                // Align center both horizontally and vertically
                gc.textAlign = TextAlignment.CENTER
                gc.textBaseline = VPos.CENTER

                val centerX = emojiSize / 2
                val centerY = emojiSize / 2

                gc.fillText(fruit.emoji, centerX, centerY)
            }

            val params = SnapshotParameters().apply {
                fill = Color.TRANSPARENT
            }

            canvas.snapshot(params, WritableImage(emojiSize.toInt(), emojiSize.toInt()))
        }
    }
    private val sortStrip = SortStrip()
    private val completedImage by lazy {
        val stream = javaClass.getResourceAsStream("/completed.png")
            ?: error("Image resource not found: completed.png")
        javafx.scene.image.Image(stream)
    }

    private var lastRenderTime = System.nanoTime()

    fun draw(frameTime: FrameTime) {
        val now = System.nanoTime()
        val deltaRenderNs = now - lastRenderTime
        lastRenderTime = now

        clearCanvas()

        drawGrid()
        drawMonkeys()
        drawStats(deltaRenderNs)
        drawOverlayIfSorted(frameTime)

        sortStrip.draw(gc, controller.gridModel)
    }

    private fun clearCanvas() {
        gc.fill = Color.BEIGE
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)
    }

    private fun drawGrid() {
        val grid = controller.gridModel.getGridCopy()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.drawImage(fruitImages[fruit], x, y, cellSize, cellSize)
            }
        }
    }

    private fun drawMonkeys() {
        for (monkey in controller.monkeys) {
            monkey.draw(gc, cellSize)
        }
        controller.particleSystem.render(gc, cellSize)
    }

    private fun drawStats(deltaRenderNs: Long) {
        gc.fill = Color.DARKGREEN
        gc.font = Utils.emojiCompatibleFont(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        gc.fillText("Bubble Monkeys: ${controller.monkeys.count { it.algorithm == SortAlgorithm.BUBBLE }}", 250.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        val fps = (1_000_000_000.0 / deltaRenderNs).roundToInt()
        gc.fillText("FPS: $fps", 400.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        gc.fillText("Speed: x${GameStats.timeFactor}", 475.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
    }

    private fun drawOverlayIfSorted(frameTime: FrameTime) {
        if (!controller.gridModel.isSorted()) {
            if (GameConfig.fps != GameConfig.MAX_FPS) {
                GameConfig.fps = GameConfig.MAX_FPS
                GameStats.timeFactor = 1.0 // Reset time factor to normal speed
            }
            return
        }

        val wave = kotlin.math.sin(frameTime.currentTimeSec * 2 * Math.PI * 0.5)
        val scale = 1.0 + 0.01 * wave
        val offsetY = 3.0 * wave

        val width = completedImage.width * scale
        val height = completedImage.height * scale
        val x = (gc.canvas.width - width) / 2
        val y = (gc.canvas.height - height) / 2 - 20 + offsetY

        gc.drawImage(completedImage, x, y, width, height)

        GameStats.timeFactor = 1.0
        GameConfig.fps = 16
    }
}
