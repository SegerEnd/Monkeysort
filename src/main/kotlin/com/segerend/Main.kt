package com.segerend

import com.segerend.ui.Buttons
import com.segerend.ui.GameRenderer
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

class MonkeySortSimulatorApp : Application() {

    val controller = GameController(GameConfig.ROWS, GameConfig.COLS)
    private val canvas = Canvas(
        GameConfig.COLS * GameConfig.CELL_SIZE,
        GameConfig.ROWS * GameConfig.CELL_SIZE + GameConfig.STRIP_HEIGHT + 30
    )
    private val root = BorderPane()
    private val renderer = GameRenderer(canvas.graphicsContext2D, controller)
    private var isMinimizedOrHidden = false

    override fun start(primaryStage: Stage) {
        root.center = canvas
        root.bottom = Buttons.createButtonPanel(controller)

        primaryStage.title = "Monkeysort 🐒"
        val scene = Scene(root)
        primaryStage.scene = scene
        primaryStage.setOnCloseRequest { javafx.application.Platform.exit() }
        primaryStage.show()

        primaryStage.minWidth = primaryStage.width
        primaryStage.minHeight = primaryStage.height

        primaryStage.iconifiedProperty().addListener { _, _, minimized -> isMinimizedOrHidden = minimized }
        primaryStage.showingProperty().addListener { _, _, showing -> isMinimizedOrHidden = !showing }

        GameLoop().start()
    }

    private inner class GameLoop : AnimationTimer() {
        private var lastUpdate = System.nanoTime()
        private var lastRender = System.nanoTime()
        private var targetFPS = GameConfig.fps
        private var nsPerUpdate = 1_000_000_000L / targetFPS
        private var accumulator = 0L

        override fun handle(now: Long) {
            if (GameStats.timeFactor == 0.0) return

            val desiredFPS = if (isMinimizedOrHidden) 1 else GameConfig.fps
            if (desiredFPS != targetFPS) {
                targetFPS = desiredFPS
                nsPerUpdate = 1_000_000_000L / targetFPS
            }

            val delta = now - lastUpdate
            lastUpdate = now
            accumulator += delta

            while (accumulator >= nsPerUpdate) {
                controller.tick(FrameTime(nsPerUpdate / 1_000_000.0, now / 1_000_000_000.0))
                accumulator -= nsPerUpdate
            }

            if (now - lastRender >= nsPerUpdate) {
                renderer.draw(FrameTime(accumulator / 1_000_000.0, now / 1_000_000_000.0))
                Buttons.updateButtons(controller)
                lastRender = now
            }
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(MonkeySortSimulatorApp::class.java, *args)
}
