package com.segerend

import javafx.scene.control.Button
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils

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
        // Spawn 5 monkeys
        var debugButton = lookup("#debugSpawn5MonkeysButton").queryButton()
        assertNotNull(debugButton, "Debug button should be present in the UI")
        fireButton(debugButton)
        assertEquals(6, monkeySortSimulatorApp.controller.monkeys.size, "There should be 6 monkeys after spawning 5")
        // check if all monkeys are bogosort initially
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.BOGO })

        debugButton = lookup("#debugBubbleButton").queryButton()
        initialButtonAssert(debugButton)
        fireButton(debugButton)
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.BUBBLE },
            "All monkeys should be set to Bubble Sort after clicking the debug bubble button")

        WaitForAsyncUtils.waitForFxEvents()

        // Check if the debug buttons are present
        debugButton = lookup("#debugBogoButton").queryButton()
        initialButtonAssert(debugButton)
        fireButton(debugButton)
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.BOGO },
            "All monkeys should be set to Bogo Sort after clicking the debug bogo button")

        debugButton = lookup("#debugInsertionButton").queryButton()
        initialButtonAssert(debugButton)
        fireButton(debugButton)
        assertTrue(monkeySortSimulatorApp.controller.monkeys.all { it.algorithm == SortAlgorithm.INSERTION },
            "All monkeys should be set to Insertion Sort after clicking the debug insertion button")

        val initialGameSpeedTimeFactor = GameStats.timeFactor
        assertEquals(1.0, initialGameSpeedTimeFactor, "Initial game speed time factor should be 1.0 by default")

        // debugSpeedx25Button
        debugButton = lookup("#debugSpeedx25Button").queryButton()
        initialButtonAssert(debugButton)
        fireButton(debugButton)
        assertEquals(25.0, GameStats.timeFactor, "Game speed should be set to 25x after clicking the debug speed x25 button")

        // Reset game speed by pressing again the speed x25 button
        fireButton(debugButton)
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset to initial value after clicking the debug speed x25 button again")

        debugButton = lookup("#debugSuperSpeedButton").queryButton()
        initialButtonAssert(debugButton)
        fireButton(debugButton)
        assertEquals(1000000.0, GameStats.timeFactor, "Game speed should be set to super speed after clicking the debug super speed button")

        WaitForAsyncUtils.waitForFxEvents()

        // Reset game speed by pressing again the super speed button
        fireButton(debugButton)
        assertEquals(initialGameSpeedTimeFactor, GameStats.timeFactor, "Game speed should be reset to initial value after clicking the debug super speed button again")
    }
}