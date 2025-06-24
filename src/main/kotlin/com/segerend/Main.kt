package com.segerend

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

class MainApp : Application() {
    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        root.center = Label("Hello World!")

        primaryStage.title = "Sorteer app"
        val scene = Scene(root, 600.0, 400.0)
        scene.stylesheets.add(this::class.java.getResource("/style.css").toExternalForm())

        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        System.setProperty("apple.awt.application.name", "SegerEnd App")
    }

    Application.launch(MainApp::class.java)
}
