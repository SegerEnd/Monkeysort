package com.segerend

import com.segerend.monkey.IdleState
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import kotlin.concurrent.thread

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
    fun hasElements() {
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

        // Access the center node of the BorderPane
        val centerNode = rootNode.center
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

    @Test
    fun fruitComboTest() {
        // set the monkey to idle
        monkeySortSimulatorApp.controller.monkeys.firstOrNull()?.let { it.state = IdleState(0.0, 0.0) }

        // set game speed to x10 for faster testing
        GameStats.timeFactor = 10.0

        // empty the grid
        monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY)
        assertTrue(
            monkeySortSimulatorApp.controller.gridModel.getGridCopy().all { row -> row.all { it == Fruit.EMPTY } },
            "GridModel should be filled with EMPTY fruits"
        )

        // Set up a grid with a 4-cell horizontal combo
        monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 0), Fruit.CHERRY)
        monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 1), Fruit.APPLE)
        monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 2), Fruit.CHERRY)
        monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 3), Fruit.BANANA)
        monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 4), Fruit.CHERRY)
        monkeySortSimulatorApp.controller.gridModel.set(Pos(1, 0), Fruit.CHERRY)

        // place with a monkey a shuffle task on the grid
        val monkey = monkeySortSimulatorApp.controller.monkeys.firstOrNull()
        assertNotNull(monkey, "There should be at least one monkey in the game")
        monkey!!.algorithm = SortAlgorithm.BUBBLE // Ensure the monkey is using a sorting algorithm

        var iterations = 0
        val maxIterations = 1000

        // set coins to 0
        GameStats.coins = 0
        WaitForAsyncUtils.waitForFxEvents()
        val initialCoinsBalance = GameStats.coins

        // wait for the monkey to complete the task
        while (!monkeySortSimulatorApp.controller.gridModel.isSorted() && iterations < maxIterations) {
            // sleep this function for 500 milliseconds
            Thread.sleep(500)
            if (monkey.isIdle()) {
                val task = monkey.strategy.getNextTask(monkeySortSimulatorApp.controller.gridModel)
                if (task != null) {
                    monkey.assignTask(task, GameConfig.CELL_SIZE)
                }
            }
        }

        assertTrue(monkeySortSimulatorApp.controller.gridModel.isSorted(), "Grid should be sorted after completing the combo task")

        WaitForAsyncUtils.waitForFxEvents()

        // check if the coins balance is increased by the combo reward
        val expectedComboReward = GameConfig.COMBO_REWARD_MULTIPLIER * 4 // 4 CHERRY fruits in the combo
        assertTrue { GameStats.coins >= expectedComboReward }
        assertTrue(GameStats.coins > initialCoinsBalance, "Coins should be increased after completing a combo task")
    }

    @Test
    fun spawn5Monkeys() {
        // Set coins to a high enough value to buy 5 monkeys
        GameStats.coins = monkeySortSimulatorApp.controller.getNewMonkeyPrice()
        GameStats.timeFactor = 5.0 // Speed up the game for faster testing
        WaitForAsyncUtils.waitForFxEvents()

        // Buy 5 monkeys
        repeat(5) {
            val buyButton = lookup("#buyButton").queryButton()
            assertFalse(buyButton.isDisable, "Buy button should be enabled when there are enough coins")
            buyButton.fire()
            WaitForAsyncUtils.waitForFxEvents()
            GameStats.coins += monkeySortSimulatorApp.controller.getNewMonkeyPrice()
            WaitForAsyncUtils.waitForFxEvents()
        }

        // Check if 5 monkeys are added to the game
        assertEquals(6, monkeySortSimulatorApp.controller.monkeys.size, "There should be 6 monkeys in the game after buying 5")

        // remove all monkeys except the first one
        monkeySortSimulatorApp.controller.monkeys.drop(1).forEach { monkey ->
            monkeySortSimulatorApp.controller.monkeys.remove(monkey)
        }
        WaitForAsyncUtils.waitForFxEvents()

        // test debug button spawn 5 monkeys
        val debugButton = lookup("#debugSpawn5MonkeysButton").queryButton()
        assertNotNull(debugButton, "Debug button should be present in the UI")
        repeat(3) {
            Thread.sleep(50)
            debugButton.fire()
            WaitForAsyncUtils.waitForFxEvents()
        }

        // Check if the 10 monkeys are added to the game
        assertEquals(16, monkeySortSimulatorApp.controller.monkeys.size, "There should be 16 monkeys in the game after spawning two times the 5 more monkeys")
    }
}