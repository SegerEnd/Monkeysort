package com.segerend

import com.segerend.monkey.*
import com.segerend.sorting.SortAlgorithm
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameControllerTest {
    private lateinit var controller: GameController

    @BeforeEach
    fun setUp() {
        controller = GameController() // it should automatically use GameConfig.COLS and GameConfig.ROWS
        GameStats.coins = 0 // Reset coins
    }

    @Test
    fun getGridModel() {
        val grid = controller.gridModel
        val gridCopy = grid.getGridCopy()
        assertNotNull(grid, "Grid model should not be null")
        assertEquals(GameConfig.COLS, gridCopy.size, "Grid should have correct number of columns")
        assertEquals(GameConfig.ROWS, gridCopy[0].size, "Grid should have correct number of rows")
        assertTrue(gridCopy.all { row -> row.all { it != Fruit.EMPTY } }, "Grid should not contain any EMPTY cells")
    }

    @Test
    fun getMonkeys() {
        val monkeys = controller.monkeys
        assertNotNull(monkeys, "Monkeys list should not be null, because the game should start with one monkey")
        assertEquals(1, monkeys.size, "Game should start with one monkey")
    }

    @Test
    fun getParticleSystem() {
        val particleSystem = controller.particleSystem
        assertNotNull(particleSystem, "Particle system should not be null")
    }

    @Test
    fun tick() {
        val lastFrameTime = System.nanoTime()
        val now = System.nanoTime()
        val deltaMs = (now - lastFrameTime) / 1_000_000.0 // Convert to milliseconds
        val frameTime = FrameTime(deltaMs, now / 1_000_000_000.0)
        assertDoesNotThrow { controller.tick(frameTime) }
    }

    @Test
    fun buyMonkey() {
        assertTrue(controller.getNewMonkeyPrice() >= GameConfig.MONKEY_BASE_COST, "New monkey price should be greater than zero")

        GameStats.coins = controller.getNewMonkeyPrice()

        // Buy a monkey with enough coins
        assertTrue(controller.buyMonkey(), "Buying a monkey should succeed with enough coins")
        assertEquals(0, GameStats.coins, "Coins should be reduced to zero after buying a monkey")

        // Try to buy another monkey without enough coins
        GameStats.coins = 0
        assertFalse(controller.buyMonkey(), "Buying a monkey should fail with insufficient coins")
        assertEquals(0, GameStats.coins, "Coins should not change when trying to buy a monkey with insufficient coins")
    }

    //    fun upgradeMonkey(): Boolean {
    //        if (GameStats.coins >= GameConfig.MONKEY_UPGRADE_COST) {
    //            monkeys.firstOrNull { it.algorithm == SortAlgorithm.BOGO }?.let {
    //                it.algorithm = SortAlgorithm.BUBBLE
    //                GameStats.coins -= GameConfig.MONKEY_UPGRADE_COST
    //                return true
    //            }
    //        }
    //        return false
    //    }
    @Test
    fun upgradeMonkey() {
        val monkey : Monkey = controller.monkeys.firstOrNull { it.algorithm == SortAlgorithm.BOGO }?.let {
            // Check if the monkey is found and has the BOGO algorithm
            assertNotNull(it, "There should be a monkey with BOGO algorithm to upgrade")
            assertEquals(SortAlgorithm.BOGO, it.algorithm, "Monkey should start with BOGO algorithm")
            it
        } ?: run {
            fail("No monkey found with BOGO algorithm to upgrade")
        }
        // Upgrade monkey to SortAlgorithm.BUBBLE check if upgrade is possible and if the monkey's algorithm is updated
        GameStats.coins = GameConfig.MONKEY_UPGRADE_COST
        assertTrue(controller.upgradeMonkey(), "Upgrading a monkey should succeed with enough coins")
        assertEquals(0, GameStats.coins, "Coins should be reduced to zero after upgrading a monkey")
        // check if the monkey's algorithm is updated to BUBBLE
        assertEquals(SortAlgorithm.BUBBLE, monkey.algorithm, "Monkey's algorithm should be upgraded to BUBBLE")

        // try to upgrade again without an available monkey with BOGO algorithm
        GameStats.coins = GameConfig.MONKEY_UPGRADE_COST
        assertFalse(controller.upgradeMonkey(), "Upgrading a monkey should fail if no monkey with BOGO algorithm is available")

        // Try to upgrade again without enough coins
        GameStats.coins = 0
        assertFalse(controller.upgradeMonkey(), "Upgrading a monkey should fail with insufficient coins")
    }
}