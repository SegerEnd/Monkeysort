package com.segerend

import com.segerend.sorting.SortAlgorithm
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
        interact { GameStats.reset() }

        fun clickButton(buttonId: String) {
            WaitForAsyncUtils.waitForFxEvents()
            Platform.runLater {
                val button = lookup(buttonId).queryButton()
                initialButtonAssert(button)
                fireButton(button)
            }
            WaitForAsyncUtils.waitForFxEvents()
        }

        // GameStats speed button tests
        Platform.runLater {
            GameStats.timeFactor = 1.0
        }
        WaitForAsyncUtils.waitForFxEvents()

        val initialGameSpeedTimeFactor = 1.0
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Initial game speed time factor should be 1.0 by default")

        // Speed x5 button
        clickButton("#speedx5Button")
        assertEquals(5.0, GameStats.timeFactor, "Game speed should be set to x5 after clicking the button")

        clickButton("#speedx5Button")
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset after clicking the speed x5 button again")

        // Super speed button
        clickButton("#speedx100Button")
        assertEquals(100.0, GameStats.timeFactor, "Game speed should be set to super speed after clicking the button")

        clickButton("#speedx100Button")
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset after clicking the super speed button again")

        // test #pauseButton to toggle between 0.0 and 1.0 timeFactor
        clickButton("#pauseButton")
        assertEquals(0.0, GameStats.timeFactor, "Game speed should be paused (0.0) after clicking the pause button")
        clickButton("#pauseButton")
        assertEquals(1.0, GameStats.timeFactor, "Game speed should be resumed (1.0) after clicking the pause button again")

        // give alot of money to buy upgrades
        interact { GameStats.coins = 1000000 }
        WaitForAsyncUtils.waitForFxEvents()
        // pres #upgradeBubbleButton
        clickButton("#upgradeBubbleButton")
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.BUBBLE },
            "All monkeys should be upgraded to BubbleSort after clicking the upgrade button")

        // test #upgradeInsertionButton
        clickButton("#upgradeInsertionButton")
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.INSERTION },
            "All monkeys should be upgraded to InsertionSort after clicking the upgrade button")

        interact { GameStats.coins = 0 }
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