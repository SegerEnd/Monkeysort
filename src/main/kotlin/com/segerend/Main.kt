package com.segerend

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class MainApp : Application() {
    override fun start(primaryStage: Stage) {
        val root = BorderPane()

        // z stack alternative
        val rootPane = StackPane()

        val canvasWithLabel = ScreenCanvas()

        rootPane.children.add(canvasWithLabel)

        root.center = rootPane

        primaryStage.title = "Sorteer app"
        primaryStage.scene = createStyledScene(root, 600.0, 400.0)
        primaryStage.show()

        // add a label
        val label = javafx.scene.control.Label("Test!")
        // add to the rootpane but then left top with small padding around it
        rootPane.children.add(label)
        javafx.scene.layout.StackPane.setAlignment(label, javafx.geometry.Pos.TOP_LEFT)
        javafx.scene.layout.StackPane.setMargin(label, javafx.geometry.Insets(10.0))
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
