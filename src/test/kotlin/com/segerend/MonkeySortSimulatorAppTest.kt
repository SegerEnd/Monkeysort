package com.segerend

import com.segerend.monkey.IdleState
import javafx.application.Platform
import javafx.scene.control.Button
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonkeySortSimulatorAppTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp : MonkeySortSimulatorApp
    private lateinit var stage: Stage

    private val buyButton: Button
        get() = lookup("#buyButton").queryButton()

    private val debugButton: Button
        get() = lookup("#debugSpawn5MonkeysButton").queryButton()

    private fun waitFx() = WaitForAsyncUtils.waitForFxEvents()

    private fun setCoins(amount: Int) = interact { GameStats.coins = amount }

    private fun clearGrid() = monkeySortSimulatorApp.controller.gridModel.fill(Fruit.EMPTY)

    override fun start(stage: Stage) {
        this.stage = stage
        monkeySortSimulatorApp = MonkeySortSimulatorApp().also {
            it.start(stage)
        }
        stage.toFront()
    }

    @BeforeEach
    fun setUp() {
        LockManager.clear()
    }

    @Test
    fun automaticButtonChanges() {
        val initialMonkeyCount = monkeySortSimulatorApp.controller.monkeys.size

        setCoins(0)
        waitFx()
        assertTrue(buyButton.isDisable)

        setCoins(monkeySortSimulatorApp.controller.getNewMonkeyPrice() + 10)
        waitFx()
        assertFalse(buyButton.isDisable)

        interact { buyButton.fire() }
        waitFx()
        assertEquals(initialMonkeyCount + 1, monkeySortSimulatorApp.controller.monkeys.size)
    }

    @Test
    fun spawn5Monkeys() {
        setCoins(monkeySortSimulatorApp.controller.getNewMonkeyPrice())
        GameStats.timeFactor = 5.0
        waitFx()

        repeat(5) {
            assertFalse(buyButton.isDisable)
            buyButton.fire()
            waitFx()
            setCoins(GameStats.coins + monkeySortSimulatorApp.controller.getNewMonkeyPrice())
            waitFx()
        }

        assertEquals(6, monkeySortSimulatorApp.controller.monkeys.size)

        monkeySortSimulatorApp.controller.monkeys.drop(1).forEach {
            monkeySortSimulatorApp.controller.monkeys.remove(it)
        }

        repeat(3) {
            Thread.sleep(50)
            debugButton.fire()
            waitFx()
        }

        assertEquals(16, monkeySortSimulatorApp.controller.monkeys.size)
    }

    @AfterEach
    fun cleanup() {
        resetMonkeys()
        waitFx()
        LockManager.clear()
        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(monkeySortSimulatorApp)
        GameStats.reset()
    }

    @AfterAll
    fun tearDown() {
        Platform.runLater {
            monkeySortSimulatorApp.controller.monkeys.clear()
            if (stage.isShowing) stage.close()
        }
        waitFx()
        clearGrid()
        monkeySortSimulatorApp = MonkeySortSimulatorApp()
    }

    private fun resetMonkeys() {
        monkeySortSimulatorApp.controller.monkeys.clear()
        if (monkeySortSimulatorApp.controller.monkeys.isEmpty()) {
            monkeySortSimulatorApp.controller.monkeys.add(
                Monkey(SortAlgorithm.BOGO).apply {
                    state = IdleState(0.0, 0.0)
                }
            )
        }
    }
}
