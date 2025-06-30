package com.segerend

import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils

class MonkeySortSimulatorAppTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp : MonkeySortSimulatorApp

    override fun start(stage: Stage) {
        monkeySortSimulatorApp = MonkeySortSimulatorApp()
        monkeySortSimulatorApp.start(stage)
//        stage.isMaximized = false
//        stage.isFullScreen = false
//        stage.toBack()
//        stage.isIconified = true
        stage.toFront()
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

        // check if the grid model is initialized with random fruits
        assertNotNull(monkeySortSimulatorApp.controller.gridModel, "GridModel should be initialized")
        // check if there are random fruits in the grid
        assertTrue(
            monkeySortSimulatorApp.controller.gridModel.getGridCopy().any { row -> row.any { it != Fruit.EMPTY } },
            "GridModel should contain random fruits"
        )
        monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY)
    }

    @Test
    fun automaticButtonChanges() {
        var monkeyCount = monkeySortSimulatorApp.controller.monkeys.size
        val buyButton = lookup("#buyButton").queryButton()

        // Set coins to zero, button should be disabled
        GameStats.coins = 0
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(buyButton.isDisable, "Buy button should be disabled when there are no coins")

        // Set coins high enough, button should enable
        GameStats.coins = monkeySortSimulatorApp.controller.getNewMonkeyPrice() + 10
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(GameStats.coins > 0, "Coins should be greater than zero")

        // Refresh buyButton reference after UI update (optional but safer)
        val updatedBuyButton = lookup("#buyButton").queryButton()
        assertFalse(updatedBuyButton.isDisable, "Buy button should be enabled when there are enough coins")

        // Fire the buy button action on the FX thread
        updatedBuyButton.fire()

        WaitForAsyncUtils.waitForFxEvents()

        // check if one monkey is added to the game
        assertEquals(monkeyCount + 1, monkeySortSimulatorApp.controller.monkeys.size, "A monkey should be added to the game after buying")

        // when coins are zero press the buy button again, it should not add a monkey
        GameStats.coins = 0
        updatedBuyButton.isDisable = false // Manually enable the button for testing
        updatedBuyButton.fire()
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(monkeyCount + 1, monkeySortSimulatorApp.controller.monkeys.size, "No monkey should be added when coins are zero")
    }
}