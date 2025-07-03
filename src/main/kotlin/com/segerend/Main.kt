package com.segerend

import com.segerend.monkey.*
import com.segerend.sorting.*
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.geometry.VPos
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import kotlin.math.roundToInt

class MonkeySortSimulatorApp : Application() {
    private val rows = GameConfig.ROWS
    private val cols = GameConfig.COLS
    private val cellSize = GameConfig.CELL_SIZE
    val controller = GameController(rows, cols)
    private val sortStrip = SortStrip()

    internal val root = BorderPane()

    private val fruitImages: Map<Fruit, Image> by lazy {
        Fruit.values().associateWith { fruit ->
//            val emojiSize : Double = (cellSize * 1.5).toInt().toDouble()
            val emojiSize = 32.0
            println("Generating image for fruit: ${fruit.name} with size $emojiSize")
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

    private val pauseButton = Button("Pause").apply {
        id = "pauseButton"
        setOnAction {
            GameStats.timeFactor = if (GameStats.timeFactor == 0.0) 1.0 else 0.0
        }
    }

    private val completedImage: Image by lazy {
        val stream = javaClass.getResourceAsStream("/completed.png")
            ?: error("Image resource not found: completed.png")
        Image(stream)
    }

    override fun start(primaryStage: Stage) {
        val canvas = Canvas(cols * cellSize, rows * cellSize + GameConfig.STRIP_HEIGHT + 30)
        val gc = canvas.graphicsContext2D

        root.bottom = FlowPane().apply {
            children.addAll(
                buyButton,
                upgradeButton,
                debugBogoButton,
                debugBubbleButton,
                debugInsertionButton,
                debugSpawnButton,
                debugSpeedButton,
                debugSuperSpeedButton,
                chartButton,
                pauseButton
            )
        }
        root.center = canvas

        val scene = Scene(root)
        primaryStage.title = "Monkeysort 🐒"
        primaryStage.scene = scene

        // Exit the application when the main window is closed
        primaryStage.setOnCloseRequest {
            javafx.application.Platform.exit()
        }

        primaryStage.show()

        primaryStage.minWidth = primaryStage.width
        primaryStage.minHeight = primaryStage.height

        // Maybe for later, if we want to resize the canvas with the window
//        primaryStage.widthProperty().addListener { _, _, newWidth ->
//            canvas.width = newWidth.toDouble()
//            canvas.height = (rows * cellSize + GameConfig.STRIP_HEIGHT + 30).toDouble()
//        }

        object : AnimationTimer() {
            private var lastUpdate = System.nanoTime()
            private var lastRender = System.nanoTime()

            private var targetFPS = GameConfig.fps

            private var nsPerUpdate = 1_000_000_000L / targetFPS

            private var accumulator = 0L

            override fun handle(now: Long) {
                if (GameStats.timeFactor == 0.0 || GameConfig.fps == 0) {
                    // If paused, skip update and render
                    return
                }
                else if (targetFPS != GameConfig.fps) {
                    // Update target FPS if it has changed
                    targetFPS = GameConfig.fps
                    nsPerUpdate = 1_000_000_000L / targetFPS
                }

                val delta = now - lastUpdate
                lastUpdate = now
                accumulator += delta

                // Fixed timestep updates (catch up if lagging)
                while (accumulator >= nsPerUpdate) {
                    val deltaTimeSeconds = nsPerUpdate / 1_000_000_000.0
                    controller.tick(FrameTime(deltaTimeSeconds * 1000, now / 1_000_000_000.0))
                    accumulator -= nsPerUpdate
                }

                // Cap rendering to fps
                if (now - lastRender >= nsPerUpdate) {
                    // Optionally pass interpolation factor: accumulator / nsPerUpdate * 1000
                    draw(gc, FrameTime(accumulator / 1_000_000.0, now / 1_000_000_000.0))
                    lastRender = now
                }
            }
        }.start()
    }

    private var lastRenderTime = System.nanoTime()

    private fun draw(gc: GraphicsContext, frameTime: FrameTime) {
        val now = System.nanoTime()
        val deltaRenderNs = now - lastRenderTime
        lastRenderTime = now

        gc.fill = Color.BEIGE
        gc.fillRect(0.0, 0.0, gc.canvas.width, gc.canvas.height)

        gc.fill = Color.OLIVEDRAB
        gc.font = Utils.emojiCompatibleFont(cellSize * 0.75)

        val grid = controller.gridModel.getGridCopy()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val fruit = grid[r][c]
                val x = c * cellSize
                val y = r * cellSize
                gc.drawImage(fruitImages[fruit], x, y, cellSize, cellSize)
//                gc.fillText(fruit.emoji, x + 2, y + cellSize * 0.55)
            }
        }

        for (monkey in controller.monkeys) monkey.draw(gc, cellSize)
        controller.particleSystem.render(gc, cellSize)

        gc.fill = Color.DARKGREEN
        gc.font = Utils.emojiCompatibleFont(16.0)
        gc.fillText("Coins: ${GameStats.coins}", 10.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        gc.fillText("Monkeys: ${controller.monkeys.size}", 120.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)
        gc.fillText("Bubble Monkeys: ${controller.monkeys.count { it.algorithm == SortAlgorithm.BUBBLE }}", 250.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)

        val fps = (1_000_000_000.0 / deltaRenderNs).roundToInt()
        gc.fillText("FPS: $fps", 400.0, gc.canvas.height - 10 - GameConfig.STRIP_HEIGHT)

        sortStrip.draw(gc, controller.gridModel)

        buyButton.isDisable = GameStats.coins < controller.getNewMonkeyPrice()
        buyButton.text = "Buy Monkey (${controller.getNewMonkeyPrice()} coins)"
        upgradeButton.isDisable = GameStats.coins < GameConfig.MONKEY_UPGRADE_COST || controller.monkeys.none { it.algorithm == SortAlgorithm.BOGO }

        if (controller.gridModel.isSorted()) {
            val img = completedImage
            val wave = kotlin.math.sin(frameTime.currentTimeSec * 2 * Math.PI * 0.5)
            val scale = 1.0 + 0.01 * wave
            val offsetY = 3.0 * wave

            val width = img.width * scale
            val height = img.height * scale
            val x = (gc.canvas.width - width) / 2
            val y = (gc.canvas.height - height) / 2 - 20 + offsetY

            gc.drawImage(img, x, y, width, height)

            GameStats.timeFactor = 0.1
            GameConfig.fps = 24
        } else if (GameConfig.fps != GameConfig.MAX_FPS) {
            GameConfig.fps = GameConfig.MAX_FPS // Reset to max FPS when not sorted
            GameStats.timeFactor = 1.0 // Reset time factor to normal speed
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(MonkeySortSimulatorApp::class.java, *args)
}