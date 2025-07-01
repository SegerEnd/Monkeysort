package com.segerend

import javafx.application.Platform
import javafx.scene.control.Button
import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DebugButtonsTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp: MonkeySortSimulatorApp
    private lateinit var stage: Stage

    override fun start(stage: Stage) {
        monkeySortSimulatorApp = MonkeySortSimulatorApp()
        monkeySortSimulatorApp.start(stage)
        stage.toFront()
        this.stage = stage
    }

    fun initialButtonAssert(button: Button, disabled: Boolean = false) {
        assertNotNull(button, "Button should not be null")
        assertTrue(button.isVisible, "Button should be visible")
        if (disabled) {
            assertTrue(button.isDisabled, "Button should be disabled")
        } else {
            assertFalse(button.isDisabled, "Button should not be disabled")
        }
    }

    fun fireButton(button: Button) {
        Thread.sleep(50)
        button.fire()
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testDebugButtons() {
        fun assertAllMonkeysAlgorithm(expected: SortAlgorithm, message: String) {
            assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == expected }, message)
        }

        fun clickButton(buttonId: String) {
            Platform.runLater {
                val button = lookup(buttonId).queryButton()
                initialButtonAssert(button)
                fireButton(button)
            }
            WaitForAsyncUtils.waitForFxEvents()
        }

        // Spawn 5 monkeys
        Platform.runLater {
            val debugButton = lookup("#debugSpawn5MonkeysButton").queryButton()
            assertNotNull(debugButton, "Debug button should be present in the UI")
            fireButton(debugButton)
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(6, monkeySortSimulatorApp.controller.monkeys.size, "There should be 6 monkeys after spawning 5")
        assertAllMonkeysAlgorithm(SortAlgorithm.BOGO, "All monkeys should be BogoSort initially")

        // Debug buttons for setting sorting algorithms
        val algorithmButtons = mapOf(
            "#debugBubbleButton" to SortAlgorithm.BUBBLE,
            "#debugBogoButton" to SortAlgorithm.BOGO,
            "#debugInsertionButton" to SortAlgorithm.INSERTION
        )

        algorithmButtons.forEach { (buttonId, algorithm) ->
            clickButton(buttonId)
            assertAllMonkeysAlgorithm(algorithm, "All monkeys should be set to $algorithm after clicking $buttonId")
        }

        // GameStats speed button tests
        Platform.runLater {
            GameStats.timeFactor = 1.0
        }
        WaitForAsyncUtils.waitForFxEvents()

        val initialGameSpeedTimeFactor = GameStats.timeFactor
        assertEquals(1.0, initialGameSpeedTimeFactor, "Initial game speed time factor should be 1.0 by default")

        // Speed x5 button
        clickButton("#debugSpeedx5Button")
        assertEquals(5.0, GameStats.timeFactor, "Game speed should be set to x5 after clicking the button")

        clickButton("#debugSpeedx5Button")
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset after clicking the speed x5 button again")

        // Super speed button
        clickButton("#debugSuperSpeedButton")
        assertEquals(1_000_000.0, GameStats.timeFactor, "Game speed should be set to super speed after clicking the button")

        clickButton("#debugSuperSpeedButton")
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset after clicking the super speed button again")
    }

    @AfterEach
    fun tearDownEach() {
        LockManager.clear()
        GameStats.reset()
    }

    @AfterAll
    fun tearDownAll() {
        // Close the application after each test
        Platform.runLater {
            if (stage.isShowing) {
                stage.close()
            }
            monkeySortSimulatorApp.controller.monkeys.clear()
        }
        WaitForAsyncUtils.waitForFxEvents()

        monkeySortSimulatorApp = MonkeySortSimulatorApp()
    }
}