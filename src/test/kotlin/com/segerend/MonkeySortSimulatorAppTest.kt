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
import org.junit.jupiter.api.fail
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

    private fun waitUntilGridIsSorted(monkey: Monkey, timeoutMillis: Long = 30_000, pollInterval: Long = 500) {
        var elapsed = 0L
        val grid = monkeySortSimulatorApp.controller.gridModel

        while (!grid.isSorted() && elapsed < timeoutMillis) {
            Thread.sleep(pollInterval)
            elapsed += pollInterval

            interact {
                if (monkey.isIdle()) {
                    val task = monkey.strategy.getNextTask(grid)
                    if (task != null) {
                        monkey.assignTask(task, GameConfig.CELL_SIZE)
                    }
                }
            }
        }

        assertTrue(
            grid.isSorted(),
            "Grid should be sorted after $elapsed ms, but it is not sorted."
        )
    }

    @Test
    fun fruitComboTest() {
        val controller = monkeySortSimulatorApp.controller
        val monkey = controller.monkeys.firstOrNull()
        assertNotNull(monkey, "There should be at least one monkey in the game")

        // Prepare the test environment
        interact {
            monkey!!.state = IdleState(0.0, 0.0)
            monkey.algorithm = SortAlgorithm.BUBBLE
            controller.gridModel.fill(Fruit.EMPTY)
        }

        waitFx()

        // Ensure grid is empty
        val grid = controller.gridModel
        assertTrue(
            grid.getGridCopy().all { row -> row.all { it == Fruit.EMPTY } },
            "Grid should be empty before setting up the combo"
        )

        // Setup combo fruits in the grid
        interact {
            val comboFruits = listOf(
                Fruit.CHERRY, Fruit.APPLE, Fruit.CHERRY, Fruit.BANANA, Fruit.CHERRY
            )
            comboFruits.forEachIndexed { i, fruit ->
                grid.set(Pos(0, i), fruit)
            }

            repeat(20) { grid.set(Pos(0, it + 5), Fruit.CHERRY) }

            grid.set(Pos(0, 20), Fruit.APPLE)
            grid.set(Pos(0, 22), Fruit.APPLE)
            GameStats.timeFactor = 1000.0
        }

        waitFx()

        // Capture coins before sort
        setCoins(0)
        val initialCoins = GameStats.coins

        // Assign tasks until grid is sorted or timeout
        waitUntilGridIsSorted(monkey!!)

        assertTrue(grid.isSorted(), "Grid should be sorted after combo task")

        // Check combo reward was applied
        val expectedComboReward = GameConfig.COMBO_REWARD_MULTIPLIER * 3 // for 3 CHERRYs
        assertTrue(
            GameStats.coins >= initialCoins + expectedComboReward,
            "Coins should increase by at least $expectedComboReward from combo"
        )

        // Reset game speed
        GameStats.timeFactor = 1.0
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
