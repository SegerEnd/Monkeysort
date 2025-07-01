package com.segerend

import com.segerend.monkey.IdleState
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testfx.api.FxToolkit
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonkeySortSimulatorAppTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp : MonkeySortSimulatorApp
    private lateinit var stage: Stage

    @BeforeEach
    fun setUp() {
        LockManager.clear()
    }

    override fun start(stage: Stage) {
        this.stage = stage
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

        WaitForAsyncUtils.waitForFxEvents()
        val buyButton = lookup("#buyButton").queryButton()

        interact {
            GameStats.coins = 0
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(buyButton.isDisable, "Buy button should be disabled when there are no coins")

        interact {
            GameStats.coins = monkeySortSimulatorApp.controller.getNewMonkeyPrice() + 10
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertFalse(buyButton.isDisable, "Buy button should be enabled when there are enough coins")

        interact {
            buyButton.fire()
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(monkeyCount + 1, monkeySortSimulatorApp.controller.monkeys.size)

        interact {
            GameStats.coins = 0
            buyButton.isDisable = false
            buyButton.fire()
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(monkeyCount + 1, monkeySortSimulatorApp.controller.monkeys.size)
    }

    @Test
    fun fruitComboTest() {
        LockManager.clear()

        val monkey = monkeySortSimulatorApp.controller.monkeys.firstOrNull()
        assertNotNull(monkey, "There should be at least one monkey in the game")

        // Set the monkey to idle safely on FX thread
        interact {
            monkey!!.state = IdleState(0.0, 0.0)
            monkey.algorithm = SortAlgorithm.BUBBLE // Set sorting algorithm
            monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY)
        }

        // Empty the grid and verify it’s empty
        monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY)

        WaitForAsyncUtils.waitForFxEvents()

        assertTrue(
            monkeySortSimulatorApp.controller.gridModel.getGridCopy().all { row -> row.all { it == Fruit.EMPTY } },
            "Grid should be empty before setting up the combo"
        )

        // Set up a grid with a 4-cell horizontal combo on FX thread
        interact {
            GameStats.timeFactor = 1_000.0 // Speed up the game for faster testing
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 0), Fruit.CHERRY)
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 1), Fruit.APPLE)
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 2), Fruit.CHERRY)
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 3), Fruit.BANANA)
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 4), Fruit.CHERRY)
            repeat(20) { col ->
                monkeySortSimulatorApp.controller.gridModel.set(Pos(0, col + 5), Fruit.CHERRY)
            }
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 20), Fruit.APPLE)
            monkeySortSimulatorApp.controller.gridModel.set(Pos(0, 22), Fruit.APPLE)
        }
        WaitForAsyncUtils.waitForFxEvents()

        // Reset coins to 0 safely
        interact {
            GameStats.coins = 0
        }
        WaitForAsyncUtils.waitForFxEvents()
        val initialCoinsBalance = GameStats.coins

        // Poll and assign tasks until grid is sorted or timeout
        var maxWaitTimeMs = 30_000L
        var pollIntervalMs = 500L
        var elapsed = 0L

        while (!monkeySortSimulatorApp.controller.gridModel.isSorted() && elapsed < maxWaitTimeMs) {
            Thread.sleep(pollIntervalMs)
            elapsed += pollIntervalMs

            interact {
                if (monkey!!.isIdle()) {
                    val task = monkey.strategy.getNextTask(monkeySortSimulatorApp.controller.gridModel)
                    if (task != null) {
                        monkey.assignTask(task, GameConfig.CELL_SIZE)
                    }
                }
            }
        }

        assertTrue(
            monkeySortSimulatorApp.controller.gridModel.isSorted(),
            "Grid should be sorted after completing the combo task"
        )

        GameStats.timeFactor = 1.0

        WaitForAsyncUtils.waitForFxEvents()

        val expectedComboReward = GameConfig.COMBO_REWARD_MULTIPLIER * 3 // 3 CHERRY fruits in the combo
        assertTrue(
            GameStats.coins >= initialCoinsBalance + expectedComboReward,
            "Coins should be increased by the combo reward after completing a combo task"
        )
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

    @AfterEach
    fun cleanup() {
        monkeySortSimulatorApp.controller.monkeys.clear() // Clear monkeys after each test
        if (monkeySortSimulatorApp.controller.monkeys.isEmpty()) {
            val newMonkey = Monkey(SortAlgorithm.BOGO)
            newMonkey.state = IdleState(0.0, 0.0)
            monkeySortSimulatorApp.controller.monkeys.add(newMonkey)
        }
        WaitForAsyncUtils.waitForFxEvents()

        LockManager.clear()
        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(monkeySortSimulatorApp)
        GameStats.reset() // Reset game stats after each test
    }

    @AfterAll
    fun tearDown() {
        // Any global cleanup after all tests
        LockManager.clear()
        Platform.runLater {
            monkeySortSimulatorApp.controller.monkeys.clear()
            if (stage.isShowing) {
                stage.close()
            }
        }
        WaitForAsyncUtils.waitForFxEvents()
        monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY) // Clear the grid model
        monkeySortSimulatorApp = MonkeySortSimulatorApp() // Reset the app instance
    }
}