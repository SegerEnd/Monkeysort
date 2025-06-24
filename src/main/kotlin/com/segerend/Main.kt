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

//        try {
//            val appClass = Class.forName("com.apple.eawt.Application")
//            val getAppMethod = appClass.getMethod("getApplication")
//            val setIconMethod = appClass.getMethod("setDockIconImage", java.awt.Image::class.java)
//
//            val app = getAppMethod.invoke(null)
//            val icon = java.awt.Toolkit.getDefaultToolkit().getImage("src/main/resources/icon.png")
//            setIconMethod.invoke(app, icon)
//        } catch (e: Exception) {
//            println("Could not set macOS Dock icon: ${e.message}")
//        }
    }

    Application.launch(MainApp::class.java)
}
