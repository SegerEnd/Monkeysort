package com.segerend

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class MainApp : Application() {
    override fun start(primaryStage: Stage) {
        val root = BorderPane()

        val canvasWithLabel = ScreenCanvas()

        root.center = canvasWithLabel

        primaryStage.title = "Sorteer app"
        primaryStage.scene = createStyledScene(root, 600.0, 400.0)
        primaryStage.show()
    }

    private fun createStyledScene(root: BorderPane, width: Double, height: Double): Scene {
        val scene = Scene(root, width, height)
        scene.stylesheets.add(this::class.java.getResource("/style.css").toExternalForm())
        return scene
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
