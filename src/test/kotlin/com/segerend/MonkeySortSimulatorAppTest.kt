package com.segerend

import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest

class MonkeySortSimulatorAppTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp : MonkeySortSimulatorApp

    override fun start(stage: Stage) {
        monkeySortSimulatorApp = MonkeySortSimulatorApp()
        monkeySortSimulatorApp.start(stage)
//        stage.isMaximized = false
//        stage.isFullScreen = false
//        stage.toBack()
        stage.isIconified = true
    }

    @Test
    fun hasButtons() {
        val rootNode: BorderPane? = monkeySortSimulatorApp.root
        assertNotNull(rootNode, "There should be a root node")

        // Access the bottom node of the BorderPane
        val bottomNode = rootNode!!.bottom
        assertNotNull(bottomNode, "There should be a bottom node in the BorderPane")

        // Check if the bottom node is a container (e.g., HBox)
        val buttonContainer = bottomNode as? javafx.scene.layout.Pane
        assertNotNull(buttonContainer, "Bottom node should be a Pane containing buttons")

        // Filter out all Button instances
        val buttons = buttonContainer!!.children.filterIsInstance<Button>()

        // Assert there are more than 3 buttons
        assertTrue(buttons.size > 3, "There should be more than 3 buttons in the bottom container")
    }

    @Test
    fun hasCanvas() {
        val rootNode: BorderPane? = monkeySortSimulatorApp.root
        assertNotNull(rootNode, "There should be a root node")

        // Access the center node of the BorderPane
        val centerNode = rootNode!!.center
        assertNotNull(centerNode, "There should be a center node in the BorderPane")

        // Check if the center node is a Canvas
        assertTrue(centerNode is javafx.scene.canvas.Canvas, "Center node should be a Canvas")

        // Optionally, you can check if the Canvas has a non-null graphics context
        val canvas = centerNode as javafx.scene.canvas.Canvas
        assertNotNull(canvas.graphicsContext2D, "Canvas should have a non-null graphics context")

        assertTrue(canvas.width > 0, "Canvas width should be greater than 0")
        assertTrue(canvas.height > 0, "Canvas height should be greater than 0")

        assertNotNull(canvas.graphicsContext2D.fill, "Canvas graphics context should not be null")
    }
}